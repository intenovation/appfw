package com.intenovation.appfw.inversemv.models;

import com.intenovation.appfw.inversemv.CheckboxModel;
import com.intenovation.appfw.inversemv.CheckboxView;
import com.intenovation.appfw.inversemv.View;

public abstract class Work implements CheckboxModel{
	boolean checked = false;

	public boolean isChecked() {
		// TODO Auto-generated method stub
		return checked;
	}

	public void setChecked(boolean checked) {
		if (this.checked == checked)
			return;
//		String oldValue = "" + this.checked;
	this.checked = checked;
//		String newValue = "" + this.checked;
		view.setChecked(this.checked);
//		view.notifyMyParent();
	}

	private String displayName;

	public String getDisplayName() {

		return displayName;
	}

	public Work(String displayName) {
		super();
		this.displayName = displayName;
	}

	public abstract void run();

	@Override
	public String toString() {
		return "Step  " + displayName;
	}

	/**
	 * Run Wird vom Scheduler aufgerufen
	 */
	public boolean isLongRunningInit() {
		return false;
	}
	private CheckboxView view;

	public void setView(CheckboxView view) {
		this.view = view;
		view.setName(getDisplayName());
	}

	@Override
	public View getView() {

		return view;
	}
}
