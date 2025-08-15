// Dashboard functionality for TodoTalk
// Global variables
let selectedMembers = [];
let currentChatId = null;
let pendingPrivateChat = null; // holds info before first message is sent
let stompClient = null;
let isConnected = false;
let chatCache = {}; // store chats by id
let allTasksCache = []; // cache tasks for current chat
let currentTaskFilter = "ALL";
let pendingCompleteTaskId = null;

function initializeMessagePage() {
  console.log("initializeMessagePage called (safe init)");
  console.log("Current User ID at init (pre-ensure):", window.currentUserId);
  ensureCurrentUserId().then(() => {
    console.log("Current User ID after ensure:", window.currentUserId);
    initializeEventListeners();
    initializeWebSocket();
    loadChatList();

    // If a chat is pre-selected (e.g., from cached state), load its tasks
    if (window.currentChatId) {
      loadTasksForCurrentChat();
    }
  });
}

async function ensureCurrentUserId() {
  if (window.currentUserId && Number(window.currentUserId) > 0) return;
  try {
    const r = await fetch("/api/users/me");
    if (r.ok) {
      const u = await r.json();
      if (u && u.userId) window.currentUserId = u.userId;
    }
  } catch (e) {
    console.warn("ensureCurrentUserId error", e);
  }
}

// Run init immediately if DOM already loaded, else wait
if (document.readyState === "loading") {
  document.addEventListener("DOMContentLoaded", initializeMessagePage);
} else {
  initializeMessagePage();
}

function initializeEventListeners() {
  console.log("Initializing event listeners...");

  const searchUsersBtn = document.getElementById("searchUsersBtn");
  const closeSearchUsersModalBtn = document.getElementById(
    "closeSearchUsersModal"
  );

  console.log("Search Users Button:", searchUsersBtn);
  console.log("Close Search Users Modal Button:", closeSearchUsersModalBtn);

  if (searchUsersBtn) {
    searchUsersBtn.addEventListener("click", openSearchUsersModal);
    console.log("Search Users button event listener added");
  } else {
    console.error("Search Users button not found!");
  }

  if (closeSearchUsersModalBtn) {
    closeSearchUsersModalBtn.addEventListener("click", closeSearchUsersModal);
    console.log("Close Search Users Modal button event listener added");
  } else {
    console.error("Close Search Users Modal button not found!");
  }

  const newGroupBtn = document.getElementById("newGroupBtn");
  const closeCreateGroupModalBtn = document.getElementById(
    "closeCreateGroupModal"
  );
  const cancelCreateGroupBtn = document.getElementById("cancelCreateGroup");

  console.log("New Group Button:", newGroupBtn);

  if (newGroupBtn) {
    newGroupBtn.addEventListener("click", openCreateGroupModal);
    console.log("New Group button event listener added");
  } else {
    console.error("New Group button not found!");
  }

  if (closeCreateGroupModalBtn)
    closeCreateGroupModalBtn.addEventListener("click", closeCreateGroupModal);
  if (cancelCreateGroupBtn)
    cancelCreateGroupBtn.addEventListener("click", closeCreateGroupModal);

  const userSearchInput = document.getElementById("userSearchInput");
  if (userSearchInput)
    userSearchInput.addEventListener("input", debounce(searchUsers, 300));

  const memberSearchInput = document.getElementById("memberSearchInput");
  if (memberSearchInput)
    memberSearchInput.addEventListener(
      "input",
      debounce(searchMembersForGroup, 300)
    );

  const createGroupForm = document.getElementById("createGroupForm");
  if (createGroupForm)
    createGroupForm.addEventListener("submit", handleCreateGroup);

  const messageForm = document.getElementById("messageForm");
  if (messageForm) messageForm.addEventListener("submit", handleSendMessage);

  // Task filter buttons
  const filterAllBtn = document.getElementById("filterAllBtn");
  const filterPendingBtn = document.getElementById("filterPendingBtn");
  const filterDoneBtn = document.getElementById("filterDoneBtn");
  if (filterAllBtn)
    filterAllBtn.addEventListener("click", () => setTaskFilter("ALL"));
  if (filterPendingBtn)
    filterPendingBtn.addEventListener("click", () => setTaskFilter("PENDING"));
  if (filterDoneBtn)
    filterDoneBtn.addEventListener("click", () => setTaskFilter("DONE"));

  // Completion modal buttons (attach immediately; script loaded after DOM)
  attachCompleteTaskModalListeners();
}

function setTaskFilter(filter) {
  currentTaskFilter = filter;
  updateFilterButtonStyles();
  renderFilteredTasks();
}

function updateFilterButtonStyles() {
  const map = {
    ALL: document.getElementById("filterAllBtn"),
    PENDING: document.getElementById("filterPendingBtn"),
    DONE: document.getElementById("filterDoneBtn"),
  };
  Object.entries(map).forEach(([key, btn]) => {
    if (!btn) return;
    if (key === currentTaskFilter) {
      btn.classList.remove(
        "text-gray-600",
        "hover:text-gray-900",
        "bg-transparent"
      );
      btn.classList.add("text-white", "bg-blue-500");
    } else {
      btn.classList.remove("text-white", "bg-blue-500");
      btn.classList.add(
        "text-gray-600",
        "hover:text-gray-900",
        "bg-transparent"
      );
    }
  });
}

function renderFilteredTasks() {
  let tasksToShow = allTasksCache;
  if (currentTaskFilter === "PENDING") {
    tasksToShow = allTasksCache.filter(
      (t) => t.status === "pending" || t.status === "PENDING"
    );
  } else if (currentTaskFilter === "DONE") {
    tasksToShow = allTasksCache.filter(
      (t) => t.status === "completed" || t.status === "COMPLETED"
    );
  }
  displayTasks(tasksToShow);
  updateTaskStats();
}

