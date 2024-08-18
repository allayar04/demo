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
import java.util.HashMap;
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
	private static final ObjectMapper mapper = new ObjectMapper();
	private final AmazonDynamoDB client;
	private final String tableName;

	public Processor() {
		this.client = AmazonDynamoDBClientBuilder.standard()
				.withRegion(System.getenv("eu-central-1"))
				.build();
		this.tableName = "cmtr-d2f4ab85-Weather-test";
	}

	@Override
	public String handleRequest(Object input, Context context) {
		LambdaLogger logger = context.getLogger();
		try {
			String weatherData = GetOpenMeteo.getOpenMeteo();
			Map<String, AttributeValue> weatherEntry = transformWeatherJsonToMap(weatherData);
			logger.log("Weather Data: " + weatherEntry);
			saveToDatabase(weatherEntry);
			return "Weather forecast fetched and saved successfully";
		} catch (Exception e) {
			logger.log("Error: " + e.getMessage());
			return "Failed to fetch or save weather data: " + e.getMessage();
		}
	}

	private Map<String, AttributeValue> transformWeatherJsonToMap(String json) throws Exception {
		JsonNode root = mapper.readTree(json);
		Map<String, AttributeValue> weatherEntry = new HashMap<>();

		weatherEntry.put("id", new AttributeValue(UUID.randomUUID().toString()));
		weatherEntry.put("temperature", new AttributeValue().withN(
				root.path("current").path("temperature_2m").asText()));
		weatherEntry.put("windSpeed", new AttributeValue().withN(
				root.path("current").path("wind_speed_10m").asText()));
		weatherEntry.put("timestamp", new AttributeValue(
				root.path("current").path("time").asText()));

		return weatherEntry;
	}

	private void saveToDatabase(Map<String, AttributeValue> item) {
		PutItemRequest putItemRequest = new PutItemRequest()
				.withTableName(tableName)
				.withItem(item);
		client.putItem(putItemRequest);
	}
}
