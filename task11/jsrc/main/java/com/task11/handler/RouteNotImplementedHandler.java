package com.task10.handler;

import static com.task10.utils.ResourceNames.SC_501;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import org.json.JSONObject;

public class RouteNotImplementedHandler implements
    RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

  @Override
  public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent requestEvent, Context context) {
    context.getLogger().log("Handler for the %s method on the %s path is not implemented."
        .formatted(requestEvent.getHttpMethod(), requestEvent.getPath()));
    return new APIGatewayProxyResponseEvent()
        .withStatusCode(SC_501)
        .withBody(
            new JSONObject().put(
                "message",
                "Handler for the %s method on the %s path is not implemented."
                    .formatted(requestEvent.getHttpMethod(), requestEvent.getPath())
            ).toString()
        );
  }

}