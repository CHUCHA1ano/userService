package com.innowise.userservice.controller;

import com.innowise.userservice.model.dto.UserCreateDto;
import com.innowise.userservice.model.dto.UserResponseDto;
import com.innowise.userservice.model.dto.UserStatusUpdateDto;
import com.innowise.userservice.model.dto.UserUpdateDto;
import com.innowise.userservice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<UserResponseDto> create(@Valid @RequestBody UserCreateDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.create(dto));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @access.isSelf(#id, authentication)")
    public ResponseEntity<UserResponseDto> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.getById(id));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<UserResponseDto>> getAll(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String surname,
            @RequestParam(required = false) Boolean active,
            Pageable pageable) {
        return ResponseEntity.ok(userService.getAll(name, surname, active, pageable));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @access.isSelf(#id, authentication)")
    public ResponseEntity<UserResponseDto> update(@PathVariable UUID id,
                                                  @Valid @RequestBody UserUpdateDto dto) {
        return ResponseEntity.ok(userService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponseDto> changeStatus(@PathVariable UUID id,
                                                        @Valid @RequestBody UserStatusUpdateDto dto) {
        return dto.getActive()
                ? ResponseEntity.ok(userService.activate(id))
                : ResponseEntity.ok(userService.deactivate(id));
    }
}
