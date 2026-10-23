package com.devsu.accounts.application.port.out;

import com.devsu.accounts.domain.model.Account;

public interface AccountRepositoryPort {

	Account save(Account account);
}
