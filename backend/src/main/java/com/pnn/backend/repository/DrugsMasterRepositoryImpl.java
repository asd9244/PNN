package com.pnn.backend.repository;

import com.pnn.backend.domain.QDrugIngredient;
import com.pnn.backend.domain.QDrugsMaster;
import com.pnn.backend.dto.DrugSearchRequestDto;
import com.pnn.backend.dto.DrugSearchResponseDto;
import com.pnn.backend.dto.PillIdentifyRequestDto;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * DrugsMasterRepositoryCustom 인터페이스의 실제 구현체.
 * 이름 규칙상 반드시 원래 Repository 이름 뒤에 'Impl'이 붙어야 Spring이 자동으로 인식합니다.
 */
@Repository
@RequiredArgsConstructor
public class DrugsMasterRepositoryImpl implements DrugsMasterRepositoryCustom {

    // QuerydslConfig에서 Bean으로 등록한 팩토리를 주입받아 쿼리를 생성합니다.
    private final JPAQueryFactory queryFactory;

    // Q-Class 인스턴스 (QueryDSL에서 테이블/컬럼에 접근하기 위해 사용)
    private final QDrugsMaster drugsMaster = QDrugsMaster.drugsMaster;
    private final QDrugIngredient drugIngredient = QDrugIngredient.drugIngredient;

    @Override
    public Page<DrugSearchResponseDto> searchDrugDetail(DrugSearchRequestDto request, Pageable pageable) {

        // 1. 동적 쿼리 조건을 담을 빈 가방(BooleanBuilder) 생성
        BooleanBuilder builder = new BooleanBuilder();

        // 2. 사용자가 입력한 조건이 있으면 가방에 AND로 추가
        if (StringUtils.hasText(request.getItemName())) {
            // 대소문자 상관없이(IgnoreCase), 이 글자가 중간에 포함되어 있는지(contains) 찾기기
            builder.and(drugsMaster.itemName.containsIgnoreCase(request.getItemName()));
        }

        if (StringUtils.hasText(request.getEntpName())) {
            // 업체명에 검색어가 포함되어 있는지
            builder.and(drugsMaster.entpName.containsIgnoreCase(request.getEntpName()));
        }

        // 3. 성분명 검색 처리 (복잡한 부분)
        // 성분명 검색이 요청된 경우에만 drug_ingredients 테이블과 조인(Join)해야 성능이 좋습니다.
        boolean needJoin = StringUtils.hasText(request.getIngredient());

        if (needJoin) {
            // 한글 성분명 OR 영문 성분명 둘 중 하나라도 일치하면 검색되도록 묶어줍니다.
            builder.and(
                    drugIngredient.ingrNameKr.containsIgnoreCase(request.getIngredient())
                            .or(drugIngredient.ingrNameEng.containsIgnoreCase(request.getIngredient())));
        }

        // 4. 메인 데이터 조회 쿼리 생성
        JPAQuery<DrugSearchResponseDto> query = queryFactory
                // Projections.constructor는 조회한 컬럼 데이터를 DTO의 생성자에 순서대로 딱 맞게 넣어줍니다.
                .select(Projections.constructor(DrugSearchResponseDto.class,
                        drugsMaster.id,
                        drugsMaster.itemSeq,
                        drugsMaster.itemName,
                        drugsMaster.entpName,
                        drugsMaster.className,
                        drugsMaster.itemImageUrl))
                .from(drugsMaster);

        // 5. 성분 검색이 필요하면 JOIN 추가
        if (needJoin) {
            // drugs_master의 itemSeq와 drug_ingredient의 itemSeq가 같은 데이터를 연결합니다.
            query.leftJoin(drugIngredient).on(drugsMaster.itemSeq.eq(drugIngredient.itemSeq));
        }

        // 6. 조건 및 페이징 적용 후 쿼리 실행
        List<DrugSearchResponseDto> content = query
                .where(builder) // 만들어둔 동적 조건 적용
                .distinct() // (특히 JOIN 시) 중복 데이터 제거
                .offset(pageable.getOffset()) // 몇 번째 데이터부터 가져올지
                .limit(pageable.getPageSize()) // 몇 개를 가져올지
                .fetch(); // 실제 DB에 쿼리 날려서 데이터 가져오기

        // 7. 전체 데이터 개수(Total Count) 조회 쿼리 (페이징 UI를 만들기 위해 필수)
        JPAQuery<Long> countQuery = queryFactory
                .select(drugsMaster.id.countDistinct())
                .from(drugsMaster);

        if (needJoin) {
            countQuery.leftJoin(drugIngredient).on(drugsMaster.itemSeq.eq(drugIngredient.itemSeq));
        }

        Long total = countQuery.where(builder).fetchOne();
        long totalCount = (total != null) ? total : 0L;

        // 8. 조회된 데이터와 페이징 정보를 묶어서 반환
        return new PageImpl<>(content, pageable, totalCount);
    }

