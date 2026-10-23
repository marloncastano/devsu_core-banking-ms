package com.devsu.customers.application.pagination;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PageResultTest {

	@ParameterizedTest(name = "{0} elements with size {1} -> {2} pages")
	@CsvSource({ "0, 20, 0", "1, 20, 1", "20, 20, 1", "21, 20, 2", "5, 2, 3" })
	@DisplayName("total pages rounds up the last partial page")
	void totalPagesRoundsUp(long totalElements, int size, int expectedPages) {
		assertThat(new PageResult<>(List.of(), 0, size, totalElements).totalPages()).isEqualTo(expectedPages);
	}

	@Test
	@DisplayName("map transforms the content and keeps the metadata")
	void mapKeepsMetadata() {
		PageResult<String> mapped = new PageResult<>(List.of(1, 2), 3, 2, 10).map(String::valueOf);

		assertThat(mapped.content()).containsExactly("1", "2");
		assertThat(mapped.page()).isEqualTo(3);
		assertThat(mapped.size()).isEqualTo(2);
		assertThat(mapped.totalElements()).isEqualTo(10);
	}

	@Test
	@DisplayName("page query rejects a size above the maximum")
	void pageQueryRejectsSizeAboveMaximum() {
		assertThatThrownBy(() -> new PageQuery(0, PageQuery.MAX_SIZE + 1)).isInstanceOf(IllegalArgumentException.class);
	}
}
