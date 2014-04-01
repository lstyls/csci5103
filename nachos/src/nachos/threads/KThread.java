package nachos.threads;

import nachos.machine.*;
import nachos.threads.PriorityScheduler.PriorityThreadQueue;

/**
 * A KThread is a thread that can be used to execute Nachos kernel code. Nachos
 * allows multiple threads to run concurrently.
 *
 * To create a new thread of execution, first declare a class that implements
 * the <tt>Runnable</tt> interface. That class then implements the <tt>run</tt>
 * method. An instance of the class can then be allocated, passed as an
 * argument when creating <tt>KThread</tt>, and forked. For example, a thread
 * that computes pi could be written as follows:
 *
 * <p><blockquote><pre>
 * class PiRun implements Runnable {
 *     public void run() {
 *         // compute pi
 *         ...
 *     }
 * }
 * </pre></blockquote>
 * <p>The following code would then create a thread and start it running:
 *
 * <p><blockquote><pre>
 * PiRun p = new PiRun();
 * new KThread(p).fork();
 * </pre></blockquote>
 */
public class KThread {
	/**
	 * Get the current thread.
	 *
	 * @return	the current thread.
	 */
	public static KThread currentThread() {
		Lib.assertTrue(currentThread != null);
		return currentThread;
	}

	/**
	 * Allocate a new <tt>KThread</tt>. If this is the first <tt>KThread</tt>,
	 * create an idle thread as well.
	 */
	public KThread() {
		if (currentThread != null) {
			tcb = new TCB();
		}	    
		else {
			readyQueue = ThreadedKernel.scheduler.newThreadQueue(false);
			readyQueue.acquire(this);	    
			ThreadedKernel.scheduler.getPriority(this);

			/* test comment */

			((PriorityScheduler.ThreadState) this.thdSchedState).logScheduled();
			currentThread = this;
			tcb = TCB.currentTCB();
			name = "main";
			restoreState();

			createIdleThread();
		}
	}

	/**
	 * Allocate a new KThread.
	 *
	 * @param	target	the object whose <tt>run</tt> method is called.
	 */
	public KThread(Runnable target) {
		this();
		this.target = target;
	}

	/**
	 * Set the target of this thread.
	 *
	 * @param	target	the object whose <tt>run</tt> method is called.
	 * @return	this thread.
	 */
	public KThread setTarget(Runnable target) {
		Lib.assertTrue(status == statusNew);

		this.target = target;
		return this;
	}

	/**
	 * Set the name of this thread. This name is used for debugging purposes
	 * only.
	 *
	 * @param	name	the name to give to this thread.
	 * @return	this thread.
	 */
	public KThread setName(String name) {
		this.name = name;
		return this;
	}

	/**
	 * Get the name of this thread. This name is used for debugging purposes
	 * only.
	 *
	 * @return	the name given to this thread.
	 */     
	public String getName() {
		return name;
	}

	public int getID() {
		return this.id;
	}

	/**
	 * Get the full name of this thread. This includes its name along with its
	 * numerical ID. This name is used for debugging purposes only.
	 *
	 * @return	the full name given to this thread.
	 */
	public String toString() {
		return (name + " (#" + id + ")");
	}

	/**
	 * Deterministically and consistently compare this thread to another
	 * thread.
	 */
	public int compareTo(Object o) {
		KThread thread = (KThread) o;

		if (id < thread.id)
			return -1;
		else if (id > thread.id)
			return 1;
		else
			return 0;
	}

