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
package tools.descartes.dlim.httploadgenerator.runner;

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

import tools.descartes.dlim.httploadgenerator.generator.ArrivalRateTuple;
import tools.descartes.dlim.httploadgenerator.http.HTTPInputGeneratorPool;
import tools.descartes.dlim.httploadgenerator.http.HTTPTransaction;

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

	/** Amount of completed transactions. */
	private long lastCompletedCount = 0;

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
	protected void process(boolean randomBatchTimes, int seed) {
		r.setSeed(seed);

		try {
			// setup initial run Variables
			HTTPInputGeneratorPool.initializePool(getScriptPath(), numberOfThreads, getTimeout());
			LinkedBlockingQueue<Runnable> executorQueue = new LinkedBlockingQueue<Runnable>();
			executor = new ThreadPoolExecutor(numberOfThreads, numberOfThreads, 0, TimeUnit.MILLISECONDS,
					executorQueue);
			TransactionQueueSingleton.getInstance().preInitializeTransactions(HTTPTransaction.class, 400);

			/*
			 * Mean wait time between batches of transactions is 10 ms or 1/10th
			 * of the time between two arrival rate tuples.
			 */
			int defaultMeanWaitTime = Math.min(10, (int) (arrRates.get(0).getTimeStamp() * 1000) / 10);
			long timeZero = System.currentTimeMillis();
			lastCompletedCount = 0;
			double nextTimeStamp = 0;

			int targetArrivalsInInterval = 0;

			for (ArrivalRateTuple t : arrRates) {
				long currentTime = System.currentTimeMillis() - timeZero;

				// set target arrival rate and next time target
				targetArrivalsInInterval += (int) t.getArrivalRate();
				long targetTime = (long) (1000.0 * t.getTimeStamp());
				
				//Set mean wait time. Ensure it is not too short for very low loads.
				long meanWaitTime = defaultMeanWaitTime;
				if (targetArrivalsInInterval < 10 && targetArrivalsInInterval > 1) {
					meanWaitTime = (targetTime - currentTime) / targetArrivalsInInterval; 
				}

				/*
				 * Dispatches the work in small batches that are then
				 * parallelized. Batch sizes are set so that the expected number
				 * of batches is timeToNextArrivalRateTuple/meanWaitTime. Then
				 * runs each batch and waits slightly randomized for the next
				 * batch to start.
				 */
				while (targetArrivalsInInterval > 0) {

					TransactionBatch batch = new TransactionBatch(targetTime, currentTime, meanWaitTime,
							targetArrivalsInInterval);
					batch.executeBatch(executor);
					targetArrivalsInInterval -= batch.getBatchSize();

					sleep(batch.getPostBatchSleepTime(r, randomBatchTimes));

					currentTime = System.currentTimeMillis() - timeZero;
				}

				sendBatchDataToDirector(t.getTimeStamp(), (int) t.getArrivalRate(), ((double) currentTime) / 1000);
				nextTimeStamp = t.getTimeStamp() * 1000;
			}

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
			executor.shutdown();

		} catch (InterruptedException e) {
			LOG.log(Level.SEVERE, "Interrupted: " + e.getMessage());
		}
	}

	/**
	 * Sending results to the director after every interval.
	 * 
	 * @param targettime Target time when load was supposed to be executed.
	 * @param loadintensity The load intensity to be reached.
	 * @param actualtime The actual time of execution.
	 */
	private void sendBatchDataToDirector(double targettime, int loadintensity, double actualtime) {
		long currentCompletedCount = executor.getCompletedTaskCount();
		long invalidTransactionCount = ResultTracker.TRACKER.getAndResetInvalidTransactionCount();
		double avgResponseTime = ResultTracker.TRACKER.getAverageResponseTimeInS();
		sendToDirector(targettime, loadintensity,
				(currentCompletedCount - lastCompletedCount - invalidTransactionCount),
				avgResponseTime, invalidTransactionCount, actualtime);
		lastCompletedCount = currentCompletedCount;
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
