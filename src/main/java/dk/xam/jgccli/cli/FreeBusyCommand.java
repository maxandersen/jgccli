package dk.xam.jgccli.cli;

import com.google.api.services.calendar.model.TimePeriod;
import dk.xam.jgccli.service.CalendarService;
import jakarta.inject.Inject;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

@Command(name = "freebusy", description = "Check free/busy status for calendars")
public class FreeBusyCommand implements Callable<Integer> {

    @Inject
    CalendarService service;

    @Parameters(index = "0", description = "Email account")
    String email;

    @Parameters(index = "1", description = "Calendar IDs (comma-separated)")
    String calendarIds;

    @Option(names = "--from", required = true, description = "Start time (ISO 8601)")
    String from;

    @Option(names = "--to", required = true, description = "End time (ISO 8601)")
    String to;

    @Override
    public Integer call() throws Exception {
        List<String> ids = Arrays.asList(calendarIds.split(","));
        Map<String, List<TimePeriod>> result = service.getFreeBusy(email, ids, from, to);

        for (Map.Entry<String, List<TimePeriod>> entry : result.entrySet()) {
            System.out.println(entry.getKey() + ":");
            List<TimePeriod> busy = entry.getValue();
            if (busy.isEmpty()) {
                System.out.println("  (free)");
            } else {
                for (TimePeriod period : busy) {
                    System.out.println("  " + period.getStart() + " - " + period.getEnd());
                }
            }
        }
        return 0;
    }
}
