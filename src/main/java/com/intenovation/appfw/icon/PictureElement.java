package com.intenovation.appfw.icon;

import java.awt.Graphics;

public interface PictureElement {
	PictureElement deepClone();
	
	/**
	 * verschiebt ein Bildelement. Dadurch lassen sich kopien erzeugen
	 * 
	 * @param dx
	 * @param dy
	 */
	void move(int dx, int dy);

	/**
	 * zeichnet das Element
	 */
	void draw(Graphics graphics);
	
	public String getTooltip() ;
}
