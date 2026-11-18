package net.thewellodev.api.event;

/** Base class of all events. */
public abstract class Event {

    public String getEventName() {
        return getClass().getSimpleName();
    }
}
