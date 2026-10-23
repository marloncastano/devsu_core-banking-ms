package com.devsu.accounts.application.service;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.devsu.accounts.application.port.in.SyncCustomerCommand;
import com.devsu.accounts.application.port.out.CustomerReferenceRepositoryPort;
import com.devsu.accounts.domain.model.CustomerReference;

@ExtendWith(MockitoExtension.class)
class CustomerReferenceServiceTest {

	private static final UUID CUSTOMER_ID = UUID.fromString("7a7043f4-d803-4a06-961b-efe46efa60de");
	private static final Instant OCCURRED_AT = Instant.parse("2026-10-22T15:00:00Z");

	@Mock
	private CustomerReferenceRepositoryPort customerReferenceRepository;

	@InjectMocks
	private CustomerReferenceService service;

	@Test
	@DisplayName("sync stores the customer state with the time the change happened")
	void syncStoresCustomerState() {
		CustomerReference expected = new CustomerReference(CUSTOMER_ID, "Jose Lema", true);
		when(customerReferenceRepository.saveIfNotOlder(expected, OCCURRED_AT)).thenReturn(true);

		service.sync(new SyncCustomerCommand(CUSTOMER_ID, "Jose Lema", true, OCCURRED_AT));

		verify(customerReferenceRepository).saveIfNotOlder(expected, OCCURRED_AT);
	}

	@Test
	@DisplayName("an outdated change is ignored without failing, so the message is not retried")
	void syncIgnoresOutdatedChange() {
		CustomerReference outdated = new CustomerReference(CUSTOMER_ID, "Old name", true);
		when(customerReferenceRepository.saveIfNotOlder(outdated, OCCURRED_AT)).thenReturn(false);

		service.sync(new SyncCustomerCommand(CUSTOMER_ID, "Old name", true, OCCURRED_AT));

		verify(customerReferenceRepository).saveIfNotOlder(outdated, OCCURRED_AT);
	}
}
