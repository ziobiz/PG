package com.pg.catalog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * SPA 메뉴(URL) 목록 — 사이드바·권한 매트릭스와 동기화
 */
public final class PageMenuCatalog {

    public record PageMenuItem(String pageUrl, String menuId, String menuName, String parentGroup) {}

    private static final List<PageMenuItem> ITEMS = build();

    private static List<PageMenuItem> build() {
        List<PageMenuItem> list = new ArrayList<>();
        /* 본사정책 — V2 사이드바 허브·탭 왼쪽→오른쪽 순서와 동일 */
        add(list, "/hq/defaultCommission", "M0102", "수수료", "본사정책");
        add(list, "/hq/pgAgencyCostPolicy", "M0128", "대행수수료", "본사정책");
        add(list, "/hq/chargebackPolicy", "M0117", "차지백", "본사정책");
        add(list, "/hq/riskCardPolicy", "M0129", "리스크", "본사정책");
        add(list, "/hq/paymentOrchestration", "M0118", "결제 라우팅", "본사정책");
        add(list, "/hq/urlPayDeploy", "M0122", "URL결제", "본사정책");
        add(list, "/hq/opsModeMng", "M0127", "태블릿 UX", "본사정책");
        add(list, "/hq/notifyEnv", "M0105", "노티 구성", "본사정책");
        add(list, "/hq/notifyMapping", "M0107", "필드 매핑", "본사정책");
        add(list, "/hq/notifyInbound", "M0121", "수령 로그", "본사정책");
        add(list, "/hq/settlementAdmin", "M0123", "정산주기", "본사정책");
        add(list, "/hq/receivableRecoverySettings", "M0124", "환수·미수금", "본사정책");
        add(list, "/hq/businessDaySetting", "M0109", "영업일", "본사정책");
        add(list, "/hq/orgViewColumnAllowance", "M0108", "조직항목", "본사정책");
        add(list, "/set/gridSetMng", "M0505", "항목순서", "본사정책");
        add(list, "/hq/permissionMng", "M0104", "본사 권한", "본사정책");
        add(list, "/hq/userSettings", "M0120", "사용자", "본사정책");
        add(list, "/hq/accountMng", "M0106", "업체 접근", "본사정책");
        add(list, "/hq/chatbotAiSettings", "M0126", "AI·챗봇", "본사정책");
        add(list, "/hq/ledgerSysSettings", "M0119", "전산·동기화", "본사정책");
        add(list, "/hq/domainConfig", "M0115", "도메인·SSL", "본사정책");
        add(list, "/hq/serverManage", "M0116", "서버", "본사정책");
        add(list, "/hq/platformReleaseNotes", "M0156", "업데이트 내용", "본사정책");
        /* 검수관리 */
        add(list, "/calc/integratedCheck", "M0332", "통합체크", "검수관리");
        add(list, "/calc/jpayTrList", "M0328", "통합개요", "검수관리");
        add(list, "/calc/payOverview", "M0333", "결제개요", "검수관리");
        add(list, "/calc/queryIntegrated", "M0331", "일별조회", "검수관리");
        add(list, "/ops/agencyTxnList", "M0607", "대행수수료", "검수관리");
        /* 업체관리 */
        add(list, "/system/noticeList", "M0201", "공지사항", "업체관리");
        add(list, "/comp/myCompMng", "M0202", "업체정보조회", "업체관리");
        add(list, "/comp/merchantApiPortal", "M0215", "가맹점API", "업체관리");
        add(list, "/comp/compReg", "M0208", "업체등록", "업체관리");
        add(list, "/comp/compMngTree", "M0209", "업체관리", "업체관리");
        add(list, "/commission/commisionList", "M0210", "수수료관리", "업체관리");
        add(list, "/comp/compInfoHistList", "M0214", "업체변경이력", "업체관리");
        /* 결제관리 */
        add(list, "/calc/chillPayTrList", "M0319", "통합내역", "결제관리");
        add(list, "/calc/dailyIntegrated", "M0326", "일별통합", "결제관리");
        add(list, "/calc/payList", "M0301", "결제내역", "결제관리");
        add(list, "/calc/dailyPay", "M0327", "일별결제", "결제관리");
        add(list, "/calc/paySuccessList", "M0315", "성공내역", "결제관리");
        add(list, "/calc/payFailList", "M0303", "실패내역", "결제관리");
        add(list, "/calc/payCancelList", "M0318", "취소내역", "결제관리");
        add(list, "/calc/payVoidList", "M0320", "무효처리", "결제관리");
        add(list, "/calc/payEmailVoidList", "M0325", "이메일 무효", "결제관리");
        add(list, "/calc/payRefundList", "M0316", "환불처리", "결제관리");
        add(list, "/calc/payForceRefundList", "M0317", "강제환불", "결제관리");
        add(list, "/pay/easyPay", "M0310", "URL결제내역", "결제관리");
        add(list, "/pay/chatbotPay", "M0311", "챗봇결제내역", "결제관리");
        add(list, "/pay/splitPay", "M0330", "분할결제내역", "결제관리");
        add(list, "/pay/jpaySubscription", "M0312", "구독결제내역", "결제관리");
        add(list, "/calc/offsetCancList", "M0309", "상계취소내역", "결제관리");
        /* 정산관리 */
        add(list, "/calc/chillPaySettlementList", "M0421", "통합정산", "정산관리");
        add(list, "/calc/feeList", "M0406", "수수료내역", "정산관리");
        add(list, "/calc/dailyFee", "M0425", "일별수수료", "정산관리");
        add(list, "/calc/exCalcList", "M0418", "정산실행", "정산관리");
        add(list, "/settlement/settlementResultDistribute", "M0423", "정산배포", "정산관리");
        add(list, "/settlement/settlementResultHold", "M0424", "정산대기", "정산관리");
        add(list, "/calc/paySettlementHoldList", "M0422", "정산보류내역", "정산관리");
        add(list, "/calc/compPointMngList", "M0407", "환수금내역", "정산관리");
        add(list, "/calc/unpaidMng", "M0413", "미수금내역", "정산관리");
        add(list, "/calc/collateralList", "M0420", "담보금내역", "정산관리");
        add(list, "/calc/calcList", "M0404", "유통망정산내역", "정산관리");
        add(list, "/calc/calcGmList", "M0405", "가맹점정산내역", "정산관리");
        add(list, "/calc/settlementReport", "M0419", "정산리포트", "정산관리");
        /* 챗봇관리 */
        add(list, "/chatbot/chatbotKbMng", "M0612", "기본설정", "챗봇관리");
        add(list, "/chatbot/productMng", "M0611", "상품관리", "챗봇관리");
        add(list, "/chatbot/orderMng", "M0613", "주문관리", "챗봇관리");
        /* 분할관리 */
        add(list, "/calc/splitPayList", "M0329", "계약관리", "분할관리");
        add(list, "/splitpay/progressMng", "M0711", "진행관리", "분할관리");
        add(list, "/splitpay/mailMng", "M0712", "이메일관리", "분할관리");
        add(list, "/splitpay/emailSettings", "M0713", "이메일설정", "분할관리");
        /* 통보관리 */
        add(list, "/noti/notiUrlMng", "M0801", "결제통보 URL관리", "통보관리");
        add(list, "/noti/notiSendMngList", "M0802", "결제통보 전송관리", "통보관리");
        add(list, "/noti/notiCashReceiptUrlMng", "M0805", "현금영수증통보 URL관리", "통보관리");
        add(list, "/noti/notiCashReceiptSendMngList", "M0806", "현금영수증통보 전송관리", "통보관리");
        /* 사용자관리 */
        add(list, "/user/userMng", "M0502", "사용자관리", "사용자관리");
        /* 운영관리 — 사이드바 순서 */
        add(list, "/ops/inactiveCard", "M0606", "카드관리", "운영관리");
        add(list, "/risk/list", "M0701", "리스크 현황", "운영관리");
        add(list, "/ops/notiProvision", "M0608", "노티관리", "운영관리");
        add(list, "/ops/distributionTxnList", "M0609", "유통망내역", "운영관리");
        add(list, "/ops/distributionSettlement", "M0610", "유통망정산", "운영관리");
        add(list, "/ops/mailLog", "M0602", "메일관리", "운영관리");
        add(list, "/ops/opsMng", "M0601", "운영관리", "운영관리");
        add(list, "/ops/integratedReport", "M0604", "통합리포트", "운영관리");
        add(list, "/ops/opsManuals", "M0614", "운영매뉴얼", "운영관리");
        add(list, "/ops/verifyReport", "M0605", "검증리포트", "운영관리");
        add(list, "/ops/taxReport", "M0603", "TAX리포트", "운영관리");
        /* 연동·배포 — 사이드바 허브·탭 순서 */
        add(list, "/hq/pgApiMng", "M0101", "PG사 연동", "연동·배포");
        add(list, "/hq/apiConfig", "M0103", "① 공통설정", "연동·배포");
        add(list, "/hq/apiMerchantDeployReg", "M0906", "② 가맹 등록", "연동·배포");
        add(list, "/hq/merchantApiGenerate", "M0905", "③ 키·문서", "연동·배포");
        add(list, "/hq/merchantApiDeployDocs", "M0907", "API 문서", "연동·배포");
        add(list, "/deploy/launchGuide", "M0904", "출시 가이드", "연동·배포");
        add(list, "/deploy/launchChecklist", "M0904", "배포 체크리스트", "연동·배포");
        add(list, "/deploy/integrationPlan", "M0901", "연동 진행안", "연동·배포");
        add(list, "/deploy/jpayWorkPlan", "M0902", "JPAY 전용 연동", "연동·배포");
        add(list, "/deploy/merchantApiPolicy", "M0903", "API 배포 정책", "연동·배포");
        return Collections.unmodifiableList(list);
    }

    private static void add(List<PageMenuItem> list, String url, String menuId, String name, String parent) {
        list.add(new PageMenuItem(url, menuId, name, parent));
    }

    public static List<PageMenuItem> items() {
        return ITEMS;
    }
}
