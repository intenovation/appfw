# Inverse Model-View Package

An implementation of the Model-View pattern where the model controls the view, inverting the traditional relationship.

## Key Components

- **Model:** Base interface for all models
- **View:** Base interface for all views
- **ActionModel:** Model that can perform actions
- **CheckboxModel:** Model for checkbox-like elements
- **ParentModel/ParentView:** Models and views that can contain children
- **Models:**
    - **BasicCheckboxMenuEntry:** Checkbox menu item
    - **BasicTextboxMenuEntry:** Text input menu item
    - **FileSelect:** File selection component
    - **RadioMenuEntry:** Radio button group
    - **RunNow:** Immediate action executor
    - **SetMenuEntry:** Set-based menu for multiple selections
    - **Work:** Abstract background task

## Usage

This package provides a foundation for building hierarchical UI components where models control their views. It's particularly useful for menu systems, trees, and other nested UI structures.