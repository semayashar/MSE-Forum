package com.mse.edu.forum.api.model;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonPropertyOrder({"accessToken", "tokenType", "expiresInSeconds"})
public class LoginResponse {

	private String accessToken;
	private String tokenType;
	private Long expiresInSeconds;
}
