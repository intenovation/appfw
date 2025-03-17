package com.intenovation.appfw.tray;

import java.awt.Dimension;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import com.intenovation.appfw.icon.SmartIcon;
import com.intenovation.appfw.inversemv.ActionModel;
import com.intenovation.appfw.inversemv.Model;
import com.intenovation.appfw.inversemv.ParentModel;
import com.intenovation.appfw.inversemv.View;
import com.intenovation.appfw.thread.IntenovationThreadPool;

public class ActionViewImpl extends AbstractTrayView implements View {
	MenuItem menuItem;

	public ActionViewImpl(ParentModel parent, final ActionModel model,
			TrayIcon trayIcon, String appname) {
		super(parent, model, trayIcon, appname);

		menuItem = new MenuItem();

		menuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				IntenovationThreadPool.invokeLater("User action",
						new Runnable() {
							public void run() {
								model.action();
							}
						});
			}
		});
		//model.setView(this);
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

}
