package com.intenovation.appfw.inversemv.models;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import com.intenovation.appfw.inversemv.ActionModel;
import com.intenovation.appfw.inversemv.View;

public class BasicTextboxMenuEntry implements ActionModel {
	private final static Logger log = Logger
			.getLogger(BasicTextboxMenuEntry.class.getName());
	protected String displayName;
	protected String text;

	public BasicTextboxMenuEntry(String displayName) {
		super();
		this.displayName = displayName;
	}

	public String getDisplayName() {
		String displayText = "click to Enter";
		if (text != null)
			displayText = text;
		return displayName + ":" + displayText;
	}

	public String getText() {
		return text;
	}

	public boolean isLongRunningInit() {

		return false;
	}

	public void run() {
	}

	public void setText(String text) {
		if (text == null) {
			return;
		}
		String oldname = getDisplayName();
		String oldtext = this.text;
		this.text = text;
		String newName = getDisplayName();

		log.info("Rename:" + oldname + "newName" + newName + " text"
				+ this.text);
		 if (text != null && !text.equals(oldtext)&&view!=null) {
		  view.setName(text);
	  }
	}

	public void stop() {
	}

	@Override
	public View getView() {

		return view;
	}

	@Override
	public void action() {
		JFrame frame = new JFrame("Question");

		frame.setUndecorated(true);
		frame.setVisible(true);
		frame.setLocationRelativeTo(null);
		String oldText = getText();
		String message = JOptionPane.showInputDialog(frame, getDisplayName(),
				getText(), JOptionPane.QUESTION_MESSAGE);

		log.log(Level.INFO, "Got " + message);

		frame.dispose();

		log.log(Level.INFO, "set" + message + " on " + this);
		if (message != null && !message.equals(oldText)) {
			setText(message);
			log.log(Level.INFO, "get" + getText());
			view.setName(getDisplayName());
			view.notifyMyParent();
		}

	}

	View view;

	@Override
	public void setView(View view) {
		this.view = view;
		view.setName(getDisplayName());
	}
}
