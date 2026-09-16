package com.researchassistant.publicsite.dto;

import java.util.UUID;

public record AssignContactRequest(
        UUID assignedToUserId
) {}
