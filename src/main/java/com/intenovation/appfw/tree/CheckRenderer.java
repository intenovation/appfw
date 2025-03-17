package com.intenovation.appfw.tree;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.plaf.ColorUIResource;
import javax.swing.tree.TreeCellRenderer;

import com.intenovation.appfw.inversemv.CheckboxModel;

class CheckRenderer extends JPanel implements TreeCellRenderer {
	private static final Dimension PREFERRED_SIZE_ZERO = new Dimension(0, 0);

	protected JCheckBox check;

	protected TreeLabel label;

	public CheckRenderer() {
		setLayout(null);
		add(check = new JCheckBox());
		add(label = new TreeLabel());
		check.setBackground(UIManager.getColor("Tree.textBackground"));
		check.setBackground(Color.WHITE);
		label.setForeground(UIManager.getColor("Tree.textForeground"));
		label.setBackground(Color.WHITE);
		this.setBackground(Color.WHITE);
	}

	public Component getTreeCellRendererComponent(JTree tree, Object value,
			boolean isSelected, boolean expanded, boolean leaf, int row,
			boolean hasFocus) {
		String stringValue = tree.convertValueToText(value, isSelected,
				expanded, leaf, row, hasFocus);
		setEnabled(tree.isEnabled());
		if (value instanceof CheckNode) {
			CheckNode checknode = (CheckNode) value;
			//checknode.setIconSize(label.getPreferredSize());
			check.setSelected(checknode.isSelected());

			if (checknode.getIcon() != null) {
				Icon icon = new ImageIcon(checknode.getIcon().getIcon());
				label.setIcon(icon);
				this.setToolTipText(checknode.getIcon().getTooltip());
			} else
				setIcon(expanded, leaf);
			check.setVisible(checknode.needsCheckbox());
			// check.setPreferredSize(PREFERRED_SIZE_ZERO);
		} else {
			check.setVisible(false);
			// check.setPreferredSize(PREFERRED_SIZE_ZERO);
			setIcon(expanded, leaf);
		}
		label.setFont(tree.getFont());
		label.setText(stringValue);
		label.setSelected(isSelected);
		label.setFocus(hasFocus);

		return this;
	}

	private void setIcon(boolean expanded, boolean leaf) {
		label.setIcon(null);
		if (true)
			return;
		// Sieht ohne icons besser aus
		if (leaf) {
			label.setIcon(UIManager.getIcon("Tree.leafIcon"));
		} else if (expanded) {
			label.setIcon(UIManager.getIcon("Tree.openIcon"));
		} else {
			label.setIcon(UIManager.getIcon("Tree.closedIcon"));
		}
	}

	public Dimension getPreferredSize() {
		Dimension d_check = check.getPreferredSize();
		Dimension d_label = label.getPreferredSize();
		d_label.height = 22;

		return new Dimension(d_check.width + d_label.width,
				(d_check.height < d_label.height ? d_label.height
						: d_check.height));
	}

	public void doLayout() {
		Dimension d_check = check.getPreferredSize();
		Dimension d_label = label.getPreferredSize();
		int y_check = 0;
		int y_label = 0;
		if (d_check.height < d_label.height) {
			y_check = (d_label.height - d_check.height) / 2;
		} else {
			y_label = (d_check.height - d_label.height) / 2;
		}
		check.setLocation(0, y_check);
		check.setBounds(0, y_check, d_check.width, d_check.height);
		label.setLocation(d_check.width, y_label);
		label.setBounds(d_check.width, y_label, d_label.width, d_label.height);
	}

	public void setBackground(Color color) {
		if (color instanceof ColorUIResource)
			color = null;
		super.setBackground(color);
	}

}
