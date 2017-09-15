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

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import tools.descartes.dlim.httploadgenerator.generator.ArrivalRateTuple;
import tools.descartes.dlim.httploadgenerator.power.IPowerCommunicator;

/**
 * Director that is run in director mode.
 * @author Joakim von Kistowski
 *
 */
public class Director extends Thread {

	private static final Logger LOG = Logger.getLogger(Director.class.getName());
	
	private static int seed = 5;

	private List<LoadGeneratorCommunicator> communicators;
	
	/**
	 * Execute the director with the given parameters.
	 * Parameters may be null. Director asks the user for null parameters if they are required.
	 * @param profilePath The path of the LIMBO-generated load profile.
	 * @param outName The name of the output log file.
	 * @param powerAddress The address of the power daemon (optional).
	 * @param generator The address of the load generator(s).
	 * @param randomSeed The random seed for exponentially distributed request arrivals.
	 * @param threadCount The number of threads that generate load.
	 * @param urlTimeout The url connection timeout.
	 * @param scriptPath The path of the script file that generates the specific requests.
	 * @param powerCommunicatorClassName Fully qualified class name of the power communicator class.
	 */
	public static void executeDirector(String profilePath, String outName, String powerAddress, String generator,
			String randomSeed, String threadCount, String urlTimeout, String scriptPath,
			String powerCommunicatorClassName) {
		try {
			Scanner scanner = new Scanner(System.in);
			IPowerCommunicator powerCommunicator = null;
			
			//Load Profile
			File file = null;
			if (profilePath != null) {
				file = new File(profilePath);
			} else {
				System.out.print("Load Profile Path: ");
				file = new File(scanner.nextLine());
			}

			//Logfile
			if (outName == null) {
				LOG.info("Using default log: " + IRunnerConstants.DEFAULT_LOG);
				outName = IRunnerConstants.DEFAULT_LOG;
			}

			//Power measurement
			if (powerCommunicatorClassName != null && !powerCommunicatorClassName.trim().isEmpty()
					&& powerAddress != null && !powerAddress.isEmpty()) {
				powerAddress = powerAddress.trim();
				String[] host = powerAddress.split(":");
				int port = 22444;
				if (host.length > 1) {
					port = Integer.parseInt(host[1].trim());
				}
				
				try {
					Class<? extends IPowerCommunicator> pcClass
						= Class.forName(powerCommunicatorClassName.trim()).asSubclass(IPowerCommunicator.class);
					powerCommunicator = pcClass.newInstance();
					powerCommunicator.initializePowerCommunicator(host[0].trim(), port);
				} catch (ClassNotFoundException e) {
					LOG.severe("PowerCommunicator class not found: " + powerCommunicatorClassName);
				} catch (InstantiationException e) {
					LOG.severe("PowerCommunicator class could not be instantiated: " + powerCommunicatorClassName);
					LOG.severe(e.getMessage());
				} catch (IllegalAccessException e) {
					LOG.severe("PowerCommunicator class could not be accessed: " + powerCommunicatorClassName);
					LOG.severe(e.getMessage());
				}
			} else {
				LOG.warning("No power measurements");
			}

			//Director Address
			String generatorAddress;
			if (generator != null) {
				generatorAddress = generator.trim();
			} else {
				LOG.warning("No load generator address, using localhost.");
				generatorAddress = IRunnerConstants.LOCALHOST_IP;
			}

			//Random Seed
			boolean randomBatchTimes = true;
			String seedStr = randomSeed;
			if (seedStr == null) {
				LOG.info("No Random Seed for Batch Generation specified. "
						+ "This parameter is unneeded for request time stamp generation.");
				randomBatchTimes = false;
				LOG.info("Using equi-distant non-random inter batch times.");
			} else {
				try {
					seed = Integer.parseInt(seedStr.trim());
					System.out.println("Seed set to: " + seedStr.trim());
				} catch (NumberFormatException e) {
					randomBatchTimes = false;
					LOG.warning("Invalid seed, using equi-distant non-random inter batch times.");
				}
			}
			
			//Thread Count
			int threadNum = IRunnerConstants.DEFAULT_THREAD_NUM;
			if (threadCount != null) {
				try {
					threadNum = Integer.parseInt(threadCount);
					LOG.info("Load Generator Thread Count set to " + threadCount);
				} catch (NumberFormatException e) {
					LOG.warning("Invalid Thread Count: " + threadCount);
				}
			} else {
				LOG.info("Using default load generation thread count: " + threadNum);
			}
			
			//Thread Count
			int timout = -1;
			if (urlTimeout != null) {
				try {
					timout = Integer.parseInt(urlTimeout);
					LOG.info("URL connection timout set to " + threadCount + " ms");
				} catch (NumberFormatException e) {
					LOG.warning("Invalid timout: " + threadCount);
				}
			} else {
				LOG.info("No timout specified.");
			}
			
			//Script Path
			String scriptPathRead;
			if (scriptPath != null) {
				scriptPathRead = scriptPath.trim();
			} else {
				LOG.warning("No Lua script path provided. Using: " + IRunnerConstants.DEFAULT_LUA_PATH);
				scriptPathRead = IRunnerConstants.DEFAULT_LUA_PATH;
			}

			if (file != null && outName != null && !outName.isEmpty()) {
				Director director = new Director(generatorAddress.split(":")[0].trim());
				director.process(file, outName, scanner, randomBatchTimes,
						threadNum, timout, scriptPathRead, powerCommunicator);
			}
			if (powerCommunicator != null) {
				powerCommunicator.stopCommunicator();
			}
			scanner.close();

		} catch (IOException e1) {
			LOG.log(Level.SEVERE, "Power Daemon error.");
			e1.printStackTrace();
		}
	}

