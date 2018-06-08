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
package tools.descartes.dlim.httpscripttester;

import java.io.File;
import java.util.logging.Logger;

import tools.descartes.dlim.httploadgenerator.http.HTTPInputGenerator;
import tools.descartes.dlim.httploadgenerator.http.HTTPTransaction;
import tools.descartes.dlim.httploadgenerator.transaction.TransactionDroppedException;
import tools.descartes.dlim.httploadgenerator.transaction.TransactionInvalidException;

/**
 * Keeper for the LUA script to use from the UI.
 * @author Joakim von Kistowski
 *
 */
public class ScriptKeeper {

	private static final Logger LOG = Logger.getLogger(ScriptKeeper.class.getName());
	
	private String call = "";
	private int callNum = 1;
	
	private HTTPTransaction transaction;
	private HTTPInputGenerator generator;
	
	/**
	 * Create a new script keeper.
	 * @param scriptPath The path of the LUA script.
	 */
	public ScriptKeeper(String scriptPath) {
		//we use 5 as the random seed for testing
		generator = new HTTPInputGenerator(0, new File(scriptPath), 5, -1);
		transaction = new HTTPTransaction();
	}
	
	/**
	 * Run the script for the next HTTP call.
	 * @return The index of the call (input value to the LUA script).
	 */
	public int execute() {
		try {
			transaction.process(generator);
		} catch (TransactionDroppedException e) {
			LOG.severe("Transaction Dropped, queuing time exceeded timeout of " + generator.getTimeout() + " ms.");
		} catch (TransactionInvalidException e) {
			LOG.severe("Transaction Invalid, response time exceeded timout of " + generator.getTimeout() + " ms.");
		}
		call = generator.getLastCall();
		callNum = generator.getCurrentCallNum() - 1;
		return callNum;
	}
	
	/**
	 * Get the last call URL.
	 * @return The last call URL.
	 */
	public String getCall() {
		return call;
	}
	
	/**
	 * Get the index of the last call.
	 * @return The index of the last call.
	 */
	public int getLastCallNum() {
		return callNum;
	}
	
	/**
	 * Get the last HTTP response.
	 * @return The last HTTP response.
	 */
	public String getLastHTTPResponse() {
		return generator.getCurrentHTML();
	}

}
