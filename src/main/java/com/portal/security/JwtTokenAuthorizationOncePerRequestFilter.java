package com.portal.security;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.ExpiredJwtException;

@Component
public class JwtTokenAuthorizationOncePerRequestFilter extends OncePerRequestFilter {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Value("${security.header}")
    private String tokenHeader;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        logger.debug("Authentication Request For '{}'", request.getRequestURL());

        final String requestTokenHeader = request.getHeader(this.tokenHeader);
        logger.trace("Token: " + requestTokenHeader);

        String username = null;
        String jwtToken = null;

        if (requestTokenHeader != null && requestTokenHeader.startsWith("Bearer ")) {

            jwtToken = requestTokenHeader.substring(7);
            try {
                username = jwtTokenUtil.getUsernameFromToken(jwtToken);
            } catch (IllegalArgumentException e)
            {
                logger.error("User not found", e);
            }
            catch (ExpiredJwtException e)
            {
                logger.warn("Token expired", e);
            }
            catch(Throwable th) {
                logger.warn(th.getStackTrace().toString());
            }
        } else {
            logger.trace("Invalid token");
        }

        logger.debug("JWT_TOKEN_USERNAME_VALUE '{}'", username);

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = getUserDetails(username, jwtToken);
            if(userDetails==null) {
                SecurityContextHolder.clearContext();
            } else
            if (jwtTokenUtil.validateToken(jwtToken, userDetails)) {

                UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                usernamePasswordAuthenticationToken
                        .setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
            }
        }
        chain.doFilter(request, response);
    }

    private UserDetails getUserDetails(String userName, String jwt) {

        try {
            String url = "http://localhost:5051/api/user/search/userid/" + userName;
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer "+ jwt);

            HttpEntity<String> entity = new HttpEntity<>(headers);
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<UtentiSecurity> response = restTemplate.exchange(url, HttpMethod.GET, entity, UtentiSecurity.class);

            UtentiSecurity user = response.getBody();

            if (response.getBody() == null) {
                String errorMsg = String.format("User %s not found", userName);
                logger.warn(errorMsg);

                throw new UsernameNotFoundException(errorMsg);
            }

            User.UserBuilder builder = null;
            builder = org.springframework.security.core.userdetails.User.withUsername(user.getUserId());
            builder.disabled((user.getAttivo().equals("Si") ? false : true));
            builder.password(user.getPassword());

            String[] profile = user.getRuoli()
                    .stream().map(a -> "ROLE_" + a).toArray(String[]::new);
            builder.authorities(profile);

            return builder.build();
        } catch(Throwable th) {
            System.out.println(th.getStackTrace());
        }

        return null;
    }
}