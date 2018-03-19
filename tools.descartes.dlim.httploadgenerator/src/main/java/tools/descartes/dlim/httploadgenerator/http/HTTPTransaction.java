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

import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
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

	
	private static final String POST_SIGNAL = "[POST]";
	
	/** The constant logging instance. */
	private static final Logger LOG = Logger.getLogger(HTTPTransaction.class.getName());

	/**
	 * Processes the transaction of sending a GET request to a web server.
	 * @param generator The input generator to use.
	 * @return HTML Response of the web server.
	 */
	public String process(HTTPInputGenerator generator) {
		String url = generator.getNextInput().trim();
		String method = "GET";
		if (url.startsWith("[")) {
			if (url.startsWith(POST_SIGNAL)) {
				method = "POST";
			}
			url = url.replaceFirst("\\[.*\\]", "");
		}
		Request request = generator.initializeHTTPRequest(url, method);
		
		long startTime = System.currentTimeMillis();
		try {
			ContentResponse response = request.send();
			if (response.getStatus() >= 400) {
				generator.revertLastCall();
				LOG.log(Level.FINEST, "Received error response code: " + response.getStatus());
			} else {
				String responseBody = response.getContentAsString();
				ResultTracker.TRACKER.logResponseTime(System.currentTimeMillis() - startTime);
				
				//store result
				generator.resetHTMLFunctions(responseBody);
				return responseBody;
			}
		} catch (java.util.concurrent.ExecutionException e) {
			LOG.log(Level.SEVERE, "ExecutionException in call for URL: " + url + "; Cause: " + e.getCause().toString());
			generator.revertLastCall();
		} catch (CancellationException e) {
			LOG.log(Level.SEVERE, "CancellationException: " + url + "; " + e.getMessage());
			generator.revertLastCall();
		} catch (InterruptedException e) {
			LOG.log(Level.SEVERE, "InterruptedException: " + e.getMessage());
			generator.revertLastCall();
		} catch (TimeoutException e) {
			generator.revertLastCall();
//		} catch (IOException e) {
//			LOG.log(Level.SEVERE, "General IOException in call for URL: " + url + "; Cause: " + e.getCause().toString());
//			generator.revertLastCall();
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
