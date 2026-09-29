package com.researchassistant.websearch;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.web-search.provider", havingValue = "none", matchIfMissing = true)
public class DisabledWebSearchProvider implements WebSearchProvider {

    @Override
    public String providerName() {
        return WebSearchProviderType.NONE.name();
    }

    @Override
    public boolean available() {
        return false;
    }

    @Override
    public WebSearchResponse search(WebSearchRequest request) {
        return WebSearchResponse.notConfigured(providerName());
    }
}