function initializeWebSocket() {
  if (typeof SockJS === "undefined" || typeof StompJs === "undefined") {
    console.error("SockJS/STOMP libraries not loaded");
    return;
  }

  const socket = new SockJS("/ws");
  stompClient = new StompJs.Client({
    webSocketFactory: () => socket,
    reconnectDelay: 5000,
    heartbeatIncoming: 4000,
    heartbeatOutgoing: 4000,
  });

  stompClient.onConnect = function () {
    isConnected = true;
    console.log("WebSocket connected");
    if (currentChatId) subscribeToChat(currentChatId);
  };

  stompClient.onWebSocketClose = function () {
    isConnected = false;
    console.log("WebSocket disconnected");
  };

  stompClient.onStompError = function (frame) {
    console.error("STOMP error:", frame);
  };

  stompClient.activate();
}

function subscribeToChat(chatId) {
  if (stompClient && isConnected) {
    stompClient.subscribe("/topic/chat/" + chatId, function (message) {
      handleIncomingMessage(JSON.parse(message.body));
    });
    stompClient.subscribe(
      "/topic/chat/" + chatId + "/read",
      function (message) {
        handleMessageRead(JSON.parse(message.body));
      }
    );
    stompClient.subscribe(
      "/topic/chat/" + chatId + "/tasks",
      function (message) {
        handleNewTask(JSON.parse(message.body));
      }
    );
  }
}

function sendMessageViaWebSocket(chatId, content, messageType = "TEXT") {
  if (stompClient && isConnected) {
    stompClient.publish({
      destination: "/app/chat.sendMessage",
      body: JSON.stringify({
        chatId,
        content,
        messageType,
      }),
    });
  }
}

function markAsReadViaWebSocket(chatId) {
  if (stompClient && isConnected) {
    stompClient.publish({
      destination: "/app/chat.markRead",
      body: JSON.stringify({ chatId, userId: window.currentUserId }),
    });
  }
}

// Modal functions
function openSearchUsersModal() {
  console.log("openSearchUsersModal called");
  const modal = document.getElementById("searchUsersModal");
  const input = document.getElementById("userSearchInput");
  console.log("Search modal element:", modal);
  console.log("Search input element:", input);
  if (modal) {
    modal.classList.remove("hidden");
    console.log("Search modal opened");
  } else {
    console.error("Search modal not found!");
  }
  if (input) input.focus();
}

function closeSearchUsersModal() {
  console.log("closeSearchUsersModal called");
  const modal = document.getElementById("searchUsersModal");
  const input = document.getElementById("userSearchInput");
  const results = document.getElementById("userSearchResults");
  console.log("Close modal elements:", { modal, input, results });
  if (modal) {
    modal.classList.add("hidden");
    console.log("Search modal closed");
  } else {
    console.error("Search modal not found when trying to close!");
  }
  if (input) input.value = "";
  if (results)
    results.innerHTML =
      '<div class="text-center text-gray-500 py-4">Nhập username để bắt đầu tìm kiếm</div>';
}

function openCreateGroupModal() {
  console.log("openCreateGroupModal called");
  const modal = document.getElementById("createGroupModal");
  const input = document.getElementById("groupNameInput");
  console.log("Create group modal element:", modal);
  console.log("Group name input element:", input);
  if (modal) {
    modal.classList.remove("hidden");
    console.log("Create group modal opened");
  } else {
    console.error("Create group modal not found!");
  }
  if (input) input.focus();
  selectedMembers = [];
  updateSelectedMembersList();
}

function closeCreateGroupModal() {
  const modal = document.getElementById("createGroupModal");
  const groupInput = document.getElementById("groupNameInput");
  const memberInput = document.getElementById("memberSearchInput");
  const results = document.getElementById("memberSearchResults");
  if (modal) modal.classList.add("hidden");
  if (groupInput) groupInput.value = "";
  if (memberInput) memberInput.value = "";
  if (results) results.innerHTML = "";
  selectedMembers = [];
  updateSelectedMembersList();
}

// Search functions
function searchUsers() {
  console.log("searchUsers called");
  const searchTerm = document.getElementById("userSearchInput").value.trim();
  const resultsContainer = document.getElementById("userSearchResults");
  console.log("Search term:", searchTerm);
  console.log("Results container:", resultsContainer);

  if (searchTerm.length < 2) {
    resultsContainer.innerHTML =
      '<div class="text-center text-gray-500 py-4">Nhập ít nhất 2 ký tự để tìm kiếm</div>';
    return;
  }

  resultsContainer.innerHTML =
    '<div class="text-center text-gray-500 py-4"><i class="fas fa-spinner fa-spin"></i> Đang tìm kiếm...</div>';

  console.log(
    "Making API call to:",
    `/api/users/search?searchTerm=${encodeURIComponent(searchTerm)}`
  );

  fetch(`/api/users/search?searchTerm=${encodeURIComponent(searchTerm)}`)
    .then((response) => {
      console.log("API response status:", response.status);
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }
      return response.json();
    })
    .then((users) => {
      console.log("Users found:", users);
      displayUserSearchResults(users);
    })
    .catch((error) => {
      console.error("Error searching users:", error);
      resultsContainer.innerHTML =
        '<div class="text-center text-red-500 py-4">Có lỗi xảy ra khi tìm kiếm</div>';
    });
}

