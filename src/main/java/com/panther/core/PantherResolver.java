package com.panther.core;

import static com.panther.util.PantherUtils.EMPTY_STRING;
import static com.panther.util.PantherUtils.isNotValidString;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.panther.exception.PantherException;
import com.panther.model.PantherModel;
import com.panther.model.PantherRequest;
import com.panther.model.PantherResponse;
import com.panther.util.PantherUtils;

public class PantherResolver {

	private static final Logger LOGGER = LoggerFactory.getLogger(PantherResolver.class);

	public Map<String, List<PantherModel>> resolveCaseTemplate(String caseTemplatePath) {
		if (isNotValidString(caseTemplatePath)) {
			LOGGER.error("Invalid location of case template(s) >> " + caseTemplatePath);
			throw new PantherException("Invalid location of case template(s).");
		}

		Map<String, List<PantherModel>> fileToPantherModels = new HashMap<>();
		try {
			Files.list(Paths.get(caseTemplatePath)).forEach(c -> {
				String fileName = c.getFileName().toString();
				fileToPantherModels.put(fileName, PantherUtils.readCaseTemplate(caseTemplatePath + "/" + fileName));
			});
		} catch (IOException e) {
			LOGGER.error(e.getMessage());
			throw new PantherException(e.getMessage());
		}

		PantherRequest pantherRequest = null;
		PantherResponse pantherResponse = null;
		String bodyLocation = EMPTY_STRING;
		String fileName = EMPTY_STRING;
		for (Entry<String, List<PantherModel>> entry : fileToPantherModels.entrySet()) {
			for (PantherModel pantherModel : entry.getValue()) {
				pantherRequest = pantherModel.getRequest();
				if (pantherRequest.getBody() != null) {
					bodyLocation = pantherRequest.getBody().textValue();
					if (bodyLocation != null && bodyLocation.startsWith("$load")) {
						bodyLocation = bodyLocation.replace("load", EMPTY_STRING).replace("$", EMPTY_STRING);
						fileName = bodyLocation.substring(1, bodyLocation.length() - 1);
						pantherRequest.setBody(PantherUtils.loadAndGetBody(fileName));
					}
				}

				pantherResponse = pantherModel.getResponse();
				if (pantherResponse.getBody() != null) {
					bodyLocation = pantherResponse.getBody().textValue();
					if (bodyLocation != null && bodyLocation.startsWith("$load")) {
						bodyLocation = bodyLocation.replace("load", EMPTY_STRING).replace("$", EMPTY_STRING);
						fileName = bodyLocation.substring(1, bodyLocation.length() - 1);
						pantherResponse.setBody(PantherUtils.loadAndGetBody(fileName));
					}
				}

				if (null != pantherRequest.getPathParams() && !pantherRequest.getPathParams().isEmpty()) {
					pantherRequest.getPathParams().entrySet().forEach(e -> {
						String url = pantherModel.getRequest().getUrl();
						if (url.contains("{" + e.getKey() + "}") && null != e.getValue()) {
							url = url.replace("{" + e.getKey() + "}", e.getValue());
							pantherModel.getRequest().setUrl(url);
						}
					});
				}
				if (null != pantherRequest.getQueryParams() && !pantherRequest.getQueryParams().isEmpty()) {
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
		return fileToPantherModels;
	}
}
