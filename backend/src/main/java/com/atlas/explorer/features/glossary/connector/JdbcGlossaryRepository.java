package com.atlas.explorer.features.glossary.connector;

import com.atlas.explorer.features.glossary.dto.*;
import com.atlas.explorer.features.glossary.service.GlossaryException;
import static com.atlas.explorer.features.glossary.service.GlossaryException.Code.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DuplicateKeyException;
import java.sql.*;
import java.util.*;

public class JdbcGlossaryRepository implements GlossaryRepository {
    private final JdbcTemplate jdbc;
    public JdbcGlossaryRepository(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    public BusinessTerm create(TermRequest request) {
        var id=UUID.randomUUID();
        try { jdbc.update("INSERT INTO business_term(id,name,definition,calculation_rule,owner,status) VALUES (?,?,?,?,?,?)",
                id,request.name(),request.definition(),request.calculationRule(),request.owner(),request.status().name()); }
        catch(DuplicateKeyException e) { throw new GlossaryException(CONFLICT); }
        return get(id);
    }
    public BusinessTerm update(UUID id, TermRequest request) {
        try {
            if(jdbc.update("UPDATE business_term SET name=?,definition=?,calculation_rule=?,owner=?,status=?,updated_at=now() WHERE id=?",
                    request.name(),request.definition(),request.calculationRule(),request.owner(),request.status().name(),id)==0)
                throw new GlossaryException(NOT_FOUND);
        } catch(DuplicateKeyException e) { throw new GlossaryException(CONFLICT); }
        return get(id);
    }
    public BusinessTerm get(UUID id) {
        var rows=jdbc.query("SELECT * FROM business_term WHERE id=?",this::term,id);
        if(rows.isEmpty())throw new GlossaryException(NOT_FOUND);
        var term=rows.getFirst();
        return new BusinessTerm(term.id(),term.name(),term.definition(),term.calculationRule(),term.owner(),term.status(),
                term.createdAt(),term.updatedAt(),jdbc.query("SELECT * FROM business_term_link WHERE term_id=? ORDER BY created_at,id",this::row,id));
    }
    public List<BusinessTerm> list(String query,int limit,int offset) {
        // Literal substring search; user input never changes SQL syntax or wildcard semantics.
        return jdbc.query("SELECT * FROM business_term WHERE strpos(lower(name),lower(?))>0 OR strpos(lower(definition),lower(?))>0 ORDER BY lower(name),id LIMIT ? OFFSET ?",
                this::term,query,query,limit,offset);
    }
    public TermLink link(UUID termId,LinkRequest target) {
        var id=UUID.randomUUID();
        try { jdbc.update("INSERT INTO business_term_link(id,term_id,source_id,schema_name,table_name,column_name) VALUES (?,?,?,?,?,?)",
                id,termId,target.sourceId(),target.schemaName(),target.tableName(),target.columnName()); }
        catch(DuplicateKeyException e) { throw new GlossaryException(CONFLICT); }
        return jdbc.queryForObject("SELECT * FROM business_term_link WHERE id=?",this::row,id);
    }
    public void unlink(UUID termId,UUID linkId) {
        if(jdbc.update("DELETE FROM business_term_link WHERE id=? AND term_id=?",linkId,termId)==0)throw new GlossaryException(NOT_FOUND);
    }
    public List<TermLink> lookup(LinkRequest target,int limit,int offset) {
        String sql="SELECT * FROM business_term_link WHERE source_id=? AND schema_name=? AND table_name=?";
        if(target.columnName()==null)return jdbc.query(sql+" ORDER BY term_id,id LIMIT ? OFFSET ?",this::row,target.sourceId(),target.schemaName(),target.tableName(),limit,offset);
        return jdbc.query(sql+" AND column_name=? ORDER BY term_id,id LIMIT ? OFFSET ?",this::row,target.sourceId(),target.schemaName(),target.tableName(),target.columnName(),limit,offset);
    }
    private BusinessTerm term(ResultSet rs,int n)throws SQLException {
        return new BusinessTerm(rs.getObject("id",UUID.class),rs.getString("name"),rs.getString("definition"),rs.getString("calculation_rule"),rs.getString("owner"),
                TermRequest.Status.valueOf(rs.getString("status")),rs.getTimestamp("created_at").toInstant(),rs.getTimestamp("updated_at").toInstant(),null);
    }
    private TermLink row(ResultSet rs,int n)throws SQLException {
        return new TermLink(rs.getObject("id",UUID.class),rs.getObject("term_id",UUID.class),rs.getString("source_id"),rs.getString("schema_name"),rs.getString("table_name"),
                rs.getString("column_name"),rs.getTimestamp("created_at").toInstant());
    }
}
