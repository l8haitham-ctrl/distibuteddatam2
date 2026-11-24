package com.atlas.explorer.features.auth.service;
import com.atlas.explorer.features.auth.dto.SessionInfo;
import org.springframework.stereotype.Service;
@Service
public class AuthService {
    public SessionInfo session(String username,String csrfToken){return new SessionInfo(username!=null,username,csrfToken);}
}
