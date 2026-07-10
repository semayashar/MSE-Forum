package com.mse.edu.forum.api.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CreateUserRequest {

	@NotBlank
	@Size(max = 100)
	private String username;

	@Email
	@Size(max = 320)
	private String email;

	private UserRole role;

	@NotBlank
	@Size(min = 8, max = 72)
	private String password;
}
