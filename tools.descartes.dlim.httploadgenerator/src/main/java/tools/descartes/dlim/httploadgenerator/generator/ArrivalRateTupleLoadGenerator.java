/**
 * Copyright 2017 Joakim von Kistowski
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package tools.descartes.dlim.httploadgenerator.generator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import tools.descartes.dlim.httploadgenerator.http.HTTPInputGeneratorPool;
import tools.descartes.dlim.httploadgenerator.http.HTTPTransaction;
import tools.descartes.dlim.httploadgenerator.runner.IRunnerConstants;
import tools.descartes.dlim.httploadgenerator.transaction.TransactionBatch;
import tools.descartes.dlim.httploadgenerator.transaction.TransactionQueueSingleton;

/**
 * The class ArrivalRateTupleLoadGenerator is a child of the
 * AbstractLoadGenerator for receiving and interpreting the transferred arrival
 * rates. It triggers a thread pool of workers for executing the transaction.
 * The number of executions of the transactions is monitored.
 * 
 * @author Joakim von Kistowski, Maximilian Deffner
 *
 */
public class ArrivalRateTupleLoadGenerator extends AbstractLoadGenerator {

	/** The constant logging instance. */
	private static final Logger LOG = Logger.getLogger(ArrivalRateTupleLoadGenerator.class.getName());

	/**
	 * Thread pool for executing the transactions in parallel threads for
	 * generating load.
	 */
	private ThreadPoolExecutor executor;

	/** Arrival rates saved in a list. */
	private List<ArrivalRateTuple> arrRates;


	/** Number of threads for generating load. */
	private static int numberOfThreads = 128;

	/** Generation of random numbers. */
	private static Random r = new Random();

	/**
	 * New instance of the class.
	 * 
	 * @param director
	 *            Socket for the communication with the director
	 * @param in
	 *            Buffered reader for the communication with the director
	 * @param out
	 *            Print writer for the communication with the director
	 */
	public ArrivalRateTupleLoadGenerator(Socket director, BufferedReader in, PrintWriter out) {
		super(director, in, out);
	}

