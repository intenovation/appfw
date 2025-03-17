package com.intenovation.appfw.inversemv.models;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.intenovation.appfw.inversemv.Model;
import com.intenovation.appfw.inversemv.ParentModel;
import com.intenovation.appfw.inversemv.ParentView;
import com.intenovation.appfw.inversemv.View;

public abstract class WorkScheduler implements ParentModel {
	/**
	 * see java.util.Timer
	 * 
	 * @return
	 */
	protected abstract long period();

	String displayName;

	public String getDisplayName() {
		if (displayName == null)
			displayName = getBasicDisplayName();
		return displayName;
	}

	protected abstract String getBasicDisplayName();

	/**
	 * wird aufgerufen, wenn eine ausführung ansteht. Hier wird die arbeit
	 * zusammengesucht, jedesmal neu
	 * 
	 * @return
	 */
	protected abstract List<Work> getSteps();

	public boolean isLongRunningInit() {

		return true;
	}

	List<Model> menueEntires;
	Timer timer;
	private final static Logger log = Logger.getLogger(WorkScheduler.class
			.getClass().getName());
	TimerTask task;

	public synchronized void run() {
		if (menueEntires == null) {
			menueEntires = new ArrayList<Model>();

			task = new TimerTask() {
				boolean running = false;

				@Override
				public void run() {
					try {
						if (running)
							throw new RuntimeException("Already runing");
						running = true;
						// Steps wird nur einmal pro ausführung aufgerufen.
						// Wichtig ist eigentlich, dass nicht noch eine
						// Ausführung
						// läuft
						final List<Work> steps = getSteps();

						if (steps == null || steps.size() == 0) {
							log.severe("no Steps!");
							return;
						}

						for (Work step : steps) {
							add(step);
						}
						try {
							for (Work step : steps) {
								if (!step.isChecked()) {
									// renameTo(step);
									step.run();
									step.setChecked(true);
								}

							}
						} catch (Throwable e) {
							log.log(Level.SEVERE, "Job interrupted", e);
						}
						for (Work step : steps) {
							remove(step);
						}

						// renameTo(getBasicDisplayName());
						log.info("done");
					} finally {
						running = false;
					}
				}

			};
			add(new RunNow(task,"Run"));
			timer = new Timer(getBasicDisplayName(), true);
			timer.schedule(task, 300, period());

		}

	}

	public void stop() {
		if (task != null)
			task.cancel();

	}

	private void renameTo(Work step) {

		renameTo(getBasicDisplayName() + " Running: " + step.getDisplayName());

	}

	private void renameTo(String newname) {
		String oldName = displayName;
		displayName = newname;
		if (!newname.equals(oldName))
			view.notifyMyParent();
	}

	public List<Model> getChildren() {
		run();
		return menueEntires;

	}

	private void add(Model newEntry) {

		view.addChild(newEntry);
		menueEntires.add(newEntry);
		// notifyObservers(new AddedMenuEntryEvent(newEntry));
	}

	private void remove(Model newEntry) {
		menueEntires.remove(newEntry);
		view.removeChild(newEntry);
	}

	private ParentView view;

	public void setView(ParentView view) {
		this.view = view;
		view.setName(getDisplayName());
	}

	@Override
	public View getView() {

		return view;
	}
}
