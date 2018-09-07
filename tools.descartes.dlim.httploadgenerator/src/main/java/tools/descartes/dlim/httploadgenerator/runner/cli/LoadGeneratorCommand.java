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
import tools.descartes.dlim.httploadgenerator.generator.AbstractLoadGenerator;

@Command(name = "loadgenerator", 
header = "Run in load generator mode.",
showDefaultValues = true,
customSynopsis = "@|bold java -jar httploadgenerator.jar |@@|red loadgenerator|@ [@|yellow <options>|@...]",
description = "Runs the load generator in director mode. The director parses configuration files, "
		+ "connects to one or multiple load generators, and writes the results to the result csv file."
        )
/**
 * Command class capturing all command line parameters and options (which are none)
 * when running the load generator.
 * @author Joakim von Kistowski
 *
 */
public class LoadGeneratorCommand implements Runnable {

	@Override
	public void run() {
		AbstractLoadGenerator.executeLoadGenerator();
	}

}
