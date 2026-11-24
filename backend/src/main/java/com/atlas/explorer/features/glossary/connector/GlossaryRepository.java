package com.atlas.explorer.features.glossary.connector;

import com.atlas.explorer.features.glossary.dto.*;
import java.util.*;

public interface GlossaryRepository {
    BusinessTerm create(TermRequest request);
    BusinessTerm update(UUID id, TermRequest request);
    BusinessTerm get(UUID id);
    List<BusinessTerm> list(String query, int limit, int offset);
    TermLink link(UUID termId, LinkRequest target);
    void unlink(UUID termId, UUID linkId);
    List<TermLink> lookup(LinkRequest target, int limit, int offset);
}
