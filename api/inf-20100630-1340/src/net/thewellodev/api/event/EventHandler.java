package net.thewellodev.api.event;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Marks a {@link Listener} method as an event handler. */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface EventHandler {

    /** Call order: LOWEST first, MONITOR last. */
    EventPriority priority() default EventPriority.NORMAL;

    /** If true, the handler is skipped when the event was already cancelled. */
    boolean ignoreCancelled() default false;
}
