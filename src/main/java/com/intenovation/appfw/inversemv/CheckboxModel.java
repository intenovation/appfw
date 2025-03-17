package com.intenovation.appfw.inversemv;


public interface CheckboxModel extends Model {
	void setView(CheckboxView view);
	boolean isChecked();
	void setChecked(boolean checked);
}
