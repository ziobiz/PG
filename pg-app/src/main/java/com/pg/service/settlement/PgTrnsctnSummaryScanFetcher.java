package com.pg.service.settlement;

import com.pg.entity.PgTrnsctn;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 결제관리·정산관리의 <strong>금액요약/집계</strong> 계산용으로, 검색 조건({@link Specification})에 맞는
 * {@link PgTrnsctn} 행을 <strong>COUNT 없이</strong> 정렬·LIMIT 한 번으로 읽어오는 공용 도우미.
 *
 * <p>배경: 기존 집계 로직들은 {@code Page} 기반으로 거래를 페이지 단위 반복 조회했는데,
 * Spring Data {@code findAll(spec, pageable)} 는 페이지마다 전체 기간 {@code COUNT(*)} 쿼리와
 * 점점 깊어지는 {@code OFFSET} 을 실행한다. 한 달 등 넓은 기간에서는 이 반복 COUNT·딥 OFFSET 이
 * 누적되어 응답이 수십~수백 초가 되고, 그 결과 프록시 단에서 <strong>504 Gateway Time-out</strong> 이 발생했다.
 *
 * <p>이 도우미는 동일한 {@code spec}(권한·기간·상태 필터)과 동일한 정렬(적재일 {@code createdAt},
 * 동률 시 {@code trnId})을 적용하되, COUNT 와 OFFSET 반복을 제거하고 단일 LIMIT 조회로 끝낸다.
 * 호출부는 {@code limit} 를 "상한+1" 로 주고 결과 크기로 capped 여부를 판정하면 된다.
 * 집계 산식·결과 구조는 호출부가 그대로 유지한다.
 */
@Component
public class PgTrnsctnSummaryScanFetcher {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * @param spec      검색 그리드와 동일한 필터(널이면 전체)
     * @param ascending 적재일 오름차순 여부(검색 정렬 방향과 동일하게 전달)
     * @param limit     읽을 최대 행 수(보통 집계 상한 + 1). 1 미만이면 1 로 보정한다.
     */
    public List<PgTrnsctn> fetch(Specification<PgTrnsctn> spec, boolean ascending, int limit) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<PgTrnsctn> cq = cb.createQuery(PgTrnsctn.class);
        Root<PgTrnsctn> root = cq.from(PgTrnsctn.class);
        cq.select(root);
        if (spec != null) {
            Predicate pred = spec.toPredicate(root, cq, cb);
            if (pred != null) {
                cq.where(pred);
            }
        }
        if (ascending) {
            cq.orderBy(cb.asc(root.get("createdAt")), cb.asc(root.get("trnId")));
        } else {
            cq.orderBy(cb.desc(root.get("createdAt")), cb.desc(root.get("trnId")));
        }
        return entityManager.createQuery(cq)
                .setMaxResults(Math.max(1, limit))
                .getResultList();
    }
}
