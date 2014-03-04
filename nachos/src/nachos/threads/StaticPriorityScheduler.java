package nachos.threads;

import nachos.machine.*;

public class StaticPriorityScheduler extends PriorityScheduler {
	
	/*
	 * StaticPriorityScheduler does not implement 
	 * increasePriority or decreasePriority methods because
	 * they are not supported in the scheduling description.
	 */
	
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
	public StaticPriorityScheduler() {
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
	public StaticPriorityThreadQueue newThreadQueue(boolean transferPriority) {
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
	protected PriorityScheduler.ThreadState getThreadState(KThread thread) {
		if (thread.schedulingState == null)
			thread.schedulingState = new ThreadState(thread);
		
		return thread.schedulingState;
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

}
