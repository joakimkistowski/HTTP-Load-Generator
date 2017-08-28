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

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A container class containing an arrival rate and its time-stamp. Also offers
 * some utilities.
 * 
 * @author Joakim von Kistowski
 */
public class ArrivalRateTuple implements Comparable<ArrivalRateTuple> {

	private static boolean sortByTime = false;

	private double timeStamp;
	private double arrivalRate;

	/**
	 * Create a new tuple of arrival rate and its time-stamp.
	 * 
	 * @param timeStamp
	 *            The time stamp.
	 * @param arrivalRate
	 *            The arrival rate.
	 */
	public ArrivalRateTuple(double timeStamp, double arrivalRate) {
		this.timeStamp = timeStamp;
		this.arrivalRate = arrivalRate;
	}

	/**
	 * Get the time-difference between two arrival rate tuples. Returns
	 * time-stamp/2 if null is passed.
	 * 
	 * @param t
	 *            The other tuple (next tuple).
	 * @return The step difference between the tuples.
	 */
	public double getStep(ArrivalRateTuple t) {
		if (t == null) {
			return timeStamp * 2;
		}
		return Math.abs(t.getTimeStamp() - timeStamp);
	}

	/**
	 * The time stamp.
	 * @return The time stamp.
	 */
	public double getTimeStamp() {
		return timeStamp;
	}

	/**
	 * Set the time stamp.
	 * @param timeStamp The time stamp.
	 */
	public void setTimeStamp(double timeStamp) {
		this.timeStamp = timeStamp;
	}

	/**
	 * The arrival rate.
	 * @return The arrival rate.
	 */
	public double getArrivalRate() {
		return arrivalRate;
	}

	/**
	 * Set the arrival rate.
	 * @param arrivalRate The arrival rate.
	 */
	public void setArrivalRate(double arrivalRate) {
		this.arrivalRate = arrivalRate;
	}

	/**
	 * Checks if arrival rate tuples are sorted by time or arrival rate. This is
	 * a static global setting!
	 * 
	 * @return True if sorted by time.
	 */
	public static boolean isSortByTime() {
		return sortByTime;
	}

	/**
	 * Set whether arrival rate tuples are to be sortet by time or arrival rate.
	 * This is a static global setting!
	 * 
	 * @param sortByTime True if to sort by time.
	 */
	public static void setSortByTime(boolean sortByTime) {
		ArrivalRateTuple.sortByTime = sortByTime;
	}

	/**
	 * Compares two arrival rate tuples within one another. Uses either
	 * time-stamp or arrival rate based on isSortByTime().
	 * 
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(ArrivalRateTuple o) {
		if (sortByTime) {
			if (timeStamp < o.getTimeStamp()) {
				return -1;
			} else if (timeStamp > o.getTimeStamp()) {
				return 1;
			}
		} else {
			if (arrivalRate < o.getArrivalRate()) {
				return -1;
			} else if (arrivalRate > o.getArrivalRate()) {
				return 1;
			}
		}
		return 0;
	}

	/**
	 * Returns a simple output String for the arrival rate tuple.
	 * @return A string representation.
	 */
	public String toString() {
		return timeStamp + "," + arrivalRate + ";";
	}

	/**
	 * Read an arrival rate tuple list from a stream.
	 * @param br The stream reader.
	 * @param offset The time stamp offset where to start parsing.
	 * @return A list of arrival rate tuples.
	 * @throws IOException IOException during parsing.
	 */
	public static List<ArrivalRateTuple> readList(BufferedReader br, double offset) throws IOException {
		return readList(br, offset, Integer.MAX_VALUE);
	}

	/**
	 * Read an arrival rate tuple list from a stream.
	 * @param br The stream reader.
	 * @param offset The time stamp offset where to start parsing.
	 * @param maxTuples The max number of tuples to read.
	 * @return A list of arrival rate tuples.
	 * @throws IOException IOException during parsing.
	 */
	public static List<ArrivalRateTuple> readList(BufferedReader br, double offset, int maxTuples) throws IOException {
		ArrayList<ArrivalRateTuple> arrRates = new ArrayList<ArrivalRateTuple>();
		String line;
		int i = 0;
		while ((line = br.readLine()) != null) {
			if (line.endsWith(";")) {
				line = line.substring(0, line.length() - 1);
			}
			String[] numbers = line.split(",");
			if (numbers.length >= 2) {
				try {
					double timeStamp = Double.parseDouble(numbers[0].trim());
					double readArrivalRate = Double.parseDouble(numbers[1].trim());
					timeStamp = timeStamp - offset;
					if (timeStamp > 0) {
						arrRates.add(new ArrivalRateTuple(timeStamp, readArrivalRate));
					}
				} catch (NumberFormatException e) {

				}
			}
			i++;

			if (i >= maxTuples) {
				break;
			}
		}
		return arrRates;
	}

}