    @Override
    public Page<DrugSearchResponseDto> searchPillIdentifier(PillIdentifyRequestDto request, Pageable pageable) {

        BooleanBuilder builder = new BooleanBuilder();

        // 1. 모양 (정확히 일치해야 함)
        if (StringUtils.hasText(request.getDrugShape())) {
            builder.and(drugsMaster.drugShape.eq(request.getDrugShape()));
        }

        // 2. 제형 (정규화된 컬럼 기준, 정확히 일치해야 함)
        if (StringUtils.hasText(request.getFormulation())) {
            builder.and(drugsMaster.normalFormName.eq(request.getFormulation()));
        }

        // 3. 앞/뒤 구분이 모호한 속성들은 내부적으로 OR 조건으로 묶어서(괄호 치듯) AND로 추가합니다.

        // 각인 (앞면 또는 뒷면에 포함)
        if (StringUtils.hasText(request.getPrintFront())) {
            builder.and(drugsMaster.printFront.containsIgnoreCase(request.getPrintFront())
                    .or(drugsMaster.printBack.containsIgnoreCase(request.getPrintFront())));
        }
        // *참고: 기획상 Front와 Back 입력창이 2개라면, Back 입력값도 똑같이 앞/뒤를 다 뒤져줍니다.
        if (StringUtils.hasText(request.getPrintBack())) {
            builder.and(drugsMaster.printFront.containsIgnoreCase(request.getPrintBack())
                    .or(drugsMaster.printBack.containsIgnoreCase(request.getPrintBack())));
        }

        // 색상 (앞면 또는 뒷면에 포함)
        if (StringUtils.hasText(request.getColor())) {
            builder.and(drugsMaster.colorFront.containsIgnoreCase(request.getColor())
                    .or(drugsMaster.colorBack.containsIgnoreCase(request.getColor())));
        }

        // 분할선 (정확히 일치)
        if (StringUtils.hasText(request.getLine())) {
            builder.and(drugsMaster.lineFront.eq(request.getLine())
                    .or(drugsMaster.lineBack.eq(request.getLine())));
        }

        // 4. 쿼리 실행
        List<DrugSearchResponseDto> content = queryFactory
                .select(Projections.constructor(DrugSearchResponseDto.class,
                        drugsMaster.id,
                        drugsMaster.itemSeq,
                        drugsMaster.itemName,
                        drugsMaster.entpName,
                        drugsMaster.className,
                        drugsMaster.itemImageUrl))
                .from(drugsMaster)
                .where(builder)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 5. 카운트 쿼리 실행
        Long total = queryFactory
                .select(drugsMaster.id.count())
                .from(drugsMaster)
                .where(builder)
                .fetchOne();
        long totalCount = (total != null) ? total : 0L;

        return new PageImpl<>(content, pageable, totalCount);
    }
}
