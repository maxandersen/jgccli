package dk.xam.jgccli.cli;

import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventAttendee;
import dk.xam.jgccli.service.CalendarService;
import jakarta.inject.Inject;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import static dk.xam.jgccli.cli.OutputFormatter.*;

@Command(name = "event", description = "Get event details")
public class EventCommand implements Callable<Integer> {

    @Inject
    CalendarService service;

    @Parameters(index = "0", description = "Email account")
    String email;

    @Parameters(index = "1", description = "Calendar ID")
    String calendarId;

    @Parameters(index = "2", description = "Event ID")
    String eventId;

    @Override
    public Integer call() throws Exception {
        Event event = service.getEvent(email, calendarId, eventId);

        printKeyValue("ID", event.getId());
        printKeyValue("Summary", orDefault(event.getSummary(), "(no title)"));

        String start = "";
        String end = "";
        if (event.getStart() != null) {
            start = event.getStart().getDateTime() != null 
                ? event.getStart().getDateTime().toString()
                : (event.getStart().getDate() != null ? event.getStart().getDate().toString() : "");
        }
        if (event.getEnd() != null) {
            end = event.getEnd().getDateTime() != null 
                ? event.getEnd().getDateTime().toString()
                : (event.getEnd().getDate() != null ? event.getEnd().getDate().toString() : "");
        }

        printKeyValue("Start", start);
        printKeyValue("End", end);

        if (event.getLocation() != null) {
            printKeyValue("Location", event.getLocation());
        }
        if (event.getDescription() != null) {
            printKeyValue("Description", event.getDescription());
        }
        if (event.getAttendees() != null && !event.getAttendees().isEmpty()) {
            String attendees = event.getAttendees().stream()
                .map(EventAttendee::getEmail)
                .collect(Collectors.joining(", "));
            printKeyValue("Attendees", attendees);
        }

        if (event.getTransparency() != null) {
            printKeyValue("Availability", "transparent".equals(event.getTransparency()) ? "free" : "busy");
        }

        printKeyValue("Status", event.getStatus());
        printKeyValue("Link", event.getHtmlLink());

        return 0;
    }
}
