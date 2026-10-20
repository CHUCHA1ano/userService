package innowise.spring.userService.controller;

import innowise.spring.userService.model.dto.CardCreateDto;
import innowise.spring.userService.model.dto.CardResponseDto;
import innowise.spring.userService.model.dto.CardUpdateDto;
import innowise.spring.userService.model.service.PaymentCardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
public class PaymentCardController {

    private final PaymentCardService cardService;

    @PostMapping
    public ResponseEntity<CardResponseDto> create(@Valid @RequestBody CardCreateDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cardService.create(dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CardResponseDto> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(cardService.getById(id));
    }

    @GetMapping
    public ResponseEntity<Page<CardResponseDto>> getAll(
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) Boolean active,
            Pageable pageable) {
        return ResponseEntity.ok(cardService.getAll(userId, active, pageable));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<CardResponseDto>> getAllByUserId(@PathVariable UUID userId) {
        return ResponseEntity.ok(cardService.getAllByUserId(userId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CardResponseDto> update(@PathVariable UUID id,
                                                  @Valid @RequestBody CardUpdateDto dto) {
        return ResponseEntity.ok(cardService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        cardService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<CardResponseDto> activate(@PathVariable UUID id) {
        return ResponseEntity.ok(cardService.activate(id));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<CardResponseDto> deactivate(@PathVariable UUID id) {
        return ResponseEntity.ok(cardService.deactivate(id));
    }
}
