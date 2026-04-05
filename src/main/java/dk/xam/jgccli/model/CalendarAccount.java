package dk.xam.jgccli.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CalendarAccount(
    String email,
    String credentialsName,  // null = default credentials
    OAuth2Credentials oauth2
) {
    // Constructor for backward compatibility (no credentialsName)
    public CalendarAccount(String email, OAuth2Credentials oauth2) {
        this(email, null, oauth2);
    }
}
