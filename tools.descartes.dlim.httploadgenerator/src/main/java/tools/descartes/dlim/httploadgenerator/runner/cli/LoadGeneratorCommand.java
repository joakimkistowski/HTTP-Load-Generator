package tools.descartes.dlim.httploadgenerator.runner.cli;

import picocli.CommandLine.Command;
import tools.descartes.dlim.httploadgenerator.generator.AbstractLoadGenerator;

@Command(name = "loadgenerator", 
header = "Run in director mode.",
showDefaultValues = true,
customSynopsis = "@|bold java -jar httploadgenerator.jar |@@|red loadgenerator|@ [@|yellow <options>|@...]",
description = "Runs the load generator in director mode. The director parses configuration files, "
		+ "connects to one or multiple load generators, and writes the results to the result csv file."
        )
public class LoadGeneratorCommand implements Runnable {

	@Override
	public void run() {
		AbstractLoadGenerator.executeLoadGenerator();
	}

}
