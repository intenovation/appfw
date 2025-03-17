package com.intenovation.appfw.icon;

public class Achtung extends Triangle {
	private static Point start = new Point(3, 0, "oben");
	private static Point end = new Point(0, 6, "links");
	private static Point middle = new Point(6, 6, "rechts");

	public Achtung(String tooltip) {
		super(start, end, middle, tooltip);

	}

}
