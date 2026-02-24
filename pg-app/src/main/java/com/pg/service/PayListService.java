package com.pg.service;

import com.pg.api.dto.PageResult;
import com.pg.entity.PgTrnsctn;
import com.pg.repository.PgTrnsctnRepository;
import com.pg.api.dto.PayListItemDto;
import com.pg.entity.OrgUnit;
import com.pg.repository.OrgUnitRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PayListService {

    private final PgTrnsctnRepository trnsctnRepository;
    private final OrgUnitRepository orgUnitRepository;

    public PayListService(PgTrnsctnRepository trnsctnRepository, OrgUnitRepository orgUnitRepository) {
        this.trnsctnRepository = trnsctnRepository;
        this.orgUnitRepository = orgUnitRepository;
    }

    public PageResult<Map<String, Object>> search(String merchantId, LocalDate fromDt, LocalDate toDt, int page, int size) {
        LocalDateTime from = fromDt != null ? fromDt.atStartOfDay() : null;
        LocalDateTime to = toDt != null ? toDt.atTime(LocalTime.MAX) : null;
        Pageable p = PageRequest.of(Math.max(0, page - 1), Math.min(100, Math.max(1, size)), Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<PgTrnsctn> result = trnsctnRepository.search(merchantId, from, to, p);
        List<String> merchantIds = result.getContent().stream().map(PgTrnsctn::getMerchantId).distinct().collect(Collectors.toList());
        Map<String, String> compNames = orgUnitRepository.findAll().stream()
                .filter(o -> merchantIds.contains(o.getCode()))
                .collect(Collectors.toMap(OrgUnit::getCode, OrgUnit::getName, (a, b) -> a));
        List<Map<String, Object>> list = result.getContent().stream()
                .map(t -> PayListItemDto.from(t, compNames.get(t.getMerchantId())))
                .collect(Collectors.toList());
        PageResult<Map<String, Object>> pr = new PageResult<>();
        pr.setList(list);
        pr.setPage(result.getNumber() + 1);
        pr.setSize(result.getSize());
        pr.setTotalElements(result.getTotalElements());
        pr.setTotalPages(result.getTotalPages());
        return pr;
    }
}
