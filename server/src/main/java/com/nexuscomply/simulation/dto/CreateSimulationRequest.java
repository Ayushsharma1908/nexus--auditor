package com.nexuscomply.simulation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** BUG-007: Request body for POST /api/v1/simulations (B-059) */
public class CreateSimulationRequest {

    @NotBlank(message = "Simulation name is required")
    @Size(min = 3, max = 200, message = "Name must be between 3 and 200 characters")
    private String name;

    private String description;

    public CreateSimulationRequest() {}

    public String getName()              { return name; }
    public void setName(String name)     { this.name = name; }

    public String getDescription()                  { return description; }
    public void setDescription(String description)  { this.description = description; }
}
