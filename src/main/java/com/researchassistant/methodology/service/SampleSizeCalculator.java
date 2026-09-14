package com.researchassistant.methodology.service;

import com.researchassistant.methodology.dto.MethodologyRequests.CalculateSampleSizeRequest;
import com.researchassistant.methodology.dto.MethodologyResponses.SampleSizeCalculationResponse;
import com.researchassistant.methodology.entity.SampleSizeMethod;
import com.researchassistant.methodology.exception.MethodologyValidationException;

import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class SampleSizeCalculator {

    public SampleSizeCalculationResponse calculate(UUID id, CalculateSampleSizeRequest request) {
        SampleSizeMethod method = request.method();
        return switch (method) {
            case COCHRAN_PROPORTION -> cochran(id, request, false);
            case COCHRAN_FINITE_POPULATION -> cochran(id, request, true);
            case YAMANE -> yamane(id, request);
            case CENSUS -> census(id, request);
            case MANUAL -> manual(id, request);
            case QUALITATIVE_JUSTIFICATION -> qualitative(id, request);
        };
    }

    private SampleSizeCalculationResponse cochran(UUID id, CalculateSampleSizeRequest request, boolean finite) {
        double z = zValue(request.confidenceLevel());
        double p = request.estimatedProportion() == null ? 0.5 : request.estimatedProportion();
        double e = requiredPositive(request.marginOfError(), "marginOfError");
        double designEffect = request.designEffect() == null ? 1.0 : request.designEffect();
        double n0 = ((z * z * p * (1.0 - p)) / (e * e)) * designEffect;
        double required = n0;
        if (finite) {
            long population = requiredPopulation(request.populationSize());
            required = n0 / (1.0 + ((n0 - 1.0) / population));
        }
        int initial = ceil(required);
        int adjusted = adjust(initial, request.expectedResponseRate());
        String assumptions = "Confidence level " + confidence(request.confidenceLevel())
                + "; Z=" + z
                + "; p=" + p
                + (request.estimatedProportion() == null ? " defaulted because no estimate was supplied" : "")
                + "; margin of error=" + e
                + "; design effect=" + designEffect
                + "; ceiling rounding used.";
        return new SampleSizeCalculationResponse(
                id,
                request.method(),
                request.populationSize(),
                confidence(request.confidenceLevel()),
                e,
                p,
                designEffect,
                request.expectedResponseRate(),
                initial,
                adjusted,
                finite ? "Cochran proportion formula with finite population correction." : "Cochran proportion formula: n0=(Z^2*p*(1-p))/e^2.",
                assumptions
        );
    }

    private SampleSizeCalculationResponse yamane(UUID id, CalculateSampleSizeRequest request) {
        long population = requiredPopulation(request.populationSize());
        double e = requiredPositive(request.marginOfError(), "marginOfError");
        int initial = ceil(population / (1.0 + population * e * e));
        int adjusted = adjust(initial, request.expectedResponseRate());
        return new SampleSizeCalculationResponse(
                id,
                request.method(),
                population,
                request.confidenceLevel(),
                e,
                null,
                null,
                request.expectedResponseRate(),
                initial,
                adjusted,
                "Yamane formula: n=N/(1+N*e^2).",
                "Ceiling rounding used; Yamane is a deterministic calculation and not a universal design recommendation."
        );
    }

    private SampleSizeCalculationResponse census(UUID id, CalculateSampleSizeRequest request) {
        long population = requiredPopulation(request.populationSize());
        return new SampleSizeCalculationResponse(id, request.method(), population, null, null, null, null, null,
                Math.toIntExact(population), Math.toIntExact(population), "Census uses the full known population.", "Population size was supplied by the user.");
    }

    private SampleSizeCalculationResponse manual(UUID id, CalculateSampleSizeRequest request) {
        if (request.manualSampleSize() == null || request.manualSampleSize() < 1) {
            throw new MethodologyValidationException("Manual sample size must be at least 1.");
        }
        return new SampleSizeCalculationResponse(id, request.method(), request.populationSize(), null, null, null, null, request.expectedResponseRate(),
                request.manualSampleSize(), adjust(request.manualSampleSize(), request.expectedResponseRate()), "Manual sample size.", "Manual value supplied by the user.");
    }

    private SampleSizeCalculationResponse qualitative(UUID id, CalculateSampleSizeRequest request) {
        int size = request.manualSampleSize() == null ? 1 : request.manualSampleSize();
        return new SampleSizeCalculationResponse(id, request.method(), null, null, null, null, null, null,
                size, size, "Qualitative justification; no quantitative formula applied.", request.qualitativeJustification() == null ? "Rationale must be documented by the researcher." : request.qualitativeJustification());
    }

    private double confidence(Double confidenceLevel) {
        return confidenceLevel == null ? 0.95 : confidenceLevel;
    }

    private double zValue(Double confidenceLevel) {
        double confidence = confidence(confidenceLevel);
        if (Math.abs(confidence - 0.90) < 0.0001) return 1.645;
        if (Math.abs(confidence - 0.95) < 0.0001) return 1.96;
        if (Math.abs(confidence - 0.99) < 0.0001) return 2.576;
        throw new MethodologyValidationException("Supported confidence levels are 0.90, 0.95, and 0.99.");
    }

    private long requiredPopulation(Long population) {
        if (population == null || population < 1) {
            throw new MethodologyValidationException("Population size must be supplied and at least 1.");
        }
        return population;
    }

    private double requiredPositive(Double value, String field) {
        if (value == null || value <= 0) {
            throw new MethodologyValidationException(field + " must be supplied and greater than 0.");
        }
        return value;
    }

    private int adjust(int requiredCompleted, Double responseRate) {
        if (responseRate == null) {
            return requiredCompleted;
        }
        if (responseRate <= 0 || responseRate > 1) {
            throw new MethodologyValidationException("expectedResponseRate must be > 0 and <= 1.");
        }
        return ceil(requiredCompleted / responseRate);
    }

    private int ceil(double value) {
        return Math.toIntExact((long) Math.ceil(value));
    }
}