	/**
	 * Inititializes a director with a load generator address.
	 * @param loadGenerators Addresses of the load generator. Seperated by ",".
	 */
	public Director(String loadGenerators) {
		String[] addresses = loadGenerators.split("[,;]");
		communicators = new ArrayList<>(addresses.length);
		for (String address : addresses) {
			String[] addressTokens = address.split(":");
			String ip = addressTokens[0].trim();
			if (!ip.isEmpty()) {
				int port = IRunnerConstants.DEFAULT_PORT;
				if (addressTokens.length > 1 && !addressTokens[1].trim().isEmpty()) {
					try {
						port = Integer.parseInt(addressTokens[1].trim());
					} catch (NumberFormatException e) {
						port = IRunnerConstants.DEFAULT_PORT;
					}
				}
				communicators.add(new LoadGeneratorCommunicator(ip, port));
			}
		}
	}

	/**
	 * Actually run the director. Sends the messages to the load generator and collects data.
	 * @param file The arrival rate file.
	 * @param outName The name of the output log.
	 * @param scanner The scanner for reading user start signal from console.
	 * @param randomBatchTimes True if batches are scheduled using a randomized distribution.
	 * @param threadCount The number of threads that generate load.
	 * @param timeout The connection timeout for the HTTP url connections.
	 * @param scriptPath The path of the script file that generates the specific requests.
	 * @param powerCommunicator Communicator to communicate with power daemon (optional).
	 */
	public void process(File file, String outName, Scanner scanner, boolean randomBatchTimes,
			int threadCount, int timeout, String scriptPath, IPowerCommunicator powerCommunicator) {

		try {
			List<ArrivalRateTuple> arrRates = Main.readFileToList(file, 0);
			LOG.info("Read " + arrRates.size() + " Arrival Rate Tuples");
			communicators.parallelStream().forEach(c-> c.sendArrivalRates(arrRates, communicators.size()));
			LOG.info("Arrival Rates sent to Load Generator(s).");

			communicators.parallelStream().forEach(c-> c.sendThreadCount(threadCount));
			LOG.info("Thread Count sent to Load Generator(s): " + threadCount);

			communicators.parallelStream().forEach(c-> c.sendTimeout(timeout));
			if (timeout > 0) {
				LOG.info("URL connection timeout sent to Load Generator(s): " + timeout);
			}
			
			communicators.parallelStream().forEach(c-> c.sendLUAScript(scriptPath));
			LOG.info("Contents of script sent to Load Generator: " + scriptPath);
			
			String parentPath = file.getParent();
			if (parentPath == null || parentPath.isEmpty()) {
				parentPath = ".";
			}
			PrintWriter writer = new PrintWriter(parentPath + "/" + outName);
			writer.print("Target Time,Load Intensity,Successful Transactions,"
			 + "Failed Transactions,Final Batch Dispatch Time,Watts");

			System.out.print("Press Enter to begin Execution");
			outName = scanner.nextLine();

			//setup initial run Variables
			ExecutorService executor = null;
			SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy;HH:mm:ssSSS");
			if (powerCommunicator != null) {
				executor = Executors.newSingleThreadExecutor();
				executor.execute(powerCommunicator);
			}
			long timeZero = communicators.parallelStream()
					.mapToLong(c -> c.startBenchmarking(randomBatchTimes, seed)).min().getAsLong();
			String dateString = sdf.format(new Date(timeZero));
			System.out.println("Beginning Run @" + timeZero + "(" + dateString + ")");
			writer.println("," + dateString);

			//get Data from LoadGenerator
			while (!collectResultRound(powerCommunicator, writer)) {
				//collectResultRound blocking waits. We don't need to wait here.
			}
			System.out.println("Workload finished.");
			writer.close();
			System.out.println("Log finished.");
			if (powerCommunicator != null) {
				powerCommunicator.stopCommunicator();
				executor.shutdown();
			}


		} catch (IOException e) {
			LOG.severe("File not found.");
			e.printStackTrace();
		}
	}
	
