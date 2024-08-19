package com.task09;

import static com.api.open_meteo.GetOpenMeteo.getOpenMeteo;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.syndicate.deployment.annotations.environment.EnvironmentVariable;
import com.syndicate.deployment.annotations.environment.EnvironmentVariables;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.annotations.lambda.LambdaLayer;
import com.syndicate.deployment.annotations.lambda.LambdaUrlConfig;
import com.syndicate.deployment.annotations.resources.DependsOn;
import com.syndicate.deployment.model.*;
import com.syndicate.deployment.model.lambda.url.AuthType;
import com.syndicate.deployment.model.lambda.url.InvokeMode;


import com.syndicate.deployment.model.TracingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@LambdaHandler(
    lambdaName = "processor",
    roleName = "processor-role",
    layers = {"sdk-layer"},
    isPublishVersion = false,
    tracingMode = TracingMode.Active,
    runtime = DeploymentRuntime.JAVA17,
    logsExpiration = RetentionSetting.SYNDICATE_ALIASES_SPECIFIED
)
@LambdaLayer(
    layerName = "sdk-layer",
    libraries = {"lib/open_meteo_api-1.0.0.jar"},
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
  private static final ObjectMapper mapper = new ObjectMapper();
  private final AmazonDynamoDB dynamoDB = AmazonDynamoDBClientBuilder.standard()
      .withRegion("eu-central-1")
      .build();

  @Override
  public String handleRequest(Object input, Context context) {
    LambdaLogger logger = context.getLogger();
    try {
      // Fetch weather data from the GetOpenMeteo method
      String weatherData = getOpenMeteo();

      // Transform the weather JSON to a DynamoDB compatible map
      Map<String, AttributeValue> weatherEntry = transformWeatherJsonToMap(weatherData);

      logger.log("Weather Data: " + weatherEntry);

      // Insert the weather data into DynamoDB
      dynamoDB.putItem(new PutItemRequest().withTableName(tableName).withItem(weatherEntry));

      return weatherEntry.toString();
    } catch (Exception e) {
      logger.log("Error: " + e.getMessage());
      return "Failed to fetch weather data";
    }
  }

  private static Map<String, AttributeValue> transformWeatherJsonToMap(String json)
      throws Exception {
    JsonNode root = mapper.readTree(json);
    Map<String, AttributeValue> weatherEntry = new HashMap<>();

    weatherEntry.put("id", new AttributeValue().withS(
        UUID.randomUUID().toString())
    );
    Map<String, AttributeValue> forecast = new HashMap<>();
    forecast.put("elevation", new AttributeValue().withN(
        String.valueOf(root.path("elevation").asInt()))
    );
    forecast.put("generationtime_ms", new AttributeValue().withN(
        String.valueOf(root.path("generationtime_ms").asInt()))
    );

    JsonNode hourlyNode = root.path("hourly");
    Map<String, AttributeValue> hourly = new HashMap<>();
    hourly.put("temperature_2m", new AttributeValue().withL(
        StreamSupport.stream(hourlyNode.path("temperature_2m").spliterator(), false)
            .map(node -> new AttributeValue().withN(String.valueOf(node.asDouble())))
            .collect(Collectors.toList()))
    );
    hourly.put("time", new AttributeValue().withL(
        StreamSupport.stream(hourlyNode.path("time").spliterator(), false)
            .map(node -> new AttributeValue().withS(node.asText()))
            .collect(Collectors.toList()))
    );
    forecast.put("hourly", new AttributeValue().withM(hourly));

    JsonNode hourlyUnitsNode = root.path("hourly_units");
    Map<String, AttributeValue> hourlyUnits = new HashMap<>();
    hourlyUnits.put("temperature_2m",
        new AttributeValue().withS(hourlyUnitsNode.path("temperature_2m").asText()));
    hourlyUnits.put("time", new AttributeValue().withS(hourlyUnitsNode.path("time").asText()));
    forecast.put("hourly_units", new AttributeValue().withM(hourlyUnits));

    forecast.put("latitude", new AttributeValue().withN(
        String.valueOf(root.path("latitude").asDouble()))
    );
    forecast.put("longitude", new AttributeValue().withN(
        String.valueOf(root.path("longitude").asDouble()))
    );
    forecast.put("timezone", new AttributeValue().withS(
        root.path("timezone").asText())
    );
    forecast.put("timezone_abbreviation", new AttributeValue().withS(
        root.path("timezone_abbreviation").asText())
    );
    forecast.put("utc_offset_seconds", new AttributeValue().withN(
        String.valueOf(root.path("utc_offset_seconds").asInt()))
    );
    weatherEntry.put("forecast", new AttributeValue().withM(forecast));

    return weatherEntry;
  }
}
