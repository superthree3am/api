package com.bni.api.repository;

import com.bni.api.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Optional;
import com.github.f4b6a3.ulid.UlidCreator;
import org.junit.jupiter.api.BeforeEach;

@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;


    private User testUser; 
    private String testUserUlid;

    @BeforeEach
    void setUp() {
        testUserUlid = UlidCreator.getUlid().toString(); // Hasilkan ULID untuk pengguna
        testUser = new User("testuser", "test@example.com", "+6281234567890", "Test User", "password");
        testUser.setId(testUserUlid); // Tetapkan ULID untuk pengguna
    }

    @Test
    void testFindByUsername() {
        entityManager.persistAndFlush(testUser); // Persist testUser dengan ULID

        Optional<User> found = userRepository.findByUsername("testuser");
        assertTrue(found.isPresent());
        assertEquals("testuser", found.get().getUsername());
        assertEquals(testUserUlid, found.get().getId());
    }

    @Test
    void testFindByUsernameNotFound() {
        Optional<User> found = userRepository.findByUsername("nonexistent");
        assertFalse(found.isPresent());
    }

    @Test
    void testExistsByUsername() {
        entityManager.persistAndFlush(testUser);
        assertTrue(userRepository.existsByUsername("testuser"));
        assertFalse(userRepository.existsByUsername("nonexistent"));
    }

    @Test
    void testExistsByEmail() {
        entityManager.persistAndFlush(testUser);

        assertTrue(userRepository.existsByEmail("test@example.com"));
        assertFalse(userRepository.existsByEmail("nonexistent@example.com"));
    }

    @Test
    void testExistsByPhone() {
        entityManager.persistAndFlush(testUser);

        assertTrue(userRepository.existsByPhone("+6281234567890"));
        assertFalse(userRepository.existsByPhone("+6280000000000"));
    }

    @Test
    void testFindByPhone() {
        entityManager.persistAndFlush(testUser);

        Optional<User> found = userRepository.findByPhone("+6281234567890");
        assertTrue(found.isPresent());
        assertEquals("+6281234567890", found.get().getPhone());
    }
}