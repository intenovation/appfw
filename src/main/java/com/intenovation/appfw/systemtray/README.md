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

## Framework Integration Components

- **FrameworkTrayView:** Base view for system tray integration
- **FrameworkParentTrayView:** Parent view implementation
- **FrameworkCheckboxTrayView:** Checkbox view implementation
- **FrameworkActionTrayView:** Action view implementation
- **FrameworkTrayFactory:** Factory for creating framework-compatible views

## Usage

This package makes it easy to create system tray applications with rich context menus and background tasks that report progress. It supports cross-platform deployment and provides a clean API that hides UI framework complexities.

### Standard Usage

Use the SystemTrayApp class with AppConfig, MenuCategories, and Tasks:

```java
AppConfig config = ...;
List<MenuCategory> menuCategories = ...;
List<Task> tasks = ...;
SystemTrayApp app = new SystemTrayApp(config, menuCategories, tasks);