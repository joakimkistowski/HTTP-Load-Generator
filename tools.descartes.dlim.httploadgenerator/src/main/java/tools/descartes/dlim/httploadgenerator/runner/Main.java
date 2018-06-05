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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import tools.descartes.dlim.httploadgenerator.generator.ArrivalRateTuple;
import tools.descartes.dlim.httploadgenerator.runner.cli.DirectorCommand;
import tools.descartes.dlim.httploadgenerator.runner.cli.LoadGeneratorCommand;

/**
 * Main class is entry point of the application. Passed arguments are checked
 * and corresponding mode is started (Director, Load Generator)
 * 
 * @author Joakim von Kistowski, Maximilian Deffner
 *
 */
@Command(name = "httploadgenerator",
	customSynopsis = "@|bold java -jar httploadgenerator.jar |@@|red COMMAND|@ [@|yellow <options>|@...]",
	description = "HTTP load generator for varying load intensities.",
	subcommands = { DirectorCommand.class, LoadGeneratorCommand.class })
public class Main implements Runnable {

	/** The constant logging instance. */
	private static final Logger LOG = Logger.getLogger(Main.class.getName());

	@Option(names = { "-h", "--help" }, usageHelp = true, description = "Display this help message.")
	private boolean helpRequested = false;
	
	/**
	 * Main method for splitting up passed arguments and starting corresponding
	 * mode.
	 * 
	 * @param args
	 *            Passed arguments by calling the application.
	 */
	public static void main(String[] args) {
		CommandLine top = new CommandLine(new Main());
		if (args.length == 0) {
			top.usage(System.out);
		}
		
		List<CommandLine> parsedCommands;
		try {
		    parsedCommands = top.parse(args);
		} catch (ParameterException ex) { // incorrect user input for one of the subcommands
		    LOG.severe("Error parsing command line: " + ex.getMessage());
		    ex.getCommandLine().usage(System.out); // get the offended subcommand from the exception
		    return;
		}
		
		for (CommandLine parsed : parsedCommands) {
		    if (parsed.isUsageHelpRequested()) {
		        parsed.usage(System.out);
		        return;
		    } else if (parsed.isVersionHelpRequested()) {
		        parsed.printVersionHelp(System.out);
		        return;
		    }
		}
		Object last = parsedCommands.get(parsedCommands.size() - 1).getCommand();
		if (last instanceof Runnable) {
		    ((Runnable) last).run();
		    return;
		}
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
	}

	/**
	 * Reads an arrival rate list file to an arrival rate list. Also capable of
	 * reading request time stamp files. In this case, all arrival rates in the
	 * list are "-1".
	 * 
	 * @param file
	 *            The file to read.
	 * @param offset
	 *            The time offset on which to start reading.
	 * @return A list of arrival rate tuples or request time stamps (arrival
	 *         rates = "-1").
	 * @throws IOException
	 *             If file is not found, can not be read, etc.
	 */
	public static List<ArrivalRateTuple> readFileToList(File file, double offset) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(file));
		List<ArrivalRateTuple> arrRates = new ArrayList<ArrivalRateTuple>();
		// read arrival rate tuples
		try {
			arrRates = ArrivalRateTuple.readList(br, offset);
			// error reading tuples, it's probably a request time stamp file
		} catch (IndexOutOfBoundsException e) {
			LOG.log(Level.SEVERE, "Index out of bounds, wrong format.");
		}
		if (arrRates.size() == 0) {
			br.close();
			br = new BufferedReader(new FileReader(file));
			arrRates = readRequestTimeStamps(br, offset);
		}
		br.close();
		return arrRates;
	}

	/**
	 * Parses the request time stamps of the specified file.
	 * 
	 * @param br
	 *            Buffer reader for reading the content of the file
	 * @param offset
	 *            Adding an offset to the time stamps
	 * @return Array list with parsed time stamps
	 * @throws IOException
	 *             If file is not found, can not be read, etc.
	 */
	private static List<ArrivalRateTuple> readRequestTimeStamps(BufferedReader br, double offset) throws IOException {
		ArrayList<ArrivalRateTuple> timeStamps = new ArrayList<ArrivalRateTuple>();
		String line = br.readLine();
		while (line != null) {
			line = line.trim();
			if (!line.isEmpty()) {
				double ts;
				try {
					ts = Double.parseDouble(line);
				} catch (NumberFormatException e) {
					ts = (Double.parseDouble(line.substring(0, line.length() - 1)));
				}
				timeStamps.add(new ArrivalRateTuple(ts - offset, -1.0));
			}
			line = br.readLine();
		}
		return timeStamps;
	}
}
