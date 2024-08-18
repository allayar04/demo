package com.task09;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.api.open_meteo.GetOpenMeteo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.syndicate.deployment.annotations.environment.EnvironmentVariable;
import com.syndicate.deployment.annotations.environment.EnvironmentVariables;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.annotations.lambda.LambdaLayer;
import com.syndicate.deployment.annotations.lambda.LambdaUrlConfig;
import com.syndicate.deployment.annotations.resources.DependsOn;
import com.syndicate.deployment.model.ArtifactExtension;
import com.syndicate.deployment.model.DeploymentRuntime;
import com.syndicate.deployment.model.ResourceType;
import com.syndicate.deployment.model.RetentionSetting;

import com.syndicate.deployment.model.lambda.url.AuthType;
import com.syndicate.deployment.model.lambda.url.InvokeMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@LambdaHandler(
    lambdaName = "processor",
	roleName = "processor-role",
		layers = {"sdk-layer"},
		isPublishVersion = false,
		runtime = DeploymentRuntime.JAVA17,
		logsExpiration = RetentionSetting.SYNDICATE_ALIASES_SPECIFIED
)
@LambdaLayer(
		layerName = "sdk-layer",
		libraries = {"lib/open_meteo_api-1.0-SNAPSHOT.jar"},
		runtime = DeploymentRuntime.JAVA17,
		artifactExtension = ArtifactExtension.ZIP
)
@LambdaUrlConfig(
		authType = AuthType.NONE,
		invokeMode = InvokeMode.BUFFERED
)
@DependsOn(name = "Weather", resourceType = ResourceType.DYNAMODB_TABLE)
@EnvironmentVariables(value = {
		@EnvironmentVariable(key = "region", value = "${region}"),
		@EnvironmentVariable(key = "target_table", value = "${target_table}")
})
public class Processor implements RequestHandler<Object, String> {
	private final String tableName = "cmtr-d2f4ab85-Weather-test";
	private final AmazonDynamoDB dynamoDB = AmazonDynamoDBClientBuilder.standard()
			.withRegion("eu-central-1")
			.build();

	@Override
	public String handleRequest(Object input, Context context) {
		LambdaLogger logger = context.getLogger();

		try {
			// Fetch weather data from Open-Meteo API
			String weatherData = GetOpenMeteo.getOpenMeteo();

			// Parse the weather data
			ObjectMapper objectMapper = new ObjectMapper();
			JsonNode weatherNode = objectMapper.readTree(weatherData);

			// Create the forecast JSON object based on the required schema
			Map<String, AttributeValue> forecast = new HashMap<>();
			forecast.put("elevation", new AttributeValue().withN(weatherNode.get("elevation").asText()));
			forecast.put("generationtime_ms", new AttributeValue().withN(weatherNode.get("generationtime_ms").asText()));

			// Process hourly data
			JsonNode hourlyNode = weatherNode.get("hourly");
			List<AttributeValue> temperatureList = new ArrayList<>();
			for (JsonNode tempNode : hourlyNode.get("temperature_2m")) {
				temperatureList.add(new AttributeValue().withN(tempNode.asText()));
			}
			List<AttributeValue> timeList = new ArrayList<>();
			for (JsonNode timeNode : hourlyNode.get("time")) {
				timeList.add(new AttributeValue().withS(timeNode.asText()));
			}
			Map<String, AttributeValue> hourly = new HashMap<>();
			hourly.put("temperature_2m", new AttributeValue().withL(temperatureList));
			hourly.put("time", new AttributeValue().withL(timeList));
			forecast.put("hourly", new AttributeValue().withM(hourly));

			// Process hourly_units data
			JsonNode hourlyUnitsNode = weatherNode.get("hourly_units");
			Map<String, AttributeValue> hourlyUnits = new HashMap<>();
			hourlyUnits.put("temperature_2m", new AttributeValue(hourlyUnitsNode.get("temperature_2m").asText()));
			hourlyUnits.put("time", new AttributeValue(hourlyUnitsNode.get("time").asText()));
			forecast.put("hourly_units", new AttributeValue().withM(hourlyUnits));

			forecast.put("latitude", new AttributeValue().withN(weatherNode.get("latitude").asText()));
			forecast.put("longitude", new AttributeValue().withN(weatherNode.get("longitude").asText()));
			forecast.put("timezone", new AttributeValue(weatherNode.get("timezone").asText()));
			forecast.put("timezone_abbreviation", new AttributeValue(weatherNode.get("timezone_abbreviation").asText()));
			forecast.put("utc_offset_seconds", new AttributeValue().withN(weatherNode.get("utc_offset_seconds").asText()));

			// Prepare the item to be inserted into DynamoDB
			Map<String, AttributeValue> item = new HashMap<>();
			item.put("id", new AttributeValue(UUID.randomUUID().toString()));
			item.put("forecast", new AttributeValue().withM(forecast));

			// Create PutItemRequest
			PutItemRequest request = new PutItemRequest()
					.withTableName(tableName)
					.withItem(item);

			// Insert the item into DynamoDB
			dynamoDB.putItem(request);

			logger.log("Weather data inserted successfully into DynamoDB table: " + System.getenv("target_table"));

			return "Weather data inserted successfully.";
		} catch (Exception e) {
			logger.log("Error: " + e.getMessage());
			return "Failed to insert weather data.";
		}
	}
}
