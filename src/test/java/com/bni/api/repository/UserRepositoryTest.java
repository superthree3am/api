package com.bni.api.repository;

import com.bni.api.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Optional;

@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Test
    void testFindByUsername() {
        User user = new User("testuser", "test@example.com", "+6281234567890", "Test User", "password");
        entityManager.persistAndFlush(user);

        Optional<User> found = userRepository.findByUsername("testuser");
        assertTrue(found.isPresent());
        assertEquals("testuser", found.get().getUsername());
    }

    @Test
    void testFindByUsernameNotFound() {
        Optional<User> found = userRepository.findByUsername("nonexistent");
        assertFalse(found.isPresent());
    }

    @Test
    void testExistsByUsername() {
        User user = new User("testuser", "test@example.com", "+6281234567890", "Test User", "password");
        entityManager.persistAndFlush(user);

        assertTrue(userRepository.existsByUsername("testuser"));
        assertFalse(userRepository.existsByUsername("nonexistent"));
    }

    @Test
    void testExistsByEmail() {
        User user = new User("testuser", "test@example.com", "+6281234567890", "Test User", "password");
        entityManager.persistAndFlush(user);

        assertTrue(userRepository.existsByEmail("test@example.com"));
        assertFalse(userRepository.existsByEmail("nonexistent@example.com"));
    }

    @Test
    void testExistsByPhone() {
        User user = new User("testuser", "test@example.com", "+6281234567890", "Test User", "password");
        entityManager.persistAndFlush(user);

        assertTrue(userRepository.existsByPhone("+6281234567890"));
        assertFalse(userRepository.existsByPhone("+6280000000000"));
    }

    @Test
    void testFindByPhone() {
        User user = new User("testuser", "test@example.com", "+6281234567890", "Test User", "password");
        entityManager.persistAndFlush(user);

        Optional<User> found = userRepository.findByPhone("+6281234567890");
        assertTrue(found.isPresent());
        assertEquals("+6281234567890", found.get().getPhone());
    }
}