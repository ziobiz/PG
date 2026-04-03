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
        add(list, "/hq/defaultCommission", "M0102", "수수료설정", "본사설정");
        add(list, "/hq/chargebackPolicy", "M0117", "차지백설정", "본사설정");
        add(list, "/hq/businessDaySetting", "M0109", "영업일설정", "본사설정");
        add(list, "/hq/permissionMng", "M0104", "본사권한설정", "본사설정");
        add(list, "/hq/accountMng", "M0106", "업체접근설정", "본사설정");
        add(list, "/hq/orgViewColumnAllowance", "M0108", "조직항목설정", "본사설정");
        add(list, "/hq/pgApiMng", "M0101", "API연동설정", "본사설정");
        add(list, "/hq/apiConfig", "M0103", "API배포설정", "본사설정");
        add(list, "/hq/paymentOrchestration", "M0118", "결제로직설정", "본사설정");
        add(list, "/hq/notifyEnv", "M0105", "노티구성설정", "본사설정");
        add(list, "/hq/notifyMapping", "M0107", "노티매핑설정", "본사설정");
        add(list, "/hq/domainConfig", "M0115", "도메인구성설정", "본사설정");
        add(list, "/hq/serverManage", "M0116", "서버운영관리", "본사설정");
        add(list, "/system/noticeList", "M0201", "공지사항", "업체관리");
        add(list, "/comp/myCompMng", "M0202", "업체정보조회", "업체관리");
        add(list, "/comp/compReg", "M0208", "업체등록", "업체관리");
        add(list, "/comp/compMngTree", "M0209", "업체관리", "업체관리");
        add(list, "/commission/commisionList", "M0210", "수수료관리", "업체관리");
        add(list, "/comp/compInfoHistList", "M0214", "업체변경이력", "업체관리");
        add(list, "/calc/payList", "M0301", "결제내역", "결제관리");
        add(list, "/calc/paySuccessList", "M0315", "성공내역", "결제관리");
        add(list, "/calc/payFailList", "M0303", "실패내역", "결제관리");
        add(list, "/calc/payRefundList", "M0316", "환불내역", "결제관리");
        add(list, "/calc/payForceRefundList", "M0317", "강제환불", "결제관리");
        add(list, "/calc/payCancelList", "M0318", "취소내역", "결제관리");
        add(list, "/calc/offsetCancList", "M0309", "상계취소내역", "결제관리");
        add(list, "/pay/easyPay", "M0310", "URL결제내역", "결제관리");
        add(list, "/pay/chatbotPay", "M0311", "챗봇결제내역", "결제관리");
        add(list, "/calc/calcList", "M0404", "유통망정산내역", "정산관리");
        add(list, "/calc/calcGmList", "M0405", "가맹정산내역", "정산관리");
        add(list, "/calc/feeList", "M0406", "수수료내역", "정산관리");
        add(list, "/calc/compPointMngList", "M0407", "환수금관리", "정산관리");
        add(list, "/calc/balcInfo", "M0412", "잔액/미수금관리", "정산관리");
        add(list, "/calc/exCalcList", "M0418", "정산실행", "정산관리");
        add(list, "/calc/settlementReport", "M0419", "정산리포트", "정산관리");
        add(list, "/calc/collateralList", "M0420", "담보금내역", "정산관리");
        add(list, "/pay/payHoldList", "M0312", "정산보류내역", "정산관리");
        add(list, "/noti/notiUrlMng", "M0801", "결제통보 URL관리", "통보관리");
        add(list, "/noti/notiSendMngList", "M0802", "결제통보 전송관리", "통보관리");
        add(list, "/noti/notiCashReceiptUrlMng", "M0805", "현금영수증통보 URL관리", "통보관리");
        add(list, "/noti/notiCashReceiptSendMngList", "M0806", "현금영수증통보 전송관리", "통보관리");
        add(list, "/user/userMng", "M0502", "사용자관리", "사용자관리");
        add(list, "/set/gridSetMng", "M0505", "메뉴별항목순서관리", "사용자관리");
        add(list, "/risk/list", "M0701", "리스크 현황", "리스크관리");
        return Collections.unmodifiableList(list);
    }

    private static void add(List<PageMenuItem> list, String url, String menuId, String name, String parent) {
        list.add(new PageMenuItem(url, menuId, name, parent));
    }

    public static List<PageMenuItem> items() {
        return ITEMS;
    }
}
