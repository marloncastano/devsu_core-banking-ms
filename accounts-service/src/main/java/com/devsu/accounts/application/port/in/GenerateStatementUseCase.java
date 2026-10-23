package com.devsu.accounts.application.port.in;

import com.devsu.accounts.application.report.AccountStatement;
import com.devsu.accounts.application.report.StatementQuery;

public interface GenerateStatementUseCase {

	AccountStatement generate(StatementQuery query);
}
