package com.intenovation.appfw.icon;

import java.awt.Graphics;

public class Triangle implements PictureElement {
	private Point start;
	private Point end;
	private Point middle;
	private String tooltip;

	@Override
	public void move(int dx, int dy) {
		start.move(dx, dy);
		end.move(dx, dy);
		middle.move(dx, dy);
	}

	public Triangle(Triangle vorlage, int dx, int dy, String tooltip) {
		super();
		this.tooltip = tooltip;
		this.start = new Point(vorlage.start, dx, dy, tooltip + " Start");
		this.end = new Point(vorlage.end, dx, dy, tooltip + " End");
		this.middle = new Point(vorlage.middle, dx, dy, tooltip + " middle");
	}

	public Triangle(Point start, Point end, Point middle, String tooltip) {
		super();
		this.tooltip = tooltip + "(" + start.getTooltip() + "->"
				+ end.getTooltip() + ")";
		this.start = new Point(start, 0, 0, tooltip + " Start");
		this.end = new Point(end, 0, 0, tooltip + " End");
		this.middle = new Point(middle, 0, 0, tooltip + " middle");
	}

	@Override
	public void draw(Graphics graphics) {
		Line line = new Line(start, end, tooltip + "line1");
		line.draw(graphics);
		line = new Line(start, middle, tooltip + "line2");
		line.draw(graphics);
		line = new Line(middle, end, tooltip + "line3");
		line.draw(graphics);
	}

	public String getTooltip() {
		return tooltip;
	}

	@Override
	public PictureElement deepClone() {

		return new Triangle(this, 0, 0, tooltip + " Clone");
	}
}
