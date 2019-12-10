package com.panther.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.panther.auth.Authentication;
import com.panther.model.PantherConfig;

public class ConfigLoader {

	private static PantherConfig pantherConfig;

	public static PantherConfig getConfig(Authentication authentication) {
		if (null == pantherConfig) {
			try {
				pantherConfig = new ObjectMapper().readValue(
						Files.readAllBytes(Paths.get("src/test/resources/panther-config.json")),
						new TypeReference<PantherConfig>() {
						});
				pantherConfig.setCredHeaders(authentication.headers());
				return pantherConfig;
			} catch (IOException e) {
				System.err.println("handle exception here.......");
			}
		}
		return pantherConfig;
	}
}
