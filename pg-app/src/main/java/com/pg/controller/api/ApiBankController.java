package com.pg.controller.api;

import com.pg.api.ApiResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 국가별 은행 목록 API (계좌정보 전산 연동)
 */
@RestController
@RequestMapping(value = "/api/bank", produces = MediaType.APPLICATION_JSON_VALUE)
public class ApiBankController {

    /** 국가별 은행 목록 (코드, 이름) */
    private static final Map<String, List<Map<String, String>>> BANKS_BY_COUNTRY = new LinkedHashMap<>();

    static {
        // 대한민국 (금융기관코드 - 한국은행 기준)
        BANKS_BY_COUNTRY.put("KR", List.of(
                Map.of("code", "02", "name", "산업은행"),
                Map.of("code", "03", "name", "기업은행"),
                Map.of("code", "04", "name", "국민"),
                Map.of("code", "07", "name", "수협"),
                Map.of("code", "11", "name", "NH농협"),
                Map.of("code", "12", "name", "지역농·축협"),
                Map.of("code", "20", "name", "우리"),
                Map.of("code", "27", "name", "한국씨티"),
                Map.of("code", "31", "name", "대구은행"),
                Map.of("code", "32", "name", "부산은행"),
                Map.of("code", "34", "name", "광주은행"),
                Map.of("code", "35", "name", "제주은행"),
                Map.of("code", "37", "name", "전북은행"),
                Map.of("code", "39", "name", "경남은행"),
                Map.of("code", "45", "name", "새마을금고"),
                Map.of("code", "48", "name", "신협"),
                Map.of("code", "50", "name", "상호저축은행"),
                Map.of("code", "64", "name", "산림조합"),
                Map.of("code", "71", "name", "우체국"),
                Map.of("code", "81", "name", "KEB하나"),
                Map.of("code", "88", "name", "신한"),
                Map.of("code", "89", "name", "케이뱅크"),
                Map.of("code", "90", "name", "카카오뱅크"),
                Map.of("code", "92", "name", "토스뱅크"),
                Map.of("code", "23", "name", "SC제일은행")
        ));
        // 태국 (ChillPay/결제 연동용)
        BANKS_BY_COUNTRY.put("TH", List.of(
                Map.of("code", "002", "name", "Bangkok Bank"),
                Map.of("code", "004", "name", "Kasikorn Bank"),
                Map.of("code", "006", "name", "Krung Thai Bank"),
                Map.of("code", "009", "name", "HSBC Thailand"),
                Map.of("code", "011", "name", "TMBThanachart Bank"),
                Map.of("code", "014", "name", "Siam Commercial Bank"),
                Map.of("code", "022", "name", "Standard Chartered"),
                Map.of("code", "024", "name", "UOB Thailand"),
                Map.of("code", "025", "name", "Bank of Ayudhya (Krungsri)"),
                Map.of("code", "030", "name", "Government Savings Bank"),
                Map.of("code", "034", "name", "Government Housing Bank"),
                Map.of("code", "067", "name", "ICBC Thai"),
                Map.of("code", "069", "name", "Kiatnakin Phatra Bank"),
                Map.of("code", "073", "name", "Land and Houses Bank"),
                Map.of("code", "076", "name", "Thanachart Bank")
        ));
        // 일본 (統一金融機関コード)
        BANKS_BY_COUNTRY.put("JP", List.of(
                Map.of("code", "0001", "name", "みずほ銀行"),
                Map.of("code", "0005", "name", "三菱UFJ銀行"),
                Map.of("code", "0009", "name", "三井住友銀行"),
                Map.of("code", "0010", "name", "りそな銀行"),
                Map.of("code", "0017", "name", "埼玉りそな銀行"),
                Map.of("code", "0033", "name", "ジャパンネット銀行"),
                Map.of("code", "0034", "name", "セブン銀行"),
                Map.of("code", "0036", "name", "楽天銀行"),
                Map.of("code", "0038", "name", "ソニー銀行"),
                Map.of("code", "0039", "name", "auじぶん銀行"),
                Map.of("code", "0040", "name", "イオン銀行"),
                Map.of("code", "9900", "name", "ゆうちょ銀行"),
                Map.of("code", "0116", "name", "横浜銀行"),
                Map.of("code", "0117", "name", "静岡銀行"),
                Map.of("code", "0118", "name", "北陸銀行")
        ));
        // 미국
        BANKS_BY_COUNTRY.put("US", List.of(
                Map.of("code", "CHASE", "name", "Chase"),
                Map.of("code", "BOA", "name", "Bank of America"),
                Map.of("code", "WELLS", "name", "Wells Fargo"),
                Map.of("code", "CITI", "name", "Citibank"),
                Map.of("code", "US", "name", "US Bank")
        ));
        // 중국
        BANKS_BY_COUNTRY.put("CN", List.of(
                Map.of("code", "ICBC", "name", "中国工商银行"),
                Map.of("code", "ABC", "name", "中国农业银行"),
                Map.of("code", "BOC", "name", "中国银行"),
                Map.of("code", "CCB", "name", "中国建设银行"),
                Map.of("code", "COMM", "name", "交通银行")
        ));
    }

    /** 국가 목록 (코드, 이름) */
    private static final List<Map<String, String>> COUNTRIES = List.of(
            Map.of("code", "KR", "name", "대한민국"),
            Map.of("code", "TH", "name", "태국"),
            Map.of("code", "JP", "name", "일본"),
            Map.of("code", "US", "name", "미국"),
            Map.of("code", "CN", "name", "중국"),
            Map.of("code", "VN", "name", "베트남"),
            Map.of("code", "SG", "name", "싱가포르"),
            Map.of("code", "MY", "name", "말레이시아"),
            Map.of("code", "ID", "name", "인도네시아"),
            Map.of("code", "PH", "name", "필리핀")
    );

    @GetMapping("/countries")
    public ResponseEntity<ApiResponse<List<Map<String, String>>>> countries() {
        return ResponseEntity.ok(ApiResponse.ok(COUNTRIES));
    }

    @GetMapping("/list")
    public ResponseEntity<ApiResponse<List<Map<String, String>>>> list(@RequestParam String countryCd) {
        String key = (countryCd != null ? countryCd.trim().toUpperCase() : "");
        List<Map<String, String>> banks = BANKS_BY_COUNTRY.getOrDefault(key, Collections.emptyList());
        return ResponseEntity.ok(ApiResponse.ok(banks));
    }
}
