# HTTP Load Generator

The HTTP Load Generator is a load generator designed to generate HTTP loads with varying load intensities. It uses load intensity specifications as specified by [LIMBO](http://descartes.tools/limbo) to generate loads that vary in intensity (number of requests per second) over time. The load generator logs application level data and supports connecting to external power measurement daemons. It specifies the http requests themselves using LUA scripts, which are read at run-time.

The HTTP Load Generator tool can be run in two modes: As director and as load generator. The director mode starts the tool in a mode where it parses necessary profiles and scripts, connects to power measurement devices, and collects data. The load generator mode receives instructions from the director and generates the actual requests. The mode is set using command line switches, which means that two instances of the HTTP Load Generator must be running for a test, one in each mode.

Structure of this README:
1. [Features and Application Scenarios](#Features-and-Application-Scenarios)
2. [Getting Started with the Load Generator](#Getting-Started-with-the-Load-Generator)
3. [Creating Custom Request Profies](#Creating-Custom-Request-Profies)
4. [Using Power Daemons](#Using-Power-Daemons)
5. [All Command Line Switches](#All-Command-Line-Switches)

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
* [Example Load Intensity Profile](HTTP-Load-Generator/examplefiles/curveArrivalRates.csv): An example load intensity profile that runs for a minute. its arrival rate increases for 30 seconds before decreasing again in a sinoid shape.
* [Example Request Generation LUA Script](HTTP-Load-Generator/examplefiles/http_calls_dvd.lua): A script for generating requests for the Dell DVD Store testing application. This file has quite a few comments that explain the general structure of the LUA scripts.

Download both files and place them on the director machine. For simplicity, we will assume that you place them in the same directory as the _httploadgenerator.jar_. You would now also modify the LUA script with calls for your web application.

## 3. Creating Custom Request Profies

### 3.1 Creating a Custom Load Intensity (Arrival Rate) Profile

### 3.2 Scripting the Requests themselves

## 4. Using Power Daemons

## 5. All Command Line Switches
