package com.intenovation.appfw.thread;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import com.intenovation.appfw.inversemv.Model;

public class IntenovationThreadPool {
	private final static Logger log = Logger
			.getLogger(IntenovationThreadPool.class.getClass().getName());
	public static IntenovationThreadPool mtpe = new IntenovationThreadPool();
	public static void invokeLaterIfWanted(String taskname, Model model) {
		if (model.isLongRunningInit())
			IntenovationThreadPool.invokeLater(taskname, model);
	}
	public static void invokeLater(String name, Runnable task) {
		IntenovationProgress prog=	new IntenovationProgress(name,task);
		mtpe.runTask(name, prog);
	}

	long keepAliveTime = 400;

	int maxPoolSize = 10;

	int poolSize = 8 ;

	final ArrayBlockingQueue<Runnable> queue = new ArrayBlockingQueue<Runnable>(
			280);

	ThreadPoolExecutor threadPool = null;;

	public IntenovationThreadPool() {
		threadPool = new ThreadPoolExecutor(poolSize, maxPoolSize,
				keepAliveTime, TimeUnit.SECONDS, queue);

	}

	private void runTask(String name, Runnable task) {
		boolean started = false;
		while (!started)
			try {
				threadPool.execute(task);
				started = true;
			} catch (java.util.concurrent.RejectedExecutionException reject) {
				reject.printStackTrace();
				String threadname = Thread.currentThread().getName();
				log.info("Starting " + name
						+ " in this thread("+threadname+")Active count is "
						+ threadPool.getActiveCount()+" TaskCount "+threadPool.getTaskCount()+" remainingCapacity"+queue.remainingCapacity());
				long start=System.currentTimeMillis();
				task.run();
				long duration=System.currentTimeMillis()-start;
				log.info("Done executing " + name + " in this thread."+threadname+"..duration:"+duration);
				// try {
				// Thread.sleep(2000);
				// } catch (InterruptedException e) {
				// // TODO Auto-generated catch block
				// // e.printStackTrace();
				// }
				// log.info("Woke up");
			}

	}

	public void shutDown() {
		threadPool.shutdown();
	}

}
