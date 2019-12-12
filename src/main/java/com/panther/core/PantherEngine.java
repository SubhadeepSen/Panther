package com.panther.core;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.panther.config.ConfigLoader;
import com.panther.exception.PantherException;
import com.panther.model.PantherModel;
import com.panther.model.PantherRequest;
import com.panther.model.PantherResponse;

public class PantherEngine {

	public void execute(PantherModel pantherModel) throws UnsupportedEncodingException {
		HttpClient httpClient = HttpClientBuilder.create().build();
		PantherRequest pantherRequest = null;
		HttpRequestBase baseRequest = null;
		pantherRequest = pantherModel.getRequest();
		switch (pantherRequest.getMethod()) {
		case "GET":
			baseRequest = new HttpGet(pantherRequest.getUrl());
			break;
		case "POST":
			baseRequest = new HttpPost(pantherRequest.getUrl());
			if (null != pantherRequest.getBody()) {
				HttpPost httpPost = (HttpPost) baseRequest;
				httpPost.setEntity(new StringEntity(pantherRequest.getBody().toString()));
				baseRequest = httpPost;
			}
			break;
		case "PUT":
			baseRequest = new HttpPut(pantherRequest.getUrl());
			if (null != pantherRequest.getBody()) {
				HttpPut httpPut = (HttpPut) baseRequest;
				httpPut.setEntity(new StringEntity(pantherRequest.getBody().toString(),
						ContentType.getByMimeType(pantherRequest.getHeaders().get("Accept"))));
				baseRequest = httpPut;
			}
			break;
		case "DELETE":
			baseRequest = new HttpDelete(pantherRequest.getUrl());
			break;
		default:
			throw new PantherException("Unsupported http method: " + pantherRequest.getMethod());
		}

		for (Entry<String, String> headerEntrySet : pantherRequest.getHeaders().entrySet()) {
			if (null != headerEntrySet.getValue() && !"".equals(headerEntrySet.getValue())) {
				baseRequest.addHeader(headerEntrySet.getKey(), headerEntrySet.getValue());
			}
		}
		for (Entry<String, String> entry : ConfigLoader.getConfig(null).getSecureHeaders().entrySet()) {
			baseRequest.addHeader(entry.getKey(), entry.getValue());
		}

		try {
			System.out.println("Executing: " + pantherModel.getDescription() + " at " + pantherModel.getRequest().getUrl());
			long startTime = System.currentTimeMillis();
			HttpResponse httpResponse = httpClient.execute(baseRequest);
			verifyResponse(httpResponse, pantherModel);
			long endTime = System.currentTimeMillis();
			pantherModel.getResponse().setResponseTime(String.valueOf(endTime - startTime));
		} catch (IOException e) {
			throw new PantherException(e.getMessage());
		}
		System.out.println();
	}

	private void verifyResponse(HttpResponse httpResponse, PantherModel pantherModel)
			throws ParseException, IOException {
		PantherResponse pantherResponse = pantherModel.getResponse();

		// status code verification
		String statusCode = String.valueOf(httpResponse.getStatusLine().getStatusCode());
		if (!pantherResponse.getStatus().equals(statusCode)) {
			pantherModel.setCaseStatus(false);
			pantherModel.setCaseMessage("Status code mismatched: { actual: " + statusCode + ", expected: "
					+ pantherResponse.getStatus() + " }");
			return;
		}

		// body verification
		if (null != httpResponse.getEntity()
				&& httpResponse.getFirstHeader("Content-Type").getValue().contains("application/json")) {
			String actualResponse = EntityUtils.toString(httpResponse.getEntity());
			String expectedResponse = pantherResponse.getBody().toString();
			if (!new ObjectMapper().readTree(actualResponse).equals(pantherResponse.getBody())) {
				pantherModel.setCaseStatus(false);
				pantherModel.setCaseMessage("body mismatched: { actual: " + actualResponse + ", expected: " + expectedResponse + " }");
				return;
			}
		}

		// header verification
		if (null != pantherResponse.getHeaders()) {
			Map<String, String> actualHeaders = new HashMap<String, String>();
			for (Header header : httpResponse.getAllHeaders()) {
				actualHeaders.put(header.getName(), header.getValue());
			}
			pantherResponse.getHeaders().entrySet().forEach(e -> {
				if (!actualHeaders.containsKey(e.getKey()) || !actualHeaders.containsKey(e.getValue())) {
					pantherModel.setCaseStatus(false);
					pantherModel.setCaseMessage("header mismatched: " + e.getKey() + " : { actual: "
							+ actualHeaders.get(e.getKey()) + ", expected: " + e.getValue() + " }");
					return;
				}
			});
		}
		pantherModel.setCaseStatus(true);
		pantherModel.setCaseMessage("");
	}
}
