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
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

import tools.descartes.dlim.httploadgenerator.runner.IRunnerConstants;

/**
 * The class AbstractLoadGenerator is a abstract class for various load
 * generator classes. It is executed on the load generator machine and
 * establishes the connection to the director on the controller system. The
 * class triggers the process method of the selected load generator class when
 * the starting message of the director is received. The main task of this class
 * is the communication with the director on the controller system.
 * 
 * @author Joakim von Kistowski, Maximilian Deffner
 *
 */
public abstract class AbstractLoadGenerator extends Thread {

	/**
	 * We store the received script to the temp dir,
	 * as the LUA engine supports reading it from storage way better than reading from memory.
	 */
	private static final String TMP_SCRIPT_PATH = System.getProperty("java.io.tmpdir") + "/http_calls.lua";
	
	/** The constant Log4j2 logging instance. */
	private static final Logger LOG = Logger.getLogger(AbstractLoadGenerator.class.getName());

	/** Socket for connection the the director on the controller machine. */
	private Socket director;

	/**
	 * Buffered reader for communication with the director on the controller
	 * machine.
	 */
	private BufferedReader in;

	/**
	 * Print writer for communication with the director on the controller
	 * machine.
	 */
	private PrintWriter out;

	private int timeout = -1;
	
	/**
	 * Constant command String to indicate that a load profile is being sent via
	 * network. E.g. "dlim" for arrival rate tuples and "timestaps" for request
	 * time stamps.
	 * 
	 * @return The constant command.
	 */
	protected abstract String loadProfileCommand();

	/**
	 * Starting point for executing the load generator mode.
	 */
	public static void executeLoadGenerator() {

		ServerSocket server = null;

		try {
			server = new ServerSocket(IRunnerConstants.LOAD_GEN_PORT);
		} catch (IOException e) {
			LOG.log(Level.SEVERE, "Couldn't create ServerSocket or accept director.");
			e.printStackTrace();
		}

		while (true) {
			NetStreamContainer streams = AbstractLoadGenerator.acceptDirector(server);
			AbstractLoadGenerator generator = new ArrivalRateTupleLoadGenerator(streams.director, streams.in,
					streams.out);
			generator.waitAndListen();
		}
	}

	/**
	 * Creating new instance of the abstract load generator.
	 * 
	 * @param director Socket for communicating with the director.
	 * @param in Input reader for reading inputs from the director.
	 * @param out Writer for writing back to the director.
	 */
	public AbstractLoadGenerator(Socket director, BufferedReader in, PrintWriter out) {
		// this.server = server;
		this.director = director;
		this.in = in;
		this.out = out;
	}

