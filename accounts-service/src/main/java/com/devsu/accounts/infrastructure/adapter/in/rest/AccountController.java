package com.devsu.accounts.infrastructure.adapter.in.rest;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.devsu.accounts.application.port.in.ManageAccountUseCase;
import com.devsu.accounts.domain.model.Account;
import com.devsu.accounts.infrastructure.adapter.in.rest.dto.AccountResponse;
import com.devsu.accounts.infrastructure.adapter.in.rest.dto.CreateAccountRequest;

import jakarta.validation.Valid;

/**
 * The resource path is kept in Spanish because it is part of the required API contract.
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
}
