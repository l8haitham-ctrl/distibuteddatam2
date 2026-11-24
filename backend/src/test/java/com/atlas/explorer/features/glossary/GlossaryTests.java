package com.atlas.explorer.features.glossary;

import com.atlas.explorer.features.glossary.api.GlossaryController;
import com.atlas.explorer.features.glossary.connector.GlossaryRepository;
import com.atlas.explorer.features.glossary.dto.*;
import com.atlas.explorer.features.glossary.service.*;
import com.atlas.explorer.features.connection.service.SourceService;
import com.atlas.explorer.features.connection.dto.ConnectionConfig;
import com.atlas.explorer.features.exploration.service.ExplorationService;
import com.atlas.explorer.shared.api.ApiExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.http.MediaType;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class GlossaryTests {
    @Test void validatesExactTableAndColumnBeforeWritingMetadata() {
        var repository=mock(GlossaryRepository.class);var sources=mock(SourceService.class);var exploration=mock(ExplorationService.class);
        var service=new GlossaryService(repository,sources,exploration);var id=UUID.randomUUID();
        var config=new ConnectionConfig("sqlserver","sqlserver",1433,"BusinessDB","reader","secret",true);
        when(sources.get("sqlserver")).thenReturn(config);
        when(exploration.tables(config)).thenReturn(Map.of("tables",List.of(Map.of("schema_name","dbo","table_name","Invoice"))));
        when(exploration.describe(config,"dbo","Invoice")).thenReturn(Map.of("columns",List.of(Map.of("name","amount"))));
        var valid=new LinkRequest("sqlserver","dbo","Invoice","amount");service.link(id,valid);
        verify(repository).link(id,valid);
        assertThrows(GlossaryException.class,()->service.link(id,new LinkRequest("sqlserver","dbo","Invoice","missing")));
        assertThrows(GlossaryException.class,()->service.link(id,new LinkRequest("sqlserver","dbo","invoice",null)));
        verify(repository,times(1)).link(any(),any());
        service.link(id,new LinkRequest("sqlserver","dbo","Invoice",null));
        verify(exploration,times(2)).describe(config,"dbo","Invoice");
    }
    @Test void sourceFailureAndMissingTermNeverCreateDanglingLinks() {
        var repository=mock(GlossaryRepository.class);var sources=mock(SourceService.class);var exploration=mock(ExplorationService.class);
        var service=new GlossaryService(repository,sources,exploration);var id=UUID.randomUUID();
        when(repository.get(id)).thenThrow(new GlossaryException(GlossaryException.Code.NOT_FOUND));
        assertThrows(GlossaryException.class,()->service.link(id,new LinkRequest("sqlserver","dbo","Invoice",null)));
        verifyNoInteractions(sources,exploration);verify(repository,never()).link(any(),any());
    }
    @Test void normalizesTextButNeverExecutesCalculationRule() {
        var repository=mock(GlossaryRepository.class);var sources=mock(SourceService.class);var exploration=mock(ExplorationService.class);
        var service=new GlossaryService(repository,sources,exploration);
        service.create(new TermRequest(" Reste a payer "," Montant du "," SELECT secret ",null,TermRequest.Status.DRAFT));
        verify(repository).create(new TermRequest("Reste a payer","Montant du","SELECT secret","",TermRequest.Status.DRAFT));
        verifyNoInteractions(sources,exploration);
    }
    @Test void apiRejectsInvalidPayloadAndDoesNotExposeDatabaseDetails() throws Exception {
        var service=mock(GlossaryService.class);var mvc=MockMvcBuilders.standaloneSetup(new GlossaryController(service)).setControllerAdvice(new ApiExceptionHandler()).build();
        mvc.perform(post("/glossary/terms").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\" \",\"definition\":\"d\",\"status\":\"DRAFT\"}")).andExpect(status().isUnprocessableEntity());
        mvc.perform(post("/glossary/terms").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"n\",\"definition\":\"d\",\"status\":\"UNKNOWN\"}")).andExpect(status().isUnprocessableEntity());
        verifyNoInteractions(service);
        var id=UUID.randomUUID();when(service.get(id)).thenThrow(new DataAccessResourceFailureException("password=private-host"));
        mvc.perform(get("/glossary/terms/"+id)).andExpect(status().isServiceUnavailable()).andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("private-host"))));
        doThrow(new GlossaryException(GlossaryException.Code.NOT_FOUND)).when(service).get(id);
        mvc.perform(get("/glossary/terms/"+id)).andExpect(status().isNotFound());
    }
}