	@Override
	protected void readLoadProfile(BufferedReader in, String header) {
		// get Arrival rate count
		int count = Integer.parseInt(header.trim().split(",")[1].trim());

		try {
			LOG.log(Level.INFO, "Receiving " + count + " Arrival Rates.");
			arrRates = ArrivalRateTuple.readList(in, 0, count);
			LOG.log(Level.INFO, "Received " + arrRates.size() + " Arrival Rate Tuples");
		} catch (IOException e) {
			LOG.log(Level.SEVERE, "Couldn't read Arrival Rates");
			e.printStackTrace();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void process(boolean randomBatchTimes, int seed,
			int warmupDurationS, double warmupLoadIntensity, int warmupPauseS, boolean randomizeUsers) {
		r.setSeed(seed);

		try {
			// setup initial run Variables
			HTTPInputGeneratorPool.PoolMode mode = HTTPInputGeneratorPool.PoolMode.QUEUE;
			if (randomizeUsers) {
				mode = HTTPInputGeneratorPool.PoolMode.RANDOM;
			}
			HTTPInputGeneratorPool.initializePool(mode, getScriptPath(), numberOfThreads, getTimeout(), seed);
			LinkedBlockingQueue<Runnable> executorQueue = new LinkedBlockingQueue<Runnable>();
			executor = new ThreadPoolExecutor(numberOfThreads, numberOfThreads, 0, TimeUnit.MILLISECONDS,
					executorQueue);
			TransactionQueueSingleton.getInstance().resetAndpreInitializeTransactions(HTTPTransaction.class, 400);

			/*
			 * Mean wait time between batches of transactions is 10 ms or 1/10th
			 * of the time between two arrival rate tuples.
			 */
			int defaultMeanWaitTime = Math.min(10, (int) (arrRates.get(0).getTimeStamp() * 1000) / 10);
			
			clearResultTracker();
			
			//Warmup, if not skipped
			if (warmupDurationS > 0 && warmupLoadIntensity >= 1) {
				long warmupStart = System.currentTimeMillis(); 
				int arrivalRate = (int) warmupLoadIntensity;
				for (long targetTime = 1000;
						targetTime <= warmupDurationS * 1000;
						targetTime += 1000) {
					long currentTime = System.currentTimeMillis() - warmupStart;
					
					currentTime = blockingScheduleTransactionBatchesForInterval(arrivalRate,
							warmupStart, currentTime, targetTime, defaultMeanWaitTime, randomBatchTimes);
					//warmup has target times <= 0
					sendBatchDataToDirector((targetTime / 1000) - warmupDurationS - warmupPauseS,
							arrivalRate, ((double) currentTime) / 1000);
				}
				
				//pause after warmup
				long pauseStartTime = System.currentTimeMillis();
				for (long targetTime = 1000;
						targetTime <= warmupPauseS * 1000;
						targetTime += 1000) {
					long currentTime = System.currentTimeMillis();
					Thread.sleep(pauseStartTime + targetTime - currentTime);
					sendBatchDataToDirector((targetTime / 1000) - warmupPauseS, 0,
							//no final dispatch time, since nothing is dispatched
							0.0);
				}
			}
			
			clearResultTracker();
			long timeZero = System.currentTimeMillis();
			double nextTimeStamp = 0;

			for (ArrivalRateTuple t : arrRates) {
				long currentTime = System.currentTimeMillis() - timeZero;

				// set target arrival rate and next time target
				int targetArrivalsInInterval = (int) t.getArrivalRate();
				long targetTime = (long) (1000.0 * t.getTimeStamp());
				
				currentTime = blockingScheduleTransactionBatchesForInterval(targetArrivalsInInterval,
						timeZero, currentTime, targetTime, defaultMeanWaitTime, randomBatchTimes);

				sendBatchDataToDirector(t.getTimeStamp(), (int) t.getArrivalRate(), ((double) currentTime) / 1000);
				nextTimeStamp = t.getTimeStamp() * 1000;
			}

			//wait for remaining transactions to trickle in
			nextTimeStamp += 1000;

			while (executor.getActiveCount() > 0) {
				long currentTime = System.currentTimeMillis() - timeZero;

				while (currentTime - (nextTimeStamp) < -defaultMeanWaitTime) {
					sleep(defaultMeanWaitTime);
					currentTime = System.currentTimeMillis() - timeZero;
				}

				sendBatchDataToDirector(nextTimeStamp / 1000, 0, nextTimeStamp / 1000);
				nextTimeStamp += 1000;
			}
			LOG.log(Level.INFO, "Workload finished, " + executor.getCompletedTaskCount() + " Tasks executed.");
			LOG.log(Level.INFO, "Invalid Transactions: " + ResultTracker.TRACKER.getTotalInvalidTransactionCount());
			LOG.log(Level.INFO, "Dropped Transactions: " + ResultTracker.TRACKER.getTotalDroppedTransactionCount());
			executor.shutdown();

		} catch (InterruptedException e) {
			LOG.log(Level.SEVERE, "Interrupted: " + e.getMessage());
		}
	}

	/**
	 * Dispatches the work in small batches that are then
	 * parallelized. Batch sizes are set so that the expected number
	 * of batches is timeToNextArrivalRateTuple/meanWaitTime. Then
	 * runs each batch and waits slightly randomized for the next
	 * batch to start.
	 * @param targetArrivalsInInterval The number of transactions to schedule before time target hits.
	 * @param timeZero Time of experiment start.
	 * @param currentTime The current time.
	 * @param targetTime The target time at which the current load intensity target is to be met.
	 * @param meanWaitTime The mean time to wait between batches.
	 * @param randomBatchTimes Weather or not batch waiting times should be randomized.
	 * @return The time of the last scheduled batch.
	 * @throws InterruptedException If thread sleep does weird things.
	 */
	private long blockingScheduleTransactionBatchesForInterval(int targetArrivalsInInterval,
			long timeZero, long currentTime, long targetTime, long meanWaitTime, boolean randomBatchTimes)
					throws InterruptedException {
		//Set mean wait time. Ensure it is not too short for very low loads.
		long actualMeanWaitTime =
				calculateMeanWaitTime(meanWaitTime, targetTime, currentTime, targetArrivalsInInterval);

		while (targetArrivalsInInterval > 0) {
			targetArrivalsInInterval -= scheduleBatch(targetTime, currentTime,
					actualMeanWaitTime, targetArrivalsInInterval);
			sleep(getPostBatchSleepTime(actualMeanWaitTime, r, randomBatchTimes));
			currentTime = System.currentTimeMillis() - timeZero;
		}
		if (targetArrivalsInInterval > 0) {
			throw new RuntimeException("Target arrivals left after scheduling. This should never happen.");
		}
		return currentTime;
	}
	
	/**
	 * Schedules a batch. Returns the number of placed transactions.
	 * @param targetTime The target time at which the current load intensity target is to be met.
	 * @param currentTime The current time.
	 * @param meanWaitTime The mean time to wait between batches.
	 * @param targetArrivalsInInterval The number of transactions to schedule before time target hits.
	 * @return The number of scheduled transactions.
	 */
	private int scheduleBatch(long targetTime, long currentTime, long meanWaitTime,
			int targetArrivalsInInterval) {
		TransactionBatch batch = new TransactionBatch(targetTime, currentTime, meanWaitTime,
				targetArrivalsInInterval);
		batch.executeBatch(executor);
		return batch.getBatchSize();
	}
	
	/**
	 * Calculates the mean wait time. Effectively uses default mean wait time and guards for some edge cases.
	 * Ensures that it is not not too short for low loads.
	 * @param defaultMeanWaitTime The mean time to wait between batches.
	 * @param targetTime The target time at which the current load intensity target is to be met.
	 * @param currentTime The current time.
	 * @param targetArrivalsInInterval The number of transactions to schedule before time target hits.
	 * @return The mean wait time to use for the current distribution.
	 */
	private long calculateMeanWaitTime(long defaultMeanWaitTime, long targetTime, long currentTime,
			int targetArrivalsInInterval) {
		long meanWaitTime = defaultMeanWaitTime;
		if (targetArrivalsInInterval < 50 && targetArrivalsInInterval > 1) {
			meanWaitTime = (targetTime - currentTime) / (targetArrivalsInInterval + 1); 
		}
		return meanWaitTime;
	}
	
	/**
	 * Returns a waiting time to wait after batch dispatch.
	 * @param r The random generator
	 * @param randomize True if sleep times should be randomized.
	 * @return The waiting time.
	 */
	public long getPostBatchSleepTime(long meanWaitTime, Random r, boolean randomize) {
		if (!randomize) {
			return meanWaitTime;
		}

		// Exponential Random Variable with meanWaitTime as mean
		double randomWaitTime = (0.5 * meanWaitTime) + (-Math.log(r.nextDouble())) * meanWaitTime / 2.0;
		// clamp
		randomWaitTime = Math.max(0.5 * meanWaitTime, randomWaitTime);
		randomWaitTime = Math.min(1.5 * meanWaitTime, randomWaitTime);
		return (long) randomWaitTime;
	}
	
	/**
	 * Sending results to the director after every interval.
	 * 
	 * @param targettime Target time when load was supposed to be executed.
	 * @param loadintensity The load intensity to be reached.
	 * @param actualtime The actual time of execution.
	 */
	private void sendBatchDataToDirector(double targettime, int loadintensity, double actualtime) {
		ResultTracker.IntervalResult result = ResultTracker.TRACKER.retreiveIntervalResultAndReset();
		sendToDirector(targettime, loadintensity, result.getSuccessfulTransactions(),
				result.getAverageResponseTimeInS(), result.getFailedTransactions(),
				result.getDroppedTransactions(), actualtime);
	}
	
	/**
	 * Clear the result tracker. Use at beginning of the measurement phase.
	 */
	private void clearResultTracker() {
		ResultTracker.TRACKER.retreiveIntervalResultAndReset();
	}

	@Override
	protected String loadProfileCommand() {
		return IRunnerConstants.ARRIVALRATE_SEND_KEY;
	}

	/**
	 * Set the number of threads for the load generator.
	 * @param threads Number of threads.
	 */
	public void setNumberOfThreads(int threads) {
		numberOfThreads = threads;
	}
}