function displayUserSearchResults(users) {
  const resultsContainer = document.getElementById("userSearchResults");

  if (users.length === 0) {
    resultsContainer.innerHTML =
      '<div class="text-center text-gray-500 py-4">Không tìm thấy người dùng nào</div>';
    return;
  }

  const html = users
    .map(
      (user) => `
        <div class="flex items-center p-3 hover:bg-gray-50 cursor-pointer border-b border-gray-100" onclick="startChatWithUser(${
          user.userId
        }, '${user.username}', '${user.fullName}', '${user.avatarUrl || ""}')">
          <img src="${
            user.avatarUrl || "https://via.placeholder.com/40"
          }" alt="${user.username}" class="w-10 h-10 rounded-full mr-3">
          <div class="flex-1">
            <div class="font-medium text-gray-900">${
              user.fullName || user.username
            }</div>
            <div class="text-sm text-gray-500">@${user.username}</div>
          </div>
          <i class="fas fa-comment text-blue-500"></i>
        </div>
      `
    )
    .join("");

  resultsContainer.innerHTML = html;
}

// Utility functions
function debounce(func, wait) {
  let timeout;
  return function executedFunction(...args) {
    const later = () => {
      clearTimeout(timeout);
      func(...args);
    };
    clearTimeout(timeout);
    timeout = setTimeout(later, wait);
  };
}

// Profile functions
function toggleUserMenu() {
  const menu = document.getElementById("userMenu");
  if (menu) {
    menu.classList.toggle("hidden");
  }
}

function logout() {
  if (confirm("Bạn có chắc chắn muốn đăng xuất?")) {
    window.location.href = "/logout";
  }
}

function openProfileModal() {
  const modal = document.getElementById("profileModal");
  if (modal) {
    modal.classList.remove("hidden");
    toggleUserMenu(); // Close user menu

    // Ensure form event listener is attached
    setupProfileForm();
  }
}

function setupProfileForm() {
  const profileForm = document.getElementById("profileForm");
  if (profileForm && !profileForm.hasAttribute("data-listener-attached")) {
    profileForm.setAttribute("data-listener-attached", "true");

    profileForm.addEventListener("submit", function (e) {
      e.preventDefault();

      const formData = new FormData(this);

      fetch("/profile", {
        method: "POST",
        body: formData,
      })
        .then((response) => {
          if (response.ok) {
            return response.text();
          } else {
            return response.text().then((text) => Promise.reject(text));
          }
        })
        .then((data) => {
          if (data === "Success") {
            showProfileMessage(
              "profileSuccess",
              "Cập nhật thông tin thành công!"
            );
            // Reload user info from session
            setTimeout(() => {
              window.location.reload();
            }, 1500);
          } else {
            showProfileMessage(
              "profileError",
              data || "Có lỗi xảy ra khi cập nhật thông tin"
            );
          }
        })
        .catch((error) => {
          console.error("Error updating profile:", error);
          showProfileMessage(
            "profileError",
            typeof error === "string"
              ? error
              : "Có lỗi xảy ra khi cập nhật thông tin"
          );
        });
    });
  }
}

function closeProfileModal() {
  const modal = document.getElementById("profileModal");
  if (modal) {
    modal.classList.add("hidden");
  }
}

// Search members for group creation
function searchMembersForGroup() {
  const searchTerm = document.getElementById("memberSearchInput").value.trim();
  const resultsContainer = document.getElementById("memberSearchResults");

  if (searchTerm.length < 2) {
    resultsContainer.innerHTML = "";
    return;
  }

  fetch(`/api/users/search?searchTerm=${encodeURIComponent(searchTerm)}`)
    .then((response) => response.json())
    .then((users) => {
      displayMemberSearchResults(users);
    })
    .catch((error) => {
      console.error("Error searching members:", error);
    });
}

function displayMemberSearchResults(users) {
  const resultsContainer = document.getElementById("memberSearchResults");

  const html = users
    .map((user) => {
      const isSelected = selectedMembers.some((m) => m.userId === user.userId);
      if (isSelected) return "";

      return `
          <div class="flex items-center p-2 hover:bg-gray-50 cursor-pointer" onclick="addMemberToGroup(${
            user.userId
          }, '${user.username}', '${user.fullName}', '${
        user.avatarUrl || ""
      }')">
            <img src="${
              user.avatarUrl || "https://via.placeholder.com/32"
            }" alt="${user.username}" class="w-8 h-8 rounded-full mr-2">
            <div class="flex-1">
              <div class="text-sm font-medium text-gray-900">${
                user.fullName || user.username
              }</div>
              <div class="text-xs text-gray-500">@${user.username}</div>
            </div>
            <i class="fas fa-plus text-green-500"></i>
          </div>
        `;
    })
    .join("");

  resultsContainer.innerHTML = html;
}

function addMemberToGroup(userId, username, fullName, avatarUrl) {
  if (!selectedMembers.some((m) => m.userId === userId)) {
    selectedMembers.push({ userId, username, fullName, avatarUrl });
    updateSelectedMembersList();
    searchMembersForGroup(); // Refresh search results
  }
}

function removeMemberFromGroup(userId) {
  selectedMembers = selectedMembers.filter((m) => m.userId !== userId);
  updateSelectedMembersList();
  searchMembersForGroup(); // Refresh search results
}

function updateSelectedMembersList() {
  const container = document.getElementById("selectedMembersList");
  if (!container) return; // Safety guard

  if (selectedMembers.length === 0) {
    container.innerHTML =
      '<div class="text-sm text-gray-500">Chưa chọn thành viên nào</div>';
    return;
  }

  const html = selectedMembers
    .map(
      (member) => `
        <div class="flex items-center bg-blue-100 rounded-full px-3 py-1">
          <img src="${
            member.avatarUrl || "https://via.placeholder.com/24"
          }" alt="${member.username}" class="w-6 h-6 rounded-full mr-2">
          <span class="text-sm text-blue-800">${
            member.fullName || member.username
          }</span>
          <button onclick="removeMemberFromGroup(${
            member.userId
          })" class="ml-2 text-blue-600 hover:text-blue-800">
            <i class="fas fa-times text-xs"></i>
          </button>
        </div>
      `
    )
    .join("");
  container.innerHTML = html;
}

