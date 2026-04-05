package dk.xam.jgccli.cli;

import com.google.api.services.calendar.model.CalendarListEntry;
import dk.xam.jgccli.service.CalendarService;
import jakarta.inject.Inject;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import static dk.xam.jgccli.cli.OutputFormatter.*;

@Command(name = "calendars", description = "List all calendars")
public class CalendarsCommand implements Callable<Integer> {

    @Inject
    CalendarService service;

    @Parameters(index = "0", description = "Email account")
    String email;

    @Override
    public Integer call() throws Exception {
        List<CalendarListEntry> calendars = service.listCalendars(email);

        if (calendars.isEmpty()) {
            System.out.println("No calendars");
        } else {
            List<String[]> rows = new ArrayList<>();
            for (CalendarListEntry c : calendars) {
                rows.add(new String[]{
                    orEmpty(c.getId()),
                    orEmpty(c.getSummary()),
                    orEmpty(c.getAccessRole())
                });
            }
            printTable(new String[]{"ID", "NAME", "ROLE"}, rows);
        }
        return 0;
    }
}
