package com.researchassistant.instruments.service;

import com.researchassistant.instruments.dto.InstrumentResponses.CviResult;
import com.researchassistant.instruments.entity.ExpertRatingCriterion;
import com.researchassistant.instruments.entity.ExpertReviewItemRating;
import com.researchassistant.methodology.exception.MethodologyValidationException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CviCalculator {
    public CviResult calculateScaleAverage(List<ExpertReviewItemRating> ratings, int relevanceThreshold) {
        if (ratings == null || ratings.isEmpty()) {
            throw new MethodologyValidationException("CVI requires expert relevance ratings.");
        }
        List<ExpertReviewItemRating> relevantCriterion = ratings.stream()
                .filter(rating -> rating.getCriterion() == ExpertRatingCriterion.RELEVANCE)
                .toList();
        Map<UUID, List<ExpertReviewItemRating>> byItem = relevantCriterion.stream()
                .collect(Collectors.groupingBy(ExpertReviewItemRating::getTargetId));
        if (byItem.isEmpty()) {
            throw new MethodologyValidationException("CVI requires RELEVANCE ratings.");
        }
        double total = 0;
        int maxExperts = 0;
        for (List<ExpertReviewItemRating> itemRatings : byItem.values()) {
            maxExperts = Math.max(maxExperts, itemRatings.size());
            long relevant = itemRatings.stream().filter(rating -> rating.getRating() >= relevanceThreshold).count();
            total += relevant / (double) itemRatings.size();
        }
        return new CviResult(total / byItem.size(), maxExperts, relevanceThreshold, "S-CVI/Ave from item I-CVI values; relevant means rating >= threshold.");
    }
}
