package com.panther.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.panther.config.ConfigLoader;
import com.panther.exception.PantherException;
import com.panther.model.PantherModel;
import com.panther.model.PantherRequest;
import com.panther.model.PantherResponse;

public class TemplateResolver {

	public Map<String, List<PantherModel>> resolve(String templatePath) {

		if (null == templatePath || templatePath == "") {
			throw new PantherException("Invalid path > " + templatePath);
		}

		Map<String, List<PantherModel>> fileToTemplateMap = new HashMap<>();
		try {
			Files.list(Paths.get(templatePath)).forEach(c -> {
				String fileName = c.getFileName().toString();
				fileToTemplateMap.put(fileName, readFromTemplate(templatePath + "/" + fileName));
			});
		} catch (IOException e) {
			throw new PantherException(e.getMessage());
		}

		PantherRequest pantherRequest = null;
		PantherResponse pantherResponse = null;
		String bodyLocation = "";
		String fileName = "";
		for (Entry<String, List<PantherModel>> entry : fileToTemplateMap.entrySet()) {
			for (PantherModel pantherModel : entry.getValue()) {
				pantherRequest = pantherModel.getRequest();
				if (pantherRequest.getBody() != null) {
					bodyLocation = pantherRequest.getBody().textValue();
					if (bodyLocation != null && bodyLocation.startsWith("$load")) {
						bodyLocation = bodyLocation.replace("load", "").replace("$", "");
						fileName = bodyLocation.substring(1, bodyLocation.length() - 1);
						pantherRequest.setBody(loadAndGetBody(fileName));
					}
				}

				pantherResponse = pantherModel.getResponse();
				if (pantherResponse.getBody() != null) {
					bodyLocation = pantherResponse.getBody().textValue();
					if (bodyLocation != null && bodyLocation.startsWith("$load")) {
						bodyLocation = bodyLocation.replace("load", "").replace("$", "");
						fileName = bodyLocation.substring(1, bodyLocation.length() - 1);
						pantherResponse.setBody(loadAndGetBody(fileName));
					}
				}

				if (null != pantherRequest.getPathParams()) {
					pantherRequest.getPathParams().entrySet().forEach(e -> {
						String url = pantherModel.getRequest().getUrl();
						if (url.contains("{" + e.getKey() + "}") && null != e.getValue()) {
							url = url.replace("{" + e.getKey() + "}", e.getValue());
							pantherModel.getRequest().setUrl(url);
						}
					});
				}
				if (null != pantherRequest.getQueryParams()) {
					pantherRequest.getQueryParams().entrySet().forEach(e -> {
						String url = pantherModel.getRequest().getUrl();
						if (url.contains("{" + e.getKey() + "}") && null != e.getValue()) {
							url = url.replace("{" + e.getKey() + "}", e.getValue());
							pantherModel.getRequest().setUrl(url);
						}
					});
				}
			}
		}

		return fileToTemplateMap;
	}

	private List<PantherModel> readFromTemplate(String pathOfRequestTemplate) {
		try {
			return new ObjectMapper().readValue(Files.readAllBytes(Paths.get(pathOfRequestTemplate)),
					new TypeReference<List<PantherModel>>() {
					});
		} catch (IOException e) {
			throw new PantherException(e.getMessage());
		}
	}

	private JsonNode loadAndGetBody(String fileName) {
		String payloadLocation = ConfigLoader.getConfig(null).getPayloadLocation();
		if (null == payloadLocation || "".equals(payloadLocation)) {
			throw new PantherException("Invalid payload location > " + payloadLocation);
		}
		String payloadPath = payloadLocation + "/" + fileName;
		try {
			return new ObjectMapper().readTree(Files.readAllBytes(Paths.get(payloadPath)));
		} catch (IOException e) {
			throw new PantherException(e.getMessage());
		}
	}
}
