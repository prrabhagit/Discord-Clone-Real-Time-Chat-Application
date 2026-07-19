package com.chatapp.server.service;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.chatapp.server.db.dao.UserDao;

import java.security.SecureRandom;
import java.util.List;
import java.util.Optional;

/** Handles registration and login, including password hashing/verification. */
public class AuthService {

    private static final List<String> AVATAR_COLORS = List.of(
            "#5865F2", "#EB459E", "#57F287", "#FEE75C", "#ED4245",
            "#3BA55D", "#FAA61A", "#9B59B6", "#1ABC9C", "#E67E22"
    );

    private final UserDao userDao;
    private final SecureRandom random = new SecureRandom();

    public AuthService(UserDao userDao) {
        this.userDao = userDao;
    }

    public static class AuthException extends RuntimeException {
        public AuthException(String message) {
            super(message);
        }
    }

    public UserDao.UserRecord register(String username, String password) {
        validateUsername(username);
        validatePassword(password);

        if (userDao.usernameExists(username)) {
            throw new AuthException("Username is already taken");
        }

        String hash = BCrypt.withDefaults().hashToString(12, password.toCharArray());
        String avatarColor = AVATAR_COLORS.get(random.nextInt(AVATAR_COLORS.size()));
        int userId = userDao.createUser(username, hash, avatarColor);
        return userDao.findById(userId).orElseThrow(() -> new AuthException("Failed to load newly created user"));
    }

    public UserDao.UserRecord login(String username, String password) {
        Optional<UserDao.UserRecord> record = userDao.findByUsername(username);
        if (record.isEmpty()) {
            throw new AuthException("Invalid username or password");
        }
        BCrypt.Result result = BCrypt.verifyer().verify(password.toCharArray(), record.get().passwordHash());
        if (!result.verified) {
            throw new AuthException("Invalid username or password");
        }
        return record.get();
    }

    private void validateUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new AuthException("Username cannot be empty");
        }
        if (username.length() < 3 || username.length() > 20) {
            throw new AuthException("Username must be between 3 and 20 characters");
        }
        if (!username.matches("^[a-zA-Z0-9_]+$")) {
            throw new AuthException("Username can only contain letters, numbers and underscores");
        }
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 6) {
            throw new AuthException("Password must be at least 6 characters");
        }
    }
}
