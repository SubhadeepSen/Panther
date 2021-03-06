package com.panther.core;

import static com.panther.util.PantherUtils.EMPTY_STRING;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.panther.config.ConfigLoader;
import com.panther.exception.PantherException;
import com.panther.model.PantherConfig;
import com.panther.model.PantherModel;
import com.panther.model.PantherRequest;
import com.panther.model.PantherResponse;
import com.panther.util.PantherUtils;

public class PantherEngine {

	private static final Logger LOGGER = LoggerFactory.getLogger(PantherEngine.class);
	private static final String DELETE = "DELETE";
	private static final String PUT = "PUT";
	private static final String POST = "POST";
	private static final String GET = "GET";

	public void execute(PantherModel pantherModel) throws UnsupportedEncodingException {
		invokeMethod(pantherModel.getPreExecution(), pantherModel);
		HttpClient httpClient = HttpClientBuilder.create().build();
		PantherRequest pantherRequest = null;
		HttpRequestBase baseRequest = null;
		pantherRequest = pantherModel.getRequest();
		switch (pantherRequest.getMethod()) {
		case GET:
			baseRequest = new HttpGet(pantherRequest.getUrl());
			break;
		case POST:
			baseRequest = new HttpPost(pantherRequest.getUrl());
			if (null != pantherRequest.getBody()) {
				HttpPost httpPost = (HttpPost) baseRequest;
				httpPost.setEntity(new StringEntity(pantherRequest.getBody().toString()));
				baseRequest = httpPost;
			}
			break;
		case PUT:
			baseRequest = new HttpPut(pantherRequest.getUrl());
			if (null != pantherRequest.getBody()) {
				HttpPut httpPut = (HttpPut) baseRequest;
				httpPut.setEntity(new StringEntity(pantherRequest.getBody().toString(),
						ContentType.getByMimeType(pantherRequest.getHeaders().get("Accept"))));
				baseRequest = httpPut;
			}
			break;
		case DELETE:
			baseRequest = new HttpDelete(pantherRequest.getUrl());
			break;
		default:
			LOGGER.error("Unsupported http method >> " + pantherRequest.getMethod());
			throw new PantherException("Unsupported http method: " + pantherRequest.getMethod());
		}

		// adding request headers
		if (pantherRequest.getHeaders() != null & !pantherRequest.getHeaders().isEmpty()) {
			for (Entry<String, String> headerEntrySet : pantherRequest.getHeaders().entrySet()) {
				if (null != headerEntrySet.getValue() && !EMPTY_STRING.equals(headerEntrySet.getValue())) {
					baseRequest.addHeader(headerEntrySet.getKey(), headerEntrySet.getValue());
				}
			}
		}

		PantherConfig pantherConfig = ConfigLoader.getConfig(null);

		// adding authentication header(s)
		if (null != pantherConfig.getSecureHeaders() && !pantherConfig.getSecureHeaders().isEmpty()) {
			for (Entry<String, String> headerEntrySet : ConfigLoader.getConfig(null).getSecureHeaders().entrySet()) {
				if (null != headerEntrySet.getValue() && !EMPTY_STRING.equals(headerEntrySet.getValue())) {
					baseRequest.addHeader(headerEntrySet.getKey(), headerEntrySet.getValue());
				}
			}
		}

		try {
			LOGGER.info("Executing case >> " + pantherModel.getDescription());
			long startTime = System.currentTimeMillis();
			HttpResponse httpResponse = httpClient.execute(baseRequest);
			long endTime = System.currentTimeMillis();
			pantherModel.getResponse().setResponseTime(String.valueOf(endTime - startTime));
			verifyResponse(httpResponse, pantherModel);

		} catch (IOException e) {
			LOGGER.error(e.getMessage());
			throw new PantherException(e.getMessage());
		} finally {
			invokeMethod(pantherModel.getPostExecution(), pantherModel);
		}
	}

