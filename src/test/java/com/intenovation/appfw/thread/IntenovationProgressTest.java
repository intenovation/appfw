package com.intenovation.appfw.thread;

import java.util.Random;

import com.intenovation.appfw.tree.Window;

import junit.framework.TestCase;

public class IntenovationProgressTest extends TestCase {

	public void testRun() throws Exception {
		Window.getInstance().setVisible(true);
		Runnable run = new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub

				Random random = new Random();
				int progress = 0;
				// Initialize progress property.

				while (progress < 100) {
					// Sleep for up to one second.
					try {
						Thread.sleep(random.nextInt(1000));
					} catch (InterruptedException ignore) {
					}
					// Make random progress.
					progress += random.nextInt(10);
					// setProgress(Math.min(progress, 100));
				}
			}

		};
		IntenovationProgress[] prog = new IntenovationProgress[10];
		for (int i = 0; i < 10; i++) {
			prog[i] = new IntenovationProgress("text lang " + i, run);

		}
		for (int i = 0; i < 10; i++) {
			prog[i].run();
		}
	}
}
