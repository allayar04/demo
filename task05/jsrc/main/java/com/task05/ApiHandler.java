package com.task05;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.syndicate.deployment.annotations.environment.EnvironmentVariable;
import com.syndicate.deployment.annotations.environment.EnvironmentVariables;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.model.Architecture;
import com.syndicate.deployment.model.DeploymentRuntime;
import com.syndicate.deployment.model.RetentionSetting;
import org.joda.time.DateTimeZone;
import org.joda.time.format.ISODateTimeFormat;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.json.JSONObject;

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
public class ApiHandler implements RequestHandler<APIGatewayProxyRequestEvent, Response> {

	//private static final ObjectMapper objectMapper = new ObjectMapper();
	private static final int SC_CREATED = 201;
	private final AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard()
			.withRegion("eu-central-1")
			.build();
	private final String tableName = "cmtr-d2f4ab85-Events-test"; //-test

	@Override
	public Response handleRequest(APIGatewayProxyRequestEvent request, Context context) {
		context.getLogger().log("Received request: " + request);
		String requestBody = request.getBody();
		context.getLogger().log("Request body: " + requestBody);

		JSONObject json = new JSONObject(requestBody.replace("content", "body"));
    Event event = new Event(json.getInt("principalId"), json.getJSONObject("body").toMap());

		context.getLogger().log("Parsed event: " + event);


		event.setId(UUID.randomUUID().toString());
		event.setCreatedAt(formatUsingJodaTime(org.joda.time.LocalDate.now()));

		context.getLogger().log("Processed event: " + event);

		Map<String, AttributeValue> item = new HashMap<>();
		item.put("id", new AttributeValue().withS(event.getId()));
		item.put("principalId", new AttributeValue().withN(String.valueOf(event.getPrincipalId())));
		item.put("createdAt", new AttributeValue().withS(event.getCreatedAt()));

		Map<String, AttributeValue> bodyMap = new HashMap<>();
		event.getBody().forEach((key, value) -> bodyMap.put(key, new AttributeValue().withS(value.toString())));
		item.put("body", new AttributeValue().withM(bodyMap));

		context.getLogger().log("DynamoDB item: " + item);
		client.putItem(new PutItemRequest().withTableName(tableName).withItem(item));
		context.getLogger().log("Item added to table: " + tableName);

		APIGatewayProxyResponseEvent apiResponse = new APIGatewayProxyResponseEvent();
		apiResponse.setStatusCode(SC_CREATED);

		Response response = new Response(SC_CREATED, event);

		context.getLogger().log("Response: " + response);
		return response;
	}

	public static String formatUsingJodaTime(org.joda.time.LocalDate localDate) {
		org.joda.time.format.DateTimeFormatter formatter = ISODateTimeFormat.dateTime();
		return formatter.print(localDate.toDateTimeAtStartOfDay(DateTimeZone.UTC));
	}
}