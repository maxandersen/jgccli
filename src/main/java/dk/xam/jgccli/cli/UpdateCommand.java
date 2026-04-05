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

@Command(name = "update", description = "Update an existing event")
public class UpdateCommand implements Callable<Integer> {

    @Inject
    CalendarService service;

    @Parameters(index = "0", description = "Email account")
    String email;

    @Parameters(index = "1", description = "Calendar ID")
    String calendarId;

    @Parameters(index = "2", description = "Event ID")
    String eventId;

    @Option(names = "--summary", description = "Event title")
    String summary;

    @Option(names = "--start", description = "Start time (ISO 8601)")
    String start;

    @Option(names = "--end", description = "End time (ISO 8601)")
    String end;

    @Option(names = "--description", description = "Event description")
    String description;

    @Option(names = "--location", description = "Event location")
    String location;

    @Option(names = "--attendees", description = "Attendees (comma-separated emails)")
    String attendees;

    @Option(names = "--all-day", description = "Mark as all-day event")
    Boolean allDay;

    @Override
    public Integer call() throws Exception {
        List<String> attendeeList = attendees != null 
            ? Arrays.asList(attendees.split(","))
            : null;

        Event event = service.updateEvent(
            email, calendarId, eventId,
            summary, description, location,
            start, end,
            attendeeList, allDay
        );

        System.out.println("Updated: " + event.getId());
        return 0;
    }
}
