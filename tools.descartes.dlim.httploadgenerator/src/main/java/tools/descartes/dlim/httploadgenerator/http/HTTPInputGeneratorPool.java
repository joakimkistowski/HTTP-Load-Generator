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
package tools.descartes.dlim.httploadgenerator.http;

import java.io.File;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

/**
 * Pool of input stateful generators to be assigned to the load geration transactions.
 * @author Joakim von Kistowski
 *
 */
public final class HTTPInputGeneratorPool {

	private static final Logger LOG = Logger.getLogger(HTTPInputGeneratorPool.class.getName());
	
	private static HTTPInputGeneratorPool pool = null;
	
	private BlockingQueue<HTTPInputGenerator> queue;
	
	private HTTPInputGeneratorPool(String luaScriptPath, int threadCount, int timeout) {
		queue = new LinkedBlockingQueue<>();
		File script = new File(luaScriptPath);
		if (!script.exists()) {
			LOG.severe("Lua script does not exist at: " + luaScriptPath);
		}
		 // We place as many input generators as threads in the pool.
		for (int i = 0; i < threadCount; i++) {
			try {
				queue.put(new HTTPInputGenerator(script, i, timeout));
			} catch (InterruptedException e) {
				LOG.severe("Interrupted initializing Queue.");
			}
		}
	}
	
	/**
	 * Get the pool. Must have been initialized.
	 * @return The pool singleton. Null if uninitialized.
	 */
	public static HTTPInputGeneratorPool getPool() {
		if (pool == null) {
			LOG.severe("HTTP input generator pool was called,"
					+ " but has not been initialized with an existing Lua script.");
		}
		return pool;
	}
	
	/**
	 * Initializes the pool (deleting an old one if it exists).
	 * @param luaScriptPath The path of the Lua script.
	 * @param threadCount The number of threads that will be used to access the pool.
	 * @param timeout The http url connection timeout.
	 */
	public static void initializePool(String luaScriptPath, int threadCount, int timeout) {
		pool = new HTTPInputGeneratorPool(luaScriptPath, threadCount, timeout);
	}
	
	/**
	 * Places an HTTPInputGenerator back into the pool.
	 * @param generator The generator to place in the pool.
	 */
	public void releaseBackToPool(HTTPInputGenerator generator) {
		try {
			queue.put(generator);
		} catch (InterruptedException e) {
			LOG.severe("Interrupted placing generator in pool.");
		}
	}
	
	/**
	 * Retrieves an HTTPInputGenerator from the pool. Don't forget to but it back after use.
	 * @return The generator to use.
	 */
	public HTTPInputGenerator takeFromPool() {
		HTTPInputGenerator generator = null;
		try {
			generator = queue.take();
		} catch (InterruptedException e) {
			LOG.severe("Interrupted retreiving generator from pool.");
		}
		return generator;
	}
	
	public static enum PoolMode {
		QUEUE, RANDOM
	}
	
}
