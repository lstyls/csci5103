package nachos.threads;

import nachos.machine.*;

import java.util.Comparator;

/**
 * A scheduler that chooses threads based on their priorities.
 *
 * <p>
 * A priority scheduler associates a priority with each thread. The next thread
 * to be dequeued is always a thread with priority no less than any other
 * waiting thread's priority. Like a round-robin scheduler, the thread that is
 * dequeued is, among all the threads of the same (highest) priority, the
 * thread that has been waiting longest.
 *
 * <p>
 * Essentially, a priority scheduler gives access in a round-robin fassion to
 * all the highest-priority threads, and ignores all other threads. This has
 * the potential to
 * starve a thread if there's always a thread waiting with higher priority.
 *
 * <p>
 * A priority scheduler must partially solve the priority inversion problem; in
 * particular, priority must be donated through locks, and through joins.
 */
public abstract class PriorityScheduler extends Scheduler {

	/* * * * * 	Track running statistics for threads. * * * */
	
	/** Running total of threads that have finished. */
	private static int nfinished;
	
	/** Running total wait time of scheduled threads. Updated on KThread.finish(). */
	private static long totalWaitTime;
	
	/** Running total turnaround time of scheduled threads. Updated on KThread.finish(). */
	private static long totalTurnTime;
	
	/** Maximum total wait time of any single thread that has finished.
	 * Updated on KThread.finish(). */
	private static long maxWaitTime;
	
	/**
	 * The default priority for a new thread. Do not change this value.
	 */
	private final int priorityDefault = 1;
	/**
	 * The minimum priority that a thread can have. Do not change this value.
	 */
	private final int priorityMinimum = 0;
	/**
	 * The maximum priority that a thread can have. Can be specified in config file.
	 */
	private int priorityMaximum = 7;    
	
	/** Reference to the kernel that instantiated the scheduler. */
	protected ThreadedKernel kernel;
	
	/**
	 * Allocate a new priority scheduler.
	 */
	public PriorityScheduler() {
		nfinished = 0;
		totalWaitTime = 0;
		totalTurnTime = 0;
		maxWaitTime = 0;
	}
		
	@SuppressWarnings("unused")
	protected long agingTime;
	
	protected void setAgingTime(long ageTime) {
		agingTime = ageTime;
	}
	
	/** Write global stats to kernel logfile for threads managed by the scheduler.
	 * To be called before kernel terminates.
	 */
	protected void logFinalStats() {
		int avWait = Math.round(((float)totalWaitTime)/nfinished);
		int avTurn = Math.round(((float)totalTurnTime)/nfinished);
		kernel.logprint(String.format("System,%d,%d,%d,%d\n", nfinished, avWait,
				maxWaitTime, avTurn));
	}
	
	
	/**
	 * Allocate a new priority thread queue.
	 *
	 * @param	transferPriority	<tt>true</tt> if this queue should
	 *					transfer priority from waiting threads
	 *					to the owning thread.
	 * @return	a new priority thread queue.
	 */
	public abstract PriorityThreadQueue newThreadQueue(boolean transferPriority) ;
	


	/**
	 * Return the scheduling state of the specified thread.
	 * 
	 * If the thread does not yet have a state specified, allocate
	 * 		a new one and assign it to the thread's schedulingState.
	 * 
	 * @param	thread	the thread whose scheduling state to return.
	 * @return	the scheduling state of the specified thread.
	 */
	protected ThreadState getThreadState(KThread thread) {
		if (thread.thdSchedState == null)
			initThreadState(thread);

		return (ThreadState) thread.thdSchedState;
	}
	
	protected void initThreadState(KThread thread) {
		thread.thdSchedState = new ThreadState(thread);
	}
	
	/**
	 * Get priority of specified thread.
	 * 
	 * @param thread	thread from which to return priority
	 * 
	 * @return priority of input thread
	 */
	public int getPriority(KThread thread) {
		Lib.assertTrue(Machine.interrupt().disabled());

		return getThreadState(thread).getPriority();
	}

	
	/**
	 * Get effective priority of input thread. Because we don't
	 * accommodate priority donation, this is equal to the plain
	 * thread priority.
	 * 
	 * @param thread	thread from which to return priority
	 * 
	 * @return priority of specified thread
	 */
	public int getEffectivePriority(KThread thread) {
		Lib.assertTrue(Machine.interrupt().disabled());

		return getThreadState(thread).getEffectivePriority();
	}
	
	
	
	protected void setSchedMaxPriority(int maxp) {
		this.priorityMaximum = maxp;
	}
	
	
	/** Sets the priority of the specified thread. Will throw
	 * assertion error exception if new priority is outside allowed bounds.	 */
	public void setPriority(KThread thread, int priority) {
		Lib.assertTrue(Machine.interrupt().disabled());
		
		Lib.assertTrue(priority >= priorityMinimum && 
				priority <= priorityMaximum);
		
		this.getThreadState(thread).setPriority(priority);
	}
	
	/** Sets the priority of the specified thread. Fixes given priority so that
	 * it falls within allowed bounds.
	 *  
	 * @param thread
	 * @param priority
	 */
	public void setFixPriority(KThread thread, int priority) {
		Lib.assertTrue(Machine.interrupt().disabled());
		
		if (priority < this.priorityMinimum)
			priority = this.priorityMinimum;
		if (priority > this.priorityMaximum)
			priority = this.priorityMaximum;
		
		setPriority(thread, priority);
	}

