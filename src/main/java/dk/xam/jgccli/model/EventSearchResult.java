package dk.xam.jgccli.model;

import com.google.api.services.calendar.model.Event;
import java.util.List;

public record EventSearchResult(
    List<Event> events,
    String nextPageToken
) {}
