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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

/**
 * Communicator for communicating with Yokogawa power meters.
 * The corresponding binary is missing due to licensing issues.
 * Consider this code as an example of how to implement a power communicator.
 * @author Joakim von Kistowski
 */
public class HIOKICommunicator extends Thread implements IPowerCommunicator {
	
	private static final Logger LOG = Logger.getLogger(HIOKICommunicator.class.getName());

	private static final int HIOKI_DEFAULT_PORT = 3300;
	private static final long QUERY_INTERVAL_MS = 250;
	
	private Socket powerSocket = null;
	private BufferedReader in = null;
	private BufferedWriter out = null;

	private BlockingQueue<Double> results = new LinkedBlockingQueue<>();
	private boolean stop = false;
	
	private String name = "";
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void initializePowerCommunicator(String hostname, int port) throws IOException {
		Socket socket = null;

		if (port < 0) {
			port = HIOKI_DEFAULT_PORT;
			name = hostname;
		} else {
			name = hostname + ":" + port;
		}
		
		try {
			socket = new Socket(hostname, port);
		} catch (IOException x) {
			LOG.severe("Exception connecting to HIOKI power analyzer: " + x.getMessage());
		}

		if (socket == null) {
			LOG.severe("Could not connect to power analyzer.");
		} else {
			this.powerSocket = socket;

			try {
				InputStream sockIn = powerSocket.getInputStream();
				this.in = new BufferedReader(new InputStreamReader(sockIn, "ASCII"));
				OutputStream sockOut = powerSocket.getOutputStream();
				this.out = new BufferedWriter(new OutputStreamWriter(sockOut, "ASCII"));

				LOG.info("Connected to HIOKI Power Analyzer.");
			} catch (IOException x) {
				LOG.severe("Exception initializing communication with HIOKI Power Analyzer: " + x.getMessage());
			}
		}
		
	}

	@Override
	public void run() {
		while (!isStop()) {
			double power = getPowerFromDevice();
			try {
				results.put(power);
			} catch (InterruptedException e) {
				LOG.severe("Interrupted storing power measurement result: " + e.getMessage());
			}
			try {
				Thread.sleep(QUERY_INTERVAL_MS);
			} catch (InterruptedException e) {
				LOG.severe("Interrupted waiting: " + e.getMessage());
			}
		}
		try {
			out.close();
			in.close();
			powerSocket.close();
		} catch (IOException e) {
			LOG.severe("Error closing connection with power meter: " + e.getMessage());
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public double getPowerMeasurement() {
		int count = 0;
		double power = 0;
		Double measurement;
		while ((measurement = results.poll()) != null) {
			power += measurement;
			count++;
		}
		if (count > 0) {
			return power / count;
		}
		return 0;
	}

	private double getPowerFromDevice() {
		try {
			out.write(":MEAS:POW?\n");
			out.flush();
			String response = in.readLine();
			String powerReport = response.split(";")[2].trim();
			if (powerReport.contains(" ")) {
				powerReport = powerReport.split(" ")[1].trim();
			}
			return Double.parseDouble(powerReport);
		} catch (IOException e) {
			LOG.severe("Error querying power meter: " + e.getMessage());
		} catch (NumberFormatException e2) {
			LOG.severe("Invalid response from power meter: " + e2.getMessage());
		}
		return 0;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void stopCommunicator() {
		setStop(true);
	}

	private synchronized boolean isStop() {
		return stop;
	}

	private synchronized void setStop(boolean stop) {
		this.stop = stop;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getCommunicatorName() {
		return name;
	}
}
