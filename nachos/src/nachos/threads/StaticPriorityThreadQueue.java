package nachos.threads;

import java.util.Comparator;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.concurrent.PriorityBlockingQueue;

import nachos.machine.*;
import nachos.threads.StaticPriorityScheduler.ThreadState;

public class StaticPriorityThreadQueue extends ThreadQueue {
	
	private SingleLevelThreadComparator tComp = new SingleLevelThreadComparator();
	private PriorityQueue waitQueue = new PriorityQueue<KThread>();

	public StaticPriorityThreadQueue(boolean transferPriority) {
		// do nothing in constructor
	}

	protected class SingleLevelThreadComparator implements Comparator<KThread>{

		@Override
		public int compare(KThread t1, KThread t2) {
			int p1 = ((ThreadState) t1.schedulingState).priority;
			int p2 = ((ThreadState) t2.schedulingState).priority;
			int comp = p1-p2;
			
			if (comp < 0) return -1;
			
			if (comp > 0) return 1;
			
			return 0;
			
		}
		
	}
	
	@Override
	public void waitForAccess(KThread thread) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public KThread nextThread() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void acquire(KThread thread) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void print() {
		// TODO Auto-generated method stub
		
	}

}
