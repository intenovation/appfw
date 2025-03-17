package com.intenovation.appfw.icon;

import java.awt.Graphics;

public class Circle implements PictureElement {
	private Point center;
	private int radius;
	private String tooltip;

	@Override
	public void move(int dx, int dy) {
		center.move(dx, dy);

	}

	@Override
	public void draw(Graphics graphics) {
		graphics.drawOval(center.x - radius, center.y - radius, radius * 2,
				radius * 2);

	}

	public Circle(Point center, int radius, String tooltip) {
		super();
		this.tooltip = tooltip + "(around " + center.getTooltip() + ")";
		this.center = new Point(center, 0, 0, tooltip + " center");
		this.radius = radius;
	}

	@Override
	public PictureElement deepClone() {

		return new Circle(center, radius, tooltip + " Clone");
	}

	public String getTooltip() {
		return tooltip;
	}
}