function handleCreateGroup(event) {
  event.preventDefault();

  const groupName = document.getElementById("groupNameInput").value.trim();
  if (!groupName) {
    alert("Vui lòng nhập tên nhóm");
    return;
  }

  if (selectedMembers.length === 0) {
    alert("Vui lòng chọn ít nhất một thành viên");
    return;
  }

  const memberIds = selectedMembers.map((m) => m.userId);

  fetch("/api/chats/group", {
    method: "POST",
    headers: {
      "Content-Type": "application/x-www-form-urlencoded",
    },
    body: `chatName=${encodeURIComponent(groupName)}&memberIds=${memberIds.join(
      ","
    )}`,
  })
    .then((response) => response.json())
    .then((chat) => {
      closeCreateGroupModal();
      loadChatList();
      openChat(chat.chatId);
    })
    .catch((error) => {
      console.error("Error creating group:", error);
      alert("Có lỗi xảy ra khi tạo nhóm");
    });
}

function startChatWithUser(userId, username, fullName, avatarUrl) {
  console.log("startChatWithUser (deferred creation) userId=", userId);
  closeSearchUsersModal();
  pendingPrivateChat = { userId, username, fullName, avatarUrl };
  currentChatId = null; // ensure no existing chat id

  // Prepare UI for a new private chat draft without creating it yet
  const header = document.getElementById("chatHeader");
  const headerContent = document.getElementById("chatHeaderContent");
  const msgInputContainer = document.getElementById("messageInputContainer");
  const messagesContainer = document.getElementById("messagesContainer");
  if (header) header.classList.remove("hidden");
  if (msgInputContainer) msgInputContainer.classList.remove("hidden");
  if (messagesContainer) {
    messagesContainer.innerHTML = `<div class="text-center text-gray-500 py-8">Bắt đầu trò chuyện với <span class="font-medium">${
      fullName || username
    }</span></div>`;
  }
  if (headerContent) {
    headerContent.innerHTML = `
      <div class="flex items-center space-x-3">
        <img src="${avatarUrl || "https://via.placeholder.com/40"}" alt="${
      fullName || username
    }" class="w-10 h-10 rounded-full"/>
        <div>
          <h3 class="text-lg font-semibold text-gray-900">${
            fullName || username
          }</h3>
          <p class="text-sm text-gray-500">Chưa có đoạn chat</p>
        </div>
      </div>
      <div class="flex items-center space-x-2">
        <button class="p-2 text-gray-400 cursor-not-allowed" title="Chưa khả dụng">
          <i class="fas fa-info-circle"></i>
        </button>
      </div>`;
  }
}

function createPrivateChatAndSend(firstMessageContent) {
  if (!pendingPrivateChat) return;
  const otherUserId = pendingPrivateChat.userId;
  console.log("Creating private chat with", otherUserId);
  fetch("/api/chats/private", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: `otherUserId=${otherUserId}`,
  })
    .then((r) => r.json())
    .then((chat) => {
      // Normalize and cache
      chatCache[chat.chatId] = chat;
      pendingPrivateChat = null; // clear draft state
      openChat(chat.chatId); // sets currentChatId and subscribes
      sendMessageViaWebSocket(chat.chatId, firstMessageContent, "TEXT");
      loadChatList();
    })
    .catch((err) => {
      console.error("Error creating private chat:", err);
      alert("Không thể tạo chat riêng");
    });
}

function loadChatList() {
  fetch("/api/chats")
    .then((response) => response.json())
    .then((chats) => {
      displayChatList(chats);
    })
    .catch((error) => {
      console.error("Error loading chats:", error);
    });
}

function displayChatList(chats) {
  const chatListContainer = document.getElementById("chatList");
  chats.forEach((c) => {
    chatCache[c.chatId] = c;
  });

  if (!chats || chats.length === 0) {
    chatListContainer.innerHTML =
      '<div class="text-center text-gray-500 py-8">Chưa có cuộc trò chuyện nào</div>';
    return;
  }

  const html = chats
    .map((chat) => {
      const participants = getParticipants(chat);
      const isGroup = chat.isGroup === true || chat.isGroup === "true";
      const chatName = isGroup
        ? chat.chatName || "Nhóm"
        : getPrivateChatName(chat);
      const avatarUrl = isGroup
        ? "https://via.placeholder.com/40?text=G"
        : getPrivateChatAvatar(chat);
      const unreadCount = chat.unreadCount || 0;
      const unread =
        unreadCount > 0
          ? `<span class="bg-blue-500 text-white text-xs rounded-full px-2 py-1">${unreadCount}</span>`
          : "";

      return `
          <div class="chat-item p-3 m-2 rounded-lg hover:bg-gray-100 cursor-pointer border border-gray-200 shadow-sm" onclick="openChat(${
            chat.chatId
          })">
            <div class="flex items-center space-x-3">
              <div class="relative">
                <img src="${avatarUrl}" alt="${chatName}" class="w-10 h-10 rounded-full">
                ${
                  isGroup
                    ? '<div class="absolute -bottom-1 -right-1 w-4 h-4 bg-blue-500 border-2 border-white rounded-full"></div>'
                    : ""
                }
              </div>
              <div class="flex-1 min-w-0">
                <div class="flex items-center justify-between">
                  <h4 class="text-sm font-medium text-gray-900 truncate">${chatName}</h4>
                  ${unread}
                </div>
                <p class="text-xs text-gray-500 truncate">${
                  (chat.lastMessage &&
                    (chat.lastMessage.content || chat.lastMessage.text)) ||
                  "Chưa có tin nhắn"
                }</p>
              </div>
            </div>
          </div>
        `;
    })
    .join("");

  chatListContainer.innerHTML = html;
}

