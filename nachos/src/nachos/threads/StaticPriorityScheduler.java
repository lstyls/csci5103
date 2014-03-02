package nachos.threads;

import nachos.machine.*;

public class StaticPriorityScheduler extends Scheduler {
	
	/*
	 * StaticPriorityScheduler does not implement 
	 * increasePriority or decreasePriority methods because
	 * they are not supported in the scheduling description.
	 */
	
	/** Barebones constructor. */
	public StaticPriorityScheduler() {
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
		
		protected KThread thread;
		protected int priority;
		
		public ThreadState(KThread thread) {
			this.thread = thread;
			setPriority(priorityDefault);
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
