package com.panther.core;

import static com.panther.util.PantherUtils.EMPTY_STRING;
import static com.panther.util.PantherUtils.NEW_LINE;
import static com.panther.util.PantherUtils.OBJECT_MAPPER;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.panther.config.ConfigLoader;
import com.panther.exception.PantherException;
import com.panther.model.PantherAnalytics;
import com.panther.model.PantherModel;
import com.panther.model.PantherResponse;
import com.panther.util.PantherUtils;;

public class PantherReport {

	private static final String PANTHER_REPORT = "panther-report";
	private static final String TARGET = "target";
	private static final String PANTHER_CONFIG = "panther-config.json";
	private static final Logger LOGGER = LoggerFactory.getLogger(PantherReport.class);
	private static final String JAR_URI_PREFIX = "jar:file:/";
	private static final String FOLDER_URI_PREFIX = "file:/";
	private static final String JAR_RESOURCE = "report";
	private static String REPORT_SRC;
	private static String REPORT_DEST;
	private String reportFile;
	private Map<String, PantherAnalytics> analytics;
	private int totalPassed = 0;
	private int totalFailed = 0;
	private int minResponseTime = Integer.MAX_VALUE;
	private int maxResponseTime = Integer.MIN_VALUE;
	private boolean isJarFile;
	private JarFile jarFile;
	private JarEntry reportBodyJarEntry;
	private JarEntry reportCaseJarEntry;
	private JarEntry reportDefJarEntry;
	private JarEntry reportRqstRspnJarEntry;

	public PantherReport() {
		String srcUrl = this.getClass().getClassLoader().getResource(JAR_RESOURCE).toString().replaceAll("%20", " ");
		if (srcUrl.startsWith(JAR_URI_PREFIX) && srcUrl.indexOf("!") != -1) {
			isJarFile = true;
			try {
				this.jarFile = new JarFile(srcUrl.substring(JAR_URI_PREFIX.length(), srcUrl.indexOf("!")));
			} catch (IOException e) {
				throw new PantherException(e.getMessage());
			}
			REPORT_SRC = JAR_RESOURCE;
		} else {
			REPORT_SRC = srcUrl.substring(FOLDER_URI_PREFIX.length());
		}
		String destUrl = this.getClass().getClassLoader().getResource(PANTHER_CONFIG).toString().replaceAll("%20", " ");
		REPORT_DEST = destUrl.substring(FOLDER_URI_PREFIX.length(), destUrl.indexOf(TARGET) + TARGET.length() + 1)
				+ PANTHER_REPORT;
	}

	public void generate(Map<String, List<PantherModel>> fileNameToPantherList) {
		createAnalytics(fileNameToPantherList);
		try {
			if (isJarFile) {
				copyStaticResources(jarFile);
			} else {
				copyStaticResources(new File(REPORT_SRC), new File(REPORT_DEST));
			}
			updatePieChartScript();
			this.reportFile = REPORT_DEST + "/panther-report-" + PantherUtils.dateTimeString() + ".html";
			Files.createFile(Paths.get(reportFile));
			copyReportDefinition();
			generateReport(fileNameToPantherList);
		} catch (IOException e) {
			LOGGER.error(e.getMessage());
			throw new PantherException(e.getMessage());
		}
	}

	private void createAnalytics(Map<String, List<PantherModel>> fileNameToPantherList) {
		analytics = new HashMap<String, PantherAnalytics>();
		PantherResponse pantherResponse = null;
		int passedCount;
		int failedCount;
		for (Entry<String, List<PantherModel>> entry : fileNameToPantherList.entrySet()) {
			passedCount = 0;
			failedCount = 0;
			for (PantherModel model : entry.getValue()) {
				pantherResponse = model.getResponse();
				if (model.caseStatus()) {
					passedCount++;
					totalPassed++;
				} else {
					failedCount++;
					totalFailed++;
				}
				if (Integer.parseInt(pantherResponse.getResponseTime()) < minResponseTime) {
					minResponseTime = Integer.parseInt(pantherResponse.getResponseTime());
				}
				if (Integer.parseInt(pantherResponse.getResponseTime()) > maxResponseTime) {
					maxResponseTime = Integer.parseInt(pantherResponse.getResponseTime());
				}
			}
			analytics.put(entry.getKey(), new PantherAnalytics(passedCount, failedCount));
		}
	}

