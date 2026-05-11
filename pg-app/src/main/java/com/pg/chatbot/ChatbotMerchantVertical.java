package com.pg.chatbot;

import java.util.Locale;
import java.util.Optional;

/**
 * 챗봇관리 기본설정 — 가맹점 업체성격(운영방식과 별개로 주문·예약 질문 흐름을 잡는 분류).
 * 공개 챗봇 LLM·주문 안내에 반영됩니다.
 */
public enum ChatbotMerchantVertical {

    GENERAL_SALE(
            "GENERAL_SALE",
            "일반판매",
            """
                    일반 소매·판매 업종으로 가정합니다.
                    주문·결제 전 고객에게 필요한 정보: 성함, 연락처(전화·이메일), 배송지 또는 수령 방식(가능 시), 수량·옵션(카탈로그에 있는 범위).
                    과도한 개인정보는 요구하지 말고, 결제·배송·세금계산서 등에 필요한 최소 항목만 질문하세요."""),

    ECOMMERCE(
            "ECOMMERCE",
            "이커머스",
            """
                    온라인 몰·배송 중심으로 가정합니다.
                    주문 전: 수령인, 연락처, 배송지(또는 픽업), 요청사항(문 앞 등), 옵션·수량(카탈로그 범위).
                    반품·교환 정책은 가맹 기본 안내·사이트 사실만 언급하고 임의 약속은 금지."""),

    CONSULTING(
            "CONSULTING",
            "컨설팅",
            """
                    상담·자문·프로젝트형 서비스로 가정합니다.
                    사전에: 상담 목적, 희망 일정(또는 시간대), 연락 가능 채널, 회사/직함(선택), 자료 전달 여부.
                    예약 상품이면 예약 일시·타임존을 명확히 확인하세요."""),

    REAL_ESTATE(
            "REAL_ESTATE",
            "부동산",
            """
                    매물·임대·중개 문의로 가정합니다.
                    관심 매물(카탈로그 항목명), 방문·상담 희망 일시, 연락처, 예산·조건은 고객이 말한 범위만 기록하고 추측하지 마세요.
                    법정 고지·계약은 전문가 안내로 연결한다는 톤을 유지하세요."""),

    AUTO_SALES(
            "AUTO_SALES",
            "자동차판매",
            """
                    차량 관련 판매·상담으로 가정합니다.
                    관심 차량(항목명), 연락처, 방문·시승 희망 일시(가능 시), 거래 형태(카탈로그·운영방식 범위).
                    등록·번호판·할부 등 법적 확정 사항은 추측하지 말고 확인 절차가 필요함을 안내하세요."""),

    SERVICE_TRADE(
            "SERVICE_TRADE",
            "서비스업",
            """
                    일반 대면·비대면 서비스 업종으로 가정합니다.
                    예약형(listingType이 RESERVATION_*)이면 주문서 전 대화에서 반드시: (1) **예약·방문 대표 성명** (2) **휴대전화** (3) **이메일** (4) **일시**·타임존 (5) **인원 또는 이용 규모**
                    (6) 서비스 항목은 카탈로그 범위, 장소·특이 요청은 사실만.
                    운영방식(선불/후불)과 모순되지 않게 안내하고, 필수 정보가 없으면 주문서를 열기 전에 채우세요."""),

    MASSAGE_GENERAL(
            "MASSAGE_GENERAL",
            "일반마사지",
            """
                    방문 기반 마사지·테라피 예약으로 가정합니다.
                    예약 상품이면 주문서 전 대화에서 반드시: (1) **예약자 성명** (2) **휴대전화** (3) **이메일** (4) **방문 일시**·타임존 (5) **방문 인원**
                    (6) 이용 예정 시간(분)·요청사항(건강상 주의는 일반적 표현만).
                    슬롯은 카탈로그·가맹 예약 규칙을 따르고, 필수 정보가 없으면 주문서를 열기 전에 채우세요."""),

    COSMETIC(
            "COSMETIC",
            "코스메틱",
            """
                    시술·뷰티 예약으로 가정합니다.
                    예약 상품이면 주문서 전 대화에서 반드시: (1) **예약자 성명** (2) **휴대전화** (3) **이메일** (4) **방문·시술 희망 일시**·타임존 (5) **인원**(동반 포함)
                    (6) 시술 항목은 카탈로그 범위, 알레르기·금기는 고객이 말한 범위만.
                    의료 행위 여부는 가맹 사실에 맞게만 안내하고, 필수 정보가 없으면 주문서 진행 전에 채우세요."""),

    CLUB_ENTERTAINMENT(
            "CLUB_ENTERTAINMENT",
            "클럽(유흥)",
            """
                    유흥업소 형태의 예약·방문 안내 업종으로 가정합니다(법령·업소 운영 정책 범위 내).
                    예약·방문 결제(주문서)로 넘기기 전 대화에서 반드시 확인: (1) 예약자 또는 방문 대표자 **성명** (2) **휴대전화** (3) **이메일**
                    (4) **방문·예약 일시**(타임존 명시, 아래 JSON의 예약 기준 타임존과 일치) (5) **방문·예약 인원** (6) 룸·테이블·이용 시간(분) 등은 카탈로그·가맹 안내 사실 범위에서만.
                    위 항목이 하나라도 없으면 주문서를 열도록 안내하지 말고 질문으로 채운 뒤, 주문서 입력과 대화 요약이 같은지 고객에게 재확인하세요.
                    동행·안내 인력 예약은 고객이 요청한 범위에서만 질문하고, 불법·미성년자 관련 요청은 거절하세요. 주문서·요청사항에는 사실로 받은 정보만 기록하세요."""),

