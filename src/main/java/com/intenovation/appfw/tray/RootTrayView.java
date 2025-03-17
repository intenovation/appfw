package com.intenovation.appfw.tray;

import java.awt.AWTException;
import java.awt.Dimension;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.intenovation.appfw.icon.PictureElement;
import com.intenovation.appfw.icon.SmartIcon;
import com.intenovation.appfw.inversemv.ParentModel;
import com.intenovation.appfw.inversemv.ParentView;
import com.intenovation.appfw.tree.Window;

public class RootTrayView extends ParentViewImpl implements ParentView {
	static Logger log = Logger.getLogger(RootTrayView.class.getName());
	SystemTray tray = SystemTray.getSystemTray();

	// TrayIcon trayIcon;
	// String appname;
	// PopupMenu menu;

	public void setName(String name) {
		appname = name;

	}

	public Dimension getIconSize() {
		if (tray == null) {
			log.warning("tray==null");
			tray = SystemTray.getSystemTray();
		}
		if (tray == null) {
			log.warning("tray==null again");
			tray = SystemTray.getSystemTray();
		}
		return tray.getTrayIconSize();
	}

	private void makeExit() {

		MenuItem exitItem = new MenuItem("Exit");
		exitItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				tray.remove(trayIcon);
				getModel().stop();
				System.exit(0);
			}
		});
		getMenu().add(exitItem);
		MenuItem windowItem = new MenuItem("Open Window");
		windowItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Window.getInstance().setVisible(true);
			}
		});
		getMenu().add(windowItem);

		getMenu().addSeparator();
	}

	SmartIcon icon;

	public void setIcon(SmartIcon icon) {
		this.icon = icon;
		trayIcon = new TrayIcon(icon.getIcon());
		// menu = new PopupMenu();
		trayIcon.setPopupMenu((PopupMenu) getMenu());
		trayIcon.setToolTip(icon.getTooltip());
		try {
			tray.add(trayIcon);
		} catch (AWTException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		makeExit();
	}

	@Override
	public void addAccent(PictureElement accent) {

		icon.addAccent(accent);
		trayIcon.setImage(icon.getIcon());
		trayIcon.setToolTip(icon.getTooltip());
	}

	@Override
	public void removeAccent(PictureElement accent) {
		icon.removeAccent(accent);

	}

	public RootTrayView(ParentModel model) {
		super(model, new PopupMenu());

		if (!SystemTray.isSupported()) {
			log.log(Level.SEVERE, "SystemTray is not supported");
			throw new RuntimeException("SystemTray is not supported");
		}
	}

}
