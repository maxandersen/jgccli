# Multi-Credentials Support Plan

## Overview

Add support for multiple named OAuth credentials while maintaining backward compatibility with the existing single-credential workflow.

**Note:** Using `jgccli` command and `~/.jgccli/` storage to avoid conflicts with TypeScript version.

---

## Current Behavior

```bash
# Single credentials file
jgccli accounts credentials ~/path/to/credentials.json
jgccli accounts add you@gmail.com
```

**Storage:** `~/.jgccli/credentials.json`
```json
{
  "clientId": "...",
  "clientSecret": "..."
}
```

---

## Proposed Behavior

### 1. Named Credentials Management

```bash
# Set default credentials (backward compatible)
jgccli accounts credentials ~/path/to/credentials.json

# Set named credentials
jgccli accounts credentials ~/path/to/work-creds.json --name work
jgccli accounts credentials ~/path/to/personal-creds.json --name personal

# List all credentials
jgccli accounts credentials --list

# Remove named credentials
jgccli accounts credentials --remove work
```

### 2. Using Named Credentials When Adding Accounts

```bash
# Use default credentials (backward compatible)
jgccli accounts add you@gmail.com

# Use named credentials
jgccli accounts add work@company.com --credentials work
jgccli accounts add personal@gmail.com --credentials personal
```

### 3. Updating Account Credentials

```bash
# Re-authorize with different credentials
jgccli accounts add existing@gmail.com --credentials newcreds --force
```

---

## Storage Changes

### Credentials File

**File:** `~/.jgccli/credentials.json`
```json
{
  "default": {
    "clientId": "...",
    "clientSecret": "..."
  },
  "work": {
    "clientId": "...",
    "clientSecret": "..."
  },
  "personal": {
    "clientId": "...",
    "clientSecret": "..."
  }
}
```

**Migration:** If old format detected (flat `clientId`/`clientSecret`), auto-migrate to `default` entry.

### Accounts File

**File:** `~/.jgccli/accounts.json`

Add `credentialsName` field to track which credentials were used:

```json
[
  {
    "email": "me@gmail.com",
    "credentialsName": "personal",
    "oauth2": {
      "clientId": "...",
      "clientSecret": "...",
      "refreshToken": "..."
    }
  },
  {
    "email": "other@gmail.com",
    "credentialsName": null,
    "oauth2": { ... }
  }
]
```

**Note:** `credentialsName` is `null` or absent for default credentials.

---

## Implementation Steps

### Step 1: Update Models

```java
// New: CredentialsStore to hold multiple named credentials
public record CredentialsStore(
    Map<String, Credentials> credentials
) {
    public Credentials get(String name) {
        return credentials.get(name != null ? name : "default");
    }
    
    public Credentials getDefault() {
        return credentials.get("default");
    }
}

// Update CalendarAccount to track credentials name
public record CalendarAccount(
    String email,
    String credentialsName,  // null = default
    OAuth2Credentials oauth2
) {}
```

### Step 2: Update `AccountStorage`

```java
public class AccountStorage {
    // Change from single Credentials to CredentialsStore
    
    // New methods:
    void setCredentials(String name, String clientId, String clientSecret);
    Credentials getCredentials(String name);  // null = default
    Map<String, Credentials> getAllCredentials();
    boolean removeCredentials(String name);
    
    // Migration: detect old format and convert
    private void migrateCredentialsIfNeeded();
}
```

### Step 3: Update `CalendarService`

```java
public class CalendarService {
    // Update addAccount to accept optional credentials name
    public void addAccount(String email, String credentialsName, boolean manual);
    
    // Lookup credentials by name, fall back to default
}
```

### Step 4: Update CLI Commands

#### `AccountsCommand.CredentialsCommand`

```java
@Command(name = "credentials", description = "Manage OAuth credentials")
static class CredentialsCommand implements Callable<Integer> {
    
    @Parameters(index = "0", arity = "0..1", description = "Path to credentials.json")
    File credentialsFile;
    
    @Option(names = "--name", description = "Credential name (default: 'default')")
    String name = "default";
    
    @Option(names = "--list", description = "List all credentials")
    boolean list;
    
    @Option(names = "--remove", description = "Remove named credentials")
    String remove;
    
    @Override
    public Integer call() {
        if (list) {
            // Print all credential names
        } else if (remove != null) {
            // Remove named credentials
        } else if (credentialsFile != null) {
            // Save credentials with name
        } else {
            // Show usage
        }
    }
}
```

#### `AccountsCommand.AddCommand`

```java
@Command(name = "add", description = "Add account")
static class AddCommand implements Callable<Integer> {
    
    @Parameters(index = "0", description = "Email address")
    String email;
    
    @Option(names = "--manual", description = "Browserless OAuth flow")
    boolean manual;
    
    @Option(names = "--credentials", description = "Named credentials to use")
    String credentialsName;
    
    @Option(names = "--force", description = "Re-authorize existing account")
    boolean force;
}
```

