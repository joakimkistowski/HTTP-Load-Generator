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

import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.VarArgFunction;

/**
 * Helper functions for the LUA script provides methods for calling script methods. The class provides methods
 * for checking for matches of particular expressions in a HTML file. The classes are split up by the number
 * of arguments per function.
 * 
 * @author Joakim von Kistowski
 *
 */
public class HTMLLuaFunctions {

	/**
	 * Function to get matches that correspond to a single regex.
	 * @author Joakim von Kistowski
	 *
	 */
	public static class GetMatches extends OneArgFunction {

		private HTMLFunctions htmlfunctions;
		
		/**
		 * Instantiate the function.
		 * @param htmlfunctions Reference to the stateful {@link HTMLFunctions}
		 * container that knows the most recent response.
		 */
		public GetMatches(HTMLFunctions htmlfunctions) {
			this.htmlfunctions = htmlfunctions;
		}
		
		@Override
		public LuaValue call(LuaValue regex) {
			String regexString = regex.optjstring("");
			ArrayList<String> matches = new ArrayList<String>();
			if (!regexString.isEmpty()) {
				matches = htmlfunctions.getMatches(regexString);
			}
			return LuaHelpers.toLuaTable(matches);
		}

	}

	/**
	 * Function to get matches that correspond to a prefix/postfix regexes.
	 * @author Joakim von Kistowski
	 *
	 */
	public static class ExtractAllMatches extends VarArgFunction {

		private HTMLFunctions htmlfunctions;
		
		/**
		 * Instantiate the function.
		 * @param htmlfunctions Reference to the stateful {@link HTMLFunctions}
		 * container that knows the most recent response.
		 */
		public ExtractAllMatches(HTMLFunctions htmlfunctions) {
			this.htmlfunctions = htmlfunctions;
		}
		
		@Override
		public LuaValue call(LuaValue prefixRegex, LuaValue postfixRegex) {
			String prefix = prefixRegex.optjstring("");
			String postfix = postfixRegex.optjstring("");
			ArrayList<String> matches = new ArrayList<String>();
			if (!prefix.isEmpty() && !postfix.isEmpty()) {
				matches = htmlfunctions.extractAllMatches(prefix, ".*", postfix);
			}
			return LuaHelpers.toLuaTable(matches);
		}

		@Override
		public LuaValue call(LuaValue prefixRegex, LuaValue matchingRegex, LuaValue postfixRegex) {
			String prefix = prefixRegex.optjstring("");
			String postfix = postfixRegex.optjstring("");
			String center = matchingRegex.optjstring("");
			ArrayList<String> matches = new ArrayList<String>();
			if (!prefix.isEmpty() && !postfix.isEmpty() && !center.isEmpty()) {
				matches = htmlfunctions.extractAllMatches(prefix, center, postfix);
			}
			return LuaHelpers.toLuaTable(matches);
		}

	}
}
