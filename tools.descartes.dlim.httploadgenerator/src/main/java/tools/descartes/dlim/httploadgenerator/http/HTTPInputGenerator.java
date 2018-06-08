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
package tools.descartes.dlim.httploadgenerator.http;

import java.io.File;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Request;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.JsePlatform;

import tools.descartes.dlim.httploadgenerator.http.lua.HTMLFunctions;
import tools.descartes.dlim.httploadgenerator.http.lua.HTMLLuaFunctions.ExtractAllMatches;
import tools.descartes.dlim.httploadgenerator.http.lua.HTMLLuaFunctions.GetMatches;

/**
 * Stateful Generator for the next HTTP-GET or POST URL.
 * URLs are generated from the script passed in the constructor.
 * @author Joakim von Kistowski
 */
public class HTTPInputGenerator {

	private static final Logger LOG = Logger.getLogger(HTTPInputGenerator.class.getName());
	
	private static final String USER_AGENT = "Mozilla/5.0";
	
	private static final String LUA_CYCLE_INIT = "onCycle";
	private static final String LUA_CALL = "onCall";


	private final HttpClient httpClient;
	
	private int id;
	
	private int currentCallNum = 0;
	private String lastInput = "";
	private int timeout = 0;

	private HTMLFunctions htmlFunctions = new HTMLFunctions("");
	private Globals luaGlobals;
	/**
	 * Constructs a new HTTPInputGenerator using a Lua generation script.
	 * The Lua script must contain the onInit() and onCall(callnum) functions.
	 * onCall(callnum) must return the HTTP request for a specific call with number callnum.
	 * callnum begins at 1 (Lua convention) and increments for each call. It resets back to 1
	 * if onCall returns nil.
	 * @param id The input generator's id.
	 * @param scriptFile The url generator script.
	 * @param randomSeed Seed for Lua random function.
	 * @param timeout The http read timeout.
	 */
	public HTTPInputGenerator(int id, File scriptFile, int randomSeed, int timeout) {
		this.id = id;
		httpClient = new HttpClient();
		
		if (timeout > 0) {
			httpClient.setConnectTimeout(timeout);
			this.timeout = timeout;
		}
		try {
			httpClient.start();
		} catch (Exception e) {
			LOG.severe("Could not start HTTP client; Exception: " + e.getMessage());
		}
		
		if (scriptFile != null) {
			luaGlobals = JsePlatform.standardGlobals();
			LuaValue library = new LuaTable();
			library.set("getMatches", new GetMatches(htmlFunctions));
			library.set("extractMatches", new ExtractAllMatches(htmlFunctions));
			luaGlobals.set("html", library);
			luaGlobals.get("math").get("randomseed").call(LuaValue.valueOf(5));
			luaGlobals.get("dofile").call(LuaValue.valueOf(scriptFile.getAbsolutePath()));
		}
	}

	/**
	 * Builds a request using the HTTP client and current cookies.
	 * @return The http client's initialized request.
	 */
	public Request initializeHTTPRequest(String url, String method) {
	Request request;
		if (method.equalsIgnoreCase("POST")) {
			 request = httpClient.POST(url);
		} else {
			request = httpClient.newRequest(url);
		}
		request = request.header("User-Agent", USER_AGENT);
		if (timeout > 0) {
			request = request.timeout(timeout, TimeUnit.MILLISECONDS)
					.idleTimeout(timeout, TimeUnit.MILLISECONDS);
		}
		return request;
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
		if (httpClient != null && httpClient.getCookieStore() != null) {
			httpClient.getCookieStore().removeAll();
		}
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

	/**
	 * Get the timeout.
	 * @return The timeout in Milliseconds.
	 */
	public int getTimeout() {
		return timeout;
	}

	int getId() {
		return id;
	}

	/**
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
		return result;
	}

	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		HTTPInputGenerator other = (HTTPInputGenerator) obj;
		if (id != other.id)
			return false;
		return true;
	}

}
