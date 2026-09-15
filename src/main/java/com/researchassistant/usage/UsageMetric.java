package com.researchassistant.usage;

import com.researchassistant.subscription.LimitUnit;

public record UsageMetric(long used, Long limit, Long remaining, LimitUnit unit) {
}
