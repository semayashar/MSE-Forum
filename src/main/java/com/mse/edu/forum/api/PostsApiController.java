package com.mse.edu.forum.api;

import com.mse.edu.forum.api.model.CreatePostRequest;
import com.mse.edu.forum.api.model.PostDetailsResponse;
import com.mse.edu.forum.api.model.PostResponse;
import com.mse.edu.forum.api.model.UpdatePostRequest;
import com.mse.edu.forum.service.PostService;
import jakarta.validation.Valid;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PostsApiController {

	private static final Logger log = LogManager.getLogger(PostsApiController.class);

	private final PostService postService;

	public PostsApiController(PostService postService) {
		this.postService = postService;
	}

	@GetMapping("/posts")
	public ResponseEntity<List<PostResponse>> listPosts() {
		log.debug("listPosts invoked");
		return ResponseEntity.ok(postService.findAll());
	}

	@GetMapping("/posts/{id}")
	public ResponseEntity<PostDetailsResponse> getPostById(
			@PathVariable Long id,
			@RequestParam(defaultValue = "0") Integer page,
			@RequestParam(defaultValue = "10") Integer size) {
		log.debug("getPostById invoked id={}", id);
		return postService
				.findById(id, page, size)
				.map(ResponseEntity::ok)
				.orElseGet(() -> ResponseEntity.notFound().build());
	}

	@PreAuthorize("isAuthenticated()")
	@PostMapping("/posts")
	public ResponseEntity<PostResponse> createPost(@Valid @RequestBody CreatePostRequest createPostRequest) {
		log.debug("createPost invoked title={}", createPostRequest.getTitle());
		PostResponse created = postService.create(createPostRequest);
		return ResponseEntity.status(HttpStatus.CREATED).body(created);
	}

	@PreAuthorize("isAuthenticated()")
	@PutMapping("/posts/{id}")
	public ResponseEntity<PostResponse> updatePost(
			@PathVariable Long id, @Valid @RequestBody UpdatePostRequest updatePostRequest) {
		log.debug("updatePost invoked id={}", id);
		return postService
				.update(id, updatePostRequest)
				.map(ResponseEntity::ok)
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
}