    CLUB_MASSAGE(
            "CLUB_MASSAGE",
            "클럽(마사지)",
            """
                    클럽 내 마사지·라운지 예약으로 가정합니다(법령·업소 정책 범위 내).
                    주문서·결제 전 대화에서 반드시 확인: (1) 예약자 또는 방문 대표자 **성명** (2) **휴대전화** (3) **이메일** (4) **방문·예약 일시**(타임존)
                    (5) **방문·예약 인원** (6) 이용 예정 시간(분)·룸·코스는 카탈로그·가맹 안내 범위에서만.
                    빠진 항목이 있으면 주문서 진행을 안내하지 말고 채운 뒤 요약 일치를 확인하세요. 불법·부적절 요구는 거절하세요."""),

    RESTAURANT(
            "RESTAURANT",
            "음식점",
            """
                    식당·예약석 중심으로 가정합니다.
                    예약 상품이면 주문서 전 대화에서 반드시: (1) **예약자 성명** (2) **휴대전화** (3) **이메일** (4) **방문 일시**·타임존 (5) **인원**
                    (6) 룸/홀·아동 동반·알레르기는 고객이 말한 경우만 기록.
                    선불·예약금은 운영방식·항목 금액과 일치시키고, 필수 정보가 없으면 주문서를 열기 전에 질문으로 채우세요."""),

    VIP_CLUB(
            "VIP_CLUB",
            "VIP 클럽",
            """
                    룸·프라이빗 예약 중심으로 가정합니다.
                    주문서·결제 전 대화에서 반드시: (1) **예약자·방문 대표 성명** (2) **휴대전화** (3) **이메일** (4) **방문 일시**·타임존 (5) **인원**
                    (6) 룸·좌석 유형·이용 시간(분) (7) 요청사항은 사실만.
                    가격·최소 사용 시간은 카탈로그·안내 사실만 사용하고, 필수 항목이 빠지면 주문서 진행 전에 모두 채우도록 하세요."""),

    OTHER(
            "OTHER",
            "기타",
            """
                    위 분류에 딱 맞지 않는 업종입니다.
                    카탈로그 listingType(SALE/RESERVATION_TIME/RESERVATION_PLACE)과 운영방식을 우선으로,
                    주문서에 필요한 최소 항목(성함·연락처·주소·예약 일시 등)만 합리적으로 질문하세요.
                    본사가 「업체성격 보조 메모」를 적어 두었으면 그 내용을 반드시 반영하세요.""");

    private final String code;
    private final String labelKo;
    private final String orderCollectHintKo;

    ChatbotMerchantVertical(String code, String labelKo, String orderCollectHintKo) {
        this.code = code;
        this.labelKo = labelKo;
        this.orderCollectHintKo = orderCollectHintKo.stripIndent().trim();
    }

    public String getCode() {
        return code;
    }

    public String getLabelKo() {
        return labelKo;
    }

    /** 주문·예약 대화에서 수집해야 할 정보에 대한 한국어 지침(LLM). */
    public String getOrderCollectHintKo() {
        return orderCollectHintKo;
    }

    /** DB 미설정·알 수 없는 값 → 일반판매. */
    public static ChatbotMerchantVertical resolveStored(String dbCode) {
        return fromCode(dbCode).orElse(GENERAL_SALE);
    }

    public static Optional<ChatbotMerchantVertical> fromCode(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String u = raw.trim().toUpperCase(Locale.ROOT);
        for (ChatbotMerchantVertical v : values()) {
            if (v.code.equals(u)) {
                return Optional.of(v);
            }
        }
        return Optional.empty();
    }

    public static ChatbotMerchantVertical fromCodeStrict(String raw) {
        return fromCode(raw).orElseThrow(() ->
                new IllegalArgumentException(
                        "지원하지 않는 가맹점 업체성격 코드입니다. GENERAL_SALE, ECOMMERCE, CONSULTING, REAL_ESTATE, "
                                + "AUTO_SALES, SERVICE_TRADE, MASSAGE_GENERAL, COSMETIC, CLUB_ENTERTAINMENT, "
                                + "CLUB_MASSAGE, RESTAURANT, VIP_CLUB, OTHER 중 하나를 사용하세요."));
    }

    /**
     * 공개 JSON(chatbotReservationOrderPrecheckKo)·LLM용.
     * 고객 주문서(이름·이메일·전화·주소·예약일시·인원 등)와 동일한 항목을 대화에서 먼저 채우라는 고정 안내(한국어).
     */
    public static String sharedReservationOrderPrecheckKo() {
        return """
                예약·방문형 상품(RESERVATION_TIME 또는 RESERVATION_PLACE)은 결제·「주문·결제 진행」 주문서로 넘기기 전,
                대화에서 아래가 모두 확인되어야 합니다. 하나라도 없으면 주문서를 열도록 안내하지 말고 질문으로 채운 뒤,
                주문서에 입력될 값과 대화 요약이 일치하는지 고객에게 재확인하세요.
                (1) 예약자 또는 방문 대표자 실명 (2) 휴대전화 (3) 이메일 (4) 연락·방문 주소
                (5) 예약 시작 일시(가맹 기본설정·카탈로그의 타임존 기준, 현지 형식으로 명확히)
                (6) 방문·예약 인원(1~999) (7) 숙박형(RESERVATION_PLACE)이면 가능 시 체크아웃 날짜
                (8) 이용 시간(분)·룸·요청사항 등은 상품·업체성격에 따라 주문서 필드 또는 요청사항에 적히도록 안내.
                """.stripIndent().trim();
    }
}
