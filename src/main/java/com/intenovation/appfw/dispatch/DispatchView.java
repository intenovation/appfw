package com.intenovation.appfw.dispatch;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.logging.Logger;

import com.intenovation.appfw.icon.PictureElement;
import com.intenovation.appfw.icon.SmartIcon;
import com.intenovation.appfw.inversemv.ActionModel;
import com.intenovation.appfw.inversemv.CheckboxModel;
import com.intenovation.appfw.inversemv.CheckboxView;
import com.intenovation.appfw.inversemv.Model;
import com.intenovation.appfw.inversemv.ParentModel;
import com.intenovation.appfw.inversemv.ParentView;
import com.intenovation.appfw.inversemv.View;
import com.intenovation.appfw.thread.IntenovationThreadPool;

public class DispatchView implements ParentView, View, CheckboxView {
	static private Logger log = Logger.getLogger(DispatchView.class.getName());
	private Model model;
	private ArrayList<View> views = new ArrayList<View>();

	public View getView(Class clazz) {
		for (View view : views) {
			if (clazz.isInstance(view))
				return view;
		}
		return null;
	}

	public void addView(View target) {
		views.add(target);
	}

	public DispatchView(Model model) {
		super();
		this.model = model;

	}

	String name;

	@Override
	public void setName(String name) {
		this.name = name;
		for (View view : views) {
			view.setName(name);
		}

	}

	@Override
	public Dimension getIconSize() {
		Dimension iconSize = null;
		for (View view : views) {
			Dimension iconSizeNew = view.getIconSize();
			if (iconSizeNew != null) {
				if (iconSize != null) {
					if (iconSize.height != iconSizeNew.height) {
						log.severe("Iconsizes " + iconSize + " " + iconSizeNew);
					}
					if (iconSize.width != iconSizeNew.width) {
						log.severe("Iconsizes " + iconSize + " " + iconSizeNew);
					}
				}
				iconSize = iconSizeNew;
			}
		}
		return iconSize;
	}

	@Override
	public void setIcon(SmartIcon icon) {
		for (View view : views) {
			view.setIcon(icon);
		}

	}

	@Override
	public void error(Throwable e) {
		for (View view : views) {
			view.error(e);
		}

	}

	@Override
	public void error(String message) {
		for (View view : views) {
			view.error(message);
		}

	}

	@Override
	public void info(String message) {
		for (View view : views) {
			view.info(message);
		}

	}

	@Override
	public void none(String message) {
		for (View view : views) {
			view.none(message);
		}

	}

	@Override
	public void warning(Throwable e) {
		for (View view : views) {
			view.warning(e);
		}

	}

	@Override
	public void warning(String message) {
		for (View view : views) {
			view.warning(message);
		}

	}

	@Override
	public void notifyMyParent() {

		// wir haben gerade keine parent referenz zu hand
		for (final View view : views) {
			if (model.isLongRunningInit()) {
				IntenovationThreadPool.invokeLater("notifyMyParent",
						new Runnable() {

							@Override
							public void run() {
								view.notifyMyParent();

							}

						});

			} else {
				view.notifyMyParent();

			}
			break; // Der Parent muss nur einmal informiert werden!!!
		}

	}

	@Override
	public View addChild(Model child) {

		DispatchView childView = new DispatchView(child);
		for (View view : views) {
			View addChild = ((ParentView) view).addChild(child);
			childView.addView(addChild);
		}
		setView(child, childView);
		IntenovationThreadPool.invokeLaterIfWanted("Init "+ childView.name, child);
		return childView;

	}

	private void setView(Model child, DispatchView childView) {
		if (child instanceof ParentModel) {
			log.finest("ParentModel");
			ParentModel childModel = (ParentModel) child;
			childModel.setView(childView);

		}
		if (child instanceof ActionModel) {
			log.finest("ActionModel");
			ActionModel actionModel = (ActionModel) child;
			actionModel.setView(childView);

		}
		if (child instanceof CheckboxModel) {
			log.finest("CheckboxModel");
			CheckboxModel checkboxModel = (CheckboxModel) child;
			checkboxModel.setView(childView);
		}

	}

	@Override
	public void removeChild(Model child) {
		for (View view : views) {
			((ParentView) view).removeChild(child);
		}

	}

	@Override
	public void setChecked(boolean checked) {
		for (View view : views) {
			((CheckboxView) view).setChecked(checked);
		}

	}

	@Override
	public void addAccent(PictureElement accent) {
		for (View view : views) {
			view.addAccent(accent);
		}

	}

	@Override
	public void removeAccent(PictureElement accent) {
		for (View view : views) {
			view.removeAccent(accent);
		}

	}

}
