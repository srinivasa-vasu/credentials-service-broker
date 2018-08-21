/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.credhub.broker.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.UUID;

import org.springframework.cloud.servicebroker.model.catalog.Catalog;
import org.springframework.cloud.servicebroker.model.catalog.Plan;
import org.springframework.cloud.servicebroker.model.catalog.ServiceDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ServiceCatalogConfiguration {
	@Bean
	public Catalog catalog() {
		Plan plan = Plan.builder().id(UUID.randomUUID().toString())
			.name("standard").description("Credhub based credential store plan").free(true).build();

		ServiceDefinition serviceDefinition = ServiceDefinition.builder()
			.id(UUID.randomUUID().toString()).name("credentialstore")
			.description("Credhub based credential store service").bindable(true)
			.tags("credhub", "secrets", "credentails", "certs").plans(plan)
			.metadata("displayName", "Credstore")
			.metadata("longDescription", "Credhub based credential store service")
			.metadata("providerDisplayName", "Credhub Service")
			.metadata("imageUrl", "https://raw.githubusercontent.com/cloudfoundry-incubator/credhub/master/docs/images/logo.png")
			.metadata("documentationUrl", "https://github.com/srinivasa-vasu/credentials-service-broker")
			.metadata("supportUrl", "https://github.com/srinivasa-vasu/credentials-service-broker")
			.build();

		return Catalog.builder().serviceDefinitions(serviceDefinition).build();
	}
}
