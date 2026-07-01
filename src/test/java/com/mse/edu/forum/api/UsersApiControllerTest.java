package com.mse.edu.forum.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mse.edu.forum.maintenance.RestoreMaintenanceState;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class UsersApiControllerTest {

	private static final String PASSWORD = "Password123!";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private RestoreMaintenanceState restoreMaintenanceState;

	@BeforeEach
	void setUp() {
		restoreMaintenanceState.finishRestore();
	}

	@Test
	void registrationDefaultsToUserRole() throws Exception {
		String username = unique("regular");

		mockMvc.perform(post("/users")
						.contentType(MediaType.APPLICATION_JSON)
						.content(userJson(username)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.username").value(username))
				.andExpect(jsonPath("$.role").value("USER"));
	}

	@Test
	void publicRegistrationCannotAssignModeratorRole() throws Exception {
		String username = unique("blocked");

		mockMvc.perform(post("/users")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "username": "%s",
								  "email": "%s@example.com",
								  "password": "%s",
								  "role": "MODERATOR"
								}
								""".formatted(username, username, PASSWORD)))
				.andExpect(status().isForbidden());
	}

	@Test
	void adminCanPromoteUserToModerator() throws Exception {
		String username = unique("promote");
		long userId = registerUser(username);
		String adminToken = loginAndGetToken("admin", "admin");

		mockMvc.perform(put("/users/{id}", userId)
						.header("Authorization", bearer(adminToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content(updateUserJson(username, "MODERATOR")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.role").value("MODERATOR"));

		String moderatorToken = loginAndGetToken(username, PASSWORD);
		mockMvc.perform(get("/users")
						.header("Authorization", bearer(moderatorToken)))
				.andExpect(status().isOk());
	}

	@Test
	void regularUserCannotPromoteSelfToModerator() throws Exception {
		String username = unique("self");
		long userId = registerUser(username);
		String token = loginAndGetToken(username, PASSWORD);

		mockMvc.perform(put("/users/{id}", userId)
						.header("Authorization", bearer(token))
						.contentType(MediaType.APPLICATION_JSON)
						.content(updateUserJson(username, "MODERATOR")))
				.andExpect(status().isForbidden());
	}

	@Test
	void moderatorCanEditOtherUsersTopicsAndReplies() throws Exception {
		String owner = unique("owner");
		String moderator = unique("mod");
		registerUser(owner);
		long moderatorId = registerUser(moderator);

		String adminToken = loginAndGetToken("admin", "admin");
		mockMvc.perform(put("/users/{id}", moderatorId)
						.header("Authorization", bearer(adminToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content(updateUserJson(moderator, "MODERATOR")))
				.andExpect(status().isOk());

		String ownerToken = loginAndGetToken(owner, PASSWORD);
		long postId = createPost(ownerToken, unique("topic"));
		long replyId = createReply(ownerToken, postId);

		String moderatorToken = loginAndGetToken(moderator, PASSWORD);
		String updatedTitle = unique("edited");
		mockMvc.perform(put("/posts/{id}", postId)
						.header("Authorization", bearer(moderatorToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "title": "%s",
								  "content": "Moderator changed the topic."
								}
								""".formatted(updatedTitle)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.title").value(updatedTitle));

		mockMvc.perform(put("/replies/{id}", replyId)
						.header("Authorization", bearer(moderatorToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "content": "Moderator changed the reply."
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content").value("Moderator changed the reply."));
	}

	private long registerUser(String username) throws Exception {
		MvcResult result = mockMvc.perform(post("/users")
						.contentType(MediaType.APPLICATION_JSON)
						.content(userJson(username)))
				.andExpect(status().isCreated())
				.andReturn();
		return extractLongField(result.getResponse().getContentAsString(), "id");
	}

	private long createPost(String token, String title) throws Exception {
		MvcResult result = mockMvc.perform(post("/posts")
						.header("Authorization", bearer(token))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "title": "%s",
								  "content": "Original topic."
								}
								""".formatted(title)))
				.andExpect(status().isCreated())
				.andReturn();
		return extractLongField(result.getResponse().getContentAsString(), "id");
	}

	private long createReply(String token, long postId) throws Exception {
		MvcResult result = mockMvc.perform(post("/posts/{postId}/replies", postId)
						.header("Authorization", bearer(token))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "content": "Original reply."
								}
								"""))
				.andExpect(status().isCreated())
				.andReturn();
		return extractLongField(result.getResponse().getContentAsString(), "id");
	}

	private String loginAndGetToken(String username, String password) throws Exception {
		MvcResult loginResult = mockMvc.perform(post("/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "username": "%s",
								  "password": "%s"
								}
								""".formatted(username, password)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").isString())
				.andReturn();

		String body = loginResult.getResponse().getContentAsString();
		String marker = "\"accessToken\":\"";
		int start = body.indexOf(marker);
		if (start < 0) {
			throw new IllegalStateException("accessToken not found in login response: " + body);
		}
		start += marker.length();
		int end = body.indexOf('"', start);
		if (end < 0) {
			throw new IllegalStateException("Invalid login response: " + body);
		}
		return body.substring(start, end);
	}

	private static String userJson(String username) {
		return """
				{
				  "username": "%s",
				  "email": "%s@example.com",
				  "password": "%s"
				}
				""".formatted(username, username, PASSWORD);
	}

	private static String updateUserJson(String username, String role) {
		return """
				{
				  "username": "%s",
				  "email": "%s@example.com",
				  "role": "%s"
				}
				""".formatted(username, username, role);
	}

	private static String bearer(String token) {
		return "Bearer " + token;
	}

	private static String unique(String prefix) {
		return prefix + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
	}

	private static long extractLongField(String json, String fieldName) {
		Pattern pattern = Pattern.compile("\"" + fieldName + "\":(\\d+)");
		Matcher matcher = pattern.matcher(json);
		if (!matcher.find()) {
			throw new IllegalStateException("Field not found: " + fieldName + " in " + json);
		}
		return Long.parseLong(matcher.group(1));
	}
}
