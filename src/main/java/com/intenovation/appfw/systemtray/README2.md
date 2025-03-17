# Enhanced System Tray Application Framework

A Java framework that allows developers with no UI experience to create system tray applications with hierarchical menus and background tasks with progress tracking, status updates, and cancellation support.

## Overview

This enhanced framework builds on the basic System Tray Framework to add:

1. **Progress Tracking**: Monitor task completion percentage with progress bars
2. **Status Updates**: Display current task status messages
3. **Task Cancellation**: Ability to cancel running tasks
4. **Manual Task Execution**: Start tasks from menu or status dialog
5. **Task Status Display**: Window showing all tasks with progress and controls

## Getting Started

### 1. Add Framework to Your Project

Copy the `SystemTrayAppFramework.java` file to your project's source directory.

### 2. Create Your Application Class

Create a class that implements the `SystemTrayApp` interface:

```java
public class MyEnhancedApp implements SystemTrayApp {
    // Implementation goes here
}
```

### 3. Implement Required Methods

Your application class needs to implement:

- `getAppName()` - Returns your application name
- `getIconPath()` - Returns path to your icon resource
- `getMenuGroups()` - Returns menu structure
- `getBackgroundTasks()` - Returns list of background tasks with progress support
- `getDefaultAction()` - Returns action for tray icon double-click

### 4. Launch Your Application

```java
public static void main(String[] args) {
    try {
        new SystemTrayAppFramework(new MyEnhancedApp());
        System.out.println("System tray app started");
    } catch (AWTException e) {
        e.printStackTrace();
        // Handle error
    }
}
```

## Key Components

### BackgroundTask Interface

The enhanced BackgroundTask interface supports progress tracking and cancellation:

```java
public interface BackgroundTask {
    String execute(ProgressCallback progressCallback, StatusCallback statusCallback) 
                  throws InterruptedException;
    int getIntervalSeconds();
    String getName();
    boolean isAvailableInMenu();
    default String getDescription() { return null; }
}
```

### Progress and Status Callbacks

Two callback interfaces allow tasks to report progress and status:

```java
public interface ProgressCallback {
    void updateProgress(int percent);
}

public interface StatusCallback {
    void updateStatus(String message);
}
```

### AbstractBackgroundTask

Helper class to simplify task implementation:

```java
// Creating a background task
BackgroundTask myTask = new AbstractBackgroundTask(
    "Task Name",          // Name shown in menus and status
    "Task Description",   // Optional description
    3600,                 // Run every hour (0 for manual only)
    true                  // Available in menu
) {
    @Override
    public String execute(ProgressCallback progressCallback, StatusCallback statusCallback) 
            throws InterruptedException {
        // Initialization
        statusCallback.updateStatus("Starting task...");
        progressCallback.updateProgress(0);
        
        // Your task logic here
        for (int i = 0; i < 100; i++) {
            // Check for interruption regularly
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException("Task cancelled");
            }
            
            // Update progress
            progressCallback.updateProgress(i);
            
            // Update status message periodically
            if (i % 10 == 0) {
                statusCallback.updateStatus("Processing step " + i);
            }
            
            // Your work here
            Thread.sleep(100); // Simulate work
        }
        
        return "Task completed successfully!";
    }
};
```

## Creating Tasks with Progress Support

When implementing the `execute` method of a BackgroundTask:

1. Use `progressCallback.updateProgress(percent)` to update completion percentage (0-100)
2. Use `statusCallback.updateStatus(message)` to update status message
3. Check `Thread.currentThread().isInterrupted()` frequently to support cancellation
4. Throw `InterruptedException` when cancelled
5. Return a completion message (or null) when task completes successfully

### Handling Long-Running Tasks

For long-running tasks:

1. Divide the task into logical stages
2. Update progress proportionally through each stage
3. Update status messages to indicate current stage
4. Check for interruption regularly
5. Use try-catch blocks to handle interruption properly

Example:

```java
// For a task with 5 stages
for (int stage = 0; stage < 5; stage++) {
    statusCallback.updateStatus("Stage " + (stage + 1) + ": " + stageNames[stage]);
    
    // Calculate progress range for this stage
    int startProgress = stage * 20;
    int endProgress = (stage + 1) * 20;
    
    // Do work for this stage
    for (int i = 0; i < workItemsPerStage; i++) {
        // Check for cancellation
        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedException("Task cancelled");
        }
        
        // Calculate progress within this stage
        int progress = startProgress + (i * (endProgress - startProgress) / workItemsPerStage);
        progressCallback.updateProgress(progress);
        
        // Do actual work...
    }
}
```

## Task Status Dialog

The framework automatically provides a status dialog that shows:

1. Progress bars for all tasks
2. Current status message for each task
3. Start buttons for tasks that aren't running
4. Cancel buttons for tasks that are running

This dialog can be accessed from the "Task Status" menu item in the system tray.

## Running Tasks from Menu

Tasks with `isAvailableInMenu()` returning `true` will appear in a "Tasks" menu in the system tray. Users can start these tasks manually by clicking on them.

## Scheduled vs. Manual-only Tasks

- Set `getIntervalSeconds()` to a positive value (like 3600 for hourly) to schedule automatic execution
- Set `getIntervalSeconds()` to 0 for tasks that should only run when manually started

## Cancellation Support

Tasks should:

1. Check for interruption frequently using `Thread.currentThread().isInterrupted()`
2. Clean up resources when interrupted
3. Throw `InterruptedException` to signal cancellation

The framework will handle updating the UI and notifications.

## Example Application

See `EnhancedSystemTrayExample.java` for a complete working example that demonstrates:

1. File indexing task with progress tracking
2. Disk cleanup task with status updates
3. Document backup task with multiple stages
4. Task cancellation support
5. Both scheduled and manual-only tasks

## Best Practices

1. Update progress frequently for a smooth user experience
2. Provide meaningful status messages
3. Check for interruption at regular intervals
4. Handle exceptions gracefully
5. Return useful completion messages
6. Divide long tasks into logical stages
7. Use descriptive names and descriptions for tasks