package com.devsu.accounts.application.port.in;

import com.devsu.accounts.domain.model.Account;

public interface ManageAccountUseCase {

	Account open(OpenAccountCommand command);
}
