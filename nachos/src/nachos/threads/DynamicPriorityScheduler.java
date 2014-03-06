package nachos.threads;

import nachos.machine.Lib;
import nachos.machine.Machine;

public class DynamicPriorityScheduler extends PriorityScheduler {
	
	/**
	 * 
	 */
	public DynamicPriorityScheduler() {
		// TODO Auto-generated constructor stub
		super();
	}


	/* (non-Javadoc)
	 * @see nachos.threads.PriorityScheduler#newThreadQueue(boolean)
	 */
	@Override
	public PriorityThreadQueue newThreadQueue(boolean transferPriority) {
		return new DynamicPriorityThreadQueue(transferPriority);
	}
	
	
	protected class DynamicPriorityThreadQueue extends PriorityScheduler.PriorityThreadQueue {

		private PriorityThreadQueue.ThreadComparator tComp = 
				new PriorityThreadQueue.ThreadComparator();
		private java.util.PriorityQueue<KThread> waitQueue =
				new java.util.PriorityQueue<KThread>(ThreadedKernel.numThreads, tComp);
		
		public DynamicPriorityThreadQueue(boolean transferPriority) {
			super(transferPriority);
		}

		public void waitForAccess(KThread thread) {
			Lib.assertTrue(Machine.interrupt().disabled(),
					"Interrupts not disabled in required critical section.");
			
			waitQueue.add(thread);
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
			
			return waitQueue.poll();
		}
		
		/* This method iterates over all the threads in the KThread
		 * readyQueue and removes them from the Queue.  While doing so it
		 * updates their priorites and adds them to a new queue so the order
		 * of the queue will be correctly updated.  Then it sets the old waitQueue to the
		 * new wait queue. 
		 */
		public void updatePriority(){
			Machine.interrupt().disable();
			java.util.PriorityQueue<KThread> newQueue =
					new java.util.PriorityQueue<KThread>(ThreadedKernel.numThreads, tComp);
			KThread thread;
			while (waitQueue.size() != 0){
				thread = waitQueue.poll();
				getThreadState(thread).updateDynamicPriority();
				newQueue.add(thread);
			}
			waitQueue = newQueue;
		}
		
		
		
	}

	/* This method grabs the agingTime used in the Dynamic Scheduler
	 * from the ThreadedKernel class
	 */
	public void setAgingTime(double num) {
		agingTime = num;
	}
	
	/* This variable will be accessed by the ThreadState class
	 * in computing effective priorites
	 */
	public static double agingTime;

	
}
