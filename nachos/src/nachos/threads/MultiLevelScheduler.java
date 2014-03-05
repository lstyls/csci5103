package nachos.threads;

import java.util.LinkedList;

import nachos.machine.Lib;
import nachos.machine.Machine;

public class MultiLevelScheduler extends StaticPriorityScheduler {
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
		 
		public ThreadState(KThread thread) {
			super(thread);
			
			this.uncountedRunTime = 0;
			this.uncountedWaitTime = 0;
		}
		
		
		public void updatePriority() {
			if (this.thread.isIdleThread()) return;
			long curtime = kernel.getTime();
			long newRunTime = curtime + uncountedRunTime - this.lastScheduled;
			int increment = (int) (newRunTime/( (long) agingTime));
			this.uncountedRunTime = newRunTime % agingTime;
			setFixPriority(thread, increment);
		}
		
	}
	
	
	private class PriorityThreadQueue extends PriorityScheduler.PriorityThreadQueue {

		LinkedList<KThread> lev1;
		KThread main;
		
		public PriorityThreadQueue(boolean transferPriority) {
			super(transferPriority);
			
			lev1 = new LinkedList<KThread>();
		}

		@Override
		public void waitForAccess(KThread thread) {
			Lib.assertTrue(Machine.interrupt().disabled(), "Interrupts not disabled in critical section.");
			if (thread.isMainThread()) {
				this.main = thread;
			}
			else {
				lev1.add(thread);
			}
		}

		@Override
		public void acquire(KThread thread) {
		    Lib.assertTrue(Machine.interrupt().disabled());
		       
		    Lib.assertTrue(lev1.isEmpty());
		}

		@Override
		public KThread nextThread() {
			if (this.isEmpty()) return main;
			return lev1.removeFirst();
		}
		
		public boolean isEmpty() {
			return lev1.isEmpty();
		}
	}
	
/*	private class MultiLevelThreadQueue extends PriorityScheduler.PriorityThreadQueue {

		private LinkedList<KThread> level1 = new LinkedList<KThread>();
		private LinkedList<KThread> level2 = new LinkedList<KThread>();
		private LinkedList<KThread> level3 = new LinkedList<KThread>();
		
		private long agingTime;
		private MultiLevelScheduler scheduler;
		
		public MultiLevelThreadQueue(boolean transferPriority) {
			super(transferPriority);
		}
		
		public MultiLevelThreadQueue() {
			super(false);
		}

		public void agePriorities() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void waitForAccess(KThread thread) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void acquire(KThread thread) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public KThread nextThread() {
			// TODO Auto-generated method stub
			return null;
		}
				
	}*/

}
