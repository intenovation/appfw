package com.intenovation.appfw.inversemv.models;

import java.io.File;
import java.util.logging.Logger;

import javax.swing.JFileChooser;

import com.intenovation.appfw.inversemv.ActionModel;
import com.intenovation.appfw.inversemv.ParentView;
import com.intenovation.appfw.inversemv.View;

public class FileSelect implements ActionModel {

	private static Logger log = Logger.getLogger(FileSelect.class.getName());
	String name;

	public FileSelect(String name, File file) {
		super();
		this.name = name;
		this.file = file;
	}

	File file;

	public File getFile() {
		return file;
	}

	public void setFile(File file) {
		this.file = file;
	}

	@Override
	public void action() {
		final JFileChooser fc = new JFileChooser();
		fc.setCurrentDirectory(new java.io.File("."));
		fc.setDialogTitle(name);
		fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		//
		// disable the "All files" option.
		//
		fc.setAcceptAllFileFilterUsed(false);
		//

		int returnVal = fc.showOpenDialog(null);

		if (returnVal == JFileChooser.APPROVE_OPTION) {
			file = fc.getSelectedFile();
			// This is where a real application would open the file.
			log.info("Opening: " + file.getName() + ".");
			view.setName(name + ": " + file);
			view.notifyMyParent();
		} else {
			log.info("Open command cancelled by user.");
		}

	}

	@Override
	public boolean isLongRunningInit() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub

	}

	@Override
	public void run() {
		// TODO Auto-generated method stub

	}

	private View view;

	public void setView(View view) {
		this.view = view;
		view.setName(name + ": " + file);
	}

	@Override
	public View getView() {

		return view;
	}

}
