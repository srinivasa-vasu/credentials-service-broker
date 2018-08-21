package org.springframework.cloud.credhub.broker.controller;

import java.util.StringJoiner;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CredentialBrokerController {

	@GetMapping(value = "/{bindingId}", produces = "application/json")
	public String getResp(@PathVariable String bindingId) {
		// @formatter:off
		return new StringJoiner(", ", "{", "}")
			.add("\"id\": \"" + bindingId + "\"")
			.add("\"create-service\": \"cf create-service credentialstore standard credhub-svc\"")
			.add("\"bind-service\": \"cf bind-service credhub-client credhub-svc\"").toString();
		// @formatter:on
	}
}