	private void copyStaticResources(JarFile jarFile) throws IOException {
		Enumeration<JarEntry> entries = jarFile.entries();
		JarEntry entry = null;
		File dest = null;
		File parent = null;
		FileOutputStream out = null;
		InputStream in = null;
		int read = 0;
		byte[] buffer = null;
		while (entries.hasMoreElements()) {
			entry = entries.nextElement();
			if (entry.getName().startsWith(REPORT_SRC + "/rpt-body")) {
				reportBodyJarEntry = entry;
			} else if (entry.getName().startsWith(REPORT_SRC + "/rpt-case")) {
				reportCaseJarEntry = entry;
			} else if (entry.getName().startsWith(REPORT_SRC + "/rpt-def")) {
				reportDefJarEntry = entry;
			} else if (entry.getName().startsWith(REPORT_SRC + "/rpt-rqst-rspn")) {
				reportRqstRspnJarEntry = entry;
			} else if (entry.getName().startsWith(REPORT_SRC + "/") && !entry.isDirectory()
					&& !entry.getName().startsWith(REPORT_SRC + "/rpt-")) {
				dest = new File(REPORT_DEST + "/" + entry.getName().substring(REPORT_SRC.length() + 1));
				parent = dest.getParentFile();
				if (parent != null) {
					parent.mkdirs();
				}
				try {
					out = new FileOutputStream(dest);
					buffer = new byte[1024];
					in = jarFile.getInputStream(entry);
					while ((read = in.read(buffer)) != -1) {
						out.write(buffer, 0, read);
					}
				} catch (IOException e) {
					throw new PantherException(e.getMessage());
				} finally {
					if (null != in) {
						in.close();
					}
					if (null != out) {
						out.close();
					}
				}
			}
		}
	}

	private void copyStaticResources(File sourceFolder, File destinationFolder) throws IOException {
		if (sourceFolder.isDirectory()) {
			if (!destinationFolder.exists()) {
				destinationFolder.mkdir();
			}
			for (String file : sourceFolder.list()) {
				copyStaticResources(new File(sourceFolder, file), new File(destinationFolder, file));
			}
		} else if (!sourceFolder.getName().startsWith("rpt-")) {
			Files.copy(sourceFolder.toPath(), destinationFolder.toPath(), StandardCopyOption.REPLACE_EXISTING);
		}
	}

	private void updatePieChartScript() throws IOException {
		StringBuilder scriptData = new StringBuilder();
		StringBuilder sb = new StringBuilder();
		scriptData.append("[{value: ").append(totalPassed + ", ").append("color: \"#4caf50\", ")
				.append("label: \"Passed\"},").append("{value: ").append(totalFailed + ", ")
				.append("color: \"#f44336\", ").append("label: \"Failed\"}]");
		List<String> lines = Files.readAllLines(Paths.get(REPORT_DEST + "/js/panther.js"));
		for (String line : lines) {
			if (line.contains("<<analyticsData>>")) {
				line = line.replaceAll("<<analyticsData>>", scriptData.toString());
			}
			sb.append(line).append(NEW_LINE);
		}
		Files.write(Paths.get(REPORT_DEST + "/js/panther.js"), sb.toString().getBytes());
	}

	private void copyReportDefinition() throws IOException {
		if (isJarFile) {
			Files.copy(jarFile.getInputStream(reportDefJarEntry), Paths.get(reportFile),
					StandardCopyOption.REPLACE_EXISTING);
		} else {
			Files.copy(Paths.get(REPORT_SRC + "/rpt-def.txt"), Paths.get(reportFile),
					StandardCopyOption.REPLACE_EXISTING);
		}
	}

