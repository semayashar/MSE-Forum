package com.mse.edu.forum.api.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class LoginRequest {

	@NotBlank
	@Size(max = 100)
	private String username;

	@NotBlank
	@Size(max = 128)
	private String password;
}
