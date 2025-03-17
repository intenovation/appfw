package com.intenovation.appfw.icon;

import java.awt.Graphics;
import java.util.ArrayList;
import java.util.List;

public class Figure implements PictureElement {
	List<PictureElement> childs = new ArrayList<PictureElement>();
	private String tooltip;

	public String getTooltip() {
		return tooltip;
	}

	public Figure(String tooltip) {
		super();
		this.tooltip = tooltip;
	}

	@Override
	public PictureElement deepClone() {

		Figure figure = new Figure(tooltip + " Clone");
		for (PictureElement child : childs)
			figure.add(child.deepClone());
		return figure;
	}

	public void add(PictureElement child) {
		childs.add(child);
	}

	@Override
	public void move(int dx, int dy) {
		for (PictureElement child : childs) {
			child.move(dx, dy);
		}

	}

	@Override
	public void draw(Graphics graphics) {
		for (PictureElement child : childs) {
			child.draw(graphics);
		}

	}

}
