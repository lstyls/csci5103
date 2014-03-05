package nachos.threads;

import java.util.LinkedList;

import nachos.machine.Lib;
import nachos.machine.Machine;

//public class MultiLevelScheduler extends PriorityScheduler {
public class MultiLevelScheduler extends StaticPriorityScheduler {
	public MultiLevelScheduler() {
		super();
	}
	
	private StaticPriorityThreadQueue threadQueue;
	
	public StaticPriorityThreadQueue getThreadQueue() {
		return threadQueue;
	}
	

	private long agingTime;

	protected void setAgingTime(long agingTime) {
		this.agingTime = agingTime;
	}
	
	@Override
	public PriorityThreadQueue newThreadQueue(boolean transferPriority) {
		// TODO Auto-generated method stub
		return new StaticPriorityThreadQueue(transferPriority);
	}
	
	
	protected class ThreadState extends StaticPriorityScheduler.ThreadState {
		
		private long uncountedRunTime;
		private long uncountedWaitTime;
		 
		public ThreadState(KThread thread) {
			super(thread);
			
			this.uncountedRunTime = 0;
			this.uncountedWaitTime = 0;
		}
		
		
		protected void updatePriority() {
			if (this.thread.isIdleThread()) return;
			long curtime = kernel.getTime();
			curtime = 100;
			long uncountedRunTime = 22;
			long newRunTime = curtime + uncountedRunTime - this.lastScheduled;
			int increment = (int) (newRunTime/( (long) agingTime));
			this.uncountedRunTime = newRunTime % agingTime;
			MultiLevelScheduler.this.setPriority(this.thread,this.priority+increment, true);
		}
		
	}
	
/*	private class MultiLevelThreadQueue extends StaticPriorityScheduler.StaticPriorityThreadQueue {

		public MultiLevelThreadQueue(
				boolean transferPriority) {
			super(transferPriority);
			// TODO Auto-generated constructor stub
		}
		
	}*/
	
	
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
