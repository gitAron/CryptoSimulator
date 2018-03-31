package com.example.aron.cryptosimulator;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Created by Aron on 10.03.2018.
 */

public class Position implements Serializable{

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

        return amount.setScale(7, RoundingMode.DOWN);
    }

    public void setCode(String code) {

        this.code = code;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
