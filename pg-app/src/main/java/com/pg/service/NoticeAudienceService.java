package com.pg.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pg.entity.AppUser;
import com.pg.entity.Notice;
import com.pg.entity.OrgLevel;
import com.pg.entity.OrgUnit;
import com.pg.notice.NoticeDeployTarget;
import com.pg.repository.OrgUnitRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 공지사항 배포 대상·노출 범위 판별.
 */
@Service
public class NoticeAudienceService {

    private final OrgUnitRepository orgUnitRepository;
    private final AuthService authService;
    private final ObjectMapper objectMapper;

    public NoticeAudienceService(OrgUnitRepository orgUnitRepository,
                                 AuthService authService,
                                 ObjectMapper objectMapper) {
        this.orgUnitRepository = orgUnitRepository;
        this.authService = authService;
        this.objectMapper = objectMapper;
    }

    public List<Map<String, String>> deployTargetOptions(OrgLevel writerLevel) {
        List<Map<String, String>> out = new ArrayList<>();
        for (NoticeDeployTarget t : NoticeDeployTarget.allowedForWriter(writerLevel)) {
            out.add(Map.of("code", t.getCode(), "label", t.getLabelKo()));
        }
        return out;
    }

    public OrgLevel notiMinLevelForWriter(OrgLevel writerLevel) {
        return NoticeDeployTarget.notiMinLevelForWriter(writerLevel);
    }

    public void validateDeploy(AppUser user, NoticeDeployTarget target, List<Long> targetOrgUnitIds) {
        OrgUnit writer = requireWriterOrg(user);
        OrgLevel wl = writer.getOrgLevel();
        if (wl == null || !NoticeDeployTarget.allowedForWriter(wl).contains(target)) {
            throw new IllegalArgumentException("선택한 배포 대상을 사용할 수 없습니다.");
        }
        if (target == NoticeDeployTarget.NOTI) {
            if (targetOrgUnitIds == null || targetOrgUnitIds.isEmpty()) {
                throw new IllegalArgumentException("특정지점(NOTI) 배포 시 대상 업체를 1곳 이상 선택하세요.");
            }
            OrgLevel min = NoticeDeployTarget.notiMinLevelForWriter(wl);
            Set<Long> subtree = collectSubtreeIds(writer.getId());
            for (Long tid : targetOrgUnitIds) {
                if (tid == null) {
                    continue;
                }
                OrgUnit t = orgUnitRepository.findById(tid)
                        .orElseThrow(() -> new IllegalArgumentException("선택한 업체를 찾을 수 없습니다."));
                if (!subtree.contains(t.getId())) {
                    throw new IllegalArgumentException("선택한 업체가 작성 조직 하위 범위에 없습니다.");
                }
                if (min != null && (t.getOrgLevel() == null || t.getOrgLevel().getCode() < min.getCode())) {
                    throw new IllegalArgumentException("특정지점 선택은 " + min.getNameKo() + " 이상 업체만 가능합니다.");
                }
            }
        }
    }

    public boolean isVisibleToUser(AppUser user, Notice notice) {
        if (user == null || notice == null) {
            return false;
        }
        Optional<OrgUnit> viewerOpt = authService.resolveOrgUnitForLoginId(user.getUsername());
        if (viewerOpt.isEmpty()) {
            return "ADMIN".equalsIgnoreCase(user.getRole());
        }
        OrgUnit viewer = viewerOpt.get();
        OrgUnit writer = resolveWriterOrg(notice);
        if (writer == null) {
            return false;
        }
        if (Objects.equals(writer.getId(), viewer.getId())) {
            return true;
        }
        Set<Long> audience = resolveAudienceOrgUnitIds(notice, writer);
        return audience.contains(viewer.getId());
    }

