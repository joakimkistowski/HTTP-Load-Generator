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
package tools.descartes.dlim.httploadgenerator.transaction;

import java.util.concurrent.ThreadPoolExecutor;

import tools.descartes.dlim.httploadgenerator.http.HTTPTransaction;

/**
 * A batch of transactions to be scheduled.
 * 
 * @author Joakim von Kistowski
 *
 */
public class TransactionBatch {

	private int size;

	/**
	 * Create a new transaction batch.
	 * 
	 * @param targetTime
	 *            Time when to schedule the batch.
	 * @param currentTime
	 *            Current time.
	 * @param meanWaitInterval
	 *            Mean wait interval between the batches.
	 * @param targetArrivalRate
	 *            Target arrival rate of this interval.
	 */
	public TransactionBatch(long targetTime, long currentTime, long meanWaitInterval, int targetArrivalRate) {
		// calculate Batch Size
		if (targetTime - currentTime <= meanWaitInterval) {
			size = targetArrivalRate;
		} else {
			size = targetArrivalRate / (int) ((targetTime - currentTime) / meanWaitInterval);
		}
	}

	/**
	 * Execute the current batch by placing all transactions in the executor.
	 * 
	 * @param executor
	 *            The thread pool to execute the transactions.
	 */
	public void executeBatch(ThreadPoolExecutor executor) {
		//use a single start time approximation for all transactions in the batch to reduce overhead
		long startTime = System.currentTimeMillis();
		TransactionQueueSingleton transactionQueue = TransactionQueueSingleton.getInstance();
		for (int i = 0; i < size; i++) {
			Transaction transaction = transactionQueue.getQueueElement();
			if (transaction == null) {
				transaction = new HTTPTransaction();
			}
			transaction.setStartTime(startTime);
			executor.execute(transaction);
		}
	}

	/**
	 * Number of transactions in Batch.
	 * 
	 * @return Number of transactions.
	 */
	public int getBatchSize() {
		return size;
	}

}
