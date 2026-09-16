package com.researchassistant.publicsite.dto;

import com.researchassistant.publicsite.entity.ContactResponseChannel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateContactReplyRequest(
        @NotBlank(message = "Response message is required.")
        @Size(min = 2, max = 5000, message = "Response message must be between 2 and 5000 characters.")
        String message,

        @NotNull(message = "Channel is required.")
        ContactResponseChannel channel
) {}
