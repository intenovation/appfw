package com.intenovation.appfw.tree;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.LayoutManager;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.logging.Logger;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.SpringLayout;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import com.intenovation.appfw.Assert;
import com.intenovation.appfw.thread.IntenovationProgress;
import javax.swing.*;

public class Window extends JFrame {
	static Window instance;
	static Logger log = Logger.getLogger(Window.class.getName());
	private static final int PROGRESS_COUNT = 36;

	public static Window getInstance() {
		if (instance == null)
			instance = new Window();
		return instance;
	}

	public static void main(String... strings) {
		new Window();
	}

	Container content;
	IntenovationProgress last;
	LayoutManager mgr;
	DefaultTreeModel model;
	DefaultMutableTreeNode node;
	JPanel progressPanel;
	JTextArea textArea;
	IntenovationProgress top;
	JTree tree;

	private Window() {

		super("Intenovation Apps");

		WindowUtilities.setNativeLookAndFeel();
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent event) {
				setVisible(false);
			}
		});
		content = getContentPane();

		node = new DefaultMutableTreeNode("Intenovation Apps");
		tree = new JTree(node);
		tree.setCellRenderer(new CheckRenderer());
		tree.getSelectionModel().setSelectionMode(
				TreeSelectionModel.SINGLE_TREE_SELECTION);
		model = (DefaultTreeModel) tree.getModel();
		tree.addMouseListener(new NodeSelectionListener(tree));
		content.add(new JScrollPane(tree), BorderLayout.CENTER);
		textArea = new JTextArea(3, 10);
		JScrollPane textPanel = new JScrollPane(textArea);
		content.add(textPanel, BorderLayout.SOUTH);
		progressPanel = new JPanel();
		mgr = new GridLayout(PROGRESS_COUNT, 1);
		// mgr.setVgap(0);
		// mgr.setHgap(0);

		// mgr = new SpringLayout();

		progressPanel.setLayout(mgr);
		// SpringUtilities.makeCompactGrid(progressPanel, //parent
		// PROGRESS_COUNT, 1,
		// 3, 3, //initX, initY
		// 3, 3); //xPad, yPad

		// progressPanel.setPreferredSize(new Dimension(200, 1000));
		// progressPanel.setPreferredSize((new IntenovationProgress("",
		// null)).getPreferredSize());
		// JScrollPane progressPanelScroll = new JScrollPane(progressPanel);
		progressPanel.setPreferredSize(new Dimension(410, 800));
		content.add(progressPanel, BorderLayout.EAST);
		setSize(1000, 1000);
		setExtendedState(MAXIMIZED_VERT);
		javax.swing.ToolTipManager.sharedInstance().registerComponent(tree);
	}

	public void add(CheckNode view) {
		log.info("Add" + view);
		node.add(view);
		model.nodeChanged(node);
		TreePath path = new TreePath(view.getPath());
		tree.expandPath(path);
	}

	void addMessage(String text) {
		textArea.append(text);
		textArea.append("\n");
	}

	public void addProgress(IntenovationProgress prog) {
		if (progressPanel.getComponentCount() == 0) {
			top = prog;
		}
		while (progressPanel.getComponentCount() >= PROGRESS_COUNT
				&& top != null && top.getPercent() == 100)

		{
			progressPanel.remove(0);
			top = (IntenovationProgress) progressPanel.getComponent(0);
		}
		if (progressPanel.getComponentCount() >= PROGRESS_COUNT) {
			for (int i = 1; i < progressPanel.getComponentCount(); i++) {
				 
					IntenovationProgress other = (IntenovationProgress) progressPanel
							.getComponent(i);
					if (other.getPercent() == 100)
						progressPanel.remove(i);
			 
			}
		}

		// if (progressPanel.getComponentCount() == 0) {
		// log.info("Top Constraint"+prog);
		// top = prog;
		// mgr.putConstraint(SpringLayout.NORTH, progressPanel, 1,
		// SpringLayout.NORTH, top);
		// mgr.putConstraint(SpringLayout.WEST, progressPanel, 1,
		// SpringLayout.WEST, top);
		// mgr.putConstraint(SpringLayout.EAST, progressPanel, 1,
		// SpringLayout.EAST, top);
		// last = top;
		// } else {
		// log.info("Last Constraint"+last+"->"+prog);
		// Assert.notNull(last, "last"+progressPanel.getComponentCount());
		// Assert.notNull(prog, "prog"+progressPanel.getComponentCount());
		// mgr.putConstraint(SpringLayout.SOUTH, last, 1, SpringLayout.NORTH,
		// prog);
		// mgr.putConstraint(SpringLayout.WEST, progressPanel, 1,
		// SpringLayout.WEST, prog);
		// mgr.putConstraint(SpringLayout.EAST, progressPanel, 1,
		// SpringLayout.EAST, prog);
		// last = prog;
		// }
		// Dimension preferredSize = prog.getPreferredSize();
		// preferredSize.width=progressPanel.getPreferredSize().width;
		progressPanel.add(prog);
		// if (progressPanel.getComponentCount() == 9) {
		// log.info("SOUTH Constraint"+prog);
		// mgr.putConstraint(SpringLayout.SOUTH, progressPanel, 1,
		// SpringLayout.SOUTH, prog);
		// }
		//
		// mgr.invalidateLayout(progressPanel);
		progressPanel.validate();
	}
}
