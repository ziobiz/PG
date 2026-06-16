package com.pg.service;

import com.pg.entity.AppUser;
import com.pg.entity.OrgUnit;
import com.pg.repository.AuthTokenRepository;
import com.pg.repository.MerchantProfileRepository;
import com.pg.repository.OrgUnitRepository;
import com.pg.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 업체 영구정지(S) 시 연동 AppUser 일괄 정지·세션 무효화.
 */
@Service
public class OrgUserSuspensionService {

    /** 로그인 차단(업체·계정 영구정지) 시 사용자에게 표시할 메시지 */
    public static final String MSG_ORG_LOGIN_SUSPENDED =
            "해당가맹점은 정지중입니다. 운영관리자에게 문의하세요.";

    private final OrgUnitRepository orgUnitRepository;
    private final MerchantProfileRepository merchantProfileRepository;
    private final UserRepository userRepository;
    private final AuthTokenRepository authTokenRepository;
    private final OrgServiceUseService orgServiceUseService;

    public OrgUserSuspensionService(OrgUnitRepository orgUnitRepository,
                                    MerchantProfileRepository merchantProfileRepository,
                                    UserRepository userRepository,
                                    AuthTokenRepository authTokenRepository,
                                    OrgServiceUseService orgServiceUseService) {
        this.orgUnitRepository = orgUnitRepository;
        this.merchantProfileRepository = merchantProfileRepository;
        this.userRepository = userRepository;
        this.authTokenRepository = authTokenRepository;
        this.orgServiceUseService = orgServiceUseService;
    }

    @Transactional
    public void suspendAllLinkedUsers(Long orgUnitId) {
        if (orgUnitId == null) {
            return;
        }
        orgUnitRepository.findById(orgUnitId).ifPresent(ou -> suspendUsersForOrgUnit(ou));
    }

    private void suspendUsersForOrgUnit(OrgUnit ou) {
        Set<Long> touched = new HashSet<>();
        String code = ou.getCode() != null ? ou.getCode().trim() : "";
        if (!code.isEmpty()) {
            List<AppUser> byCode = userRepository.findByOrgUnitCode(code);
            for (AppUser u : byCode) {
                if (u.getId() != null && touched.add(u.getId())) {
                    applySuspended(u);
                }
            }
        }
        merchantProfileRepository.findByOrgUnitId(ou.getId()).ifPresent(mp -> {
            String lid = mp.getLoginId() != null ? mp.getLoginId().trim() : "";
            if (!lid.isEmpty()) {
                userRepository.findByUsername(lid).ifPresent(u -> {
                    if (u.getId() != null && touched.add(u.getId())) {
                        applySuspended(u);
                    }
                });
            }
        });
        for (Long uid : touched) {
            authTokenRepository.deleteByUserId(uid);
        }
    }

    private void applySuspended(AppUser u) {
        u.setUserStatus("SUSPENDED");
        u.setEnabled(false);
        userRepository.save(u);
    }

    /** 업체 영구정지(S) 해제 시 연동 사용자 중 SUSPENDED 만 ACTIVE 복원(수동 INACTIVE 는 유지). */
    @Transactional
    public void restoreAllLinkedUsers(Long orgUnitId) {
        if (orgUnitId == null) {
            return;
        }
        orgUnitRepository.findById(orgUnitId).ifPresent(this::restoreUsersForOrgUnit);
    }

    private void restoreUsersForOrgUnit(OrgUnit ou) {
        Set<Long> touched = new HashSet<>();
        String code = ou.getCode() != null ? ou.getCode().trim() : "";
        if (!code.isEmpty()) {
            for (AppUser u : userRepository.findByOrgUnitCode(code)) {
                if (u.getId() != null && touched.add(u.getId())) {
                    applyRestoredIfSuspended(u);
                }
            }
        }
        merchantProfileRepository.findByOrgUnitId(ou.getId()).ifPresent(mp -> {
            String lid = mp.getLoginId() != null ? mp.getLoginId().trim() : "";
            if (!lid.isEmpty()) {
                userRepository.findByUsername(lid).ifPresent(u -> {
                    if (u.getId() != null && touched.add(u.getId())) {
                        applyRestoredIfSuspended(u);
                    }
                });
            }
        });
    }

    private void applyRestoredIfSuspended(AppUser u) {
        if (!isUserSuspended(u)) {
            return;
        }
        u.setUserStatus("ACTIVE");
        u.setEnabled(true);
        userRepository.save(u);
    }

    /** 비밀번호 초기화 등 — 업체가 로그인 가능 상태일 때 SUSPENDED 대표 계정만 복원 */
    public void reactivateRepresentativeForLogin(OrgUnit ou, AppUser user) {
        if (ou == null || user == null || orgServiceUseService.isOrgLoginSuspended(ou.getId())) {
            return;
        }
        if (isUserSuspended(user)) {
            user.setUserStatus("ACTIVE");
            user.setEnabled(true);
        }
    }

    static boolean isUserSuspended(AppUser user) {
        if (user == null) {
            return false;
        }
        String ust = user.getUserStatus();
        return ust != null && "SUSPENDED".equalsIgnoreCase(ust.trim());
    }

    static boolean isUserLoginAllowed(AppUser user) {
        if (user == null || !user.isEnabled()) {
            return false;
        }
        String ust = user.getUserStatus();
        return ust == null || ust.isBlank() || "ACTIVE".equalsIgnoreCase(ust.trim());
    }
}
