const API_BASE = window.FORUM_API_BASE || "";
const REPLY_PAGE_SIZE = 10;

const state = {
  token: localStorage.getItem("forum.token"),
  user: null,
  topics: [],
  users: [],
  selectedTopic: null,
  authMode: "login",
  editingTopic: false,
  editingReplyId: null
};

let noticeTimer;

const elements = {
  sessionSummary: document.getElementById("sessionSummary"),
  authTabs: document.getElementById("authTabs"),
  loginForm: document.getElementById("loginForm"),
  registerForm: document.getElementById("registerForm"),
  signedInPanel: document.getElementById("signedInPanel"),
  signedInName: document.getElementById("signedInName"),
  signedInRole: document.getElementById("signedInRole"),
  logoutButton: document.getElementById("logoutButton"),
  topicComposer: document.getElementById("topicComposer"),
  createTopicForm: document.getElementById("createTopicForm"),
  userAdminPanel: document.getElementById("userAdminPanel"),
  refreshUsersButton: document.getElementById("refreshUsersButton"),
  usersList: document.getElementById("usersList"),
  topicCount: document.getElementById("topicCount"),
  refreshButton: document.getElementById("refreshButton"),
  notice: document.getElementById("notice"),
  topicsList: document.getElementById("topicsList"),
  emptyDetail: document.getElementById("emptyDetail"),
  topicDetail: document.getElementById("topicDetail"),
  topicMeta: document.getElementById("topicMeta"),
  topicTitle: document.getElementById("topicTitle"),
  topicContent: document.getElementById("topicContent"),
  editTopicButton: document.getElementById("editTopicButton"),
  editTopicForm: document.getElementById("editTopicForm"),
  cancelEditTopicButton: document.getElementById("cancelEditTopicButton"),
  repliesList: document.getElementById("repliesList"),
  replyPageInfo: document.getElementById("replyPageInfo"),
  prevRepliesButton: document.getElementById("prevRepliesButton"),
  nextRepliesButton: document.getElementById("nextRepliesButton"),
  createReplyForm: document.getElementById("createReplyForm")
};

document.addEventListener("DOMContentLoaded", init);

function init() {
  hydrateSession();
  bindEvents();
  renderAuth();
  loadTopics();
  loadUsers({ silent: true });
  startAutoRefresh();
}

function bindEvents() {
  elements.authTabs.addEventListener("click", (event) => {
    const button = event.target.closest("[data-auth-mode]");
    if (!button) {
      return;
    }
    state.authMode = button.dataset.authMode;
    renderAuth();
  });

  elements.loginForm.addEventListener("submit", handleLogin);
  elements.registerForm.addEventListener("submit", handleRegister);
  elements.logoutButton.addEventListener("click", logout);
  elements.refreshButton.addEventListener("click", loadTopics);
  elements.refreshUsersButton.addEventListener("click", loadUsers);
  elements.createTopicForm.addEventListener("submit", handleCreateTopic);
  elements.editTopicButton.addEventListener("click", startTopicEdit);
  elements.cancelEditTopicButton.addEventListener("click", stopTopicEdit);
  elements.editTopicForm.addEventListener("submit", handleEditTopic);
  elements.prevRepliesButton.addEventListener("click", () => changeReplyPage(-1));
  elements.nextRepliesButton.addEventListener("click", () => changeReplyPage(1));
  elements.createReplyForm.addEventListener("submit", handleCreateReply);
}

function hydrateSession() {
  if (!state.token) {
    return;
  }

  const payload = parseJwt(state.token);
  if (!payload || isExpired(payload)) {
    localStorage.removeItem("forum.token");
    state.token = null;
    return;
  }

  state.user = {
    id: Number(payload.uid),
    username: payload.sub,
    role: payload.role
  };
}

