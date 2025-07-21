package com.bni.api.repository;

import com.bni.api.entity.LoginAttempt;
import com.bni.api.entity.User;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import static org.junit.jupiter.api.Assertions.*;
import java.time.LocalDateTime;
import java.util.Optional;
import com.github.f4b6a3.ulid.UlidCreator;

@DataJpaTest
@ActiveProfiles("test")
class LoginAttemptRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private LoginAttemptRepository loginAttemptRepository;


    private User testUser; 
    private String testUserUlid; 
    private String testLoginAttemptUlid; 

    @BeforeEach
    void setUp() {
        testUserUlid = UlidCreator.getUlid().toString();
        testLoginAttemptUlid = UlidCreator.getUlid().toString(); 
        testUser = new User("testuser", "test@example.com", "+6281234567890", "Test User", "password");
        testUser.setId(testUserUlid); 
    }

    @Test
    void testFindByUserId() {
        User savedUser = entityManager.persistAndFlush(testUser);

        LoginAttempt attempt = new LoginAttempt();
        attempt.setId(testLoginAttemptUlid); 
        attempt.setUserId(savedUser.getId());
        attempt.setFailedAttempts(2);
        attempt.setLastFailedAttempt(LocalDateTime.now());
        entityManager.persistAndFlush(attempt);

        Optional<LoginAttempt> found = loginAttemptRepository.findByUserId(savedUser.getId());
        assertTrue(found.isPresent());
        assertEquals(2, found.get().getFailedAttempts());
        assertEquals(savedUser.getId(), found.get().getUserId());
        assertEquals(testLoginAttemptUlid, found.get().getId());
    }

    @Test
    void testFindByUserIdNotFound() {
        Optional<LoginAttempt> found = loginAttemptRepository.findByUserId(UlidCreator.getUlid().toString());
        assertFalse(found.isPresent());
    }

    @Test
    void testSaveLoginAttempt() {
        User savedUser = entityManager.persistAndFlush(testUser);

        LoginAttempt attempt = new LoginAttempt();
        attempt.setId(UlidCreator.getUlid().toString()); 
        attempt.setUserId(savedUser.getId()); 
        attempt.setFailedAttempts(1);
        attempt.setLastFailedAttempt(LocalDateTime.now());

        LoginAttempt saved = loginAttemptRepository.save(attempt);
        assertNotNull(saved.getId());
        assertEquals(1, saved.getFailedAttempts());
        assertEquals(attempt.getId(), saved.getId());
    }
}