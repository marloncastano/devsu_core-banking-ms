package com.devsu.customers.infrastructure.adapter.in.rest;

import java.net.URI;
import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.devsu.customers.application.pagination.PageQuery;
import com.devsu.customers.application.port.in.ManageCustomerUseCase;
import com.devsu.customers.domain.model.Customer;
import com.devsu.customers.infrastructure.adapter.in.rest.dto.CreateCustomerRequest;
import com.devsu.customers.infrastructure.adapter.in.rest.dto.CustomerResponse;
import com.devsu.customers.infrastructure.adapter.in.rest.dto.PageResponse;
import com.devsu.customers.infrastructure.adapter.in.rest.dto.PatchCustomerRequest;
import com.devsu.customers.infrastructure.adapter.in.rest.dto.UpdateCustomerRequest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * The resource path is kept in Spanish because it is part of the required API contract.
 */
@RestController
@RequestMapping("/api/clientes")
public class CustomerController {

	private final ManageCustomerUseCase manageCustomer;
	private final Clock clock;

	public CustomerController(ManageCustomerUseCase manageCustomer, Clock clock) {
		this.manageCustomer = manageCustomer;
		this.clock = clock;
	}

	@PostMapping
	public ResponseEntity<CustomerResponse> create(@Valid @RequestBody CreateCustomerRequest request) {
		Customer customer = manageCustomer.create(CustomerRestMapper.toCommand(request));
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
				.path("/{customerId}")
				.buildAndExpand(customer.getCustomerId())
				.toUri();
		return ResponseEntity.created(location).body(toResponse(customer));
	}

	@GetMapping
	public PageResponse<CustomerResponse> list(
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(PageQuery.MAX_SIZE) int size) {
		return PageResponse.from(manageCustomer.list(new PageQuery(page, size)).map(this::toResponse));
	}

	@GetMapping("/{customerId}")
	public CustomerResponse getByCustomerId(@PathVariable UUID customerId) {
		return toResponse(manageCustomer.getByCustomerId(customerId));
	}

	@PutMapping("/{customerId}")
	public CustomerResponse update(@PathVariable UUID customerId, @Valid @RequestBody UpdateCustomerRequest request) {
		return toResponse(manageCustomer.update(customerId, CustomerRestMapper.toCommand(request)));
	}

	@PatchMapping("/{customerId}")
	public CustomerResponse patch(@PathVariable UUID customerId, @Valid @RequestBody PatchCustomerRequest request) {
		return toResponse(manageCustomer.patch(customerId, CustomerRestMapper.toCommand(request)));
	}

	@DeleteMapping("/{customerId}")
	public ResponseEntity<Void> delete(@PathVariable UUID customerId) {
		manageCustomer.delete(customerId);
		return ResponseEntity.noContent().build();
	}

	private CustomerResponse toResponse(Customer customer) {
		return CustomerRestMapper.toResponse(customer, LocalDate.now(clock));
	}
}
