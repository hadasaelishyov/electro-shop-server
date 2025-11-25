package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import lombok.Data;

@Data

public class ProductImageDto {
    @JsonProperty("id")
    private Long id;
    @Column(length = 2000)
    private String imageUrl;
    @JsonProperty("isMain")
    private boolean isMain;
}

