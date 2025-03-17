# Backend-Friendly System Tray Framework

A specialized wrapper for the System Tray Framework that completely hides UI dependencies (Swing/AWT), allowing backend developers to create system tray applications without any UI programming knowledge.

## Overview

This framework provides a simplified API that abstracts away all UI-related code, allowing backend developers to focus on business logic while still creating rich system tray applications.

## Key Features

1. **No UI Framework Knowledge Required**: The wrapper hides all Swing/AWT dependencies
2. **Simple Interface-based API**: Designed for backend developers
3. **Builder Pattern for Easy Configuration**: Create menus and tasks with minimal code
4. **Progress and Status Updates**: Background tasks can report progress without UI code
5. **Cross-Platform Support**: Works on Windows, macOS, and Linux

## Getting Started

### 1. Define Application Configuration

First, define your application configuration:

```java
AppConfig appConfig = new AppConfig() {
    @Override
    public String getAppName() {
        return "My App";
    }

    @Override
    public String getIconPath() {
        return "/icon.png";  // Icon in resources folder
    }

    @Override
    public void onIconDoubleClick() {
        // What happens when user double-clicks the tray icon
        showAppStatus();
    }
};
```

### 2. Create Menu Categories

Next, create menu categories using the builder pattern:

```java
List<MenuCategory> menuCategories = new ArrayList<>();

// Add a File menu
menuCategories.add(new CategoryBuilder("File")
    .addAction("Open", MyApp::openFile)
    .addAction("Save", MyApp::saveFile)
    .build());

// Add a Tools menu with a submenu
MenuCategory submenu = new CategoryBuilder("Advanced Tools")
        .addAction("Tool 1", MyApp::runTool1)
        .addAction("Tool 2", MyApp::runTool2)
        .build();

menuCategories.add(new CategoryBuilder("Tools")
    .addAction("Simple Tool", MyApp::runSimpleTool)
    .addSubcategory(submenu)
    .build());
```

### 3. Create Background Tasks

Create background tasks that can run on a schedule or be started manually:

```java
List<Task> tasks = new ArrayList<>();

// Add a task that runs every hour
tasks.add(new TaskBuilder("Hourly Task")
    .withDescription("Runs every hour")
    .withIntervalSeconds(3600) // Every hour
    .showInMenu(true)
    .withExecutor(MyApp::runHourlyTask)
    .build());

// Add a task that can only be started manually
        tasks.add(new TaskBuilder("Manual Task")
    .withDescription("Run only when requested")
    .withIntervalSeconds(0) // Manual only
    .showInMenu(true)
    .withExecutor(MyApp::runManualTask)
    .build());
```

### 4. Initialize the System Tray Application

Finally, initialize the application:

```java
new BackendFriendlySystemTray(appConfig, menuCategories, tasks);
```

## Task Implementation

Implement task executor methods with progress and status updates:

```java
private static String runHourlyTask(Consumer<Integer> progressUpdater, Consumer<String> statusUpdater)
        throws InterruptedException {
    // Start task
    statusUpdater.accept("Starting hourly task...");
    progressUpdater.accept(0);

    // Do some work
    for (int i = 0; i < 10; i++) {
        // Check for cancellation
        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedException("Task cancelled");
        }

        // Update progress (0-100)
        int progress = (i + 1) * 10;
        progressUpdater.accept(progress);

        // Update status message
        statusUpdater.accept("Processing step " + (i + 1) + " of 10");

        // Simulate work
        Thread.sleep(500);
    }

    // Return completion message
    return "Hourly task completed successfully";
}
```

## Display Messages

Show messages to the user without using Swing directly:

```java
// Show information message
BackendFriendlySystemTray.showMessage("Information", "Operation completed successfully");

// Show error message
BackendFriendlySystemTray.showError("Error", "Failed to connect to server");

// Show warning message
BackendFriendlySystemTray.showWarning("Warning", "Low disk space detected");
```

## File Operations

Open directories without dealing with UI frameworks:

```java
File directory = new File("/path/to/directory");
if (!BackendFriendlySystemTray.openDirectory(directory)) {
    BackendFriendlySystemTray.showError("Error", "Could not open directory");
}
```

## How It Works

The wrapper uses a facade pattern to completely hide the UI implementation details:

1. The `BackendFriendlySystemTray` class wraps the `SystemTrayAppFramework`
2. All UI-specific interfaces are replaced with simpler interfaces
3. Builders make it easy to create menu structures and tasks
4. `Consumer<T>` interfaces are used for progress and status updates

## Benefits for Backend Developers

1. **Familiar Patterns**: Uses standard Java interfaces and functional patterns
2. **No UI Dependencies**: All Swing/AWT code is hidden
3. **Simple Progress Updates**: Report progress without UI knowledge
4. **Easy Menu Creation**: Create complex menus with a simple builder API
5. **Separation of Concerns**: Business logic is separated from UI code

## Example Use Cases

- **Background Service Management**: Monitor and control background services
- **Data Sync Applications**: Synchronize data with progress indication
- **System Monitoring Tools**: Display system status in the tray
- **Scheduled Task Management**: Run and monitor scheduled tasks

## Requirements

- Java 8 or higher
- Place your tray icon in the resources folder
- Make sure your PATH includes the Java runtime