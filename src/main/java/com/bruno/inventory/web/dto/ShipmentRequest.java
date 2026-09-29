package com.bruno.inventory.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ShipmentRequest(
        @NotBlank @Size(max = 200)
        String destination
) {
}
