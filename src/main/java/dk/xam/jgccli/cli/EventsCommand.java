package dk.xam.jgccli.cli;

import com.google.api.services.calendar.model.Event;
import dk.xam.jgccli.model.EventSearchResult;
import dk.xam.jgccli.service.CalendarService;
import jakarta.inject.Inject;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import static dk.xam.jgccli.cli.OutputFormatter.*;

@Command(name = "events", description = "List events from a calendar")
public class EventsCommand implements Callable<Integer> {

    @Inject
    CalendarService service;

    @Parameters(index = "0", description = "Email account")
    String email;

    @Parameters(index = "1", description = "Calendar ID")
    String calendarId;

    @Option(names = "--from", description = "Start time (ISO 8601, default: now)")
    String from;

    @Option(names = "--to", description = "End time (ISO 8601, default: 1 week from now)")
    String to;

    @Option(names = "--max", description = "Max results (default: 10)")
    Integer maxResults;

    @Option(names = "--page", description = "Page token for pagination")
    String pageToken;

    @Option(names = "--query", description = "Free text search")
    String query;

    @Override
    public Integer call() throws Exception {
        Instant now = Instant.now();
        String timeMin = from != null ? from : now.toString();
        String timeMax = to != null ? to : now.plus(7, ChronoUnit.DAYS).toString();

        EventSearchResult result = service.listEvents(
            email, calendarId, timeMin, timeMax, maxResults, pageToken, query
        );

        if (result.events().isEmpty()) {
            System.out.println("No events");
        } else {
            List<String[]> rows = new ArrayList<>();
            for (Event e : result.events()) {
                String start = "";
                String end = "";
                if (e.getStart() != null) {
                    start = e.getStart().getDateTime() != null 
                        ? e.getStart().getDateTime().toString()
                        : (e.getStart().getDate() != null ? e.getStart().getDate().toString() : "");
                }
                if (e.getEnd() != null) {
                    end = e.getEnd().getDateTime() != null 
                        ? e.getEnd().getDateTime().toString()
                        : (e.getEnd().getDate() != null ? e.getEnd().getDate().toString() : "");
                }
                rows.add(new String[]{
                    orEmpty(e.getId()),
                    start,
                    end,
                    orDefault(e.getSummary(), "(no title)")
                });
            }
            printTable(new String[]{"ID", "START", "END", "SUMMARY"}, rows);

            if (result.nextPageToken() != null) {
                System.out.println();
                System.out.println("# Next page: --page " + result.nextPageToken());
            }
        }
        return 0;
    }
}
