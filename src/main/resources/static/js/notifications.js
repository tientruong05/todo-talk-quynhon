// Notifications functionality for TodoTalk
class NotificationManager {
    constructor() {
        this.permission = Notification.permission;
        this.init();
    }

    init() {
        // Request notification permission if not already granted
        if (this.permission === 'default') {
            this.requestPermission();
        }
    }

    async requestPermission() {
        try {
            this.permission = await Notification.requestPermission();
        } catch (error) {
            console.error('Error requesting notification permission:', error);
        }
    }

    showNotification(title, options = {}) {
        if (this.permission !== 'granted') {
            console.warn('Notification permission not granted');
            return;
        }

        // Default options
        const defaultOptions = {
            icon: '/favicon.ico',
            badge: '/favicon.ico',
            tag: 'todotalk-notification',
            requireInteraction: false,
            ...options
        };

        try {
            const notification = new Notification(title, defaultOptions);
            
            // Auto close after 5 seconds if not requiring interaction
            if (!defaultOptions.requireInteraction) {
                setTimeout(() => {
                    notification.close();
                }, 5000);
            }

            return notification;
        } catch (error) {
            console.error('Error showing notification:', error);
        }
    }

    showMessageNotification(senderName, message, chatId) {
        // Don't show notification if the chat is currently open
        if (window.currentChatId === chatId) {
            return;
        }

        this.showNotification(`New message from ${senderName}`, {
            body: message.length > 100 ? message.substring(0, 100) + '...' : message,
            icon: '/favicon.ico',
            tag: `chat-${chatId}`,
            data: { chatId, type: 'message' },
            onclick: function() {
                window.focus();
                if (window.openChat && typeof window.openChat === 'function') {
                    window.openChat(chatId);
                }
                this.close();
            }
        });
    }

    showTaskNotification(taskTitle, dueTime) {
        this.showNotification('Task Reminder', {
            body: `Task "${taskTitle}" is due ${dueTime}`,
            icon: '/favicon.ico',
            tag: 'task-reminder',
            requireInteraction: true,
            data: { type: 'task' }
        });
    }

    isSupported() {
        return 'Notification' in window;
    }

    isPermissionGranted() {
        return this.permission === 'granted';
    }
}

// Initialize notification manager
window.notificationManager = new NotificationManager();

// Export for use in other scripts
if (typeof module !== 'undefined' && module.exports) {
    module.exports = NotificationManager;
}