// Helper to unify participants array naming from backend
function getParticipants(chat) {
  return chat.members || chat.participants || chat.participantList || [];
}

function getPrivateChatName(chat) {
  const participants = getParticipants(chat);
  const otherUser = participants.find((m) => m.userId !== window.currentUserId);
  return otherUser
    ? otherUser.fullName || otherUser.username || "Người dùng"
    : "Unknown";
}

function getPrivateChatAvatar(chat) {
  const participants = getParticipants(chat);
  const otherUser = participants.find((m) => m.userId !== window.currentUserId);
  return otherUser
    ? otherUser.avatarUrl || "https://via.placeholder.com/40"
    : "https://via.placeholder.com/40";
}

function openChat(chatId) {
  currentChatId = chatId;
  window.currentChatId = chatId; // Make it globally available

  if (stompClient && isConnected) {
    subscribeToChat(chatId);
  }

  document.getElementById("chatHeader").classList.remove("hidden");
  document.getElementById("messageInputContainer").classList.remove("hidden");

  loadChatDetails(chatId);
  loadMessages(chatId);
  // Load tasks AFTER messages to avoid race conditions
  loadTasksForCurrentChat();
  markAsReadViaWebSocket(chatId);
}

function loadChatDetails(chatId) {
  const chat = chatCache[chatId];
  if (!chat) return;
  const participants = getParticipants(chat);
  const isGroup = chat.isGroup === true || chat.isGroup === "true";
  const chatName = isGroup ? chat.chatName || "Nhóm" : getPrivateChatName(chat);
  const avatarUrl = isGroup
    ? "https://via.placeholder.com/40?text=G"
    : getPrivateChatAvatar(chat);

  const headerContent = document.getElementById("chatHeaderContent");
  headerContent.innerHTML = `
        <div class="flex items-center space-x-3">
            <img src="${avatarUrl}" alt="${chatName}" class="w-10 h-10 rounded-full">
            <div>
                <h3 class="text-lg font-semibold text-gray-900">${chatName}</h3>
                <p class="text-sm text-gray-500">${
                  isGroup
                    ? `${participants.length} thành viên`
                    : "Đang hoạt động"
                }</p>
            </div>
        </div>
    `;
}

function loadMessages(chatId) {
  fetchJsonSafe(`/api/messages/chat/${chatId}?page=0&size=100`)
    .then((messages) => {
      if (!messages) messages = [];
      if (
        !Array.isArray(messages) &&
        messages.content &&
        Array.isArray(messages.content)
      ) {
        messages = messages.content;
      }
      displayMessages(messages);
    })
    .catch((error) => {
      console.error("Error loading messages:", error);
      displayMessages([]);
    });
}

function fetchJsonSafe(url, options) {
  return fetch(url, options || {})
    .then((response) => {
      if (!response.ok) {
        if (response.status === 404) return null; // signal fallback
        if (response.status === 204) return []; // no content
        throw new Error(`HTTP ${response.status}`);
      }
      const len = response.headers.get("content-length");
      if (len === "0" || len === null) {
        // Might still have body; attempt text then parse if not empty
        return response.text().then((t) => (t ? JSON.parse(t) : []));
      }
      return response.json();
    })
    .catch((err) => {
      console.warn("fetchJsonSafe error for", url, err);
      return null; // allow caller to decide fallback
    });
}

function createMessageElement(message) {
  const currentUserIdNum = Number(window.currentUserId);
  const isOwn = Number(message.senderId) === currentUserIdNum;
  const name = isOwn
    ? "Bạn"
    : message.senderFullName ||
      message.senderUsername ||
      message.senderName ||
      "Người dùng";
  const avatar =
    message.senderAvatarUrl ||
    message.avatarUrl ||
    "/images/avatars/default-avatar.png";
  const ts = message.sentAt || message.createdAt || message.timestamp;

  // Wrapper line
  const line = document.createElement("div");
  line.className = `flex items-center mb-3 ${
    isOwn ? "justify-end" : "justify-start"
  }`;
  if (ts) {
    line.setAttribute("data-ts", ts);
  }

  // Avatar element
  const avatarImg = document.createElement("img");
  avatarImg.src = avatar;
  avatarImg.alt = name;
  avatarImg.className =
    "w-8 h-8 rounded-full object-cover border border-gray-300";

  // Message block
  const block = document.createElement("div");
  block.className = `max-w-xs sm:max-w-sm flex flex-col ${
    isOwn ? "items-end mr-2" : "items-start ml-2"
  }`;

  if (!isOwn) {
    const nameEl = document.createElement("div");
    nameEl.className = "text-xs text-gray-500 font-medium mb-1";
    nameEl.textContent = name;
    block.appendChild(nameEl);
  }

  const bubble = document.createElement("div");
  bubble.className = `px-4 py-2 rounded-2xl text-sm shadow ${
    isOwn
      ? "bg-blue-500 text-white rounded-br-none"
      : "bg-gray-200 text-gray-900 rounded-bl-none"
  }`;
  bubble.textContent = message.content || "";

  const timeEl = document.createElement("div");
  timeEl.className = `mt-1 text-[10px] tracking-wide ${
    isOwn ? "text-blue-100" : "text-gray-500"
  }`;
  timeEl.textContent = ts ? formatMessageTime(ts) : "";

  block.appendChild(bubble);
  block.appendChild(timeEl);

  if (isOwn) {
    // Own: bubble then avatar on right
    line.appendChild(block);
    line.appendChild(avatarImg);
  } else {
    // Other: avatar left, bubble after
    line.appendChild(avatarImg);
    line.appendChild(block);
  }

  return line;
}

