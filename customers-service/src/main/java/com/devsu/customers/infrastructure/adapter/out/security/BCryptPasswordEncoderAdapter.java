package com.devsu.customers.infrastructure.adapter.out.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.devsu.customers.application.port.out.PasswordEncoderPort;

@Component
class BCryptPasswordEncoderAdapter implements PasswordEncoderPort {

	private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

	@Override
	public String encode(String rawPassword) {
		return passwordEncoder.encode(rawPassword);
	}
}
