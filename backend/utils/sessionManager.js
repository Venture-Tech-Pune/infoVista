// Session tracking for admin users
// Simple in-memory store (for production, use Redis or database)
class SessionManager {
    constructor() {
        this.activeSessions = new Map(); // userId -> Set of tokens
        this.MAX_ADMIN_SESSIONS = 2;
    }

    /**
     * Add a new session for a user
     * @param {String} userId - User ID
     * @param {String} token - JWT token
     * @param {String} role - User role
     * @returns {Boolean} - Success status
     */
    addSession(userId, token, role) {
        // Only track admin sessions
        if (role !== 'admin') {
            return true; // Allow unlimited sessions for non-admins
        }

        if (!this.activeSessions.has(userId)) {
            this.activeSessions.set(userId, new Set());
        }

        const userSessions = this.activeSessions.get(userId);

        // Check admin session limit
        if (userSessions.size >= this.MAX_ADMIN_SESSIONS) {
            return false; // Limit reached
        }

        userSessions.add(token);
        return true;
    }

    /**
     * Remove a session
     * @param {String} userId - User ID
     * @param {String} token - JWT token
     */
    removeSession(userId, token) {
        if (this.activeSessions.has(userId)) {
            const userSessions = this.activeSessions.get(userId);
            userSessions.delete(token);

            // Clean up empty sets
            if (userSessions.size === 0) {
                this.activeSessions.delete(userId);
            }
        }
    }

    /**
     * Get active session count for a user
     * @param {String} userId - User ID
     * @returns {Number} - Active session count
     */
    getSessionCount(userId) {
        if (!this.activeSessions.has(userId)) {
            return 0;
        }
        return this.activeSessions.get(userId).size;
    }

    /**
     * Clear all sessions for a user
     * @param {String} userId - User ID
     */
    clearUserSessions(userId) {
        this.activeSessions.delete(userId);
    }
}

// Export singleton instance
module.exports = new SessionManager();
