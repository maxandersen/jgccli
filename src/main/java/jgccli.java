///usr/bin/env jbang "$0" "$@" ; exit $?

//DEPS io.quarkus.platform:quarkus-bom:3.34.1@pom
//DEPS io.quarkus:quarkus-picocli
//DEPS io.quarkus:quarkus-jackson

//DEPS com.google.api-client:google-api-client:2.4.0
//DEPS com.google.apis:google-api-services-calendar:v3-rev20250404-2.0.0
//DEPS com.google.oauth-client:google-oauth-client-jetty:1.35.0

//SOURCES dk/xam/jgccli/cli/AccountsCommand.java
//SOURCES dk/xam/jgccli/cli/AclCommand.java
//SOURCES dk/xam/jgccli/cli/CalendarsCommand.java
//SOURCES dk/xam/jgccli/cli/CreateCommand.java
//SOURCES dk/xam/jgccli/cli/DeleteCommand.java
//SOURCES dk/xam/jgccli/cli/EventCommand.java
//SOURCES dk/xam/jgccli/cli/EventsCommand.java
//SOURCES dk/xam/jgccli/cli/FreeBusyCommand.java
//SOURCES dk/xam/jgccli/cli/GccliCommand.java
//SOURCES dk/xam/jgccli/cli/OutputFormatter.java
//SOURCES dk/xam/jgccli/cli/UpdateCommand.java
//SOURCES dk/xam/jgccli/exception/AccountNotFoundException.java
//SOURCES dk/xam/jgccli/exception/AuthorizationException.java
//SOURCES dk/xam/jgccli/exception/GccliException.java
//SOURCES dk/xam/jgccli/model/CalendarAccount.java
//SOURCES dk/xam/jgccli/model/Credentials.java
//SOURCES dk/xam/jgccli/model/EventSearchResult.java
//SOURCES dk/xam/jgccli/model/OAuth2Credentials.java
//SOURCES dk/xam/jgccli/oauth/CalendarOAuthFlow.java
//SOURCES dk/xam/jgccli/service/CalendarService.java
//SOURCES dk/xam/jgccli/storage/AccountStorage.java

//FILES application.properties=../resources/application.properties

// JBang bootstrap for jgccli - Quarkus Picocli handles entry via @TopCommand
// 
// Usage:
//   jbang jgccli.java --help
//   jbang https://github.com/maxandersen/jgccli/blob/main/src/main/java/jgccli.java --help
