package com.task05;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.syndicate.deployment.annotations.environment.EnvironmentVariable;
import com.syndicate.deployment.annotations.environment.EnvironmentVariables;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.model.Architecture;
import com.syndicate.deployment.model.DeploymentRuntime;
import com.syndicate.deployment.model.RetentionSetting;
import org.joda.time.DateTimeZone;
import org.joda.time.format.ISODateTimeFormat;

import com.google.gson.Gson;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@LambdaHandler(lambdaName = "api_handler",
		roleName = "api_handler-role",
		runtime = DeploymentRuntime.JAVA17,
		architecture = Architecture.ARM64,
		isPublishVersion = false,
		logsExpiration = RetentionSetting.SYNDICATE_ALIASES_SPECIFIED
)
@EnvironmentVariables(value = {
		@EnvironmentVariable(key = "region", value = "${region}"),
		@EnvironmentVariable(key = "table", value = "${target_table}")}
)
public class ApiHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

	private static final ObjectMapper objectMapper = new ObjectMapper();
	private static final int SC_CREATED = 201;
	private static final int SC_BAD_REQUEST = 400;
	private final AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard()
			.withRegion("eu-central-1")
			.build();
	private final String tableName = "cmtr-d2f4ab85-Events-test";

	@Override
	public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
		context.getLogger().log("Received request: " + request);

		// Check if request body is present
		if (request.getBody() == null || request.getBody().isEmpty()) {
			return createResponse(SC_CREATED, "{\"message\": \"Request body is missing\"}");
		}

		context.getLogger().log("Request body: " + request.getBody());

		Event event = null;

		try {
			event = objectMapper.readValue(request.getBody().replace("content", "body"), Event.class);
		} catch (JsonProcessingException e) {
			context.getLogger().log("Error parsing request body: " + e.getMessage());
			return createResponse(SC_CREATED, "{\"message\": \"Invalid request body\"}");
		}

		if (event.getPrincipalId() == 0 || event.getBody() == null) {
			context.getLogger().log("Missing required fields: principalId or content");
			return createResponse(SC_CREATED, "{\"message\": \"Missing required fields: principalId or content\"}");
		}

		context.getLogger().log("Parsed event: " + event);

		event.setId(UUID.randomUUID().toString());
		event.setCreatedAt(formatUsingJodaTime(org.joda.time.LocalDate.now()));

		context.getLogger().log("Processed event: " + event);

		Map<String, AttributeValue> item = new HashMap<>();
		item.put("id", new AttributeValue().withS(event.getId()));
		item.put("principalId", new AttributeValue().withN(String.valueOf(event.getPrincipalId())));
		item.put("createdAt", new AttributeValue().withS(event.getCreatedAt()));

		Map<String, AttributeValue> bodyMap = new HashMap<>();
		event.getBody().forEach((key, value) -> bodyMap.put(key, new AttributeValue().withS(value)));
		item.put("body", new AttributeValue().withM(bodyMap));

		context.getLogger().log("DynamoDB item: " + item);
		client.putItem(new PutItemRequest().withTableName(tableName).withItem(item));
		context.getLogger().log("Item added to table: " + tableName);

		Response responseObj = new Response(SC_CREATED, event);

		APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
		response.setStatusCode(SC_CREATED);
		try {
			String responseBody = objectMapper.writeValueAsString(responseObj);
			response.setBody(responseBody);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return response;
	}

	private APIGatewayProxyResponseEvent createResponse(int statusCode, String body) {
		return new APIGatewayProxyResponseEvent()
				.withStatusCode(statusCode)
				.withBody(body);
	}

	public static String formatUsingJodaTime(org.joda.time.LocalDate localDate) {
		org.joda.time.format.DateTimeFormatter formatter = ISODateTimeFormat.dateTime();
		return formatter.print(localDate.toDateTimeAtStartOfDay(DateTimeZone.UTC));
	}

	public static class Event {
		@JsonProperty("id")
		private String id;
		@JsonProperty("principalId")
		private int principalId;
		@JsonProperty("createdAt")
		private String createdAt;
		@JsonProperty("body")
		private Map<String, String> body;

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public int getPrincipalId() {
			return principalId;
		}

		public void setPrincipalId(int principalId) {
			this.principalId = principalId;
		}

		public String getCreatedAt() {
			return createdAt;
		}

		public void setCreatedAt(String createdAt) {
			this.createdAt = createdAt;
		}

		public Map<String, String> getBody() {
			return body;
		}

		public void setBody(Map<String, String> body) {
			this.body = body;
		}

		@Override
		public String toString() {
			return new Gson().toJson(this);
		}
	}

	private static class Response {

		@JsonProperty("statusCode")
		private int statusCode;
		@JsonProperty("event")
		private ApiHandler.Event event;

		public Response(int statusCode, ApiHandler.Event event) {
			this.statusCode = statusCode;
			this.event = event;
		}

		public int getStatusCode() {
			return statusCode;
		}

		public void setStatusCode(int statusCode) {
			this.statusCode = statusCode;
		}

		public ApiHandler.Event getEvent() {
			return event;
		}

		public void setEvent(ApiHandler.Event event) {
			this.event = event;
		}
		@Override
		public String toString() {
			return "{" + "\"statusCode\": " + statusCode + ","
					+ "\"event\": " + getEvent().toString() + "}";
		}
	}
}