package com.devsu.customers.infrastructure.adapter.in.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.devsu.customers.application.port.in.CreateCustomerCommand;
import com.devsu.customers.application.port.in.ManageCustomerUseCase;
import com.devsu.customers.domain.exception.DomainValidationException;
import com.devsu.customers.domain.exception.DuplicateCustomerException;
import com.devsu.customers.domain.model.Customer;
import com.devsu.customers.domain.model.Gender;
import com.devsu.customers.domain.model.PersonalData;

/**
 * Web layer slice test for POST /api/clientes: JSON binding, validation, error mapping and response shape.
 */
@WebMvcTest(CustomerController.class)
@Import(CustomerControllerCreateTest.FixedClockConfig.class)
class CustomerControllerCreateTest {

	private static final LocalDate TODAY = LocalDate.of(2026, 10, 22);
	private static final UUID CUSTOMER_ID = UUID.fromString("7a7043f4-d803-4a06-961b-efe46efa60de");

	private static final String VALID_BODY = """
			{
			  "name": "Jose Lema",
			  "gender": "MALE",
			  "birthDate": "1990-05-14",
			  "identification": "1712345678",
			  "address": "Otavalo sn y principal",
			  "phone": "098254785",
			  "password": "1234"
			}
			""";

	@Autowired
	private MockMvcTester mvc;

	@MockitoBean
	private ManageCustomerUseCase manageCustomer;

	@Test
	@DisplayName("valid body returns 201 with location, generated id and derived age, never the password")
	void createReturnsCreatedCustomer() {
		when(manageCustomer.create(any(CreateCustomerCommand.class))).thenReturn(savedCustomer());

		MvcTestResult result = post(VALID_BODY);

		assertThat(result).hasStatus(HttpStatus.CREATED);
		assertThat(result).headers().hasValue("Location", "http://localhost/api/clientes/" + CUSTOMER_ID);
		assertThat(result).bodyJson().extractingPath("$.customerId").isEqualTo(CUSTOMER_ID.toString());
		assertThat(result).bodyJson().extractingPath("$.age").isEqualTo(36);
		assertThat(result).bodyJson().extractingPath("$.active").isEqualTo(true);
		assertThat(result).bodyJson().doesNotHavePath("$.password");
	}

	@Test
	@DisplayName("invalid fields return 400 listing every field error and never reach the use case")
	void createRejectsInvalidFields() {
		MvcTestResult result = post("""
				{
				  "name": "",
				  "gender": "MALE",
				  "birthDate": "2030-01-01",
				  "identification": "12",
				  "address": "Otavalo",
				  "phone": "34E23432",
				  "password": "123"
				}
				""");

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(result).bodyJson().extractingPath("$.errors.name").isEqualTo("must not be blank");
		assertThat(result).bodyJson().extractingPath("$.errors.birthDate").isEqualTo("must be a past date");
		assertThat(result).bodyJson().extractingPath("$.errors.identification")
				.isEqualTo("must contain between 5 and 20 alphanumeric characters");
		assertThat(result).bodyJson().extractingPath("$.errors.phone")
				.isEqualTo("must contain between 7 and 15 digits, optionally prefixed with +");
		assertThat(result).bodyJson().extractingPath("$.errors.password").isEqualTo("must have at least 4 characters");
		verifyNoInteractions(manageCustomer);
	}

	@Test
	@DisplayName("password longer than 72 UTF-8 bytes returns 400 even if it has fewer than 72 characters")
	void createRejectsPasswordOverBcryptLimit() {
		String body = VALID_BODY.replace("\"1234\"", "\"" + "ñ".repeat(40) + "\"");

		MvcTestResult result = post(body);

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(result).bodyJson().extractingPath("$.errors.password")
				.isEqualTo("must not exceed 72 bytes when UTF-8 encoded");
		verifyNoInteractions(manageCustomer);
	}

	@Test
	@DisplayName("unknown fields such as a client-supplied customerId are rejected")
	void createRejectsUnknownField() {
		MvcTestResult result = post(VALID_BODY.replace("{", "{ \"customerId\": \"21321\","));

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(result).bodyJson().extractingPath("$.detail").isEqualTo("Unknown field 'customerId'");
		verifyNoInteractions(manageCustomer);
	}

	@Test
	@DisplayName("invalid enum value returns 400 with the allowed values")
	void createRejectsInvalidGender() {
		MvcTestResult result = post(VALID_BODY.replace("\"MALE\"", "\"FAME\""));

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(result).bodyJson().extractingPath("$.detail")
				.isEqualTo("Invalid value 'FAME' for field 'gender'. Allowed values: [MALE, FEMALE, OTHER]");
	}

	@Test
	@DisplayName("duplicated identification returns 409")
	void createReturnsConflictForDuplicatedIdentification() {
		when(manageCustomer.create(any(CreateCustomerCommand.class)))
				.thenThrow(DuplicateCustomerException.byIdentification("1712345678"));

		MvcTestResult result = post(VALID_BODY);

		assertThat(result).hasStatus(HttpStatus.CONFLICT);
		assertThat(result).bodyJson().extractingPath("$.detail")
				.isEqualTo("A person with identification 1712345678 already exists");
	}

	@Test
	@DisplayName("domain rule violations such as underage customers return 400")
	void createReturnsBadRequestForDomainViolation() {
		when(manageCustomer.create(any(CreateCustomerCommand.class)))
				.thenThrow(new DomainValidationException("Customer must be at least 18 years old"));

		MvcTestResult result = post(VALID_BODY);

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(result).bodyJson().extractingPath("$.detail").isEqualTo("Customer must be at least 18 years old");
	}

	private MvcTestResult post(String body) {
		return mvc.post().uri("/api/clientes").contentType(MediaType.APPLICATION_JSON).content(body).exchange();
	}

	private static Customer savedCustomer() {
		PersonalData data = new PersonalData("Jose Lema", Gender.MALE, LocalDate.of(1990, 5, 14), "1712345678",
				"Otavalo sn y principal", "098254785");
		return Customer.restore(1L, CUSTOMER_ID, data, "$2a$10$encoded", true);
	}

	@TestConfiguration
	static class FixedClockConfig {

		@Bean
		Clock clock() {
			return Clock.fixed(TODAY.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
		}
	}
}
