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


	/** 
	 * @see nachos.threads.PriorityScheduler#newThreadQueue(boolean)
	 */
	@Override
	public PriorityThreadQueue newThreadQueue(boolean transferPriority) {
		return new PriorityThreadQueue(transferPriority);
	}
	
	protected void initThreadState(KThread thread) {
		thread.thdSchedState = new DynamicPriorityScheduler.ThreadState(thread);
	}
	
	protected class ThreadState extends PriorityScheduler.ThreadState {
		
		private long uncountedRunTime;
		private long uncountedWaitTime;
		private long lastWaitAgeTime;
		
		public ThreadState(KThread thread) {
			super(thread);
			
			this.uncountedRunTime = 0;
			this.uncountedWaitTime = -1;
			this.lastWaitAgeTime = 0;
		}
		
		
		public void ageValUp() {
			if (this.thread.isIdleThread()) return;
			int oldPriority = this.getPriority();
			long curtime = kernel.getTime();
			long newRunTime = curtime + uncountedRunTime - this.lastScheduled;
			int increment = (int) (newRunTime/( (long) agingTime));
			this.uncountedRunTime = newRunTime % agingTime;
			setFixPriority(thread, increment + oldPriority);
		}
		
		public void ageValDown() {
			if (this.thread.isIdleThread()) return;
			int oldPriority = this.getPriority();
			long curtime = kernel.getTime();
			if (lastWaitAgeTime < lastEnqueued) lastWaitAgeTime = lastEnqueued;
			if (lastWaitAgeTime < 0) return;
			long newWaitTime = curtime + uncountedWaitTime - lastWaitAgeTime;
			this.uncountedRunTime = newWaitTime % agingTime;
			int increment = (int) (newWaitTime/((long) agingTime));
			setFixPriority(thread, oldPriority - increment);
		}
		
	}
	
	
	protected class PriorityThreadQueue extends PriorityScheduler.PriorityThreadQueue {

		private PriorityThreadQueue.ThreadComparator tComp = 
				new PriorityThreadQueue.ThreadComparator();
		private java.util.PriorityQueue<KThread> waitQueue =
				new java.util.PriorityQueue<KThread>(ThreadedKernel.numThreads, tComp);
		
		KThread main;
		
		public PriorityThreadQueue(boolean transferPriority) {
			super(transferPriority);
		}

		@Override
		public void waitForAccess(KThread thread) {
			Lib.assertTrue(Machine.interrupt().disabled(), "Interrupts not disabled in critical section.");
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
			updatePriority();
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
				getThreadState(thread).ageValDown();
				newQueue.add(thread);
			}
			waitQueue = newQueue;
		}
		
		
		
	}
}
