package com.intenovation.appfw.inversemv.models;

import com.intenovation.appfw.inversemv.ActionModel;
import com.intenovation.appfw.inversemv.View;

public class RunNow implements ActionModel {
	public Runnable getEntryToRun() {
		return entryToRun;
	}

	Runnable entryToRun;
	String displayname;

	public String getDisplayName() {

		return displayname;
	}

	public RunNow(Runnable entryToRun, String displayname) {
		super();
		this.entryToRun = entryToRun;
		this.displayname = displayname;
	}

	public boolean isLongRunningInit() {

		return false;
	}

	public void stop() {

	}

	public void run() {

	}

	public void action() {
		entryToRun.run();

	}

	private View view;

	public void setView(View view) {
		this.view = view;
		view.setName(getDisplayName());
	}

	@Override
	public View getView() {

		return view;
	}

}
