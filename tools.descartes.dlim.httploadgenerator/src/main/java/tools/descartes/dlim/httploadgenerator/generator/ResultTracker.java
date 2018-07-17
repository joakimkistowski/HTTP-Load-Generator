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

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Offers tracking of results, such as response times and
 * invalid transactions using atomic counter operations.
 * @author Joakim von Kistowski
 *
 */
public final class ResultTracker {
	
	/**
	 * The tracker singleton.
	 */
	public static final ResultTracker TRACKER = new ResultTracker();
	
	private ReentrantLock transactionLock = new ReentrantLock();
	
	private AtomicLong invalidTransactionsPerMeasurementInterval = new AtomicLong(0);
	private AtomicLong invalidTransactionsTotal = new AtomicLong(0);
	private AtomicLong droppedTransactionsPerMeasurementInterval = new AtomicLong(0);
	private AtomicLong droppedTransactionsTotal = new AtomicLong(0);
	private AtomicLong successfulTransactionsPerMeasurementInterval = new AtomicLong(0);
	private AtomicLong successfulTransactionsTotal = new AtomicLong(0);
	
	private AtomicLong responseTimeSum = new AtomicLong(0);
	private AtomicLong responseTimeLogCount = new AtomicLong(0);
	
	private ResultTracker() {
		
	}
	
	/**
	 * Log a transaction.
	 * @param responseTimeMs The response time, ignored in non-successful transactions.
	 * @param finishingState The finishing state.
	 */
	public void logTransaction(long responseTimeMs, TransactionState finishingState) {
		transactionLock.lock();
		try {
			switch (finishingState) {
				case FAILED:
					invalidTransactionsPerMeasurementInterval.incrementAndGet();
					invalidTransactionsTotal.incrementAndGet();
					break;
				case DROPPED:
					droppedTransactionsPerMeasurementInterval.incrementAndGet();
					droppedTransactionsTotal.incrementAndGet();
					break;
				default:
					responseTimeSum.addAndGet(responseTimeMs);
					responseTimeLogCount.incrementAndGet();
					successfulTransactionsPerMeasurementInterval.incrementAndGet();
					successfulTransactionsTotal.incrementAndGet();
					break;
			}
		} finally {
			transactionLock.unlock();
		}
	}
	
	/**
	 * Resets the validity tracker.
	 */
	public void reset() {
		transactionLock.lock();
		try {
			invalidTransactionsPerMeasurementInterval.set(0);
			invalidTransactionsTotal.set(0);
			droppedTransactionsPerMeasurementInterval.set(0);
			droppedTransactionsTotal.set(0);
			successfulTransactionsPerMeasurementInterval.set(0);
			successfulTransactionsTotal.set(0);
			responseTimeSum.set(0);
			responseTimeLogCount.set(0);
		} finally {
			transactionLock.unlock();
		}
	}
	
	
	/**
	 * Returns the total invalid transaction counter since initialization or the last call of {@link #reset()}.
	 * @return The total invalid transaction counter.
	 */
	public long getTotalInvalidTransactionCount() {
		long invTrans;
		transactionLock.lock();
		try {
			invTrans = invalidTransactionsTotal.get();
		} finally {
			transactionLock.unlock();
		}
		return invTrans;
	}
	
	/**
	 * Returns the total successful transaction counter since initialization or the last call of {@link #reset()}.
	 * @return The total successful transaction counter.
	 */
	public long getTotalSuccessfulTransactionCount() {
		long sucTrans;
		transactionLock.lock();
		try {
			sucTrans = successfulTransactionsTotal.get();
		} finally {
			transactionLock.unlock();
		}
		return sucTrans;
	}
	
	/**
	 * Returns the total dropped transaction counter since initialization or the last call of {@link #reset()}.
	 * @return The total dropped transaction counter.
	 */
	public long getTotalDroppedTransactionCount() {
		long dropTrans;
		transactionLock.lock();
		try {
			dropTrans = droppedTransactionsTotal.get();
		} finally {
			transactionLock.unlock();
		}
		return dropTrans;
	}
	
	/**
	 * Returns the average response time for all recently logged results in seconds.
	 * Clears the result storage for new results.
	 * @return The average response time in seconds.
	 */
	private double getAverageResponseTimeInS() {
		long avgResponseTimeMs;
			if (responseTimeLogCount.get() == 0) {
				avgResponseTimeMs = 0;
			} else {
				avgResponseTimeMs = responseTimeSum.getAndSet(0) / responseTimeLogCount.getAndSet(0);
			}
		return ((double) avgResponseTimeMs) / 1000.0;
	}
	
	public IntervalResult retreiveIntervalResultAndReset() {
		IntervalResult result = new IntervalResult();
		transactionLock.lock();
		try {
			result.droppedTransactions = droppedTransactionsPerMeasurementInterval.getAndSet(0);
			result.failedTransactions = invalidTransactionsPerMeasurementInterval.getAndSet(0);
			result.successfulTransactions = successfulTransactionsPerMeasurementInterval.getAndSet(0);
			result.averageResponseTimeInS = getAverageResponseTimeInS();
		} finally {
			transactionLock.unlock();
		}
		return result;
	}
	
	/**
	 * States that a transaction may have upon finishing.
	 * @author Joakim von Kistowski
	 *
	 */
	public static enum TransactionState {
		/**
		 * Transaction finished successfully.
		 */
		SUCCESS,
		/**
		 * Transaction failed.
		 */
		FAILED,
		/**
		 * Transaction was dropped and never executed.
		 */
		DROPPED;
	}
	
	/**
	 * Result of a measurement interval.
	 * @author Joakim von Kistowski
	 */
	public static class IntervalResult {
		
		private long droppedTransactions = 0;
		private long failedTransactions = 0;
		private long successfulTransactions = 0;
		private double averageResponseTimeInS = 0.0;
		
		private IntervalResult() { }
		
		/**
		 * Returns the number of dropped transactions.
		 * @return The number of dropped transactions.
		 */
		public long getDroppedTransactions() {
			return droppedTransactions;
		}

		/**
		 * Returns the number of failed transactions.
		 * @return The number of failed transactions.
		 */
		public long getFailedTransactions() {
			return failedTransactions;
		}

		/**
		 * Returns the number of successful transactions.
		 * @return The number of successful transactions
		 */
		public long getSuccessfulTransactions() {
			return successfulTransactions;
		}

		/**
		 * Returns the average response time in Seconds.
		 * @return The average response time.
		 */
		public double getAverageResponseTimeInS() {
			return averageResponseTimeInS;
		}

	}
}
