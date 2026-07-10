package com.mse.edu.forum.api.model;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@JsonPropertyOrder({"id", "username", "role"})
public class UserSummary {

	private Long id;
	private String username;
	private UserRole role;
}
