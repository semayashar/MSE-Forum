package com.mse.edu.forum.service;

import com.mse.edu.forum.api.generated.model.CreateReplyRequest;
import com.mse.edu.forum.api.generated.model.ReplyPageResponse;
import com.mse.edu.forum.api.generated.model.ReplyResponse;
import com.mse.edu.forum.api.generated.model.UpdateReplyRequest;
import com.mse.edu.forum.domain.ReplyEntity;
import com.mse.edu.forum.domain.UserEntity;
import com.mse.edu.forum.mapper.ReplyMapper;
import com.mse.edu.forum.repo.PostRepository;
import com.mse.edu.forum.repo.ReplyRepository;
import com.mse.edu.forum.repo.UserRepository;
import com.mse.edu.forum.security.CurrentUserService;
import com.mse.edu.forum.security.ForumUserDetails;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ReplyService {

	private final ReplyRepository replyRepository;
	private final PostRepository postRepository;
	private final UserRepository userRepository;
	private final ReplyMapper replyMapper;
	private final CurrentUserService currentUserService;

	public ReplyService(
			ReplyRepository replyRepository,
			PostRepository postRepository,
			UserRepository userRepository,
			ReplyMapper replyMapper,
			CurrentUserService currentUserService) {
		this.replyRepository = replyRepository;
		this.postRepository = postRepository;
		this.userRepository = userRepository;
		this.replyMapper = replyMapper;
		this.currentUserService = currentUserService;
	}

	@Transactional(readOnly = true)
	public ReplyPageResponse findByPostId(Long postId, Integer page, Integer size) {
		if (!postRepository.existsById(postId)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Topic not found");
		}
		var pageable = PageRequest.of(normalizedPage(page), normalizedSize(size));
		var replies = replyRepository.findByPostIdOrderByCreatedAtAsc(postId, pageable);
		return new ReplyPageResponse()
				.items(replies.getContent().stream().map(replyMapper::toResponse).toList())
				.page(replies.getNumber())
				.size(replies.getSize())
				.totalItems(replies.getTotalElements())
				.totalPages(replies.getTotalPages());
	}

	@Transactional(readOnly = true)
	public Optional<ReplyResponse> findById(Long id) {
		return replyRepository.findById(id).map(replyMapper::toResponse);
	}

	@Transactional
	public ReplyResponse create(Long postId, CreateReplyRequest request) {
		if (!postRepository.existsById(postId)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Topic not found");
		}
		ForumUserDetails currentUser = currentUserService.requireCurrentUser();
		ReplyEntity entity = replyMapper.toEntity(request, postId);
		entity.setContent(requireText(request.getContent(), "Reply content is required"));
		entity.setAuthor(findCurrentUser(currentUser));
		ReplyEntity saved = replyRepository.save(entity);
		return replyMapper.toResponse(saved);
	}

	@Transactional
	public Optional<ReplyResponse> update(Long id, UpdateReplyRequest request) {
		Optional<ReplyEntity> existing = replyRepository.findById(id);
		if (existing.isEmpty()) {
			return Optional.empty();
		}
		ReplyEntity entity = existing.get();
		currentUserService.requireCanEdit(entity.getAuthor().getId(), "Not allowed to edit this reply");
		replyMapper.applyUpdate(request, entity);
		entity.setContent(requireText(request.getContent(), "Reply content is required"));
		entity.setUpdatedAt(Instant.now());
		ReplyEntity saved = replyRepository.save(entity);
		return Optional.of(replyMapper.toResponse(saved));
	}

	private UserEntity findCurrentUser(ForumUserDetails currentUser) {
		return userRepository
				.findById(currentUser.getId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User no longer exists"));
	}

	private static int normalizedPage(Integer page) {
		return page == null || page < 0 ? 0 : page;
	}

	private static int normalizedSize(Integer size) {
		if (size == null) {
			return 10;
		}
		return Math.max(1, Math.min(100, size));
	}

	private static String requireText(String value, String message) {
		String text = value == null ? "" : value.trim();
		if (text.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
		}
		return text;
	}
}
