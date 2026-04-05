package dk.xam.jgccli.cli;

import com.google.api.services.calendar.model.Event;
import dk.xam.jgccli.service.CalendarService;
import jakarta.inject.Inject;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

@Command(name = "create", description = "Create a new event")
public class CreateCommand implements Callable<Integer> {

    @Inject
    CalendarService service;

    @Parameters(index = "0", description = "Email account")
    String email;

    @Parameters(index = "1", description = "Calendar ID")
    String calendarId;

    @Option(names = "--summary", required = true, description = "Event title")
    String summary;

    @Option(names = "--start", required = true, description = "Start time (ISO 8601)")
    String start;

    @Option(names = "--end", required = true, description = "End time (ISO 8601)")
    String end;

    @Option(names = "--description", description = "Event description")
    String description;

    @Option(names = "--location", description = "Event location")
    String location;

    @Option(names = "--attendees", description = "Attendees (comma-separated emails)")
    String attendees;

    @Option(names = "--all-day", description = "Create all-day event")
    boolean allDay;

    @Override
    public Integer call() throws Exception {
        List<String> attendeeList = attendees != null 
            ? Arrays.asList(attendees.split(","))
            : null;

        Event event = service.createEvent(
            email, calendarId,
            summary, description, location,
            start, end,
            attendeeList, allDay
        );

        System.out.println("Created: " + event.getId());
        System.out.println("Link: " + event.getHtmlLink());
        return 0;
    }
}
