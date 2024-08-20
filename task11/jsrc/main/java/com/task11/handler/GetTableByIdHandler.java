package com.task11.handler;

import static com.task11.utils.ResourceNames.*;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import org.json.JSONObject;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;

public class GetTableByIdHandler implements
    RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
  private final String tableName = TABLE_NAME + "Tables-test";
  private final AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard()
      .withRegion(REGION)
      .build();
  @Override
  public APIGatewayProxyResponseEvent handleRequest(
      APIGatewayProxyRequestEvent apiGatewayProxyRequestEvent, Context context) {
    try {
      String tableId = apiGatewayProxyRequestEvent.getPath().replace("/tables/", "");
      DynamoDB dynamoDB = new DynamoDB(client);
      Table table = dynamoDB.getTable(tableName);
      Item item = table.getItem("id", Integer.parseInt(tableId));
      if (item == null) {
        return new APIGatewayProxyResponseEvent()
            .withStatusCode(SC_404)
            .withBody(new JSONObject().put("error", "Table not found!").toString());
      }

      JSONObject tableObj = new JSONObject();
      tableObj.put("id", item.getInt("id"));
      tableObj.put("number", item.getInt("number"));
      tableObj.put("places", item.getInt("places"));
      tableObj.put("isVip", item.getBoolean("isVip"));
      if (item.isPresent("minOrder")) {
        tableObj.put("minOrder", item.getInt("minOrder"));
      }

      return new APIGatewayProxyResponseEvent()
          .withStatusCode(SC_200)
          .withBody(tableObj.toString());
    } catch (Exception e) {
      context.getLogger().log("Error in GetTableHandler: " + e.getMessage());
      return new APIGatewayProxyResponseEvent()
          .withStatusCode(SC_500)
          .withBody(new JSONObject().put("error", "Failed to find table").toString());
    }
  }
}
