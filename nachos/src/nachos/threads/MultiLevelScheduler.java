package nachos.threads;

import java.util.LinkedList;
import java.util.ListIterator;

import nachos.machine.Lib;
import nachos.machine.Machine;

public class MultiLevelScheduler extends PriorityScheduler {
	public MultiLevelScheduler() {
		super();
	}
	

	
	public ThreadState getThreadState(KThread thread) {
		if (thread.thdSchedState == null) initThreadState(thread);
		return (ThreadState) thread.thdSchedState;
	}
	
	
	protected void initThreadState(KThread thread) {
		thread.thdSchedState = new MultiLevelScheduler.ThreadState(thread);
	}


	@Override
	public PriorityThreadQueue newThreadQueue(boolean transferPriority) {
		return new PriorityThreadQueue(transferPriority);
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
	
	
	private class PriorityThreadQueue extends PriorityScheduler.PriorityThreadQueue {

		LinkedList<KThread> lev1;
		LinkedList<KThread> lev2;
		LinkedList<KThread> lev3;
		
		KThread main;
		
		public PriorityThreadQueue(boolean transferPriority) {
			super(transferPriority);
			
			lev1 = new LinkedList<KThread>();
			lev2 = new LinkedList<KThread>();
			lev3 = new LinkedList<KThread>();
		}

		@Override
		public void waitForAccess(KThread thread) {
			Lib.assertTrue(Machine.interrupt().disabled(), "Interrupts not disabled in critical section.");
			if (thread.isMainThread()) {
				this.main = thread;
			}
			else {
				enqueue(thread);
			}
		}
		
		private void enqueue(KThread thread) {
			int p = thread.thdSchedState.getPriority();
			Lib.assertTrue(p <= priorityMaximum && p >= 1);
			if (1 <= p && p <= 10) lev1.add(thread);
			else if (11 <= p && p <= 20) lev2.add(thread);
			else lev3.add(thread);
		}
		
		private KThread dequeue() {
			if (!lev1.isEmpty()) return lev1.removeFirst();
			if (!lev2.isEmpty()) return lev2.removeFirst();
			return lev3.removeFirst();
		}

		@Override
		public void acquire(KThread thread) {
		    Lib.assertTrue(Machine.interrupt().disabled());
		       
		    Lib.assertTrue(lev1.isEmpty());
		}

		@Override
		public KThread nextThread() {
			if (this.isEmpty()) return main;
			this.ageWaiting();
			return dequeue();
		}
		
		public boolean isEmpty() {
			return (lev1.isEmpty() && lev2.isEmpty() && lev3.isEmpty());
		}
		
		
		protected void ageWaiting() {
			Lib.assertTrue(Machine.interrupt().disabled());
			
			LinkedList<KThread> requeue = new LinkedList<KThread>();
			
			ageLevel(this.lev1, requeue, 1, 10);
			ageLevel(this.lev2, requeue, 11, 20);
			ageLevel(this.lev3, requeue, 21);
			
			for (KThread thread : requeue) {
				this.enqueue(thread);
			}
			
			
		}
		
		private void ageLevel(LinkedList<KThread> level, LinkedList<KThread> requeue, int min) {
			this.ageLevel(level, requeue, min, priorityMaximum);
		}
		
		private void ageLevel(LinkedList<KThread> level, LinkedList<KThread> requeue, int min, int max) {
			KThread curthread;
			ListIterator<KThread> li = level.listIterator();
			
			while (li.hasNext()) {
				curthread = li.next();
				curthread.thdSchedState.ageValDown();
				int p = curthread.thdSchedState.getPriority();
				if (p<min || p>max){
					li.remove();
					requeue.add(curthread);
				}
			}
		}
	}
}
