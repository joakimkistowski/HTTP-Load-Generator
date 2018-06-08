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
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import tools.descartes.dlim.httploadgenerator.generator.ArrivalRateTuple;

/**
 * Sends and receives data to/from a load generator.
 * @author Joakim von Kistowski
 *
 */
public class LoadGeneratorCommunicator {
	
	private static final Logger LOG = Logger.getLogger(LoadGeneratorCommunicator.class.getName());
	
	private Socket socket = null;
	private BufferedReader in = null;
	private PrintWriter out = null;

	private String ip;
	private int port;
	
	private boolean finished = false;
	
	private BlockingQueue<String> resultMessageQueue = new LinkedBlockingQueue<>();
	
	/**
	 * Create a new communicator.
	 * @param ip IP or host name of the load generator.
	 * @param port Port of the load generator.
	 */
	public LoadGeneratorCommunicator(String ip, int port) {
		this.ip = ip;
		this.port = port;
		try {
			socket = new Socket(ip, port);
			out = new PrintWriter(socket.getOutputStream(), true);
			in = new BufferedReader(new InputStreamReader(
					socket.getInputStream()));
		} catch  (IOException e) {
			LOG.severe("Could not connect to LoadGenerator.");
		}
	}
	
	/**
	 * Sends a lua script to the load generator.
	 * @param scriptPath The path of the script file on the director's file system.
	 */
	public void sendLUAScript(String scriptPath) {
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
			LOG.severe(e.getMessage());
		}
		waitForOK();
	}
	
	/**
	 * Sends a list of arrival rates to the load generator.
	 * @param rates List of arrival rate tuples.
	 * @param divisor Divisor to divide arrival rates by before sending.
	 * Use if you want the load generator to only execute a fraction of the arrival rates.
	 * Otherwise, set to 1.
	 */
	public void sendArrivalRates(List<ArrivalRateTuple> rates, int divisor) {
		//send load profile
		out.write(IRunnerConstants.ARRIVALRATE_SEND_KEY + "," + rates.size() + "\r\n");
		for (ArrivalRateTuple t : rates) {
			out.write("" + t.getTimeStamp() + "," + (t.getArrivalRate() / divisor));
			out.write("\r\n");
		}
		out.flush();
		waitForOK();
	}
	
	/**
	 * Send the thread count to the load generator.
	 * @param threadCount The number of thread the load generator is to use for generating.
	 */
	public void sendThreadCount(int threadCount) {
		out.println(IRunnerConstants.THREAD_NUM_KEY + threadCount);
		waitForOK();
	}
	
	/**
	 * Send the url connection timeout to the load generator.
	 * @param timeout The url connection timout.
	 */
	public void sendTimeout(int timeout) {
		out.println(IRunnerConstants.TIMEOUT_KEY + timeout);
		waitForOK();
	}

	/**
	 * Tells the load drivers to start benchmarking.
	 * Returns the start time as reported by the load driver and then returns.
	 * The benchmark itself runs asynchronously and results can
	 * be polled by using {@link #getLatestResultMessageBlocking()}.
	 * @param randomBatchTimes True if batch times are to be randomized.
	 * @param seed The random seed for random batch times.
	 * @param warmupDurationS
	 * 			  The duration of a potential warmup period in seconds.
	 * 			  Warmup is skipped if the duration is 0.
	 * @param warmupLoad
	 * 			  The load intensity of the warmup period.
	 * 			  Warmup runs a constant load intensity and is skipped if the load is < 1.
	 * @param warmupPauseS
	 * 			  The pause after warmup before starting measurement in seconds.
	 * @param randomizeUsers True if users should be randoized.
	 * 		False if they should be taken from a queue in order.
	 * @return The time of start.
	 */
	public long startBenchmarking(boolean randomBatchTimes, int seed, int warmupDurationS,
			double warmupLoad, int warmupPauseS, boolean randomizeUsers) {
		out.println(IRunnerConstants.START_KEY + "," + randomBatchTimes + "," + seed + ","
				+ warmupDurationS + "," + warmupLoad + "," + warmupPauseS + "," + randomizeUsers);
		long time = 0;
		try {
			time = Long.parseLong(in.readLine().trim());
		} catch (IOException e) {
			LOG.severe("Error receiving start response from load generator at: " + ip + ":" + port);
		}
		new LoadGeneratorCommunicatorThread().start();
		return time;
	}
	
	/**
	 * Gets the latest result message received by the communicator.
	 * Blocks and waits if no message was received.
	 * Returns {@link IRunnerConstants#DONE_KEY} if the load generator is finished.
	 * @return The result message or {@link IRunnerConstants#DONE_KEY}.
	 */
	public String getLatestResultMessageBlocking() {
		try {
			//5 second timeout means no load intensity with granularity > 5 seconds
			return resultMessageQueue.poll(5, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			LOG.severe("Interrupted taking from message queue.");
		}
		return null;
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
				LOG.severe("Read Failed for load driver at " + ip + ":" + port);
			}
		}
	}
	
	/**
	 * Returns true if this communcator has received the done signal.
	 * @return The finshed flag.
	 */
	public synchronized boolean isFinished() {
		return finished;
	}

	private synchronized void setFinished(boolean finished) {
		this.finished = finished;
	}

	/**
	 * Thread for the continuous result recording by the communicator.
	 * @author Joakim von Kistowski
	 *
	 */
	private class LoadGeneratorCommunicatorThread extends Thread {
		
		@Override
		public void run() {
			try {
				while (true) {
					String line = in.readLine();
					if (line.trim().equals(IRunnerConstants.DONE_KEY)) {
						setFinished(true);
						break;
					} else if (line != null && !line.isEmpty()) {
						resultMessageQueue.put(line.trim());
					}
				} 
			} catch (IOException | InterruptedException e) {
				LOG.severe("Error reading result response from load generator at: " + ip + ":" + port);
			} finally {
				try {
					in.close();
					out.close();
					socket.close();
				} catch (IOException e) {
					LOG.severe("Error closing network connection to load generator at: " + ip + ":" + port);
				}
				
			}
			
		}
	}
}
