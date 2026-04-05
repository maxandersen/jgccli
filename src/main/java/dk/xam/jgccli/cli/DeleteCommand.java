package dk.xam.jgccli.cli;

import dk.xam.jgccli.service.CalendarService;
import jakarta.inject.Inject;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.util.concurrent.Callable;

@Command(name = "delete", description = "Delete an event")
public class DeleteCommand implements Callable<Integer> {

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
        service.deleteEvent(email, calendarId, eventId);
        System.out.println("Deleted");
        return 0;
    }
}