async function loadTopics(options = {}) {
  try {
    if (!options.silent) {
      setBusy(elements.refreshButton, true);
    }
    const topics = await api("/posts");
    state.topics = Array.isArray(topics) ? topics : [];
    syncSelectedTopicFromList();
    renderTopics();
    renderTopicDetail();
  } catch (error) {
    if (!options.silent) {
      showNotice(error.message, "error");
    }
  } finally {
    if (!options.silent) {
      setBusy(elements.refreshButton, false);
    }
  }
}

async function openTopic(topicId, page = 0) {
  try {
    showNotice("");
    const topic = await api(`/posts/${topicId}?page=${page}&size=${REPLY_PAGE_SIZE}`);
    state.selectedTopic = topic;
    state.editingTopic = false;
    state.editingReplyId = null;
    syncTopicInList(topic);
    renderTopics();
    renderTopicDetail();
  } catch (error) {
    showNotice(error.message, "error");
  }
}

async function loadRepliesPage(page, options = {}) {
  if (!state.selectedTopic) {
    return;
  }

  try {
    const replies = await api(`/posts/${state.selectedTopic.id}/replies?page=${page}&size=${REPLY_PAGE_SIZE}`);
    state.selectedTopic.replies = replies;
    if (!options.silent) {
      state.editingReplyId = null;
    }
    renderTopicDetail();
  } catch (error) {
    if (!options.silent) {
      showNotice(error.message, "error");
    }
  }
}

async function handleLogin(event) {
  event.preventDefault();
  const form = event.currentTarget;
  const data = formData(event.currentTarget);

  try {
    setBusy(form, true);
    showNotice("Logging in...", "progress");
    const response = await api("/auth/login", {
      method: "POST",
      body: JSON.stringify({
        username: data.username,
        password: data.password
      })
    });
    state.token = response.accessToken;
    localStorage.setItem("forum.token", state.token);
    hydrateSession();
    form.reset();
    renderAuth();
    await loadTopics({ silent: true });
    await loadUsers({ silent: true });
    showNotice("Logged in successfully.", "success");
  } catch (error) {
    showNotice(error.message || "Login failed.", "error");
  } finally {
    setBusy(form, false);
  }
}

async function handleRegister(event) {
  event.preventDefault();
  const form = event.currentTarget;
  const data = formData(form);
  const payload = {
    username: data.username,
    password: data.password
  };

  if (data.email) {
    payload.email = data.email;
  }

  try {
    setBusy(form, true);
    showNotice("Creating account...", "progress");
    await api("/users", {
      method: "POST",
      body: JSON.stringify(payload)
    });

    const loginResponse = await api("/auth/login", {
      method: "POST",
      body: JSON.stringify({
        username: data.username,
        password: data.password
      })
    });

    state.token = loginResponse.accessToken;
    localStorage.setItem("forum.token", state.token);
    hydrateSession();
    form.reset();
    renderAuth();
    await loadTopics({ silent: true });
    await loadUsers({ silent: true });
    showNotice("Account created and logged in.", "success");
  } catch (error) {
    showNotice(error.message || "Registration failed.", "error");
  } finally {
    setBusy(form, false);
  }
}

async function handleCreateTopic(event) {
  event.preventDefault();
  const form = event.currentTarget;
  const data = formData(form);

  try {
    setBusy(form, true);
    showNotice("Publishing topic...", "progress");
    const created = await api("/posts", {
      method: "POST",
      body: JSON.stringify({
        title: data.title,
        content: data.content || ""
      })
    });
    form.reset();
    await loadTopics({ silent: true });
    await openTopic(created.id);
    showNotice("Topic published.", "success");
  } catch (error) {
    showNotice(error.message || "Could not publish topic.", "error");
  } finally {
    setBusy(form, false);
  }
}

function startTopicEdit() {
  if (!state.selectedTopic) {
    return;
  }
  state.editingTopic = true;
  elements.editTopicForm.elements.title.value = state.selectedTopic.title || "";
  elements.editTopicForm.elements.content.value = state.selectedTopic.content || "";
  renderTopicDetail();
}

