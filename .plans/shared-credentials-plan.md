# Shared Credentials Plan

## Goal
Share OAuth client credentials across jgccli, jgmcli, jgdcli so users only configure once.

## Directory Structure

**New shared location: `~/.jgcli/`**

```
~/.jgcli/
├── credentials.json           # Default OAuth client (shared)
├── credentials-work.json      # Named credential set (shared)
├── credentials-personal.json  # Named credential set (shared)
├── accounts-calendar.json     # Calendar account tokens (jgccli)
├── accounts-gmail.json        # Gmail account tokens (jgmcli)
├── accounts-drive.json        # Drive account tokens (jgdcli)
├── attachments/               # Gmail attachments (jgmcli)
└── downloads/                 # Drive downloads (jgdcli)
```

## What's Shared vs Tool-Specific

| Item | Shared? | Notes |
|------|---------|-------|
| OAuth client credentials | ✅ Yes | Same Google Cloud project for all tools |
| Named credential sets | ✅ Yes | `credentials-<name>.json` |
| Account tokens | ❌ No | Different scopes per API, separate files |
| Attachments/downloads | ❌ No | Tool-specific subdirs |

## Implementation Changes

### 1. AccountStorage class (all three tools)

Change config directory from tool-specific to shared:

```java
// Before
private static final Path CONFIG_DIR = Paths.get(System.getProperty("user.home"), ".jgccli");

// After  
private static final Path CONFIG_DIR = Paths.get(System.getProperty("user.home"), ".jgcli");
```

Change accounts file to tool-specific name:

```java
// Before (jgccli)
private static final String ACCOUNTS_FILE = "accounts.json";

// After (jgccli)
private static final String ACCOUNTS_FILE = "accounts-calendar.json";

// jgmcli
private static final String ACCOUNTS_FILE = "accounts-gmail.json";

// jgdcli
private static final String ACCOUNTS_FILE = "accounts-drive.json";
```

### 2. Credentials loading (unchanged logic)

```java
// Default credentials
CONFIG_DIR.resolve("credentials.json")

// Named credentials  
CONFIG_DIR.resolve("credentials-" + name + ".json")
```

### 3. Tool-specific subdirs

```java
// jgmcli attachments
CONFIG_DIR.resolve("attachments")

// jgdcli downloads
CONFIG_DIR.resolve("downloads")
```

## Files to Modify

| Project | File | Changes |
|---------|------|---------|
| jgccli | `AccountStorage.java` | CONFIG_DIR → `~/.jgcli`, ACCOUNTS_FILE → `accounts-calendar.json` |
| jgmcli | `AccountStorage.java` | CONFIG_DIR → `~/.jgcli`, ACCOUNTS_FILE → `accounts-gmail.json` |
| jgmcli | `GmailService.java` | Attachments dir → `~/.jgcli/attachments` |
| jgdcli | `AccountStorage.java` | CONFIG_DIR → `~/.jgcli`, ACCOUNTS_FILE → `accounts-drive.json` |
| jgdcli | `DriveService.java` | Downloads dir → `~/.jgcli/downloads` |

## Migration (for existing users)

Before implementing, migrate existing config:

```bash
# Create shared directory
mkdir -p ~/.jgcli

# Migrate credentials (pick one source, they should be identical)
cp ~/.jgccli/credentials.json ~/.jgcli/credentials.json 2>/dev/null || \
cp ~/.jgmcli/credentials.json ~/.jgcli/credentials.json 2>/dev/null || \
cp ~/.jgdcli/credentials.json ~/.jgcli/credentials.json 2>/dev/null

# Copy any named credentials
cp ~/.jgccli/credentials-*.json ~/.jgcli/ 2>/dev/null
cp ~/.jgmcli/credentials-*.json ~/.jgcli/ 2>/dev/null
cp ~/.jgdcli/credentials-*.json ~/.jgcli/ 2>/dev/null

# Migrate account tokens (rename to new convention)
cp ~/.jgccli/accounts.json ~/.jgcli/accounts-calendar.json 2>/dev/null
cp ~/.jgmcli/accounts.json ~/.jgcli/accounts-gmail.json 2>/dev/null
cp ~/.jgdcli/accounts.json ~/.jgcli/accounts-drive.json 2>/dev/null

# Migrate attachments/downloads
cp -r ~/.jgmcli/attachments ~/.jgcli/ 2>/dev/null
cp -r ~/.jgdcli/downloads ~/.jgcli/ 2>/dev/null
```

## User Experience After Implementation

```bash
# One-time setup (once for all three tools)
jgccli accounts credentials ~/path/to/credentials.json

# Add accounts (still per-tool due to different scopes)
jgccli accounts add me@gmail.com
jgmcli accounts add me@gmail.com
jgdcli accounts add me@gmail.com

# All tools now work
jgccli calendars me@gmail.com
jgmcli search me@gmail.com "in:inbox"
jgdcli ls me@gmail.com
```

## Documentation Updates

Update README.md and SKILL.md for all three tools:
- Change `~/.jgccli/` references to `~/.jgcli/`
- Note that credentials are shared across jgccli/jgmcli/jgdcli
- Update "Data Storage" sections

## Testing

1. Run migration script
2. Verify each tool can read shared credentials
3. Verify each tool reads/writes its own accounts file
4. Verify attachments/downloads go to correct subdirs
5. Test named credentials work across tools
