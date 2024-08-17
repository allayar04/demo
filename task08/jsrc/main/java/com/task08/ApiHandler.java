package com.task08;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.api.open_meteo.GetOpenMeteo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.annotations.lambda.LambdaLayer;
import com.syndicate.deployment.annotations.lambda.LambdaUrlConfig;
import com.syndicate.deployment.model.Architecture;
import com.syndicate.deployment.model.ArtifactExtension;
import com.syndicate.deployment.model.DeploymentRuntime;
import com.syndicate.deployment.model.RetentionSetting;

import com.syndicate.deployment.model.lambda.url.AuthType;
import com.syndicate.deployment.model.lambda.url.InvokeMode;
import java.util.HashMap;
import java.util.Map;

@LambdaHandler(
    lambdaName = "api_handler",
	  roleName = "api_handler-role",
		layers = {"sdk-layer"},
	  isPublishVersion = false,
		runtime = DeploymentRuntime.JAVA17,
	  logsExpiration = RetentionSetting.SYNDICATE_ALIASES_SPECIFIED
)
@LambdaLayer(
		layerName = "sdk-layer",
		libraries = {"lib/open_meteo_api-1.0-SNAPSHOT.jar"},
		runtime = DeploymentRuntime.JAVA17,
		artifactExtension = ArtifactExtension.ZIP
)
@LambdaUrlConfig(
		authType = AuthType.NONE,
		invokeMode = InvokeMode.BUFFERED
)
public class ApiHandler implements RequestHandler<Object, String> {
	private static final ObjectMapper mapper = new ObjectMapper();

	@Override
	public String handleRequest(Object o, Context context) {
		try {
			return GetOpenMeteo.getOpenMeteo();
		} catch (Exception e) {
			return e.getMessage();
		}
	}
}