	/** Raise the priority of current thread by one */

	public boolean increasePriority() {
		boolean intStatus = Machine.interrupt().disable();

		KThread thread = KThread.currentThread();

		int priority = getPriority(thread);
		if (priority == priorityMaximum)
			return false;

		setPriority(thread, priority+1);

		Machine.interrupt().restore(intStatus);
		return true;
	}

	/** Decrease the priority of current thread by one */

	public boolean decreasePriority() {
		boolean intStatus = Machine.interrupt().disable();

		KThread thread = KThread.currentThread();

		int priority = getPriority(thread);
		if (priority == priorityMinimum)
			return false;

		setPriority(thread, priority-1);

		Machine.interrupt().restore(intStatus);
		return true;
	}



	/**
	 * A <tt>ThreadQueue</tt> that sorts threads by priority.
	 */
	protected abstract class PriorityThreadQueue extends ThreadQueue {
		
		public PriorityThreadQueue(boolean transferPriority) {
		}
		
		/** Comparator used to order threads in priority queue based on priority. */
		protected class ThreadComparator implements Comparator<KThread> {

			@Override
			public int compare(KThread t1, KThread t2) {
				int p1 = (ThreadedKernel.scheduler.getPriority(t1));
				int p2 = (ThreadedKernel.scheduler.getPriority(t2));
				int comp = p1-p2;
				
				if (comp < 0) return -1;
				
				if (comp > 0) return 1;
				
				return 0;
				
			}
			
		}

		public abstract void waitForAccess(KThread thread);

		public abstract void acquire(KThread thread);

		public abstract KThread nextThread();

		public void print() {
			Lib.assertTrue(Machine.interrupt().disabled());
			// implement me (if you want)
		}

		/**
		 * <tt>true</tt> if this queue should transfer priority from waiting
		 * threads to the owning thread.
		 */
		public boolean transferPriority;
	}

	/**
	 * The scheduling state of a thread. This should include the thread's
	 * priority, its effective priority, any objects it owns, and the queue
	 * it's waiting for, if any.
	 *
	 * @see	nachos.threads.KThread#schedulingState
	 */
	/** Inner class tracks state variables associated with a specific thread. */
	protected class ThreadState {
		
		/**
		 * Variables to track statistics for a thread.
		 */
		private long arrivalTime;
		protected long lastScheduled;
		protected long lastEnqueued;
		protected long thdTotWait;
		protected long thdTotRun;
		
		protected KThread thread;
		protected int priority;
		
		
		public ThreadState(KThread thread) {
			this.thread = thread;
			setPriority(priorityDefault);
			arrivalTime = -1;
			thdTotWait = 0;
			thdTotRun = 0;
			lastScheduled = -1;
			lastEnqueued = -1;
		}
				
		/**
		 * Updates statistics and prints to kernel logfile. To be in KThread when the thread is scheduled to run.
		 */
		protected void logScheduled() {
			long curtime = kernel.getTime();
			
			lastScheduled = curtime;
			
			if (lastEnqueued > 0) thdTotWait += curtime - lastEnqueued;
			
			kernel.logprint(String.format("%d,%s(%d),%d\n", curtime, thread.getName(),
					thread.getID(), priority));
		}
		
		
		/**	Update waiting statistics when the thread is placed in the queue. 	 */
		protected void logEnqueued() {
			long curtime = kernel.getTime();
			
			if (lastEnqueued < 0) {
				// Thread has not been scheduled for the first time yet.
				arrivalTime = curtime;
				lastScheduled = curtime;
			}
			else {
				thdTotRun += curtime - lastScheduled;
			}
			lastEnqueued = curtime;
		}
		
		/** Update global statistics from thread and write thread stats to logfile.
		 *  To be called when thread finishes. 
		 */
		protected void logFinished() {
			long curtime = kernel.getTime();
			nfinished++;
			
			//Lib.assertTrue(arrivalTime>=0,"Trying to log finish of thread with no arrival time.");
			
			if (arrivalTime<=0) return;
			
			totalTurnTime += curtime - arrivalTime;
			
			if (thdTotWait>0) totalWaitTime += thdTotWait;
			if (thdTotWait>maxWaitTime)  maxWaitTime = thdTotWait;
			
			kernel.logprint(String.format("%s(%d),%d,%d,%d,%d\n", thread.getName(), thread.getID(),
					arrivalTime, thdTotRun+curtime-lastScheduled, thdTotWait, curtime));
			
		}

		
		public int getPriority() {
			return this.priority;
		}
		
		public int getEffectivePriority() {
			// We aren't worrying about donating priority, so this is the same as vanilla priority.
			return this.getPriority();
		}
		
		/**
		 * Sets priority of the thread. 
		 *  
		 * @param desired priority of the thread.
		 */
		
		protected void setPriority(int priority) {
			/*  We don't worry about the priority being outside
			 * of allowed bounds because this is checked in the 
			 * enclosing method. */
			this.priority = priority;
		}
		
	}
}
