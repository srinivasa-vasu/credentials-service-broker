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

import java.security.SecureRandom;

import org.springframework.cloud.credhub.broker.model.User;
import org.springframework.cloud.credhub.broker.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import static org.springframework.cloud.credhub.broker.config.SecurityAuthorities.ADMIN;
import static org.springframework.cloud.credhub.broker.config.SecurityAuthorities.FULL_ACCESS;

@Service
public class UserService {
	private static final String PASSWORD_CHARS = "itsarandomcharsencodertext";
	private static final int PASSWORD_LENGTH = 12;

	private static final SecureRandom RANDOM = new SecureRandom();

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
	}

	public void initializeUsers() {
		if (userRepository.count() == 0) {
			userRepository.save(adminUser());
		}
	}

	public User createUser(String username, String... authorities) {
		String password = generatePassword();
		String encodedPassword = passwordEncoder.encode(password);

		userRepository.save(new User(username, encodedPassword, authorities));

		return new User(username, password, authorities);
	}

	public void deleteUser(String username) {
		User user = userRepository.findByUsername(username);
		if (user != null) {
			userRepository.deleteById(user.getId());
		}
	}

	private User adminUser() {
		return new User("admin", passwordEncoder.encode("admin"),
				ADMIN.toString(), FULL_ACCESS.toString());
	}

	private String generatePassword() {
		StringBuilder sb = new StringBuilder(PASSWORD_LENGTH);
		for (int i = 0; i < PASSWORD_LENGTH; i++) {
			sb.append(PASSWORD_CHARS.charAt(RANDOM.nextInt(PASSWORD_CHARS.length())));
		}
		return sb.toString();
	}
}