	/**
	 * Accepting new client for connection to the socket.
	 * 
	 * @param server
	 *            Server socket.
	 * @return A container with the network streams.
	 */
	private static NetStreamContainer acceptDirector(ServerSocket server) {
		NetStreamContainer streams = new NetStreamContainer();
		try {
			LOG.log(Level.INFO, "Waiting for director.");
			streams.director = server.accept();
			LOG.log(Level.INFO, "Director connected.");
			streams.in = new BufferedReader(new InputStreamReader(streams.director.getInputStream()));
			streams.out = new PrintWriter(streams.director.getOutputStream(), true);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return streams;
	}

	/**
	 * Waiting and listening for new instructions of the director on the
	 * controller machine.
	 */
	private void waitAndListen() {
		String line;
		while (true) {
			try {
				line = in.readLine();
				if (line.startsWith(loadProfileCommand())) {
					readLoadProfile(in, line);
					ok();
				} else if (line.equals(IRunnerConstants.RESULTS_KEY)) {
					LOG.log(Level.INFO, "Incoming Result Request.");
					sendResults();
					ok();
				} else if (line.startsWith(IRunnerConstants.START_KEY)) {
					startBenchmark(line.trim().split(","));
					disconnectFromDirector();
					break;
				} else if (line.startsWith(IRunnerConstants.THREAD_NUM_KEY)) {
					if (this instanceof ArrivalRateTupleLoadGenerator) {
						try {
							int threads = Integer.parseInt(line.split(":")[1].trim());
							((ArrivalRateTupleLoadGenerator) this).setNumberOfThreads(threads);
						} catch (IndexOutOfBoundsException | NumberFormatException e) {
							LOG.log(Level.WARNING, "Invalid thread count.");
						}
						ok();
					}
				} else if (line.startsWith(IRunnerConstants.TIMEOUT_KEY)) {
					try {
						int timeout = Integer.parseInt(line.split(":")[1].trim());
						this.timeout = timeout;
					} catch (IndexOutOfBoundsException | NumberFormatException e) {
						LOG.log(Level.WARNING, "Invalid timeout.");
					}
					ok();
				} else if (line.startsWith(IRunnerConstants.SCRIPT_SEND_KEY)) {
					receiveScript(in);
					LOG.info("Received LUA script.");
					ok();
				} else {
					LOG.log(Level.SEVERE, "Unknown Command: " + line);
				}
			} catch (IOException e) {
				LOG.log(Level.SEVERE, "Read Failed");
			}
		}
	}

	/**
	 * Receiving the load profile transferred by the director.
	 * 
	 * @param in
	 *            Input reader.
	 * @param header
	 *            Load profile header.
	 */
	protected abstract void readLoadProfile(BufferedReader in, String header);

	/**
	 * Sending results back to the director at the end of every interval.
	 */
	private void sendResults() {
		out.flush();
	}

	/**
	 * Disconnect from the director at the end of every benchmark.
	 */
	private void disconnectFromDirector() {
		try {
			director.close();
		} catch (IOException e) {
			LOG.log(Level.SEVERE, "Could not close connection. Error: " + e.getMessage());
		}
	}

	/**
	 * Start execution of the benchmark with the transaction name, seed and
	 * random batch time transferred by the director.
	 * 
	 * @param params
	 *            String array of the parameters received via network.
	 *            Expected message:
	 *            "start,[randomseed(int)],[randombatchtimes(boolean)],[warmup-duration-s(int)],[warmup-load(double)]"
	 */
	private void startBenchmark(String[] params) {
		LOG.log(Level.INFO, "Receiving Benchmark Parameters.");
		// Read Params
		boolean randomBatchTimes = Boolean.parseBoolean(params[1].trim());
		int seed = Integer.parseInt(params[2].trim());
		int warmupDurationS = Integer.parseInt(params[3]);
		double warmupLoad = Double.parseDouble(params[4]);
		int warmupPauseS = Integer.parseInt(params[5]);
		boolean randomizeUsers = Boolean.parseBoolean(params[6].trim());
		ResultTracker.TRACKER.reset();
		out.println(System.currentTimeMillis());

		LOG.log(Level.INFO, "Starting run with randomBatchTimes=" + randomBatchTimes + ", seed=" + seed + "\n"
				+ "warmupDuration=" + warmupDurationS + " s, warmupLoadIntensity=" + warmupLoad
				+ ", warmupPause=" + warmupPauseS + " s, randomizeUsers=" + randomizeUsers);
		File script = new File(TMP_SCRIPT_PATH);
		if (!script.exists()) {
			error("Temporary load generator side script not found at " + TMP_SCRIPT_PATH);
		}
		process(randomBatchTimes, seed, warmupDurationS, warmupLoad, warmupPauseS, randomizeUsers);
		out.println(IRunnerConstants.DONE_KEY);
	}
	
	/**
	 * Sending error message to the director.
	 * 
	 * @param message
	 *            Error message.
	 */
	private void error(String message) {
		out.println("Error: " + message);
	}

	/**
	 * Sending confirmation to the director.
	 */
	private void ok() {
		out.println(IRunnerConstants.OK_KEY);
	}

	/**
	 * Places and executes the work.
	 *
	 * @param randomBatchTimes
	 *            True, if wait times should be randomized a bit.
	 * @param seed
	 *            The random number generator seed.
	 * @param warmupDurationS
	 * 			  The duration of a potential warmup period in seconds.
	 * 			  Warmup is skipped if the duration is 0.
	 * @param warmupLoadIntensity
	 * 			  The load intensity of the warmup period.
	 * 			  Warmup runs a constant load intensity and is skipped if the load is < 1.
	 * @param warmupPauseS
	 * 			  The pause after warmup before starting measurement in seconds.
	 * @param randomizeUsers True if users should be randoized.
	 * 			  False if they should be taken from a queue in order.
	 */
	protected abstract void process(boolean randomBatchTimes, int seed,
			int warmupDurationS, double warmupLoadIntensity, int warmupPauseS, boolean randomizeUsers);

	/**
	 * Sending results to the director after every interval.
	 * 
	 * @param targettime
	 *            time stamp of the arrival rate tuples
	 * @param loadintensity
	 *            preset load throughput
	 * @param throughput
	 *            actual achieved load throughput
	 * @param avgResponseTime
	 * 			  average response time
	 * @param invalidTransactionCount
	 * 			  Count of invalid transactions for the measurement interval.
	 * * @param droppedTransactionCount
	 * 			  Count of dropped transactions for the measurement interval.
	 * @param actualtime
	 *            actual time
	 */
	protected void sendToDirector(double targettime, int loadintensity, long throughput,
				double avgResponseTime, long invalidTransactionCount, long droppedTransactionCount, double actualtime) {
		out.println("" + targettime + "," + loadintensity + "," + throughput
				+ "," + avgResponseTime + "," + invalidTransactionCount + ","
				+ droppedTransactionCount + "," + actualtime);
	}

	/**
	 * Container for network streams.
	 * 
	 * @author Joakim von Kistowski
	 *
	 */
	private static class NetStreamContainer {
		private Socket director;
		private BufferedReader in;
		private PrintWriter out;
	}
	
	//Receives the script and writes it to the temp dir.
	private void receiveScript(BufferedReader br) throws IOException {
		try (PrintWriter tmpScriptFileWriter = new PrintWriter(TMP_SCRIPT_PATH)) {
			String line;
			while ((line = br.readLine()) != null) { 
				if (line.equals(IRunnerConstants.SCRIPT_TERM_KEY)) {
					break;
				} else {
					tmpScriptFileWriter.println(line);
				}
			}
		}
		
	}
	
	/**
	 * The path of the script file for the load generator.
	 * (i.e. the tmp path were the network received script has been stored.
	 * @return The script path.
	 */
	protected String getScriptPath() {
		return TMP_SCRIPT_PATH;
	}
	
	/**
	 * Get the http url connection read timout.
	 * @return The timout.
	 */
	public int getTimeout() {
		return timeout;
	}
}
