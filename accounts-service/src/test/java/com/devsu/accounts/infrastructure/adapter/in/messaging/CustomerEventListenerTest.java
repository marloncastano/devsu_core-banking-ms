package com.devsu.accounts.infrastructure.adapter.in.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;

import com.devsu.accounts.application.port.in.SyncCustomerCommand;
import com.devsu.accounts.application.port.in.SyncCustomerUseCase;

@ExtendWith(MockitoExtension.class)
class CustomerEventListenerTest {

	private static final UUID CUSTOMER_ID = UUID.fromString("7a7043f4-d803-4a06-961b-efe46efa60de");
	private static final Instant OCCURRED_AT = Instant.parse("2026-10-22T15:00:00Z");

	@Mock
	private SyncCustomerUseCase syncCustomer;

	private CustomerEventListener listener;

	@BeforeEach
	void setUp() {
		listener = new CustomerEventListener(syncCustomer);
	}

	@Test
	@DisplayName("a complete event is applied to the local copy of the customer")
	void appliesCompleteEvent() {
		listener.onCustomerEvent(new CustomerEventMessage(UUID.randomUUID(), "DEACTIVATED", CUSTOMER_ID, "Jose Lema",
				false, OCCURRED_AT));

		ArgumentCaptor<SyncCustomerCommand> command = ArgumentCaptor.forClass(SyncCustomerCommand.class);
		verify(syncCustomer).sync(command.capture());
		assertThat(command.getValue())
				.isEqualTo(new SyncCustomerCommand(CUSTOMER_ID, "Jose Lema", false, OCCURRED_AT));
	}

	@Test
	@DisplayName("an incomplete event is rejected without retry so it goes to the dead letter queue")
	void rejectsIncompleteEvent() {
		CustomerEventMessage withoutStatus = new CustomerEventMessage(UUID.randomUUID(), "CREATED", CUSTOMER_ID,
				"Jose Lema", null, OCCURRED_AT);

		assertThatThrownBy(() -> listener.onCustomerEvent(withoutStatus))
				.isInstanceOf(AmqpRejectAndDontRequeueException.class);
		verifyNoInteractions(syncCustomer);
	}
}