	/**
	 * Causes this thread to begin execution. The result is that two threads
	 * are running concurrently: the current thread (which returns from the
	 * call to the <tt>fork</tt> method) and the other thread (which executes
	 * its target's <tt>run</tt> method).
	 */
	public void fork() {
		Lib.assertTrue(status == statusNew);
		Lib.assertTrue(target != null);

		Lib.debug(dbgThread,
				"Forking thread: " + toString() + " Runnable: " + target);

		boolean intStatus = Machine.interrupt().disable();

		tcb.start(new Runnable() 	{ 
			public void run() 	{ 
				runThread();
			}
		});

		//ThreadedKernel.scheduler.initThreadState(this);
		ready();

		Machine.interrupt().restore(intStatus);
	}


//	/* Fork with prespecified priority */
//	public void fork(int priority) {
//		Lib.assertTrue(status == statusNew);
//		Lib.assertTrue(target != null);
//
//		Lib.debug(dbgThread,
//				"Forking thread: " + toString() + " Runnable: " + target);
//
//		boolean intStatus = Machine.interrupt().disable();
//
//		tcb.start(new Runnable() 	{ 
//			public void run() 	{ 
//				runThread();
//			}
//		});
//
//		ThreadedKernel.scheduler.initThreadState(this);
//		((StaticPriorityScheduler) ThreadedKernel.scheduler).setPriority(priority);
//		ready();
//
//		Machine.interrupt().restore(intStatus);
//	}

	private void runThread() {
		begin();
		target.run();
		finish();
	}

	private void begin() {
		Lib.debug(dbgThread, "Beginning thread: " + toString());

		Lib.assertTrue(this == currentThread);

		restoreState();

		Machine.interrupt().enable();
	}

	/**
	 * Finish the current thread and schedule it to be destroyed when it is
	 * safe to do so. This method is automatically called when a thread's
	 * <tt>run</tt> method returns, but it may also be called directly.
	 *
	 * The current thread cannot be immediately destroyed because its stack and
	 * other execution state are still in use. Instead, this thread will be
	 * destroyed automatically by the next thread to run, when it is safe to
	 * delete this thread.
	 */
	public static void finish() {
		Lib.debug(dbgThread, "Finishing thread: " + currentThread.toString());

		Machine.interrupt().disable();

		Machine.autoGrader().finishingCurrentThread();

		Lib.assertTrue(toBeDestroyed == null);
		toBeDestroyed = currentThread;
		((StaticPriorityScheduler.ThreadState) toBeDestroyed.thdSchedState).logFinished();

		currentThread.status = statusFinished;

		sleep();
	}

	/**
	 * Relinquish the CPU if any other thread is ready to run. If so, put the
	 * current thread on the ready queue, so that it will eventually be
	 * rescheuled.
	 *
	 * <p>
	 * Returns immediately if no other thread is ready to run. Otherwise
	 * returns when the current thread is chosen to run again by
	 * <tt>readyQueue.nextThread()</tt>.
	 *
	 * <p>
	 * Interrupts are disabled, so that the current thread can atomically add
	 * itself to the ready queue and switch to the next thread. On return,
	 * restores interrupts to the previous state, in case <tt>yield()</tt> was
	 * called with interrupts disabled.
	 */
	public static void yield() {
		Lib.debug(dbgThread, "Yielding thread: " + currentThread.toString());

		Lib.assertTrue(currentThread.status == statusRunning);

		boolean intStatus = Machine.interrupt().disable();

		currentThread.ready();
		currentThread.updatePriority();
		runNextThread();

		Machine.interrupt().restore(intStatus);
	}

	/**
	 * Relinquish the CPU, because the current thread has either finished or it
	 * is blocked. This thread must be the current thread.
	 *
	 * <p>
	 * If the current thread is blocked (on a synchronization primitive, i.e.
	 * a <tt>Semaphore</tt>, <tt>Lock</tt>, or <tt>Condition</tt>), eventually
	 * some thread will wake this thread up, putting it back on the ready queue
	 * so that it can be rescheduled. Otherwise, <tt>finish()</tt> should have
	 * scheduled this thread to be destroyed by the next thread to run.
	 */
	public static void sleep() {
		Lib.debug(dbgThread, "Sleeping thread: " + currentThread.toString());

		Lib.assertTrue(Machine.interrupt().disabled());

		if (currentThread.status != statusFinished)
			currentThread.status = statusBlocked;

		runNextThread();
	}

