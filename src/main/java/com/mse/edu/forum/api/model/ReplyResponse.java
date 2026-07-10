package com.mse.edu.forum.api.model;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@JsonPropertyOrder({"id", "postId", "content", "author", "createdAt", "updatedAt"})
public class ReplyResponse {

	private Long id;
	private Long postId;
	private String content;
	private UserSummary author;
	private OffsetDateTime createdAt;
	private OffsetDateTime updatedAt;
}
