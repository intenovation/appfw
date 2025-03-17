
// File: TaskStatus.java
package com.intenovation.appfw.systemtray;

/**
 * Class to track task status
 */
public class TaskStatus {
    private volatile int progress = 0;
    private volatile String status = "Not started";
    private volatile boolean running = false;
    
    public int getProgress() {
        return progress;
    }
    
    public void setProgress(int progress) {
        this.progress = Math.max(0, Math.min(100, progress));
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public boolean isRunning() {
        return running;
    }
    
    public void setRunning(boolean running) {
        this.running = running;
    }
}