package com.panther.core;

import static com.panther.util.PantherUtils.EMPTY_STRING;
import static com.panther.util.PantherUtils.NEW_LINE;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.panther.config.ConfigLoader;
import com.panther.exception.PantherException;
import com.panther.model.PantherAnalytics;
import com.panther.model.PantherModel;
import com.panther.model.PantherResponse;
import com.panther.util.PantherUtils;;

public class PantherReport {

	private static final Logger LOGGER = LoggerFactory.getLogger(PantherReport.class);
	private static final String REPORT_SRC = "./src/main/java/com/panther/report";
	private static final String REPORT_DEST = "./target/panther-report";
	private String reportFile;
	private Map<String, PantherAnalytics> analytics;
	private int totalPassed = 0;
	private int totalFailed = 0;
	private int minResponseTime = Integer.MAX_VALUE;
	private int maxResponseTime = Integer.MIN_VALUE;

	public void generate(Map<String, List<PantherModel>> fileNameToPantherList) {
		createAnalytics(fileNameToPantherList);
		try {
			copyResources(new File(REPORT_SRC), new File(REPORT_DEST));
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

	private void copyResources(File sourceFolder, File destinationFolder) throws IOException {
		if (sourceFolder.isDirectory()) {
			if (!destinationFolder.exists()) {
				destinationFolder.mkdir();
			}
			for (String file : sourceFolder.list()) {
				copyResources(new File(sourceFolder, file), new File(destinationFolder, file));
			}
		} else if (!sourceFolder.getName().startsWith("rpt-")) {
			Files.copy(sourceFolder.toPath(), destinationFolder.toPath(), StandardCopyOption.REPLACE_EXISTING);
		}
	}

	private void updatePieChartScript() throws IOException {
		StringBuilder scriptData = new StringBuilder();
		StringBuilder sb = new StringBuilder();
		scriptData.append("[{value: ").append(totalPassed).append(", color: \"#4caf50\"},").append("{value: ")
				.append(totalFailed).append(", color: \"#f44336\"}]");
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
		Files.copy(Paths.get(REPORT_SRC + "/rpt-def.txt"), Paths.get(reportFile), StandardCopyOption.REPLACE_EXISTING);
	}

	private void generateReport(Map<String, List<PantherModel>> fileNameToPantherList) throws IOException {
		StringBuilder sb = new StringBuilder();
		List<String> defLines = Files.readAllLines(Paths.get(reportFile));
		for (String defLine : defLines) {
			if (defLine.contains("<<appName>>")) {
				sb.append(defLine.replaceAll("<<appName>>", ConfigLoader.getConfig(null).getReportName()));
			} else if (defLine.contains("<<totalCount>>")) {
				sb.append(defLine.replaceAll("<<totalCount>>", String.valueOf(totalPassed + totalFailed)));
			} else if (defLine.contains("<<passedCount>>")) {
				sb.append(defLine.replaceAll("<<passedCount>>", String.valueOf(totalPassed)));
			} else if (defLine.contains("<<failedCount>>")) {
				sb.append(defLine.replaceAll("<<failedCount>>", String.valueOf(totalFailed)));
			} else if (defLine.contains("<<minResponseTime>>")) {
				sb.append(defLine.replaceAll("<<minResponseTime>>", String.valueOf(minResponseTime)));
			} else if (defLine.contains("<<maxResponseTime>>")) {
				sb.append(defLine.replaceAll("<<maxResponseTime>>", String.valueOf(maxResponseTime)));
			} else if (defLine.contains("<<rpt-body>>")) {
				fileNameToPantherList.entrySet().forEach(entry -> {
					try {
						sb.append(defLine.replaceAll("<<rpt-body>>", parseAndLoadBody(entry)));
					} catch (IOException e) {
						LOGGER.error(e.getMessage());
						throw new PantherException(e.getMessage());
					}
				});
			} else {
				sb.append(defLine);
			}
		}
		Files.write(Paths.get(reportFile), sb.toString().getBytes());
	}

	private String parseAndLoadBody(Entry<String, List<PantherModel>> entry) throws IOException {
		StringBuilder result = new StringBuilder();
		List<String> bodyLines = Files.readAllLines(Paths.get(REPORT_SRC + "/rpt-body.txt"));
		int passedCount = analytics.get(entry.getKey()).getPassedCaseCount();
		int totalCount = analytics.get(entry.getKey()).getTotalCaseCount();
		for (String line : bodyLines) {
			if (line.contains("<<fileName>>")) {
				line = line.replaceAll("<<fileName>>", entry.getKey().replaceAll(".json", EMPTY_STRING));
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
		List<String> caseLines = null;
		for (PantherModel model : pantherModels) {
			caseLines = Files.readAllLines(Paths.get(REPORT_SRC + "/rpt-case.txt"));
			for (String line : caseLines) {
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
						line = line.replaceAll("<<rpt-rqst-rspn>>", addRequestResponse(model));
					} else {
						line = line.replaceAll("<<rpt-rqst-rspn>>", EMPTY_STRING);
					}
				}
				result.append(line).append(NEW_LINE);
			}
		}
		return result.toString();
	}

	private String addRequestResponse(PantherModel model) throws IOException {
		StringBuilder result = new StringBuilder();
		List<String> caseLines = null;
		caseLines = Files.readAllLines(Paths.get(REPORT_SRC + "/rpt-rqst-rspn.txt"));
		for (String line : caseLines) {
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
				line = line.replaceAll("<<request-body>>", new ObjectMapper().writeValueAsString(model.getRequest()));
			}
			if (line.contains("<<response-body>>")) {
				line = line.replaceAll("<<response-body>>", model.getActualResponse());
			}
			result.append(line).append(NEW_LINE);
		}
		return result.toString();
	}
}
