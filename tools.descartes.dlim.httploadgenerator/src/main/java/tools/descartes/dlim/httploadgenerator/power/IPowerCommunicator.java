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

import java.io.IOException;

/**
 * Interface for Communicators that communicate with network power measurement daemons.
 * @author Joakim von Kistowski
 *
 */
public interface IPowerCommunicator extends Runnable {

	/**
	 * Initializes the power communicator.
	 * Usually called directly after constructor.
	 * @param hostname The hostname/IP.
	 * @param port The port.
	 * @throws IOException Exception if things go wrong.
	 */
	public void initializePowerCommunicator(String hostname, int port) throws IOException;
	
	/**
	 * Takes a power measurement. Usually resets the internal
	 * state of the communicator by clearing any data that has been
	 * collected so far.
	 * @return The avg power consumption since the last call of this method in Watts.
	 * 			Returns negative values on error.
	 */
	public double getPowerMeasurement();
	
	/**
	 * Stops the communicator from collecting data and causes it to shut down
	 * connections to the daemon it is communicating with.
	 */
	public void stopCommunicator();
	
	/**
	 * Returns a unique name for a communicator instance.
	 * E.g., the address of the power analyzer it is connecting to.
	 * @return The communicator name.
	 */
	default public String getCommunicatorName() {
		return this.getClass().getSimpleName();
	}
	
}
