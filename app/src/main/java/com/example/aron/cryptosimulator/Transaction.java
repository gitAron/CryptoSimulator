package com.example.aron.cryptosimulator;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Created by Aron on 09.03.2018.
 */

public class Transaction {

    private String formattedDate;
    private String currency;
    private BigDecimal amount;
    private double eurAmount;

    public double getEurAmount() {
        return eurAmount;
    }

    public void setEurAmount(double eurAmount) {
        this.eurAmount = eurAmount;
    }

    public Transaction(String currency, BigDecimal amount, double eurAmount, String formattedDate) {
        this.formattedDate = formattedDate;
        this.currency = currency;
        this.amount = amount;
        this.eurAmount = eurAmount;

    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getFormattedDate() {

        return formattedDate;
    }

    public String getCurrency() {
        return currency;
    }

    public BigDecimal getAmount() {
        return amount;
    }
}
