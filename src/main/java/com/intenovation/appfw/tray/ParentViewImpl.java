package com.intenovation.appfw.tray;

import java.awt.Dimension;
import java.awt.Image;
import java.awt.Menu;
import java.awt.TrayIcon;
import java.util.logging.Logger;

import com.intenovation.appfw.Assert;
import com.intenovation.appfw.dispatch.DispatchView;
import com.intenovation.appfw.icon.SmartIcon;
import com.intenovation.appfw.inversemv.ActionModel;
import com.intenovation.appfw.inversemv.CheckboxModel;
import com.intenovation.appfw.inversemv.Model;
import com.intenovation.appfw.inversemv.ParentModel;
import com.intenovation.appfw.inversemv.ParentView;
import com.intenovation.appfw.inversemv.View;
import com.intenovation.appfw.thread.IntenovationThreadPool;

public class ParentViewImpl extends AbstractTrayView implements ParentView {
	static private Logger log = Logger
			.getLogger(ParentViewImpl.class.getName());
	private Menu menu;
	ParentModel model;

	public ParentModel getModel() {
		return model;
	}

	public Menu getMenu() {
		return menu;
	}

	public void setName(String name) {
		log.finest("setName" + name);
		if (menu == null)
			throw new RuntimeException("menu==null setName");
		menu.setLabel(name);

	}

	/**
	 * Constructor für die Rootview
	 * 
	 * @param model
	 * @param menu
	 */
	protected ParentViewImpl(ParentModel model, Menu menu) {
		super(null, model, null, null);
		log.info("ParentViewImpl protected");
		this.model = model;

		this.menu = menu;
		if (menu == null)
			throw new RuntimeException("menu==null");
		// model.setView(this);
		// IntenovationThreadPool.invokeLaterIfWanted("init after setView",
		// model);
	}

	public ParentViewImpl(ParentModel parent, ParentModel model,
			TrayIcon trayIcon, String appname) {

		super(parent, model, trayIcon, appname);
		log.info("ParentViewImpl public");
		this.model = model;

		menu = new Menu();
		// model.setView(this);
		// IntenovationThreadPool.invokeLaterIfWanted("init after setView",
		// model);
	}

	/**
	 * Rootview überschreibt dies um anzuzeigen, dass dort ein Icon möglich ist.
	 * im Tree ist überall ein Icon möglich
	 */
	public Dimension getIconSize() {

		return null;
	}

	public void setIcon(SmartIcon icon) {
		// TODO Auto-generated method stub

	}

	// Um bei stop alle stoppen zu können
	// Set<com.intenovation.appfw.inversemv.View> childs = new
	// HashSet<com.intenovation.appfw.inversemv.View>();

	public View addChild(Model child) {
	//	log.info("addChild" + child);
		if (child instanceof ParentModel) {
	//		log.info("ParentModel");
			ParentModel childModel = (ParentModel) child;
			ParentViewImpl parentViewImpl = new ParentViewImpl(model,
					childModel, trayIcon, appname);

			menu.add(parentViewImpl.getMenu());
			return parentViewImpl;
		}
		if (child instanceof ActionModel) {
	//		log.info("ActionModel");
			ActionModel actionModel = (ActionModel) child;
			ActionViewImpl actionViewImpl = new ActionViewImpl(model,
					actionModel, trayIcon, appname);

			menu.add(actionViewImpl.getMenu());
			return actionViewImpl;
		}
		if (child instanceof CheckboxModel) {
//			log.info("CheckboxModel");
			CheckboxModel checkboxModel = (CheckboxModel) child;
			CheckboxViewImpl checkboxViewImpl = new CheckboxViewImpl(model,
					checkboxModel, trayIcon, appname);
			menu.add(checkboxViewImpl.getMenu());
			return checkboxViewImpl;
		}
		return null;
	}

	public void removeChild(Model child) {

		Assert.notNull(child, "nothing to remove");
		child.stop();
		com.intenovation.appfw.dispatch.DispatchView displatchview = (DispatchView) child
				.getView();
		if (displatchview != null) {

			AbstractTrayView traychild = (AbstractTrayView) displatchview
					.getView(AbstractTrayView.class);
			Assert.notNull(traychild, "no tray view");

			menu.remove(traychild.getMenu());
		}
	}

}
