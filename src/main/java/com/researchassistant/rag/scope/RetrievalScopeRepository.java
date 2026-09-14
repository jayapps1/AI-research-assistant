package com.researchassistant.rag.scope;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public class RetrievalScopeRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public RetrievalScopeRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<RetrievalScopeDocument> findEligibleCurrentDocuments(UUID projectId) {
        String sql = eligibleSql("");
        return jdbcTemplate.query(
                sql,
                new MapSqlParameterSource("projectId", projectId),
                (rs, rowNum) -> new RetrievalScopeDocument(
                        rs.getObject("document_id", UUID.class),
                        rs.getObject("document_version_id", UUID.class)
                )
        );
    }

    public List<RetrievalScopeDocument> findEligibleSelectedDocuments(
            UUID projectId,
            Collection<UUID> documentIds
    ) {
        String sql = eligibleSql(" and d.id in (:documentIds) ");
        return jdbcTemplate.query(
                sql,
                new MapSqlParameterSource()
                        .addValue("projectId", projectId)
                        .addValue("documentIds", documentIds),
                (rs, rowNum) -> new RetrievalScopeDocument(
                        rs.getObject("document_id", UUID.class),
                        rs.getObject("document_version_id", UUID.class)
                )
        );
    }

    private String eligibleSql(String documentPredicate) {
        return """
                select d.id as document_id,
                       dv.id as document_version_id
                from documents d
                join document_versions dv on dv.id = d.current_version_id
                where d.project_id = :projectId
                  and d.status = 'READY'
                  and dv.status = 'READY'
                  and exists (
                      select 1
                      from document_chunks c
                      where c.document_version_id = dv.id
                        and length(trim(c.text_content)) > 0
                  )
                """ + documentPredicate + """
                order by d.document_code asc
                """;
    }
}
