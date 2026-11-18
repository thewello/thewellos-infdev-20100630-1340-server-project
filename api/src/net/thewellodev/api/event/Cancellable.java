package net.thewellodev.api.event;

/** An event that can be cancelled. If cancelled, the server skips its default action. */
public interface Cancellable {

    boolean isCancelled();

    void setCancelled(boolean cancel);
}
