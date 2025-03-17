# Icon Package

A vector graphics system for creating and manipulating simple 2D graphics.

## Components

- **PictureElement:** Base interface for all drawable elements
- **Point:** Represents a 2D point with coordinates and tooltip
- **Basic Shapes:**
    - **Circle:** Circular shape with center and radius
    - **Line:** Line between two points
    - **Rect:** Rectangle defined by two corner points
    - **Triangle:** Triangle defined by three points
- **Figure:** Composite picture element containing multiple shapes
- **SmartIcon:** Icon with support for dynamic accents/modifications

## Usage

This package allows for programmatic creation of simple vector graphics that can be used as icons or UI elements. The component-based approach allows for composing complex shapes from simpler ones.