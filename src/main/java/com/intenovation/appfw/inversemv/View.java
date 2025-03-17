package com.intenovation.appfw.inversemv;

import java.awt.Dimension;
import java.awt.Image;
import java.awt.MenuComponent;

import com.intenovation.appfw.icon.PictureElement;
import com.intenovation.appfw.icon.SmartIcon;

public interface View {
	/**
	 * the model sets its name initially oder changes it.
	 * 
	 * @param name
	 */
	void setName(String name);

	/**
	 * wenn das Modell ein Icon Anzeigen wollte, welche groesse muesste es haben?
	 * Wenn null, dann ist es nicht moeglich, ein Icon anzuzeigen
	 * 
	 * @return
	 */
	Dimension getIconSize();

	/**
	 * Icon der Anwendung
	 * 
	 * @return
	 */
	void setIcon(SmartIcon icon);
	/**
	 * Kleiner Zustandshinweis im Icon der Anwendung
	 * 
	 * @return
	 */
	void addAccent(PictureElement accent);
	/**
	 * entfernt Kleinen Zustandshinweis im Icon der Anwendung
	 * 
	 * @return
	 */
	void removeAccent(PictureElement accent);
	/**
	 * the model uses this to display an error
	 * 
	 * @param e
	 */
	void error(Throwable e);

	/**
	 * the model uses this to display an error
	 * 
	 * @param e
	 */
	void error(String message);

	/**
	 * the model uses this to display an error
	 * 
	 * @param e
	 */
	void info(String message);

	/**
	 * the model uses this to display an info
	 * 
	 * @param e
	 */
	void none(String message);

	/**
	 * the model uses this to display warning
	 * 
	 * @param e
	 */
	void warning(Throwable e);

	/**
	 * the model uses this to display warning
	 * 
	 * @param e
	 */
	void warning(String message);
	void notifyMyParent();

	//MenuComponent getMenu();
}
