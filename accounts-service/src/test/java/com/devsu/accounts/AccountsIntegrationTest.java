package com.devsu.accounts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.devsu.accounts.application.port.in.ManageAccountUseCase;
import com.devsu.accounts.application.port.in.OpenAccountCommand;
import com.devsu.accounts.application.port.in.RegisterMovementCommand;
import com.devsu.accounts.application.port.in.RegisterMovementUseCase;
import com.devsu.accounts.application.port.out.CustomerReferenceRepositoryPort;
import com.devsu.accounts.domain.exception.InsufficientFundsException;
import com.devsu.accounts.domain.model.AccountType;
import com.devsu.accounts.infrastructure.config.CustomerEventsTopology;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * F6. Runs the whole service against real PostgreSQL and RabbitMQ containers: Flyway migrations, the RabbitMQ
 * consumer, the REST API, the row lock and the database constraints are all exercised for real.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AccountsIntegrationTest {

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private RabbitTemplate rabbitTemplate;

	@Autowired
	private CustomerReferenceRepositoryPort customerReferences;

	@Autowired
	private ManageAccountUseCase manageAccount;

	@Autowired
	private RegisterMovementUseCase registerMovement;

	@Autowired
	private JsonMapper jsonMapper;

	@Test
	@DisplayName("a customer event enables accounts, movements and the account statement end to end")
	void customerEventEnablesAccountMovementsAndStatement() {
		UUID customerId = publishCustomerCreated("Jose Lema");

		String accountNumber = json(post("/api/cuentas", """
				{ "accountType": "SAVINGS", "initialBalance": 2000, "customerId": "%s" }
				""".formatted(customerId), HttpStatus.CREATED)).get("accountNumber").asString();

		JsonNode withdrawal = json(post("/api/movimientos", """
				{ "accountNumber": "%s", "amount": -575 }
				""".formatted(accountNumber), HttpStatus.CREATED));
		assertThat(withdrawal.get("movementType").asString()).isEqualTo("WITHDRAWAL");
		assertThat(withdrawal.get("balance").decimalValue()).isEqualByComparingTo("1425");

		JsonNode rejected = json(post("/api/movimientos", """
				{ "accountNumber": "%s", "amount": -1425.01 }
				""".formatted(accountNumber), HttpStatus.UNPROCESSABLE_CONTENT));
		assertThat(rejected.get("detail").asString()).isEqualTo("Saldo no disponible");

		LocalDate today = LocalDate.now(ZoneId.systemDefault());
		MvcTestResult report = mvc.get()
				.uri("/api/reportes?fecha={from},{to}&cliente={customer}", today.minusDays(1), today, customerId)
				.exchange();
		assertThat(report).hasStatus(HttpStatus.OK);
		JsonNode account = json(report).get("accounts").get(0);
		assertThat(json(report).get("customerName").asString()).isEqualTo("Jose Lema");
		assertThat(account.get("initialBalance").decimalValue()).isEqualByComparingTo("2000");
		assertThat(account.get("availableBalance").decimalValue()).isEqualByComparingTo("1425");
		assertThat(account.get("totalDebits").decimalValue()).isEqualByComparingTo("575");
		assertThat(account.get("movements")).hasSize(1);
	}

	@Test
	@DisplayName("concurrent withdrawals on the same account never overdraw it")
	void concurrentWithdrawalsNeverOverdraw() throws Exception {
		UUID customerId = publishCustomerCreated("Juan Osorio");
		String accountNumber = manageAccount
				.open(new OpenAccountCommand(AccountType.SAVINGS, new BigDecimal("1000"), customerId))
				.getAccountNumber();

		List<Callable<Boolean>> withdrawals = new ArrayList<>();
		for (int i = 0; i < 20; i++) {
			withdrawals.add(() -> {
				try {
					registerMovement.register(new RegisterMovementCommand(accountNumber, new BigDecimal("-100"), null));
					return true;
				} catch (InsufficientFundsException ex) {
					return false;
				}
			});
		}
		ExecutorService pool = Executors.newFixedThreadPool(20);
		long approved;
		try {
			approved = 0;
			for (Future<Boolean> result : pool.invokeAll(withdrawals)) {
				if (result.get()) {
					approved++;
				}
			}
		} finally {
			pool.shutdown();
		}

		assertThat(approved).isEqualTo(10);
		assertThat(manageAccount.getByAccountNumber(accountNumber).getAvailableBalance())
				.isEqualByComparingTo(BigDecimal.ZERO);
	}

	/**
	 * Publishes the event exactly as customers-service does and waits until the consumer has stored the customer.
	 */
	private UUID publishCustomerCreated(String name) {
		UUID customerId = UUID.randomUUID();
		rabbitTemplate.convertAndSend(CustomerEventsTopology.EXCHANGE, "customer.created", Map.of(
				"eventId", UUID.randomUUID().toString(),
				"eventType", "CREATED",
				"customerId", customerId.toString(),
				"name", name,
				"active", true,
				"occurredAt", Instant.now().toString()));
		await().atMost(Duration.ofSeconds(10))
				.until(() -> customerReferences.findById(customerId).isPresent());
		return customerId;
	}

	private MvcTestResult post(String uri, String body, HttpStatus expectedStatus) {
		MvcTestResult result = mvc.post().uri(uri).contentType(MediaType.APPLICATION_JSON).content(body).exchange();
		assertThat(result).hasStatus(expectedStatus);
		return result;
	}

	private JsonNode json(MvcTestResult result) {
		return jsonMapper.readTree(result.getResponse().getContentAsByteArray());
	}
}
