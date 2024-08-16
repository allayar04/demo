package com.task06;



import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.syndicate.deployment.annotations.environment.EnvironmentVariable;
import com.syndicate.deployment.annotations.environment.EnvironmentVariables;
import com.syndicate.deployment.annotations.events.DynamoDbTriggerEventSource;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.annotations.resources.DependsOn;
import com.syndicate.deployment.model.ResourceType;
import com.syndicate.deployment.model.RetentionSetting;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.joda.time.DateTimeZone;
import org.joda.time.format.ISODateTimeFormat;

@LambdaHandler(
    lambdaName = "audit_producer",
	roleName = "audit_producer-role",
	isPublishVersion = false,
	logsExpiration = RetentionSetting.SYNDICATE_ALIASES_SPECIFIED
)
@EnvironmentVariables(value = {
		@EnvironmentVariable(key = "region", value = "${region}"),
		@EnvironmentVariable(key = "table", value = "${target_table}")
})
@DynamoDbTriggerEventSource(targetTable = "Configuration", batchSize = 10)
@DependsOn(name = "Configuration", resourceType = ResourceType.DYNAMODB_TABLE)
public class AuditProducer implements RequestHandler<DynamodbEvent, Void> {
	private final AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard()
			.withRegion("eu-central-1")
			.build();
	private final String tableName = "cmtr-d2f4ab85-Audit-test";

	@Override
	public Void handleRequest(DynamodbEvent dynamodbEvent, Context context) {
		context.getLogger().log("Event: " + dynamodbEvent.getRecords());

		dynamodbEvent.getRecords().forEach(record -> {
			context.getLogger().log("Record: " + record);

			Map<String, AttributeValue> auditEntry = new HashMap<>();
			String itemKey = record.getDynamodb().getKeys().get("key").getS();
			auditEntry.put("id", new AttributeValue().withS(UUID.randomUUID().toString()));
			auditEntry.put("itemKey", new AttributeValue().withS(itemKey));
			auditEntry.put("modificationTime", new AttributeValue().withS(formatUsingJodaTime(org.joda.time.LocalDate.now())));

			switch (record.getEventName()) {
				case "INSERT":
					createAuditEntry(auditEntry, record, context);
					break;

				case "MODIFY":
					updateAuditEntry(auditEntry, record, context);
					break;
			}
		});

		return null;
	}

	private void createAuditEntry(Map<String, AttributeValue> auditEntry, DynamodbEvent.DynamodbStreamRecord record, Context context) {
		context.getLogger().log("Creating audit entry for INSERT event.");

		Map<String, AttributeValue> newValueMap = new HashMap<>();
		newValueMap.put("key", new AttributeValue().withS(record.getDynamodb().getKeys().get("key").getS()));
		newValueMap.put("value", new AttributeValue().withN(record.getDynamodb().getNewImage().get("value").getN()));

		auditEntry.put("newValue", new AttributeValue().withM(newValueMap));

		client.putItem(new PutItemRequest().withTableName(tableName).withItem(auditEntry));
	}

	private void updateAuditEntry(Map<String, AttributeValue> auditEntry, DynamodbEvent.DynamodbStreamRecord record, Context context) {
		context.getLogger().log("Updating audit entry for MODIFY event.");

		auditEntry.put("updatedAttribute", new AttributeValue().withS("value"));
		auditEntry.put("oldValue", new AttributeValue().withN(record.getDynamodb().getOldImage().get("value").getN()));
		auditEntry.put("newValue", new AttributeValue().withN(record.getDynamodb().getNewImage().get("value").getN()));

		client.putItem(new PutItemRequest().withTableName(tableName).withItem(auditEntry));
	}

	private String formatUsingJodaTime(org.joda.time.LocalDate localDate) {
		org.joda.time.format.DateTimeFormatter formatter = ISODateTimeFormat.dateTime();
		return formatter.print(localDate.toDateTimeAtStartOfDay(DateTimeZone.UTC));
	}
}
