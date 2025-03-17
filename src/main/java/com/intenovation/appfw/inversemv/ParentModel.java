package com.intenovation.appfw.inversemv;

public interface ParentModel extends Model {
	void childHasChanged(Model child);

	void setView(ParentView view);
}
