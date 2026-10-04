package org.example.shop1.model.dto;

public class AiResponseDto {
    private String description;
    public AiResponseDto() {}
    public AiResponseDto(String description) { this.description = description; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
