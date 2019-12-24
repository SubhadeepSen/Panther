# Panther - An API Test Automation Framework

This automation test framework has been made as simple as possible to automate the REST API testing. You just need to add the panther dependency in your project. You can write the feature files in json format or you can generate the feature files by using the framework. If you decide to generate the feature files then you will have to provide the location of swagger document to the framework and the location could be http(s) url or local system in panther-config file. The panther-config.json is a configuration file to the framework which has very minimum number of parameters and gives you better control. This file should be present under src/test/resource. 

#### panther-config.json

```
{
	"apiDocsLocation": [String] "location of your swagger document, http(s) or local",
	"wantToParse": [Boolean] true = if you want to generate the feature files otherwise false,
	"testCasesLocation": [String] "location of your feature files",
	"apiScheme": "http or https",
	"payloadLocation": [String] "location of your payloads",
	"enableReportLogging": [Boolean] true = if you want to log requests and responsesin the generated report,
	"reportName": [String] "title of your report"
}
```

## Prerequisites

- JDK 1.8
- Maven or Gradle

## Getting Started

Add the panther dependency in your project.
```
<groupId>com</groupId>
<artifactId>panther</artifactId>
<version>1.0.0</version>
```
Create __panther-config.json__ under __src/test/resource__ and add the following parameters,
```
{
	"apiDocsLocation": "http://localhost:8085/message-queue-manager/api/v2/api-docs",
	"wantToParse": false,
	"testCasesLocation": "src/test/resources/cases",
	"apiScheme": "https",
	"payloadLocation": "src/test/resources/payloads",
	"enableReportLogging": true,
	"reportName": "Message Queue API"
}
```
Create a test class and annotate the class with __@RunWith(PantherRunner.class)__

If your API requires authentication, then create a method annotated with __@Auth__ which returns Authentication interface. You can also implement the Authentication interface.

Below is an example for creating the Runner class.
```
@RunWith(PantherRunner.class)
public class MQManagerApiTestRunner {

	@Auth
	public Authentication authHeader() {
		return new BasicAuthentication(username, password);
	}
}
```
Now you can run the Runner class with JUnit.

__Every feature file is a json array with scenarios.__ Below is an example of a feature file.

Explanation:
```
{
  "description" : "unique name of the scenario",
  "fieldValidationEnable" : true = validates response body field by field and useful when you want to use $ignore or $contains, false = validates response as an object,
  "request" : {
    "url" : "api endpoint url",
    "method" : "HTTP Method",
    "body" : {},
    "pathParams" : {},
    "queryParams" : {},
    "headers" : {}
  },
  "response" : {
    "headers" : {},
    "status" : "200",
    "body" : {}
  }
}
```

- when fieldValidationEnable = true
- __$ignore__ => does not validate that field
- __$contains('String')__ => check whether the field contains the given string or not

- __$load('fileName')__ => use to load request or response from different json file

```
{
	"description": "Publish Message To Queue",
	"fieldValidationEnable": true,
	"request": {
		"url": "http://localhost:8085/message-queue-manager/api/message",
		"method": "POST",
		"body": "$load('publish-message.json')",
		"headers": {
			"Authorization": "",
			"Accept": "application/json",
			"Content-Type": "application/json"
		}
	},
	"response": {
		"status": "200",
		"body": {
			"queueName": "$contains('test')",
			"messageId": "$ignore 1",
			"message": "this is a test message"
		}
	}
}
```

## Generated Test Report

