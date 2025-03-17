package com.intenovation.appfw.tree;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.UIManager;
import javax.swing.plaf.ColorUIResource;

public class TreeLabel extends JLabel {
    boolean isSelected;

    boolean hasFocus;

    public TreeLabel() {
    }

    public void setBackground(Color color) {
      if (color instanceof ColorUIResource)
        color = null;
      super.setBackground(color);
    }

    public void paint(Graphics graphics) {
      String str;
      if ((str = getText()) != null) {
        if (0 < str.length()) {
          if (isSelected) {
            graphics.setColor(UIManager
                .getColor("Tree.selectionBackground"));
          } else {
            graphics.setColor(UIManager.getColor("Tree.textBackground"));
          }
          Dimension dimensionPreferedSize = getPreferredSize();
          int imageOffset = 0;
          Icon currentI = getIcon();
          if (currentI != null) {
            imageOffset = currentI.getIconWidth()
                + Math.max(0, getIconTextGap() - 1);
          }
          graphics.fillRect(imageOffset, 0, dimensionPreferedSize.width - 1 - imageOffset,
              dimensionPreferedSize.height);
          if (hasFocus) {
            graphics.setColor(UIManager
                .getColor("Tree.selectionBorderColor"));
            graphics.drawRect(imageOffset, 0, dimensionPreferedSize.width - 1 - imageOffset,
                dimensionPreferedSize.height - 1);
          }
        }
      }
      super.paint(graphics);
    }

    public Dimension getPreferredSize() {
      Dimension retDimension = super.getPreferredSize();
      if (retDimension != null) {
        retDimension = new Dimension(retDimension.width + 3,
            retDimension.height);
      }
      return retDimension;
    }

    public void setSelected(boolean isSelected) {
      this.isSelected = isSelected;
    }

    public void setFocus(boolean hasFocus) {
      this.hasFocus = hasFocus;
    }
  }