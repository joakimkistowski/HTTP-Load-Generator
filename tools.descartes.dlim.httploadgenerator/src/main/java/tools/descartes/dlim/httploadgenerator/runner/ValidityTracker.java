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

/**
 * Offers tracking of invalid transactions using atomic counter operations.
 * @author Joakim von Kistowski
 *
 */
public final class ValidityTracker {

	/**
	 * The tracker singleton.
	 */
	public static final ValidityTracker TRACKER = new ValidityTracker();
	
	private long invalidTransactionsPerMeasurementInterval = 0;
	private long invalidTransactionsTotal = 0;
	
	private ValidityTracker() {
		
	}
	
	/**
	 * Resets the validity tracker.
	 */
	public synchronized void reset() {
		invalidTransactionsPerMeasurementInterval = 0;
		invalidTransactionsTotal = 0;
	}
	
	/**
	 * Adds an invalid transaction to the counter.
	 */
	public synchronized void incrementInvalidTransctionCount() {
		invalidTransactionsPerMeasurementInterval++;
		invalidTransactionsTotal++;
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
	 * Returns the total invalid transaction counter since initialization or the last call of {@link #reset()}.
	 * @return The total invalid transaction counter.
	 */
	public synchronized long getTotalInvalidTransactionCount() {
		return invalidTransactionsTotal;
	}
}
