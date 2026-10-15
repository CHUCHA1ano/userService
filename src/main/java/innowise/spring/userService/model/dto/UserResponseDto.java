package innowise.spring.userService.model.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class UserResponseDto {
    private UUID id;
    private String name;
    private String surname;
    private LocalDate birthDate;
    private String email;
    private Boolean active;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
