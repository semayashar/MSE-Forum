package com.mse.edu.forum.api.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CreatePostRequest {

	@NotBlank
	@Size(max = 255)
	private String title;

	@Size(max = 10000)
	private String content;
}
