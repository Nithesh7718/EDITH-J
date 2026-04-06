package com.edithj.util;

import java.util.Objects;

/**
 * Utility for running tasks asynchronously in daemon threads. Useful for
 * long-running operations that shouldn't block the UI.
 */
public final class BackgroundTaskRunner {

    private BackgroundTaskRunner() {
        // Utility class
    }

    /**
     * Run a task in a background daemon thread.
     *
     * @param taskName descriptive name for the thread
     * @param task the runnable task
     * @return the created thread (already started)
     */
    public static Thread runAsync(String taskName, Runnable task) {
        Objects.requireNonNull(task, "task cannot be null");
        Thread thread = new Thread(task, taskName);
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    /**
     * Run a task in a background daemon thread with error handling.
     *
     * @param taskName descriptive name for the thread
     * @param task the runnable task
     * @param onError handler for exceptions (receives exception and thread
     * name)
     * @return the created thread (already started)
     */
    public static Thread runAsync(String taskName, Runnable task,
            java.util.function.BiConsumer<Exception, String> onError) {
        Objects.requireNonNull(task, "task cannot be null");
        Objects.requireNonNull(onError, "onError cannot be null");

        Thread thread = new Thread(() -> {
            try {
                task.run();
            } catch (Exception exception) {
                onError.accept(exception, taskName);
            }
        }, taskName);

        thread.setDaemon(true);
        thread.start();
        return thread;
    }
}
