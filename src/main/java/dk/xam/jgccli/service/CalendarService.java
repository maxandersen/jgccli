package dk.xam.jgccli.service;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.*;
import dk.xam.jgccli.exception.AccountNotFoundException;
import dk.xam.jgccli.exception.GccliException;
import dk.xam.jgccli.model.CalendarAccount;
import dk.xam.jgccli.model.Credentials;
import dk.xam.jgccli.model.Credentials.CredentialsStore;
import dk.xam.jgccli.model.EventSearchResult;
import dk.xam.jgccli.model.OAuth2Credentials;
import dk.xam.jgccli.oauth.CalendarOAuthFlow;
import dk.xam.jgccli.storage.AccountStorage;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@ApplicationScoped
public class CalendarService {

    private static final String APPLICATION_NAME = "jgccli";
    private static final GsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    @Inject
    AccountStorage accountStorage;

    private final Map<String, Calendar> calendarClients = new ConcurrentHashMap<>();
    private NetHttpTransport transport;

    private NetHttpTransport getTransport() {
        if (transport == null) {
            try {
                transport = GoogleNetHttpTransport.newTrustedTransport();
            } catch (GeneralSecurityException | IOException e) {
                throw new GccliException("Failed to initialize HTTP transport", e);
            }
        }
        return transport;
    }

    public void addAccount(String email, String credentialsName, boolean manual, boolean force) {
        if (accountStorage.hasAccount(email) && !force) {
            throw new GccliException("Account '" + email + "' already exists. Use --force to re-authorize.");
        }

        Credentials creds = accountStorage.getCredentials(credentialsName);
        if (creds == null) {
            String name = credentialsName != null ? credentialsName : Credentials.DEFAULT_NAME;
            throw new GccliException("Credentials '" + name + "' not found. Run: jgccli accounts credentials <file.json>" +
                (credentialsName != null ? " --name " + credentialsName : ""));
        }

        CalendarOAuthFlow oauthFlow = new CalendarOAuthFlow(creds.clientId(), creds.clientSecret());
        String refreshToken = oauthFlow.authorize(manual);

        CalendarAccount account = new CalendarAccount(
            email,
            credentialsName,  // null for default
            new OAuth2Credentials(creds.clientId(), creds.clientSecret(), refreshToken)
        );

        // Remove from cache if re-authorizing
        calendarClients.remove(email);
        accountStorage.addAccount(account);
    }

    public boolean deleteAccount(String email) {
        calendarClients.remove(email);
        return accountStorage.deleteAccount(email);
    }

    public List<CalendarAccount> listAccounts() {
        return accountStorage.getAllAccounts();
    }

    public void setCredentials(String name, String clientId, String clientSecret) {
        accountStorage.setCredentials(name, clientId, clientSecret);
    }

    public Credentials getCredentials(String name) {
        return accountStorage.getCredentials(name);
    }

    public CredentialsStore getAllCredentials() {
        return accountStorage.getAllCredentials();
    }

    public boolean removeCredentials(String name) {
        return accountStorage.removeCredentials(name);
    }

