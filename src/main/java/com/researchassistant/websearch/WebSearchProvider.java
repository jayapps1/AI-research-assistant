package com.researchassistant.websearch;

public interface WebSearchProvider {

    String providerName();

    boolean available();

    WebSearchResponse search(WebSearchRequest request);
}