	private void generateReport(Map<String, List<PantherModel>> fileNameToPantherList) throws IOException {
		StringBuilder sb = new StringBuilder();
		List<String> lines = Files.readAllLines(Paths.get(reportFile));
		for (String line : lines) {
			if (line.contains("<<appName>>")) {
				sb.append(line.replaceAll("<<appName>>", ConfigLoader.getConfig(null).getReportName()));
			} else if (line.contains("<<totalCount>>")) {
				sb.append(line.replaceAll("<<totalCount>>", String.valueOf(totalPassed + totalFailed)));
			} else if (line.contains("<<passedCount>>")) {
				sb.append(line.replaceAll("<<passedCount>>", String.valueOf(totalPassed)));
			} else if (line.contains("<<failedCount>>")) {
				sb.append(line.replaceAll("<<failedCount>>", String.valueOf(totalFailed)));
			} else if (line.contains("<<minResponseTime>>")) {
				sb.append(line.replaceAll("<<minResponseTime>>", String.valueOf(minResponseTime)));
			} else if (line.contains("<<maxResponseTime>>")) {
				sb.append(line.replaceAll("<<maxResponseTime>>", String.valueOf(maxResponseTime)));
			} else if (line.contains("<<rpt-body>>")) {
				fileNameToPantherList.entrySet().forEach(entry -> {
					try {
						sb.append(line.replaceAll("<<rpt-body>>", parseAndLoadBody(entry)));
					} catch (IOException e) {
						LOGGER.error(e.getMessage());
						throw new PantherException(e.getMessage());
					}
				});
			} else {
				sb.append(line);
			}
		}
		Files.write(Paths.get(reportFile), sb.toString().getBytes());
	}

	private String parseAndLoadBody(Entry<String, List<PantherModel>> entry) throws IOException {
		StringBuilder result = new StringBuilder();
		List<String> lines = null;
		if (isJarFile) {
			lines = getLinesFromJarEntry(reportBodyJarEntry);
		} else {
			lines = Files.readAllLines(Paths.get(REPORT_SRC + "/rpt-body.txt"));
		}
		int passedCount = analytics.get(entry.getKey()).getPassedCaseCount();
		int totalCount = analytics.get(entry.getKey()).getTotalCaseCount();
		for (String line : lines) {
			if (line.contains("<<fileName>>")) {
				line = line.replaceAll("<<fileName>>", entry.getKey().replace(".json", EMPTY_STRING).replaceAll(" ", "-"));
			}
			if (line.contains("<<caseStatusColor>>")) {
				if (passedCount == totalCount) {
					line = line.replaceAll("<<caseStatusColor>>", "green-background");
				} else if (passedCount >= (totalCount / 2)) {
					line = line.replaceAll("<<caseStatusColor>>", "orange-background");
				} else {
					line = line.replaceAll("<<caseStatusColor>>", "red-background");
				}
			}
			if (line.contains("<<passedCount>>")) {
				line = line.replaceAll("<<passedCount>>", String.valueOf(passedCount));
			}
			if (line.contains("<<totalCount>>")) {
				line = line.replaceAll("<<totalCount>>", String.valueOf(totalCount));
			}
			if (line.contains("<<loadCase>>")) {
				line = line.replaceAll("<<loadCase>>", parseAndLoadCase(entry.getValue()));
			}
			result.append(line).append(NEW_LINE);
		}
		return result.toString();
	}

