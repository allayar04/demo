package com.task07;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.syndicate.deployment.annotations.environment.EnvironmentVariable;
import com.syndicate.deployment.annotations.environment.EnvironmentVariables;
import com.syndicate.deployment.annotations.events.EventBridgeRuleSource;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.model.DeploymentRuntime;
import com.syndicate.deployment.model.RetentionSetting;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    @EnvironmentVariable(key = "target_bucket", value = "${target_bucket}")
})
public class UuidGenerator implements RequestHandler<ScheduledEvent, String> {

  private final AmazonS3 clientS3 = AmazonS3ClientBuilder.standard().build();
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final String bucketName = System.getenv("cmtr-d2f4ab85-uuid-storage-test");

  @Override
  public String handleRequest(ScheduledEvent event, Context context) {
    context.getLogger().log("Lambda function triggered at: " + ZonedDateTime.now());
    List<String> uuids = generateUUIDs();
    try {
      String fileName = ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT) + ".json";
      Map<String, List<String>> idsMap = new HashMap<>();
      idsMap.put("ids", uuids);
      String content = objectMapper.writeValueAsString(idsMap);
      context.getLogger().log("Content: " + content);
      ByteArrayInputStream inputStream = new ByteArrayInputStream(
          content.getBytes(StandardCharsets.UTF_8));
      ObjectMetadata metadata = new ObjectMetadata();
      metadata.setContentType("application/json");
      metadata.setContentLength(content.length());
      context.getLogger().log("Uploading to S3: " + fileName);
      clientS3.putObject(bucketName, fileName, inputStream, metadata);
      context.getLogger().log("Successfully uploaded " + fileName + " to " + bucketName);

      listFilesInBucket(context);

      return "Execution completed successfully";
    } catch (Exception e) {
      context.getLogger().log("Error uploading to S3: " + e.getMessage());
      throw new RuntimeException(e);
    }
  }

  private void listFilesInBucket(Context context) {
    try {
      ListObjectsV2Result result = clientS3.listObjectsV2(bucketName);
      List<S3ObjectSummary> objects = result.getObjectSummaries();
      context.getLogger().log("Files in bucket:");
      for (S3ObjectSummary os : objects) {
        context.getLogger().log("* " + os.getKey());
      }
    } catch (Exception e) {
      context.getLogger().log("Error listing files in S3: " + e.getMessage());
    }
  }

  private static List<String> generateUUIDs() {
    int countOfUuids = 10;
    List<String> uuids = new ArrayList<>();
    for (int i = 0; i < countOfUuids; i++) {
      uuids.add(UUID.randomUUID().toString());
    }
    return uuids;
  }
}

