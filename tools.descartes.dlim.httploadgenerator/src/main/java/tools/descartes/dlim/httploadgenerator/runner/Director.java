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
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.text.SimpleDateFormat;
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
	
	private Socket socket = null;
	private BufferedReader in = null;
	private PrintWriter out = null;

	private static int seed = 5;

	/**
	 * Execute the director with the given parameters.
	 * Parameters may be null. Director asks the user for null parameters if they are required.
	 * @param profilePath The path of the LIMBO-generated load profile.
	 * @param outName The name of the output log file.
	 * @param powerAddress The address of the power daemon (optional).
	 * @param generator The address of the load generator.
	 * @param randomSeed The random seed for exponentially distributed request arrivals.
	 * @param threadCount The number of threads that generate load.
	 * @param scriptPath The path of the script file that generates the specific requests.
	 * @param powerCommunicatorClassName Fully qualified class name of the power communicator class.
	 */
	public static void executeDirector(String profilePath, String outName, String powerAddress, String generator,
			String randomSeed, String threadCount, String scriptPath, String powerCommunicatorClassName) {
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
					LOG.severe(e.getLocalizedMessage());
				} catch (IllegalAccessException e) {
					LOG.severe("PowerCommunicator class could not be accessed: " + powerCommunicatorClassName);
					LOG.severe(e.getLocalizedMessage());
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
						threadNum, scriptPathRead, powerCommunicator);
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
	 * @param loadGenerator Address of the load generator.
	 */
	public Director(String loadGenerator) {
		try {
			socket = new Socket(loadGenerator, 24226);
			out = new PrintWriter(socket.getOutputStream(), true);
			in = new BufferedReader(new InputStreamReader(
					socket.getInputStream()));
		} catch  (IOException e) {
			System.out.println("Could not connect to LoadGenerator.");
		}
	}

	/**
	 * Actually run the director. Sends the messages to the load generator and collects data.
	 * @param file The arrival rate file.
	 * @param outName The name of the output log.
	 * @param scanner The scanner for reading user start signal from console.
	 * @param randomBatchTimes True if batches are scheduled using a randomized distribution.
	 * @param threadCount The number of threads that generate load.
	 * @param scriptPath The path of the script file that generates the specific requests.
	 * @param powerCommunicator Communicator to communicate with power daemon (optional).
	 */
	public void process(File file, String outName, Scanner scanner, boolean randomBatchTimes,
			int threadCount, String scriptPath, IPowerCommunicator powerCommunicator) {

		try {
			List<ArrivalRateTuple> arrRates = Main.readFileToList(file, 0);
			LOG.info("Read " + arrRates.size() + " Arrival Rate Tuples");
			sendArrivalRates(arrRates);
			LOG.info("Arrival Rates sent to Load Generator.");

			sendThreadCount(threadCount);
			LOG.info("Thread Count sent to Load Generator: " + threadCount);

			sendLUAScript(scriptPath);
			LOG.info("Contents of script sent to Load Generator: " + scriptPath);
			
			String parentPath = file.getParent();
			if (parentPath == null || parentPath.isEmpty()) {
				parentPath = ".";
			}
			PrintWriter writer = new PrintWriter(parentPath + "/" + outName);
			writer.print("Target Time,Load Intensity,Throughput,Final Batch Dispatch Time,Watts");

			System.out.print("Press Enter to begin Execution");
			outName = scanner.nextLine();

			//setup initial run Variables
			ExecutorService executor = null;
			SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy;HH:mm:ssSSS");
			if (powerCommunicator != null) {
				executor = Executors.newSingleThreadExecutor();
				executor.execute(powerCommunicator);
			}
			long timeZero = startBenchmarking(randomBatchTimes, seed);
			String dateString = sdf.format(new Date(timeZero));
			System.out.println("Beginning Run @" + timeZero + "(" + dateString + ")");
			writer.println("," + dateString);

			//get Data from LoadGenerator
			while (true) {
				String line = in.readLine();
				if (line.trim().equals("done")) {
					break;
				} else if (line != null && !line.isEmpty()) {
					logState(line.trim(), powerCommunicator, writer);
				}
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

	private void logState(String generatorLine, IPowerCommunicator powerCommunicator, PrintWriter writer) {
		//get Power
		double power = 0;
		if (powerCommunicator != null) {
			power = powerCommunicator.getPowerMeasurement();
		}
		writer.println(generatorLine.trim() + "," + power);
	}

	private void sendArrivalRates(List<ArrivalRateTuple> rates) {
		//send type, for initialization of load generator
		out.println("type:" + IRunnerConstants.ARRIVALRATE_SEND_KEY);
		//send load profile
		out.write(IRunnerConstants.ARRIVALRATE_SEND_KEY + "," + rates.size() + "\r\n");
		for (ArrivalRateTuple t : rates) {
			out.write("" + t.getTimeStamp() + "," + t.getArrivalRate());
			out.write("\r\n");
		}
		out.flush();
		waitForOK();
	}

	private void sendLUAScript(String scriptPath) {
		out.println(IRunnerConstants.SCRIPT_SEND_KEY);
		try (BufferedReader br = new BufferedReader(new FileReader(scriptPath))) {
			String line;
			while ((line = br.readLine()) != null) {
				out.write(line + "\n");
			}
			out.write(IRunnerConstants.SCRIPT_TERM_KEY + "\n");
			out.flush();
		} catch (FileNotFoundException e) {
			LOG.severe("Script file not found at: " + scriptPath);
		} catch (IOException e) {
			LOG.severe("IOException parsing script file at: " + scriptPath);
			LOG.severe(e.getLocalizedMessage());
		}
		waitForOK();
	}
	
	private void sendThreadCount(int threadCount) {
		out.println(IRunnerConstants.THREAD_NUM_KEY + threadCount);
		waitForOK();
	}
	
	private void waitForOK() {
		waitForMessage("ok");
	}

	private void waitForMessage(String message) {
		String line;
		while (true) {
			try {
				line = in.readLine();
				if (line.trim().equals(message)) {
					System.out.println("Load Generator sent: " + message);
					break;
				}
			} catch (IOException e) {
				System.out.println("Read Failed");
			}
		}
	}

	private long startBenchmarking(boolean randomBatchTimes, int seed) throws IOException {
		out.println("start," + randomBatchTimes + "," + seed);
		long time = Long.parseLong(in.readLine().trim());
		return time;
	}

}
