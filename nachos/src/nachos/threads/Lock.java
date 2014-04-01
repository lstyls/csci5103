package nachos.threads;

import java.util.ArrayList;
import java.util.LinkedList;

import nachos.machine.*;

/**
 * A <tt>Lock</tt> is a synchronization primitive that has two states,
 * <i>busy</i> and <i>free</i>. There are only two operations allowed on a
 * lock:
 *
 * <ul>
 * <li><tt>acquire()</tt>: atomically wait until the lock is <i>free</i> and
 * then set it to <i>busy</i>.
 * <li><tt>release()</tt>: set the lock to be <i>free</i>, waking up one
 * waiting thread if possible.
 * </ul>
 *
 * <p>
 * Also, only the thread that acquired a lock may release it. As with
 * semaphores, the API does not allow you to read the lock state (because the
 * value could change immediately after you read it).
 */
public class Lock {
	/**
	 * Allocate a new lock. The lock will initially be <i>free</i>.
	 */
	
	protected int effPriority;
	private KThread lockHolder = null;
	private KThread maxPThread = null;
	private LinkedList<KThread> waitQueue;
	//private ThreadQueue waitQueue = ThreadedKernel.scheduler.newThreadQueue(true);
	
	
	public Lock() {
		if (ThreadedKernel.usePriorityDonation) {
			effPriority = ThreadedKernel.scheduler.priorityMaximum;
			waitQueue = new LinkedList<KThread>();
		}
	}

	/**
	 * Atomically acquire this lock. The current thread must not already hold
	 * this lock.
	 */
	public void acquire() {
		Lib.assertTrue(!isHeldByCurrentThread());

		boolean intStatus = Machine.interrupt().disable();
		KThread thread = KThread.currentThread();

		if (lockHolder != null) {
			if (ThreadedKernel.usePriorityDonation) {
				int newEffP = thread.thdSchedState.getEffectivePriority();
				if (newEffP < effPriority) {
					effPriority = newEffP;
					maxPThread = thread;
					lockHolder.thdSchedState.updateEP(this);				
				}
			}
			waitQueue.addFirst(thread);
			KThread.sleep();
		}
		else {
			lockHolder = thread;
			if (ThreadedKernel.usePriorityDonation) {
				maxPThread = thread;
				effPriority = thread.thdSchedState.getEffectivePriority();
			}
		}

		Lib.assertTrue(lockHolder == thread);

		Machine.interrupt().restore(intStatus);
	}

	/**
	 * Atomically release this lock, allowing other threads to acquire it.
	 */
	public void release() {
		Lib.assertTrue(isHeldByCurrentThread());

		boolean intStatus = Machine.interrupt().disable();

		if (ThreadedKernel.usePriorityDonation) lockHolder.thdSchedState.releaseLock(this);
		if (!waitQueue.isEmpty()) {
			// If the thread releasing had the max priority, find next highest in wait queue
			if (lockHolder == maxPThread && ThreadedKernel.usePriorityDonation) updateEP();
			
			// Get next blocked thread from waitqueue
			lockHolder = waitQueue.removeLast();
			
			// Update this next lock holder's effective priority
			if (ThreadedKernel.usePriorityDonation) lockHolder.thdSchedState.updateEP(this);
			
			// Make the new lock holder runnable
			lockHolder.ready();
		}
		else if (ThreadedKernel.usePriorityDonation) {
			lockHolder = null;
			effPriority = ThreadedKernel.scheduler.priorityMaximum;
		}


		Machine.interrupt().restore(intStatus);
	}
	
	private void updateEP() {
		
	}
	

	/**
	 * Test if the current thread holds this lock.
	 *
	 * @return	true if the current thread holds this lock.
	 */
	public boolean isHeldByCurrentThread() {
		return (lockHolder == KThread.currentThread());
	}

}
