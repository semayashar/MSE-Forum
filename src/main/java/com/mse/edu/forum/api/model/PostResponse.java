package com.mse.edu.forum.api.model;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@JsonPropertyOrder({"id", "title", "content", "author", "createdAt", "updatedAt", "viewCount"})
public class PostResponse {

	private Long id;
	private String title;
	private String content;
	private UserSummary author;
	private OffsetDateTime createdAt;
	private OffsetDateTime updatedAt;
	private Long viewCount;
}
