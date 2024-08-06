package com.task03;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.annotations.lambda.LambdaUrlConfig;
import com.syndicate.deployment.model.Architecture;
import com.syndicate.deployment.model.DeploymentRuntime;
import com.syndicate.deployment.model.RetentionSetting;
import com.syndicate.deployment.model.lambda.url.AuthType;
import com.syndicate.deployment.model.lambda.url.InvokeMode;

import com.task03.custom_event.CustomAPIGatewayProxyResponseEvent;
import java.util.Map;
import java.util.function.Function;

@LambdaHandler(
		lambdaName = "hello_world",
		roleName = "hello_world-role",
		runtime = DeploymentRuntime.JAVA17,
		architecture = Architecture.ARM64,
		logsExpiration = RetentionSetting.SYNDICATE_ALIASES_SPECIFIED
)
@LambdaUrlConfig(
		authType = AuthType.NONE,
		invokeMode = InvokeMode.BUFFERED
)
public class HelloWorld implements RequestHandler<APIGatewayProxyRequestEvent, CustomAPIGatewayProxyResponseEvent> {

	private static final int SC_OK = 200;
	private static final String HELLO_MESSAGE = "Hello from Lambda";
	private static final int SC_BAD_REQUEST = 400;
	private final Map<String, String> responseHeaders = Map.of("Content-Type", "application/json");
	private final Map<RouteKey, Function<APIGatewayProxyRequestEvent, CustomAPIGatewayProxyResponseEvent>> routeHandlers = Map.of(
			new RouteKey("GET", "/hello"), this::handleGetHello
	);

	@Override
	public CustomAPIGatewayProxyResponseEvent handleRequest(
			APIGatewayProxyRequestEvent apiGatewayProxyRequestEvent, Context context) {
		RouteKey routeKey = new RouteKey(getMethod(apiGatewayProxyRequestEvent), getPath(apiGatewayProxyRequestEvent));
		return routeHandlers.getOrDefault(routeKey, this::handleGetHello).apply(apiGatewayProxyRequestEvent);
	}

	private CustomAPIGatewayProxyResponseEvent handleGetHello(APIGatewayProxyRequestEvent requestEvent) {
		return new CustomAPIGatewayProxyResponseEvent()
				.withStatusCode(SC_OK)
				.withMessage(HELLO_MESSAGE);
	}

	private APIGatewayV2HTTPResponse buildResponse(int statusCode, String body) {
		return APIGatewayV2HTTPResponse.builder()
				.withStatusCode(statusCode)
				.withHeaders(responseHeaders)
				.withBody(body)
				.build();
	}

	private String getMethod(APIGatewayProxyRequestEvent requestEvent) {
		return requestEvent.getHttpMethod();
	}

	private String getPath(APIGatewayProxyRequestEvent requestEvent) {
		return requestEvent.getPath();
	}

	private record RouteKey(String method, String path) {
	}

	private record Body(String statusCode, String message) {
		@Override
		public String toString() {
			return "{\"statusCode\": " + statusCode + ", \"message\": \"" + message + "\"}";
		}
	}
}