	private String parseAndLoadCase(List<PantherModel> pantherModels) throws IOException {
		StringBuilder result = new StringBuilder();
		List<String> lines = null;
		for (PantherModel model : pantherModels) {
			if (isJarFile) {
				lines = getLinesFromJarEntry(reportCaseJarEntry);
			} else {
				lines = Files.readAllLines(Paths.get(REPORT_SRC + "/rpt-case.txt"));
			}
			for (String line : lines) {
				if (line.contains("<<description>>")) {
					line = line.replaceAll("<<description>>", model.getDescription().replaceAll(" ", EMPTY_STRING));
				}
				if (line.contains("<<caseStatusColor>>")) {
					if (!model.caseStatus()) {
						line = line.replaceAll("<<caseStatusColor>>", "red-background");
					} else {
						line = line.replaceAll("<<caseStatusColor>>", "green-background");
					}
				}
				if (line.contains("<<caseStatus>>")) {
					if (!model.caseStatus()) {
						line = line.replaceAll("<<caseStatus>>", "Failed");
					} else {
						line = line.replaceAll("<<caseStatus>>", "Passed");
					}
				}
				if (line.contains("<<responseTime>>")) {
					line = line.replaceAll("<<responseTime>>", "(" + model.getResponse().getResponseTime() + " ms)");
				}
				if (line.contains("<<errorMessage>>")) {
					line = line.replaceAll("<<errorMessage>>", model.getCaseMessage());
				}
				if (line.contains("<<rpt-rqst-rspn>>")) {
					if (ConfigLoader.getConfig(null).isEnableReportLogging()) {
						line = line.replaceAll("<<rpt-rqst-rspn>>", parseAddRequestResponse(model));
					} else {
						line = line.replaceAll("<<rpt-rqst-rspn>>", EMPTY_STRING);
					}
				}
				result.append(line).append(NEW_LINE);
			}
		}
		return result.toString();
	}

	private String parseAddRequestResponse(PantherModel model) throws IOException {
		StringBuilder result = new StringBuilder();
		List<String> lines = null;
		if (isJarFile) {
			lines = getLinesFromJarEntry(reportRqstRspnJarEntry);
		} else {
			lines = Files.readAllLines(Paths.get(REPORT_SRC + "/rpt-rqst-rspn.txt"));
		}
		for (String line : lines) {
			if (line.contains("<<rqst-rspn>>")) {
				line = line.replaceAll("<<rqst-rspn>>", model.getDescription().replaceAll(" ", EMPTY_STRING) + "-rr");
			}
			if (line.contains("<<caseStatusColor>>")) {
				if (model.caseStatus()) {
					line = line.replaceAll("<<caseStatusColor>>", "green-background");
				} else {
					line = line.replaceAll("<<caseStatusColor>>", "red-background");
				}
			}
			if (line.contains("<<request-body>>")) {
				line = line.replaceAll("<<request-body>>", formatToHtmlJsonView(OBJECT_MAPPER.writeValueAsString(model.getRequest())));
			}
			if (line.contains("<<response-body-expected>>")) {
				String expectedBody = model.getResponse().getBody().toString();
				if(expectedBody.contains("$ignore")) {
					expectedBody = expectedBody.replace("$", "");
				}
				if(expectedBody.contains("$contains")) {
					expectedBody = expectedBody.replace("$", "");
				}
				line = line.replaceAll("<<response-body-expected>>", formatToHtmlJsonView(expectedBody));
			}
			if (line.contains("<<response-body-actual>>")) {
				line = line.replaceAll("<<response-body-actual>>", formatToHtmlJsonView(model.getActualResponse()));
			}
			result.append(line).append(NEW_LINE);
		}
		return result.toString();
	}

	private List<String> getLinesFromJarEntry(JarEntry jarEntry) throws IOException {
		return new BufferedReader(new InputStreamReader(jarFile.getInputStream(jarEntry), StandardCharsets.UTF_8))
				.lines().collect(Collectors.toList());
	}

	private String formatToHtmlJsonView(String jsonString) {
		return jsonString.replaceAll(",", ", <br>").replaceAll("\\{", "{<br>").replaceAll("\\}", "<br>}")
				.replaceAll("\\[", "[<br>").replaceAll("\\]", "<br>]");
	}
}
