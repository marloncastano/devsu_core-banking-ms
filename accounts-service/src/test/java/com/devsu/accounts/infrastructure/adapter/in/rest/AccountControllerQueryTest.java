package com.devsu.accounts.infrastructure.adapter.in.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.devsu.accounts.application.pagination.PageQuery;
import com.devsu.accounts.application.pagination.PageResult;
import com.devsu.accounts.application.port.in.ManageAccountUseCase;
import com.devsu.accounts.domain.exception.AccountNotFoundException;
import com.devsu.accounts.domain.model.Account;
import com.devsu.accounts.domain.model.AccountType;

/**
 * Web layer slice test for GET /api/cuentas and GET /api/cuentas/{accountNumber}.
 */
@WebMvcTest(AccountController.class)
class AccountControllerQueryTest {

	private static final UUID CUSTOMER_ID = UUID.fromString("7a7043f4-d803-4a06-961b-efe46efa60de");
	private static final Account ACCOUNT = Account.restore(1L, "100000", AccountType.SAVINGS, new BigDecimal("2000"),
			new BigDecimal("1425"), true, CUSTOMER_ID);

	@Autowired
	private MockMvcTester mvc;

	@MockitoBean
	private ManageAccountUseCase manageAccount;

	@Test
	@DisplayName("get returns the account with its current balance")
	void getReturnsAccount() {
		when(manageAccount.getByAccountNumber("100000")).thenReturn(ACCOUNT);

		MvcTestResult result = mvc.get().uri("/api/cuentas/100000").exchange();

		assertThat(result).hasStatus(HttpStatus.OK);
		assertThat(result).bodyJson().extractingPath("$.accountNumber").isEqualTo("100000");
		assertThat(result).bodyJson().extractingPath("$.initialBalance").isEqualTo(2000.00);
		assertThat(result).bodyJson().extractingPath("$.availableBalance").isEqualTo(1425.00);
	}

	@Test
	@DisplayName("get of an unknown account returns 404")
	void getReturnsNotFound() {
		when(manageAccount.getByAccountNumber("999999")).thenThrow(new AccountNotFoundException("999999"));

		MvcTestResult result = mvc.get().uri("/api/cuentas/999999").exchange();

		assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
		assertThat(result).bodyJson().extractingPath("$.detail").isEqualTo("Account not found with number 999999");
	}

	@Test
	@DisplayName("list without parameters uses page 0, size 20 and no customer filter")
	void listUsesDefaults() {
		when(manageAccount.list(new PageQuery(0, 20), null)).thenReturn(new PageResult<>(List.of(ACCOUNT), 0, 20, 1));

		MvcTestResult result = mvc.get().uri("/api/cuentas").exchange();

		assertThat(result).hasStatus(HttpStatus.OK);
		assertThat(result).bodyJson().extractingPath("$.content[0].accountNumber").isEqualTo("100000");
		assertThat(result).bodyJson().extractingPath("$.totalElements").isEqualTo(1);
	}

	@Test
	@DisplayName("list can be filtered by customer")
	void listFiltersByCustomer() {
		when(manageAccount.list(new PageQuery(0, 5), CUSTOMER_ID))
				.thenReturn(new PageResult<>(List.of(ACCOUNT), 0, 5, 1));

		MvcTestResult result = mvc.get().uri("/api/cuentas?size=5&customerId=" + CUSTOMER_ID).exchange();

		assertThat(result).hasStatus(HttpStatus.OK);
		assertThat(result).bodyJson().extractingPath("$.content[0].customerId").isEqualTo(CUSTOMER_ID.toString());
	}

	@Test
	@DisplayName("size above the maximum returns 400 and never reaches the use case")
	void listRejectsSizeAboveMaximum() {
		MvcTestResult result = mvc.get().uri("/api/cuentas?size=500").exchange();

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(result).bodyJson().extractingPath("$.errors.size").isEqualTo("must be less than or equal to 100");
		verifyNoInteractions(manageAccount);
	}

	@Test
	@DisplayName("an invalid customer filter returns 400")
	void listRejectsInvalidCustomerId() {
		MvcTestResult result = mvc.get().uri("/api/cuentas?customerId=abc").exchange();

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		verifyNoInteractions(manageAccount);
	}
}
