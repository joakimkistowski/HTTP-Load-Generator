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

import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;


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

	/** The constant logging instance. */
	private static final Logger LOG = Logger.getLogger(TransactionQueueSingleton.class.getName());
	
	/** A linked blocking queue of transactions for better performance. */
	private LinkedBlockingQueue<Transaction> transactionQueue = new LinkedBlockingQueue<Transaction>();

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
	 * Resets and initializes a number of transactions. The pre-initialization is intended to reduce
	 * dispatching times during load generation.
	 * @param transactionClass Class of the transaction to initialize. Must have a default Constructor.
	 * @param numInitialTransactions Number of transactions to pre-initialize.
	 */
	public void resetAndpreInitializeTransactions(Class<? extends Transaction> transactionClass, int numInitialTransactions) {
		transactionQueue = new LinkedBlockingQueue<Transaction>();
		for (int i = 0; i < numInitialTransactions; i++) {
			try {
				transactionQueue.add(transactionClass.newInstance());
			} catch (InstantiationException e) {
				LOG.severe("Error instantiating transaction object of class " + transactionClass.getName()
				+ "; Does the class have the default Constructor?\n Exception: " + e.getMessage());
			} catch (IllegalAccessException e) {
				LOG.severe("IllegalAccessException intantiating transaction class: " + e.getMessage());
			}
		}
	}

	/**
	 * Getter method for accessing the blocked linked queue with transaction
	 * instances.
	 * 
	 * @return Queue with transaction instances.
	 */
	public Transaction getQueueElement() {
		return transactionQueue.poll();
	}

	/**
	 * Adding new element to the blocked linked queue with transaction
	 * instances.
	 * 
	 * @param transaction
	 *            The transaction to be added to the list.
	 */
	public void addQueueElement(Transaction transaction) {
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
