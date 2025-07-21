package com.bni.api.repository;

import com.bni.api.entity.LoginAttempt;
import com.bni.api.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import static org.junit.jupiter.api.Assertions.*;
import java.time.LocalDateTime;
import java.util.Optional;

@DataJpaTest
@ActiveProfiles("test")
class LoginAttemptRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private LoginAttemptRepository loginAttemptRepository;

    @Test
    void testFindByUserId() {
        User user = new User("testuser", "test@example.com", "+6281234567890", "Test User", "password");
        User savedUser = entityManager.persistAndFlush(user);

        LoginAttempt attempt = new LoginAttempt(savedUser.getId(), 2, null, LocalDateTime.now());
        entityManager.persistAndFlush(attempt);

        Optional<LoginAttempt> found = loginAttemptRepository.findByUserId(savedUser.getId());
        assertTrue(found.isPresent());
        assertEquals(2, found.get().getFailedAttempts());
        assertEquals(savedUser.getId(), found.get().getUserId());
    }

    @Test
    void testFindByUserIdNotFound() {
        Optional<LoginAttempt> found = loginAttemptRepository.findByUserId("999");
        assertFalse(found.isPresent());
    }

    @Test
    void testSaveLoginAttempt() {
        User user = new User("testuser", "test@example.com", "+6281234567890", "Test User", "password");
        User savedUser = entityManager.persistAndFlush(user);

        LoginAttempt attempt = new LoginAttempt();
        attempt.setUserId(savedUser.getId());
        attempt.setFailedAttempts(1);
        attempt.setLastFailedAttempt(LocalDateTime.now());

        LoginAttempt saved = loginAttemptRepository.save(attempt);
        assertNotNull(saved.getId());
        assertEquals(1, saved.getFailedAttempts());
    }
}