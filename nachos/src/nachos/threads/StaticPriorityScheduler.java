/**
 * 
 */
package nachos.threads;

import nachos.machine.Lib;
import nachos.machine.Machine;

/**
 *	Scheduler for Problem 1 of Programming Assignment 1, CSCI5103.
 *
 * @author styles
 *
 */
public class StaticPriorityScheduler extends PriorityScheduler {

	/**
	 * 
	 */
	public StaticPriorityScheduler() {
		// TODO Auto-generated constructor stub
		super();
	}


	/* (non-Javadoc)
	 * @see nachos.threads.PriorityScheduler#newThreadQueue(boolean)
	 */
	@Override
	public PriorityThreadQueue newThreadQueue(boolean transferPriority) {
		return new StaticPriorityThreadQueue(transferPriority);
	}
	
	
	protected class StaticPriorityThreadQueue extends PriorityScheduler.PriorityThreadQueue {

		private PriorityThreadQueue.ThreadComparator tComp = 
				new PriorityThreadQueue.ThreadComparator();
		public java.util.PriorityQueue<KThread> waitQueue =
				new java.util.PriorityQueue<KThread>(ThreadedKernel.numThreads, tComp);
		
		KThread main;
		
		public StaticPriorityThreadQueue(boolean transferPriority) {
			super(transferPriority);
		}

		public void waitForAccess(KThread thread) {
			Lib.assertTrue(Machine.interrupt().disabled(),
					"Interrupts not disabled in required critical section.");
			if (thread.isMainThread()) {
				this.main = thread;
			}
			else {
				int p = thread.thdSchedState.getPriority();
				Lib.assertTrue( p>= priorityMinimum && p <= priorityMaximum);
				waitQueue.add(thread);
			}
		}

		public void acquire(KThread thread) {
			Lib.assertTrue(Machine.interrupt().disabled(),
					"Interrupts not disabled in required critical section.");
			Lib.assertTrue(waitQueue.isEmpty(), 
					"Attempted to aquire with non-empty wait queue.");			
		}

		@Override
		public KThread nextThread() {
			Lib.assertTrue(Machine.interrupt().disabled(),
					"Interrupts not disabled in required critical section.");
			if (waitQueue.isEmpty()) return main;
			return waitQueue.poll();
		}
		
		/**
		 * peak at highest priority thread 
		 * if empty return 1
		 */
		
		
		public int getHighestPriority(){
			int priority;
			priority = waitQueue.peek().thdSchedState.getPriority();
			return priority;
		}
		
		
		public void updatePriority(){
		}
		

		
	}

}
