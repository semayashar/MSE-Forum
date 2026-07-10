package com.mse.edu.forum.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

@Configuration
public class ApiDocumentationConfig {

	@Bean
	public OpenAPI apiDocumentation() throws IOException {
		var resource = new ClassPathResource("api/forum.yaml");
		if (!resource.exists()) {
			throw new IllegalStateException("Missing classpath resource: api/forum.yaml");
		}
		String yaml = new String(resource.getContentAsByteArray(), StandardCharsets.UTF_8);
		var result = new OpenAPIV3Parser().readContents(yaml, null, null);
		if (result.getOpenAPI() == null) {
			throw new IllegalStateException("Failed to parse API spec: " + result.getMessages());
		}
		return result.getOpenAPI();
	}
}
