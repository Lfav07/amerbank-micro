package com.amerbank.auth_server.integration.repository;

import com.amerbank.auth_server.dto.Role;
import com.amerbank.auth_server.model.User;
import com.amerbank.auth_server.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(properties = "spring.cloud.config.enabled=false")
@Testcontainers
@ActiveProfiles("test")
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
class UserRepositoryIntegrationTests {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private UserRepository userRepository;

    @Nested
    @DisplayName("Lookup Operations")
    class LookupTests {


        User savedUser;

        @BeforeEach
        void setUp() {
            savedUser = userRepository.save(
                    User.builder()
                            .email("test@email.com")
                            .password("testPassword")
                            .active(true)
                            .roles(Set.of(Role.ROLE_USER))
                            .build()
            );
        }

        @Test
        @DisplayName("Should find user by id")
        void shouldFindUserById() {
            Optional<User> found = userRepository.findById(savedUser.getId());

            assertTrue(found.isPresent());
            assertEquals(savedUser, found.get());
        }

        @Test
        @DisplayName("Should find user by email")
        void shouldFindUserByEmail() {
            Optional<User> found = userRepository.findByEmailIgnoreCase(savedUser.getEmail());
            assertTrue(found.isPresent());
            assertEquals(savedUser, found.get());
        }

        @Test
        @DisplayName("Should return empty when user not found by id")
        void shouldReturnEmptyWhenUserNotFoundById() {
            Optional<User> found = userRepository.findById(2151L);
            assertTrue(found.isEmpty());
        }

        @Test
        @DisplayName("Should return empty when user not found by email")
        void shouldReturnEmptyWhenUserNotFoundByEmail() {
            Optional<User> found = userRepository.findByEmailIgnoreCase("missing@email.com");
            assertTrue(found.isEmpty());
        }
    }

    @Nested
    @DisplayName("Existence Checking Operations")
    class ExistenceTests {

        User savedUser;

        @BeforeEach
        void setUp() {
            savedUser = userRepository.save(
                    User.builder()
                            .email("test@email.com")
                            .password("testPassword")
                            .active(true)
                            .roles(Set.of(Role.ROLE_USER))
                            .build()
            );
        }

        @Test
        @DisplayName("Should return true when user exists by id")
        void shouldReturnTrueWhenUserExistsById() {
            boolean result = userRepository.existsById(savedUser.getId());
            assertTrue(result);
        }

        @Test
        @DisplayName("Should return true when user exists by email")
        void shouldReturnTrueWhenUserExistsByEmail() {
            boolean result = userRepository.existsByEmailIgnoreCase(savedUser.getEmail());
            assertTrue(result);
        }

        @Test
        @DisplayName("Should return false when user does not exist by id")
        void shouldReturnFalseWhenUserDoesNotExistById() {
            boolean exists = userRepository.existsById(999L);
            assertFalse(exists);
        }

        @Test
        @DisplayName("Should return false when user does not exist by email")
        void shouldReturnFalseWhenUserDoesNotExistByEmail() {
            boolean exists = userRepository.existsByEmailIgnoreCase("notfound@email.com");
            assertFalse(exists);
        }
    }

    @Nested
    @DisplayName("Create Operations")
    class CreateTests {
        @Test
        @DisplayName("Should save user to database")
        void shouldSaveUser() {
            User savedUser = userRepository.save(
                    User.builder()
                            .email("test@email.com")
                            .password("testPassword")
                            .active(true)
                            .roles(Set.of(Role.ROLE_USER))
                            .build()
            );
            assertEquals(1, userRepository.count());
        }
    }

    @Nested
    @DisplayName("Delete Operations")
    class DeleteTests {

        User savedUser;

        @BeforeEach
        void setUp() {
            savedUser = userRepository.save(
                    User.builder()
                            .email("test@email.com")
                            .password("testPassword")
                            .active(true)
                            .roles(Set.of(Role.ROLE_USER))
                            .build()
            );
        }

        @Test
        @DisplayName("Should delete user")
        void shouldDeleteUser() {
            assertEquals(1, userRepository.count());
            userRepository.delete(savedUser);
            assertEquals(0, userRepository.count());
        }
    }


}
