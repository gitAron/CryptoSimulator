package com.example.aron.cryptosimulator;

import java.math.BigDecimal;
import java.text.DecimalFormat;

/**
 * Created by Aron on 10.03.2018.
 */

public class Position {

    private String code;
    private BigDecimal amount;

    public Position(String code, BigDecimal amount) {

        this.code = code;
        this.amount = amount;
    }

    public String getCode() {
        return code;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setCode(String code) {

        this.code = code;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