function stopTopicEdit() {
  state.editingTopic = false;
  renderTopicDetail();
}

async function handleEditTopic(event) {
  event.preventDefault();
  if (!state.selectedTopic) {
    return;
  }

  const form = event.currentTarget;
  const data = formData(form);
  try {
    setBusy(form, true);
    showNotice("Saving topic...", "progress");
    const updated = await api(`/posts/${state.selectedTopic.id}`, {
      method: "PUT",
      body: JSON.stringify({
        title: data.title,
        content: data.content || ""
      })
    });
    state.selectedTopic = {
      ...state.selectedTopic,
      ...updated,
      replies: state.selectedTopic.replies
    };
    state.editingTopic = false;
    syncTopicInList(state.selectedTopic);
    renderTopics();
    renderTopicDetail();
    showNotice("Topic saved.", "success");
  } catch (error) {
    showNotice(error.message || "Could not save topic.", "error");
  } finally {
    setBusy(form, false);
  }
}

async function handleCreateReply(event) {
  event.preventDefault();
  if (!state.selectedTopic) {
    return;
  }

  const form = event.currentTarget;
  const data = formData(form);
  try {
    setBusy(form, true);
    showNotice("Posting reply...", "progress");
    await api(`/posts/${state.selectedTopic.id}/replies`, {
      method: "POST",
      body: JSON.stringify({ content: data.content })
    });
    form.reset();
    const currentPage = state.selectedTopic.replies?.page || 0;
    await loadTopics({ silent: true });
    await loadRepliesPage(currentPage, { silent: true });
    showNotice("Reply posted.", "success");
  } catch (error) {
    showNotice(error.message || "Could not post reply.", "error");
  } finally {
    setBusy(form, false);
  }
}

async function handleEditReply(replyId, form) {
  const data = formData(form);
  try {
    setBusy(form, true);
    showNotice("Saving reply...", "progress");
    const updated = await api(`/replies/${replyId}`, {
      method: "PUT",
      body: JSON.stringify({ content: data.content })
    });
    const replies = state.selectedTopic?.replies?.items || [];
    state.selectedTopic.replies.items = replies.map((reply) => reply.id === replyId ? updated : reply);
    state.editingReplyId = null;
    renderTopicDetail();
    showNotice("Reply saved.", "success");
  } catch (error) {
    showNotice(error.message || "Could not save reply.", "error");
  } finally {
    setBusy(form, false);
  }
}

async function handleDeleteReply(replyId) {
  if (!window.confirm("Delete this reply?")) {
    return;
  }

  try {
    showNotice("Deleting reply...", "progress");
    const currentPage = state.selectedTopic?.replies?.page || 0;
    await api(`/replies/${replyId}`, { method: "DELETE" });
    await loadTopics({ silent: true });
    await loadRepliesPage(currentPage, { silent: true });
    const replies = state.selectedTopic?.replies;
    if (replies && replies.page > 0 && replies.items.length === 0) {
      await loadRepliesPage(replies.page - 1, { silent: true });
    }
    showNotice("Reply deleted.", "success");
  } catch (error) {
    showNotice(error.message || "Could not delete reply.", "error");
  }
}

async function loadUsers(options = {}) {
  if (!isAdmin()) {
    state.users = [];
    renderUsers();
    return;
  }

  try {
    if (!options.silent) {
      setBusy(elements.refreshUsersButton, true);
    }
    const users = await api("/users");
    state.users = Array.isArray(users) ? users : [];
    renderUsers();
  } catch (error) {
    if (!options.silent) {
      showNotice(error.message || "Could not load users.", "error");
    }
  } finally {
    if (!options.silent) {
      setBusy(elements.refreshUsersButton, false);
    }
  }
}

