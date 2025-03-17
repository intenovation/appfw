package com.intenovation.appfw.inversemv;

import java.awt.MenuComponent;


/**
 * Dieses Modell kennt im gegensatz zum Klassischen MVC seine View.
 * Dafuer kennt die View das Modell nicht :)
 * @author jens
 *
 */
public interface Model extends Runnable {
	/**
	 * setzt die View des Modells. Wenn es mehrere gibt, wird Hier ein
	 * dispatcher gesetzt
	 * 
	 * @param view
	 */
//	void setView(View view);

	/**
	 * True, wenn Run aufgerufen werden soll
	 * 
	 * @return
	 */
	boolean isLongRunningInit();

	/**
	 * wird beim Beenden der anwendung aufgerufen. Run dagegen nach dem Start.
	 */
	void stop();

	View getView();

	

}