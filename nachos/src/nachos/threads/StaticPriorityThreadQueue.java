package nachos.threads;

import java.util.Comparator;
import java.util.Iterator;
import java.util.PriorityQueue;

import nachos.machine.*;
import nachos.threads.StaticPriorityScheduler.ThreadState;

public class StaticPriorityThreadQueue extends ThreadQueue {
	
	private SingleLevelThreadComparator tComp = new SingleLevelThreadComparator();
	private PriorityQueue<KThread> waitQueue = new PriorityQueue<KThread>(ThreadedKernel.numThreads, tComp);

	public StaticPriorityThreadQueue(boolean transferPriority) {
		
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
		Lib.assertTrue(Machine.interrupt().disabled());
		
		waitQueue.add(thread);
	}


	public KThread nextThread() {
		Lib.assertTrue(Machine.interrupt().disabled());
		
		return waitQueue.poll();
	}


	public void acquire(KThread thread) {
		Lib.assertTrue(Machine.interrupt().disabled());
		
		Lib.assertTrue(waitQueue.isEmpty());
	}

	public void print() {
		Lib.assertTrue(Machine.interrupt().disabled());
		
		for (Iterator<KThread> i = waitQueue.iterator(); i.hasNext(); )
			System.out.print(i.next() + " ");
		
	}

}
