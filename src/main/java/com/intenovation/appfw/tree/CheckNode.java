package com.intenovation.appfw.tree;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import com.intenovation.appfw.icon.Achtung;
import com.intenovation.appfw.icon.PictureElement;
import com.intenovation.appfw.icon.SmartIcon;
import com.intenovation.appfw.inversemv.CheckboxModel;
import com.intenovation.appfw.inversemv.CheckboxView;
import com.intenovation.appfw.inversemv.Model;
import com.intenovation.appfw.inversemv.ParentModel;
import com.intenovation.appfw.inversemv.ParentView;
import com.intenovation.appfw.inversemv.View;
import com.intenovation.appfw.inversemv.internal.AbstractView;

public class CheckNode extends DefaultMutableTreeNode implements CheckboxView,
		View, ParentView {
	public final static int DIG_IN_SELECTION = 4;
	static private Logger log = Logger.getLogger(AbstractView.class.getName());
	public final static int SINGLE_SELECTION = 0;

	private	SmartIcon icon;

	private Dimension iconSize;

	protected boolean isSelected;

	Model model;

	private ParentModel parent;

	protected int selectionMode;

	public CheckNode(ParentModel parent, Model model) {
		super(null, model instanceof ParentModel);
		this.isSelected = false;
		setSelectionMode(DIG_IN_SELECTION);
		this.parent = parent;
		this.model = model;
	}

	@Override
	public void addAccent(PictureElement accent) {

		if (icon == null)
			icon = new SmartIcon(getIconSize(), super.getUserObject()
					.toString());
		icon.addAccent(accent);
		nodeChanged();

	}

	@Override
	public View addChild(Model child) {
		CheckNode newChild = new CheckNode((ParentModel) model, child);
		super.add(newChild);

		nodeStructureChanged();
		return newChild;
	}

	// public void setSelected(boolean isSelected) {
	// this.isSelected = isSelected;
	//
	// if ((selectionMode == DIG_IN_SELECTION) && (children != null)) {
	// Enumeration e = children.elements();
	// while (e.hasMoreElements()) {
	// CheckNode node = (CheckNode) e.nextElement();
	// node.setSelected(isSelected);
	// }
	// }
	// }

	@Override
	public void error(String message) {
		warning(message);

	}

	@Override
	public void error(Throwable e) {
		warning(e.getMessage());

	}

	public SmartIcon getIcon() {

		return icon;
	}

	@Override
	public Dimension getIconSize() {
		if (iconSize == null)
			iconSize = new Dimension(15, 15);
		return iconSize;
	}

	public int getSelectionMode() {
		return selectionMode;
	}

	@Override
	public void info(String message) {
		Window.getInstance().addMessage(message);

	}

	public boolean isSelected() {
		return isSelected;
	}

	public boolean needsCheckbox() {
		return model instanceof CheckboxModel;
	}

	private void nodeChanged() {
		DefaultTreeModel model2 = (DefaultTreeModel) Window.getInstance().tree
				.getModel();

		model2.nodeChanged(this);
	}

	private void nodeStructureChanged() {
		DefaultTreeModel model2 = (DefaultTreeModel) Window.getInstance().tree
				.getModel();

		Enumeration preorderEnumeration = this.preorderEnumeration();

		List<TreePath> expandedPaths = new ArrayList<TreePath>();
		while (preorderEnumeration.hasMoreElements()) {
			DefaultMutableTreeNode nextElement = (DefaultMutableTreeNode) preorderEnumeration
					.nextElement();
			TreePath path = new TreePath(nextElement.getPath());
			if (Window.getInstance().tree.isExpanded(path)) {
				expandedPaths.add(path);
			}
		}
		model2.nodeStructureChanged(this);
		// model2.nodeChanged(this);

		for (TreePath path : expandedPaths) {
			Window.getInstance().tree.expandPath(path);

		}

	}

	@Override
	public void none(String message) {
		Window.getInstance().addMessage(message);

	}

	@Override
	public void notifyMyParent() {

		Achtung achtung = new Achtung("Notification");
		addAccent(achtung);
		parent.getView().addAccent(achtung);
		parent.childHasChanged(model);
		removeAccent(achtung);
		parent.getView().removeAccent(achtung);
	}

	@Override
	public void removeAccent(PictureElement accent) {
		icon.removeAccent(accent);
		nodeChanged();
	}

	@Override
	public void removeChild(Model child) {
		//log.info("remove child:" + child);
		Enumeration children2 = super.children();
		while (children2.hasMoreElements()) {
			CheckNode checknodechild = (CheckNode) children2.nextElement();
			if (checknodechild.model == child) {
				super.remove(checknodechild);
				//log.info("removeChild erledigt:");
				return;
			} else {
				//log.info("No Hit for remove :" + checknodechild.model);
			}
		}
		throw new RuntimeException("Failed to remove " + child);
	}

	@Override
	public void setChecked(boolean checked) {
		this.isSelected = checked;
		((CheckboxModel) model).setChecked(checked);
		nodeChanged();

	}

	@Override
	public void setIcon(SmartIcon icon) {

		this.icon = icon;
	}

	 
	@Override
	public void setName(String name) {
		super.setUserObject(name);
		nodeChanged();
	}

	public void setSelectionMode(int mode) {
		selectionMode = mode;
	}

	@Override
	public void warning(String message) {
		Achtung warnung = new Achtung(message);

		getIcon().addAccent(warnung);
		this.nodeChanged();
		Window.getInstance().addMessage(message);

	}

	@Override
	public void warning(Throwable e) {
		warning(e.getMessage());

	}

	// If you want to change "isSelected" by CellEditor,
	/*
	 * public void setUserObject(Object obj) { if (obj instanceof Boolean) {
	 * setSelected(((Boolean)obj).booleanValue()); } else {
	 * super.setUserObject(obj); } }
	 */

}
