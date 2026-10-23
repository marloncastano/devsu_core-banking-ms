package com.devsu.accounts.infrastructure.adapter.in.rest;

import java.net.URI;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.devsu.accounts.application.pagination.PageQuery;
import com.devsu.accounts.application.port.in.ManageAccountUseCase;
import com.devsu.accounts.domain.model.Account;
import com.devsu.accounts.infrastructure.adapter.in.rest.dto.AccountResponse;
import com.devsu.accounts.infrastructure.adapter.in.rest.dto.CreateAccountRequest;
import com.devsu.accounts.infrastructure.adapter.in.rest.dto.PageResponse;
import com.devsu.accounts.infrastructure.adapter.in.rest.dto.PatchAccountRequest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * The resource path is kept in Spanish because it is part of the required API contract.
 * Accounts are never deleted (CRU): they are deactivated through PATCH, keeping balance and history.
 */
@RestController
@RequestMapping("/api/cuentas")
public class AccountController {

	private final ManageAccountUseCase manageAccount;

	public AccountController(ManageAccountUseCase manageAccount) {
		this.manageAccount = manageAccount;
	}

	@PostMapping
	public ResponseEntity<AccountResponse> create(@Valid @RequestBody CreateAccountRequest request) {
		Account account = manageAccount.open(AccountRestMapper.toCommand(request));
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
				.path("/{accountNumber}")
				.buildAndExpand(account.getAccountNumber())
				.toUri();
		return ResponseEntity.created(location).body(AccountRestMapper.toResponse(account));
	}

	@GetMapping
	public PageResponse<AccountResponse> list(
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(PageQuery.MAX_SIZE) int size,
			@RequestParam(required = false) UUID customerId) {
		return PageResponse.from(manageAccount.list(new PageQuery(page, size), customerId)
				.map(AccountRestMapper::toResponse));
	}

	@GetMapping("/{accountNumber}")
	public AccountResponse getByAccountNumber(@PathVariable String accountNumber) {
		return AccountRestMapper.toResponse(manageAccount.getByAccountNumber(accountNumber));
	}

	@PatchMapping("/{accountNumber}")
	public AccountResponse patch(@PathVariable String accountNumber, @Valid @RequestBody PatchAccountRequest request) {
		return AccountRestMapper.toResponse(manageAccount.changeStatus(accountNumber, request.active()));
	}
}
