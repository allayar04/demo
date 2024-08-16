package com.task07;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.AmazonS3;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.syndicate.deployment.annotations.environment.EnvironmentVariable;
import com.syndicate.deployment.annotations.environment.EnvironmentVariables;
import com.syndicate.deployment.annotations.events.EventBridgeRuleSource;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.model.DeploymentRuntime;
import com.syndicate.deployment.model.RetentionSetting;
import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@LambdaHandler(lambdaName = "uuid_generator",
		roleName = "uuid_generator-role",
		runtime = DeploymentRuntime.JAVA17,
		isPublishVersion = false,
		logsExpiration = RetentionSetting.SYNDICATE_ALIASES_SPECIFIED
)
@EventBridgeRuleSource(targetRule = "uuid_trigger")
@EnvironmentVariables(value = {
		@EnvironmentVariable(key = "region", value = "${region}"),
		@EnvironmentVariable(key = "target_bucket", value = "${target_bucket}}")
})
public class UuidGenerator implements RequestHandler<ScheduledEvent, String> {

	private final AmazonS3 s3 = AmazonS3Client.builder().build();
	private final ObjectMapper objectMapper = new ObjectMapper();

	@Override
	public String handleRequest(ScheduledEvent event, Context context) {
		context.getLogger().log("Received Event: " + event);

		String bucketName = System.getenv("cmtr-d2f4ab85-uuid-storage-test");
		String executionTime = ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT);
		String fileName = executionTime + ".json";
		List<String> uuids = generateUUIDs(10);

		try {
			File tempFile = new File("/tmp/" + fileName);
			if (tempFile.createNewFile()) {
				try (PrintWriter writer = new PrintWriter(tempFile)) {
					writer.write(objectMapper.writeValueAsString(new UUIDResponse(uuids)));
				}

				s3.putObject(new PutObjectRequest(bucketName, fileName, Paths.get(tempFile.getPath()).toFile()));

				context.getLogger().log("Successfully uploaded object to S3: " + bucketName + "/" + fileName);
			}
		} catch (Exception e) {
			context.getLogger().log("Failed to put object in S3: " + e.getMessage());
			throw new RuntimeException("Failed to execute Lambda function", e);
		}

		return fileName;
	}

	private List<String> generateUUIDs(int count) {
		List<String> uuids = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			uuids.add(UUID.randomUUID().toString());
		}
		return uuids;
	}

	private static class UUIDResponse {
		public List<String> ids;

		public UUIDResponse(List<String> ids) {
			this.ids = ids;
		}
	}
}