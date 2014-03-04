package nachos.threads;

import nachos.machine.*;
import nachos.threads.DynamicPriorityScheduler.ThreadState;

public class DynamicPriorityScheduler extends Scheduler {
	
	
	/** Pointer to the kernel */
	
	ThreadedKernel kernel;
	
	
	/**
	 * 	Track running statistics for threads.
	 * 
	 * 		nthreads: running total number of threads to have completed, updated on thread death
	 * 
	 * 		totalRunTime: running total of times threads have spent on CPU, updated on thread death
	 * 
	 * 		totalTurnTime: as totalRunTime, but with turnaround times
	 * 
	 * 		maxWaitingTime: to be compared and updated on thread death
	 */
	
	
	private static int nfinished;
	private static long totalWaitTime;
	private static long totalTurnTime;
	private static long maxWaitTime;
	
	
	/** Barebones constructor. */
	public DynamicPriorityScheduler() {
		nfinished = 0;
		totalWaitTime = 0;
		totalTurnTime = 0;
		maxWaitTime = 0;
	}
	
	
	protected void logFinalStats() {
		int avWait = Math.round(((float)totalWaitTime)/nfinished);
		int avTurn = Math.round(((float)totalTurnTime)/nfinished);
		kernel.logprint(String.format("System,%d,%d,%d,%d\n", nfinished, avWait,
				maxWaitTime, avTurn));
	}

	
	/** Allocate new ThreadQueue. */
	public ThreadQueue newThreadQueue(boolean transferPriority) {
		return new StaticPriorityThreadQueue(transferPriority);
	}
	
	/**
	 *  Return the scheduling state of the input thread.
	 * 
	 * If the thread does not yet have a state specified, allocate
	 * 		a new one and assign it to the thread's schedulingState.
	 * 
	 * @param thread the thread whose scheduling state to return.
	 * @return the scheduling state of the input thread.
	 */
	protected ThreadState getThreadState(KThread thread) {
		if (thread.schedulingState == null)
			thread.schedulingState = new ThreadState(thread);
		
		return (ThreadState) thread.schedulingState;
	}
	
	
	/**
	 * Get priority of input thread.
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
	 * @return priority of input thread
	 */
	public int getEffectivePriority(KThread thread) {
		Lib.assertTrue(Machine.interrupt().disabled());
		
		return getThreadState(thread).getEffectivePriority();
	}
	
	
	public void setPriority(KThread thread, int priority) {
		Lib.assertTrue(Machine.interrupt().disabled());
		
		Lib.assertTrue(priority >= priorityMinimum && 
				priority <= priorityMaximum);
		
		getThreadState(thread).setPriority(priority);
	}
	
	
	public static final int priorityDefault = 1;
		
	// TODO Modify code so that priorityMaximum is specified in config file
	private int priorityMaximum = 7;
	
	public static final int priorityMinimum = 0;
	
	public void setPriorityMaximum(int priority) {
		Lib.assertTrue(priority >= priorityMinimum);
		
		this.priorityMaximum = priority;
	}
	
	
	
	/** Inner class tracks state variables associated with a specific thread. */
	protected class ThreadState {
		
		/**
		 * Variables to track statistics for a thread.
		 */
		private long arrivalTime;
		private long lastScheduled;
		private long lastEnqueued;
		private long thdTotWait;
		private long thdTotRun;
		
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
			
			updateDynamicPriority();
			
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
			updateDynamicPriority();
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
		
		public void updateDynamicPriority(){
			if (thdTotWait > ThreadedKernel.agingTime){
				increasePriority();
				System.out.println("update");
			}
			if (thdTotRun > ThreadedKernel.agingTime){
				decreasePriority();
			}
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
		
		/**
	     * If possible, raise the priority of the current thread in some
	     * scheduler-dependent way.
	     *
	     * @return	<tt>true</tt> if the scheduler was able to increase the current
	     *		thread's
	     *		priority.
	     */
	    public boolean increasePriority() {
			if (priority > priorityMinimum){
				priority--;
				System.out.println("increase pri");
				return true;
			}else{
				return false;
			}
	    }
	    
	    /**
	     * If possible, lower the priority of the current thread user in some
	     * scheduler-dependent way, preferably by the same amount as would a call
	     * to <tt>increasePriority()</tt>.
	     *
	     * @return	<tt>true</tt> if the scheduler was able to decrease the current
	     *		thread's priority.
	     */
	    public boolean decreasePriority() {
			if (priority < priorityMaximum){
				priority++;
				System.out.println("increase pri");
				return true;
			}else{
				return false;
			}
	    }
	    	
	}
}