async function updateUserRole(user, role) {
  try {
    showNotice("Updating role...", "progress");
    await api(`/users/${user.id}`, {
      method: "PUT",
      body: JSON.stringify({
        username: user.username,
        email: user.email || null,
        role
      })
    });
    await loadUsers({ silent: true });
    showNotice("User role updated.", "success");
  } catch (error) {
    showNotice(error.message || "Could not update role.", "error");
  }
}

function changeReplyPage(direction) {
  const replies = state.selectedTopic?.replies;
  if (!replies) {
    return;
  }
  const nextPage = replies.page + direction;
  if (nextPage < 0 || nextPage >= replies.totalPages) {
    return;
  }
  loadRepliesPage(nextPage);
}

function logout() {
  localStorage.removeItem("forum.token");
  state.token = null;
  state.user = null;
  state.users = [];
  state.authMode = "login";
  state.editingTopic = false;
  state.editingReplyId = null;
  renderAuth();
  renderTopics();
  renderTopicDetail();
  showNotice("Logged out.", "success");
}

function startAutoRefresh() {
  window.setInterval(async () => {
    if (document.hidden) {
      return;
    }

    await loadTopics({ silent: true });
    if (state.selectedTopic) {
      const currentPage = state.selectedTopic.replies?.page || 0;
      await loadRepliesPage(currentPage, { silent: true });
    }
    await loadUsers({ silent: true });
  }, 10000);
}

function renderAuth() {
  const isSignedIn = Boolean(state.user);
  elements.sessionSummary.textContent = isSignedIn
    ? `${state.user.username} - ${state.user.role}`
    : "Browsing as guest";
  elements.sessionSummary.classList.toggle("is-signed-in", isSignedIn);

  elements.loginForm.classList.toggle("hidden", isSignedIn || state.authMode !== "login");
  elements.registerForm.classList.toggle("hidden", isSignedIn || state.authMode !== "register");
  elements.signedInPanel.classList.toggle("hidden", !isSignedIn);
  elements.topicComposer.classList.toggle("hidden", !isSignedIn);
  elements.userAdminPanel.classList.toggle("hidden", !isAdmin());
  elements.authTabs.classList.toggle("hidden", isSignedIn);

  elements.authTabs.querySelectorAll(".tab").forEach((tab) => {
    tab.classList.toggle("active", tab.dataset.authMode === state.authMode);
  });

  if (isSignedIn) {
    elements.signedInName.textContent = state.user.username;
    elements.signedInRole.textContent = state.user.role;
  }
  renderUsers();
}

function renderTopics() {
  elements.topicCount.textContent = `${state.topics.length} ${state.topics.length === 1 ? "topic" : "topics"}`;
  elements.topicsList.replaceChildren();

  if (state.topics.length === 0) {
    elements.topicsList.append(emptyBlock("No topics yet."));
    return;
  }

  state.topics.forEach((topic) => {
    const button = document.createElement("button");
    button.type = "button";
    button.className = "topic-card";
    if (state.selectedTopic?.id === topic.id) {
      button.classList.add("active");
    }
    button.addEventListener("click", () => openTopic(topic.id));

    button.append(
      textElement("h3", topic.title),
      textElement("p", topic.content || "No description yet."),
      metaRow([
        `By ${topic.author?.username || "unknown"}`,
        `${topic.viewCount || 0} views`,
        formatDate(topic.updatedAt || topic.createdAt)
      ])
    );
    elements.topicsList.append(button);
  });
}

function renderTopicDetail() {
  const topic = state.selectedTopic;
  elements.emptyDetail.classList.toggle("hidden", Boolean(topic));
  elements.topicDetail.classList.toggle("hidden", !topic);
  elements.createReplyForm.classList.toggle("hidden", !topic || !state.user);

  if (!topic) {
    return;
  }

  elements.topicMeta.textContent = `By ${topic.author?.username || "unknown"} - ${topic.viewCount || 0} views - Updated ${formatDate(topic.updatedAt || topic.createdAt)}`;
  elements.topicTitle.textContent = topic.title || "";
  elements.topicContent.textContent = topic.content || "No description yet.";

  const canEditTopic = canEdit(topic.author);
  elements.editTopicButton.classList.toggle("hidden", !canEditTopic);
  elements.editTopicForm.classList.toggle("hidden", !state.editingTopic);

  renderReplies(topic.replies);
}

