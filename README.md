# HTTP Load Generator

The HTTP Load Generator is a load generator designed to generate HTTP loads with varying load intensities. It uses load intensity specifications as specified by [LIMBO](http://descartes.tools/limbo) to generate loads that vary in intensity (number of requests per second) over time. The load generator logs application level data and supports connecting to external power measurement daemons. It specifies the http requests themselves using LUA scripts, which are read at run-time.

The HTTP Load Generator tool can be run in two modes: As director and as load generator. The director mode starts the tool in a mode where it parses necessary profiles and scripts, connects to power measurement devices, and collects data. The load generator mode receives instructions from the director and generates the actual requests. The mode is set using command line switches, which means that two instances of the HTTP Load Generator must be running for a test, one in each mode.

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
3. An infrastructure that allows for collection of powre measurements from external measurement devices (optional).

The load generator can be used for testing of web applications regarding testing of energy efficiency, testing of scaling behavior over time and testing of time-dependent scenarios, such as the effect of sudden bursts, seasonal variations in request patterns, trends, etc.

## 2. Getting Started with the Load Generator

First build or download the httploadgenetor.jar (TODO). Deploy the httploadgenerator on two machines:
1. The **director machine** (experiment controller): Usually your PC. This machine must have access to the load profile and request script to be run. In addition this machine must be able to communicate with the power meters (optional).
2. The **load generator machine**: The machine that sends the network loads. Usually a quite powerful machine. No additional files, except for the jar itself, are required on this machine.

In addition to the jar, you need a load intensity profile and a LUA script for generating the actual requests. We provide an example for each in the examplefiles directory:
* [Example Load Intensity Profile](https://github.com/joakimkistowski/HTTP-Load-Generator/tree/master/examplefiles/curveArrivalRates.csv): An example load intensity profile that runs for a minute. its arrival rate increases for 30 seconds before decreasing again in a sinoid shape.
* [Example Minimal Request Generation LUA Script](https://github.com/joakimkistowski/HTTP-Load-Generator/tree/master/examplefiles/http_calls_minimal.lua): A minimal example script for generating requests. Alternates between calls on index.html and index.htm. For a more complex example see [here](https://github.com/joakimkistowski/HTTP-Load-Generator/tree/master/examplefiles/http_calls_dvd.lua). This secnd scripts specifies calls for the Dell DVD Store. However, use the minimal script for now, bacouse it is far easier to run with almost any web application.

Download both files and place them on the director machine. For simplicity, we will assume that you place them in the same directory as the _httploadgenerator.jar_. You would now also modify the LUA script with calls for your web application. For the minimal example, just make sure that index.html and index.htm are accessible and enter the adress of your server hosting the web application in line 7 of the script.

Now, on the **load generator machine** start the HTTP Load Generator in load generator mode, using the following command line with the _-l_ switch:

    $ java -jar httploadgenerator.jar -l

Next, on the **director machine** start the HTTP Load Generator in director mode:

    $ java -jar .\httploadgenerator.jar -d -s IP_OF_THE_LOAD_GENERATOR_MACHINE -a curveArrivalRates.csv -o testlog.csv -r 5 -l .\http_calls_minimal.lua

The director call does the following:
* _-d_ starts the director mode.
* _-s_ specifies the address of the load generator machine.
* _-a_ specifies the load intensity (*a*rrival rate) profile.
* _-o_ specifies the name of the output log, containing the results.
* _-r_ specifies the random seed (always specify it for reproducibility)
* _-l_ specifies the *L*UA script.

The director will now connect with the load generator, send the load intensity profile, script, and other settings. It will then prompt you to press enter to start the test. Once the test has concluded, the output log file will appear in the directory.

## 3. Creating Custom Request Profies

Since you don't always to be running our example profiles, you can specify your own. We specify the load intensity (arrival rate) and the requests separately in separate files.

### 3.1 Creating a Custom Load Intensity (Arrival Rate) Profile

The easiest way of creating a load intensity profile is using the [LIMBO](http://descartes.tools/limbo) tool. LIMBO is an Eclipse plugin for modeling arrival rates. You can model the arrival rates using a graphical editor and then export them to various formats. The HTTP Load Generator supports two of those formats:

* Simple Arrival Rate File (.txt)
* Simple Arrival Rate File (.csv)

To get files of this format, right click on a _.dlim_ model's file in the Eclipse Package Explorer, then select _Generate Time Stamps_. A dialog with export options appears and our two supported options should be among them. In the following dialog, you get to pick the sampling interval, this is the interval at which LIMBO samples the arrival rate curve and is also the interval at which the HTTP Load Generator re-adjusts the arrival rate and reports results. It can be freely configured, but an interval of 1 is recommended.

If you do not wish to use LIMBO (you should, though) you can also speficy the arrival rates manually. The format of the file is a simple CSV format with the first column being the middle time stamp of each scheduling interval and the second column being the load intensity. Intervals must always have the step (e.g. always increment by 1) and you may not skip any!

Example:

    0.5,10
    1.5,20
    2.5,30

Note, that the time stamp is always the middle of the interval. Meaning that it is 0.5, 1.5, ... instead of 0, 1, ... This is for compatibility with LIMBO, where this design decision makes more sense. Again, intervals with a period of 1 secod (0.5, 1.5, 2.5, ...) are recommended.

### 3.2 Scripting the Requests Themselves

## 4. Using Power Daemons

## 5. All Command Line Switches
