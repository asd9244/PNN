package com.pnn.backend.config;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * QueryDSL 설정 클래스
 * <p>
 * JPAQueryFactory를 Spring Bean으로 등록하여,
 * 어플리케이션 전역(Repository 등)에서 의존성 주입(DI)을 통해
 * 동적 쿼리를 편리하게 사용할 수 있도록 합니다.
 * </p>
 */
@Configuration
public class QuerydslConfig {

    // JPA 환경에서 엔티티를 관리하는 객체
    @PersistenceContext
    private EntityManager entityManager;

    /**
     * JPAQueryFactory Bean 등록
     * 
     * @return EntityManager를 주입받은 JPAQueryFactory 인스턴스
     */
    @Bean
    public JPAQueryFactory jpaQueryFactory() {
        return new JPAQueryFactory(entityManager);
    }
}
