package com.intenovation.appfw.inversemv.models;

import java.util.logging.Logger;

import com.intenovation.appfw.inversemv.CheckboxModel;
import com.intenovation.appfw.inversemv.CheckboxView;
import com.intenovation.appfw.inversemv.View;

public class BasicCheckboxMenuEntry implements CheckboxModel {
	private final static Logger log = Logger
			.getLogger(BasicTextboxMenuEntry.class.getName());
	private String displayName;
	private boolean checked;

	@Override
	public String toString() {
		return "Checkbox "+ displayName
				+ " " + checked ;
	}

	public BasicCheckboxMenuEntry(String displayName) {
		super();
		this.displayName = displayName;
	}

	public String getDisplayName() {

		return displayName;
	}

	public boolean isLongRunningInit() {

		return false;
	}

	public void run() {
	}

	public void stop() {
	}

	@Override
	public View getView() {

		return view;
	}

	CheckboxView view;

	@Override
	public void setView(CheckboxView view) {
		this.view = view;
		view.setName(getDisplayName());
		view.setChecked(checked);
	}

	@Override
	public boolean isChecked() {
	 
		return checked;
	}

	@Override
	public void setChecked(boolean checked) {
		log.info("setChecked "+ displayName+" to "+ checked);
		if (this.checked != checked) {
			log.info("changing value of" + displayName + " to " + checked);
			this.checked = checked;
			// Hack um refresh zu erreichen
			// view.setName(getDisplayName());
			view.setChecked(checked);
			view.notifyMyParent();
		}
	
	}
}
