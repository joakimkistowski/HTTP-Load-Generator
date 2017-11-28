# HTTP Load Generator

Download the binary [here](https://se3.informatik.uni-wuerzburg.de/httploadgenerator/tools.descartes.dlim.httploadgenerator/target/httploadgenerator.jar).

The HTTP Load Generator is a load generator designed to generate HTTP loads with varying load intensities. It uses load intensity specifications as specified by [LIMBO](http://descartes.tools/limbo) to generate loads that vary in intensity (number of requests per second) over time. The load generator logs application level data and supports connecting to external power measurement daemons. It specifies the http requests themselves using LUA scripts, which are read at run-time.

The HTTP Load Generator tool can be run in two modes: As director and as load generator. The director mode starts the tool in a mode where it parses necessary profiles and scripts, connects to power measurement devices, and collects data. The load generator mode receives instructions from the director and generates the actual requests. The mode is set using command line switches, which means that at least two instances of the HTTP Load Generator must be running for a test, one in each mode. The HTTP Load Generator also supports multi-node load generation, where one director connects to multiple instances running in load generator mode.

Structure of this README:
1. [Features and Application Scenarios](#1-features-and-application-scenarios)
2. [Getting Started with the Load Generator](#2-getting-started-with-the-load-generator)
3. [Creating Custom Request Profies](#3-creating-custom-request-profies)
4. [Using Power Daemons](#4-using-power-daemons)
5. [All Command Line Switches](#5-all-command-line-switches)

## 1. Features and Application Scenarios

The load generator has the following three primary features:
1. Generation of loads with a varying load intensity (number of requests per second).
2. Generation of loads with context sensitive requests that can be scripted to be dependent on previous responses.
3. An infrastructure that allows for collection of power measurements from external measurement devices (optional).

The load generator can be used for testing of web applications regarding testing of energy efficiency, testing of scaling behavior over time, and testing of time-dependent scenarios, such as the effect of sudden bursts, seasonal variations in request patterns, trends, etc.

## 2. Getting Started with the Load Generator

First build or download the [httploadgenetor.jar](https://se3.informatik.uni-wuerzburg.de/httploadgenerator/tools.descartes.dlim.httploadgenerator/target/httploadgenerator.jar). Deploy the httploadgenerator on two machines:
1. The **director machine** (experiment controller): Usually your PC. This machine must have access to the load profile and request script to be run. In addition, this machine must be able to communicate with the power meters (optional, we are not using a power meter in this _getting started_ section).
2. The **load generator machine**: The machine that sends the network loads. Usually a quite powerful machine. No additional files, except for the jar itself, are required on this machine. You may also choose to use multiple load generation machines if you do not have a single machine with sufficient power or network capabilities.

In addition to the jar, you need a load intensity profile and a LUA script for generating the actual requests. We provide an example for each in the examplefiles directory:
* [Example Load Intensity Profile](https://github.com/joakimkistowski/HTTP-Load-Generator/tree/master/examplefiles/curveArrivalRates.csv): An example load intensity profile that runs for one minute. Its arrival rate increases for 30 seconds before decreasing again in a sinoid shape.
* [Example Minimal Request Generation LUA Script](https://github.com/joakimkistowski/HTTP-Load-Generator/tree/master/examplefiles/http_calls_minimal.lua): A minimal example script for generating requests. Alternates between calls on index.html and index.htm. For a more complex example see [here](https://github.com/joakimkistowski/HTTP-Load-Generator/tree/master/examplefiles/http_calls_dvd.lua). This second scripts specifies calls for the Dell DVD Store. However, use the minimal script for now. It is far easier to run with almost any web application.

Download both files and place them on the director machine. For simplicity, we will assume that you place them in the same directory as the _httploadgenerator.jar_. You would now also modify the LUA script with calls for your web application. For the minimal example, just make sure that index.html and index.htm are accessible and enter the adress of your server hosting the web application in line 7 of the script.

Now, on the **load generator machine** start the HTTP Load Generator in load generator mode, using the following command line with the _-l_ switch. If you use multiple load generator machines, do this on each of them:

    $ java -jar httploadgenerator.jar -l

Next, on the **director machine** start the HTTP Load Generator in director mode:

    $ java -jar httploadgenerator.jar -d -s IP_OF_THE_LOAD_GENERATOR_MACHINE -a curveArrivalRates.csv -o testlog.csv -r 5 -l http_calls_minimal.lua

The director call does the following:
* _-d_ starts the director mode.
* _-s_ specifies the address of the load generator machine. For multiple load generators, use a comma delimiter (no white spaces!).
* _-a_ specifies the load intensity (*a*rrival rate) profile.
* _-o_ specifies the name of the *o*utput log, containing the results.
* _-r_ specifies the random seed (always specify it for reproducibility)
* _-l_ specifies the *L*UA script.

The director will now connect with the load generator, send the load intensity profile, script, and other settings. It will then prompt you to press enter to start the test. Once the test has concluded, the output log file will appear in the directory.

## 3. Creating Custom Request Profiles

Since you don't always want to be running our example profiles, you can specify your own. We specify the load intensity (arrival rate) and the requests separately in separate files.

### 3.1 Creating a Custom Load Intensity (Arrival Rate) Profile

The easiest way of creating a load intensity profile is using the [LIMBO](http://descartes.tools/limbo) tool. LIMBO is an Eclipse plugin for modeling arrival rates. You can model the arrival rates using a graphical editor and then export them to various formats. The HTTP Load Generator supports two of those formats:

* Simple Arrival Rate File (.txt)
* Simple Arrival Rate File (.csv)

To get files of this format, right click on a _.dlim_ model's file in the Eclipse Package Explorer, then select _Generate Time Stamps_. A dialog with export options appears and our two supported options should be among them. In the following dialog, you get to pick the sampling interval, this is the interval at which LIMBO samples the arrival rate and is also the interval at which the HTTP Load Generator re-adjusts the arrival rate and reports results. It can be freely configured, but an interval of 1 is recommended.

If you do not wish to use LIMBO (you should, though) you can also speficy the arrival rates manually. The format of the file is a simple CSV format with the first column being the middle time stamp of each scheduling interval and the second column being the load intensity. Intervals must always have the same step (e.g. always increment by 1) and you may not skip any!

Example:

    0.5,10
    1.5,20
    2.5,30

Note, that the time stamp is always the middle of the interval. Meaning that it is 0.5, 1.5, ... instead of 0, 1, ... This is for compatibility with LIMBO, where this design decision makes more sense. Again, intervals with a period of 1 second (0.5, 1.5, 2.5, ...) are recommended.

### 3.2 Scripting the Requests Themselves

The requests are specified using a LUA script. We recommend modifying one of the examples, such as the [minimal example](https://github.com/joakimkistowski/HTTP-Load-Generator/tree/master/examplefiles/http_calls_minimal.lua) or the [Dell DVD Store example](https://github.com/joakimkistowski/HTTP-Load-Generator/tree/master/examplefiles/http_calls_dvd.lua). The examples contain explanations in their code comments.

Two LUA functions in the script are called from by HTTP Load Generator:
* **onCycle()**: Is called at the beginning of each call cycle. No return value is expected. Initialize all global variables here. Note that _math.random_ is already initialized using a fixed seed (5) for reproducibility.
* **onCall(callnum)**: Is called for each HTTP request. Must return the URL to call. This function is called with an index for the call, starting at 1 (LUA convention). The index increases with each call and resets once _onCall_ returns _nil_.

You can parse the HTTP response in the _onCall_ function using regular expressions. We provide HTML helper functions (considering the response is usually html). Specifically, we offer:
* _html.getMatches( regex )_ : Returns all lines in the returned text stream that match a provided regex.
* _html.extractMatches( prefixRegex, postfixRegex )_ : Returns all matches that are preceeded by a prefixRegex match and followed by a postfixRegex match. The regexes must have one unique match for each line in which they apply.
* _html.extractMatches( prefixRegex, matchingRegex, postfixRegex )_ : Variant of extractMatches with a matching regex defining the string that is to be extracted.

Note that all regular expressions are passed directly to the Java backend. They must be specified, as if they were specified directly in the Java code. I.e., use "\\\\" instead of a single "\\".

URLs returned by _onCall_ are called using HTTP GET. To send a HTTP POST request, prepend _[POST]_ (including the brackets) before the returned URL.

You can test your LUA scripts using our HTTP Script Tester ([download the binary here](https://se3.informatik.uni-wuerzburg.de/httploadgenerator/tools.descartes.dlim.httpscripttester/target/httpscripttester.jar)). The HTTP Script Tester is a graphical application that runs the script and renders HTML responses in a graphical web view to check for correct functionality of the script. Lauch the script tester using your graphical user interface or using the following command line:

    $ java -jar httpscripttester.jar ./MYSCRIPTFILE.lua

We recommend a command line lauch, as the command line in the background will show potential errors and LUA _print_ statements, which is very helpful when debugging.

## 4. Using Power Daemons

The HTTP Load Generator supports connecting to power analyzer daemons. The general idea behind the infrastructure is to connect to a network daemon that may run on a separate machine with the power analyzer. Unfortunately, most power analyzer daemons, such as the SPEC PTDaemon, have restrictive licenses, prohibiting their use with the HTTP Load Generator or preventing us from providing them to you.

We provide a **IPowerCommunicator** interface in the _tools.descartes.dlim.httploadgenerator.power_ package. Implement your own power daemon communicator against this interface. The _HIOKICommunicator_ is a functioning reference implementation that supports ethernet-capable HIOKI power measurement devices. The _TMCTLDCommunicator_ can be used as a further example implementation.

To start the HTTP Load Generator with your power communicator, add it to the classpath and then specify the fully quailified class name of your communicator using the _-c_ switch of the HTTP Load Generator in director mode. Use the _-p_ switch to specify the network address of your power daemon. You may enter multiple, comma separated (no whitespeces!), addresses. If you do, the director will instantiate a power communicator for each of those addresses and log its results in a separate column.

Example (with the power communicator compiled into the httploadgenerator.jar):

    $ java -jar httploadgenerator.jar -d -s LOADGENIP -a myArrivalRates.csv -o myLog.csv -p PWRRDAEEMONIP:PWRDAEMONPORT -c my.fully.qualified.Classname -l./http_calls.lua

Example (with the power communicator compiled into a separate jar):

    $ java -cp "MYJAR.jar;httploadgenerator.jar" tools.descartes.dlim.httploadgenerator.runner.Main -d -s LOADGENIP -a myArrivalRates.csv -o myLog.csv -p PWRRDAEEMONIP:PWRDAEMONPORT -c my.fully.qualified.Classname -l./http_calls.lua

## 5. All Command Line Switches

Use the _-h_ switch to show the following help page:

Usage:

    $ java -jar httploadgenerator.jar [-d|-l|-h [optional params]]
   
Example:

    $ java -jar httploadgenerator.jar -d -s 192.168.0.201 -a ./arrivalRates/test.txt -o myLog.csv -p 127.0.0.1:8888 -c tools.descartes.dlim.httploadgenerator.power.TMCTLDCommunicator -l./http_calls.lua

Primary parameters (pick one):

* "_-d_": 'd'irector mode. starts the director. Additional optional parameters are useful.
* "_-l_": 'l'oad generator mode. Needs no additional parameters.
* "_-h_": the 'h'elp page.

Secondary parameters for director (optional):
Missing parameters may cause the director to prompt for the data.
* "_-s [ip]_": Adre's's of load generator. Multiple addresses must be delimited with ",".
* "_-p [ip[:port]]_": Adress of 'p'owerDaemon. No address => no power measurements.
* "_-a [path]_": Path of LIMBO-generated 'a'rrival rate file.
* "_-o [name]_": Name of 'o'utput log relative to directory of arrival rate file.
* "_-r [seed]_": Integer seed for the 'r'andom generator. No seed => Equi-distant dispatch times.
* "_-l [Lua script]_": Path of the 'l'ua script that generates the call URLs. No script => "http_calls.lua".
* "_-t [thread count]_": Number of threads in load generator. No thread count => 128.
* "_-u [url con timeout]_": 'U'rl connection timeout in ms. Default => no timout.
* "_-c [class name]_": Fully qualified classname of the power communicator. Must be on the classpath.
