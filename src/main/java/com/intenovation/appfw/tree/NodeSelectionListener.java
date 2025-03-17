package com.intenovation.appfw.tree;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.logging.Logger;

import javax.swing.JTree;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import com.intenovation.appfw.inversemv.ActionModel;
import com.intenovation.appfw.inversemv.models.BasicTextboxMenuEntry;
import com.intenovation.appfw.thread.IntenovationThreadPool;

class NodeSelectionListener extends MouseAdapter {
	JTree tree;
	private final static Logger log = Logger
			.getLogger(NodeSelectionListener.class.getName());

	NodeSelectionListener(JTree tree) {
		this.tree = tree;
	}

	public void mouseClicked(MouseEvent e) {

		int x = e.getX();
		int y = e.getY();
		int row = tree.getRowForLocation(x, y);
		TreePath path = tree.getPathForRow(row);
		// TreePath path = tree.getSelectionPath();
		if (path != null) {

			Object lastPathComponent = path.getLastPathComponent();
			log.info("clicked " + lastPathComponent);
			if (lastPathComponent instanceof CheckNode) {
				CheckNode node = (CheckNode) lastPathComponent;
				if (node.needsCheckbox()) {
					boolean isSelected = !(node.isSelected());
					node.setChecked(isSelected);
					if (node.getSelectionMode() == CheckNode.DIG_IN_SELECTION) {
						if (isSelected) {
							tree.expandPath(path);
						} else {
							tree.collapsePath(path);
						}
					}
				} else if (node.model instanceof ActionModel) {
					final ActionModel action = (ActionModel) node.model;
					IntenovationThreadPool.invokeLater(
							"Action Clicked " + path, new Runnable() {

								@Override
								public void run() {
									action.action();
								}
							});

				}
				((DefaultTreeModel) tree.getModel()).nodeChanged(node);
			}

			// I need revalidate if node is root. but why?
			if (row == 0) {
				tree.revalidate();
				tree.repaint();
			}
		}
	}
}
