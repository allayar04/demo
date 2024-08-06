package com.task03.custom_event;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

public class CustomAPIGatewayProxyResponseEvent extends APIGatewayProxyResponseEvent {
  private String message;

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public CustomAPIGatewayProxyResponseEvent withStatusCode(Integer statusCode) {
    super.setStatusCode(statusCode);
    return this;
  }

  public CustomAPIGatewayProxyResponseEvent withMessage(String message) {
    setMessage(message);
    return this;
  }

  @Override
  public String toString() {
    return "CustomAPIGatewayProxyResponseEvent{" +
            "statusCode=" + getStatusCode() +
            ", message='" + message + '\'' +
            '}';
  }
}
