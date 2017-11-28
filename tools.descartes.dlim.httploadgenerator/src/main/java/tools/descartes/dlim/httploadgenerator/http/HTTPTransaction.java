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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.util.logging.Level;
import java.util.logging.Logger;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
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

	private static final MediaType MEDIATYPE_FORM_URLENCODED = MediaType.parse("application/x-www-form-urlencoded");
	
	/** The constant logging instance. */
	private static final Logger LOG = Logger.getLogger(HTTPTransaction.class.getName());

	/**
	 * Processes the transaction of sending a GET request to a web server.
	 * @param generator The input generator to use.
	 * @return HTML Response of the web server.
	 */
	public String process(HTTPInputGenerator generator) {
		String url = generator.getNextInput().trim();
		try {
			String method = "GET";
			if (url.startsWith("[")) {
				if (url.startsWith(POST_SIGNAL)) {
					method = "POST";
				}
				url = url.replaceFirst("\\[.*\\]", "");
			}
			Request request;
			if (method.equals("POST")) {
				String[] query = url.split("\\?");
				String formData = "";
				if (query.length > 1) {
					formData = query[1];
				}
				request = new Request.Builder().url(url).header("User-Agent", USER_AGENT)
						.post(RequestBody.create(MEDIATYPE_FORM_URLENCODED, formData)).build();
			} else {
				request = new Request.Builder().url(url).header("User-Agent", USER_AGENT).get().build();
			}
			
			long startTime = System.currentTimeMillis();
			try (Response httpResponse = generator.getHttpClient().newCall(request).execute()) {
				if (httpResponse.code() >= 400) {
					generator.revertLastCall();
					LOG.log(Level.FINEST, "Received error response code: " + httpResponse.code());
				} else {
					String responseBody = httpResponse.body().string();
					ResultTracker.TRACKER.logResponseTime(System.currentTimeMillis() - startTime);
					
					//store result
					generator.resetHTMLFunctions(responseBody);
					return responseBody;
				}
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
