package com.devsu.accounts.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.dao.TransientDataAccessResourceException;

class CustomerEventsTopologyTest {

	@Test
	@DisplayName("transient failures such as a database hiccup are retried")
	void retriesTransientFailures() {
		assertThat(CustomerEventsTopology.isRetryable(
				new RuntimeException(new TransientDataAccessResourceException("connection reset")))).isTrue();
	}

	@Test
	@DisplayName("rejected or unreadable messages are not retried, even when wrapped by the listener container")
	void doesNotRetryUnprocessableMessages() {
		assertThat(CustomerEventsTopology.isRetryable(
				new RuntimeException(new AmqpRejectAndDontRequeueException("incomplete")))).isFalse();
		assertThat(CustomerEventsTopology.isRetryable(new MessageConversionException("bad json"))).isFalse();
	}
}
