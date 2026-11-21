package net.thewellodev.server.plugin;

import net.thewellodev.api.event.Event;

/** Turns the server's events into the events of a build's own plugin API (see api227 and api316). */
public interface EventTranslator {
    void translate(Event e);
}