    public Set<Long> resolveAudienceOrgUnitIds(Notice notice, OrgUnit writerOrg) {
        if (writerOrg == null || writerOrg.getId() == null) {
            return Set.of();
        }
        NoticeDeployTarget target = NoticeDeployTarget.fromCode(notice.getDeployTarget());
        Set<Long> subtreeIds = collectSubtreeIds(writerOrg.getId());
        Map<Long, OrgUnit> subtreeById = orgUnitRepository.findAllById(subtreeIds).stream()
                .collect(Collectors.toMap(OrgUnit::getId, o -> o, (a, b) -> a));
        List<OrgUnit> subtree = subtreeIds.stream()
                .map(subtreeById::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return switch (target) {
            case ALL -> new LinkedHashSet<>(subtreeIds);
            case MARKETING -> subtree.stream()
                    .filter(o -> o.getOrgLevel() != null && NoticeDeployTarget.MARKETING_LEVELS.contains(o.getOrgLevel()))
                    .map(OrgUnit::getId)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            case MERCHANT -> subtree.stream()
                    .filter(o -> o.getOrgLevel() == OrgLevel.MERCHANT)
                    .map(OrgUnit::getId)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            case HQ_ONLY -> subtree.stream()
                    .filter(o -> o.getOrgLevel() == OrgLevel.REGIONAL)
                    .map(OrgUnit::getId)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            case MASTER_ONLY, MASTER_DIST_ONLY -> subtree.stream()
                    .filter(o -> o.getOrgLevel() == OrgLevel.MASTER_DIST)
                    .map(OrgUnit::getId)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            case HQ_AND_MASTER -> subtree.stream()
                    .filter(o -> o.getOrgLevel() == OrgLevel.REGIONAL || o.getOrgLevel() == OrgLevel.MASTER_DIST)
                    .map(OrgUnit::getId)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            case HQ_SUB -> subtree.stream()
                    .filter(o -> o.getOrgLevel() != null && o.getOrgLevel().getCode() >= OrgLevel.REGIONAL.getCode())
                    .map(OrgUnit::getId)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            case MASTER_SUB -> subtree.stream()
                    .filter(o -> o.getOrgLevel() != null && o.getOrgLevel().getCode() >= OrgLevel.MASTER_DIST.getCode())
                    .map(OrgUnit::getId)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            case NOTI -> new LinkedHashSet<>(parseTargetOrgUnitIds(notice.getTargetOrgUnitIdsJson()));
        };
    }

    public List<Long> parseTargetOrgUnitIds(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            List<Long> ids = objectMapper.readValue(json, new TypeReference<>() {});
            if (ids == null) {
                return List.of();
            }
            return ids.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
        } catch (Exception e) {
            return List.of();
        }
    }

    public String serializeTargetOrgUnitIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return null;
        }
        List<Long> clean = ids.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (clean.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(clean);
        } catch (Exception e) {
            throw new IllegalArgumentException("대상 업체 저장 형식 오류");
        }
    }

    public NoticeDeployTarget defaultTargetForWriter(OrgLevel writerLevel) {
        if (writerLevel == OrgLevel.HEADQUARTERS) {
            return null;
        }
        if (writerLevel == OrgLevel.REGIONAL || writerLevel == OrgLevel.MASTER_DIST) {
            return NoticeDeployTarget.ALL;
        }
        return null;
    }

    public OrgUnit requireWriterOrg(AppUser user) {
        return authService.resolveOrgUnitForLoginId(user.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("로그인 조직 정보를 찾을 수 없습니다."));
    }

    public OrgUnit resolveWriterOrg(Notice notice) {
        if (notice == null || notice.getOrgUnitId() == null) {
            return null;
        }
        return orgUnitRepository.findById(notice.getOrgUnitId()).orElse(null);
    }

    public String labelForTarget(String deployTargetCode) {
        return NoticeDeployTarget.fromCode(deployTargetCode).getLabelKo();
    }

    private Set<Long> collectSubtreeIds(Long rootId) {
        if (rootId == null) {
            return Set.of();
        }
        List<OrgUnit> all = orgUnitRepository.findAll();
        Map<Long, List<OrgUnit>> byParent = all.stream()
                .filter(o -> o.getParentId() != null)
                .collect(Collectors.groupingBy(OrgUnit::getParentId));
        Set<Long> out = new LinkedHashSet<>();
        out.add(rootId);
        collectDescendantIdsRec(rootId, byParent, out);
        return out;
    }

    private void collectDescendantIdsRec(Long id, Map<Long, List<OrgUnit>> byParent, Set<Long> out) {
        for (OrgUnit child : byParent.getOrDefault(id, Collections.emptyList())) {
            out.add(child.getId());
            collectDescendantIdsRec(child.getId(), byParent, out);
        }
    }
}
