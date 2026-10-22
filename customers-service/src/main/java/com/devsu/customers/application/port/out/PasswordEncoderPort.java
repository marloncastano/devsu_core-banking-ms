package com.devsu.customers.application.port.out;

public interface PasswordEncoderPort {

	String encode(String rawPassword);
}
