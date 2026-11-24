package com.atlas.explorer.features.auth.api;
import com.atlas.explorer.features.auth.dto.SessionInfo;
import com.atlas.explorer.features.auth.service.AuthService;
import org.springframework.http.*;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;

@RestController
public class AuthController {
    private final AuthService auth;
    public AuthController(AuthService auth){this.auth=auth;}
    @GetMapping("/auth/session") public ResponseEntity<SessionInfo> session(Principal principal,CsrfToken csrf){
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(auth.session(principal==null?null:principal.getName(),csrf.getToken()));
    }
}
