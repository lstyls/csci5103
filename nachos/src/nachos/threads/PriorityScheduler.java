package nachos.threads;

import nachos.machine.*;

import java.util.Comparator;
import java.util.HashSet;

/**
 * A scheduler that chooses threads based on their priorities.
 *
 * This is an abstract class from which the schedulers described in the problem
 * description are extended.
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
	private final int priorityDefault = 11;
	/**
	 * The minimum priority that a thread can have. Do not change this value.
	 */
	protected final int priorityMinimum = 1;
	/**
	 * The maximum priority that a thread can have. Can be specified in config file.
	 */
	protected int priorityMaximum = 40;    
	
	/** Reference to the kernel that instantiated the scheduler. */
	protected ThreadedKernel kernel;
	
	/**
	 * Allocate, initialize a new priority scheduler.
	 */
	public PriorityScheduler() {
		nfinished = 0;
		totalWaitTime = 0;
		totalTurnTime = 0;
		maxWaitTime = 0;
	}
		

	/** Interval of running/waiting time for which priority will be incremented/decremented
	 * respectively.	 */
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
	
	/** Allocate and assign a thread state object for specified thread. */
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
	
	
	/** Set globally allowed maximum priority as specified in the config file. */
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
	
	/** Sets the priority of the specified thread. Will throw
	 * assertion error exception if new priority is outside allowed bounds.	 */
	public void initPriority(KThread thread, int priority) {
		Lib.assertTrue(Machine.interrupt().disabled());
		
		Lib.assertTrue(priority >= priorityMinimum && 
				priority <= priorityMaximum);
		
		this.getThreadState(thread).initPriority(priority);
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
				int p1 = (ThreadedKernel.scheduler.getEffectivePriority(t1));
				int p2 = (ThreadedKernel.scheduler.getEffectivePriority(t2));
				return p1-p2;
				
			}
			
		}
		
		/** Add specified thread to waiting queue. */
		public abstract void waitForAccess(KThread thread);

		/** Acquire CPU. */
		public abstract void acquire(KThread thread);

		/** Return next scheduled thread. Return the main thread if there are no test threads remaining so 
		 * that the kernel exits. */
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
		protected int effPriority;
		private Lock minPriorLock;
		
		private HashSet<Lock> locksHeld;
		
		public ThreadState(KThread thread) {
			this.thread = thread;
			setPriority(priorityDefault);
			setEffPriority(priorityDefault);
			arrivalTime = -1;
			thdTotWait = 0;
			thdTotRun = 0;
			lastScheduled = -1;
			lastEnqueued = -1;
			
			if (ThreadedKernel.usePriorityDonation) {
				locksHeld = new HashSet<Lock>();
			}
		}
				
		/**
		 * Updates statistics and prints to kernel logfile. To be in KThread when the thread is scheduled to run.
		 */
		protected void logScheduled() {
			long curtime = kernel.getTime();
			
			lastScheduled = curtime;
			
			if (lastEnqueued > 0) thdTotWait += curtime - lastEnqueued;
			
			kernel.logprint(String.format("S,%d,%d,%d\n", curtime, thread.getID(), effPriority));
		}
		
		/** Age the thread associated with this state upward by some scheduler-dependent method. Default behavior does nothing. */
		public void ageValUp() {
		}
		
		/** Age the thread associated with this state downward by some scheduler-dependent method. Default behavior does nothing. */
		public void ageValDown() {
		}
		
		/** To be called by a lock when the thread acquires it. Adds the lock to the thread's set of held locks and updates the 
		 * thread's effective priority.  */
		public void acquireLock(Lock lock) {
			locksHeld.add(lock);
			updateEP(lock);
		}
		
		/** To be called by a lock when the thread releases it. Removes the lock from thread's set of held locks and updates the thread's
		 * effective priority if it was the lock granting the thread's current effective priority.
		 * 
		 * @param lock
		 */
		public void releaseLock(Lock lock) {
			locksHeld.remove(lock);
			if (minPriorLock != null && lock == minPriorLock) {
				updateEP();
			}
		}
		

		/**
		 * Update the effective priority of the thread by iterating through the locks held. To be called
		 * when the thread releases the lock that was currently granting the highest priority.
		 */
		public void updateEP() {
			// Iterate through locks held to find effective priority
			minPriorLock = null;
			effPriority = ThreadedKernel.scheduler.priorityMaximum;
			for (Lock l : locksHeld) {
				if (effPriority > l.effPriority) effPriority = l.effPriority;
				minPriorLock = l;
			}
			effPriority = Math.min(priority, effPriority);
		}
		
		/**
		 * Update the effective priority of the thread. To be called by one of the locks held by the thread when
		 * that thread's priority has changed.
		 */
		public void updateEP(Lock lock) {
			// Update priority of the thread because the effective priority value of a held lock has increased
			if (lock.effPriority < this.effPriority) {
				setEffPriority(lock.effPriority);
				this.minPriorLock = lock;
			}
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
						
			if (arrivalTime<=0) return;
			
			totalTurnTime += curtime - arrivalTime;
			
			if (thdTotWait>0) totalWaitTime += thdTotWait;
			if (thdTotWait>maxWaitTime)  maxWaitTime = thdTotWait;
			
			kernel.logprint(String.format("%d,%d,%d,%d,%d\n", thread.getID(),
					arrivalTime, thdTotRun+curtime-lastScheduled, thdTotWait, curtime));
			
		}
		
		public int getEffPriority() {
			return this.effPriority;
		}
		
		public void setEffPriority(int effPriority) {
			this.effPriority = effPriority;
		}
		
		public void initPriority(int p) {
			this.effPriority = p;
			this.priority = p;
		}

		/** Return priority of associated thread. */
		public int getPriority() {
			return this.priority;
		}
		/** Return effective priority of thread based on priority donation. Our implementation does nothing
		 * as the assignment ignores this feature.
		 */
		public int getEffectivePriority() {
			// We aren't worrying about donating priority, so this is the same as vanilla priority.
			return this.effPriority;
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
			if (!ThreadedKernel.usePriorityDonation) this.effPriority = priority;
		}
		
	}
}
