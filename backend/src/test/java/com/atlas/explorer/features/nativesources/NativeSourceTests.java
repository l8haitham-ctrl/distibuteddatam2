package com.atlas.explorer.features.nativesources;

import com.atlas.explorer.features.nativesources.api.*;
import com.atlas.explorer.features.nativesources.connector.*;
import com.atlas.explorer.features.nativesources.dto.NativeConnection;
import com.atlas.explorer.features.nativesources.service.*;
import com.atlas.explorer.shared.api.ApiExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class NativeSourceTests {
    @Test void configuredProfilesNeverExposeSecrets() {
        var env=new org.springframework.mock.env.MockEnvironment().withProperty("REDIS_READER_PASSWORD","never-echo-reader");
        var profiles=new NativeSourceProfiles(env);
        assertEquals("redis",profiles.list().getFirst().get("engine"));
        assertFalse(profiles.list().toString().contains("never-echo-reader"));
        assertFalse(profiles.list().getFirst().containsKey("password"));
        assertThrows(NativeSourceException.class,()->profiles.get("unknown"));
    }
    @Test void configurationDefaultsToVerifiedTlsAndRedactsCredentials() {
        var c=new NativeConnection("redis","server",6379,"0","reader","secret",null,null);
        assertTrue(c.tls());assertEquals("",c.prefix());assertFalse(c.toString().contains("secret"));
        assertEquals("a\\*\\?\\[x\\]",RedisNativeConnector.literalPrefix("a*?[x]"));
    }
    @Test void providerErrorsAreSanitizedAndDuplicatesRejected() {
        var provider=mock(NativeConnector.class);when(provider.engine()).thenReturn("redis");
        when(provider.test(any())).thenThrow(new IllegalStateException("secret-password@private-host"));
        var service=new NativeSourceService(List.of(provider));
        assertFalse(assertThrows(NativeSourceException.class,()->service.test(new NativeConnection("redis","server",6379,"0","r","secret",false,""))).getMessage().contains("secret"));
        assertThrows(IllegalArgumentException.class,()->new NativeSourceService(List.of(provider,provider)));
    }
    @Test void apiDelegatesValidatesAndDoesNotEchoPasswords() throws Exception {
        var service=mock(NativeSourceService.class);
        when(service.test(any())).thenReturn(Map.of("status","connected"));
        var mvc=MockMvcBuilders.standaloneSetup(new NativeSourceController(service))
            .setControllerAdvice(new ApiExceptionHandler(),new NativeSourceExceptionHandler()).build();
        String request="""
          {"engine":"redis","host":"server","port":6379,"namespace":"0","username":"reader","password":"never-echo","tls":false,"prefix":"analytics:"}
          """;
        mvc.perform(post("/native/connections/test").contentType(MediaType.APPLICATION_JSON).content(request))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("connected"));
        mvc.perform(post("/native/connections/test").contentType(MediaType.APPLICATION_JSON).content(request.replace("6379","0")))
            .andExpect(status().isUnprocessableEntity());
        when(service.test(any())).thenThrow(new NativeSourceException());
        var response=mvc.perform(post("/native/connections/test").contentType(MediaType.APPLICATION_JSON).content(request))
            .andExpect(status().isBadGateway()).andReturn();
        assertFalse(response.getResponse().getContentAsString().contains("never-echo"));
        verify(service,times(2)).test(any());
    }
}
