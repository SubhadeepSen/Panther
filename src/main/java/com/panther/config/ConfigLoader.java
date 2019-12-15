package com.panther.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.panther.auth.Authentication;
import com.panther.exception.PantherException;
import com.panther.model.PantherConfig;

public class ConfigLoader {
	private static final Logger LOGGER = LoggerFactory.getLogger(ConfigLoader.class);
	private static final String PANTHER_CONFIG_JSON = "src/test/resources/panther-config.json";
	private static PantherConfig pantherConfig;

	public static PantherConfig getConfig(Authentication authentication) {
		if (null == pantherConfig) {
			LOGGER.info("Loading panther configuration file...");
			try {
				pantherConfig = new ObjectMapper().readValue(Files.readAllBytes(Paths.get(PANTHER_CONFIG_JSON)),
						new TypeReference<PantherConfig>() {
						});
				if (null != authentication) {
					if (!pantherConfig.wantToParse()) {
						LOGGER.info("Adding authentication headers...");
					}
					pantherConfig.setSecureHeaders(authentication.headers());
				}
				return pantherConfig;
			} catch (IOException e) {
				LOGGER.error("Unable to load panther-config.json, make sure you have panther-config.json file in src/test/resources");
				throw new PantherException("Unable to load panther-config.json...");
			}
		}
		return pantherConfig;
	}
}
