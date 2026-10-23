package com.devsu.customers.infrastructure.adapter.in.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.devsu.customers.application.pagination.PageQuery;
import com.devsu.customers.application.pagination.PageResult;
import com.devsu.customers.application.port.in.ManageCustomerUseCase;
import com.devsu.customers.domain.model.Customer;
import com.devsu.customers.domain.model.Gender;
import com.devsu.customers.domain.model.PersonalData;

/**
 * Web layer slice test for GET /api/clientes pagination.
 */
@WebMvcTest(CustomerController.class)
@Import(CustomerControllerListTest.FixedClockConfig.class)
class CustomerControllerListTest {

	private static final LocalDate TODAY = LocalDate.of(2026, 10, 22);
	private static final UUID CUSTOMER_ID = UUID.fromString("7a7043f4-d803-4a06-961b-efe46efa60de");

	@Autowired
	private MockMvcTester mvc;

	@MockitoBean
	private ManageCustomerUseCase manageCustomer;

	@Test
	@DisplayName("without parameters uses page 0 and size 20")
	void listUsesDefaultPagination() {
		when(manageCustomer.list(new PageQuery(0, 20))).thenReturn(new PageResult<>(List.of(), 0, 20, 0));

		MvcTestResult result = mvc.get().uri("/api/clientes").exchange();

		assertThat(result).hasStatus(HttpStatus.OK);
		verify(manageCustomer).list(new PageQuery(0, 20));
	}

	@Test
	@DisplayName("returns the requested page with its metadata")
	void listReturnsPageWithMetadata() {
		when(manageCustomer.list(new PageQuery(1, 2)))
				.thenReturn(new PageResult<>(List.of(customer()), 1, 2, 5));

		MvcTestResult result = mvc.get().uri("/api/clientes?page=1&size=2").exchange();

		assertThat(result).hasStatus(HttpStatus.OK);
		assertThat(result).bodyJson().extractingPath("$.content[0].customerId").isEqualTo(CUSTOMER_ID.toString());
		assertThat(result).bodyJson().extractingPath("$.content[0].age").isEqualTo(36);
		assertThat(result).bodyJson().doesNotHavePath("$.content[0].password");
		assertThat(result).bodyJson().extractingPath("$.page").isEqualTo(1);
		assertThat(result).bodyJson().extractingPath("$.size").isEqualTo(2);
		assertThat(result).bodyJson().extractingPath("$.totalElements").isEqualTo(5);
		assertThat(result).bodyJson().extractingPath("$.totalPages").isEqualTo(3);
	}

	@Test
	@DisplayName("size above the maximum returns 400 and never reaches the use case")
	void listRejectsSizeAboveMaximum() {
		MvcTestResult result = mvc.get().uri("/api/clientes?size=500").exchange();

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(result).bodyJson().extractingPath("$.errors.size").isEqualTo("must be less than or equal to 100");
		verifyNoInteractions(manageCustomer);
	}

	@Test
	@DisplayName("negative page returns 400")
	void listRejectsNegativePage() {
		MvcTestResult result = mvc.get().uri("/api/clientes?page=-1").exchange();

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(result).bodyJson().extractingPath("$.errors.page").isEqualTo("must be greater than or equal to 0");
		verifyNoInteractions(manageCustomer);
	}

	private static Customer customer() {
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
