package com.task10.handler;

import static com.task10.utils.ResourceNames.REGION;
import static com.task10.utils.ResourceNames.SC_200;
import static com.task10.utils.ResourceNames.SC_400;
import static com.task10.utils.ResourceNames.TABLE_NAME;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.spec.ScanSpec;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.json.JSONObject;

public class PostReservationHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

  private final AmazonDynamoDB amazonDynamoDB = AmazonDynamoDBClientBuilder.standard()
      .withRegion(REGION)
      .build();
  private final String reservationTable = TABLE_NAME + "Reservations-test";
  private final String tablesTable = TABLE_NAME + "Tables-test";

  @Override
  public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
    try {
      JSONObject requestBody = new JSONObject(request.getBody());
      context.getLogger().log("Request body: " + requestBody);
      DynamoDB dynamoDB = new DynamoDB(amazonDynamoDB);
      Table reservationTable = dynamoDB.getTable(this.reservationTable);
      Table tablesTable = dynamoDB.getTable(this.tablesTable);
      int table_number = requestBody.getInt("tableNumber");
      List<Integer> tables = new ArrayList<>();
      tablesTable.scan(new ScanSpec()).forEach(item -> {
        tables.add(item.getInt("number"));
      });
      if (!tables.contains(table_number)) {
        throw new IllegalArgumentException("Table not found");
      }
      if (reservationTable.scan(new ScanSpec()).iterator().hasNext()) {
        reservationTable.scan(new ScanSpec()).forEach(item -> {
          if (item.getInt("tableNumber") == table_number) {
            throw new IllegalArgumentException("Table is already reserved for this date!");
          }
        });
      }


      String reservationId = UUID.randomUUID().toString();
      Item item = new Item()
          .withPrimaryKey("id", reservationId)
          .withInt("tableNumber", table_number)
          .withString("clientName", requestBody.getString("clientName"))
          .withString("phoneNumber", requestBody.getString("phoneNumber"))
          .withString("date", requestBody.getString("date"))
          .withString("slotTimeStart", requestBody.getString("slotTimeStart"))
          .withString("slotTimeEnd", requestBody.getString("slotTimeEnd"));

      reservationTable.putItem(item);

      context.getLogger().log("Reservation created: " + item);
      return new APIGatewayProxyResponseEvent()
          .withStatusCode(SC_200)
          .withBody(new JSONObject().put("reservationId", reservationId).toString());

    } catch (Exception e) {
      return new APIGatewayProxyResponseEvent()
          .withStatusCode(SC_400)
          .withBody("There was an error in the request: " + e.getMessage());
    }
  }
}