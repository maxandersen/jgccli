package dk.xam.jgccli.cli;

import dk.xam.jgccli.exception.GccliException;
import io.quarkus.picocli.runtime.annotations.TopCommand;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.IExecutionExceptionHandler;
import picocli.CommandLine.ParseResult;

@TopCommand
@Command(name = "jgccli",
         mixinStandardHelpOptions = true,
         version = "jgccli 0.1.0",
         description = "Minimal Google Calendar CLI (Java)",
         subcommands = {
             AccountsCommand.class,
             CalendarsCommand.class,
             AclCommand.class,
             EventsCommand.class,
             EventCommand.class,
             CreateCommand.class,
             UpdateCommand.class,
             DeleteCommand.class,
             FreeBusyCommand.class
         },
         footer = {
             "",
             "USAGE EXAMPLES",
             "",
             "  Credentials management:",
             "    jgccli accounts credentials ~/creds.json              # Set default",
             "    jgccli accounts credentials ~/work.json --name work   # Set named",
             "    jgccli accounts credentials --list                    # List all",
             "    jgccli accounts credentials --remove work             # Remove",
             "",
             "  Account management:",
             "    jgccli accounts add you@gmail.com                     # Use default creds",
             "    jgccli accounts add you@work.com --credentials work   # Use named creds",
             "    jgccli accounts add you@gmail.com --manual            # Browserless",
             "    jgccli accounts add you@gmail.com --force             # Re-authorize",
             "    jgccli accounts list",
             "    jgccli accounts remove you@gmail.com",
             "",
             "  Calendar operations:",
             "    jgccli calendars you@gmail.com",
             "    jgccli events you@gmail.com primary",
             "    jgccli events you@gmail.com primary --from 2024-01-01T00:00:00Z --max 50",
             "    jgccli event you@gmail.com primary eventId123",
             "    jgccli create you@gmail.com primary --summary \"Meeting\" --start 2024-01-15T10:00:00 --end 2024-01-15T11:00:00",
             "    jgccli update you@gmail.com primary eventId123 --summary \"Updated Meeting\"",
             "    jgccli delete you@gmail.com primary eventId123",
             "    jgccli freebusy you@gmail.com primary,other@group.calendar.google.com --from 2024-01-15T00:00:00Z --to 2024-01-16T00:00:00Z",
             "    jgccli acl you@gmail.com primary",
             "",
             "DATA STORAGE",
             "",
             "  ~/.jgccli/credentials.json   OAuth client credentials",
             "  ~/.jgccli/accounts.json      Account tokens"
         })
public class GccliCommand implements IExecutionExceptionHandler {

    @Override
    public int handleExecutionException(Exception ex, CommandLine cmd, ParseResult parseResult) {
        String message = ex.getMessage();
        if (ex instanceof GccliException) {
            cmd.getErr().println("Error: " + message);
        } else {
            cmd.getErr().println("Error: " + (message != null ? message : ex.getClass().getSimpleName()));
        }
        return 1;
    }
}
