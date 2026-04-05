# Agent Guide for jgccli

## Project Overview

Java port of [gccli](https://github.com/badlogic/pi-skills/tree/main/gccli) (TypeScript). Keep feature parity with the original.

**Source:** https://github.com/maxandersen/jgccli

## Architecture

- **Framework:** Quarkus 3.x + Picocli (`quarkus-picocli` extension)
- **Java version:** 21+
- **Entry point:** `@TopCommand` on `GccliCommand.java`
- **DI:** `@ApplicationScoped` services, `@Inject` in commands
- **Google API:** `google-api-services-calendar` + `google-oauth-client-jetty`

## Package Structure

```
dk.xam.jgccli/
├── cli/                 # Command classes (one per subcommand)
│   ├── GccliCommand.java      # @TopCommand - main entry
│   ├── AccountsCommand.java   # accounts subcommand
│   ├── CalendarsCommand.java  # calendars subcommand
│   └── ...
├── service/             # Business logic & Google API calls
│   └── CalendarService.java
├── storage/             # Persistence
│   └── AccountStorage.java
├── oauth/               # OAuth flow handling
│   └── CalendarOAuthFlow.java
├── model/               # Records/POJOs
│   ├── CalendarAccount.java
│   ├── Credentials.java
│   └── OAuth2Credentials.java
└── exception/           # Custom exceptions
```

## Key Patterns

- **Commands:** Each subcommand is a separate class with `@Command` annotation
- **Output:** `OutputFormatter` handles `--json` flag (JSON vs TSV output)
- **Storage:** `AccountStorage` manages OAuth tokens in `~/.jgcli/` (shared)
- **Credentials:** Support multiple named credential sets

## Adding a New Command

1. Create `XxxCommand.java` in `cli/` package
2. Add `@Command(name = "xxx", description = "...")` annotation
3. Implement `Callable<Integer>` returning exit code
4. Register in `GccliCommand.java` `subcommands` array
5. Inject `CalendarService` for API calls

Example:
```java
@Command(name = "mycommand", description = "Does something")
public class MyCommand implements Callable<Integer> {
    @Inject
    CalendarService calendarService;
    
    @Parameters(index = "0", description = "Account email")
    String account;
    
    @Option(names = "--json", description = "Output as JSON")
    boolean json;
    
    @Override
    public Integer call() throws Exception {
        // Implementation
        return 0;
    }
}
```

## Common Pitfalls

- **Date/Time:** Use `DateTime` from Google API or RFC3339 strings
- **API errors:** Handle `GoogleJsonResponseException` for meaningful error messages
- **Calendar ID:** Use `"primary"` for the user's main calendar

## Build & Test

```bash
# Quick test with JBang (no build needed)
jbang src/main/java/jgccli.java --help
jbang src/main/java/jgccli.java accounts list

# Full Maven build
mvn package
./jgccli --help

# Run from jar
java -jar target/quarkus-app/quarkus-run.jar --help
```

## Config & Data

- `~/.jgcli/credentials.json` - OAuth client credentials (shared with jgmcli, jgdcli)
- `~/.jgcli/credentials-<name>.json` - Named credential sets (shared)
- `~/.jgcli/accounts-calendar.json` - Calendar account tokens

## Related Projects

- **jgmcli** - Gmail CLI (same patterns): https://github.com/maxandersen/jgmcli
- **jgdcli** - Google Drive CLI (same patterns): https://github.com/maxandersen/jgdcli
- **Original TypeScript:** https://github.com/badlogic/pi-skills/tree/main/gccli
