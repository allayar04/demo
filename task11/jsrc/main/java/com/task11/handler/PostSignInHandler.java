package com.task11.handler;


import static com.task11.utils.ResourceNames.*;
import com.task11.dto.*;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import org.json.JSONObject;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;


public class PostSignInHandler extends CognitoSupport implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

  public PostSignInHandler(CognitoIdentityProviderClient cognitoClient) {
    super(cognitoClient);
  }

  @Override
  public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent requestEvent, Context context) {
    try {
      SignIn signIn = SignIn.fromJson(requestEvent.getBody());

      String accessToken = cognitoSignIn(signIn.email(), signIn.password())
          .authenticationResult()
          .idToken();

      return new APIGatewayProxyResponseEvent()
          .withStatusCode(SC_200)
          .withBody(new JSONObject().put("accessToken", accessToken).toString());
    } catch (Exception e) {
      return new APIGatewayProxyResponseEvent()
          .withStatusCode(SC_400)
          .withBody(new JSONObject().put("error", e.getMessage()).toString());
    }
  }
}