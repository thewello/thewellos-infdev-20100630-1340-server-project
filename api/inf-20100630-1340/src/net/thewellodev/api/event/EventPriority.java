package net.thewellodev.api.event;

/** Handler priority. MONITOR is only for observing the outcome; do not change the event. */
public enum EventPriority {
    LOWEST, LOW, NORMAL, HIGH, HIGHEST, MONITOR
}
