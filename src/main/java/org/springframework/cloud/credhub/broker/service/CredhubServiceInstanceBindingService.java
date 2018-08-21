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

package org.springframework.cloud.credhub.broker.service;

import static org.springframework.credhub.support.permissions.Operation.READ;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.cloud.credhub.broker.model.ServiceBinding;
import org.springframework.cloud.credhub.broker.repository.ServiceBindingRepository;
import org.springframework.cloud.credhub.broker.model.ApplicationInformation;
import org.springframework.cloud.credhub.broker.model.User;
import org.springframework.cloud.credhub.broker.config.SecurityAuthorities;
import org.springframework.cloud.servicebroker.exception.ServiceInstanceBindingDoesNotExistException;
import org.springframework.cloud.servicebroker.model.binding.*;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceAppBindingResponse.CreateServiceInstanceAppBindingResponseBuilder;
import org.springframework.cloud.servicebroker.service.ServiceInstanceBindingService;
import org.springframework.credhub.core.CredHubOperations;
import org.springframework.credhub.support.CredentialDetails;
import org.springframework.credhub.support.ServiceInstanceCredentialName;
import org.springframework.credhub.support.json.JsonCredential;
import org.springframework.credhub.support.json.JsonCredentialRequest;
import org.springframework.credhub.support.permissions.CredentialPermission;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class CredhubServiceInstanceBindingService
		implements ServiceInstanceBindingService {
	private static final String URI_KEY = "uri";
	private static final String USERNAME_KEY = "username";
	private static final String PASSWORD_KEY = "password";

	private final ServiceBindingRepository bindingRepository;
	private final UserService userService;
	private final ApplicationInformation applicationInformation;
	private final CredHubOperations credHubOperations;

	public CredhubServiceInstanceBindingService(
			ServiceBindingRepository bindingRepository, UserService userService,
			CredHubOperations credHubOperations,
			ApplicationInformation applicationInformation) {
		this.bindingRepository = bindingRepository;
		this.userService = userService;
		this.applicationInformation = applicationInformation;
		this.credHubOperations = credHubOperations;
	}

	@Override
	public CreateServiceInstanceBindingResponse createServiceInstanceBinding(
			CreateServiceInstanceBindingRequest request) {
		CreateServiceInstanceAppBindingResponseBuilder responseBuilder = CreateServiceInstanceAppBindingResponse
				.builder();

		Optional<ServiceBinding> binding = bindingRepository
				.findById(request.getBindingId());

		if (binding.isPresent()) {
			responseBuilder.bindingExisted(true)
					.credentials(binding.get().getCredentials());
		}
		else {
			User user = createUser(request);
			CredentialDetails<JsonCredential> credhubResponse = secureCredentials(request,
					user);
			Map<String, Object> credMap = new HashMap<String, Object>() {
				{
					put("credhub-ref", credhubResponse.getName().getName());
				}
			};
			saveBinding(request, credMap);
			responseBuilder.bindingExisted(false).credentials(credMap);
		}
		return responseBuilder.build();
	}

	@Override
	public GetServiceInstanceBindingResponse getServiceInstanceBinding(
			GetServiceInstanceBindingRequest request) {
		String bindingId = request.getBindingId();
		Optional<ServiceBinding> serviceBinding = bindingRepository.findById(bindingId);
		if (serviceBinding.isPresent()) {
			return GetServiceInstanceAppBindingResponse.builder()
					.parameters(serviceBinding.get().getParameters())
					.credentials(serviceBinding.get().getCredentials()).build();
		}
		else {
			throw new ServiceInstanceBindingDoesNotExistException(bindingId);
		}
	}

	@Override
	public void deleteServiceInstanceBinding(
			DeleteServiceInstanceBindingRequest request) {
		String bindingId = request.getBindingId();

		if (bindingRepository.existsById(bindingId)) {
			userService.deleteUser(bindingId);
			credHubOperations.deleteByName(ServiceInstanceCredentialName.builder()
					.serviceBrokerName(request.getServiceInstanceId())
					.serviceOfferingName(request.getPlanId())
					.serviceBindingId(request.getBindingId())
					.credentialName(request.getBindingId()).build());
			bindingRepository.deleteById(bindingId);
		}
		else {
			throw new ServiceInstanceBindingDoesNotExistException(bindingId);
		}
	}

	private User createUser(CreateServiceInstanceBindingRequest request) {
		return userService.createUser(request.getBindingId(), SecurityAuthorities.FULL_ACCESS.toString(),
				request.getServiceInstanceId());
	}

	private CredentialDetails<JsonCredential> secureCredentials(
			CreateServiceInstanceBindingRequest request, User user) {
		String uri = buildUri(request.getServiceInstanceId());

		Map<String, Object> credentials = new HashMap<>();
		credentials.put(URI_KEY, uri);
		credentials.put(USERNAME_KEY, user.getUsername());
		credentials.put(PASSWORD_KEY, user.getPassword());

		// @formatter:off
		JsonCredentialRequest credhubRequest = JsonCredentialRequest.builder()
			.overwrite(true)
			.value(credentials)
			.permission(CredentialPermission.builder().app(request.getBindResource().getAppGuid())
			.operations(READ).build())
			.name(ServiceInstanceCredentialName.builder()
			.serviceBrokerName(request.getServiceInstanceId())
			.serviceOfferingName(request.getPlanId())
			.serviceBindingId(request.getBindingId())
			.credentialName(request.getBindingId()).build())
			.build();
		// @formatter:on

		return credHubOperations.write(credhubRequest);
	}

	private String buildUri(String instanceId) {
		return UriComponentsBuilder.fromUriString(applicationInformation.getBaseUrl())
				.pathSegment(instanceId).build().toUriString();
	}

	private void saveBinding(CreateServiceInstanceBindingRequest request,
			Map<String, Object> credentials) {
		ServiceBinding serviceBinding = new ServiceBinding(request.getBindingId(),
				request.getParameters(), credentials);
		bindingRepository.save(serviceBinding);
	}
}
