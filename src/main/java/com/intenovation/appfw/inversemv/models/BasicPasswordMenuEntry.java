package com.intenovation.appfw.inversemv.models;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import com.intenovation.appfw.inversemv.ActionModel;
import com.intenovation.appfw.inversemv.View;

public class BasicPasswordMenuEntry extends BasicTextboxMenuEntry {

	public BasicPasswordMenuEntry(String displayName) {
		super(displayName);

	}

	public String getDisplayName() {
		String displayText = "click to Enter";
		if (text != null)
			displayText = "**************";

		return displayName + ":" + displayText;
	}

}