	/**
	 * Moves this thread to the ready state and adds this to the scheduler's
	 * ready queue.
	 */
	public void ready() {
		Lib.debug(dbgThread, "Ready thread: " + toString());

		Lib.assertTrue(Machine.interrupt().disabled());
		Lib.assertTrue(status != statusReady);

		status = statusReady;
		if (this != idleThread) {
			readyQueue.waitForAccess(this);
			this.thdSchedState.logEnqueued();
			
			/*
			if(ThreadedKernel.schedulerName.equals("nachos.threads.DynamicPriorityScheduler")){
				readyQueue.updatePriority();
			}
			*/
		}



		Machine.autoGrader().readyThread(this);
	}

	/**
	 * Waits for this thread to finish. If this thread is already finished,
	 * return immediately. This method must only be called once; the second
	 * call is not guaranteed to return. This thread must not be the current
	 * thread.
	 */
	public void join() {
		Lib.debug(dbgThread, "Joining to thread: " + toString());

		Lib.assertTrue(this != currentThread);

	}

	/**
	 * Create the idle thread. Whenever there are no threads ready to be run,
	 * and <tt>runNextThread()</tt> is called, it will run the idle thread. The
	 * idle thread must never block, and it will only be allowed to run when
	 * all other threads are blocked.
	 *
	 * <p>
	 * Note that <tt>ready()</tt> never adds the idle thread to the ready set.
	 */
	private static void createIdleThread() {
		Lib.assertTrue(idleThread == null);

		idleThread = new KThread(new Runnable() {
			public void run() { while (true) yield(); }
		});
		idleThread.setName("idle");

		Machine.autoGrader().setIdleThread(idleThread);

		idleThread.fork();
	}

	/**
	 * Determine the next thread to run, then dispatch the CPU to the thread
	 * using <tt>run()</tt>.
	 */
	private static void runNextThread() {
		KThread nextThread = readyQueue.nextThread();
		if (nextThread == null)
			nextThread = idleThread;

		nextThread.run();
	}

	/**
	 * Dispatch the CPU to this thread. Save the state of the current thread,
	 * switch to the new thread by calling <tt>TCB.contextSwitch()</tt>, and
	 * load the state of the new thread. The new thread becomes the current
	 * thread.
	 *
	 * <p>
	 * If the new thread and the old thread are the same, this method must
	 * still call <tt>saveState()</tt>, <tt>contextSwitch()</tt>, and
	 * <tt>restoreState()</tt>.
	 *
	 * <p>
	 * The state of the previously running thread must already have been
	 * changed from running to blocked or ready (depending on whether the
	 * thread is sleeping or yielding).
	 *
	 * @param	finishing	<tt>true</tt> if the current thread is
	 *				finished, and should be destroyed by the new
	 *				thread.
	 */
	private void run() {
		Lib.assertTrue(Machine.interrupt().disabled());

		Machine.yield();

		currentThread.saveState();

		Lib.debug(dbgThread, "Switching from: " + currentThread.toString()
				+ " to: " + toString());

		currentThread = this;
		this.thdSchedState.logScheduled();

		tcb.contextSwitch();

		currentThread.restoreState();
	}

	/**
	 * Prepare this thread to be run. Set <tt>status</tt> to
	 * <tt>statusRunning</tt> and check <tt>toBeDestroyed</tt>.
	 */
	protected void restoreState() {
		Lib.debug(dbgThread, "Running thread: " + currentThread.toString());

		Lib.assertTrue(Machine.interrupt().disabled());
		Lib.assertTrue(this == currentThread);
		Lib.assertTrue(tcb == TCB.currentTCB());

		Machine.autoGrader().runningThread(this);

		status = statusRunning;

		if (toBeDestroyed != null) {
			toBeDestroyed.tcb.destroy();
			toBeDestroyed.tcb = null;
			toBeDestroyed = null;
		}
	}

	/**
	 * Prepare this thread to give up the processor. Kernel threads do not
	 * need to do anything here.
	 */
	protected void saveState() {
		Lib.assertTrue(Machine.interrupt().disabled());
		Lib.assertTrue(this == currentThread);
	}

