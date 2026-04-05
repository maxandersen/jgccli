# jgccli

Minimal Google Calendar CLI for listing calendars, managing events, and checking availability.

## Why Java?

This is a Java port of [gccli](../gccli/) (TypeScript/Node.js). Both implementations are functionally equivalent.

Use **jgccli** if you prefer Java/JBang and don't want to install Node.js/npm.
Use **gccli** if you prefer Node.js/TypeScript.

The command structure is identical, making it easy to switch between them.

## Agent Skills

For AI coding agents (Claude Code, etc.), install the skill from [maxandersen/skills](https://github.com/maxandersen/skills):
```bash
npx skills install maxandersen/skills/jgccli
```

## Run with JBang (no build required)

```bash
# Run directly from GitHub
jbang https://github.com/maxandersen/jgccli/blob/main/src/main/java/jgccli.java --help

# Install as 'jgccli' command
jbang app install https://github.com/maxandersen/jgccli/blob/main/src/main/java/jgccli.java
jgccli --help

# Or clone and run locally
jbang src/main/java/jgccli.java --help
```

## Build with Maven

```bash
# JVM build
mvn package

# Run
./jgccli --help
# or: java -jar target/quarkus-app/quarkus-run.jar --help

# Native build (requires GraalVM)
mvn package -Pnative
./target/jgccli --help
```

## Setup

Before adding an account, you need OAuth2 credentials from Google Cloud Console:

1. [Create a new project](https://console.cloud.google.com/projectcreate) (or select existing)
2. [Enable the Google Calendar API](https://console.cloud.google.com/apis/api/calendar-json.googleapis.com)
3. [Set app name](https://console.cloud.google.com/auth/branding) in OAuth branding
4. [Add test users](https://console.cloud.google.com/auth/audience) (all Gmail addresses you want to use with jgccli)
5. [Create OAuth client](https://console.cloud.google.com/auth/clients):
   - Click "Create Client"
   - Application type: "Desktop app"
   - Download the JSON file

Then:

```bash
jgccli accounts credentials ~/path/to/credentials.json
jgccli accounts add you@gmail.com
```

### Shared Credentials

Credentials are stored in `~/.jgcli/` and **shared across jgccli, jgmcli, and jgdcli**. Set up once, use with all three tools:

```bash
# Set credentials once (from any tool)
jgccli accounts credentials ~/path/to/credentials.json

# All tools can now use them
jgccli accounts add you@gmail.com   # Calendar
jgmcli accounts add you@gmail.com   # Gmail  
jgdcli accounts add you@gmail.com   # Drive
```

If you need separate credentials (e.g., different Google Cloud projects), use named credentials:

```bash
# Set up named credentials
jgccli accounts credentials ~/work-creds.json --name work
jgccli accounts credentials ~/personal-creds.json --name personal

# Use specific credentials per account
jgccli accounts add work@company.com --credentials work
jgccli accounts add me@gmail.com --credentials personal
```

## Usage

```
jgccli accounts <action>                Account management
jgccli <command> <email> [options]      Calendar operations
```

## Commands

### accounts

**Credentials management:**
```bash
jgccli accounts credentials <file.json>              # Set default credentials
jgccli accounts credentials <file.json> --name work  # Set named credentials
jgccli accounts credentials --list                   # List all credentials
jgccli accounts credentials --remove <name>          # Remove named credentials
```

**Account management:**
```bash
jgccli accounts list                           # List accounts (shows credentials used)
jgccli accounts add <email>                    # Add with default credentials
jgccli accounts add <email> --credentials work # Add with named credentials
jgccli accounts add <email> --manual           # Browserless OAuth flow
jgccli accounts add <email> --force            # Re-authorize existing account
jgccli accounts remove <email>                 # Remove account
```

### calendars

List all calendars for an account.

```bash
jgccli calendars <email>
```

Returns: ID, name, access role.

### events

List events from a calendar.

```bash
jgccli events <email> <calendarId> [options]
```

Options:
- `--from <datetime>` - Start time (ISO 8601, default: now)
- `--to <datetime>` - End time (ISO 8601, default: 1 week from now)
- `--max <n>` - Max results (default: 10)
- `--page <token>` - Page token for pagination
- `--query <q>` - Free text search

Examples:
```bash
jgccli events you@gmail.com primary
jgccli events you@gmail.com primary --from 2024-01-01T00:00:00Z --max 50
jgccli events you@gmail.com primary --query "meeting"
```

### event

Get details for a specific event.

```bash
jgccli event <email> <calendarId> <eventId>
```

### create

Create a new event.

```bash
jgccli create <email> <calendarId> --summary <s> --start <dt> --end <dt> [options]
```

Options:
- `--summary <s>` - Event title (required)
- `--start <datetime>` - Start time (required, ISO 8601)
- `--end <datetime>` - End time (required, ISO 8601)
- `--description <d>` - Event description
- `--location <l>` - Event location
- `--attendees <emails>` - Required attendees (comma-separated)
- `--optional-attendees <emails>` - Optional attendees (comma-separated)
- `--all-day` - Create all-day event (use YYYY-MM-DD for start/end)

Examples:
```bash
jgccli create you@gmail.com primary --summary "Meeting" --start 2024-01-15T10:00:00 --end 2024-01-15T11:00:00
jgccli create you@gmail.com primary --summary "Vacation" --start 2024-01-20 --end 2024-01-25 --all-day
jgccli create you@gmail.com primary --summary "Team Sync" --start 2024-01-15T14:00:00 --end 2024-01-15T15:00:00 \
    --attendees a@x.com,b@x.com --optional-attendees c@x.com
```

### update

Update an existing event.

```bash
jgccli update <email> <calendarId> <eventId> [options]
```

Options: same as create (all optional).

Example:
```bash
jgccli update you@gmail.com primary abc123 --summary "Updated Meeting" --location "Room 2"
```

### delete

Delete an event.

```bash
jgccli delete <email> <calendarId> <eventId>
```

### freebusy

Check free/busy status for calendars.

```bash
jgccli freebusy <email> <calendarIds> --from <dt> --to <dt>
```

Calendar IDs are comma-separated.

Example:
```bash
jgccli freebusy you@gmail.com primary,work@group.calendar.google.com --from 2024-01-15T00:00:00Z --to 2024-01-16T00:00:00Z
```

### acl

List access control rules for a calendar.

```bash
jgccli acl <email> <calendarId>
```

Returns: scope type, scope value, role.

Example:
```bash
jgccli acl you@gmail.com primary
```

## Data Storage

All data is stored in `~/.jgcli/` (shared with jgmcli, jgdcli):
- `credentials.json` - OAuth client credentials (shared)
- `accounts-calendar.json` - Calendar account tokens

## License

MIT
