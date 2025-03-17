# System Tray Package

A comprehensive framework for creating system tray applications with menus, background tasks, and status reporting.

## Key Components

- **SystemTrayApp:** Main entry point for tray applications
- **AppConfig:** Configuration interface for tray applications
- **Action/MenuAction:** Interfaces for menu actions
- **MenuCategory/MenuGroup:** Hierarchical menu structure
- **Task:** Background task with progress reporting
- **BackgroundTask:** Interface for long-running operations
- **Progress/Status Callbacks:** Interfaces for reporting progress
- **Builder Classes:** Convenience builders for menus and tasks

## Usage

This package makes it easy to create system tray applications with rich context menus and background tasks that report progress. It supports cross-platform deployment and provides a clean API that hides UI framework complexities.
# Task Progress in System Tray Menu

The framework now supports displaying task progress directly in the menu items. This provides users with immediate visibility into task status without needing to open the status dialog.

## How It Works

When a task is running, its menu item label will be updated to show the current progress percentage:

```
File Indexer (45%)
```

The percentage updates in real-time as the task progresses. When the task completes or is cancelled, the menu item reverts to its original label.

## Implementation Details

The framework has been enhanced with:

1. **Menu Item Tracking**: Each task menu item is stored in a `ConcurrentHashMap` for easy access
2. **Progress Updates**: When a task reports progress, its menu item label is updated
3. **Reset on Completion**: Labels are reset when tasks complete or are cancelled

## Code Changes

The key changes include:

1. Added a `taskMenuItems` map to store references to task menu items:

```java
private final ConcurrentHashMap<String, MenuItem> taskMenuItems = new ConcurrentHashMap<>();
```

2. Storing menu items when they're created:

```java
MenuItem taskItem = new MenuItem(task.getName());
taskItem.addActionListener(e -> startTask(task));
tasksMenu.add(taskItem);

// Store the menu item for later updates
taskMenuItems.put(task.getName(), taskItem);
```

3. Added a method to update the menu item labels:

```java
private void updateTaskMenuLabel(String taskName, int progress, boolean running) {
    MenuItem menuItem = taskMenuItems.get(taskName);
    if (menuItem != null) {
        SwingUtilities.invokeLater(() -> {
            String baseLabel = taskName;
            if (running) {
                menuItem.setLabel(baseLabel + " (" + progress + "%)");
            } else {
                menuItem.setLabel(baseLabel);
            }
        });
    }
}
```

4. Updated the progress callback to also update menu labels:

```java
percent -> {
    SwingUtilities.invokeLater(() -> {
        status.setProgress(percent);
        updateTaskMenuLabel(task.getName(), percent, true);
        updateStatusDialog();
    });
}
```

5. Reset menu labels when tasks complete or are cancelled.

## Benefits

This enhancement provides several advantages:

1. **Immediate Visibility**: Users can see task progress without opening additional windows
2. **Quick Status Checks**: A glance at the menu reveals which tasks are running and their progress
3. **Better User Experience**: More intuitive status indication with less user interaction required
4. **Context Awareness**: Users can quickly identify which tasks are running even when multitasking

## Usage Example

The functionality is automatically integrated. No changes are needed to your task implementation code - the framework handles all menu updates transparently.

Tasks will show their progress in the menu when they:
1. Are available in the menu (`isAvailableInMenu()` returns true)
2. Are currently running
3. Report progress through the `ProgressCallback`

## Design Considerations

1. **Performance**: Menu updates use `SwingUtilities.invokeLater()` to ensure they don't block the task execution
2. **Thread Safety**: The implementation is thread-safe using `ConcurrentHashMap` and EDT dispatching
3. **Visual Clarity**: The format "Task Name (XX%)" is concise yet informative
4. **Consistency**: Menu items revert to their original labels when tasks complete