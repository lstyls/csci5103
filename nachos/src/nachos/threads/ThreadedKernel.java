package nachos.threads;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import nachos.machine.*;

/**
 * A multi-threaded OS kernel.
 */
public class ThreadedKernel extends Kernel {



	/** I/0 file writer to handle logging specification in Part D. */
	private PrintWriter logWriter;

	/**
	 * Allocate a new multi-threaded kernel.
	 */
	public ThreadedKernel() {
		super();
	}

	/**
	 * Initialize this kernel. Creates a scheduler, the first thread, and an
	 * alarm, and enables interrupts. Creates a file system if necessary.   
	 */
	public void initialize(String[] args) {
		// Set scheduler to one of the three assignment types as specified in the config file.
		String schedulerName = Config.getString("ThreadedKernel.scheduler");
		if (schedulerName.equals("nachos.threads.StaticPriorityScheduler")) {
			scheduler = new StaticPriorityScheduler();
			usePriorityDonation = Config.getBoolean("Locks.usePriorityDonation");
		}
		else if (schedulerName.equals("nachos.threads.DynamicPriorityScheduler")) {
			scheduler = new DynamicPriorityScheduler();
			int ageTime = Config.getInteger("scheduler.agingTime");
			scheduler.setAgingTime(ageTime);
		}
		else if (schedulerName.equals("nachos.threads.MultiLevelScheduler")) {
			scheduler = new MultiLevelScheduler();
			int ageTime = Config.getInteger("scheduler.agingTime");
			scheduler.setAgingTime(ageTime);
		}
		else {
			System.err.println("No priority scheduler specified. Will exit.");
			Machine.terminate();
		}

		// assign reference in scheduler to the kernel
		 scheduler.kernel = this;
		 ThreadedKernel.selfTestNum = Config.getInteger("ThreadedKernel.whichTest");

		// set fileSystem
		String fileSystemName = Config.getString("ThreadedKernel.fileSystem");
		if (fileSystemName != null)
			fileSystem = (FileSystem) Lib.constructObject(fileSystemName);
		else if (Machine.stubFileSystem() != null)
			fileSystem = Machine.stubFileSystem();
		else
			fileSystem = null;

		ThreadedKernel.numThreads = Config.getInteger("Kernel.numThreads");
		
		

		
		int maxPriority = Config.getInteger("scheduler.maxPriorityValue");
		scheduler.setSchedMaxPriority(maxPriority);
		
		/* Grab the initialization time */
		inittime = System.currentTimeMillis();

		/* Initialize logfile */
		logWriter = new PrintWriter(System.out);
		
//		String logFileName = Config.getString("statistics.logFile");
//
//		if (logFileName == null) {
//			logWriter = new PrintWriter(System.out);
//		}
//
//		// Initialize logger.
//		else {
//			try{
//				logWriter = new PrintWriter(new FileWriter(logFileName));
//			}
//			catch (IOException err) {
//				System.err.println("Error creating logfile:");
//				System.err.println(err);
//			}
//		}

		// TODO: remove commented test code
		//	logWriter.write("THIS IS A TEST");
		//	logWriter.flush();

		// start threading
		new KThread(null);

		alarm  = new Alarm();

		Machine.interrupt().enable();
	}

	/**
	 * Test this kernel. Test the <tt>KThread</tt>, class. Note that the
	 * autograder never calls this method, so it is safe to put additional
	 * tests here.
	 */	
	public void selfTest() {
		KThread.selfTest();
	}

	/**
	 * A threaded kernel does not run user programs, so this method does
	 * nothing.
	 */
	public void run() {
	}


	/** Print newline to log file */
	protected void logprintln(String str) {
		logWriter.println(str);
		logWriter.flush();
	}


	/** Print line to log file */
	protected void logprint(String str) {
		logWriter.print(str);
		logWriter.flush();
	}

	/**
	 * Terminate this kernel. Never returns.
	 */
	public void terminate() {
		//scheduler.logFinalStats();
		Machine.halt();
	}

	/** method to return time since kernel initialized */
	public long getTime(){
		long curtime = System.currentTimeMillis();
		return curtime-inittime;
	}
	
	/** Indicates which self test is to be run as specified in config file. */
	public static int selfTestNum;

	public static boolean usePriorityDonation;
	
	/** Time of Initialization of the Kernel */
	private long inittime = 0;
	/** Globally accessible reference to the scheduler. */
	public static PriorityScheduler scheduler = null;
	/** Globally accessible reference to the alarm. */
	public static Alarm alarm = null;
	/** Globally accessible reference to the file system. */
	public static FileSystem fileSystem = null;

	public static int numThreads;
}
