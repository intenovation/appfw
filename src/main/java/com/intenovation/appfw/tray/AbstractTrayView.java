package com.intenovation.appfw.tray;

import java.awt.MenuComponent;
import java.awt.TrayIcon;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.intenovation.appfw.icon.PictureElement;
import com.intenovation.appfw.inversemv.Model;
import com.intenovation.appfw.inversemv.ParentModel;
import com.intenovation.appfw.inversemv.internal.AbstractView;

public abstract class AbstractTrayView extends AbstractView {
	@Override
	public void addAccent(PictureElement accent) {

	}

	@Override
	public void removeAccent(PictureElement accent) {

	}

	protected String appname;

	static private Logger log = Logger.getLogger(AbstractTrayView.class
			.getName());

	protected TrayIcon trayIcon;

	public AbstractTrayView(ParentModel parent, Model model, TrayIcon trayIcon,
			String appname) {
		super(parent, model);
		this.trayIcon = trayIcon;
		this.appname = appname;

	}

	public void error(Throwable e) {

		log.log(Level.SEVERE, e.getMessage(), e);
		trayIcon.displayMessage(appname, e.getMessage(),
				TrayIcon.MessageType.ERROR);
	}

	public void error(String message) {
		log.log(Level.SEVERE, message);
		trayIcon.displayMessage(appname, message, TrayIcon.MessageType.ERROR);

	}

	public void info(String message) {
		log.log(Level.INFO, message);
		trayIcon.displayMessage(appname, message, TrayIcon.MessageType.INFO);

	}

	public void none(String message) {
		log.log(Level.FINE, message);
		trayIcon.displayMessage(appname, message, TrayIcon.MessageType.NONE);

	}

	public void warning(Throwable e) {

		log.log(Level.WARNING, e.getMessage(), e);
		trayIcon.displayMessage(appname, e.getMessage(),
				TrayIcon.MessageType.WARNING);
	}

	public void warning(String message) {
		log.log(Level.WARNING, message);
		trayIcon.displayMessage(appname, message, TrayIcon.MessageType.WARNING);

	}

	public abstract MenuComponent getMenu();

}
