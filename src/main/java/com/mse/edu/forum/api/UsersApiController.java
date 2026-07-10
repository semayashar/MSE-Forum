package com.mse.edu.forum.api;

import com.mse.edu.forum.api.model.CreateUserRequest;
import com.mse.edu.forum.api.model.UpdateUserRequest;
import com.mse.edu.forum.api.model.UserResponse;
import com.mse.edu.forum.service.UserService;
import jakarta.validation.Valid;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UsersApiController {

	private static final Logger log = LogManager.getLogger(UsersApiController.class);

	private final UserService userService;

	public UsersApiController(UserService userService) {
		this.userService = userService;
	}

	@PreAuthorize("hasAnyRole('ADMIN','MODERATOR')")
	@GetMapping("/users")
	public ResponseEntity<List<UserResponse>> listUsers() {
		log.debug("listUsers invoked");
		return ResponseEntity.ok(userService.findAll());
	}

	@PreAuthorize("hasAnyRole('ADMIN','MODERATOR') or @userSecurity.isSelf(#id)")
	@GetMapping("/users/{id}")
	public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
		log.debug("getUserById invoked id={}", id);
		return userService
				.findById(id)
				.map(ResponseEntity::ok)
				.orElseGet(() -> ResponseEntity.notFound().build());
	}

	@PostMapping("/users")
	public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest createUserRequest) {
		log.debug("createUser invoked username={}", createUserRequest.getUsername());
		UserResponse created = userService.create(createUserRequest);
		return ResponseEntity.status(HttpStatus.CREATED).body(created);
	}

	@PreAuthorize("hasRole('ADMIN') or @userSecurity.isSelf(#id)")
	@PutMapping("/users/{id}")
	public ResponseEntity<UserResponse> updateUser(
			@PathVariable Long id, @Valid @RequestBody UpdateUserRequest updateUserRequest) {
		log.debug("updateUser invoked id={}", id);
		return userService
				.update(id, updateUserRequest)
				.map(ResponseEntity::ok)
				.orElseGet(() -> ResponseEntity.notFound().build());
	}

	@PreAuthorize("hasRole('ADMIN')")
	@DeleteMapping("/users/{id}")
	public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
		log.debug("deleteUser invoked id={}", id);
		if (userService.delete(id)) {
			return ResponseEntity.noContent().build();
		}
		return ResponseEntity.notFound().build();
	}
}
