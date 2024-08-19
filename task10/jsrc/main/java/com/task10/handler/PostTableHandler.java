package com.task10.handler;

import static com.task10.utils.ResourceNames.REGION;
import static com.task10.utils.ResourceNames.SC_200;
import static com.task10.utils.ResourceNames.SC_500;
import static com.task10.utils.ResourceNames.TABLE_NAME;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import org.json.JSONObject;

public class PostTableHandler implements
    RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
  private final AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard()
      .withRegion(REGION)
      .build();
  private final String tableName = TABLE_NAME + "Tables-test";

  @Override
  public APIGatewayProxyResponseEvent handleRequest(
      APIGatewayProxyRequestEvent apiGatewayProxyRequestEvent, Context context) {
    try {
      JSONObject requestBody = new JSONObject(apiGatewayProxyRequestEvent.getBody());
      context.getLogger().log("Request body: " + requestBody);
      DynamoDB dynamoDB = new DynamoDB(client);
      Table table = dynamoDB.getTable(tableName);

      Item item = new Item()
          .withPrimaryKey("id", requestBody.getInt("id"))
          .withInt("number", requestBody.getInt("number"))
          .withInt("places", requestBody.getInt("places"))
          .withBoolean("isVip", requestBody.getBoolean("isVip"));
      if (requestBody.has("minOrder")) {
        item = item.withInt("minOrder", requestBody.getInt("minOrder"));
      }

      table.putItem(item);
      context.getLogger().log("Table created: " + item);

      JSONObject responseBody = new JSONObject().put("id", requestBody.getInt("id"));
      return new APIGatewayProxyResponseEvent()
          .withStatusCode(SC_200)
          .withBody(responseBody.toString());
    } catch (Exception e) {
      context.getLogger().log("Error in PostTablesHandler: " + e.getMessage());
      return new APIGatewayProxyResponseEvent()
          .withStatusCode(SC_500)
          .withBody(new JSONObject().put("error", "Failed to create table!").toString());
    }

  }
}
