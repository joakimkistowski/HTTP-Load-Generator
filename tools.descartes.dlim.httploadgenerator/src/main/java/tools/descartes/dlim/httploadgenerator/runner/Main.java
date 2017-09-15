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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import tools.descartes.dlim.httploadgenerator.generator.ArrivalRateTuple;

/**
 * Main class is entry point of the application. Passed arguments are checked
 * and corresponding mode is started (Director, Load Generator)
 * 
 * @author Joakim von Kistowski, Maximilian Deffner
 *
 */
public class Main extends Thread {

	/** The constant logging instance. */
	private static final Logger LOG = Logger.getLogger(Main.class.getName());

	/**
	 * Main method for splitting up passed arguments and starting corresponding
	 * mode.
	 * 
	 * @param args
	 *            Passed arguments by calling the application.
	 */
	public static void main(String[] args) {
		HashMap<String, String> params = new HashMap<String, String>();

		String currentParam = "";
		String currentKey = "";
		for (int i = 0; i < args.length; i++) {
			String word = args[i].trim();
			if (word.startsWith("-")) {
				addToMap(currentKey, currentParam, params);
				currentKey = word.substring(0, 2).toLowerCase();
				currentParam = "";
				if (word.length() > 2) {
					currentParam = word.substring(2);
				}
			} else {
				currentParam += word;
			}
		}
		addToMap(currentKey, currentParam, params);
		if (params.get("-h") != null || args.length == 0) {
			printHelp();
		}

		if (params.get("-d") != null) {
			Director.executeDirector(params.get("-a"), params.get("-o"), params.get("-p"), params.get("-s"),
					params.get("-r"), params.get("-t"), params.get("-u"), params.get("-l"), params.get("-c"));
		} else if (params.get("-l") != null) {
			AbstractLoadGenerator.executeLoadGenerator();
		}
	}

	/**
	 * Method for printing the help instructions to the console.
	 */
	private static void printHelp() {
		System.out.println("Welcome to LIMBO HTTP Load Generator");
		System.out.println("Usage:");
		System.out.println("   java -jar httploadgenerator.jar [-d|-l|-h [optional params]]");
		System.out.println("Example:");
		System.out.println(
				"   java -jar httploadgenerator.jar -d -s 192.168.0.201 "
						+ "-a ./arrivalRates/test.txt -o myLog.csv -p 127.0.0.1:8888 "
						+ "-c tools.descartes.dlim.httploadgenerator.power.TMCTLDCommunicator -l ./http_calls.lua");
		System.out.println("");
		System.out.println("Primary parameters (pick one):");
		System.out.println(
				"   \"-d\": \'d\'irector mode. starts the director. Additional optional parameters are useful.");
		System.out.println("   \"-l\": \'l\'oad generator mode. Needs no additional parameters.");
		System.out.println("   \"-h\": this \'h\'elp page.");
		System.out.println("");
		System.out.println("Secondary parameters for director (optional):");
		System.out.println("Missing parameters may cause the director to prompt for the data.");
		System.out.println(
				"   \"-s [ip]\": Adre\'s\'s of load generator(s). Multiple addresses must be delimited with \",\".");
		System.out.println("   \"-p [ip[:port]]\": Adress of \'p\'owerDaemon. No address => no power measurements.");
		System.out.println("   \"-a [path]\": Path of LIMBO-generated \'a\'rrival rate file.");
		System.out.println("   \"-o [name]\": Name of \'o\'utput log relative to directory of arrival rate file.");
		System.out.println(
				"   \"-r [seed]\": Integer seed for the \'r\'andom generator. No seed => Equi-distant dispatch times.");
		System.out.println(
				"   \"-l [Lua script]\": Path of the \'l\'ua script that generates the call URLs. "
						+ "No script => \"http_calls.lua\".");
		System.out.println("   \"-t [thread count]\": Number of threads in load generator. No thread count => 128.");
		System.out.println("   \"-u [url con timeout]\": \'U\'rl connection timeout in ms. Default => no timout.");
		System.out.println("   \"-c [class name]\": Fully qualified classname of the power communicator."
				+ " Must be on the classpath.");
	}

	/**
	 * Creating a hash map of the received arguments.
	 * 
	 * @param key
	 *            Key starting with "-" like -d of received arguments of the
	 *            main method.
	 * @param params
	 *            Received parameters of the arguments of the main method.
	 * @param map
	 *            Hash map in which the keys and parameters are stored.
	 */
	private static void addToMap(String key, String params, HashMap<String, String> map) {
		if (!key.isEmpty()) {
			map.put(key, params.trim());
		}
	}

	/**
	 * Reads an arrival rate list file to an arrival rate list. Also capable of
	 * reading request time stamp files. In this case, all arrival rates in the
	 * list are "-1".
	 * 
	 * @param file
	 *            The file to read.
	 * @param offset
	 *            The time offset on which to start reading.
	 * @return A list of arrival rate tuples or request time stamps (arrival
	 *         rates = "-1").
	 * @throws IOException
	 *             If file is not found, can not be read, etc.
	 */
	public static List<ArrivalRateTuple> readFileToList(File file, double offset) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(file));
		List<ArrivalRateTuple> arrRates = new ArrayList<ArrivalRateTuple>();
		// read arrival rate tuples
		try {
			arrRates = ArrivalRateTuple.readList(br, offset);
			// error reading tuples, it's probably a request time stamp file
		} catch (IndexOutOfBoundsException e) {
			LOG.log(Level.SEVERE, "Index out of bounds, wrong format.");
		}
		if (arrRates.size() == 0) {
			br.close();
			br = new BufferedReader(new FileReader(file));
			arrRates = readRequestTimeStamps(br, offset);
		}
		br.close();
		return arrRates;
	}

	/**
	 * Parses the request time stamps of the specified file.
	 * 
	 * @param br
	 *            Buffer reader for reading the content of the file
	 * @param offset
	 *            Adding an offset to the time stamps
	 * @return Array list with parsed time stamps
	 * @throws IOException
	 *             If file is not found, can not be read, etc.
	 */
	private static List<ArrivalRateTuple> readRequestTimeStamps(BufferedReader br, double offset) throws IOException {
		ArrayList<ArrivalRateTuple> timeStamps = new ArrayList<ArrivalRateTuple>();
		String line = br.readLine();
		while (line != null) {
			line = line.trim();
			if (!line.isEmpty()) {
				double ts;
				try {
					ts = Double.parseDouble(line);
				} catch (NumberFormatException e) {
					ts = (Double.parseDouble(line.substring(0, line.length() - 1)));
				}
				timeStamps.add(new ArrivalRateTuple(ts - offset, -1.0));
			}
			line = br.readLine();
		}
		return timeStamps;
	}

}
