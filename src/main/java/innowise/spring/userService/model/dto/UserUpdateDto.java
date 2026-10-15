package innowise.spring.userService.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Past;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class UserUpdateDto {
    private String name;

    private String surname;

    @Past(message = "Birth date must be in the past")
    private LocalDate birthDate;

    @Email(message = "Email must be valid")
    private String email;
}
