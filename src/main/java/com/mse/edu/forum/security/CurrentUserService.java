package com.mse.edu.forum.security;

import com.mse.edu.forum.domain.UserRole;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CurrentUserService {

	public ForumUserDetails requireCurrentUser() {
		ForumUserDetails user = currentUser();
		if (user == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
		}
		return user;
	}

	public boolean isAdmin() {
		ForumUserDetails user = currentUser();
		return user != null && user.getDomainRole() == UserRole.ADMIN;
	}

	public boolean isModeratorOrAdmin() {
		ForumUserDetails user = currentUser();
		return user != null
				&& (user.getDomainRole() == UserRole.ADMIN || user.getDomainRole() == UserRole.MODERATOR);
	}

	public void requireCanEdit(Long ownerId, String message) {
		ForumUserDetails user = requireCurrentUser();
		if (isModeratorOrAdmin() || (ownerId != null && ownerId == user.getId())) {
			return;
		}
		throw new ResponseStatusException(HttpStatus.FORBIDDEN, message);
	}

	private ForumUserDetails currentUser() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null || !auth.isAuthenticated()) {
			return null;
		}
		return auth.getPrincipal() instanceof ForumUserDetails details ? details : null;
	}
}
