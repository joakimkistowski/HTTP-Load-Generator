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
package tools.descartes.dlim.httploadgenerator.http.lua;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Stateful helper functions for extracting matches and substrings in HTML code from LUA.
 * Stores the current response to be accessed from the LUA script.
 * @author Joakim von Kistowski
 *
 */
public class HTMLFunctions {

	private String html;
	private String[] lines;

	/**
	 * Replaces the HTMLFunctions singleton with a new one.
	 * @param html The new html content.
	 */
	public void resetHTMLFunctions(String html) {
		this.html = html;
		this.lines = html.split("\n");
		for (int i = 0; i < lines.length; i++) {
			lines[i] = lines[i].trim();
		}
	}

	/**
	 * Private constructor for only returning the same instance of the class.
	 * 
	 * @param html The html page's content, for which to provide the functions.
	 */
	public HTMLFunctions(String html) {
		resetHTMLFunctions(html);
	}

	/**
	 * Checking the HTML file saved as string for a specific expression.
	 * 
	 * @param regex The regex for which to get the matches.
	 * @return An array list of all matches.
	 */
	public ArrayList<String> getMatches(String regex) {
		ArrayList<String> matches = new ArrayList<String>();
		for (int i = 0; i < lines.length; i++) {
			if (lines[i].matches(regex)) {
				matches.add(lines[i]);
			}
		}
		return matches;
	}

	/**
	 * Postfix and prefix must be unique within a line and directly border the string
	 * that is to be extracted.
	 * @param parentString String to match against.
	 * @param prefixRegex Prefix before the section that must match.
	 * @param postfixRegex Postfix after the matching section.
	 * @return The matching section in the parent string.
	 */
	public static String extractSubString(String parentString, String prefixRegex, String postfixRegex) {
		Pattern prefixPattern = Pattern.compile(prefixRegex);
		Pattern postfixPattern = Pattern.compile(postfixRegex);
		Matcher prefixMatcher = prefixPattern.matcher(parentString);
		String subString = parentString;
		if (prefixMatcher.find()) {
			subString = subString.substring(prefixMatcher.end());
		}
		Matcher postfixMatcher = postfixPattern.matcher(subString);
		if (postfixMatcher.find()) {
			subString = subString.substring(0, postfixMatcher.start());
		}
		return subString;
	}

	/**
	 * Get the HTML content against which the html functions are run.
	 * @return The html content.
	 */
	public String getHTML() {
		return html;
	}

	/**
	 * Postfix and prefix must be unique within a line and directly border the string
	 * that is to be extracted.
	 * @param prefixRegex Prefix before the section that must match.
	 * @param matchingRegex must match the String that is to be extracted.
	 * @param postfixRegex Postfix after the matching section.
	 * @return The matching string.
	 */
	public ArrayList<String> extractAllMatches(String prefixRegex, String matchingRegex, String postfixRegex) {
		String lineRegex = ".*" + prefixRegex + matchingRegex + postfixRegex + ".*";
		ArrayList<String> matches = getMatches(lineRegex);
		ArrayList<String> subStrings = new ArrayList<String>();
		for (String line : matches) {
			subStrings.add(extractSubString(line, prefixRegex, postfixRegex));
		}
		return subStrings;
	}
}
