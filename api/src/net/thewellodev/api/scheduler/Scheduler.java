package net.thewellodev.api.scheduler;

import net.thewellodev.api.plugin.Plugin;

/**
 * Runs tasks on the main server thread. 20 ticks = 1 second.
 * Use this (or events) to touch the world or players; never change the world from your own thread.
 */
public interface Scheduler {

    /** Runs on the next tick. @return task id */
    int runTask(Plugin plugin, Runnable task);

    /** Runs once after delay ticks. */
    int runTaskLater(Plugin plugin, Runnable task, long delayTicks);

    /** Runs after delay ticks, then every period ticks. */
    int runTaskTimer(Plugin plugin, Runnable task, long delayTicks, long periodTicks);

    /** Runs on a separate thread (do not touch the world from it!). */
    int runTaskAsync(Plugin plugin, Runnable task);

    void cancelTask(int taskId);

    void cancelTasks(Plugin plugin);

    boolean isQueued(int taskId);
}
