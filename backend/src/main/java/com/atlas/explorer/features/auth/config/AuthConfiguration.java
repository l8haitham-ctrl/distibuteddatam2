package com.atlas.explorer.features.auth.config;

import org.springframework.context.annotation.*;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;

@Configuration(proxyBeanMethods=false)
@EnableConfigurationProperties(AuthProperties.class)
@ConditionalOnWebApplication(type=ConditionalOnWebApplication.Type.SERVLET)
public class AuthConfiguration {
    @Bean io.swagger.v3.oas.models.OpenAPI platformOpenApi(){
        return new io.swagger.v3.oas.models.OpenAPI()
            .components(new io.swagger.v3.oas.models.Components().addSecuritySchemes("csrf",new io.swagger.v3.oas.models.security.SecurityScheme()
                .type(io.swagger.v3.oas.models.security.SecurityScheme.Type.APIKEY).in(io.swagger.v3.oas.models.security.SecurityScheme.In.HEADER)
                .name("X-CSRF-TOKEN").description("Connectez-vous dans Angular, puis copiez csrf_token depuis GET /auth/session. La session navigateur reste necessaire.")))
            .addSecurityItem(new io.swagger.v3.oas.models.security.SecurityRequirement().addList("csrf"));
    }
    @Bean PasswordEncoder platformPasswordEncoder(){return new BCryptPasswordEncoder();}
    @Bean UserDetailsService platformUsers(AuthProperties properties,PasswordEncoder encoder){
        return new InMemoryUserDetailsManager(User.withUsername(properties.username())
                .password(encoder.encode(properties.password())).roles("USER").build());
    }
    @Bean SecurityFilterChain platformSecurity(HttpSecurity http)throws Exception {
        http.authorizeHttpRequests(requests->requests
                .requestMatchers("/auth/session","/auth/login","/health","/actuator/health/readiness","/actuator/health/liveness").permitAll()
                .anyRequest().authenticated())
            .csrf(csrf->csrf.csrfTokenRepository(new HttpSessionCsrfTokenRepository()))
            .sessionManagement(session->session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED).sessionFixation(fixation->fixation.changeSessionId()))
            .requestCache(cache->cache.disable())
            .exceptionHandling(errors->errors
                .authenticationEntryPoint((req,res,e)->{res.setStatus(401);res.setContentType("application/json");res.getWriter().write("{\"detail\":\"Connexion requise.\"}");})
                .accessDeniedHandler((req,res,e)->{res.setStatus(403);res.setContentType("application/json");res.getWriter().write("{\"detail\":\"Session ou jeton de protection invalide. Actualisez la page.\"}");}))
            .formLogin(form->form.loginPage("/login").loginProcessingUrl("/auth/login")
                .successHandler((req,res,auth)->res.setStatus(204))
                .failureHandler((req,res,e)->{res.setStatus(401);res.setContentType("application/json");res.getWriter().write("{\"detail\":\"Identifiants incorrects.\"}");}))
            .logout(logout->logout.logoutUrl("/auth/logout").invalidateHttpSession(true).clearAuthentication(true)
                .deleteCookies("ATLAS_SESSION").logoutSuccessHandler((req,res,auth)->res.setStatus(204)));
        return http.build();
    }
}
