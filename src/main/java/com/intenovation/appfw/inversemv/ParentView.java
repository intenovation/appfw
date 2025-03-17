package com.intenovation.appfw.inversemv;

public interface ParentView extends View {
 
	/**
	 * The model adds a child that has to be displayed
	 * during this call another view gets set at the model
	 * 
	 * @param child
	 */
	View addChild(Model child);

	/**
	 * The model removes a child that has not to be displayed anymore
	 * 
	 * @param child
	 */
	void removeChild(Model child);
}
