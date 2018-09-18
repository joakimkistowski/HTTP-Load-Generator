/**
 * Copyright 2018 Joakim von Kistowski
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
package tools.descartes.dlim.httploadgenerator.runner.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import tools.descartes.dlim.httploadgenerator.runner.Director;
import tools.descartes.dlim.httploadgenerator.runner.IRunnerConstants;

@Command(name = "director",
header = "Run in director mode.",
showDefaultValues = true,
customSynopsis = "@|bold java -jar httploadgenerator.jar |@@|red director|@ [@|yellow <options>|@...]",
description = "Runs the load generator in director mode. The director parses configuration files, "
		+ "connects to one or multiple load generators, and writes the results to the result csv file."
        )
/**
 * Command class capturing all command line parameters and options when running the director.
 * @author Joakim von Kistowski
 *
 */
public class DirectorCommand implements Runnable {
	
	@Option(names = {"--arrivals", "--load", "--loadintensity", "-a"},
			paramLabel = "ARRIVALRATE_FILE",
			description="Path of the (LIMBO-generated) @|yellow a|@rrival rate file.")
	private String profilePath = IRunnerConstants.DEFAULT_ARRIVAL_RATE_PATH;
	
	@Option(names = {"--outfile", "--out", "--log", "--csv", "-o"},
			paramLabel = "OUT_FILE",
			description="Name of @|yellow o|@utput log relative to directory of arrival rate file.")
	private String outName = IRunnerConstants.DEFAULT_LOG;
	
	@Option(names = {"--power", "--poweraddress", "-p"},
			paramLabel = "POWER_IP[:POWER_PORT]",
			description="Adress of @|yellow p|@owerDaemon. Multiple addresses are "
					+ "delimited with \",\" (no white-spaces). No address => no power measurements.")
	private String powerAddresses = null;
	
	@Option(names = {"--ip", "--adress", "--generator", "-s"},
			paramLabel = "IP",
			description="Adre@|yellow s|@s of load generator(s). Multiple addresses are delimited with \",\" "
					+ "(no white-spaces).")
	private String generators = IRunnerConstants.LOCALHOST_IP;
	
	@Option(names = {"--randomseed", "--random", "--seed", "-r"},
			paramLabel = "SEED",
			description="Integer seed for the @|yellow r|@andom generator. Seed of 0 => Equi-distant dispatch times.")
	private int randomSeed = 5;
	
	@Option(names = {"--threads", "--threadcount", "-t"},
			paramLabel = "NUM_THREADS",
			description="Number of @|yellow t|@hreads used by the load generator. "
					+ "Increase this number in case of dropped transactions.")
	private int threadCount = IRunnerConstants.DEFAULT_THREAD_NUM;;
	
	@Option(names = {"--timeout", "-u"},
			paramLabel = "TIMEOUT",
			description="@|yellow U|@rl connection timeout in ms. Timout of 0 => no timout.")
	private int urlTimeout = 0;
	
	@Option(names = {"--script", "--lua", "-l"},
			paramLabel = "LUASCRIPT",
			description="Path of the @|yellow l|@ua script that generates the call URLs.")
	private String scriptPath = IRunnerConstants.DEFAULT_LUA_PATH;
	
	@Option(names = {"--powerclass", "--classname", "--class", "-c"},
			paramLabel = "POWER_CLASS",
			description="Fully qualified @|yellow c|@lassname of the power communicator."
					+ " Must be on the classpath.")
	private String powerCommunicatorClassName;
	
	@Option(names = {"--warmup", "--warmuprate", "--warmup-rate", "--wr"},
			paramLabel = "WARMUP_RATE",
			description="Load intensity for warmup period. Warmup is skipped if set to < 1.")
	private double warmupRate = 0;
	
	@Option(names = {"--warmupduration", "--warmup-duration", "--wd"},
			paramLabel = "WARMUP_DURATION",
			description="Duration of the warmup period in seconds. Warmup is skipped if set to 0.")
	private int warmupDuration =  IRunnerConstants.DEFAULT_WARMUP_DURATION;
	
	@Option(names = {"--warmupause", "--warmup-pause", "--wp"},
			paramLabel = "WARMUP_PAUSE",
			description="Duration of the pause after conclusion of the warmup period in seconds."
					+ " Ignored if warmup is skipped.")
	private int warmupPause =  IRunnerConstants.DEFAULT_WARMUP_PAUSE;
	
	@Option(names = {"--randomize-users"},
			description="With this flag, threads will not pick users (HTTP input generators, LUA script contexts) in order."
					+ " Instead, each request will pick a random user."
					+ " This setting can compensate for burstiness, caused by the fixed order of LUA calls."
					+ " It is highly recommended to configure long warmup times when randomizing users.")
	private boolean randomizeUsers =  false;
	
	@Option(names = { "-h", "--help" }, usageHelp = true, description = "Display this help message.")
	private boolean helpRequested = false;

	@Override
	public void run() {
		Director.executeDirector(profilePath, outName, powerAddresses, generators,
				randomSeed, threadCount, urlTimeout, scriptPath, randomizeUsers, warmupRate, warmupDuration,
				warmupPause, powerCommunicatorClassName);
	}
}
