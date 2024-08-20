package com.task11.handler;


import static com.task11.utils.ResourceNames.*;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.task11.dto.SignUp;
import org.json.JSONObject;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;



public class PostSignUpHandler extends CognitoSupport implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

  public PostSignUpHandler(CognitoIdentityProviderClient cognitoClient) {
    super(cognitoClient);
  }

  @Override
  public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent requestEvent, Context context) {
    try {
      SignUp signUp = SignUp.fromJson(requestEvent.getBody());
      context.getLogger().log("Sign up: " + signUp);

      String userId = cognitoSignUp(signUp)
          .user().attributes().stream()
          .filter(attr -> attr.name().equals("sub"))
          .map(AttributeType::value)
          .findAny()
          .orElseThrow(() -> new RuntimeException("User ID not found in the response."));
      // Confirm sign up
      String idToken = confirmSignUp(signUp)
          .authenticationResult()
          .accessToken();

      return new APIGatewayProxyResponseEvent()
          .withStatusCode(SC_200)
          .withBody(new JSONObject()
              .put("message", "User signed up successfully.")
              .put("userId", userId)
              .put("accessToken", idToken)
              .toString());
    }catch (UsernameExistsException e) {
      return new APIGatewayProxyResponseEvent()
          .withStatusCode(SC_400)
          .withBody(new JSONObject().put("error", "User already exists.").toString());
    }
    catch (Exception e) {
      return new APIGatewayProxyResponseEvent()
          .withStatusCode(SC_400)
          .withBody(new JSONObject().put("error", e.getMessage()).toString());
    }
  }
}