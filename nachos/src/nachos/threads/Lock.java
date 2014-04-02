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

	
	/** Effective priority for the lock holder. */
	protected int effPriority;
	
	/** Point to the lock holder. */
	private KThread lockHolder = null;
	
	/** Point the thread with the maximum priority. Can be waiting or the lock holder. */
	private KThread maxPThread = null;
	
	/** Use a LinkedList to implement FIFO queue for waiting threads. */
	private LinkedList<KThread> waitQueue;
	//private ThreadQueue waitQueue = ThreadedKernel.scheduler.newThreadQueue(true);
	
	/**
	 * Allocate a new lock. The lock will initially be <i>free</i>.
	 */
	public Lock() {
		effPriority = ThreadedKernel.scheduler.priorityMaximum;
		waitQueue = new LinkedList<KThread>();
	}

	/**
	 * Atomically acquire this lock. The current thread must not already hold
	 * this lock.
	 */
	public void acquire() {
		Lib.assertTrue(!isHeldByCurrentThread());

		boolean intStatus = Machine.interrupt().disable();
		KThread thread = KThread.currentThread();
		
		// Log aquisition attempt
		long curtime = ThreadedKernel.scheduler.kernel.getTime();
		ThreadedKernel.scheduler.kernel.logprintln(
				String.format("W,%s,%d,%d,%d", this.toString(), curtime,
						thread.getID(), thread.thdSchedState.getEffectivePriority()));
		
		if (lockHolder != null) {
			// The thread attempting to acquire will have to block
			
			
			if (ThreadedKernel.usePriorityDonation) {
				int newEffP = thread.thdSchedState.getEffectivePriority();
				// If the new thread has a higher effective priority than the current lock effective priority, 
				// 		update the effective priority of the lock and signal to the lock holder that there has been a change
				if (newEffP < effPriority) {
					effPriority = newEffP;
					maxPThread = thread;
					lockHolder.thdSchedState.updateEP();				
				}
			}
			
			// add to the wait queue and sleep
			waitQueue.addFirst(thread);
			KThread.sleep();
		}
		else {
			// Lock is available, so the thread can acquire it.
			lockHolder = thread;
			if (ThreadedKernel.usePriorityDonation) {
				
				// Update effective priority of lock to be this thread.
				maxPThread = thread;
				effPriority = thread.thdSchedState.getEffectivePriority();
				
				// Have the acquiring thread add the lock to its set of locks held.
				lockHolder.thdSchedState.acquireLock(this);
			}
			
			// Log the acquisition
			curtime = ThreadedKernel.scheduler.kernel.getTime();
			ThreadedKernel.scheduler.kernel.logprintln(
					String.format("A,%s,%d,%d,%d", this.toString(), curtime,
							thread.getID(), thread.thdSchedState.getEffectivePriority()));

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
		
		// Print info to kernel
		long curtime = ThreadedKernel.scheduler.kernel.getTime();
		ThreadedKernel.scheduler.kernel.logprintln(
				String.format("R,%s,%d,%d,%d", this.toString(), curtime, lockHolder.getID(),
						lockHolder.thdSchedState.getEffectivePriority()));
		
		if (ThreadedKernel.usePriorityDonation) lockHolder.thdSchedState.releaseLock(this);
		if (!waitQueue.isEmpty()) {
			// If the thread releasing had the max priority, find next highest in wait queue
			if (lockHolder == maxPThread && ThreadedKernel.usePriorityDonation) updateEP();
			
			// Get next blocked thread from waitqueue
			lockHolder = waitQueue.removeLast();
			
			// Update this next lock holder's effective priority
			if (ThreadedKernel.usePriorityDonation) lockHolder.thdSchedState.acquireLock(this);

			// Log acquisition
			curtime = ThreadedKernel.scheduler.kernel.getTime();
			ThreadedKernel.scheduler.kernel.logprintln(
					String.format("A,%s,%d,%d,%d", this.toString(), curtime,
							lockHolder.getID(), lockHolder.thdSchedState.getEffectivePriority()));
			
			// Make the new lock holder runnable
			lockHolder.ready();
		}
		else if (ThreadedKernel.usePriorityDonation) {
			lockHolder = null;
			effPriority = ThreadedKernel.scheduler.priorityMaximum;
		}


		Machine.interrupt().restore(intStatus);
	}
	
	
	/**
	 * Update the effective priority associated with the lock by iterating through all 
	 * the threads waiting. To be called when the highest priority thread is the lock 
	 * holder and it releases the lock.
	 */
	private void updateEP() {
		effPriority = ThreadedKernel.scheduler.priorityMaximum;
		maxPThread = null;
		
		for (KThread thread : waitQueue) {
			if (thread.thdSchedState.getEffectivePriority() < effPriority) {
				effPriority = thread.thdSchedState.getEffectivePriority();
				maxPThread = thread;
			}
		}
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
