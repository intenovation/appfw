package com.intenovation.appfw.tray;

import java.awt.CheckboxMenuItem;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.TrayIcon;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.logging.Logger;

import com.intenovation.appfw.icon.SmartIcon;
import com.intenovation.appfw.inversemv.CheckboxModel;
import com.intenovation.appfw.inversemv.CheckboxView;
import com.intenovation.appfw.inversemv.ParentModel;

public class CheckboxViewImpl extends AbstractTrayView implements CheckboxView {
	CheckboxMenuItem menuItem;
	static private Logger log = Logger.getLogger(CheckboxViewImpl.class
			.getName());

	public CheckboxViewImpl(ParentModel model,
			final CheckboxModel checkboxModel, TrayIcon trayIcon, String appname) {
		super(model, checkboxModel, trayIcon, appname);
		menuItem = new CheckboxMenuItem();
		menuItem.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				log.info("Event" + e);
				int cb1Id = e.getStateChange();
				if (cb1Id == ItemEvent.SELECTED) {
					checkboxModel.setChecked(true);
				} else {
					checkboxModel.setChecked(false);
				}
				log.info("Checked" + checkboxModel.isChecked());
			}
		});
		//checkboxModel.setView(this);
	}

	@Override
	public void setName(String name) {
		menuItem.setLabel(name);
	}

	@Override
	public Dimension getIconSize() {
		return null;
	}

	@Override
	public void setIcon(SmartIcon icon) {
		throw new RuntimeException("nicht mšglich");
	}

	public MenuItem getMenu() {

		return menuItem;
	}

	@Override
	public void setChecked(boolean checked) {
		menuItem.setState(checked);

	}

}
