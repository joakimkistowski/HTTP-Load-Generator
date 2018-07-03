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
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
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
	
	private ReentrantLock invalidTransactionLock = new ReentrantLock();
	private ReentrantLock droppedTransactionLock = new ReentrantLock();
	private ReentrantLock responseTimeLock = new ReentrantLock();
	
	private AtomicLong invalidTransactionsPerMeasurementInterval = new AtomicLong(0);
	private AtomicLong invalidTransactionsTotal = new AtomicLong(0);
	private AtomicLong droppedTransactionsPerMeasurementInterval = new AtomicLong(0);
	private AtomicLong droppedTransactionsTotal = new AtomicLong(0);
	
	private BlockingQueue<Long> responseTimeQueue = new LinkedBlockingQueue<>();
	
	private ResultTracker() {
		
	}
	
	/**
	 * Resets the validity tracker.
	 */
	public void reset() {
		invalidTransactionLock.lock();
		try {
			invalidTransactionsPerMeasurementInterval.set(0);
			invalidTransactionsTotal.set(0);
		} finally {
			invalidTransactionLock.unlock();
		}
		
		droppedTransactionLock.lock();
		try {
			droppedTransactionsPerMeasurementInterval.set(0);
			droppedTransactionsTotal.set(0);
		} finally {
			droppedTransactionLock.unlock();
		}
		
		responseTimeLock.lock();
		try {
			responseTimeQueue.clear();
		} finally {
			responseTimeLock.unlock();
		}
	}
	
	/**
	 * Adds an invalid transaction to the counter.
	 */
	public void incrementInvalidTransactionCount() {
		invalidTransactionLock.lock();
		try {
			invalidTransactionsPerMeasurementInterval.incrementAndGet();
			invalidTransactionsTotal.incrementAndGet();
		} finally {
			invalidTransactionLock.unlock();
		}
	}
	
	/**
	 * Adds an dropped transaction to the counter.
	 */
	public void incrementDroppedTransactionCount() {
		droppedTransactionLock.lock();
		try {
			droppedTransactionsPerMeasurementInterval.incrementAndGet();
			droppedTransactionsTotal.incrementAndGet();
		} finally {
			droppedTransactionLock.unlock();
		}
	}
	
	/**
	 * Returns the current invalid transaction count and resets the counter.
	 * @return The current invalid transaction count.
	 */
	public long getAndResetInvalidTransactionCount() {
		invalidTransactionLock.lock();
		try {
			return invalidTransactionsPerMeasurementInterval.getAndSet(0);
		} finally {
			invalidTransactionLock.unlock();
		}
	}
	
	/**
	 * Returns the current dropped transaction count and resets the counter.
	 * @return The current dropped transaction count.
	 */
	public long getAndResetDroppedTransactionCount() {
		droppedTransactionLock.lock();
		try {
			return droppedTransactionsPerMeasurementInterval.getAndSet(0);
		} finally {
			droppedTransactionLock.unlock();
		}
	}
	
	/**
	 * Returns the total invalid transaction counter since initialization or the last call of {@link #reset()}.
	 * @return The total invalid transaction counter.
	 */
	public long getTotalInvalidTransactionCount() {
		invalidTransactionLock.lock();
		try {
			return invalidTransactionsTotal.get();
		} finally {
			invalidTransactionLock.unlock();
		}
	}
	
	/**
	 * Returns the total dropped transaction counter since initialization or the last call of {@link #reset()}.
	 * @return The total dropped transaction counter.
	 */
	public long getTotalDroppedTransactionCount() {
		droppedTransactionLock.lock();
		try {
			return droppedTransactionsTotal.get();
		} finally {
			droppedTransactionLock.unlock();
		}
	}
	
	/**
	 * Append a response time measurement (in ms) to the logging queue.
	 * @param responseTimeMs The resonse time in ms.
	 */
	public void logResponseTime(long responseTimeMs) {
		responseTimeLock.lock();
		try {
			responseTimeQueue.add(responseTimeMs);
		} catch (IllegalStateException e) {
			LOG.severe("Error logging response time: " + e.getMessage());
		} finally {
			responseTimeLock.unlock();
		}
	}
	
	/**
	 * Returns the average response time for all recently logged results in seconds.
	 * Clears the result storage for new results.
	 * @return The average response time in seconds.
	 */
	public double getAverageResponseTimeInS() {
		responseTimeLock.lock();
		try {
			 Long timems;
			 int count = 0;
			 double timeSumInS = 0.0;
			 while ((timems = responseTimeQueue.poll()) != null) {
				 count++;
				 timeSumInS += ((double) timems) / 1000.0;
			 }
			 if (count == 0) {
				 return 0.0;
			 }
			 return timeSumInS / count;
		} finally {
			responseTimeLock.unlock();
		}
	}
}