function renderReplies(replies) {
  elements.repliesList.replaceChildren();

  const page = replies?.page || 0;
  const totalPages = replies?.totalPages || 0;
  const items = replies?.items || [];

  elements.replyPageInfo.textContent = totalPages > 0 ? `Page ${page + 1} of ${totalPages}` : "No pages";
  elements.prevRepliesButton.disabled = page <= 0;
  elements.nextRepliesButton.disabled = totalPages === 0 || page >= totalPages - 1;

  if (items.length === 0) {
    elements.repliesList.append(emptyBlock("No replies yet."));
    return;
  }

  items.forEach((reply) => {
    const card = document.createElement("article");
    card.className = "reply-card";

    const head = document.createElement("div");
    head.className = "reply-card-head";
    head.append(metaRow([
      `By ${reply.author?.username || "unknown"}`,
      formatDate(reply.updatedAt || reply.createdAt)
    ], "reply-meta"));

    if (canEdit(reply.author)) {
      const actions = document.createElement("div");
      actions.className = "reply-actions";
      const editButton = button("Edit", "secondary", () => {
        state.editingReplyId = reply.id;
        renderTopicDetail();
      }, "button");
      const deleteButton = button("Delete", "secondary danger-action", () => handleDeleteReply(reply.id), "button");
      actions.append(editButton, deleteButton);
      head.append(actions);
    }

    card.append(head);

    if (state.editingReplyId === reply.id) {
      card.append(replyEditForm(reply));
    } else {
      card.append(textElement("p", reply.content || ""));
    }

    elements.repliesList.append(card);
  });
}

function renderUsers() {
  elements.usersList.replaceChildren();
  if (!isAdmin()) {
    return;
  }

  if (state.users.length === 0) {
    elements.usersList.append(emptyBlock("No users loaded."));
    return;
  }

  state.users.forEach((user) => {
    const row = document.createElement("article");
    row.className = "user-row";

    const summary = document.createElement("div");
    summary.append(
      textElement("strong", user.username),
      textElement("small", user.role)
    );

    const actions = document.createElement("div");
    actions.className = "user-actions";
    if (user.role === "USER") {
      actions.append(button("Make Moderator", "secondary", () => updateUserRole(user, "MODERATOR"), "button"));
    } else if (user.role === "MODERATOR") {
      actions.append(button("Make User", "secondary", () => updateUserRole(user, "USER"), "button"));
    }

    row.append(summary, actions);
    elements.usersList.append(row);
  });
}

function replyEditForm(reply) {
  const form = document.createElement("form");
  form.className = "edit-box";
  form.addEventListener("submit", (event) => {
    event.preventDefault();
    handleEditReply(reply.id, form);
  });

  const label = document.createElement("label");
  label.textContent = "Reply";
  const textarea = document.createElement("textarea");
  textarea.name = "content";
  textarea.rows = 4;
  textarea.required = true;
  textarea.maxLength = 10000;
  textarea.value = reply.content || "";
  label.append(textarea);

  const actions = document.createElement("div");
  actions.className = "button-row";
  actions.append(
    button("Save Reply", "primary"),
    button("Cancel", "secondary", () => {
      state.editingReplyId = null;
      renderTopicDetail();
    }, "button")
  );

  form.append(label, actions);
  return form;
}

async function api(path, options = {}) {
  const headers = {
    Accept: "application/json",
    ...(options.headers || {})
  };

  if (options.body) {
    headers["Content-Type"] = "application/json";
  }

  if (state.token) {
    headers.Authorization = `Bearer ${state.token}`;
  }

  const response = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers
  });

  if (response.status === 401 || response.status === 403) {
    const message = response.status === 401
      ? "Please log in first."
      : "You do not have permission for that action.";
    throw new Error(message);
  }

  if (!response.ok) {
    throw new Error(await errorMessage(response));
  }

  if (response.status === 204) {
    return null;
  }

  return response.json();
}

