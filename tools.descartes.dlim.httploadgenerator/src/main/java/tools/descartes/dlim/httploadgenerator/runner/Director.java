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
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
	 * @param powerAddresses The addresses of the power daemon (optional).
	 * @param generators The addresses of the load generator(s).
	 * @param randomSeed The random seed for exponentially distributed request arrivals.
	 * @param threadCount The number of threads that generate load.
	 * @param urlTimeout The url connection timeout.
	 * @param scriptPath The path of the script file that generates the specific requests.
	 * @param warmupDurationS The duration of a potential warmup period in seconds.
	 * 		Warmup is skipped if the duration is 0.
	 * @param warmupRate The load intensity of the warmup period.
	 * 		Warmup runs a constant load intensity and is skipped if the load is < 1.
	 * @param warmupPauseS The pause after warmup before starting measurement in seconds.
	 * @param randomizeUsers True if users should be randoized.
	 * 		False if they should be taken from a queue in order.
	 * @param powerCommunicatorClassName Fully qualified class name of the power communicator class.
	 */
	public static void executeDirector(String profilePath, String outName, String powerAddresses,
			String generators, int randomSeed, int threadCount, int urlTimeout, String scriptPath,
			boolean randomizeUsers, double warmupRate, int warmupDurationS,
			int warmupPauseS, String powerCommunicatorClassName) {
			List<IPowerCommunicator> powerCommunicators = new LinkedList<>();
			
			//Load Profile
			File file = null;
			if (profilePath != null) {
				file = new File(profilePath);
			} else {
				LOG.severe("No arrival rate profile specified.");
				return;
			}

			String[] generatorIPs = generators.split(",");
			String[] powerIPs = null;
			if (powerAddresses != null && !powerAddresses.isEmpty()) {
				powerIPs = powerAddresses.split(",");
			}
			
			//Power measurement
			if (powerCommunicatorClassName != null && !powerCommunicatorClassName.trim().isEmpty()
					&& powerIPs != null && !(powerIPs.length == 0)) {
				initializePowerCommunicators(powerCommunicators, powerCommunicatorClassName, powerIPs);
			} else if (powerCommunicatorClassName != null && !powerCommunicatorClassName.trim().isEmpty()
					&& (powerIPs == null || powerIPs.length == 0)) {
				LOG.warning("Power Communicator class provided, but no power communication address specified."
						+ " No power measurements will be performed.");
			} else if ((powerCommunicatorClassName == null || powerCommunicatorClassName.trim().isEmpty())
						&& powerIPs != null && !(powerIPs.length == 0)) {
					LOG.warning("Power communication address specified but no Power Communicator class provided."
							+ " No power measurements will be performed.");
			} else {
				LOG.warning("No power measurements");
			}

			//Random Seed
			boolean randomBatchTimes = true;
			if (randomSeed <= 0) {
				LOG.info("No Random Seed for Batch Generation specified. "
						+ "This parameter is unneeded for request time stamp generation.");
				randomBatchTimes = false;
				LOG.info("Using equi-distant non-random inter batch times.");
			}
			
			LOG.info("Load Generator Thread Count set to " + threadCount);
			LOG.info("URL connection timout set to " + urlTimeout + " ms");
			
			
			//Script Path
			String scriptPathRead = scriptPath.trim();
			LOG.info("Using Lua Script: " + scriptPathRead);

			if (file != null && outName != null && !outName.isEmpty()) {
				Director director = new Director(generatorIPs);
				director.process(file, outName, randomBatchTimes,
						threadCount, urlTimeout, scriptPathRead,
						warmupDurationS, warmupRate, warmupPauseS, randomizeUsers,
						powerCommunicators);
			}
			powerCommunicators.forEach(pc -> pc.stopCommunicator());
	}

	/**
	 * Inititializes a director with a load generator address.
	 * @param loadGenerators Addresses of the load generator. Seperated by ",".
	 */
	public Director(String[] loadGenerators) {
		communicators = new ArrayList<>(loadGenerators.length);
		for (String address : loadGenerators) {
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
	 * @param warmupDurationS The duration of a potential warmup period in seconds.
	 * 		Warmup is skipped if the duration is 0.
	 * @param warmupRate The load intensity of the warmup period.
	 * 		Warmup runs a constant load intensity and is skipped if the load is < 1.
	 * @param warmupPauseS The pause after warmup before starting measurement in seconds.
	 * @param randomizeUsers True if users should be randoized.
	 * 		False if they should be taken from a queue in order.
	 * @param powerCommunicators Communicators for communicating with power daemon (optional).
	 */
	public void process(File file, String outName, boolean randomBatchTimes,
			int threadCount, int timeout, String scriptPath,
			int warmupDurationS, double warmupRate, int warmupPauseS,
			boolean randomizeUsers,
			List<IPowerCommunicator> powerCommunicators) {

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
			 + "Failed Transactions,Dropped Transactions,Avg Response Time,Final Batch Dispatch Time");
			powerCommunicators.stream().forEachOrdered(pc -> writer.print(",Watts(" + pc.getCommunicatorName() + ")"));
			
			LOG.info("Starting Load Generation");

			//setup initial run Variables
			ExecutorService executor = null;
			SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy;HH:mm:ssSSS");
			if (powerCommunicators != null && !powerCommunicators.isEmpty()) {
				executor = Executors.newFixedThreadPool(powerCommunicators.size());
				for (IPowerCommunicator pc : powerCommunicators) {
					executor.execute(pc);
				}
			}
			communicators.parallelStream()
					.mapToLong(c -> c.startBenchmarking(randomBatchTimes, seed,
							warmupDurationS, warmupRate, warmupPauseS, randomizeUsers))
					.min().getAsLong();
			long timeZero = System.currentTimeMillis();
			String timeZeroString = sdf.format(new Date(timeZero));
			System.out.println("Beginning Run @" + timeZero + "(" + timeZeroString + ")");
			//print time zero if no warmup was set
			if (warmupRate < 1 || warmupDurationS <= 0) {
				writer.println("," + timeZeroString);
			}
			
			//get Data from LoadGenerator
			IntervalResult result;
			while (!(result = collectResultRound()).isMeasurementConcluded()) {
				//Check if a result for time 0 is sent. This result is only sent if warmup occured.
				if (result.getTargetTime() == 0.0) {
					timeZero = System.currentTimeMillis();
					String dateString = sdf.format(new Date(timeZero));
					//print time zero after conclusion of warmup
					System.out.println("Starting Measurement @" + timeZero + "(" + dateString + ")");
					writer.println("," + dateString);
				}
				logState(result, powerCommunicators, writer);
			}
			System.out.println("Workload finished.");
			writer.close();
			System.out.println("Log finished.");
			if (powerCommunicators != null && !powerCommunicators.isEmpty()) {
				powerCommunicators.forEach(pc -> pc.stopCommunicator());
				executor.shutdown();
			}


		} catch (IOException e) {
			LOG.severe("File not found: " + e.getMessage()  + "\n\t"
					+ "Did you specify the location of all files? "
					+ "Consult \"java -jar ... director --help\" for the necessary command line switches.");
		}
	}
	
	private static void initializePowerCommunicators(List<IPowerCommunicator> pcList,
			String pcClassName, String[] addresses) {
		for (String address : addresses) {
			if (!address.trim().isEmpty()) {
				String[] host = address.split(":");
				int port = 22444;
				if (host.length > 1) {
					port = Integer.parseInt(host[1].trim());
				}
				
				try {
					Class<? extends IPowerCommunicator> pcClass
						= Class.forName(pcClassName.trim()).asSubclass(IPowerCommunicator.class);
					IPowerCommunicator powerCommunicator = pcClass.newInstance();
					powerCommunicator.initializePowerCommunicator(host[0].trim(), port);
					LOG.info("Initializing Power Communicator for address: " + host[0].trim() + ":" + port);
					pcList.add(powerCommunicator);
				} catch (ClassNotFoundException e) {
					LOG.severe("PowerCommunicator class not found: " + pcClassName);
				} catch (InstantiationException e) {
					LOG.severe("PowerCommunicator class could not be instantiated: " + pcClassName);
					LOG.severe(e.getMessage());
				} catch (IllegalAccessException e) {
					LOG.severe("PowerCommunicator class could not be accessed: " + pcClassName);
					LOG.severe(e.getMessage());
				} catch (IOException e) {
					LOG.severe("IOException initializing power communicator: " + e.getMessage());
				}
			}
		}
		
		
	}
	
	/**
	 * Collects one iteration of the results, aggregates them and returns them.
	 * {@link IntervalResult#isMeasurementConcluded()} is false if more results are expected in the future.
	 * Such a container contains valid measurements results for the current interval.<br/>
	 * {@link IntervalResult#isMeasurementConcluded()} is true if the measurements have concluded and the
	 * "done" signal was received from all communicators. No valid results in this container.
	 * @return The interval's result.
	 */
	private IntervalResult collectResultRound() {
		int finishedCommunicators = 0;
		double targetTime = Double.NEGATIVE_INFINITY;
		int loadIntensity = 0;
		int successfulTransactions = 0;
		int failedTransactions = 0;
		int droppedTransactions = 0;
		ArrayList<Double> responseTimes = new ArrayList<Double>();
		ArrayList<Double> finalBatchTimes = new ArrayList<Double>();
		for (LoadGeneratorCommunicator communicator : communicators) {
			if (communicator.isFinished()) {
				finishedCommunicators++;
				if (finishedCommunicators == communicators.size()) {
					return IntervalResult.createIntervalResultWithMeasurementConcludedFlag();
				}
			} else {
				String receivedResults = communicator.getLatestResultMessageBlocking();
				if (receivedResults == null) {
					finishedCommunicators++;
					if (finishedCommunicators == communicators.size()) {
						return IntervalResult.createIntervalResultWithMeasurementConcludedFlag();
					}
				} else {
					String[] tokens = receivedResults.split(",");
					double receivedTargetTime = Double.parseDouble(tokens[0].trim());
					if (targetTime == Double.NEGATIVE_INFINITY) {
						targetTime = receivedTargetTime;
					} else {
						if (targetTime != receivedTargetTime) {
							LOG.severe("Time mismatch in load generator responses! Measurement invalid.");
						}
					}
					loadIntensity += Integer.parseInt(tokens[1].trim());
					successfulTransactions += Integer.parseInt(tokens[2].trim());
					responseTimes.add(Double.parseDouble(tokens[3].trim()));
					failedTransactions += Integer.parseInt(tokens[4].trim());
					droppedTransactions += Integer.parseInt(tokens[5].trim());
					finalBatchTimes.add(Double.parseDouble(tokens[6].trim()));
				}
			}
		}
		double avgResponseTime = responseTimes.stream().mapToDouble(d -> d.doubleValue()).average().getAsDouble();
		double finalBatchTime = finalBatchTimes.stream().mapToDouble(d -> d.doubleValue()).max().getAsDouble();
		return new IntervalResult(targetTime, loadIntensity, successfulTransactions, failedTransactions,
				droppedTransactions, avgResponseTime, finalBatchTime);
	}

	private void logState(IntervalResult result, List<IPowerCommunicator> powerCommunicators,
			PrintWriter writer) {
		//get Power
		List<Double> powers = null;
		if (powerCommunicators != null && !powerCommunicators.isEmpty()) {
			powers = new ArrayList<>(powerCommunicators.size());
			for (IPowerCommunicator pc : powerCommunicators) {
				powers.add(pc.getPowerMeasurement());
			}
		}
		System.out.println("Target Time = " + result.getTargetTime()
				+ "; Load Intensity = " + result.getLoadIntensity()
				+ "; #Success = " + result.getSuccessfulTransactions()
				+ "; #Failed = " + result.getFailedTransactions()
				+ "; #Dropped = " + result.getDroppedTransactions());
		//warmup has target times <= 0, ignore it
		if (result.getTargetTime() > 0) {
			writer.print(result.getTargetTime() + "," + result.getLoadIntensity() + ","
					+ result.getSuccessfulTransactions() + "," + result.getFailedTransactions() + ","
					+ result.getDroppedTransactions() + "," + result.getAvgResponseTime() + ","
					+ result.getFinalBatchTime());
			if (powers != null && !powers.isEmpty()) {
				powers.stream().forEachOrdered(p -> writer.print("," + p));
			}
			writer.println("");
		}
	}

}
