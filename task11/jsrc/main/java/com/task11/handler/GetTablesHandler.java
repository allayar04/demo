package com.task11.handler;

import static com.task11.utils.ResourceNames.*;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.spec.ScanSpec;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import org.json.JSONArray;
import org.json.JSONObject;

public class GetTablesHandler implements
    RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
  private final AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard()
      .withRegion(REGION)
      .build();
  private final String tableName = TABLE_NAME + "Tables-test";
  @Override
  public APIGatewayProxyResponseEvent handleRequest(
      APIGatewayProxyRequestEvent apiGatewayProxyRequestEvent, Context context) {
    try {
      DynamoDB dynamoDB = new DynamoDB(client);
      Table table = dynamoDB.getTable(tableName);
      JSONArray tablesArray = new JSONArray();

      table.scan(new ScanSpec()).forEach(item -> {
        JSONObject tableObj = new JSONObject();
        tableObj.put("id", item.getInt("id"));
        tableObj.put("number", item.getInt("number"));
        tableObj.put("places", item.getInt("places"));
        tableObj.put("isVip", item.getBoolean("isVip"));
        if (item.isPresent("minOrder")) {
          tableObj.put("minOrder", item.getInt("minOrder"));
        }
        tablesArray.put(tableObj);
      });

      context.getLogger().log("Tables: " + tablesArray);
      return new APIGatewayProxyResponseEvent()
          .withStatusCode(SC_200)
          .withBody(new JSONObject().put("tables", tablesArray).toString());
    } catch (Exception e) {
      context.getLogger().log("Error in GetTablesHandler: " + e.getMessage());
      return new APIGatewayProxyResponseEvent()
          .withStatusCode(SC_500)
          .withBody(new JSONObject().put("error", "Failed to fetch tables!").toString());
    }
  }

}
