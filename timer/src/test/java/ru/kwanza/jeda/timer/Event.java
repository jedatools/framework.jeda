package ru.kwanza.jeda.timer;

import ru.kwanza.jeda.api.AbstractEvent;


public class Event extends AbstractEvent {
    private String contextId;

    public Event(String contextId) {
        this.contextId = contextId;
    }

    public String getContextId() {
        return contextId;
    }
}
