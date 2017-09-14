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
package tools.descartes.dlim.httploadgenerator.power;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.logging.Logger;

/**
 * Communicator for communicating with Yokogawa power meters.
 * The corresponding binary is missing due to licensing issues.
 * Consider this code as an example of how to implement a power communicator.
 * @author Joakim von Kistowski
 */
public class TMCTLDCommunicator extends Thread implements IPowerCommunicator {
	
	private static final Logger LOG = Logger.getLogger(TMCTLDCommunicator.class.getName());
	
	private static final long QUERY_INTERVAL = 100;

	private Socket tmctlSocket = null;
	private BufferedReader in = null;
	private BufferedWriter out = null;

	private volatile boolean stop = false;

	//never write directly, ALWAYS use setPower
	private volatile int measurements = 0;
	private volatile double powerSum = 0;


	/**
	 * Returns the average power measurements that have been measured by the Communicator.
	 * Optionally adds a concrete new measurement to be averaged.
	 * Examples:
	 * - To get the current avg measurement and reset the communicator: getAndSetPowerCalc(0,0,false)
	 * - To get the current avg measurent and not reset the communicator: getAndSetPowerCalc(0,0,true);
	 * - To get the current avg measurement, reset the communicator and store a new measurement for later:
	 * 		getAndSetPowerCalc(1,newWatts,false);
	 * - To get the current avg measurement and add a new concrete measurement to be included in the average:
	 * 		getAndSetPowerCalc(1,newWatts,true);
	 * @param measurements The amounts of measruements to be added to the current avg. Usually either 0 or 1.
	 * @param power The measured power of the sample(s) to be added.
	 * @param incr True if the currently stored value is to be added to (more samples for averaging),
	 * 		false if the stored value is to be reset.
	 * @return Average of all stored power consumption samples.
	 */
	private synchronized double getAndSetPowerCalc(int measurements, double power, boolean incr) {
		double avgPower = powerSum / this.measurements;
		if (incr) {
			this.measurements += measurements;
			powerSum += power;
		} else {
			this.measurements = measurements;
			powerSum = power;
		}
		return avgPower;
	}

	@Override
	public void run() {
		long nextQueryTime = System.currentTimeMillis();
		while (!stop) {
			nextQueryTime += QUERY_INTERVAL;
			String wattString = this.queryDevice("measure");
			try {
				wattString = wattString.split(",")[2].trim();
				double watts = Double.parseDouble(wattString);
				getAndSetPowerCalc(1, watts, true);

			} catch (ArrayIndexOutOfBoundsException e) {
				LOG.info("Sampled too fast? Output: " + wattString);
			}

			long queryReceivedTime = System.currentTimeMillis();
			long sleepTime = nextQueryTime - queryReceivedTime;
			if (sleepTime > 0) {
				try {
					sleep(sleepTime);
				} catch (InterruptedException e) {
					LOG.severe("Couldn't sleep.");
					e.printStackTrace();
				}
			}

		}
		LOG.info("Disconnecting from tmctld.");
		disconnect();
	}

	/**
	 * Query the power meter with a command.
	 * @param command The command to send to the power meter.
	 * @return The return message.
	 */
	public synchronized String queryDevice(String command) {
		if (out != null) {
			try {
				out.write(command);
				out.write("\n\0");
				out.flush();
				return in.readLine();
			} catch (IOException x) {
				return "";
			}
		}
		return "";
	}


	/**
	 * Disconnect from PTDaemon.
	 * @ if communication with PTDaemon failed
	 */
	private void disconnect() {
		if (tmctlSocket != null) {
			try {
				tmctlSocket.close();
			} catch (IOException x) {
				LOG.severe("Exception closing connection to TMCTLD");
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void initializePowerCommunicator(String hostname, int port) throws IOException {
		Socket socket = null;

		try {
			socket = new Socket(hostname, port);
		} catch (IOException x) {
			LOG.severe("Exception connecting to TMCTLD: " + x.getMessage());
		}

		if (socket == null) {
			LOG.severe("Could not connect to tmctl.");
		} else {
			this.tmctlSocket = socket;

			try {
				InputStream sockIn = tmctlSocket.getInputStream();
				this.in = new BufferedReader(new InputStreamReader(sockIn, "ASCII"));
				OutputStream sockOut = tmctlSocket.getOutputStream();
				this.out = new BufferedWriter(new OutputStreamWriter(sockOut, "ASCII"));

				LOG.info("Connected to power daemon.");
			} catch (IOException x) {
				LOG.severe("Exception connecting to tmctl.");
			}
		}
		
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public double getPowerMeasurement() {
		return getAndSetPowerCalc(0, 0, false);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void stopCommunicator() {
		stop = true;
	}
}
