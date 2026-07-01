package com.mse.edu.forum.service;

import com.mse.edu.forum.api.generated.model.CreatePostRequest;
import com.mse.edu.forum.api.generated.model.PostDetailsResponse;
import com.mse.edu.forum.api.generated.model.PostResponse;
import com.mse.edu.forum.api.generated.model.ReplyPageResponse;
import com.mse.edu.forum.api.generated.model.UpdatePostRequest;
import com.mse.edu.forum.domain.PostEntity;
import com.mse.edu.forum.domain.UserEntity;
import com.mse.edu.forum.mapper.PostMapper;
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
public class PostService {

	private final PostRepository postRepository;
	private final ReplyRepository replyRepository;
	private final UserRepository userRepository;
	private final PostMapper postMapper;
	private final ReplyMapper replyMapper;
	private final CurrentUserService currentUserService;

	public PostService(
			PostRepository postRepository,
			ReplyRepository replyRepository,
			UserRepository userRepository,
			PostMapper postMapper,
			ReplyMapper replyMapper,
			CurrentUserService currentUserService) {
		this.postRepository = postRepository;
		this.replyRepository = replyRepository;
		this.userRepository = userRepository;
		this.postMapper = postMapper;
		this.replyMapper = replyMapper;
		this.currentUserService = currentUserService;
	}

	@Transactional(readOnly = true)
	public List<PostResponse> findAll() {
		return postRepository.findAllByOrderByCreatedAtAscIdAsc().stream()
				.map(postMapper::toResponse)
				.toList();
	}

	@Transactional
	public Optional<PostDetailsResponse> findById(Long id, Integer page, Integer size) {
		return postRepository.findById(id).map(entity -> {
			entity.setViewCount(entity.getViewCount() + 1);
			PostDetailsResponse response = postMapper.toDetailsResponse(entity);
			response.setReplies(replyPage(id, page, size));
			return response;
		});
	}

	@Transactional
	public PostResponse create(CreatePostRequest request) {
		ForumUserDetails currentUser = currentUserService.requireCurrentUser();
		String title = requireText(request.getTitle(), "Title is required");
		if (postRepository.existsByTitle(title)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Topic title already exists");
		}

		PostEntity postEntity = postMapper.toEntity(request);
		postEntity.setTitle(title);
		postEntity.setContent(optionalText(request.getContent()));
		postEntity.setAuthor(findCurrentUser(currentUser));
		PostEntity saved = postRepository.save(postEntity);
		return postMapper.toResponse(saved);
	}

	@Transactional
	public Optional<PostResponse> update(Long id, UpdatePostRequest request) {
		Optional<PostEntity> existing = postRepository.findById(id);
		if (existing.isEmpty()) {
			return Optional.empty();
		}
		PostEntity entity = existing.get();
		currentUserService.requireCanEdit(entity.getAuthor().getId(), "Not allowed to edit this topic");

		String title = requireText(request.getTitle(), "Title is required");
		if (postRepository.existsByTitleAndIdNot(title, id)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Topic title already exists");
		}

		postMapper.applyUpdate(request, entity);
		entity.setTitle(title);
		entity.setContent(optionalText(request.getContent()));
		entity.setUpdatedAt(Instant.now());
		PostEntity saved = postRepository.save(entity);
		return Optional.of(postMapper.toResponse(saved));
	}

	private ReplyPageResponse replyPage(Long postId, Integer page, Integer size) {
		var pageable = PageRequest.of(normalizedPage(page), normalizedSize(size));
		var replies = replyRepository.findByPostIdOrderByCreatedAtAsc(postId, pageable);
		return new ReplyPageResponse()
				.items(replies.getContent().stream().map(replyMapper::toResponse).toList())
				.page(replies.getNumber())
				.size(replies.getSize())
				.totalItems(replies.getTotalElements())
				.totalPages(replies.getTotalPages());
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

	private static String optionalText(String value) {
		return value == null ? "" : value.trim();
	}
}
