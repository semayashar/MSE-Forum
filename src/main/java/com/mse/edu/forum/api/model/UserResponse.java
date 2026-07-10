package com.mse.edu.forum.api.model;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@JsonPropertyOrder({"id", "username", "email", "role", "createdAt"})
public class UserResponse {

	private Long id;
	private String username;
	private String email;
	private UserRole role;
	private OffsetDateTime createdAt;
}
