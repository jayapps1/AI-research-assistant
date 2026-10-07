package com.researchassistant.retrieval.repository;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DocumentRetrievalRepositoryTest {

    @Test
    void lexicalRetrievalUsesNonNullDisplayTitleExpression() {
        NamedParameterJdbcTemplate jdbcTemplate = jdbcTemplateReturningNoRows();
        DocumentRetrievalRepository repository = new DocumentRetrievalRepository(jdbcTemplate);

        repository.lexical(UUID.randomUUID(), List.of(), "alpha", 10);

        List<String> sql = capturedSql(jdbcTemplate, 2);
        assertDisplayTitleFallback(sql.get(0));
        assertDisplayTitleFallback(sql.get(1));
    }

    @Test
    void scopedLexicalRetrievalUsesNonNullDisplayTitleExpression() {
        NamedParameterJdbcTemplate jdbcTemplate = jdbcTemplateReturningNoRows();
        DocumentRetrievalRepository repository = new DocumentRetrievalRepository(jdbcTemplate);

        repository.lexicalScoped(
                UUID.randomUUID(),
                List.of(UUID.randomUUID()),
                List.of(UUID.randomUUID()),
                "alpha",
                10
        );

        List<String> sql = capturedSql(jdbcTemplate, 2);
        assertDisplayTitleFallback(sql.get(0));
        assertDisplayTitleFallback(sql.get(1));
    }

    @Test
    void semanticRetrievalUsesNonNullDisplayTitleExpression() {
        NamedParameterJdbcTemplate jdbcTemplate = jdbcTemplateReturningNoRows();
        DocumentRetrievalRepository repository = new DocumentRetrievalRepository(jdbcTemplate);

        repository.semantic(
                UUID.randomUUID(),
                List.of(),
                "test",
                "model",
                2,
                new Double[]{0.1d, 0.2d},
                10
        );

        assertDisplayTitleFallback(capturedSql(jdbcTemplate, 1).getFirst());
    }

    @Test
    void scopedSemanticRetrievalUsesNonNullDisplayTitleExpression() {
        NamedParameterJdbcTemplate jdbcTemplate = jdbcTemplateReturningNoRows();
        DocumentRetrievalRepository repository = new DocumentRetrievalRepository(jdbcTemplate);

        repository.semanticScoped(
                UUID.randomUUID(),
                List.of(UUID.randomUUID()),
                List.of(UUID.randomUUID()),
                "test",
                "model",
                2,
                new Double[]{0.1d, 0.2d},
                10
        );

        assertDisplayTitleFallback(capturedSql(jdbcTemplate, 1).getFirst());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private NamedParameterJdbcTemplate jdbcTemplateReturningNoRows() {
        NamedParameterJdbcTemplate jdbcTemplate = mock(NamedParameterJdbcTemplate.class);
        when(jdbcTemplate.query(anyString(), any(SqlParameterSource.class), any(RowMapper.class)))
                .thenReturn(List.of());
        return jdbcTemplate;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private List<String> capturedSql(NamedParameterJdbcTemplate jdbcTemplate, int times) {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate, times(times))
                .query(captor.capture(), any(SqlParameterSource.class), any(RowMapper.class));
        return captor.getAllValues();
    }

    private void assertDisplayTitleFallback(String sql) {
        assertThat(sql).contains("coalesce(");
        assertThat(sql).contains("nullif(btrim(d.title), '')");
        assertThat(sql).contains("nullif(btrim(d.bibliographic_title), '')");
        assertThat(sql).contains("d.document_code");
        assertThat(sql).contains("as document_title");
    }
}
