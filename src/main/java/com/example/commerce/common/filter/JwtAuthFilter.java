package com.example.commerce.common.filter;

import com.example.commerce.common.jwt.JwtProvider;
import com.example.commerce.domain.auth.entity.AuthUser;
import com.example.commerce.domain.user.entity.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {
        if (request.getRequestURI().startsWith("/auth")) {
            chain.doFilter(request, response);

            return;
        }

        String bearerJwt = request.getHeader("Authorization");
        if (bearerJwt == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "JWT 토큰이 필요합니다.");

            return;
        }

        try {
            String jwt = jwtProvider.substringToken(bearerJwt);
            Claims claims = jwtProvider.extractClaims(jwt);
            UserRole role = UserRole.valueOf(claims.get("role", String.class));
            AuthUser authUser = new AuthUser(
                    Long.parseLong(claims.getSubject()),
                    claims.get("email", String.class),
                    role
            );

            List<GrantedAuthority> authorities =
                    List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
            Authentication authentication =
                    new UsernamePasswordAuthenticationToken(authUser, null, authorities);

            SecurityContextHolder.getContext().setAuthentication(authentication);

            chain.doFilter(request, response);
        } catch (SecurityException | MalformedJwtException e) {
            log.error("유효하지 않는 JWT 서명입니다.", e);

            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "유효하지 않는 JWT 서명입니다.");
        } catch (ExpiredJwtException e) {
            log.error("만료된 JWT 토큰입니다.", e);

            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "만료된 JWT 토큰입니다.");
        } catch (UnsupportedJwtException | IllegalArgumentException e) {
            log.error("지원되지 않거나 잘못된 JWT 토큰입니다.", e);

            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "지원되지 않거나 잘못된 JWT 토큰입니다.");
        }
    }
}
