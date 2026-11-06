package com.innowise.userservice.model.dto;

import com.innowise.userservice.model.validation.NotBlankOrNull;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Past;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserUpdateDto {
    @NotBlankOrNull(message = "Name cannot be empty")
    private String name;

    @NotBlankOrNull(message = "Surname cannot be empty")
    private String surname;

    @Past(message = "Birth date must be in the past")
    private LocalDate birthDate;

    @NotBlankOrNull(message = "Email cannot be empty")
    @Email(message = "Email must be valid")
    private String email;
}
