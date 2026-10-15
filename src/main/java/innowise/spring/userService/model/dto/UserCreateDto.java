package innowise.spring.userService.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class UserCreateDto {
    @NotBlank(message = "Name cannot be empty")
    private String name;

    @NotBlank(message = "Surname cannot be empty")
    private String surname;

    @NotNull(message = "Birth date cannot be null")
    @Past(message = "Birth date must be in the past")
    private LocalDate birthDate;

    @NotBlank(message = "Email cannot be empty")
    @Email(message = "Email must be valid")
    private String email;
}
