
/**
 * Copyright 2018 Joakim von Kistowski
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
 */package tools.descartes.dlim.httploadgenerator.runner;

 /**
  * Container for all interval results received by the director.
  * @author Joakim von Kistowski
  *
  */
public class IntervalResult {
	
	private double targetTime;
	private double loadIntensity;
	private int successfulTransactions;
	private double avgResponseTime;
	private int failedTransactions;
	private int droppedTransactions;
	private double finalBatchTime;
	private boolean measurementConcluded = false;
	
	public static IntervalResult createIntervalResultWithMeasurementConcludedFlag() {
		return new IntervalResult();
	}
	
	/**
	 * Creates a new interval result with measurement results.
	 * @param targetTime he target time.
	 * @param loadIntensity The load intensity.
	 * @param successfulTransactions Number of successful transactions.
	 * @param failedTransactions Number of failed transactions.
	 * @param droppedTransactions Number of dropped transactions.
	 * @param avgResponseTime The average response time.
	 * @param finalBatchTime The final batch time.
	 */
	public IntervalResult(double targetTime, double loadIntensity, int successfulTransactions,
			int failedTransactions, int droppedTransactions,
			double avgResponseTime, double finalBatchTime) {
		this.targetTime = targetTime;
		this.loadIntensity = loadIntensity;
		this.successfulTransactions = successfulTransactions;
		this.failedTransactions = failedTransactions;
		this.droppedTransactions = droppedTransactions;
		this.avgResponseTime = avgResponseTime;
		this.finalBatchTime = finalBatchTime;
		this.measurementConcluded = false;
	}
	
	/**
	 * Creates an interval result where measurement has concluded.
	 * No further results need be provided.
	 */
	private IntervalResult() {
		this.measurementConcluded = true;
	}

	/**
	 * Returns the target time.
	 * @return The target time.
	 */
	public double getTargetTime() {
		return targetTime;
	}

	/**
	 * Returns the load intensity.
	 * @return The load intensity.
	 */
	public double getLoadIntensity() {
		return loadIntensity;
	}

	/**
	 * Returns the number of successful transactions.
	 * @return Number of successful transactions.
	 */
	public int getSuccessfulTransactions() {
		return successfulTransactions;
	}

	/**
	 * Returns the average response time.
	 * @return The average response time.
	 */
	public double getAvgResponseTime() {
		return avgResponseTime;
	}

	/**
	 * Returns the number of failed transactions.
	 * @return Number of failed transactions.
	 */
	public int getFailedTransactions() {
		return failedTransactions;
	}

	/**
	 * Returns the number of dropped transactions.
	 * @return Number of dropped transactions.
	 */
	public int getDroppedTransactions() {
		return droppedTransactions;
	}

	/**
	 * Returns the final batch time.
	 * @return The final batch time.
	 */
	public double getFinalBatchTime() {
		return finalBatchTime;
	}

	/**
	 * Returns true if measurement has concluded. False, otherwise.
	 * @return If the measurement has concluded.
	 */
	public boolean isMeasurementConcluded() {
		return measurementConcluded;
	}

}
