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
package tools.descartes.httploadgenerator.http;

import java.io.File;
import java.net.CookieManager;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.JsePlatform;

import tools.descartes.httploadgenerator.http.lua.HTMLFunctions;
import tools.descartes.httploadgenerator.http.lua.HTMLLuaFunctions.ExtractAllMatches;
import tools.descartes.httploadgenerator.http.lua.HTMLLuaFunctions.GetMatches;

/**
 * Stateful Generator for the next HTTP-GET or POST URL.
 * URLs are generated from the script passed in the constructor.
 * @author Joakim von Kistowski
 */
public class HTTPInputGenerator {

	private static final String LUA_CYCLE_INIT = "onCycle";
	private static final String LUA_CALL = "onCall";


	private int currentCallNum = 0;
	private String lastInput = "";

	private HTMLFunctions htmlFunctions = new HTMLFunctions("");
	private CookieManager cookieManager = new CookieManager();
	private Globals luaGlobals;
	/**
	 * Constructs a new HTTPInputGenerator using a Lua generation script.
	 * The Lua script must contain the onInit() and onCall(callnum) functions.
	 * onCall(callnum) must return the HTTP request for a specific call with number callnum.
	 * callnum begins at 1 (Lua convention) and increments for each call. It resets back to 1
	 * if onCall returns nil.
	 * @param scriptFile The url generator script.
	 * @param randomSeed Seed for Lua random function.
	 */
	public HTTPInputGenerator(File scriptFile, int randomSeed) {
		if (scriptFile != null) {
			luaGlobals = JsePlatform.standardGlobals();
			//luaGlobals.get("require").call(LuaValue.valueOf("tools.descartes.httploadgenerator.http.lua.HTML"));
			LuaValue library = new LuaTable();
			library.set("getMatches", new GetMatches(htmlFunctions));
			library.set("extractMatches", new ExtractAllMatches(htmlFunctions));
			luaGlobals.set("html", library);
			luaGlobals.get("math").get("randomseed").call(LuaValue.valueOf(5));
			luaGlobals.get("dofile").call(LuaValue.valueOf(scriptFile.getAbsolutePath()));
		}
	}

	/**
	 * Returns the next URL for the HTTPTransaction. Runs the script.
	 * @return The next URL to call.
	 */
	public String getNextInput() {
		if (currentCallNum < 1) {
			restartCycle();
		}
		LuaValue lvcall = luaGlobals.get(LUA_CALL).call(LuaValue.valueOf(currentCallNum));
		if (lvcall.isnil()) {
			restartCycle();
			return getNextInput();
		} else {
			currentCallNum++;
			lastInput = lvcall.optjstring("");
			return lastInput;
		}
	}

	/**
	 * Restarts the call cycle.
	 * Resets the current call number to one and calls init from the script.
	 */
	private void restartCycle() {
		currentCallNum = 1;
		LuaValue cycleInit = luaGlobals.get(LUA_CYCLE_INIT);
		if (!cycleInit.isnil()) {
			cycleInit.call();
		}
	}

	/**
	 * Current number of the lua call (position in call cycle).
	 * @return The current number of the lua call.
	 */
	public int getCurrentCallNum() {
		return currentCallNum;
	}
	
	/**
	 * Reset the HTML functions that are passed to LUA.
	 * @param html The html response that will be accessed from LUA next.
	 */
	public void resetHTMLFunctions(String html) {
		htmlFunctions.resetHTMLFunctions(html);
	}
	
	/**
	 * Get the cookie manager.
	 * @return The cookie manager.
	 */
	public CookieManager getCookieManager() {
		return cookieManager;
	}
	
	/**
	 * Get the last call that was generated on calling {@link #getNextInput()}.
	 * @return The last call URL.
	 */
	public String getLastCall() {
		return lastInput;
	}
	
	/**
	 * Get the current HTML content that was last received using this generator.
	 * @return The HTML content.
	 */
	public String getCurrentHTML() {
		return htmlFunctions.getHTML();
	}
	
	/**
	 * Decrements the last call number. Use this after an unsuccessful call
	 * in order to be repeat it on the next call of {@link #getNextInput()}.
	 */
	public void revertLastCall() {
		currentCallNum--;
	}
}
