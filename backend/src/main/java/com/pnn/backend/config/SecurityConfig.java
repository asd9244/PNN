package com.pnn.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration // 이 클래스가 Spring 설정 클래스임을 선언, 내부 @Bean 메서드를 스캔하여 빈 등록
public class SecurityConfig {

    @Bean // 반환 객체를 Spring 컨테이너에 빈으로 등록
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception { // HttpSecurity: 보안 규칙을 체이닝으로 정의하는 빌더
        http
                .csrf(csrf -> csrf.disable()) // CSRF 비활성화 — REST API는 토큰 기반이라 불필요
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/health").permitAll() // 헬스 체크는 인증 없이 공개
                        .anyRequest().permitAll() // 개발 초기: 전체 공개. JWT 구현 후 .authenticated()로 변경 예정
                )
                .httpBasic(Customizer.withDefaults()); // HTTP Basic 인증 기본 설정 활성화. 향후 JWT로 교체 예정

        return http.build(); // 위 설정을 조합하여 SecurityFilterChain 객체 생성
    }
}
