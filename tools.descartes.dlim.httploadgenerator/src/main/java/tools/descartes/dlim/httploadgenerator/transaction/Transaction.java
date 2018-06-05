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

/**
 * Abstract transaction class.
 * @author Joakim von Kistowski
 *
 */
public abstract class Transaction implements Runnable {

	//problemsize for computing problems
	private static int problemsize = 10000;
	
	private long startTimeMs = 0;
	
	@Override
	public abstract void run();
	
	/**
	 * Setup the transaction.
	 * @param problemsize Problemsize to compute.
	 */
	public void setup(int problemsize) {
		Transaction.problemsize = problemsize;
	}
	
	/**
	 * Return the problem size.
	 * @return The problem size.
	 */
	protected static int getProblemSize() {
		return problemsize;
	}
	
	/**
	 * Set the start time of the transaction when queuing it into the threadpool.
	 * This start time may then be used for execution time logging, etc.
	 * @param startTimeMs The start time in system milliseconds.
	 */
	public void setStartTime(long startTimeMs) {
		this.startTimeMs = startTimeMs;
	}
	
	/**
	 * Gets the Transaction's start time. The start time is the time at which it was queued
	 * into the threadpool.
	 * @return The start time in system milliseconds.
	 */
	public long getStartTime() {
		return startTimeMs;
	}

}
