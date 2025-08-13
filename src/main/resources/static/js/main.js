// Main JavaScript file for TodoTalk
// Global variables
let currentUser = null;
let authToken = null;

// Initialize when DOM is loaded
document.addEventListener("DOMContentLoaded", function () {
  // Load user session
  loadUserSession();

  // Initialize navigation
  initializeNavigation();

  // Initialize notifications
  initializeNotifications();
});

// Load user session from localStorage
function loadUserSession() {
  const token = localStorage.getItem("token");
  const user = localStorage.getItem("user");

  if (token && user) {
    authToken = token;
    currentUser = JSON.parse(user);
    updateNavigation(true);
  } else {
    updateNavigation(false);
  }
}

// Update navigation based on auth status
function updateNavigation(isLoggedIn) {
  const authElements = document.querySelectorAll(
    '[sec\\:authorize="isAuthenticated()"]'
  );
  const unauthElements = document.querySelectorAll(
    '[sec\\:authorize="!isAuthenticated()"]'
  );

  authElements.forEach((el) => {
    el.style.display = isLoggedIn ? "flex" : "none";
  });

  unauthElements.forEach((el) => {
    el.style.display = isLoggedIn ? "none" : "flex";
  });

  if (isLoggedIn && currentUser) {
    const usernameElement = document.querySelector(
      "[th\\:text=\"${user?.username} ?: 'User'\"]"
    );
    if (usernameElement) {
      usernameElement.textContent = currentUser.username;
    }

    const avatarElement = document.querySelector(
      "[th\\:src=\"${user?.avatarUrl} ?: '/images/default-avatar.png'\"]"
    );
    if (avatarElement) {
      avatarElement.src = currentUser.avatarUrl || "/images/default-avatar.png";
    }
  }
}

// Initialize navigation interactions
function initializeNavigation() {
  // User menu dropdown
  const userMenuBtn = document.getElementById("userMenuBtn");
  const userDropdown = document.getElementById("userDropdown");

  if (userMenuBtn && userDropdown) {
    userMenuBtn.addEventListener("click", function (e) {
      e.stopPropagation();
      userDropdown.classList.toggle("hidden");
    });

    // Close dropdown when clicking outside
    document.addEventListener("click", function () {
      userDropdown.classList.add("hidden");
    });
  }

  // Logout functionality
  const logoutLinks = document.querySelectorAll('a[href="/logout"]');
  logoutLinks.forEach((link) => {
    link.addEventListener("click", function (e) {
      e.preventDefault();
      logout();
    });
  });
}

// Initialize notification system
function initializeNotifications() {
  const notificationBtn = document.getElementById("notificationBtn");
  if (notificationBtn) {
    notificationBtn.addEventListener("click", function () {
      // Toggle notifications panel
      console.log("Notifications clicked");
    });
  }
}

// Logout function
function logout() {
  localStorage.removeItem("token");
  localStorage.removeItem("user");
  authToken = null;
  currentUser = null;
  window.location.href = "/login";
}

// API Helper functions
async function apiCall(endpoint, options = {}) {
  const defaultOptions = {
    headers: {
      "Content-Type": "application/json",
    },
  };

  if (authToken) {
    defaultOptions.headers["Authorization"] = `Bearer ${authToken}`;
  }

  const mergedOptions = {
    ...defaultOptions,
    ...options,
    headers: {
      ...defaultOptions.headers,
      ...options.headers,
    },
  };

  try {
    const response = await fetch(endpoint, mergedOptions);

    if (response.status === 401) {
      // Unauthorized - redirect to login
      logout();
      return null;
    }

    return response;
  } catch (error) {
    console.error("API call failed:", error);
    throw error;
  }
}

// Show toast notification
function showToast(message, type = "info", duration = 3000) {
  const toast = document.createElement("div");
  toast.className = `fixed top-4 right-4 px-6 py-4 rounded-lg shadow-lg z-50 ${getToastClasses(
    type
  )}`;
  toast.textContent = message;

  document.body.appendChild(toast);

  // Auto remove after duration
  setTimeout(() => {
    toast.remove();
  }, duration);
}

function getToastClasses(type) {
  switch (type) {
    case "success":
      return "bg-green-100 text-green-800 border border-green-200";
    case "error":
      return "bg-red-100 text-red-800 border border-red-200";
    case "warning":
      return "bg-yellow-100 text-yellow-800 border border-yellow-200";
    default:
      return "bg-blue-100 text-blue-800 border border-blue-200";
  }
}

// Format time ago
function timeAgo(date) {
  const now = new Date();
  const then = new Date(date);
  const diffInMs = now - then;
  const diffInMinutes = Math.floor(diffInMs / 60000);
  const diffInHours = Math.floor(diffInMinutes / 60);
  const diffInDays = Math.floor(diffInHours / 24);

  if (diffInMinutes < 1) return "Just now";
  if (diffInMinutes < 60) return `${diffInMinutes}m ago`;
  if (diffInHours < 24) return `${diffInHours}h ago`;
  if (diffInDays < 7) return `${diffInDays}d ago`;

  return then.toLocaleDateString();
}

// Format date
function formatDate(date, includeTime = false) {
  const d = new Date(date);
  const options = {
    year: "numeric",
    month: "short",
    day: "numeric",
  };

  if (includeTime) {
    options.hour = "2-digit";
    options.minute = "2-digit";
  }

  return d.toLocaleDateString(undefined, options);
}

// Debounce function for search inputs
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

// Escape HTML to prevent XSS
function escapeHtml(text) {
  const div = document.createElement("div");
  div.textContent = text;
  return div.innerHTML;
}

// Check if message contains @Todo
function isTodoMessage(content) {
  return content.includes("@Todo") || content.includes("@todo");
}

// Extract todo content from message
function extractTodoContent(content) {
  const todoRegex = /@[Tt]odo\s+(.+)/;
  const match = content.match(todoRegex);
  return match ? match[1].trim() : content;
}

// Global error handler
window.addEventListener("error", function (e) {
  console.error("Global error:", e.error);
  showToast("An unexpected error occurred. Please refresh the page.", "error");
});

// Handle unhandled promise rejections
window.addEventListener("unhandledrejection", function (e) {
  console.error("Unhandled promise rejection:", e.reason);
  showToast("Something went wrong. Please try again.", "error");
});
