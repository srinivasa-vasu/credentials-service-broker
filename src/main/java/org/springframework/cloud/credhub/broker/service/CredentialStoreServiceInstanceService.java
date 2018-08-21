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

import java.util.Optional;

import org.springframework.cloud.credhub.broker.model.ServiceInstance;
import org.springframework.cloud.credhub.broker.repository.ServiceInstanceRepository;
import org.springframework.cloud.servicebroker.exception.ServiceInstanceDoesNotExistException;
import org.springframework.cloud.servicebroker.model.instance.*;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceResponse.CreateServiceInstanceResponseBuilder;
import org.springframework.cloud.servicebroker.service.ServiceInstanceService;
import org.springframework.stereotype.Service;

@Service
public class CredentialStoreServiceInstanceService implements ServiceInstanceService {
	private final ServiceInstanceRepository instanceRepository;

	public CredentialStoreServiceInstanceService(
			ServiceInstanceRepository instanceRepository) {
		this.instanceRepository = instanceRepository;
	}

	@Override
	public CreateServiceInstanceResponse createServiceInstance(
			CreateServiceInstanceRequest request) {
		String instanceId = request.getServiceInstanceId();

		CreateServiceInstanceResponseBuilder responseBuilder = CreateServiceInstanceResponse
				.builder();

		if (instanceRepository.existsById(instanceId)) {
			responseBuilder.instanceExisted(true);
		}
		else {
			saveInstance(request, instanceId);
		}

		return responseBuilder.build();
	}

	@Override
	public GetServiceInstanceResponse getServiceInstance(
			GetServiceInstanceRequest request) {
		String instanceId = request.getServiceInstanceId();

		Optional<ServiceInstance> serviceInstance = instanceRepository
				.findById(instanceId);

		if (serviceInstance.isPresent()) {
			return GetServiceInstanceResponse.builder()
					.serviceDefinitionId(serviceInstance.get().getServiceDefinitionId())
					.planId(serviceInstance.get().getPlanId())
					.parameters(serviceInstance.get().getParameters()).build();
		}
		else {
			throw new ServiceInstanceDoesNotExistException(instanceId);
		}
	}

	@Override
	public DeleteServiceInstanceResponse deleteServiceInstance(
			DeleteServiceInstanceRequest request) {
		String instanceId = request.getServiceInstanceId();

		if (instanceRepository.existsById(instanceId)) {
			instanceRepository.deleteById(instanceId);

			return DeleteServiceInstanceResponse.builder().build();
		}
		else {
			throw new ServiceInstanceDoesNotExistException(instanceId);
		}
	}

	private void saveInstance(CreateServiceInstanceRequest request, String instanceId) {
		ServiceInstance serviceInstance = new ServiceInstance(instanceId,
				request.getServiceDefinitionId(), request.getPlanId(),
				request.getParameters());
		instanceRepository.save(serviceInstance);
	}
}
