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
public class DirectorCommand implements Runnable {
	
	@Option(names = {"--arrivals", "--load", "--loadintensity", "-a"},
			paramLabel = "ARRIVALRATEFILE",
			description="Path of the (LIMBO-generated) @|yellow a|@rrival rate file.")
	private String profilePath = IRunnerConstants.DEFAULT_ARRIVAL_RATE_PATH;
	
	@Option(names = {"--outfile", "--log", "--csv", "-o"},
			paramLabel = "OUTFILE",
			description="Name of @|yellow o|@utput log relative to directory of arrival rate file.")
	private String outName = IRunnerConstants.DEFAULT_LOG;
	
	@Option(names = {"--power", "--poweraddress", "-p"},
			paramLabel = "POWERIP[:POWERPORT]",
			description="Adress of @|yellow p|@owerDaemon. Multiple addresses are "
					+ "delimited with \",\". No address => no power measurements.")
	private String[] powerAddress = {};
	
	@Option(names = {"--ip", "--adress", "--generator", "-s"},
			paramLabel = "IP",
			description="Adre@|yellow s|@s of load generator(s). Multiple addresses are delimited with \",\".")
	private String[] generator = {IRunnerConstants.LOCALHOST_IP};
	
	@Option(names = {"--randomseed", "--random", "--seed", "-r"},
			paramLabel = "SEED",
			description="Integer seed for the @|yellow r|@andom generator. Seed of 0 => Equi-distant dispatch times.")
	private int randomSeed = 5;
	
	@Option(names = {"--threads", "--threadcount", "-t"},
			paramLabel = "NUM_THREADS",
			description="Number of @|yellow t|@hreads used by the load generator. "
					+ "Increase this number in case of dropped transactions.")
	private int threadCount = IRunnerConstants.DEFAULT_THREAD_NUM;;
	
	@Option(names = {"--timout", "-u"},
			paramLabel = "TIMOUT",
			description="@|yellow U|@rl connection timeout in ms. Timout of 0 => no timout.")
	private int urlTimeout = 0;
	
	@Option(names = {"--script", "--lua", "-l"},
			paramLabel = "LUASCRIPT",
			description="Path of the @|yellow l|@ua script that generates the call URLs.")
	private String scriptPath = IRunnerConstants.DEFAULT_LUA_PATH;
	
	@Option(names = {"--powerclass", "--classname", "--class", "-c"},
			paramLabel = "POWERCLASS",
			description="Fully qualified @|yellow c|@lassname of the power communicator."
					+ " Must be on the classpath.")
	private String powerCommunicatorClassName;
	
	@Option(names = { "-h", "--help" }, usageHelp = true, description = "Display this help message.")
	private boolean helpRequested = false;

	@Override
	public void run() {
		Director.executeDirector(profilePath, outName, powerAddress, generator,
				randomSeed, threadCount, urlTimeout, scriptPath, powerCommunicatorClassName);
	}
}
