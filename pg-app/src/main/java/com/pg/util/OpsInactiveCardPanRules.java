package com.pg.util;

import java.util.Locale;

/** 비활성카드 수동 등록 — 브랜드별 접두·자릿수 */
public final class OpsInactiveCardPanRules {

    private OpsInactiveCardPanRules() {
    }

    public static void validateForMaskRegister(String maskKey) {
        if (!PayCardMaskKeyUtil.isValidMaskKey(maskKey)) {
            throw new IllegalArgumentException("카드번호는 앞 6자리 + *** + 뒤 4자리 형식(예: 531289***8601)으로 입력하세요.");
        }
    }

    public static void validateForRegister(String brandKey, String panDigits) {
        String pan = PayCardBrandDetector.normalizePan(panDigits);
        if (pan.length() < 13 || pan.length() > 19) {
            throw new IllegalArgumentException("카드번호는 13~19자리 숫자여야 합니다.");
        }
        String brand = brandKey != null ? brandKey.trim().toUpperCase(Locale.ROOT) : "";
        if ("OTHER".equals(brand)) {
            if (pan.length() >= 13 && pan.length() <= 16) {
                return;
            }
            throw new IllegalArgumentException("기타 카드번호는 13~16자리로 입력하세요.");
        }
        if (brand.isEmpty()) {
            if (pan.length() >= 13 && pan.length() <= 19) {
                return;
            }
            throw new IllegalArgumentException("카드번호는 13~19자리 숫자여야 합니다.");
        }
        if (!prefixMatches(brand, pan)) {
            throw new IllegalArgumentException("선택한 카드 종류와 카드번호 형식(접두·자릿수)이 일치하지 않습니다.");
        }
        if (!lengthMatches(brand, pan)) {
            throw new IllegalArgumentException(lengthHint(brand));
        }
    }

    private static boolean prefixMatches(String brand, String pan) {
        return switch (brand) {
            case "VISA" -> pan.startsWith("4");
            case "MASTERCARD" -> isMastercardPrefix(pan);
            case "AMEX" -> pan.startsWith("34") || pan.startsWith("37");
            case "DINERS" -> pan.startsWith("36") || pan.startsWith("38") || pan.startsWith("39");
            case "JCB" -> isJcbPrefix(pan);
            case "DISCOVER" -> isDiscoverPrefix(pan);
            case "UNIONPAY" -> pan.startsWith("62");
            case "DOMESTIC_KR" -> pan.startsWith("9");
            default -> true;
        };
    }

    private static boolean lengthMatches(String brand, String pan) {
        int len = pan.length();
        return switch (brand) {
            case "VISA" -> len == 13 || len == 16;
            case "AMEX" -> len == 15;
            case "DINERS" -> len == 14;
            case "UNIONPAY" -> len == 16;
            case "MASTERCARD", "JCB", "DISCOVER", "DOMESTIC_KR" -> len == 16;
            default -> len >= 13 && len <= 19;
        };
    }

    private static String lengthHint(String brand) {
        return switch (brand) {
            case "AMEX" -> "AMEX 카드번호 15자리를 모두 입력하세요.";
            case "DINERS" -> "Diners Club 카드번호 14자리를 모두 입력하세요.";
            case "VISA" -> "Visa 카드번호는 13자리 또는 16자리입니다.";
            case "UNIONPAY" -> "UnionPay 카드번호는 16자리입니다. (17~19자리는 기타를 선택하세요.)";
            case "DOMESTIC_KR" -> "국내 카드번호 16자리를 모두 입력하세요.";
            default -> "카드번호 16자리를 모두 입력하세요.";
        };
    }

    private static boolean isMastercardPrefix(String pan) {
        if (pan.length() < 2) {
            return false;
        }
        if (pan.charAt(0) == '5') {
            int d1 = pan.charAt(1) - '0';
            return d1 >= 1 && d1 <= 5;
        }
        if (pan.length() >= 6) {
            try {
                int p6 = Integer.parseInt(pan.substring(0, 6));
                if (p6 >= 222100 && p6 <= 272099) {
                    return true;
                }
            } catch (NumberFormatException ignored) {
                // fall through
            }
        }
        return false;
    }

    private static boolean isJcbPrefix(String pan) {
        if (pan.length() < 4) {
            return false;
        }
        try {
            int p4 = Integer.parseInt(pan.substring(0, 4));
            return p4 >= 3528 && p4 <= 3589;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static boolean isDiscoverPrefix(String pan) {
        if (pan.startsWith("6011") || pan.startsWith("65")) {
            return true;
        }
        if (pan.length() >= 3) {
            String p3 = pan.substring(0, 3);
            if (p3.compareTo("644") >= 0 && p3.compareTo("649") <= 0) {
                return true;
            }
        }
        if (pan.length() >= 6) {
            try {
                int p6 = Integer.parseInt(pan.substring(0, 6));
                return p6 >= 622126 && p6 <= 622925;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return false;
    }
}
