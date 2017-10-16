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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.CookieHandler;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import tools.descartes.dlim.httploadgenerator.runner.TransactionQueueSingleton;
import tools.descartes.dlim.httploadgenerator.runner.ResultTracker;
import tools.descartes.dlim.httploadgenerator.runner.Transaction;

/**
 * HTTP transaction sends HTML requests to a HTTP web server based on a LUA script.
 * 
 * @author Joakim von Kistowski, Maximilian Deffner
 *
 */
public class HTTPTransaction extends Transaction {

	private static final String USER_AGENT = "Mozilla/5.0";
	private static final String POST_SIGNAL = "[POST]";

	/** The constant logging instance. */
	private static final Logger LOG = Logger.getLogger(HTTPTransaction.class.getName());

	/**
	 * Processes the transaction of sending a GET request to a web server.
	 * @param generator The input generator to use.
	 * @return HTML Response of the web server.
	 */
	public String process(HTTPInputGenerator generator) {
		HttpURLConnection con = null;
		BufferedReader in = null;
		CookieHandler.setDefault(generator.getCookieManager());
		URL obj;
		String url = generator.getNextInput().trim();
		try {
			String method = "GET";
			if (url.startsWith("[")) {
				if (url.startsWith(POST_SIGNAL)) {
					method = "POST";
				}
				url = url.replaceFirst("\\[.*\\]", "");
			}
			obj = new URL(url);
			con = (HttpURLConnection) obj.openConnection();

			if (generator.getTimeout() > 0) {
				con.setReadTimeout(generator.getTimeout());
			}
			con.setRequestMethod(method);
			con.setRequestProperty("User-Agent", USER_AGENT);
			
			long startTime = System.currentTimeMillis();
			if (con.getResponseCode() >= 400) {
				generator.revertLastCall();
				LOG.log(Level.FINEST, "Received error response code: " + con.getResponseCode());
			} else {
				in = new BufferedReader(
						new InputStreamReader(con.getInputStream()));
				String inputLine;
				StringBuffer response = new StringBuffer();

				while ((inputLine = in.readLine()) != null) {
					response.append(inputLine + "\n");
				}
				ResultTracker.TRACKER.logResponseTime(System.currentTimeMillis() - startTime);
				
				//store result
				String html = response.toString();
				generator.resetHTMLFunctions(html);
				return response.toString();
			}

		} catch (MalformedURLException e) {
			LOG.log(Level.SEVERE, "Malformed URL: " + url);
			generator.revertLastCall();
		} catch (ProtocolException e) {
			LOG.log(Level.SEVERE, "ProtocolException: " + e.getMessage());
			generator.revertLastCall();
		} catch (java.net.SocketTimeoutException e) {
			generator.revertLastCall();
		} catch (IOException e) {
			LOG.log(Level.SEVERE, "General IO Exception Occured with Input @ " + url + ": " + e.getMessage());
			generator.revertLastCall();
		} finally {
			try {
				if (in != null) {
					in.close();
				}
			} catch (IOException e) {
				LOG.log(Level.SEVERE, "General IO Exception closing message reader.");
			}
			if (con != null) {
				con.disconnect();
			}
		}
		return null;
	}

	@Override
	public void run() {
		HTTPInputGenerator generator = HTTPInputGeneratorPool.getPool().takeFromPool();
		String response = this.process(generator);
		HTTPInputGeneratorPool.getPool().releaseBackToPool(generator);
		if (response == null) {
			ResultTracker.TRACKER.incrementInvalidTransctionCount();
		}
		TransactionQueueSingleton transactionQueue = TransactionQueueSingleton.getInstance();
		transactionQueue.addQueueElement(this);
	}
}
