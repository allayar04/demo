package com.task04;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.model.RetentionSetting;

import java.util.HashMap;
import java.util.Map;

@LambdaHandler(
    lambdaName = "sns_handler",
	roleName = "sns_handler-role",
	logsExpiration = RetentionSetting.SYNDICATE_ALIASES_SPECIFIED
)
public class SnsHandler implements RequestHandler<SNSEvent, Object> {

	@Override
	public Object handleRequest(SNSEvent snsEvent, Context context) {
		for(SNSEvent.SNSRecord record : snsEvent.getRecords()) {
			context.getLogger().log("SNS Message Received: " + record.getSNS().getMessage());
		}
		return null;

	}
}
