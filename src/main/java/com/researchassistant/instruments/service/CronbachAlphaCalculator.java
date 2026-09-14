package com.researchassistant.instruments.service;

import com.researchassistant.instruments.dto.InstrumentResponses.CronbachAlphaResult;
import com.researchassistant.methodology.exception.MethodologyValidationException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CronbachAlphaCalculator {
    public CronbachAlphaResult calculate(List<List<Double>> observations) {
        if (observations == null || observations.size() < 2) {
            throw new MethodologyValidationException("At least two complete observations are required.");
        }
        int itemCount = observations.getFirst().size();
        if (itemCount < 2) {
            throw new MethodologyValidationException("Cronbach alpha requires at least two items.");
        }
        double[] totals = new double[observations.size()];
        double[] itemVariances = new double[itemCount];
        for (int row = 0; row < observations.size(); row++) {
            List<Double> observation = observations.get(row);
            if (observation.size() != itemCount || observation.stream().anyMatch(v -> v == null || !Double.isFinite(v))) {
                throw new MethodologyValidationException("Cronbach alpha requires a complete numeric matrix; missing values are not imputed.");
            }
            for (int col = 0; col < itemCount; col++) totals[row] += observation.get(col);
        }
        for (int col = 0; col < itemCount; col++) {
            double[] values = new double[observations.size()];
            for (int row = 0; row < observations.size(); row++) values[row] = observations.get(row).get(col);
            itemVariances[col] = sampleVariance(values);
        }
        double totalVariance = sampleVariance(totals);
        if (totalVariance == 0) {
            return new CronbachAlphaResult(null, itemCount, observations.size(), "LISTWISE_COMPLETE_ONLY", "Cronbach alpha using sample variance.", List.of("Total-score variance is zero; alpha is undefined."));
        }
        double sumItemVariances = 0;
        for (double variance : itemVariances) sumItemVariances += variance;
        double alpha = (itemCount / (double) (itemCount - 1)) * (1.0 - (sumItemVariances / totalVariance));
        return new CronbachAlphaResult(alpha, itemCount, observations.size(), "LISTWISE_COMPLETE_ONLY", "alpha=(k/(k-1))*(1-sum(item variances)/variance(total score)) using sample variance.", new ArrayList<>());
    }

    private double sampleVariance(double[] values) {
        if (values.length < 2) throw new MethodologyValidationException("At least two observations are required.");
        double mean = 0;
        for (double value : values) mean += value;
        mean /= values.length;
        double sum = 0;
        for (double value : values) sum += Math.pow(value - mean, 2);
        return sum / (values.length - 1);
    }
}