	private static class BusyRunWithYield implements Runnable {
		public void run() {
			for (long i=0; i<1000000001; i++) {
				long j = i%100000000;
				if (j==0) {
					yield();
				}
			}
		}
	}
	
	private static class BusyRunNoYield implements Runnable {
		public void run() {
			for (long i=0; i<1000000001; i++) {
				long j = i%100000000;
				if (j==0) {
					//yield();
				}
			}
		}
	}
	

	
	private static class RunThreeNoYield21 implements Runnable {
		public void run() {
			boolean intState = Machine.interrupt().disable();
			KThread newthread = new KThread (new RunThreeNoYield11());
			ThreadedKernel.scheduler.setPriority(newthread, 11);
			newthread.fork();
			Machine.interrupt().setStatus(intState);
			
			for (long i=0; i<1000000001; i++) {
				long j = i%100000000;
				if (j==0) {
				}
			}
		}
	}
	
	
	private static class RunThreeNoYield11 implements Runnable {
		public void run() {
			boolean intState = Machine.interrupt().disable();
			KThread newthread = new KThread (new BusyRunNoYield());
			ThreadedKernel.scheduler.setPriority(newthread, 1);
			newthread.fork();
			Machine.interrupt().setStatus(intState);
			
			for (long i=0; i<1000000001; i++) {
				long j = i%100000000;
				if (j==0) {
				}
			}
		}
	}
	
	
	private static class RunThreeWithYield21 implements Runnable {
		public void run() {
			boolean intState = Machine.interrupt().disable();
			KThread newthread = new KThread (new RunThreeWithYield11());
			ThreadedKernel.scheduler.setPriority(newthread, 11);
			newthread.fork();
			Machine.interrupt().setStatus(intState);
			
			for (long i=0; i<1000000001; i++) {
				long j = i%100000000;
				if (j==0) {
					yield();
				}
			}
		}
	}
	
	
	private static class RunThreeWithYield11 implements Runnable {
		public void run() {
			boolean intState = Machine.interrupt().disable();
			KThread newthread = new KThread (new BusyRunWithYield());
			ThreadedKernel.scheduler.setPriority(newthread, 1);
			newthread.fork();
			Machine.interrupt().setStatus(intState);
			
			for (long i=0; i<1000000001; i++) {
				long j = i%100000000;
				if (j==0) {
					yield();
				}
			}
		}
	}


	/**
	 * Tests whether this module is working.
	 */
	public static void selfTest() {
		Lib.debug(dbgThread, "Enter KThread.selfTest");
		
		switch (ThreadedKernel.selfTestNum) {
		
			case 1: 
				st1();
				break;
		
			case 2:
				st2();
				break;
					
			
			case 3: 
				st3();
				break;
			
			case 4: 
				st4();
				break;
				
			case 5:
				st5();
				break;
				
			case 6:
				st6();
				break;
			
		}
		
		yield();
	}
	
	
	/** Self test 1 forks six threads of equal (medium) priority. The threads will not yield
	 * while executing. */
	private static void st1() {
		boolean intState = Machine.interrupt().disable();
		for (int i = 0; i<6; i++) {
			KThread newthread = new KThread (new BusyRunNoYield()).setName("Forked Thread");
			ThreadedKernel.scheduler.setPriority(newthread, 15);
			newthread.fork();
		}
		Machine.interrupt().setStatus(intState);
	}
	
	/** Self test 2 forks six threads of equal (medium) priority. The threads will undergo
	 * a CPU burst and then yield, five times each. */
	private static void st2() {
		boolean intState = Machine.interrupt().disable();
		for (int i = 0; i<6; i++) {
			KThread newthread = new KThread (new BusyRunWithYield()).setName("Forked Thread");
			ThreadedKernel.scheduler.setPriority(newthread, 15);
			newthread.fork();
		}
		Machine.interrupt().setStatus(intState);
	}
	
