package com.task10;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.syndicate.deployment.annotations.environment.EnvironmentVariable;
import com.syndicate.deployment.annotations.environment.EnvironmentVariables;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.annotations.resources.DependsOn;
import com.syndicate.deployment.model.DeploymentRuntime;
import com.syndicate.deployment.model.ResourceType;
import com.syndicate.deployment.model.RetentionSetting;

import com.syndicate.deployment.model.environment.ValueTransformer;
import com.task10.dto.RouteKey;
import com.task10.handler.GetReservationsHandler;
import com.task10.handler.GetTableByIdHandler;
import com.task10.handler.GetTablesHandler;
import com.task10.handler.PostReservationHandler;
import com.task10.handler.PostSignInHandler;
import com.task10.handler.PostSignUpHandler;
import com.task10.handler.PostTableHandler;
import com.task10.handler.RouteNotImplementedHandler;
import java.util.Map;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;

@LambdaHandler(lambdaName = "api_handler",
		roleName = "api_handler-role",
		runtime = DeploymentRuntime.JAVA17,
		isPublishVersion = false,
		logsExpiration = RetentionSetting.SYNDICATE_ALIASES_SPECIFIED
)
@DependsOn(resourceType = ResourceType.COGNITO_USER_POOL, name = "${booking_userpool}")
@EnvironmentVariables(value = {
		@EnvironmentVariable(key = "REGION", value = "${region}"),
		@EnvironmentVariable(key = "COGNITO_ID", value = "${booking_userpool}",
				valueTransformer = ValueTransformer.USER_POOL_NAME_TO_USER_POOL_ID),
		@EnvironmentVariable(key = "CLIENT_ID", value = "${booking_userpool}",
				valueTransformer = ValueTransformer.USER_POOL_NAME_TO_CLIENT_ID)
})

public class ApiHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
	private final CognitoIdentityProviderClient cognitoClient;
	private final Map<RouteKey, RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent>> handlersByRouteKey;
	private final Map<String, String> headersForCORS;
	private final RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> routeNotImplementedHandler;

	public ApiHandler() {
		this.cognitoClient = initCognitoClient();
		this.handlersByRouteKey = initHandlers();
		this.headersForCORS = initHeadersForCORS();
		this.routeNotImplementedHandler = new RouteNotImplementedHandler();
	}

	@Override
	public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent requestEvent, Context context) {
		return getHandler(requestEvent)
				.handleRequest(requestEvent, context)
				.withHeaders(headersForCORS);
	}

	private RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> getHandler(APIGatewayProxyRequestEvent requestEvent) {
		return handlersByRouteKey.getOrDefault(getRouteKey(requestEvent), routeNotImplementedHandler);
	}

	private RouteKey getRouteKey(APIGatewayProxyRequestEvent requestEvent) {
		String path = requestEvent.getPath();
		if (path.matches("/tables/\\d+")) {
			path = "/tables/{tableId}";
		}
		return new RouteKey(requestEvent.getHttpMethod(), path);
	}

	private CognitoIdentityProviderClient initCognitoClient() {
		return CognitoIdentityProviderClient.builder()
				.region(Region.of(System.getenv("REGION")))
				.credentialsProvider(DefaultCredentialsProvider.create())
				.build();
	}

	private Map<RouteKey, RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent>> initHandlers() {
		return Map.of(
				new RouteKey("POST", "/signup"), new PostSignUpHandler(cognitoClient),
				new RouteKey("POST", "/signin"), new PostSignInHandler(cognitoClient),
				new RouteKey("GET", "/tables"), new GetTablesHandler(),
				new RouteKey("POST", "/tables"), new PostTableHandler(),
				new RouteKey("GET", "/tables/{tableId}"), new GetTableByIdHandler(),
				new RouteKey("POST", "/reservations"), new PostReservationHandler(),
				new RouteKey("GET", "/reservations"), new GetReservationsHandler()
		);
	}

	private Map<String, String> initHeadersForCORS() {
		return Map.of(
				"Access-Control-Allow-Headers", "Content-Type,X-Amz-Date,Authorization,X-Api-Key,X-Amz-Security-Token",
				"Access-Control-Allow-Origin", "*",
				"Access-Control-Allow-Methods", "*",
				"Accept-Version", "*"
		);
	}
}
