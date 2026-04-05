package dk.xam.jgccli.cli;

import com.google.api.services.calendar.model.AclRule;
import dk.xam.jgccli.service.CalendarService;
import jakarta.inject.Inject;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import static dk.xam.jgccli.cli.OutputFormatter.*;

@Command(name = "acl", description = "List access control rules for a calendar")
public class AclCommand implements Callable<Integer> {

    @Inject
    CalendarService service;

    @Parameters(index = "0", description = "Email account")
    String email;

    @Parameters(index = "1", description = "Calendar ID")
    String calendarId;

    @Override
    public Integer call() throws Exception {
        List<AclRule> rules = service.getCalendarAcl(email, calendarId);

        if (rules.isEmpty()) {
            System.out.println("No ACL rules");
        } else {
            List<String[]> rows = new ArrayList<>();
            for (AclRule rule : rules) {
                String scopeType = rule.getScope() != null ? orEmpty(rule.getScope().getType()) : "";
                String scopeValue = rule.getScope() != null ? orEmpty(rule.getScope().getValue()) : "";
                rows.add(new String[]{
                    scopeType,
                    scopeValue,
                    orEmpty(rule.getRole())
                });
            }
            printTable(new String[]{"SCOPE_TYPE", "SCOPE_VALUE", "ROLE"}, rows);
        }
        return 0;
    }
}