#### `AccountsCommand.ListCommand`

```java
@Command(name = "list", description = "List configured accounts")
static class ListCommand implements Callable<Integer> {
    @Override
    public Integer call() {
        for (CalendarAccount account : accounts) {
            if (account.credentialsName() != null) {
                System.out.println(account.email() + "\t(" + account.credentialsName() + ")");
            } else {
                System.out.println(account.email());
            }
        }
    }
}
```

### Step 5: Update Help Text

Update `GccliCommand` footer with new examples:

```
Credentials management:
  jgccli accounts credentials ~/creds.json                    # Set default
  jgccli accounts credentials ~/work.json --name work         # Set named
  jgccli accounts credentials --list                          # List all
  jgccli accounts credentials --remove work                   # Remove named

Using named credentials:
  jgccli accounts add work@company.com --credentials work
```

---

## Migration Strategy

1. On startup, check if `credentials.json` has old format (flat structure)
2. If old format detected:
   - Read `clientId` and `clientSecret`
   - Write new format with `"default"` entry
   - Log: "Migrated credentials to new format"
3. Existing accounts continue to work (they store their own clientId/clientSecret)

---

## Files to Modify

| File | Changes |
|------|---------|
| `CalendarAccount.java` | Add `credentialsName` field |
| `Credentials.java` | Add `CredentialsStore` record |
| `AccountStorage.java` | Multi-credential storage + migration |
| `CalendarService.java` | Accept credentials name in `addAccount()`, store in account |
| `AccountsCommand.java` | New options for credentials + add commands |
| `GccliCommand.java` | Update help text |
| `README.md` | Document new features |

---

## Backward Compatibility

| Scenario | Behavior |
|----------|----------|
| Old `credentials.json` format | Auto-migrated to `default` |
| `gccli accounts credentials file.json` (no --name) | Saves as `default` |
| `gccli accounts add email` (no --credentials) | Uses `default` |
| Existing accounts | Continue working (have embedded credentials) |

---

## Example Workflow

```bash
# Setup multiple credentials
jgccli accounts credentials ~/personal-oauth.json --name personal
jgccli accounts credentials ~/work-oauth.json --name work

# Verify
jgccli accounts credentials --list
# Output:
# default   (not set)
# personal  ✓
# work      ✓

# Add accounts with specific credentials
jgccli accounts add me@gmail.com --credentials personal
jgccli accounts add me@company.com --credentials work
jgccli accounts add other@gmail.com   # uses default

# List accounts - shows credentials name if not default
jgccli accounts list
# Output:
# me@gmail.com       (personal)
# me@company.com     (work)
# other@gmail.com
```

---

## Checklist

- [x] Update `Credentials.java` with `CredentialsStore`
- [x] Update `AccountStorage.java` with multi-credential support
- [x] Add migration logic for old format
- [x] Update `CalendarService.addAccount()` signature
- [x] Update `CredentialsCommand` with --name, --list, --remove
- [x] Update `AddCommand` with --credentials, --force
- [x] Update help text and README
- [ ] Test migration from old format
- [ ] Test backward compatibility

---

## Testing Notes

### Manual Testing Scenarios

1. **Fresh install (no existing credentials)**
   ```bash
   jgccli accounts credentials --list
   # Should show: No credentials configured
   
   jgccli accounts credentials ~/my-creds.json
   jgccli accounts credentials --list
   # Should show: default <clientId preview>
   ```

2. **Named credentials**
   ```bash
   jgccli accounts credentials ~/work-creds.json --name work
   jgccli accounts credentials ~/personal-creds.json --name personal
   jgccli accounts credentials --list
   # Should show: default, work, personal
   
   jgccli accounts credentials --remove work
   jgccli accounts credentials --list
   # Should show: default, personal
   ```

3. **Add accounts with named credentials**
   ```bash
   jgccli accounts add me@gmail.com                      # uses default
   jgccli accounts add work@company.com --credentials work
   jgccli accounts list
   # Should show:
   # me@gmail.com
   # work@company.com    (work)
   ```

4. **Re-authorize with --force**
   ```bash
   jgccli accounts add me@gmail.com
   # Error: Account already exists
   
   jgccli accounts add me@gmail.com --force
   # Should complete OAuth flow
   ```

5. **Migration from old format**
   ```bash
   # Create old-format credentials.json:
   echo '{"clientId":"old-id","clientSecret":"old-secret"}' > ~/.jgccli/credentials.json
   
   jgccli accounts credentials --list
   # Should show: default  old-id...
   # File should be migrated to new format
   ```

### Verification Commands

```bash
# Check credentials.json format after migration
cat ~/.jgccli/credentials.json | jq .
# Should show: {"default": {"clientId": ..., "clientSecret": ...}}

# Check accounts.json includes credentialsName
cat ~/.jgccli/accounts.json | jq '.[].credentialsName'
```
