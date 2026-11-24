package com.atlas.explorer.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;

/** Real session and CSRF flow for API integration tests; no security bypass. */
public final class AuthenticatedHttp {
    private AuthenticatedHttp(){}
    public static <T> HttpResponse<T> send(HttpRequest request,HttpResponse.BodyHandler<T> handler)throws Exception {
        var base=request.uri().getScheme()+"://"+request.uri().getAuthority();
        var client=HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).build();
        var mapper=new ObjectMapper();
        var session=client.send(HttpRequest.newBuilder(URI.create(base+"/auth/session")).GET().build(),HttpResponse.BodyHandlers.ofString());
        var token=mapper.readTree(session.body()).get("csrf_token").asText();
        String username=System.getenv().getOrDefault("APP_LOGIN_USERNAME","admin");
        String password=System.getenv().getOrDefault("APP_LOGIN_PASSWORD","OnlyForAutomatedTests2026!");
        var login=client.send(HttpRequest.newBuilder(URI.create(base+"/auth/login")).header("X-CSRF-TOKEN",token)
                .header("Content-Type","application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString("username="+URLEncoder.encode(username,StandardCharsets.UTF_8)+"&password="+URLEncoder.encode(password,StandardCharsets.UTF_8))).build(),HttpResponse.BodyHandlers.discarding());
        if(login.statusCode()!=204)throw new IllegalStateException("Test authentication failed: "+login.statusCode());
        session=client.send(HttpRequest.newBuilder(URI.create(base+"/auth/session")).GET().build(),HttpResponse.BodyHandlers.ofString());
        token=mapper.readTree(session.body()).get("csrf_token").asText();
        return client.send(HttpRequest.newBuilder(request,(name,value)->!name.equalsIgnoreCase("X-CSRF-TOKEN")).header("X-CSRF-TOKEN",token).build(),handler);
    }
}
