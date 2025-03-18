package com.intenovation.appfw.systemtray;

/**
 * Class to track task status
 */
public class TaskStatus {
    private volatile int progress = 0;
    private volatile String status = "Not started";
    private volatile boolean running = false;

    /**
     * Get the current progress percentage
     *
     * @return Progress percentage (0-100)
     */
    public int getProgress() {
        return progress;
    }

    /**
     * Set the progress percentage
     *
     * @param progress Progress percentage (0-100)
     */
    public void setProgress(int progress) {
        this.progress = Math.max(0, Math.min(100, progress));
    }

    /**
     * Get the current status message
     *
     * @return Status message
     */
    public String getStatus() {
        return status;
    }

    /**
     * Set the status message
     *
     * @param status Status message
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Check if the task is running
     *
     * @return true if the task is running
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * Set whether the task is running
     *
     * @param running true if the task is running
     */
    public void setRunning(boolean running) {
        this.running = running;
    }

    /**
     * Update both progress and status in one call
     *
     * @param progress Progress percentage (0-100)
     * @param status Status message
     */
    public void update(int progress, String status) {
        setProgress(progress);
        setStatus(status);
    }
}