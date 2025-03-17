package com.intenovation.appfw.icon;

import java.awt.Graphics;

public class Rect implements PictureElement {
	private Point start;
	private Point end;
	private String tooltip;

	@Override
	public void move(int dx, int dy) {
		start.move(dx, dy);
		end.move(dx, dy);
	}

	public Rect(Rect vorlage, int dx, int dy, String tooltip) {
		super();
		this.tooltip = tooltip;
		this.start = new Point(vorlage.start, dx, dy,tooltip+" Start");
		this.end = new Point(vorlage.end, dx, dy,tooltip+" End");

	}

	public Rect(Point start, Point end, String tooltip) {
		super();
		this.tooltip = tooltip+"("+start.getTooltip()+"->"+end.getTooltip()+")";
		this.start = new Point(start, 0, 0,tooltip+" Start");
		this.end = new Point(end, 0, 0,tooltip+" End");
	}

	@Override
	public void draw(Graphics graphics) {
		graphics.drawRect(start.x, start.y, end.x - start.x, end.y - start.y);

	}
	public String getTooltip() {
		return tooltip;
	}

	@Override
	public PictureElement deepClone() {
		 
		return new Rect(this,0,0,tooltip+" Clone");
	}
}
