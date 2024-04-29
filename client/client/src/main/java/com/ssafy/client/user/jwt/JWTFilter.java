package com.ssafy.client.user.jwt;


import java.io.IOException;
import java.io.PrintWriter;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.ssafy.client.user.domain.CustomOAuth2User;
import com.ssafy.client.user.domain.UserDTO;
import com.ssafy.client.user.service.JWTService;
import com.ssafy.client.user.CustomSuccessHandler;

import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class JWTFilter extends OncePerRequestFilter {

    private final JWTUtil jwtUtil;
    private final JWTService jwtService;


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException, IOException {
        Cookie[] cookiesForAccess = request.getCookies();
        String accessToken = null;
        if (cookiesForAccess == null) {
            log.info("Cookies are null. Guest.");
            filterChain.doFilter(request, response);
            return;
        }
        for (Cookie cookie : cookiesForAccess) {
            if (cookie.getName().equals("access")) {
                accessToken = cookie.getValue();
            }
        }

        if (accessToken == null) {
            log.info("access token is null. Guest.");
            filterChain.doFilter(request, response);
            return;
        }

        //access 토큰 만료 여부 확인
        try {
            jwtUtil.isExpired(accessToken);

            String category = jwtUtil.getCategory(accessToken);

            //이상한 토큰일 경우
            if (!category.equals("access")) {
                PrintWriter writer = response.getWriter();
                writer.print("invalid access token");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

                log.info("invalid access token");
                return;
            }

            //정상 토큰일 경우
            String username = jwtUtil.getUsername(accessToken);
            String role = jwtUtil.getRole(accessToken);

            UserDTO userDTO = new UserDTO();
            userDTO.setUsername(username);
            userDTO.setRole(role);
            CustomOAuth2User userDto = new CustomOAuth2User(userDTO);

            Authentication authToken = new UsernamePasswordAuthenticationToken(userDto, null, userDto.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authToken);

            filterChain.doFilter(request, response);
        } catch(ExpiredJwtException e) {
            //토큰 만료의 경우

            //refresh토큰 확인
            //cookie들을 불러온 뒤 Authorization Key에 담긴 쿠키를 찾음
            String refresh = null;
            Cookie[] cookies = request.getCookies();
            if (cookies == null) {
                log.info("access is invalid. Cookies is null. Do login");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
//                response.sendRedirect("http://localhost:3000");

                return;
            }

            for (Cookie cookie : cookies) {
                if (cookie.getName().equals("refresh")) {
                    refresh = cookie.getValue();
                }
            }

            //refresh토큰  검증
            if (refresh == null) {
                //access가 이상한데 refresh도 없어?
                PrintWriter writer = response.getWriter();
                writer.print("do login");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
//                response.sendRedirect("http://localhost:3000");

                log.info("access is invalid. refresh is null. Do login");

                //조건이 해당되면 메소드 종료 (필수)
                return;
            }


            //토큰 소멸 시간 검증
            if (jwtUtil.isExpired(refresh)) {
                //refresh 만료의 경우
                PrintWriter writer = response.getWriter();
                writer.print("do login");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
//                response.sendRedirect("http://localhost:3000");

                log.info("refresh is expired. Do login");

                //조건이 해당되면 메소드 종료 (필수)
                return;
            }


            /*
            ******************************************
             ******************************************
             * ******************************************
             * ******************************************
             * ******************************************
             * ******************************************
             * ******************************************
             * ******************************************
             */
            //재발급 로직
            String username = jwtUtil.getUsername(refresh);
            String role = jwtUtil.getRole(refresh);

            //make new JWT
            String newAccess = jwtUtil.createJwt("access", username, role, 600000L);
            String newRefresh = jwtUtil.createJwt("refresh", username, role, 86400000L);

            //response
            response.addCookie(CustomSuccessHandler.createCookie("access", newAccess));
            Cookie refreshCookie = new Cookie("refresh", newRefresh);
            refreshCookie.setMaxAge(24*60*60);
            //cookie.setSecure(true);
            refreshCookie.setPath("/");
            refreshCookie.setHttpOnly(true);

            log.info("newRefresh = " + newRefresh);
            response.addCookie(refreshCookie);

            UserDTO userDTO = new UserDTO();
            userDTO.setUsername(username);
            userDTO.setRole(role);
            CustomOAuth2User userDto = new CustomOAuth2User(userDTO);

            Authentication authToken = new UsernamePasswordAuthenticationToken(userDto, null, userDto.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authToken);

            filterChain.doFilter(request, response);
        }
    }
}
