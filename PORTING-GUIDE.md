# Porting Guide: Google API CLI Tools with Quarkus + Picocli

This guide outlines how to create minimal CLI tools for Google APIs, based on the jgccli implementation.

---

## Overview

**Stack:**
- Java 21
- Quarkus (for DI, JSON, native compilation)
- Picocli (CLI framework)
- Google API Client libraries

**Key features:**
- Multiple named OAuth credentials
- Browser + manual (browserless) OAuth flows
- Local storage in `~/.{toolname}/`
- Tab-separated output for easy parsing

---

## Step 1: Project Setup

### 1.1 Create Maven Project

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>

    <groupId>dk.xam</groupId>
    <artifactId>j{service}cli</artifactId>  <!-- e.g., jgmcli, jgdcli -->
    <version>0.1.0-SNAPSHOT</version>
    <packaging>jar</packaging>

    <properties>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <quarkus.platform.version>3.34.1</quarkus.platform.version>
        <compiler-plugin.version>3.15.0</compiler-plugin.version>
        <surefire-plugin.version>3.5.4</surefire-plugin.version>
        
        <!-- Google API versions - check for latest -->
        <google-api.version>2.4.0</google-api.version>
        <google-{service}.version>...</google-{service}.version>
        <google-oauth.version>1.35.0</google-oauth.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>io.quarkus.platform</groupId>
                <artifactId>quarkus-bom</artifactId>
                <version>${quarkus.platform.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <dependencies>
        <!-- Quarkus Picocli -->
        <dependency>
            <groupId>io.quarkus</groupId>
            <artifactId>quarkus-picocli</artifactId>
        </dependency>
        
        <!-- Quarkus Jackson -->
        <dependency>
            <groupId>io.quarkus</groupId>
            <artifactId>quarkus-jackson</artifactId>
        </dependency>

        <!-- Google APIs -->
        <dependency>
            <groupId>com.google.api-client</groupId>
            <artifactId>google-api-client</artifactId>
            <version>${google-api.version}</version>
        </dependency>
        <dependency>
            <groupId>com.google.apis</groupId>
            <artifactId>google-api-services-{service}</artifactId>
            <version>${google-{service}.version}</version>
        </dependency>
        <dependency>
            <groupId>com.google.oauth-client</groupId>
            <artifactId>google-oauth-client-jetty</artifactId>
            <version>${google-oauth.version}</version>
        </dependency>

        <!-- Testing -->
        <dependency>
            <groupId>io.quarkus</groupId>
            <artifactId>quarkus-junit5</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <!-- Build plugins same as jgccli -->
</project>
```

### 1.2 Find the Google API Library

Visit [Google API Java Client Services](https://github.com/googleapis/google-api-java-client-services) or search Maven Central:

| Service | Artifact | Scopes |
|---------|----------|--------|
| Calendar | `google-api-services-calendar` | `CalendarScopes.CALENDAR` |
| Gmail | `google-api-services-gmail` | `GmailScopes.GMAIL_MODIFY` or `GMAIL_READONLY` |
| Drive | `google-api-services-drive` | `DriveScopes.DRIVE` or `DRIVE_FILE` |
| Sheets | `google-api-services-sheets` | `SheetsScopes.SPREADSHEETS` |
| Tasks | `google-api-services-tasks` | `TasksScopes.TASKS` |
| People | `google-api-services-people` | `PeopleServiceScopes.CONTACTS` |

### 1.3 Directory Structure

```
src/main/java/dk/xam/{tool}/
├── cli/
│   ├── {Tool}Command.java        # @TopCommand with subcommands
│   ├── AccountsCommand.java      # Copy from jgccli (reusable)
│   ├── OutputFormatter.java      # Copy from jgccli (reusable)
│   └── {Feature}Command.java     # Service-specific commands
├── model/
│   ├── CalendarAccount.java      # Rename to {Service}Account
│   ├── Credentials.java          # Copy from jgccli (reusable)
│   ├── OAuth2Credentials.java    # Copy from jgccli (reusable)
│   └── {custom models}           # Service-specific models
├── service/
│   └── {Service}Service.java     # Main service class
├── storage/
│   └── AccountStorage.java       # Copy from jgccli, update CONFIG_DIR
├── oauth/
│   └── {Service}OAuthFlow.java   # Copy from jgccli, update SCOPES
└── exception/
    ├── {Tool}Exception.java      # Base exception
    ├── AuthorizationException.java
    └── AccountNotFoundException.java
```

---

## Step 2: Reusable Components (Copy from jgccli)

These can be copied with minimal changes:

### 2.1 Credentials & OAuth (update scopes only)

```java
// oauth/{Service}OAuthFlow.java
private static final List<String> SCOPES = List.of(
    GmailScopes.GMAIL_MODIFY  // Change per service
);
```

### 2.2 Storage (update CONFIG_DIR only)

```java
// storage/AccountStorage.java
private static final Path CONFIG_DIR = Paths.get(
    System.getProperty("user.home"), ".j{service}cli"
);
```

### 2.3 Models (copy as-is)

- `Credentials.java` - No changes needed
- `OAuth2Credentials.java` - No changes needed
- `{Service}Account.java` - Rename from CalendarAccount

### 2.4 Exceptions (rename only)

- `{Tool}Exception.java` - Rename from GccliException
- `AuthorizationException.java` - Update import
- `AccountNotFoundException.java` - Update import

### 2.5 CLI helpers (copy as-is)

- `OutputFormatter.java` - No changes needed
- `AccountsCommand.java` - Update imports, tool name in messages

---

## Step 3: Service-Specific Implementation

### 3.1 Main Service Class

```java
@ApplicationScoped
public class {Service}Service {
    
    private static final String APPLICATION_NAME = "j{service}cli";
    private static final GsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    @Inject
    AccountStorage accountStorage;

    private final Map<String, {ServiceClient}> clients = new ConcurrentHashMap<>();
    
    // Account management methods (same pattern as jgccli)
    public void addAccount(String email, String credentialsName, boolean manual, boolean force) { ... }
    public boolean deleteAccount(String email) { ... }
    public List<{Service}Account> listAccounts() { ... }
    
    // Credentials methods (same pattern as jgccli)
    public void setCredentials(String name, String clientId, String clientSecret) { ... }
    public Credentials getCredentials(String name) { ... }
    public CredentialsStore getAllCredentials() { ... }
    public boolean removeCredentials(String name) { ... }

    // Service client factory
    @SuppressWarnings("deprecation")
    private {ServiceClient} getClient(String email) {
        return clients.computeIfAbsent(email, e -> {
            {Service}Account account = accountStorage.getAccount(e);
            if (account == null) {
                throw new AccountNotFoundException(e);
            }

            GoogleCredential credential = new GoogleCredential.Builder()
                .setTransport(getTransport())
                .setJsonFactory(JSON_FACTORY)
                .setClientSecrets(account.oauth2().clientId(), account.oauth2().clientSecret())
                .build()
                .setRefreshToken(account.oauth2().refreshToken());

            return new {ServiceClient}.Builder(getTransport(), JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
        });
    }

    // Service-specific methods
    public List<{Item}> list{Items}(String email, ...) throws IOException { ... }
    public {Item} get{Item}(String email, String id) throws IOException { ... }
    public {Item} create{Item}(String email, ...) throws IOException { ... }
    public {Item} update{Item}(String email, String id, ...) throws IOException { ... }
    public void delete{Item}(String email, String id) throws IOException { ... }
}
```

### 3.2 CLI Commands

```java
@TopCommand
@Command(name = "j{service}cli",
         mixinStandardHelpOptions = true,
         version = "j{service}cli 0.1.0",
         description = "Minimal Google {Service} CLI",
         subcommands = {
             AccountsCommand.class,
             List{Items}Command.class,
             Get{Item}Command.class,
             Create{Item}Command.class,
             // ...
         },
         footer = { /* Usage examples */ })
public class {Tool}Command implements IExecutionExceptionHandler {
    @Override
    public int handleExecutionException(Exception ex, CommandLine cmd, ParseResult parseResult) {
        // Same pattern as jgccli
    }
}
```

### 3.3 Command Pattern

```java
@Command(name = "{action}", description = "{Description}")
public class {Action}Command implements Callable<Integer> {

    @Inject
    {Service}Service service;

    @Parameters(index = "0", description = "Email account")
    String email;

    @Parameters(index = "1", description = "{Resource} ID", arity = "0..1")
    String resourceId;

    @Option(names = "--option", description = "...")
    String option;

    @Override
    public Integer call() throws Exception {
        // Call service, format output
        var result = service.doSomething(email, resourceId, option);
        
        // Use OutputFormatter for consistent output
        List<String[]> rows = result.stream()
            .map(item -> new String[]{item.getId(), item.getName(), ...})
            .toList();
        OutputFormatter.printTable(new String[]{"ID", "NAME", ...}, rows);
        
        return 0;
    }
}
```

---

## Step 4: Configuration

### 4.1 application.properties

```properties
# Quarkus configuration for j{service}cli
quarkus.banner.enabled=false
quarkus.log.level=WARN
quarkus.log.category."dk.xam.j{service}cli".level=INFO
quarkus.devservices.enabled=false
```

### 4.2 Wrapper Script

```bash
#!/usr/bin/env bash
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JAR="$SCRIPT_DIR/target/quarkus-app/quarkus-run.jar"

if [ ! -f "$JAR" ]; then
    echo "Error: JAR not found. Run 'mvn package' first." >&2
    exit 1
fi

cd "$SCRIPT_DIR"
exec java -jar "$JAR" "$@"
```

---

## Step 5: Common Patterns

### 5.1 Pagination

```java
public record SearchResult<T>(List<T> items, String nextPageToken) {}

// In command:
@Option(names = "--max", description = "Max results")
Integer maxResults;

@Option(names = "--page", description = "Page token")
String pageToken;

// Print next page hint:
if (result.nextPageToken() != null) {
    System.out.println();
    System.out.println("# Next page: --page " + result.nextPageToken());
}
```

### 5.2 Date/Time Handling

```java
// Use ISO 8601 strings for CLI input
@Option(names = "--after", description = "After date (ISO 8601)")
String after;

// Convert to Google DateTime
if (after != null) {
    request.setTimeMin(new DateTime(after));
}
```

### 5.3 Output Formats

```java
// Tab-separated for parsing
OutputFormatter.printTable(headers, rows);

// Key-value for details
OutputFormatter.printKeyValue("ID", item.getId());
OutputFormatter.printKeyValue("Name", item.getName());
```

### 5.4 Error Handling

```java
// Wrap Google API exceptions
try {
    return client.items().get(id).execute();
} catch (GoogleJsonResponseException e) {
    if (e.getStatusCode() == 404) {
        throw new {Tool}Exception("Item not found: " + id);
    }
    throw new {Tool}Exception("API error: " + e.getDetails().getMessage(), e);
}
```

---

## Step 6: Testing Checklist

```bash
# Build
mvn clean package -DskipTests

# Test credentials management
./j{service}cli accounts credentials ~/creds.json
./j{service}cli accounts credentials --list
./j{service}cli accounts credentials ~/other.json --name work
./j{service}cli accounts credentials --remove work

# Test account management
./j{service}cli accounts add you@gmail.com
./j{service}cli accounts add you@gmail.com --force  # re-auth
./j{service}cli accounts add other@gmail.com --credentials work
./j{service}cli accounts list
./j{service}cli accounts remove other@gmail.com

# Test service operations
./j{service}cli {list-command} you@gmail.com
./j{service}cli {get-command} you@gmail.com {id}
./j{service}cli {create-command} you@gmail.com --option value
./j{service}cli {delete-command} you@gmail.com {id}
```

---

## Quick Reference: Google API Services

| Tool | Service | Maven Artifact | Primary Scope |
|------|---------|----------------|---------------|
| jgccli | Calendar | `google-api-services-calendar` | `CalendarScopes.CALENDAR` |
| jgmcli | Gmail | `google-api-services-gmail` | `GmailScopes.GMAIL_MODIFY` |
| jgdcli | Drive | `google-api-services-drive` | `DriveScopes.DRIVE` |
| jgscli | Sheets | `google-api-services-sheets` | `SheetsScopes.SPREADSHEETS` |
| jgtcli | Tasks | `google-api-services-tasks` | `TasksScopes.TASKS` |
| jgpcli | People/Contacts | `google-api-services-people` | `PeopleServiceScopes.CONTACTS` |

---

## Files to Copy (Minimal Changes)

| File | Changes Needed |
|------|----------------|
| `model/Credentials.java` | Package only |
| `model/OAuth2Credentials.java` | Package only |
| `model/{Service}Account.java` | Package + rename |
| `storage/AccountStorage.java` | Package + CONFIG_DIR |
| `oauth/{Service}OAuthFlow.java` | Package + SCOPES |
| `exception/*` | Package + base class rename |
| `cli/OutputFormatter.java` | Package only |
| `cli/AccountsCommand.java` | Package + messages |
| `application.properties` | Tool name + package |
| `pom.xml` | Artifact + Google API dependency |
| Wrapper script | Rename |

---

## Example: Porting to Gmail CLI (jgmcli)

1. Copy jgccli project
2. Rename: `jgccli` → `jgmcli`, `gccli` → `gmcli`
3. Update `pom.xml`:
   - `google-api-services-calendar` → `google-api-services-gmail`
4. Update `AccountStorage.java`:
   - `.jgccli` → `.jgmcli`
5. Update `GmailOAuthFlow.java`:
   - `CalendarScopes.CALENDAR` → `GmailScopes.GMAIL_MODIFY`
6. Create `GmailService.java`:
   - Replace Calendar API calls with Gmail API calls
7. Create Gmail-specific commands:
   - `ThreadsCommand`, `MessagesCommand`, `SendCommand`, `LabelsCommand`, etc.

---

## Tips

1. **Start with list/get operations** - Easiest to implement and test
2. **Use `--dry-run` for mutations** - Add option to preview without executing
3. **Keep output parseable** - Tab-separated, one item per line
4. **Match existing CLI conventions** - If porting from another language, keep same command structure
5. **Reuse credentials** - All tools can share `~/.jgccli/credentials.json` if scopes are compatible, or each tool manages its own
