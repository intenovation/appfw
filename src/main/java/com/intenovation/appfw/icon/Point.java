package com.intenovation.appfw.icon;

import java.awt.Graphics;

public class Point implements PictureElement {
	private String tooltip;
	int x;
	int y;

	@Override
	public PictureElement deepClone() {

		return new Point(this, 0, 0, tooltip + " Clone");
	}

	/**
	 * kopiert einen anderen Punkt, um ihn anschlie§end relativ zu verschieben.
	 * 
	 * @param vorlage
	 */
	public Point(Point vorlage, int dx, int dy, String tooltip) {
		super();
		this.tooltip = tooltip;
		this.x = vorlage.x;
		this.y = vorlage.y;
		move(dx, dy);
	}

	public Point(int x, int y, String tooltip) {
		super();
		this.tooltip = tooltip;
		this.x = x;
		this.y = y;
	}

	@Override
	public void move(int dx, int dy) {
		x = x + dx;
		y = y + dy;

	}

	@Override
	public void draw(Graphics graphics) {
		graphics.drawOval(x, y, 0, 0);

	}

	public String getTooltip() {
		return tooltip;
	}
}
