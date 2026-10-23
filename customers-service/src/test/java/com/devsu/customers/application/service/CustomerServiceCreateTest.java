package com.devsu.customers.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.devsu.customers.application.event.CustomerEvent;
import com.devsu.customers.application.port.in.CreateCustomerCommand;
import com.devsu.customers.application.port.out.CustomerEventPublisherPort;
import com.devsu.customers.application.port.out.CustomerRepositoryPort;
import com.devsu.customers.application.port.out.PasswordEncoderPort;
import com.devsu.customers.domain.exception.DomainValidationException;
import com.devsu.customers.domain.exception.DuplicateCustomerException;
import com.devsu.customers.domain.model.Customer;
import com.devsu.customers.domain.model.Gender;
import com.devsu.customers.domain.model.PersonalData;

@ExtendWith(MockitoExtension.class)
class CustomerServiceCreateTest {

	private static final LocalDate TODAY = LocalDate.of(2026, 10, 22);
	private static final Clock FIXED_CLOCK = Clock.fixed(TODAY.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

	@Mock
	private CustomerRepositoryPort customerRepository;

	@Mock
	private PasswordEncoderPort passwordEncoder;

	@Mock
	private CustomerEventPublisherPort eventPublisher;

	private CustomerService customerService;

	@BeforeEach
	void setUp() {
		customerService = new CustomerService(customerRepository, passwordEncoder, eventPublisher, FIXED_CLOCK);
	}

	@Test
	@DisplayName("create encodes the password, saves an active customer and publishes a created event")
	void createEncodesPasswordAndSavesCustomer() {
		when(customerRepository.existsByIdentification("1712345678")).thenReturn(false);
		when(passwordEncoder.encode("1234")).thenReturn("$2a$10$encoded");
		when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

		Customer created = customerService.create(command(LocalDate.of(1990, 5, 14)));

		ArgumentCaptor<Customer> saved = ArgumentCaptor.forClass(Customer.class);
		verify(customerRepository).save(saved.capture());
		assertThat(saved.getValue().getPassword()).isEqualTo("$2a$10$encoded");
		assertThat(saved.getValue().isActive()).isTrue();
		assertThat(saved.getValue().getCustomerId()).isNotNull();
		assertThat(saved.getValue().getPersonalData().identification()).isEqualTo("1712345678");
		assertThat(created).isSameAs(saved.getValue());

		ArgumentCaptor<CustomerEvent> published = ArgumentCaptor.forClass(CustomerEvent.class);
		verify(eventPublisher).publish(published.capture());
		assertThat(published.getValue().type()).isEqualTo(CustomerEvent.Type.CREATED);
		assertThat(published.getValue().customerId()).isEqualTo(created.getCustomerId());
		assertThat(published.getValue().name()).isEqualTo("Jose Lema");
		assertThat(published.getValue().active()).isTrue();
		assertThat(published.getValue().occurredAt()).isEqualTo(FIXED_CLOCK.instant());
	}

	@Test
	@DisplayName("create rejects a duplicated identification without encoding or saving")
	void createRejectsDuplicatedIdentification() {
		when(customerRepository.existsByIdentification("1712345678")).thenReturn(true);

		assertThatThrownBy(() -> customerService.create(command(LocalDate.of(1990, 5, 14))))
				.isInstanceOf(DuplicateCustomerException.class)
				.hasMessage("A person with identification 1712345678 already exists");

		verify(passwordEncoder, never()).encode(any());
		verify(customerRepository, never()).save(any());
		verify(eventPublisher, never()).publish(any());
	}

	@Test
	@DisplayName("create uses the injected clock to reject an underage customer without saving")
	void createRejectsUnderageCustomerUsingClock() {
		when(customerRepository.existsByIdentification("1712345678")).thenReturn(false);
		when(passwordEncoder.encode("1234")).thenReturn("$2a$10$encoded");

		assertThatThrownBy(() -> customerService.create(command(TODAY.minusYears(18).plusDays(1))))
				.isInstanceOf(DomainValidationException.class)
				.hasMessage("Customer must be at least 18 years old");

		verify(customerRepository, never()).save(any());
		verify(eventPublisher, never()).publish(any());
	}

	private static CreateCustomerCommand command(LocalDate birthDate) {
		PersonalData data = new PersonalData("Jose Lema", Gender.MALE, birthDate, "1712345678",
				"Otavalo sn y principal", "098254785");
		return new CreateCustomerCommand(data, "1234");
	}
}