function displayMessages(messages) {
  const container = document.getElementById("messagesContainer");

  if (!Array.isArray(messages)) messages = [];

  // Sort ascending by timestamp so older messages at top, newer at bottom
  messages.sort((a, b) => {
    const getTs = (m) =>
      new Date(m.sentAt || m.createdAt || m.timestamp || m.time || 0).getTime();
    return getTs(a) - getTs(b);
  });

  if (messages.length === 0) {
    container.innerHTML =
      '<div class="text-center text-gray-500 py-8">Chưa có tin nhắn nào</div>';
    return;
  }

  const currentUserIdNum = Number(window.currentUserId);
  console.log("Current User ID for message display:", currentUserIdNum);

  const uniqueSenders = [...new Set(messages.map((m) => m.senderId))];
  if (uniqueSenders.length === 1 && messages.length > 1) {
    console.warn(
      "⚠️ ALL MESSAGES HAVE THE SAME SENDER ID! Backend có thể đang trả sai dữ liệu. Hãy kiểm tra JSON response của /api/messages/chat/{chatId} trong Network tab."
    );
  }

  // Build DOM nodes
  container.innerHTML = ""; // Clear container

  messages.forEach((message) => {
    const messageElement = createMessageElement(message);
    container.appendChild(messageElement);
  });

  container.scrollTop = container.scrollHeight;
}

function formatMessageTime(timestamp) {
  const date = new Date(timestamp);
  const now = new Date();
  const diffInHours = (now - date) / (1000 * 60 * 60);

  if (diffInHours < 24) {
    return date.toLocaleTimeString("vi-VN", {
      hour: "2-digit",
      minute: "2-digit",
    });
  } else {
    return date.toLocaleDateString("vi-VN", {
      day: "2-digit",
      month: "2-digit",
    });
  }
}

function handleSendMessage(event) {
  // override previous definition
  event.preventDefault();
  const messageInput = document.getElementById("messageInput");
  if (!messageInput) return;
  const content = (messageInput.value || "").trim();
  if (!content) return;

  // If this is a draft private chat, create chat first then send
  if (!currentChatId && pendingPrivateChat) {
    createPrivateChatAndSend(content);
    messageInput.value = "";
    return;
  }

  if (!currentChatId) {
    console.warn("No chat selected and no pending private chat");
    return;
  }

  // Normal flow via WebSocket
  sendMessageViaWebSocket(currentChatId, content, "TEXT");
  messageInput.value = "";
}

function handleIncomingMessage(message) {
  // If this message is for the current chat, add it to the display
  if (Number(message.chatId) === Number(currentChatId)) {
    addMessageToDisplay(message);
  }

  // Update chat list to show new message
  loadChatList();

  // Show notification if not current chat
  if (
    Number(message.chatId) !== Number(currentChatId) &&
    window.notificationManager
  ) {
    window.notificationManager.showMessageNotification(
      message.senderName,
      message.content,
      message.chatId
    );
  }
}

function handleMessageRead(readInfo) {
  // Update read status in UI if needed
  console.log("Message read:", readInfo);
}

function addMessageToDisplay(message) {
  const container = document.getElementById("messagesContainer");
  const messageElement = createMessageElement(message);

  // Insert in correct chronological order (in case of out-of-order arrival)
  const newTs = new Date(
    messageElement.getAttribute("data-ts") || new Date()
  ).getTime();
  const children = Array.from(container.children);
  let inserted = false;
  for (let i = children.length - 1; i >= 0; i--) {
    const childTs = new Date(
      children[i].getAttribute("data-ts") || 0
    ).getTime();
    if (childTs <= newTs) {
      children[i].insertAdjacentElement("afterend", messageElement);
      inserted = true;
      break;
    }
  }
  if (!inserted) {
    // Either list empty or all existing are newer
    container.insertAdjacentElement("afterbegin", messageElement);
  }

  container.scrollTop = container.scrollHeight;
}

// Profile form handling
function previewModalAvatar(input) {
  if (input.files && input.files[0]) {
    const file = input.files[0];

    // Validate file type
    if (!file.type.startsWith("image/")) {
      alert("Vui lòng chọn file ảnh");
      input.value = "";
      return;
    }

    // Validate file size (5MB)
    if (file.size > 5 * 1024 * 1024) {
      alert("File quá lớn. Vui lòng chọn file nhỏ hơn 5MB");
      input.value = "";
      return;
    }

    const reader = new FileReader();
    reader.onload = function (e) {
      const preview = document.getElementById("modalAvatarPreview");
      if (preview) {
        preview.src = e.target.result;
      }
    };
    reader.readAsDataURL(file);
  }
}

// Profile form submission
document.addEventListener("DOMContentLoaded", function () {
  const profileForm = document.getElementById("profileForm");
  if (profileForm) {
    profileForm.addEventListener("submit", function (e) {
      e.preventDefault();

      const formData = new FormData(this);

      fetch("/profile", {
        method: "POST",
        body: formData,
      })
        .then((response) => {
          console.log("Response status:", response.status);
          if (response.ok) {
            return response.text();
          } else {
            return response.text().then((text) => {
              console.log("Error response:", text);
              return Promise.reject(text);
            });
          }
        })
        .then((data) => {
          if (data === "Success") {
            showProfileMessage(
              "profileSuccess",
              "Cập nhật thông tin thành công!"
            );
            // Reload user info from session
            setTimeout(() => {
              window.location.reload();
            }, 1500);
          } else {
            showProfileMessage(
              "profileError",
              data || "Có lỗi xảy ra khi cập nhật thông tin"
            );
          }
        })
        .catch((error) => {
          console.error("Error updating profile:", error);
          showProfileMessage(
            "profileError",
            typeof error === "string"
              ? error
              : "Có lỗi xảy ra khi cập nhật thông tin"
          );
        });
    });
  }
});

