package com.pg.dto;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.math.BigDecimal;

/**
 * DirectCredit JSON {@code Amount} 를 {@link ChillPayDirectCreditRequest#toConcatString()} 의 금액 문자열과
 * 동일하게 출력( {@link BigDecimal#stripTrailingZeros()} + {@link BigDecimal#toPlainString()} ).
 * ChillPay 측이 본문 Amount 기준으로 체크섬을 재검증할 때 2003 불일치를 줄입니다.
 */
public class ChillPayAmountSerializer extends JsonSerializer<BigDecimal> {

    @Override
    public void serialize(BigDecimal value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value == null) {
            gen.writeNull();
            return;
        }
        gen.writeRawValue(value.stripTrailingZeros().toPlainString());
    }
}
