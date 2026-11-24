package com.atlas.explorer.features.auth;

import com.atlas.explorer.features.auth.config.AuthProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.beans.factory.annotation.Autowired;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties={"jobs.enabled=false","storage.enabled=false","catalog.enabled=false"})
class AuthenticationTests {
    @LocalServerPort int port;
    @Autowired AuthProperties properties;
    private final ObjectMapper json=new ObjectMapper();
    private HttpRequest.Builder request(String path){return HttpRequest.newBuilder(URI.create("http://localhost:"+port+path));}
    private HttpResponse<String> send(HttpClient client,String path)throws Exception{return client.send(request(path).GET().build(),HttpResponse.BodyHandlers.ofString());}
    private HttpClient browser(CookieManager cookies){return HttpClient.newBuilder().cookieHandler(cookies).build();}
    private String csrf(HttpClient client)throws Exception{return json.readTree(send(client,"/auth/session").body()).get("csrf_token").asText();}
    private HttpResponse<String> login(HttpClient client,String token,String password)throws Exception {
        return client.send(request("/auth/login").header("X-CSRF-TOKEN",token).header("Content-Type","application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString("username="+URLEncoder.encode(properties.username(),StandardCharsets.UTF_8)+"&password="+URLEncoder.encode(password,StandardCharsets.UTF_8))).build(),HttpResponse.BodyHandlers.ofString());
    }
    @Test void anonymousCannotAccessPlatformOrDocumentation()throws Exception {
        var client=HttpClient.newHttpClient();
        for(var path:new String[]{"/sources","/connectors","/storage/jobs","/glossary/terms","/catalog/tables","/native/sources","/openapi.json"}) {
            var response=send(client,path);assertEquals(401,response.statusCode(),path);assertFalse(response.body().contains("password"));
        }
        assertEquals(200,send(client,"/health").statusCode());assertEquals(200,send(client,"/actuator/health/readiness").statusCode());
    }
    @Test void loginRotatesSessionAndLogoutRevokesAccess()throws Exception {
        var cookies=new CookieManager(null,CookiePolicy.ACCEPT_ALL);var client=browser(cookies);
        var session=send(client,"/auth/session");assertFalse(json.readTree(session.body()).get("authenticated").asBoolean());
        assertTrue(session.headers().firstValue("Cache-Control").orElse("").contains("no-store"));
        var cookie=session.headers().allValues("Set-Cookie").toString();assertTrue(cookie.contains("HttpOnly"));assertTrue(cookie.contains("SameSite=Strict"));assertFalse(cookie.contains("jsessionid="));
        var before=cookies.getCookieStore().getCookies().getFirst().getValue();
        assertEquals(204,login(client,json.readTree(session.body()).get("csrf_token").asText(),properties.password()).statusCode());
        assertNotEquals(before,cookies.getCookieStore().getCookies().getFirst().getValue());
        var authenticated=send(client,"/auth/session");var body=json.readTree(authenticated.body());
        assertTrue(body.get("authenticated").asBoolean());assertEquals(properties.username(),body.get("username").asText());
        // A protected endpoint independent of source credentials also runs during image builds.
        assertEquals(200,send(client,"/openapi.json").statusCode());
        var logout=client.send(request("/auth/logout").header("X-CSRF-TOKEN",body.get("csrf_token").asText()).POST(HttpRequest.BodyPublishers.noBody()).build(),HttpResponse.BodyHandlers.ofString());
        assertEquals(204,logout.statusCode());assertEquals(401,send(client,"/sources").statusCode());
    }
    @Test void missingCsrfAndWrongCredentialsNeverAuthenticate()throws Exception {
        var client=browser(new CookieManager(null,CookiePolicy.ACCEPT_ALL));
        assertEquals(403,client.send(request("/auth/login").POST(HttpRequest.BodyPublishers.ofString("username=admin&password=wrong")).build(),HttpResponse.BodyHandlers.ofString()).statusCode());
        var wrong=login(client,csrf(client),"wrong-password");assertEquals(401,wrong.statusCode());assertFalse(wrong.body().contains("wrong-password"));
        assertFalse(json.readTree(send(client,"/auth/session").body()).get("authenticated").asBoolean());
    }
    @Test void protectedWritesRequireCurrentCsrfEvenAfterLogin()throws Exception {
        var client=browser(new CookieManager(null,CookiePolicy.ACCEPT_ALL));var stale=csrf(client);assertEquals(204,login(client,stale,properties.password()).statusCode());
        for(var token:new String[]{"",stale}) {
            var response=client.send(request("/sources/sqlserver/test").header("X-CSRF-TOKEN",token).POST(HttpRequest.BodyPublishers.ofString("{}")).build(),HttpResponse.BodyHandlers.ofString());
            assertEquals(403,response.statusCode());
        }
        assertEquals(403,client.send(request("/auth/logout").POST(HttpRequest.BodyPublishers.noBody()).build(),HttpResponse.BodyHandlers.ofString()).statusCode());
        assertTrue(json.readTree(send(client,"/auth/session").body()).get("authenticated").asBoolean());
    }
    @Test void invalidConfigurationNeverIncludesSecretInDiagnostic() {
        var error=assertThrows(IllegalArgumentException.class,()->new AuthProperties("admin","short-secret"));assertFalse(error.getMessage().contains("short-secret"));
        assertFalse(new AuthProperties("admin","OnlyForAutomatedTests2026!").toString().contains("OnlyFor"));
    }
}
