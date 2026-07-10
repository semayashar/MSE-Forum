package com.mse.edu.forum.api;

import com.mse.edu.forum.api.model.CreateReplyRequest;
import com.mse.edu.forum.api.model.ReplyPageResponse;
import com.mse.edu.forum.api.model.ReplyResponse;
import com.mse.edu.forum.api.model.UpdateReplyRequest;
import com.mse.edu.forum.service.ReplyService;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RepliesApiController {

	private static final Logger log = LogManager.getLogger(RepliesApiController.class);

	private final ReplyService replyService;

	public RepliesApiController(ReplyService replyService) {
		this.replyService = replyService;
	}

	@GetMapping("/posts/{postId}/replies")
	public ResponseEntity<ReplyPageResponse> listRepliesForPost(
			@PathVariable Long postId,
			@RequestParam(defaultValue = "0") Integer page,
			@RequestParam(defaultValue = "10") Integer size) {
		log.debug("listRepliesForPost postId={}", postId);
		return ResponseEntity.ok(replyService.findByPostId(postId, page, size));
	}

	@GetMapping("/replies/{id}")
	public ResponseEntity<ReplyResponse> getReplyById(@PathVariable Long id) {
		log.debug("getReplyById id={}", id);
		return replyService
				.findById(id)
				.map(ResponseEntity::ok)
				.orElseGet(() -> ResponseEntity.notFound().build());
	}

	@PreAuthorize("isAuthenticated()")
	@PostMapping("/posts/{postId}/replies")
	public ResponseEntity<ReplyResponse> createReply(
			@PathVariable Long postId, @Valid @RequestBody CreateReplyRequest createReplyRequest) {
		log.debug("createReply postId={}", postId);
		ReplyResponse created = replyService.create(postId, createReplyRequest);
		return ResponseEntity.status(HttpStatus.CREATED).body(created);
	}

	@PreAuthorize("isAuthenticated()")
	@PutMapping("/replies/{id}")
	public ResponseEntity<ReplyResponse> updateReply(
			@PathVariable Long id, @Valid @RequestBody UpdateReplyRequest updateReplyRequest) {
		log.debug("updateReply id={}", id);
		return replyService
				.update(id, updateReplyRequest)
				.map(ResponseEntity::ok)
				.orElseGet(() -> ResponseEntity.notFound().build());
	}

	@PreAuthorize("isAuthenticated()")
	@DeleteMapping("/replies/{id}")
	public ResponseEntity<Void> deleteReply(@PathVariable Long id) {
		log.debug("deleteReply id={}", id);
		return replyService.delete(id)
				? ResponseEntity.noContent().build()
				: ResponseEntity.notFound().build();
	}
}
