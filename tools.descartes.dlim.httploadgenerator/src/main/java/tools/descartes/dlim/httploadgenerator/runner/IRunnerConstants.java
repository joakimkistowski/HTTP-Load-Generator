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


/**
 * Interface IRunnerConstants provides the constants for the director and the load generator.
 * 
 * @author Joakim von Kistowski, Maximilian Deffner
 *
 */
public interface IRunnerConstants {

	/** Default values */
	/**
	 * Default port for communication between director and load generator.
	 */
	public static final int DEFAULT_PORT = 24226;
	/**
	 * Default arrival rate file path.
	 */
	public static final String DEFAULT_ARRIVAL_RATE_PATH = "arrivalrates.csv";
	/**
	 * Default output log name.
	 */
	public static final String DEFAULT_LOG = "default_log.txt";
	/**
	 * Default lua script path.
	 */
	public static final String DEFAULT_LUA_PATH = "http_calls.lua";
	/**
	 * Default number of load generation threads.
	 */
	public static final int DEFAULT_THREAD_NUM = 128;
	
	/**
	 * The default warmup duration in seconds.
	 */
	public static final int DEFAULT_WARMUP_DURATION = 30;
	/**
	 * The default pause to wait after warmup before starting measurement (in seconds).
	 */
	public static final int DEFAULT_WARMUP_PAUSE = 5;
	
	/** Socket information */
	/**
	 * Localhost IP.
	 */
	public static final String LOCALHOST_IP = "127.0.0.1";
	/**
	 * Port for communication between load generator and director.
	 */
	public static final int LOAD_GEN_PORT = 24226;

	/** Communication keys between director and load generator */
	/**
	 * Signal for incoming arrival rates.
	 */
	public static final String ARRIVALRATE_SEND_KEY = "dlim";
	/**
	 * Signal for LUA script sending.
	 */
	public static final String SCRIPT_SEND_KEY = "luascript";
	/**
	 * Termination signal for LUA script sending.
	 */
	public static final String SCRIPT_TERM_KEY = "tools.descartes.dlin.httploadgenerator.signal.luascriptterm";
	/**
	 * Signal when done.
	 */
	public static final String DONE_KEY = "done";
	/**
	 * Signal for sending number of load generation threads.
	 */
	public static final String THREAD_NUM_KEY = "threadnum:";
	/**
	 * Signal for sending the http timeout.
	 */
	public static final String TIMEOUT_KEY = "timout:";
	/**
	 * Signal for sending script path.
	 */
	public static final String SCRIPT_PATH_KEY = "scriptpath:";
	/**
	 * Signal for sending results.
	 */
	public static final String RESULTS_KEY = "results";
	/**
	 * Signal for starting measurement. Followed by comma-seperated parameters.
	 * Expected: "start,[randomseed(int)],[randombatchtimes(boolean)],[warmup-duration-s(int)],
	 * [warmup-load(double)],[warmup-pause-s(int)]"
	 */
	public static final String START_KEY = "start";
	/**
	 * Ok response.
	 */
	public static final String OK_KEY = "ok";

}
