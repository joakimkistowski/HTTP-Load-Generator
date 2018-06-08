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
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.logging.Logger;

/**
 * Pool of input stateful generators to be assigned to the load generation transactions.
 * @author Joakim von Kistowski
 *
 */
public final class HTTPInputGeneratorPool {

	private static final Logger LOG = Logger.getLogger(HTTPInputGeneratorPool.class.getName());
	
	private static HTTPInputGeneratorPool pool = null;
	
	private Random random;
	private PoolMode mode;
	private BlockingQueue<HTTPInputGenerator> queue;
	private ConcurrentHashMap<Integer,HTTPInputGenerator> map;
	private Semaphore mapAccessControlSemaphore;
	
	private HTTPInputGeneratorPool(PoolMode mode, String luaScriptPath, int threadCount, int timeout, int randomSeed) {
		this.mode = mode;
		queue = new LinkedBlockingQueue<>();
		map = new ConcurrentHashMap<>();
		mapAccessControlSemaphore = new Semaphore(threadCount, true);
		if (randomSeed > 0) {
			random = new Random(randomSeed);
		} else {
			random = new Random(5);
		}
		File script = new File(luaScriptPath);
		if (!script.exists()) {
			LOG.severe("Lua script does not exist at: " + luaScriptPath);
		}
		 // We place as many input generators as threads in the pool.
		for (int i = 0; i < threadCount; i++) {
			addInputGenerator(new HTTPInputGenerator(i, script, i, timeout));
		}
		if (mode.equals(PoolMode.QUEUE)) {
			LOG.info("Created pool of " + queue.size() + " users (LUA contexts, HTTP input generators).");
		} else {
			LOG.info("Created pool of " + map.size() + " users (LUA contexts, HTTP input generators).");
		}
	}
	
	private void addInputGenerator(HTTPInputGenerator generator) {
		if (mode.equals(PoolMode.QUEUE)) {
			try {
				queue.put(generator);
			} catch (InterruptedException e) {
				LOG.severe("Interrupted initializing Queue.");
			}
		} else {
			map.put(generator.getId(), generator);
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
	public static void initializePool(PoolMode mode, String luaScriptPath, int threadCount, int timeout, int randomSeed) {
		pool = new HTTPInputGeneratorPool(mode, luaScriptPath, threadCount, timeout, randomSeed);
	}
	
	/**
	 * Places an HTTPInputGenerator back into the pool.
	 * @param generator The generator to place in the pool.
	 */
	public void releaseBackToPool(HTTPInputGenerator generator) {
		if (mode.equals(PoolMode.QUEUE)) {
			try {
				queue.put(generator);
			} catch (InterruptedException e) {
				LOG.severe("Interrupted placing generator in pool.");
			}
		} else {
			map.put(generator.getId(), generator);
			mapAccessControlSemaphore.release();
		}
		
	}
	
	/**
	 * Retrieves an HTTPInputGenerator from the pool. Don't forget to but it back after use.
	 * @return The generator to use.
	 */
	public HTTPInputGenerator takeFromPool() {
		HTTPInputGenerator generator = null;
		if (mode.equals(PoolMode.QUEUE)) {
			try {
				generator = queue.take();
			} catch (InterruptedException e) {
				LOG.severe("Interrupted retreiving generator from pool.");
			}
		} else {
			try {
				mapAccessControlSemaphore.acquire();
				generator = takeRandomFromMapWithAccess();
			} catch (InterruptedException e) {
				LOG.severe("Interrupted acquiring access for retreiving generator from pool.");
			}
			
		}
		return generator;
	}
	
	private synchronized HTTPInputGenerator takeRandomFromMapWithAccess() {
		if (map.size() == 0) {
			LOG.severe("No HTTPInputGenerator available. It should have been available as access was granted.");
			return null;
		}
		int index = random.nextInt(map.size());
		int i = 0;
		Entry<Integer, HTTPInputGenerator> entry = null;
		for (Entry<Integer, HTTPInputGenerator> e : map.entrySet()) {
			if (i == index) {
				entry = e;
				break;
			}
			i++;
		}
		if (entry != null && entry.getKey() != null) {
			map.remove(entry.getKey());
			return entry.getValue();
		}
		LOG.severe("No HTTPInputGenerator available. Entry in pool was null but access was granted.");
		return null;
	}
	
	public static enum PoolMode {
		QUEUE, RANDOM
	}
	
}
