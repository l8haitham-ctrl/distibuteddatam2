package com.atlas.explorer.features.glossary.service;

import com.atlas.explorer.features.glossary.connector.GlossaryRepository;
import com.atlas.explorer.features.glossary.dto.*;
import com.atlas.explorer.features.connection.service.SourceService;
import com.atlas.explorer.features.exploration.service.ExplorationService;
import java.util.*;

public class GlossaryService {
    private final GlossaryRepository repository;
    private final SourceService sources;
    private final ExplorationService exploration;
    public GlossaryService(GlossaryRepository repository,SourceService sources,ExplorationService exploration) {
        this.repository=repository;this.sources=sources;this.exploration=exploration;
    }
    public BusinessTerm create(TermRequest request) { return repository.create(request.normalized()); }
    public BusinessTerm update(UUID id,TermRequest request) { return repository.update(id,request.normalized()); }
    public BusinessTerm get(UUID id) { return repository.get(id); }
    public List<BusinessTerm> list(String query,int limit,int offset) { return repository.list(query.strip(),limit,offset); }
    public TermLink link(UUID termId,LinkRequest target) {
        repository.get(termId);
        // Check exact names from metadata, including case, rather than database collation aliases.
        // No writes or business SQL are executed on the source.
        var config=sources.get(target.sourceId());
        var tables=(List<?>)exploration.tables(config).get("tables");
        if(tables.stream().map(t->(Map<?,?>)t).noneMatch(t->target.schemaName().equals(t.get("schema_name"))&&target.tableName().equals(t.get("table_name"))))
            throw new GlossaryException(GlossaryException.Code.INVALID_TARGET);
        if(target.columnName()!=null) {
            var columns=(List<?>)exploration.describe(config,target.schemaName(),target.tableName()).get("columns");
            if(columns.stream().map(c->(Map<?,?>)c).noneMatch(c->target.columnName().equals(c.get("name"))))
                throw new GlossaryException(GlossaryException.Code.INVALID_TARGET);
        }
        return repository.link(termId,target);
    }
    public void unlink(UUID termId,UUID linkId) { repository.unlink(termId,linkId); }
    public List<TermLink> lookup(LinkRequest target,int limit,int offset) { return repository.lookup(target,limit,offset); }
}
