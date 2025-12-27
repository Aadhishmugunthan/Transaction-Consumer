package com.example.TransactionConsumer.repository;

import com.jayway.jsonpath.JsonPath;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.UUID;

@Repository
public class TransactionRepository {

    private final JdbcTemplate jdbc;

    public TransactionRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void insertAll(String json) {
        // Extract values from JSON
        String tranId = JsonPath.read(json, "$.transactionId");
        String tranType = JsonPath.read(json, "$.transactionType");
        Integer amount = JsonPath.read(json, "$.amount");
        String currency = JsonPath.read(json, "$.currency");

        String recipientName = JsonPath.read(json, "$.recipientName");
        String recipientEmail = JsonPath.read(json, "$.recipientEmail");

        String city = JsonPath.read(json, "$.city");
        String country = JsonPath.read(json, "$.country");

        Timestamp currentTime = new Timestamp(System.currentTimeMillis());
        String addressId = UUID.randomUUID().toString();

        // Insert into SEND_TRANSACTIONS
        jdbc.update(
                "INSERT INTO SEND_TRANSACTIONS (TRAN_ID, TRAN_TYPE, TRAN_AMT, TRAN_CURR, TRAN_CRTE_DT, CUR_STAT, ORIG_STAT, RPLCTN_UPDT_TS) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                tranId, tranType, amount, currency, currentTime, "PENDING", "NEW", currentTime
        );

        // Insert into SEND_TRAN_DTL
        jdbc.update(
                "INSERT INTO SEND_TRAN_DTL (TRAN_ID, TRAN_CRTE_DT, RPLCTN_UPDT_TS) VALUES (?, ?, ?)",
                tranId, currentTime, currentTime
        );

        // Insert into SEND_RECIP_DTL
        jdbc.update(
                "INSERT INTO SEND_RECIP_DTL (TRAN_ID, RECIP_FIRST_NAM, RECIP_EMAIL, TRAN_CRTE_DT, RPLCTN_UPDT_TS) " +
                        "VALUES (?, ?, ?, ?, ?)",
                tranId, recipientName, recipientEmail, currentTime, currentTime
        );

        // Insert into SEND_TRAN_ADDR_DTL
        jdbc.update(
                "INSERT INTO SEND_TRAN_ADDR_DTL (ID, TRAN_ID, CITY, CNTRY_NAM) VALUES (?, ?, ?, ?)",
                addressId, tranId, city, country
        );
    }
}