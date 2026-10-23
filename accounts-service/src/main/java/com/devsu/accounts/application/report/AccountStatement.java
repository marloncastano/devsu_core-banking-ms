package com.devsu.accounts.application.report;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.devsu.accounts.domain.model.Account;
import com.devsu.accounts.domain.model.CustomerReference;
import com.devsu.accounts.domain.model.Movement;

/**
 * Account statement (F4): every account of the customer with its balances and its movements in the period.
 */
public record AccountStatement(CustomerReference customer, LocalDate from, LocalDate to, List<Entry> accounts) {

	public AccountStatement {
		accounts = List.copyOf(accounts);
	}

	/**
	 * @param movements movements of the account in the period, oldest first.
	 */
	public record Entry(Account account, List<Movement> movements) {

		public Entry {
			movements = List.copyOf(movements);
		}

		public BigDecimal totalCredits() {
			return movements.stream().map(Movement::amount).filter(amount -> amount.signum() > 0)
					.reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2);
		}

		public BigDecimal totalDebits() {
			return movements.stream().map(Movement::amount).filter(amount -> amount.signum() < 0)
					.map(BigDecimal::negate).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2);
		}
	}
}
