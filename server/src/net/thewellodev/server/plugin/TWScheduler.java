package net.thewellodev.server.plugin;

import net.thewellodev.api.plugin.Plugin;
import net.thewellodev.api.scheduler.Scheduler;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Main-thread scheduler. tick() is called on every server tick. */
public class TWScheduler implements Scheduler {

    private static final class Task {
        final int id;
        final Plugin plugin;
        final Runnable runnable;
        long nextRun;
        final long period;
        boolean cancelled;

        Task(int id, Plugin plugin, Runnable runnable, long nextRun, long period) {
            this.id = id;
            this.plugin = plugin;
            this.runnable = runnable;
            this.nextRun = nextRun;
            this.period = period;
        }
    }

    private final Logger logger;
    private final List<Task> tasks = new ArrayList<Task>();
    private final List<Task> added = new ArrayList<Task>();
    private int nextId = 1;
    private long currentTick;
    private final ExecutorService async = Executors.newCachedThreadPool(new ThreadFactory() {
        private int n;

        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "TW async-" + (++n));
            t.setDaemon(true);
            return t;
        }
    });

    public TWScheduler(Logger logger) {
        this.logger = logger;
    }

    private synchronized int add(Plugin plugin, Runnable r, long delay, long period) {
        if (plugin == null || r == null) {
            throw new IllegalArgumentException("plugin and task must not be null");
        }
        Task t = new Task(nextId++, plugin, r, currentTick + Math.max(1, delay), period);
        added.add(t);
        return t.id;
    }

    public int runTask(Plugin plugin, Runnable task) {
        return add(plugin, task, 1, -1);
    }

    public int runTaskLater(Plugin plugin, Runnable task, long delayTicks) {
        return add(plugin, task, delayTicks, -1);
    }

    public int runTaskTimer(Plugin plugin, Runnable task, long delayTicks, long periodTicks) {
        return add(plugin, task, delayTicks, Math.max(1, periodTicks));
    }

    public int runTaskAsync(final Plugin plugin, final Runnable task) {
        final int id;
        synchronized (this) {
            id = nextId++;
        }
        async.execute(new Runnable() {
            public void run() {
                try {
                    task.run();
                } catch (Throwable t) {
                    logger.log(Level.SEVERE, "Async task error (" + plugin.getName() + ")", t);
                }
            }
        });
        return id;
    }

    public synchronized void cancelTask(int taskId) {
        for (Task t : tasks) {
            if (t.id == taskId) {
                t.cancelled = true;
            }
        }
        for (Task t : added) {
            if (t.id == taskId) {
                t.cancelled = true;
            }
        }
    }

    public synchronized void cancelTasks(Plugin plugin) {
        for (Task t : tasks) {
            if (t.plugin == plugin) {
                t.cancelled = true;
            }
        }
        for (Task t : added) {
            if (t.plugin == plugin) {
                t.cancelled = true;
            }
        }
    }

    public synchronized boolean isQueued(int taskId) {
        for (Task t : tasks) {
            if (t.id == taskId && !t.cancelled) {
                return true;
            }
        }
        for (Task t : added) {
            if (t.id == taskId && !t.cancelled) {
                return true;
            }
        }
        return false;
    }

    public int getPendingCount() {
        synchronized (this) {
            return tasks.size() + added.size();
        }
    }

    /** Called on the main thread every tick. */
    public void tick() {
        List<Task> due = new ArrayList<Task>();
        synchronized (this) {
            currentTick++;
            tasks.addAll(added);
            added.clear();
            Iterator<Task> it = tasks.iterator();
            while (it.hasNext()) {
                Task t = it.next();
                if (t.cancelled) {
                    it.remove();
                } else if (t.nextRun <= currentTick) {
                    due.add(t);
                    if (t.period > 0) {
                        t.nextRun = currentTick + t.period;
                    } else {
                        it.remove();
                    }
                }
            }
        }
        for (Task t : due) {
            if (t.cancelled || !t.plugin.isEnabled()) {
                continue;
            }
            try {
                t.runnable.run();
            } catch (Throwable ex) {
                logger.log(Level.SEVERE, "Scheduled task error (" + t.plugin.getName() + ")", ex);
            }
        }
    }

    public void shutdown() {
        async.shutdownNow();
    }
}
