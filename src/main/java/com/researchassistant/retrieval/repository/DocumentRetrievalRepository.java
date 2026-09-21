package com.researchassistant.retrieval.repository;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

@Repository
public class DocumentRetrievalRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public DocumentRetrievalRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<RetrievalCandidateRow> lexical(
            UUID projectId,
            List<UUID> documentIds,
            String query,
            int limit
    ) {
        String documentPredicate = documentIds.isEmpty()
                ? ""
                : " and d.id in (:documentIds) ";
        String sql = """
                select c.id as chunk_id,
                       rp.workspace_id,
                       d.project_id,
                       d.id as document_id,
                       d.document_code,
                       dv.id as document_version_id,
                       dv.version_number,
                       p.page_number,
                       c.chunk_number,
                       c.text_content,
                       ts_rank_cd(c.search_vector, websearch_to_tsquery('english', :query)) as score,
                       d.title as document_title
                from document_chunks c
                join document_pages p on p.id = c.page_id
                join document_versions dv on dv.id = c.document_version_id
                join documents d on d.id = dv.document_id
                join research_projects rp on rp.id = d.project_id
                where d.project_id = :projectId
                  and d.status <> 'ARCHIVED'
                  and d.current_version_id = dv.id
                  and c.search_vector @@ websearch_to_tsquery('english', :query)
                """ + documentPredicate + """
                order by score desc, d.document_code asc, c.chunk_number asc
                limit :limit
                """;
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("projectId", projectId)
                .addValue("query", query)
                .addValue("limit", limit);
        if (!documentIds.isEmpty()) {
            parameters.addValue("documentIds", documentIds);
        }
        List<RetrievalCandidateRow> rows = jdbcTemplate.query(sql, parameters, this::mapRow);
        if (!rows.isEmpty()) {
            return rows;
        }

        String fallbackSql = """
                select c.id as chunk_id,
                       rp.workspace_id,
                       d.project_id,
                       d.id as document_id,
                       d.document_code,
                       dv.id as document_version_id,
                       dv.version_number,
                       p.page_number,
                       c.chunk_number,
                       c.text_content,
                       0.5 as score,
                       d.title as document_title
                from document_chunks c
                join document_pages p on p.id = c.page_id
                join document_versions dv on dv.id = c.document_version_id
                join documents d on d.id = dv.document_id
                join research_projects rp on rp.id = d.project_id
                where d.project_id = :projectId
                  and d.status <> 'ARCHIVED'
                  and d.current_version_id = dv.id
                  and length(trim(c.text_content)) > 0
                """ + documentPredicate + """
                order by c.chunk_number asc, d.document_code asc
                limit :limit
                """;
        return jdbcTemplate.query(fallbackSql, parameters, this::mapRow);
    }

    public List<RetrievalCandidateRow> lexicalScoped(
            UUID projectId,
            List<UUID> documentIds,
            List<UUID> versionIds,
            String query,
            int limit
    ) {
        if (documentIds.isEmpty() || versionIds.isEmpty()) {
            return List.of();
        }
        String sql = """
                select c.id as chunk_id,
                       rp.workspace_id,
                       d.project_id,
                       d.id as document_id,
                       d.document_code,
                       dv.id as document_version_id,
                       dv.version_number,
                       p.page_number,
                       c.chunk_number,
                       c.text_content,
                       ts_rank_cd(c.search_vector, websearch_to_tsquery('english', :query)) as score,
                       d.title as document_title
                from document_chunks c
                join document_pages p on p.id = c.page_id
                join document_versions dv on dv.id = c.document_version_id
                join documents d on d.id = dv.document_id
                join research_projects rp on rp.id = d.project_id
                where d.project_id = :projectId
                  and d.id in (:documentIds)
                  and d.status <> 'ARCHIVED'
                  and dv.id in (:versionIds)
                  and c.search_vector @@ websearch_to_tsquery('english', :query)
                order by score desc, d.document_code asc, c.chunk_number asc
                limit :limit
                """;
        List<RetrievalCandidateRow> rows = jdbcTemplate.query(
                sql,
                new MapSqlParameterSource()
                        .addValue("projectId", projectId)
                        .addValue("documentIds", documentIds)
                        .addValue("versionIds", versionIds)
                        .addValue("query", query)
                        .addValue("limit", limit),
                this::mapRow
        );
        if (!rows.isEmpty()) {
            return rows;
        }

        String fallbackSql = """
                select c.id as chunk_id,
                       rp.workspace_id,
                       d.project_id,
                       d.id as document_id,
                       d.document_code,
                       dv.id as document_version_id,
                       dv.version_number,
                       p.page_number,
                       c.chunk_number,
                       c.text_content,
                       0.5 as score,
                       d.title as document_title
                from document_chunks c
                join document_pages p on p.id = c.page_id
                join document_versions dv on dv.id = c.document_version_id
                join documents d on d.id = dv.document_id
                join research_projects rp on rp.id = d.project_id
                where d.project_id = :projectId
                  and d.id in (:documentIds)
                  and d.status <> 'ARCHIVED'
                  and dv.id in (:versionIds)
                  and length(trim(c.text_content)) > 0
                order by c.chunk_number asc, d.document_code asc
                limit :limit
                """;
        return jdbcTemplate.query(
                fallbackSql,
                new MapSqlParameterSource()
                        .addValue("projectId", projectId)
                        .addValue("documentIds", documentIds)
                        .addValue("versionIds", versionIds)
                        .addValue("limit", limit),
                this::mapRow
        );
    }

    public List<RetrievalCandidateRow> semantic(
            UUID projectId,
            List<UUID> documentIds,
            String provider,
            String model,
            int dimensions,
            Double[] queryVector,
            int limit
    ) {
        String documentPredicate = documentIds.isEmpty()
                ? ""
                : " and d.id in (:documentIds) ";
        String sql = """
                select c.id as chunk_id,
                       rp.workspace_id,
                       d.project_id,
                       d.id as document_id,
                       d.document_code,
                       dv.id as document_version_id,
                       dv.version_number,
                       p.page_number,
                       c.chunk_number,
                       c.text_content,
                       (1.0 / (1.0 + sqrt(distance.squared_distance))) as score,
                       d.title as document_title
                from document_chunk_embeddings e
                join document_chunks c on c.id = e.chunk_id
                join document_pages p on p.id = c.page_id
                join document_versions dv on dv.id = c.document_version_id
                join documents d on d.id = dv.document_id
                join research_projects rp on rp.id = d.project_id
                cross join lateral (
                    select coalesce(sum(power(value_pair.embedding_value - value_pair.query_value, 2)), 0) as squared_distance
                    from unnest(e.vector_values, cast(:queryVector as double precision[]))
                         as value_pair(embedding_value, query_value)
                ) distance
                where d.project_id = :projectId
                  and d.status <> 'ARCHIVED'
                  and d.current_version_id = dv.id
                  and e.provider = :provider
                  and e.model = :model
                  and e.dimensions = :dimensions
                  and e.status = 'COMPLETED'
                  and e.vector_values is not null
                  and e.chunk_checksum_sha256 = c.content_checksum_sha256
                """ + documentPredicate + """
                order by distance.squared_distance asc, d.document_code asc, c.chunk_number asc
                limit :limit
                """;
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("projectId", projectId)
                .addValue("provider", provider)
                .addValue("model", model)
                .addValue("dimensions", dimensions)
                .addValue("queryVector", queryVector)
                .addValue("limit", limit);
        if (!documentIds.isEmpty()) {
            parameters.addValue("documentIds", documentIds);
        }
        return jdbcTemplate.query(sql, parameters, this::mapRow);
    }

    public List<RetrievalCandidateRow> semanticScoped(
            UUID projectId,
            List<UUID> documentIds,
            List<UUID> versionIds,
            String provider,
            String model,
            int dimensions,
            Double[] queryVector,
            int limit
    ) {
        String sql = """
                select c.id as chunk_id,
                       rp.workspace_id,
                       d.project_id,
                       d.id as document_id,
                       d.document_code,
                       dv.id as document_version_id,
                       dv.version_number,
                       p.page_number,
                       c.chunk_number,
                       c.text_content,
                       (1.0 / (1.0 + sqrt(distance.squared_distance))) as score,
                       d.title as document_title
                from document_chunk_embeddings e
                join document_chunks c on c.id = e.chunk_id
                join document_pages p on p.id = c.page_id
                join document_versions dv on dv.id = c.document_version_id
                join documents d on d.id = dv.document_id
                join research_projects rp on rp.id = d.project_id
                cross join lateral (
                    select coalesce(sum(power(value_pair.embedding_value - value_pair.query_value, 2)), 0) as squared_distance
                    from unnest(e.vector_values, cast(:queryVector as double precision[]))
                         as value_pair(embedding_value, query_value)
                ) distance
                where d.project_id = :projectId
                  and d.id in (:documentIds)
                  and d.status <> 'ARCHIVED'
                  and dv.id in (:versionIds)
                  and e.provider = :provider
                  and e.model = :model
                  and e.dimensions = :dimensions
                  and e.status = 'COMPLETED'
                  and e.vector_values is not null
                  and e.chunk_checksum_sha256 = c.content_checksum_sha256
                order by distance.squared_distance asc, d.document_code asc, c.chunk_number asc
                limit :limit
                """;
        return jdbcTemplate.query(
                sql,
                new MapSqlParameterSource()
                        .addValue("projectId", projectId)
                        .addValue("documentIds", documentIds)
                        .addValue("versionIds", versionIds)
                        .addValue("provider", provider)
                        .addValue("model", model)
                        .addValue("dimensions", dimensions)
                        .addValue("queryVector", queryVector)
                        .addValue("limit", limit),
                this::mapRow
        );
    }

    private RetrievalCandidateRow mapRow(ResultSet rs, int rowNum)
            throws SQLException {
        return new RetrievalCandidateRow(
                rs.getObject("chunk_id", UUID.class),
                rs.getObject("workspace_id", UUID.class),
                rs.getObject("project_id", UUID.class),
                rs.getObject("document_id", UUID.class),
                rs.getString("document_code"),
                rs.getObject("document_version_id", UUID.class),
                rs.getInt("version_number"),
                rs.getInt("page_number"),
                rs.getInt("chunk_number"),
                rs.getString("text_content"),
                rs.getDouble("score"),
                rs.getString("document_title")
        );
    }
}
