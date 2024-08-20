package com.task11.handler;


import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.spec.ScanSpec;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import static com.task11.utils.ResourceNames.*;

import org.json.JSONArray;
import org.json.JSONObject;

public class GetReservationsHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

  private final String tableName = TABLE_NAME + "Reservations-test";
  private final AmazonDynamoDB dynamoDbClient = AmazonDynamoDBClientBuilder.standard()
      .withRegion(REGION).build();

  @Override
  public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
    try {
      DynamoDB dynamoDB = new DynamoDB(dynamoDbClient);
      Table table = dynamoDB.getTable(tableName);
      JSONArray reservationsArray = new JSONArray();
      table.scan(new ScanSpec()).forEach(item -> {
        JSONObject reservationObj = new JSONObject();
        reservationObj.put("id", item.getString("id"));
        reservationObj.put("tableNumber", item.getInt("tableNumber"));
        reservationObj.put("clientName", item.getString("clientName"));
        reservationObj.put("phoneNumber", item.getString("phoneNumber"));
        reservationObj.put("date", item.getString("date"));
        reservationObj.put("slotTimeStart", item.getString("slotTimeStart"));
        reservationObj.put("slotTimeEnd", item.getString("slotTimeEnd"));
        reservationsArray.put(reservationObj);
      });
      context.getLogger().log("Reservations: " + reservationsArray);
      return new APIGatewayProxyResponseEvent()
          .withStatusCode(SC_200)
          .withBody(new JSONObject().put("reservations", reservationsArray).toString());
    } catch (Exception e) {
      return new APIGatewayProxyResponseEvent()
          .withStatusCode(SC_400)
          .withBody("There was an error in the request: " + e.getMessage());
    }
  }
}