	/**
	 * Collects one iteration of the results, aggregates them and logs them.
	 * Returns false if more results are expected in the future.
	 * Returns true if the measurements have concluded and the "done" signal was received from all communicators.
	 * @return True, if the measurement has concluded.
	 */
	private boolean collectResultRound(IPowerCommunicator powerCommunicator, PrintWriter writer) {
		int finishedCommunicators = 0;
		double targetTime = 0;
		int loadIntensity = 0;
		int successfulTransactions = 0;
		int failedTransactions = 0;
		ArrayList<Double> finalBatchTimes = new ArrayList<Double>();
		for (LoadGeneratorCommunicator communicator : communicators) {
			if (communicator.isFinished()) {
				finishedCommunicators++;
				if (finishedCommunicators == communicators.size()) {
					return true;
				}
			} else {
				String receivedResults = communicator.getLatestResultMessageBlocking();
				if (receivedResults == null) {
					finishedCommunicators++;
					if (finishedCommunicators == communicators.size()) {
						return true;
					}
				} else {
					String[] tokens = receivedResults.split(",");
					double receivedTargetTime = Double.parseDouble(tokens[0].trim());
					if (targetTime == 0) {
						targetTime = receivedTargetTime;
					} else {
						if (targetTime != receivedTargetTime) {
							LOG.severe("Time mismatch in load generator responses! Measurement invalid.");
						}
					}
					loadIntensity += Integer.parseInt(tokens[1].trim());
					successfulTransactions += Integer.parseInt(tokens[2].trim());
					failedTransactions += Integer.parseInt(tokens[3].trim());
					finalBatchTimes.add(Double.parseDouble(tokens[4].trim()));
				}
			}
		}
		double finalBatchTime = finalBatchTimes.stream().mapToDouble(d -> d.doubleValue()).max().getAsDouble();
		logState(targetTime, loadIntensity, successfulTransactions,
				failedTransactions, finalBatchTime, powerCommunicator, writer);
		return false;
	}

	private void logState(double targetTime, int loadIntensity, int successfulTransactions, int failedTransactions,
			double finalBatchTime, IPowerCommunicator powerCommunicator, PrintWriter writer) {
		//get Power
		double power = 0;
		if (powerCommunicator != null) {
			power = powerCommunicator.getPowerMeasurement();
		}
		System.out.println("Target Time = " + targetTime
				+ "; Load Intensity = " + loadIntensity
				+ "; Successful Transactions = " + successfulTransactions
				+ "; Failed Transactions = " + failedTransactions);
		writer.println(targetTime + "," + loadIntensity + ","
				+ successfulTransactions + "," + failedTransactions + ","
				+ finalBatchTime + "," + power);
	}

}
