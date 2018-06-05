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

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

/**
 * Offers tracking of results, such as response times and
 * invalid transactions using atomic counter operations.
 * @author Joakim von Kistowski
 *
 */
public final class ResultTracker {

	private static final Logger LOG = Logger.getLogger(ResultTracker.class.getName());
	
	/**
	 * The tracker singleton.
	 */
	public static final ResultTracker TRACKER = new ResultTracker();
	
	private long invalidTransactionsPerMeasurementInterval = 0;
	private long invalidTransactionsTotal = 0;
	private long droppedTransactionsPerMeasurementInterval = 0;
	private long droppedTransactionsTotal = 0;
	
	private BlockingQueue<Long> responseTimeQueue = new LinkedBlockingQueue<>();
	
	private ResultTracker() {
		
	}
	
	/**
	 * Resets the validity tracker.
	 */
	public synchronized void reset() {
		invalidTransactionsPerMeasurementInterval = 0;
		invalidTransactionsTotal = 0;
		droppedTransactionsPerMeasurementInterval = 0;
		droppedTransactionsTotal = 0;
		responseTimeQueue.clear();
	}
	
	/**
	 * Adds an invalid transaction to the counter.
	 */
	public synchronized void incrementInvalidTransactionCount() {
		invalidTransactionsPerMeasurementInterval++;
		invalidTransactionsTotal++;
	}
	
	/**
	 * Adds an dropped transaction to the counter.
	 */
	public synchronized void incrementDroppedTransactionCount() {
		droppedTransactionsPerMeasurementInterval++;
		droppedTransactionsTotal++;
	}
	
	/**
	 * Returns the current invalid transaction count and resets the counter.
	 * @return The current invalid transaction count.
	 */
	public synchronized long getAndResetInvalidTransactionCount() {
		long tmpCount = invalidTransactionsPerMeasurementInterval;
		invalidTransactionsPerMeasurementInterval = 0;
		return tmpCount;
	}
	
	/**
	 * Returns the current dropped transaction count and resets the counter.
	 * @return The current dropped transaction count.
	 */
	public synchronized long getAndResetDroppedTransactionCount() {
		long tmpCount = droppedTransactionsPerMeasurementInterval;
		droppedTransactionsPerMeasurementInterval = 0;
		return tmpCount;
	}
	
	/**
	 * Returns the total invalid transaction counter since initialization or the last call of {@link #reset()}.
	 * @return The total invalid transaction counter.
	 */
	public synchronized long getTotalInvalidTransactionCount() {
		return invalidTransactionsTotal;
	}
	
	/**
	 * Returns the total dropped transaction counter since initialization or the last call of {@link #reset()}.
	 * @return The total dropped transaction counter.
	 */
	public synchronized long getTotalDroppedTransactionCount() {
		return droppedTransactionsTotal;
	}
	
	/**
	 * Append a response time measurement (in ms) to the logging queue.
	 * @param responseTimeMs The resonse time in ms.
	 */
	public void logResponseTime(long responseTimeMs) {
		try {
			responseTimeQueue.put(responseTimeMs);
		} catch (InterruptedException e) {
			LOG.severe("Error logging response time: " + e.getMessage());
		}
	}
	
	/**
	 * Returns the average response time for all recently logged results in seconds.
	 * Clears the result storage for new results.
	 * @return The average response time in seconds.
	 */
	public double getAverageResponseTimeInS() {
		 Long timems;
		 int count = 0;
		 double timeSumInS = 0.0;
		 while ((timems = responseTimeQueue.poll()) != null) {
			 count++;
			 timeSumInS += (double) timems / 1000.0;
		 }
		 if (count == 0) {
			 return 0.0;
		 }
		 return timeSumInS / count;
	}
}