function showProfileMessage(elementId, message) {
  const element = document.getElementById(elementId);
  const textElement = document.getElementById(elementId + "Text");

  if (element && textElement) {
    textElement.textContent = message;
    element.classList.remove("hidden");

    // Hide after 3 seconds
    setTimeout(() => {
      element.classList.add("hidden");
    }, 3000);
  }
}

// Expose init so page can manually call after dynamic load
window.initializeMessagePage = initializeMessagePage;

// Make functions globally available
window.openSearchUsersModal = openSearchUsersModal;
window.closeSearchUsersModal = closeSearchUsersModal;
window.openCreateGroupModal = openCreateGroupModal;
window.closeCreateGroupModal = closeCreateGroupModal;
window.toggleUserMenu = toggleUserMenu;
window.logout = logout;
window.openProfileModal = openProfileModal;
window.closeProfileModal = closeProfileModal;
window.setupProfileForm = setupProfileForm;
window.addMemberToGroup = addMemberToGroup;

// Task Management Functions
function handleNewTask(task) {
  console.log("New task received:", task);
  addTaskToUI(task);
  updateTaskStats();

  // Show notification if it's not the current user's task
  if (task.user.userId !== window.currentUserId) {
    showTaskNotification(task);
  }

  // Add to cache & re-render respecting filter
  allTasksCache.unshift(task);
  renderFilteredTasks();
}

function addTaskToUI(task) {
  const todoList = document.getElementById("todoList");
  if (!todoList) return;

  // Hide empty message when adding first task
  const emptyMessage = document.getElementById("todoListEmpty");
  if (emptyMessage) {
    emptyMessage.style.display = "none";
  }

  const taskElement = createTaskElement(task);
  todoList.insertBefore(taskElement, todoList.firstChild);
}

function createTaskElement(task) {
  const taskDiv = document.createElement("div");
  taskDiv.className =
    "todo-item bg-white border border-gray-200 rounded-lg p-3 hover:shadow-md transition-shadow";
  taskDiv.dataset.taskId = task.taskId;

  const statusVal = (task.status || "").toString();
  const isCompleted = statusVal === "completed" || statusVal === "COMPLETED";
  const dueDateText = formatDueDate(task.dueDate);

  taskDiv.innerHTML = `
    <div class="flex items-start space-x-3">
      <input
        type="checkbox"
        class="mt-1 w-4 h-4 text-blue-600 rounded focus:ring-blue-500 task-checkbox"
        ${isCompleted ? "checked" : ""}
        onchange="onTaskCheckboxChange(${task.taskId}, this)"
        ${isCompleted ? "disabled" : ""}
      />
      <div class="flex-1 min-w-0">
        <h4 class="text-sm font-medium text-gray-900 ${
          isCompleted ? "line-through" : ""
        }">
          ${task.description}
        </h4>
        <p class="text-xs text-gray-500 mt-1">
          Từ: ${task.user.username} ${
    dueDateText ? "• Hạn: " + dueDateText : ""
  }
        </p>
        <div class="flex items-center mt-2 space-x-2">
          <span class="inline-flex items-center px-2 py-1 rounded-full text-xs font-medium ${
            isCompleted
              ? "bg-green-100 text-green-800"
              : "bg-blue-100 text-blue-800"
          }">
            ${isCompleted ? "Đã Hoàn Thành" : "Đang Chờ"}
          </span>
          ${
            task.completionNote
              ? `<button onclick="showTaskNote(${task.taskId})" class="text-xs text-blue-600 hover:text-blue-800" title="Xem ghi chú hoàn thành"><i class="fas fa-sticky-note"></i></button>`
              : ""
          }
        </div>
      </div>
    </div>
  `;

  return taskDiv;
}

function formatDueDate(dueDate) {
  if (!dueDate) return "";

  const date = new Date(dueDate);
  const now = new Date();
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
  const taskDate = new Date(
    date.getFullYear(),
    date.getMonth(),
    date.getDate()
  );

  if (taskDate.getTime() === today.getTime()) {
    return date.toLocaleTimeString("vi-VN", {
      hour: "2-digit",
      minute: "2-digit",
    });
  } else if (taskDate < today) {
    return "Quá hạn";
  } else {
    return date.toLocaleDateString("vi-VN");
  }
}

function capitalizeFirst(str) {
  return str.charAt(0).toUpperCase() + str.slice(1);
}

function toggleTaskStatus(taskId, isCompleted) {
  const status = isCompleted ? "completed" : "pending";
  fetch(`/api/tasks/${taskId}/status`, {
    method: "PUT",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: `status=${status}`,
  })
    .then((response) => {
      if (!response.ok) throw new Error("Failed to update task status");
      return response.json();
    })
    .then((updatedTask) => {
      updateTaskInCache(updatedTask);
      renderFilteredTasks(); // re-render whole list to respect current filter & stats
    })
    .catch((error) => {
      console.error("Error updating task status:", error);
      // Revert checkbox visual state
      const checkbox = document.querySelector(
        `[data-task-id="${taskId}"] .task-checkbox`
      );
      if (checkbox) checkbox.checked = !isCompleted;
    });
}

function updateTaskInCache(updatedTask) {
  if (!allTasksCache || !updatedTask) return;
  const idx = allTasksCache.findIndex((t) => t.taskId === updatedTask.taskId);
  if (idx !== -1) {
    allTasksCache[idx] = updatedTask; // replace
  }
}

