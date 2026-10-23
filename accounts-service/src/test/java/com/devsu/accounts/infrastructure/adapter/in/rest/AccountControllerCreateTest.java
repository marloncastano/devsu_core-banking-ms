package com.devsu.accounts.infrastructure.adapter.in.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.devsu.accounts.application.port.in.ManageAccountUseCase;
import com.devsu.accounts.application.port.in.OpenAccountCommand;
import com.devsu.accounts.domain.exception.CustomerNotAvailableException;
import com.devsu.accounts.domain.model.Account;
import com.devsu.accounts.domain.model.AccountType;

/**
 * Web layer slice test for POST /api/cuentas: JSON binding, validation, error mapping and response shape.
 */
@WebMvcTest(AccountController.class)
class AccountControllerCreateTest {

	private static final UUID CUSTOMER_ID = UUID.fromString("7a7043f4-d803-4a06-961b-efe46efa60de");

	private static final String VALID_BODY = """
			{
			  "accountType": "CHECKING",
			  "initialBalance": 1000,
			  "customerId": "7a7043f4-d803-4a06-961b-efe46efa60de"
			}
			""";

	@Autowired
	private MockMvcTester mvc;

	@MockitoBean
	private ManageAccountUseCase manageAccount;

	@Test
	@DisplayName("valid body returns 201 with location and the opened account")
	void createReturnsOpenedAccount() {
		when(manageAccount.open(any(OpenAccountCommand.class))).thenReturn(Account.restore(1L, "100000",
				AccountType.CHECKING, new BigDecimal("1000"), new BigDecimal("1000"), true, CUSTOMER_ID));

		MvcTestResult result = post(VALID_BODY);

		assertThat(result).hasStatus(HttpStatus.CREATED);
		assertThat(result).headers().hasValue("Location", "http://localhost/api/cuentas/100000");
		assertThat(result).bodyJson().extractingPath("$.accountNumber").isEqualTo("100000");
		assertThat(result).bodyJson().extractingPath("$.availableBalance").isEqualTo(1000.00);
		assertThat(result).bodyJson().extractingPath("$.active").isEqualTo(true);
	}

	@Test
	@DisplayName("invalid fields return 400 listing every field error and never reach the use case")
	void createRejectsInvalidFields() {
		MvcTestResult result = post("""
				{
				  "accountType": "SAVINGS",
				  "initialBalance": -5,
				  "customerId": null
				}
				""");

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(result).bodyJson().extractingPath("$.errors.initialBalance")
				.isEqualTo("must be greater than or equal to 0");
		assertThat(result).bodyJson().extractingPath("$.errors.customerId").isEqualTo("must not be null");
		verifyNoInteractions(manageAccount);
	}

	@Test
	@DisplayName("more than two decimals in the initial balance return 400")
	void createRejectsTooManyDecimals() {
		MvcTestResult result = post(VALID_BODY.replace("1000", "10.555"));

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(result).bodyJson().extractingPath("$.errors.initialBalance").asString()
				.contains("out of bounds");
		verifyNoInteractions(manageAccount);
	}

	@Test
	@DisplayName("invalid account type returns 400 with the allowed values")
	void createRejectsInvalidAccountType() {
		MvcTestResult result = post(VALID_BODY.replace("CHECKING", "CORRIENTE"));

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(result).bodyJson().extractingPath("$.detail")
				.isEqualTo("Invalid value 'CORRIENTE' for field 'accountType'. Allowed values: [SAVINGS, CHECKING]");
	}

	@Test
	@DisplayName("unknown or inactive customer returns 422")
	void createReturnsUnprocessableForUnavailableCustomer() {
		when(manageAccount.open(any(OpenAccountCommand.class)))
				.thenThrow(CustomerNotAvailableException.notFound(CUSTOMER_ID));

		MvcTestResult result = post(VALID_BODY);

		assertThat(result).hasStatus(HttpStatus.UNPROCESSABLE_CONTENT);
		assertThat(result).bodyJson().extractingPath("$.detail")
				.isEqualTo("Customer " + CUSTOMER_ID + " does not exist");
	}

	@Test
	@DisplayName("a client-supplied account number is rejected because the bank assigns it")
	void createRejectsClientSuppliedAccountNumber() {
		MvcTestResult result = post(VALID_BODY.replace("{", "{ \"accountNumber\": \"585545\","));

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(result).bodyJson().extractingPath("$.detail").isEqualTo("Unknown field 'accountNumber'");
		verifyNoInteractions(manageAccount);
	}

	private MvcTestResult post(String body) {
		return mvc.post().uri("/api/cuentas").contentType(MediaType.APPLICATION_JSON).content(body).exchange();
	}
}
