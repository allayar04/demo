package com.task06;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
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
import org.joda.time.DateTimeZone;
import org.joda.time.format.ISODateTimeFormat;


import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
	private final AmazonDynamoDB dynamoDBClient = AmazonDynamoDBClientBuilder.standard()
			.withRegion("eu-central-1")
			.build();
	private final String auditTableName = "cmtr-d0429c20-Audit-test";

	@Override
	public Void handleRequest(DynamodbEvent dynamodbEvent, Context context) {
		context.getLogger().log("Received DynamoDB Event: " + dynamodbEvent.getRecords());

		dynamodbEvent.getRecords().forEach(record -> {
			Map<String, AttributeValue> auditRecord = generateAuditRecord(record, context);
			context.getLogger().log("Generated audit record: " + auditRecord);
			dynamoDBClient.putItem(new PutItemRequest().withTableName(auditTableName).withItem(auditRecord));
			context.getLogger().log("Audit record saved to table: " + auditTableName);
		});

		return null;
	}

	private Map<String, AttributeValue> generateAuditRecord(DynamodbEvent.DynamodbStreamRecord record, Context context) {
		Map<String, AttributeValue> auditEntry = new HashMap<>();
		String configKey = record.getDynamodb().getKeys().get("key").getS();
		auditEntry.put("id", new AttributeValue(UUID.randomUUID().toString()));
		auditEntry.put("itemKey", new AttributeValue(configKey));
		auditEntry.put("modificationTime", new AttributeValue(getCurrentTimestamp()));

		switch (record.getEventName()) {
			case "INSERT":
				processInsertEvent(record, auditEntry, configKey);
				break;
			case "MODIFY":
				processModifyEvent(record, auditEntry);
				break;
		}

		return auditEntry;
	}

	private void processInsertEvent(DynamodbEvent.DynamodbStreamRecord record, Map<String, AttributeValue> auditEntry, String itemKey) {
		Map<String, AttributeValue> newValues = new HashMap<>();
		newValues.put("key", new AttributeValue(itemKey));
		newValues.put("value", new AttributeValue(record.getDynamodb().getNewImage().get("value").getN()));
		auditEntry.put("newValue", new AttributeValue().withM(newValues));
	}

	private void processModifyEvent(DynamodbEvent.DynamodbStreamRecord record, Map<String, AttributeValue> auditEntry) {
		auditEntry.put("updatedAttribute", new AttributeValue("value"));
		auditEntry.put("oldValue", new AttributeValue(record.getDynamodb().getOldImage().get("value").getN()));
		auditEntry.put("newValue", new AttributeValue(record.getDynamodb().getNewImage().get("value").getN()));
	}

	private String getCurrentTimestamp() {
		org.joda.time.LocalDate localDate = org.joda.time.LocalDate.now(DateTimeZone.UTC);
		return ISODateTimeFormat.dateTime().print(localDate);
	}
}
