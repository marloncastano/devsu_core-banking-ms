package com.devsu.customers.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import com.devsu.customers.domain.exception.DomainValidationException;

class PersonalDataTest {

	private static final LocalDate BIRTH_DATE = LocalDate.of(1990, 5, 14);

	@Test
	@DisplayName("trims surrounding whitespace from text fields")
	void trimsTextFields() {
		PersonalData data = new PersonalData("  Jose Lema ", Gender.MALE, BIRTH_DATE, " 1712345678 ",
				" Otavalo sn y principal ", " 098254785 ");

		assertThat(data.name()).isEqualTo("Jose Lema");
		assertThat(data.identification()).isEqualTo("1712345678");
		assertThat(data.address()).isEqualTo("Otavalo sn y principal");
		assertThat(data.phone()).isEqualTo("098254785");
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = "   ")
	@DisplayName("rejects a missing name")
	void rejectsBlankName(String name) {
		assertThatThrownBy(() -> new PersonalData(name, Gender.MALE, BIRTH_DATE, "1712345678", "Otavalo", "098254785"))
				.isInstanceOf(DomainValidationException.class)
				.hasMessage("Field name is required");
	}

	@Test
	@DisplayName("rejects a missing gender")
	void rejectsMissingGender() {
		assertThatThrownBy(() -> new PersonalData("Jose Lema", null, BIRTH_DATE, "1712345678", "Otavalo", "098254785"))
				.isInstanceOf(DomainValidationException.class)
				.hasMessage("Gender is required");
	}

	@Test
	@DisplayName("rejects a missing birth date")
	void rejectsMissingBirthDate() {
		assertThatThrownBy(() -> new PersonalData("Jose Lema", Gender.MALE, null, "1712345678", "Otavalo", "098254785"))
				.isInstanceOf(DomainValidationException.class)
				.hasMessage("Birth date is required");
	}
}
