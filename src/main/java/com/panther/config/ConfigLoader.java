package com.panther.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.panther.auth.Authentication;
import com.panther.exception.PantherException;
import com.panther.model.PantherConfig;

public class ConfigLoader {
	private static final String PANTHER_CONFIG_JSON = "src/test/resources/panther-config.json";
	private static PantherConfig pantherConfig;

	public static PantherConfig getConfig(Authentication authentication) {
		if (null == pantherConfig) {
			try {
				pantherConfig = new ObjectMapper().readValue(Files.readAllBytes(Paths.get(PANTHER_CONFIG_JSON)),
						new TypeReference<PantherConfig>() {
						});
				pantherConfig.setSecureHeaders(authentication.headers());
				return pantherConfig;
			} catch (IOException e) {
				throw new PantherException("Unable to load panther-config.json...");
			}
		}
		return pantherConfig;
	}
}
