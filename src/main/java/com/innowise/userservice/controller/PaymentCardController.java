package com.innowise.userservice.controller;

import com.innowise.userservice.model.dto.CardCreateDto;
import com.innowise.userservice.model.dto.CardResponseDto;
import com.innowise.userservice.model.dto.CardUpdateDto;
import com.innowise.userservice.service.PaymentCardService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Card endpoints follow the API spec: creating and listing a user's cards live under
 * /api/users/{userId}/cards, while id-scoped operations stay under /api/cards/{id}.
 */
@RestController
@RequiredArgsConstructor
public class PaymentCardController {

    private final PaymentCardService cardService;

    @PostMapping("/api/users/{userId}/cards")
    @PreAuthorize("hasRole('ADMIN') or @access.isSelf(#userId, authentication)")
    public ResponseEntity<CardResponseDto> create(@PathVariable UUID userId,
                                                  @Valid @RequestBody CardCreateDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cardService.create(userId, dto));
    }

    @GetMapping("/api/users/{userId}/cards")
    @PreAuthorize("hasRole('ADMIN') or @access.isSelf(#userId, authentication)")
    public ResponseEntity<List<CardResponseDto>> getAllByUserId(@PathVariable UUID userId) {
        return ResponseEntity.ok(cardService.getAllByUserId(userId));
    }

    @GetMapping("/api/cards/{id}")
    @PreAuthorize("hasRole('ADMIN') or @access.isCardOwner(#id, authentication)")
    public ResponseEntity<CardResponseDto> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(cardService.getById(id));
    }

    @GetMapping("/api/cards")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<CardResponseDto>> getAll(
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) Boolean active,
            Pageable pageable) {
        return ResponseEntity.ok(cardService.getAll(userId, active, pageable));
    }

    @PutMapping("/api/cards/{id}")
    @PreAuthorize("hasRole('ADMIN') or @access.isCardOwner(#id, authentication)")
    public ResponseEntity<CardResponseDto> update(@PathVariable UUID id,
                                                  @Valid @RequestBody CardUpdateDto dto) {
        return ResponseEntity.ok(cardService.update(id, dto));
    }

    @DeleteMapping("/api/cards/{id}")
    @PreAuthorize("hasRole('ADMIN') or @access.isCardOwner(#id, authentication)")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        cardService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/api/cards/{id}/activate")
    @PreAuthorize("hasRole('ADMIN') or @access.isCardOwner(#id, authentication)")
    public ResponseEntity<CardResponseDto> activate(@PathVariable UUID id) {
        return ResponseEntity.ok(cardService.activate(id));
    }

    @PatchMapping("/api/cards/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN') or @access.isCardOwner(#id, authentication)")
    public ResponseEntity<CardResponseDto> deactivate(@PathVariable UUID id) {
        return ResponseEntity.ok(cardService.deactivate(id));
    }
}