function updateTaskStats() {
  // Use full cache (independent of current filter)
  const totalTasks = allTasksCache.length;
  const completed = allTasksCache.filter(
    (t) => t.status === "completed" || t.status === "COMPLETED"
  ).length;
  const pending = totalTasks - completed;
  const pendingCount = document.getElementById("pendingCount");
  const completedCount = document.getElementById("completedCount");
  if (pendingCount) pendingCount.textContent = pending;
  if (completedCount) completedCount.textContent = completed;
}

function showTaskNotification(task) {
  if (
    window.notificationManager &&
    window.notificationManager.isPermissionGranted()
  ) {
    window.notificationManager.showNotification("Công việc mới", {
      body: `${task.user.username} đã tạo: ${task.description}`,
      icon: "/favicon.ico",
      tag: "new-task",
    });
  }
}

function showTaskNote(taskId) {
  fetch(`/api/tasks/${taskId}`)
    .then((response) => response.json())
    .then((task) => {
      if (task.completionNote) {
        alert(`Ghi chú hoàn thành:\n\n${task.completionNote}`);
      }
    })
    .catch((error) => console.error("Error fetching task note:", error));
}

function loadTasksForCurrentChat() {
  if (!currentChatId) return;
  fetch(`/api/tasks/chat/${currentChatId}`)
    .then((r) => r.json())
    .then((tasks) => {
      allTasksCache = tasks || [];
      renderFilteredTasks();
    })
    .catch((err) => console.error("Error loading tasks:", err));
}

function displayTasks(tasks) {
  const todoList = document.getElementById("todoList");
  if (!todoList) return;

  // Clear existing tasks
  todoList.innerHTML = "";

  if (!tasks || tasks.length === 0) {
    // Show empty message if no tasks
    todoList.innerHTML = `
      <div class="text-center text-gray-500 py-8" id="todoListEmpty">
        <i class="fas fa-tasks text-3xl text-gray-300 mb-3"></i>
        <p class="text-sm">Chưa có task nào</p>
        <p class="text-xs text-gray-400 mt-1">Gửi tin nhắn với @Todo để tạo task mới</p>
      </div>
    `;
    return;
  }

  // Display tasks
  tasks.forEach((task) => {
    const taskElement = createTaskElement(task);
    todoList.appendChild(taskElement);
  });
}

// Load tasks when chat is opened
function loadTasks() {
  loadTasksForCurrentChat();
}

function onTaskCheckboxChange(taskId, checkboxEl) {
  const task = allTasksCache.find((t) => t.taskId === taskId);
  if (!task) return;
  const wasCompleted =
    (task.status || "").toString().toLowerCase() === "completed";
  if (wasCompleted) {
    // Should never allow unchecking (checkbox disabled, but safeguard)
    checkboxEl.checked = true;
    return;
  }
  if (checkboxEl.checked) {
    // Open modal to confirm completion
    pendingCompleteTaskId = taskId;
    openCompleteTaskModal();
  } else {
    // If user tried to uncheck while not completed yet, just keep pending state
    checkboxEl.checked = false;
  }
}

function openCompleteTaskModal() {
  const modal = document.getElementById("completeTaskModal");
  const noteInput = document.getElementById("completionNoteInput");
  const error = document.getElementById("completionNoteError");
  if (modal) {
    modal.classList.remove("hidden");
    modal.classList.add("flex");
    if (noteInput) {
      noteInput.value = "";
      noteInput.focus();
    }
    if (error) error.classList.add("hidden");
  }
}
function closeCompleteTaskModal() {
  const modal = document.getElementById("completeTaskModal");
  if (modal) {
    modal.classList.add("hidden");
    modal.classList.remove("flex");
  }
  pendingCompleteTaskId = null;
}

function confirmCompleteTask() {
  const noteInput = document.getElementById("completionNoteInput");
  const error = document.getElementById("completionNoteError");
  if (!noteInput) return;
  const note = noteInput.value.trim();
  if (!note) {
    if (error) error.classList.remove("hidden");
    return;
  }
  const taskId = pendingCompleteTaskId;
  if (!taskId) return;
  // First set status to completed
  fetch(`/api/tasks/${taskId}/status`, {
    method: "PUT",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: "status=completed",
  })
    .then((r) => {
      if (!r.ok) throw new Error("Status update failed");
      return r.json();
    })
    .then((updatedTask) => {
      // Then add completion note
      return fetch(`/api/tasks/${taskId}/note`, {
        method: "PUT",
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
        body: `note=${encodeURIComponent(note)}`,
      })
        .then((r2) => {
          if (!r2.ok) throw new Error("Note update failed");
          return r2.json();
        })
        .then((taskWithNote) => {
          updateTaskInCache(taskWithNote);
          renderFilteredTasks();
          closeCompleteTaskModal();
        });
    })
    .catch((err) => {
      console.error("Complete task error", err);
      // Revert checkbox if failed
      const cb = document.querySelector(
        `[data-task-id="${taskId}"] .task-checkbox`
      );
      if (cb) cb.checked = false;
      closeCompleteTaskModal();
      alert("Không thể hoàn thành task. Vui lòng thử lại.");
    });
}

function attachCompleteTaskModalListeners() {
  if (document.getElementById("completeTaskModal").getAttribute("data-init")) {
    return; // already attached
  }
  const cancelBtn = document.getElementById("cancelCompleteTaskBtn");
  const confirmBtn = document.getElementById("confirmCompleteTaskBtn");
  if (cancelBtn)
    cancelBtn.addEventListener("click", () => {
      if (pendingCompleteTaskId) {
        const cb = document.querySelector(
          `[data-task-id="${pendingCompleteTaskId}"] .task-checkbox`
        );
        if (cb) cb.checked = false;
      }
      closeCompleteTaskModal();
    });
  if (confirmBtn) confirmBtn.addEventListener("click", confirmCompleteTask);
  const modal = document.getElementById("completeTaskModal");
  if (modal) modal.setAttribute("data-init", "true");
}
