package com.panther.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalTime;
import java.util.List;

import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.panther.config.ConfigLoader;
import com.panther.exception.PantherException;
import com.panther.model.PantherModel;

public class PantherUtils {

	private static final Logger LOGGER = LoggerFactory.getLogger(PantherUtils.class);

	public static final String EMPTY_STRING = "";
	public static final String NEW_LINE = "\n";
	public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private PantherUtils() {
	}

	public static JsonNode convertToJsonNode(String content) {
		try {
			return OBJECT_MAPPER.readTree(content);
		} catch (IOException e) {
			LOGGER.error(e.getMessage());
			throw new PantherException(e.getMessage());
		}
	}

	public static String convertToJsonString(Object obj) {
		try {
			return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
		} catch (JsonProcessingException e) {
			LOGGER.error(e.getMessage());
			throw new PantherException(e.getMessage());
		}
	}

	public static ObjectNode createObjectNode() {
		return OBJECT_MAPPER.createObjectNode();
	}

	public static ArrayNode createArrayNode() {
		return OBJECT_MAPPER.createArrayNode();
	}

	public static JsonNode loadAndGetBody(String fileName) {
		String payloadLocation = ConfigLoader.getConfig(null).getPayloadLocation();
		if (null == payloadLocation || EMPTY_STRING.equals(payloadLocation)) {
			LOGGER.error("Invalid payload location >>> " + payloadLocation);
			throw new PantherException("Invalid payload location.");
		}
		try {
			return OBJECT_MAPPER.readTree(Files.readAllBytes(Paths.get(payloadLocation + "/" + fileName)));
		} catch (IOException e) {
			LOGGER.error(e.getMessage());
			throw new PantherException(e.getMessage());
		}
	}

	public static List<PantherModel> readCaseTemplate(String caseTemplate) {
		try {
			return OBJECT_MAPPER.readValue(Files.readAllBytes(Paths.get(caseTemplate)),
					new TypeReference<List<PantherModel>>() {
					});
		} catch (IOException e) {
			LOGGER.error(e.getMessage());
			throw new PantherException(e.getMessage());
		}
	}

	public static String dateTimeString() {
		String date = LocalDate.now().toString().replaceAll("-", EMPTY_STRING);
		String time = LocalTime.now().toString().split("\\.")[0].replaceAll(":", EMPTY_STRING);
		return date + time;
	}

	public static boolean isNotValidString(String content) {
		if (null == content || EMPTY_STRING.equals(content)) {
			return true;
		}
		return false;
	}
}
