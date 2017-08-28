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

import java.util.concurrent.LinkedBlockingQueue;

import tools.descartes.httploadgenerator.http.HTTPTransaction;

/**
 * Singleton for saving a linked blocking queue of transaction instances for
 * better performance of the load generator. Every working thread takes a free
 * transaction instance of the queue. The transaction instances can be reused
 * and do not have to be initialized every time.
 * 
 * @author Joakim von Kistowski, Maximilian Deffner
 *
 */
public final class TransactionQueueSingleton {

	/** A linked blocking queue of transactions for better performance. */
	private LinkedBlockingQueue<Runnable> transactionQueue = new LinkedBlockingQueue<Runnable>();

	/** One hidden instance of the type of the own class. */
	private static TransactionQueueSingleton instance = null;

	/**
	 * Private constructor for preventing the creation of the object in other
	 * methods.
	 */
	private TransactionQueueSingleton() {
	}

	/**
	 * Get the transaction queue.
	 * @return The transaction queue.
	 */
	public static synchronized TransactionQueueSingleton getInstance() {
		if (instance == null) {
			instance = new TransactionQueueSingleton();
		}
		return instance;
	}

	/**
	 * Creates a new queue of transactions.
	 */
	public void renewQueue() {
		transactionQueue = new LinkedBlockingQueue<Runnable>();
		// Initialize Transactions before run
		for (int i = 0; i < 200; i++) {
			transactionQueue.add(new HTTPTransaction());
		}
	}

	/**
	 * Getter method for accessing the blocked linked queue with transaction
	 * instances.
	 * 
	 * @return Queue with transaction instances.
	 */
	public Runnable getQueueElement() {
		Runnable temp = transactionQueue.element();
		transactionQueue.remove();
		return temp;
	}

	/**
	 * Adding new element to the blocked linked queue with transaction
	 * instances.
	 * 
	 * @param transaction
	 *            The transaction to be added to the list.
	 */
	public void addQueueElement(Runnable transaction) {
		transactionQueue.add(transaction);
	}

	/**
	 * Checking if transaction queue is empty.
	 * 
	 * @return True if transaction queue is empty.
	 */
	public Boolean queueIsEmpty() {
		return transactionQueue.isEmpty();
	}
}
