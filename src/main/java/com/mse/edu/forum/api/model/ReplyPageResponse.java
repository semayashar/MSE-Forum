package com.mse.edu.forum.api.model;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@JsonPropertyOrder({"items", "page", "size", "totalItems", "totalPages"})
public class ReplyPageResponse {

	private List<ReplyResponse> items;
	private Integer page;
	private Integer size;
	private Long totalItems;
	private Integer totalPages;

	public ReplyPageResponse items(List<ReplyResponse> items) {
		this.items = items;
		return this;
	}

	public ReplyPageResponse page(Integer page) {
		this.page = page;
		return this;
	}

	public ReplyPageResponse size(Integer size) {
		this.size = size;
		return this;
	}

	public ReplyPageResponse totalItems(Long totalItems) {
		this.totalItems = totalItems;
		return this;
	}

	public ReplyPageResponse totalPages(Integer totalPages) {
		this.totalPages = totalPages;
		return this;
	}
}
