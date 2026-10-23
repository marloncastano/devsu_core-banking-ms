package com.devsu.accounts.infrastructure.adapter.in.rest.dto;

import java.util.List;

import com.devsu.accounts.application.pagination.PageResult;

public record PageResponse<T>(
		List<T> content,
		int page,
		int size,
		long totalElements,
		int totalPages) {

	public static <T> PageResponse<T> from(PageResult<T> result) {
		return new PageResponse<>(result.content(), result.page(), result.size(), result.totalElements(),
				result.totalPages());
	}
}
