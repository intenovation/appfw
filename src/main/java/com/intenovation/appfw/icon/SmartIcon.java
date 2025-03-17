package com.intenovation.appfw.icon;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageFilter;
import java.awt.image.ImageProducer;
import java.awt.image.RGBImageFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import com.intenovation.appfw.Assert;

public class SmartIcon {
	static private Logger log = Logger.getLogger(SmartIcon.class.getName());
	private String tooltip;
	private Image image;
	private Dimension size;
	private Graphics graphics;
	private List<PictureElement> pictureElements = new ArrayList<PictureElement>();
	private List<PictureElement> accents = new ArrayList<PictureElement>();

	public SmartIcon(Dimension size, String tooltip) {
		this.tooltip = tooltip;
		Assert.notNull(size, "null size");
		this.size = size;
	//	log.info("" + size);

		// graphics.drawRect(6, 7, 8, 7);
		//
		// graphics.drawLine(6, 7, 10, 11);
		// graphics.drawLine(10, 11, 14, 7);
		// graphics.drawLine(13, 17, 16, 20);
		// graphics.drawLine(16, 20, 22, 10);
	}

	Icon icon;

	public SmartIcon(Icon icon , String tooltip) {
		this.icon = icon;
		this.tooltip = tooltip;

		this.size = new Dimension(icon.getIconWidth(), icon.getIconHeight());
	//	log.info("" + size);

	}

	private void drawImage() {
		Assert.notNull(size, "keine grš§e");
		image = new BufferedImage(size.width, size.height,
				BufferedImage.TYPE_INT_ARGB);

		graphics = image.getGraphics();
		if (icon != null)
			icon.paintIcon(null, graphics, 0, 0);

		setBackground(Color.LIGHT_GRAY);
		// graphics.drawRect(0, 0, size.width - 1, size.height - 1);
		// String string = "" + size.width + "," + size.height;
		// graphics.drawChars(string.toCharArray(), 0, string.length(), 0, 10);
		for (PictureElement pe : pictureElements) {
	//		log.info("drawing " + pe.getTooltip());
			pe.draw(graphics);

		}
		int pos = 0;
		for (PictureElement pe : accents) {
	//		log.info("drawing " + pe.getTooltip());
			PictureElement deepClone = pe.deepClone();
			deepClone.move(pos, 0);
			pos += 5;
			deepClone.draw(graphics);
		}
	}

	public String getTooltip() {
		StringBuffer buf = new StringBuffer(tooltip);
		for (PictureElement pe : accents)
			buf.append(pe.getTooltip());
		return tooltip;
	}

	// public void drawRect(Point lefttop, Point rightbotton) {
	//
	// graphics.drawRect(lefttop.x, lefttop.y, rightbotton.x-lefttop.x,
	// rightbotton.y-lefttop.y);
	// }
	//
	// public void drawLine(Point begin, Point end) {
	//
	// graphics.drawRect(begin.x, begin.y, end.x-begin.x, end.y-begin.y);
	// }
	//
	private void setBackground(Color color) {
		this.background = color;

		graphics.setColor(color);

		graphics.fillRect(0, 0, size.width, size.height);
		graphics.setColor(Color.BLACK);

	}

	private Color background;

	public Image getIcon() {
		drawImage();
		return makeColorTransparent(background);
	}

	private Image makeColorTransparent(final Color color) {
		ImageFilter filter = new RGBImageFilter() {

			// the color we are looking for... Alpha bits are set to opaque
			public int markerRGB = color.getRGB() | 0xFF000000;

			public final int filterRGB(int x, int y, int rgb) {
				if ((rgb | 0xFF000000) == markerRGB) {
					// Mark the alpha bits as zero - transparent
					return 0x00FFFFFF & rgb;
				} else {
					// nothing to do
					return rgb;
				}
			}
		};

		ImageProducer ip = new FilteredImageSource(image.getSource(), filter);
		return Toolkit.getDefaultToolkit().createImage(ip);
	}

	public void addPictureElement(PictureElement pe) {

		pictureElements.add(pe);
	}

	public void addAccent(PictureElement accent) {

		accents.add(accent);
	}

	public void removeAccent(PictureElement accent) {

		accents.remove(accent);
	}
}