    @SuppressWarnings("deprecation")
    private Calendar getCalendarClient(String email) {
        return calendarClients.computeIfAbsent(email, e -> {
            CalendarAccount account = accountStorage.getAccount(e);
            if (account == null) {
                throw new AccountNotFoundException(e);
            }

            GoogleCredential credential = new GoogleCredential.Builder()
                .setTransport(getTransport())
                .setJsonFactory(JSON_FACTORY)
                .setClientSecrets(account.oauth2().clientId(), account.oauth2().clientSecret())
                .build()
                .setRefreshToken(account.oauth2().refreshToken());

            if (account.oauth2().accessToken() != null) {
                credential.setAccessToken(account.oauth2().accessToken());
            }

            return new Calendar.Builder(getTransport(), JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
        });
    }

    public List<CalendarListEntry> listCalendars(String email) throws IOException {
        Calendar calendar = getCalendarClient(email);
        CalendarList response = calendar.calendarList().list().execute();
        return response.getItems() != null ? response.getItems() : Collections.emptyList();
    }

    public List<AclRule> getCalendarAcl(String email, String calendarId) throws IOException {
        Calendar calendar = getCalendarClient(email);
        Acl response = calendar.acl().list(calendarId).execute();
        return response.getItems() != null ? response.getItems() : Collections.emptyList();
    }

    public EventSearchResult listEvents(String email, String calendarId,
                                        String timeMin, String timeMax,
                                        Integer maxResults, String pageToken,
                                        String query) throws IOException {
        Calendar calendar = getCalendarClient(email);

        Calendar.Events.List request = calendar.events().list(calendarId)
            .setSingleEvents(true)
            .setOrderBy("startTime");

        if (timeMin != null) {
            request.setTimeMin(new DateTime(timeMin));
        }
        if (timeMax != null) {
            request.setTimeMax(new DateTime(timeMax));
        }
        if (maxResults != null) {
            request.setMaxResults(maxResults);
        } else {
            request.setMaxResults(10);
        }
        if (pageToken != null) {
            request.setPageToken(pageToken);
        }
        if (query != null) {
            request.setQ(query);
        }

        Events response = request.execute();
        List<Event> events = response.getItems() != null ? response.getItems() : Collections.emptyList();
        return new EventSearchResult(events, response.getNextPageToken());
    }

    public Event getEvent(String email, String calendarId, String eventId) throws IOException {
        Calendar calendar = getCalendarClient(email);
        return calendar.events().get(calendarId, eventId).execute();
    }

    public Event createEvent(String email, String calendarId,
                             String summary, String description, String location,
                             String start, String end,
                             List<String> attendees, boolean allDay) throws IOException {
        Calendar calendar = getCalendarClient(email);

        Event event = new Event()
            .setSummary(summary)
            .setDescription(description)
            .setLocation(location);

        if (allDay) {
            event.setStart(new EventDateTime().setDate(new DateTime(start)));
            event.setEnd(new EventDateTime().setDate(new DateTime(end)));
        } else {
            event.setStart(new EventDateTime().setDateTime(new DateTime(start)));
            event.setEnd(new EventDateTime().setDateTime(new DateTime(end)));
        }

        if (attendees != null && !attendees.isEmpty()) {
            event.setAttendees(attendees.stream()
                .map(e -> new EventAttendee().setEmail(e))
                .collect(Collectors.toList()));
        }

        return calendar.events().insert(calendarId, event).execute();
    }

    public Event updateEvent(String email, String calendarId, String eventId,
                             String summary, String description, String location,
                             String start, String end,
                             List<String> attendees, Boolean allDay) throws IOException {
        Calendar calendar = getCalendarClient(email);

        // Get existing event first
        Event existing = getEvent(email, calendarId, eventId);

        if (summary != null) {
            existing.setSummary(summary);
        }
        if (description != null) {
            existing.setDescription(description);
        }
        if (location != null) {
            existing.setLocation(location);
        }

        if (start != null) {
            if (Boolean.TRUE.equals(allDay)) {
                existing.setStart(new EventDateTime().setDate(new DateTime(start)));
            } else {
                existing.setStart(new EventDateTime().setDateTime(new DateTime(start)));
            }
        }

        if (end != null) {
            if (Boolean.TRUE.equals(allDay)) {
                existing.setEnd(new EventDateTime().setDate(new DateTime(end)));
            } else {
                existing.setEnd(new EventDateTime().setDateTime(new DateTime(end)));
            }
        }

        if (attendees != null) {
            existing.setAttendees(attendees.stream()
                .map(e -> new EventAttendee().setEmail(e))
                .collect(Collectors.toList()));
        }

        return calendar.events().update(calendarId, eventId, existing).execute();
    }

    public void deleteEvent(String email, String calendarId, String eventId) throws IOException {
        Calendar calendar = getCalendarClient(email);
        calendar.events().delete(calendarId, eventId).execute();
    }

    public Map<String, List<TimePeriod>> getFreeBusy(String email, List<String> calendarIds,
                                                      String timeMin, String timeMax) throws IOException {
        Calendar calendar = getCalendarClient(email);

        FreeBusyRequest request = new FreeBusyRequest()
            .setTimeMin(new DateTime(timeMin))
            .setTimeMax(new DateTime(timeMax))
            .setItems(calendarIds.stream()
                .map(id -> new FreeBusyRequestItem().setId(id))
                .collect(Collectors.toList()));

        FreeBusyResponse response = calendar.freebusy().query(request).execute();

        Map<String, List<TimePeriod>> result = new LinkedHashMap<>();
        Map<String, FreeBusyCalendar> calendars = response.getCalendars();

        if (calendars != null) {
            for (Map.Entry<String, FreeBusyCalendar> entry : calendars.entrySet()) {
                List<TimePeriod> busy = entry.getValue().getBusy();
                result.put(entry.getKey(), busy != null ? busy : Collections.emptyList());
            }
        }

        return result;
    }
}
