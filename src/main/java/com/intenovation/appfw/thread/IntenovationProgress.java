package com.intenovation.appfw.thread;

import javax.swing.JPanel;
import javax.swing.JProgressBar;

import com.intenovation.appfw.tree.Window;

public class IntenovationProgress extends JPanel implements Runnable {
	// JLabel name =new JLabel();
	String name;

	public IntenovationProgress(String name, Runnable run) {
		super();
		this.name = name;
		this.run = run;
		progress.setValue(0);
		progress.setStringPainted(true);
		progress.setString(name);
		add(progress);
		setVisible(true);
		Window.getInstance().addProgress(this);
	}

	@Override
	public String toString() {
		return name;
	}

	JProgressBar progress = new JProgressBar(0, 100);
	// JButton cancel =new JButton("cancel");
	Runnable run;

	@Override
	public void run() {
		progress.setValue(50);
		try {
			run.run();
		} finally {
			progress.setValue(100);
		}
	}
	public int getPercent(){
		return progress.getValue();
	}
}
