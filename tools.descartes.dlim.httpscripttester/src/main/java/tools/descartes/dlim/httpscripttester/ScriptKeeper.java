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

import tools.descartes.httploadgenerator.http.HTTPInputGenerator;
import tools.descartes.httploadgenerator.http.HTTPTransaction;

/**
 * Keeper for the LUA script to use from the UI.
 * @author Joakim von Kistowski
 *
 */
public class ScriptKeeper {

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
		generator = new HTTPInputGenerator(new File(scriptPath), 5);
		transaction = new HTTPTransaction();
	}
	
	/**
	 * Run the script for the next HTTP call.
	 * @return The index of the call (input value to the LUA script).
	 */
	public int execute() {
		transaction.process(generator);
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