	private void verifyResponse(HttpResponse httpResponse, PantherModel pantherModel)
			throws ParseException, IOException {
		PantherResponse pantherResponse = pantherModel.getResponse();

		// status code verification
		String actualStatusCode = String.valueOf(httpResponse.getStatusLine().getStatusCode());
		String expectedStatusCode = pantherResponse.getStatus();
		if (null != expectedStatusCode && !expectedStatusCode.equals(actualStatusCode)) {
			pantherModel.setCaseStatus(false);
			pantherModel.setCaseMessage("Status code mismatched: { actual: " + actualStatusCode + ", expected: "
					+ pantherResponse.getStatus() + " }");
			return;
		}

		// body verification
		if (null != httpResponse.getEntity()
				&& httpResponse.getFirstHeader("Content-Type").getValue().contains("application/json")) {
			String actualResponse = EntityUtils.toString(httpResponse.getEntity());
			pantherModel.setActualResponse(actualResponse);
			String expectedResponse = pantherResponse.getBody().toString();
			if (pantherModel.isFieldValidationEnable()) {
				if (!isValidBody(pantherResponse.getBody(), PantherUtils.convertToJsonNode(actualResponse))) {
					pantherModel.setCaseStatus(false);
					pantherModel.setCaseMessage(
							"body mismatched: { actual: " + actualResponse + ", expected: " + expectedResponse + " }");
					return;
				}
			} else {
				if (!PantherUtils.convertToJsonNode(actualResponse).equals(pantherResponse.getBody())) {
					pantherModel.setCaseStatus(false);
					pantherModel.setCaseMessage(
							"body mismatched: { actual: " + actualResponse + ", expected: " + expectedResponse + " }");
					return;
				}
			}
		}

		// header verification
		Map<String, String> extectedHeaders = pantherResponse.getHeaders();
		if (null != extectedHeaders && !extectedHeaders.isEmpty()) {
			Map<String, String> actualHeaders = new HashMap<String, String>();
			for (Header header : httpResponse.getAllHeaders()) {
				actualHeaders.put(header.getName(), header.getValue());
			}
			extectedHeaders.entrySet().forEach(e -> {
				if (!actualHeaders.containsKey(e.getKey()) || !actualHeaders.containsKey(e.getValue())) {
					pantherModel.setCaseStatus(false);
					pantherModel.setCaseMessage("header mismatched: " + e.getKey() + " : { actual: "
							+ actualHeaders.get(e.getKey()) + ", expected: " + e.getValue() + " }");
					return;
				}
			});
		}
		pantherModel.setCaseStatus(true);
		pantherModel.setCaseMessage(EMPTY_STRING);
	}

	// method to verify elements field by field #Recursive
	private static boolean isValidBody(JsonNode expectedResponseBody, JsonNode actualResponseBody) {
		if (expectedResponseBody == null && actualResponseBody == null) {
			return true;
		}
		if (expectedResponseBody == null || actualResponseBody == null) {
			return false;
		}
		String expectedTextValue = expectedResponseBody.asText();
		if (!EMPTY_STRING.equals(expectedTextValue)) {
			if (expectedTextValue.startsWith("$ignore")) {
				return true;
			} else if (expectedTextValue.startsWith("$contains")) {
				int i = expectedTextValue.indexOf('\'');
				int j = expectedTextValue.lastIndexOf('\'');
				expectedTextValue = expectedTextValue.substring(i + 1, j);
				if (actualResponseBody.asText().contains(expectedTextValue)) {
					return true;
				}
			} else if (expectedTextValue.equals(actualResponseBody.asText())) {
				return true;
			}
		}

		boolean isValid = false;
		if (expectedResponseBody instanceof ArrayNode) {
			Iterator<JsonNode> expResIt = expectedResponseBody.iterator();
			Iterator<JsonNode> actResIt = actualResponseBody.iterator();
			while (expResIt.hasNext()) {
				isValid = isValidBody(expResIt.next(), actResIt.next());
				if (!isValid) {
					return false;
				}
			}
		} else if (expectedResponseBody instanceof ObjectNode) {
			Iterator<Entry<String, JsonNode>> objFieldIt = expectedResponseBody.fields();
			Entry<String, JsonNode> node = null;
			while (objFieldIt.hasNext()) {
				node = objFieldIt.next();
				isValid = isValidBody(node.getValue(), actualResponseBody.get(node.getKey()));
				if (!isValid) {
					return false;
				}
			}
		}
		return isValid;
	}

	private void invokeMethod(String executionString, PantherModel pantherModel) {
		if (null != executionString && !EMPTY_STRING.equals(executionString)) {
			try {
				String[] tokens = executionString.split("::");
				Class<?> forName = Class.forName(tokens[0]);
				Object obj = forName.newInstance();
				Method method = forName.getMethod(tokens[1], PantherModel.class);
				method.invoke(obj, pantherModel);
			} catch (Exception e) {
				throw new PantherException(e.getMessage());
			}
		}
	}
}
