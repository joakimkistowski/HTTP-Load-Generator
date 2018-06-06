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
import java.util.logging.Logger;

/**
 * A container class containing an arrival rate and its time-stamp. Also offers
 * some utilities.
 * 
 * @author Joakim von Kistowski
 */
public class ArrivalRateTuple {


	private static final Logger LOG = Logger.getLogger(ArrivalRateTuple.class.getName());
	
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
	 * Returns a simple output String for the arrival rate tuple.
	 * @return A string representation.
	 */
	public String toString() {
		return "(" + timeStamp + "," + arrivalRate + ")";
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
					} else {
						LOG.warning("Parsed non-positive timestamp with value\"" + timeStamp
								+ "\". The load generator supports positive time stamps only.");
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