async function errorMessage(response) {
  const text = await response.text();
  if (!text) {
    return fallbackErrorMessage(response);
  }

  try {
    const body = JSON.parse(text);
    return body.message || body.detail || body.error || fallbackErrorMessage(response);
  } catch {
    return text || fallbackErrorMessage(response);
  }
}

function fallbackErrorMessage(response) {
  if (response.status === 409) {
    return "That username, email, or topic title is already used.";
  }
  if (response.status === 400) {
    return "Please check the form and try again.";
  }
  return `${response.status} ${response.statusText}`;
}

function canEdit(author) {
  if (!state.user || !author) {
    return false;
  }

  return state.user.role === "ADMIN"
    || state.user.role === "MODERATOR"
    || Number(author.id) === Number(state.user.id);
}

function isAdmin() {
  return state.user?.role === "ADMIN";
}

function syncTopicInList(topic) {
  state.topics = state.topics.map((item) => item.id === topic.id ? { ...item, ...topic } : item);
}

function syncSelectedTopicFromList() {
  if (!state.selectedTopic) {
    return;
  }

  const freshTopic = state.topics.find((topic) => topic.id === state.selectedTopic.id);
  if (!freshTopic) {
    return;
  }

  state.selectedTopic = {
    ...state.selectedTopic,
    ...freshTopic,
    replies: state.selectedTopic.replies
  };
}

function setBusy(targetElement, busy) {
  if (!targetElement) {
    return;
  }

  const controls = targetElement.matches?.("form")
    ? targetElement.querySelectorAll("button, input, textarea, select")
    : [targetElement];
  controls.forEach((control) => {
    control.disabled = busy;
  });
}

function showNotice(message, type = "success") {
  window.clearTimeout(noticeTimer);
  if (!message) {
    elements.notice.className = "notice hidden";
    elements.notice.textContent = "";
    return;
  }
  elements.notice.className = `notice ${type}`;
  elements.notice.textContent = message;
  if (type === "success") {
    noticeTimer = window.setTimeout(() => showNotice(""), 4500);
  }
}

function formData(form) {
  return Object.fromEntries(new FormData(form).entries());
}

function parseJwt(token) {
  try {
    const payload = token.split(".")[1];
    const normalized = payload.replace(/-/g, "+").replace(/_/g, "/");
    const padded = normalized.padEnd(normalized.length + (4 - normalized.length % 4) % 4, "=");
    return JSON.parse(atob(padded));
  } catch {
    return null;
  }
}

function isExpired(payload) {
  return payload.exp && Date.now() / 1000 >= payload.exp;
}

function formatDate(value) {
  if (!value) {
    return "Unknown date";
  }

  return new Intl.DateTimeFormat(undefined, {
    dateStyle: "medium",
    timeStyle: "short"
  }).format(new Date(value));
}

function textElement(tag, text) {
  const node = document.createElement(tag);
  node.textContent = text;
  return node;
}

function metaRow(items, className = "topic-meta") {
  const row = document.createElement("div");
  row.className = className;
  items.forEach((item) => {
    const span = document.createElement("span");
    span.textContent = item;
    row.append(span);
  });
  return row;
}

function emptyBlock(message) {
  const block = document.createElement("div");
  block.className = "empty-state";
  block.append(textElement("p", message));
  return block;
}

function button(label, className, onClick, type = "submit") {
  const buttonElement = document.createElement("button");
  buttonElement.type = type;
  buttonElement.className = className;
  buttonElement.textContent = label;
  if (onClick) {
    buttonElement.addEventListener("click", onClick);
  }
  return buttonElement;
}
