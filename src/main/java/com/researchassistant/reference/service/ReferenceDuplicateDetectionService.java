package com.researchassistant.reference.service;

import com.researchassistant.reference.dto.ReferenceDtos.*;
import com.researchassistant.reference.entity.ReferenceEntry;
import com.researchassistant.reference.repository.ReferenceEntryRepository;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Service
public class ReferenceDuplicateDetectionService {
    private final ReferenceEntryRepository referenceRepository;
    private final ReferenceNormalizationService normalizationService;

    public ReferenceDuplicateDetectionService(ReferenceEntryRepository referenceRepository, ReferenceNormalizationService normalizationService) {
        this.referenceRepository = referenceRepository;
        this.normalizationService = normalizationService;
    }

    public DuplicateCheckResponse check(DuplicateCheckRequest request) {
        return check(referenceRepository.findAll(), request);
    }

    public DuplicateCheckResponse check(Collection<ReferenceEntry> candidates, DuplicateCheckRequest request) {
        List<DuplicateCandidate> exact = new ArrayList<>();
        List<DuplicateCandidate> possible = new ArrayList<>();
        String doi = normalizationService.normalizeDoi(request.doi());
        if (doi != null) {
            candidates.stream()
                    .filter(reference -> doi.equalsIgnoreCase(reference.getNormalizedDoi()))
                    .findFirst()
                    .ifPresent(r -> exact.add(candidate(r, "EXACT_DOI")));
        }
        String title = normalizationService.normalizeTitle(request.title());
        if (title != null) {
            for (ReferenceEntry entry : candidates) {
                if (entry.getPublicationYear() != null && entry.getPublicationYear().equals(request.publicationYear())
                        && title.equals(normalizationService.normalizeTitle(entry.getTitle()))) {
                    possible.add(candidate(entry, "NORMALIZED_TITLE_YEAR"));
                }
            }
        }
        return new DuplicateCheckResponse(exact, possible);
    }

    private DuplicateCandidate candidate(ReferenceEntry entry, String reason) {
        return new DuplicateCandidate(entry.getId(), reason, entry.getTitle(), entry.getPublicationYear());
    }
}
