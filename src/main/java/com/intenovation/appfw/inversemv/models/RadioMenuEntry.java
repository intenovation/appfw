package com.intenovation.appfw.inversemv.models;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import com.intenovation.appfw.inversemv.CheckboxModel;
import com.intenovation.appfw.inversemv.Model;
import com.intenovation.appfw.inversemv.ParentModel;
import com.intenovation.appfw.inversemv.ParentView;
import com.intenovation.appfw.inversemv.View;

public class RadioMenuEntry<T> implements ParentModel {
	List<CheckboxModel> children;
	String displayName;
	T enumvalues[];

	Logger log = Logger.getLogger(RadioMenuEntry.class.getName());

	T selected;

	public RadioMenuEntry(String displayName, T[] enumvalues, T selected) {
		super();
		this.displayName = displayName;
		this.enumvalues = enumvalues;
		this.selected = selected;
		if (this.selected == null)
			throw new RuntimeException("Select an Entry, please");
	}

	public String displayName(T entry) {
		return entry.toString().replace("_", ".");
	}

	public List getChildren() {
		if (children == null) {
			children = new ArrayList<CheckboxModel>();
			if (this.selected == null)
				throw new RuntimeException("Select an Entry, please");
			for (T entry : enumvalues) {
				BasicCheckboxMenuEntry basicCheckboxMenuEntry = new BasicCheckboxMenuEntry(
						displayName(entry));
				children.add(basicCheckboxMenuEntry);
				view.addChild(basicCheckboxMenuEntry);
				if (this.selected.equals(entry)) {
					basicCheckboxMenuEntry.setChecked(true);
				}

				// basicCheckboxMenuEntry.addObserver(this);
			}

		}
		return children;
	}

	public String getDisplayName() {

		return displayName;
	}

	public T getSelected() {
		return selected;
	}

	public boolean isLongRunningInit() {

		return false;
	}

	public void run() {

	}

	public void setSelected(T selected) {
		String oldValue = "" + this.selected;
		this.selected = selected;
		String newValue = "" + this.selected;
		// setChanged();

	}

	public void childHasChanged(Model o) {
		log.info("update" + o);
		BasicCheckboxMenuEntry checkbox = (BasicCheckboxMenuEntry) o;
		if (checkbox.isChecked()) {
			log.info("number of children " + children.size());
			for (CheckboxModel child : children) {
				log.info("checking "
						+ ((BasicCheckboxMenuEntry) child).getDisplayName()
						+ "  " + checkbox.getDisplayName());
				if (!((BasicCheckboxMenuEntry) child).getDisplayName().equals(
						checkbox.getDisplayName())) {
					log.info("found unselected " + child);
					child.setChecked(false);
				}
			}
			for (T entry : enumvalues) {
				if (displayName(entry).equals(checkbox.getDisplayName())) {
					log.info("found selected " + entry);
					setSelected(entry);

				}
			}
		}
		view.notifyMyParent();
		// setChanged();
		// notifyObservers(new ChangedValueEvent());
	}

	public void stop() {
	}

	private ParentView view;

	public void setView(ParentView view) {
		this.view = view;
		view.setName(getDisplayName());
		getChildren();
	}

	@Override
	public View getView() {

		return view;
	}

}