	/** Self test 3 forks three threads of increasing priority. They race to acquire the CPU
	 * and will not yield until they finish executing. */
	private static void st3() {
		
		boolean intState = Machine.interrupt().disable();
		
		KThread newguy1 = new KThread(new BusyRunNoYield()).setName("forked thread");
		ThreadedKernel.scheduler.setPriority(newguy1, 30);
		newguy1.fork();
		
		KThread newguy2 = new KThread(new BusyRunNoYield()).setName("forked thread");
		ThreadedKernel.scheduler.setPriority(newguy2, 15);
		newguy2.fork();
		
		KThread newguy3 = new KThread(new BusyRunNoYield()).setName("forked thread");
		ThreadedKernel.scheduler.setPriority(newguy3, 1);
		newguy3.fork();
		
		Machine.interrupt().setStatus(intState);
	}
	
	/** Self test 4 forks three threads of increasing priority. They race to acquire the CPU
	 * and will perform alternating CPU bursts and yields five times each. */
	private static void st4() {
		
		boolean intState = Machine.interrupt().disable();
		
		KThread newguy1 = new KThread(new BusyRunWithYield()).setName("forked thread");
		ThreadedKernel.scheduler.setPriority(newguy1, 30);
		newguy1.fork();
		
		KThread newguy2 = new KThread(new BusyRunWithYield()).setName("forked thread");
		ThreadedKernel.scheduler.setPriority(newguy2, 15);
		newguy2.fork();
		
		KThread newguy3 = new KThread(new BusyRunWithYield()).setName("forked thread");
		ThreadedKernel.scheduler.setPriority(newguy3, 1);
		newguy3.fork();
		
		Machine.interrupt().setStatus(intState);
	}
	
	/** Self test 5 forks three threads recursively with increasing priority. The threads
	 * do not yield.*/
	private static void st5() {
		boolean intState = Machine.interrupt().disable();
		KThread newguy = new KThread(new RunThreeNoYield21()).setName("forked thread");
		ThreadedKernel.scheduler.setPriority(newguy, 21);
		newguy.fork();
		Machine.interrupt().setStatus(intState);
	}
	
	/** Self test 6 forks three threads recursively with increasing priority. The threads
	 * alternately perform computation and yield 5 times.*/
	private static void st6() {
		boolean intState = Machine.interrupt().disable();
		KThread newguy = new KThread(new RunThreeWithYield21()).setName("forked thread");
		ThreadedKernel.scheduler.setPriority(newguy, 21);
		newguy.fork();
		Machine.interrupt().setStatus(intState);
	}
	
	public boolean isIdleThread() {
		return (this == KThread.idleThread) && (this != null);
	}
	
	public boolean isMainThread() {
		return (this.id == 0);
	}
	

	/** Update priority of the thread after running by some scheduler-dependent aging function.
	 * To be called when the thread is switched off the CPU. */
	private void updatePriority() {
		if (this.isIdleThread()) return;
		if (this.isMainThread()) return;
//		Object test = this.thdSchedState.getClass();
//		Object y = test.getClass();
		this.thdSchedState.ageValUp();
	}

	private static final char dbgThread = 't';

	/**
	 * Additional state used by schedulers.
	 * Leaving this here to keep machine startup test happy.
	 *
	 * @see	nachos.threads.PriorityScheduler.ThreadState
	 */
	public PriorityScheduler.ThreadState thdSchedState = null;
	public Object schedulingState = null;

	private static final int statusNew = 0;
	private static final int statusReady = 1;
	private static final int statusRunning = 2;
	private static final int statusBlocked = 3;
	private static final int statusFinished = 4;

	/**
	 * The status of this thread. A thread can either be new (not yet forked),
	 * ready (on the ready queue but not running), running, or blocked (not
	 * on the ready queue and not running).
	 */
	protected int status = statusNew;
	private String name = "(unnamed thread)";
	private Runnable target;
	private TCB tcb;

	/**
	 * Unique identifer for this thread. Used to deterministically compare
	 * threads.
	 */
	private int id = numCreated++;
	/** Number of times the KThread constructor was called. */
	private static int numCreated = 0;


	private static PriorityThreadQueue readyQueue = null;
	private static KThread currentThread = null;
	private static KThread toBeDestroyed = null;
	private static KThread idleThread = null;
}
