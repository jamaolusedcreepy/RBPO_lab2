package com.support.controller;

import com.support.domain.User;
import com.support.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    // Создание пользователя
    @PostMapping
    public ResponseEntity<?> createUser(@Valid @RequestBody User user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            return ResponseEntity.badRequest()
                    .body("Error: Email is already in use!");
        }
        User savedUser = userRepository.save(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedUser);
    }

    // Получение всех пользователей
    @GetMapping
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // Получение пользователя по ID
    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        Optional<User> user = userRepository.findById(id);
        return user.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Обновление пользователя
    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @Valid @RequestBody User userDetails) {
        return userRepository.findById(id)
                .map(user -> {
                    if (!user.getEmail().equals(userDetails.getEmail()) &&
                            userRepository.existsByEmail(userDetails.getEmail())) {
                        return ResponseEntity.badRequest()
                                .body("Error: Email is already in use!");
                    }
                    user.setName(userDetails.getName());
                    user.setEmail(userDetails.getEmail());
                    User updatedUser = userRepository.save(user);
                    return ResponseEntity.ok(updatedUser);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // Удаление пользователя
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        return userRepository.findById(id)
                .map(user -> {
                    userRepository.delete(user);
                    return ResponseEntity.ok().build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}