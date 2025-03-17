package com.intenovation.appfw.inversemv.internal;

import java.util.logging.Logger;

import com.intenovation.appfw.icon.Achtung;
import com.intenovation.appfw.inversemv.Model;
import com.intenovation.appfw.inversemv.ParentModel;

public abstract class AbstractView implements
		com.intenovation.appfw.inversemv.View {

	private ParentModel parent;
	private Model model;
	static private Logger log = Logger.getLogger(AbstractView.class.getName());

	public AbstractView(ParentModel parent, Model model) {
		super();

		this.parent = parent;
		this.model = model;
	}

	@Override
	public void notifyMyParent() {
		// throw new RuntimeException("Dispatch sollte das machen");
		Achtung achtung = new Achtung("Notification");
		addAccent(achtung);
		parent.getView().addAccent(achtung);
		parent.childHasChanged(model);
		removeAccent(achtung);
		parent.getView().removeAccent(achtung);

	}

}
