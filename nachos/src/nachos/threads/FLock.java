package nachos.threads;

import nachos.machine.*;

import java.util.*;

public class FLock {
	//variables
	private KThread lockHolder = null;
	private java.util.LinkedList<KThread> lockQueue = new java.util.LinkedList<KThread>();
	boolean intStatus;
		
		//Constructor
		public FLock(){
			
		}
		
		/**
	     * Try to acquire this lock. The current thread must not already hold
	     * this lock.
	     */
	    public void acquire(KThread k) {

	    	intStatus = Machine.interrupt().disable();
	    	KThread thread = k;
	    	
	    	if (lockHolder == null){
	    		lockHolder = thread;
	    		System.out.println(thread.getName() + " in acquire lock null");
	    		Iterator<KThread> itr = lockQueue.iterator();
	    		int highestPriority = thread.thdSchedState.getPriority();
	    		while(itr.hasNext()){
	    			KThread next = itr.next();
	    			System.out.println("hello");
	    			if (next.thdSchedState.getPriority() < highestPriority){
	    				highestPriority = next.thdSchedState.getPriority();
	    			}
	    		}
	    		thread.thdSchedState.inheritPriority(highestPriority);
	    	}else{
	    		System.out.println("right before add queue");
	    		lockQueue.add(thread);
	    		if (thread.thdSchedState.getPriority() < lockHolder.thdSchedState.getPriority()){
	    			lockHolder.thdSchedState.cascadePriority(thread.thdSchedState.getPriority());
	    		}
	    		System.out.println(thread.getName() + " is put to sleep");
	    		thread.sleep();
	    	}
	    	
	    	Machine.interrupt().restore(intStatus);
	    	
	    }
	    
	    public void release(KThread k){
	    	intStatus = Machine.interrupt().disable();
	    	KThread thread = k;
	    	thread.thdSchedState.restorePriority();
	    	lockHolder = null;
	    	System.out.println(thread.getName() + " in release");
	    	if (lockQueue.peek() != null){
	    		KThread nextThread = lockQueue.poll();
	    		acquire(nextThread);
	    		System.out.println(nextThread.getName() + " is woken up");
	    		nextThread.ready();
	    	}
	    	Machine.interrupt().restore(intStatus);
	    }
}
