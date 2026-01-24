package com.reviewsystem.dto;

import com.reviewsystem.entity.ReviewStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewModerationDTO {

    @NotNull(message = "Review status is required")
    private ReviewStatus status;

    private String moderationNote;
}
