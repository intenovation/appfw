# Consolidated System Tray Framework

A unified system tray framework with a clean, backend-friendly API that doesn't expose UI dependencies, allowing developers to create system tray applications without UI programming knowledge.

## Overview

This framework provides a simplified API that abstracts away all UI framework complexities, enabling backend developers to create feature-rich system tray applications with hierarchical menus, background tasks with progress tracking, and status reporting.

## Key Features

1. **No UI Dependencies in Public API**: All UI framework dependencies are hidden in the internal implementation
2. **Simple Interface-Based Design**: Clean interfaces designed for backend developers
3. **Builder Pattern API**: Create complex configurations with simple builder methods
4. **Progress and Status Updates**: Track task progress without UI code
5. **Task Management**: Start, monitor, and cancel background tasks
6. **Hierarchical Menus**: Create complex menu structures easily
7. **Cross-Platform**: Works on Windows, macOS, and Linux

## Getting Started

### 1. Define Application Configuration

```java
AppConfig appConfig = new AppConfig() {
    @Override
    public String getAppName() {
        return "My Application";
    }
    
    @Override
    public String getIconPath() {
        return "/app_icon.png";  // Icon in resources folder
    }
    
    @Override
    public void onIconDoubleClick() {
        // What happens when the tray icon is double-clicked
        SystemTrayApp.showMessage("Status", "Application is running");
    }
};
```

### 2. Create Menu Categories

```java
List<MenuCategory> menuCategories = new ArrayList<>();

// File menu
menuCategories.add(new CategoryBuilder("File")
    .addAction("Open", MyApp::openFile)
    .addAction("Save", MyApp::saveFile)
    .addAction("Exit", MyApp::exit)
    .build());

// Tools menu with a submenu
MenuCategory advancedTools = new CategoryBuilder("Advanced Tools")
    .addAction("Tool 1", MyApp::runTool1)
    .addAction("Tool 2", MyApp::runTool2)
    .build();

menuCategories.add(new CategoryBuilder("Tools")
    .addAction("Simple Tool", MyApp::runSimpleTool)
    .addSubcategory(advancedTools)
    .build());
```

### 3. Create Background Tasks

```java
List<Task> tasks = new ArrayList<>();

// Task with progress reporting
tasks.add(new TaskBuilder("Data Sync")
    .withDescription("Synchronizes data with the server")
    .withIntervalSeconds(3600)  // Run every hour
    .showInMenu(true)  // Show in the Tasks menu
    .withExecutor(MyApp::syncData)
    .build());

// Task that only runs manually
tasks.add(new TaskBuilder("Database Backup")
    .withDescription("Creates a backup of the database")
    .withIntervalSeconds(0)  // Manual execution only
    .showInMenu(true)
    .withExecutor(MyApp::backupDatabase)
    .build());
```

### 4. Initialize the System Tray App

```java
SystemTrayApp app = new SystemTrayApp(appConfig, menuCategories, tasks);
```

## Implementing Tasks

Tasks can report progress and status updates using simple callbacks:

```java
private static String syncData(Consumer<Integer> progressUpdater, Consumer<String> statusUpdater) 
        throws InterruptedException {
    // Initialize
    statusUpdater.accept("Starting data synchronization...");
    progressUpdater.accept(0);
    
    // Get data to sync
    statusUpdater.accept("Getting data...");
    progressUpdater.accept(10);
    
    // Process data in chunks
    for (int i = 0; i < 10; i++) {
        // Check for cancellation
        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedException("Task cancelled");
        }
        
        // Update progress (10% to 90%)
        int progress = 10 + (i * 8);
        progressUpdater.accept(progress);
        
        // Update status
        statusUpdater.accept("Processing chunk " + (i + 1) + " of 10");
        
        // Do some work
        Thread.sleep(500);  // Simulate work
    }
    
    // Finalize
    statusUpdater.accept("Finalizing...");
    progressUpdater.accept(95);
    Thread.sleep(500);
    
    // Complete
    progressUpdater.accept(100);
    return "Data synchronization completed successfully";
}
```

## User Interaction

Show messages to the user without UI code:

```java
// Information message
SystemTrayApp.showMessage("Information", "Operation completed successfully");

// Error message
SystemTrayApp.showError("Error", "Failed to connect to the server");

// Warning message
SystemTrayApp.showWarning("Warning", "Disk space is running low");
```

## File Operations

Open directories without UI framework knowledge:

```java
File logDirectory = new File("/path/to/logs");
if (!SystemTrayApp.openDirectory(logDirectory)) {
    SystemTrayApp.showError("Error", "Failed to open log directory");
}
```

## Task Management

Manually start and cancel tasks:

```java
// Start a task
app.startTask("Database Backup");

// Cancel a running task
app.cancelTask("Data Sync");

// Show task status dialog
app.showTaskStatus();
```

## How It Works

The framework uses a facade pattern to hide UI implementation details:

1. The public API (`SystemTrayApp`) is clean and has no UI dependencies
2. The actual UI implementation (`SystemTrayAppImpl`) is a private inner class
3. All UI interactions are abstracted through simple interfaces and static methods
4. Background tasks run in separate threads and update the UI via callbacks

## Benefits for Backend Developers

1. **No UI Framework Knowledge Required**: No need to understand Swing/AWT
2. **Functional Programming Style**: Use of `Consumer<T>` for callbacks
3. **Builder Pattern**: Fluent API for configuration
4. **Method References**: Use of method references for actions and task executors
5. **No UI Imports**: Application code has no UI framework imports

## Architecture

The framework uses a layered architecture:

1. **Public API Layer**: Clean interfaces and builders with no UI dependencies
2. **Internal Implementation Layer**: Hidden UI implementation details
3. **Background Task Layer**: Executor services for scheduled and manual tasks

## Requirements

- Java 8 or higher
- System tray support on the operating system
- Icon file in application resources