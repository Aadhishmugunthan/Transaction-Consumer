package com.example.TransactionConsumer.repository;

import com.example.TransactionConsumer.service.MappingService;
import com.jayway.jsonpath.JsonPath;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.Map;

@Repository
public class TransactionRepository {

    private final JdbcTemplate jdbc;
    private final MappingService mappingService;

    public TransactionRepository(JdbcTemplate jdbc, MappingService mappingService) {
        this.jdbc = jdbc;
        this.mappingService = mappingService;
    }

    @Transactional(rollbackFor = Exception.class)
    public void insertAll(String json) {

        Timestamp currentTime = new Timestamp(System.currentTimeMillis());
        String systemUser = "SYSTEM";

        if (!areMappingsLoaded()) {
            System.err.println("⚠️ Mappings not loaded → Using fallback");
            insertAllHardcoded(json);
            return;
        }

        try {
            // ================= INSERT MAIN TRANSACTION =================
            String tranId = JsonPath.read(json, "$.transactionId");
            String tranType = JsonPath.read(json, "$.transactionType");
            Integer tranAmt = JsonPath.read(json, "$.amount");
            String tranCurr = JsonPath.read(json, "$.currency");
            String custRefNum = JsonPath.read(json, "$.customerReferenceNumber");
            String origInstNam = JsonPath.read(json, "$.originatingInstitution");
            String tranfrAcptNam = JsonPath.read(json, "$.transferAcceptorName");

            jdbc.update(
                    "INSERT INTO SEND_TRANSACTIONS (" +
                            "TRAN_ID, TRAN_TYPE, TRAN_AMT, TRAN_CURR, TRAN_CRTE_DT, " +
                            "CUR_STAT, ORIG_STAT, CUST_REF_NUM, ORIG_INST_NAM, TRANFR_ACPT_NAM, " +
                            "CRTE_TS, CRTE_USER_NAM, RPLCTN_UPDT_TS, NON_FIN_TXN" +
                            ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    tranId, tranType, tranAmt, tranCurr, currentTime,
                    "COMPLETED", "NEW", custRefNum, origInstNam, tranfrAcptNam,
                    currentTime, systemUser, currentTime, 0
            );

            // ================= INSERT TRANSACTION DETAIL =================
            String paymtRef = JsonPath.read(json, "$.paymentReference");
            String fundSrc = JsonPath.read(json, "$.fundingSource");
            String paymtType = JsonPath.read(json, "$.paymentType");

            jdbc.update(
                    "INSERT INTO SEND_TRAN_DTL (" +
                            "TRAN_ID, PAYMT_REF, FUND_SRC, PAYMT_TYPE, " +
                            "TRAN_CRTE_DT, CRTE_TS, CRTE_USER_NAM, RPLCTN_UPDT_TS" +
                            ") VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    tranId, paymtRef, fundSrc, paymtType,
                    currentTime, currentTime, systemUser, currentTime
            );

            // ================= INSERT PARTY DETAILS =================
            Map<String, Object> senderParty = mappingService.extractAllValues(json, mappingService.getSenderPartyMappings());
            Map<String, Object> recipientParty = mappingService.extractAllValues(json, mappingService.getRecipientPartyMappings());

            jdbc.update(
                    "INSERT INTO SEND_RECIP_DTL (" +
                            "TRAN_ID, " +
                            "SEND_FIRST_NAM, SEND_LST_NAM, SEND_EMAIL, SEND_PHN, SEND_CITY, SEND_CNTRY_NAM, " +
                            "RECIP_FIRST_NAM, RECIP_LST_NAM, RECIP_EMAIL, RECIP_PHN, RECIP_CITY, RECIP_CNTRY_NAM, " +
                            "TRAN_CRTE_DT, CRTE_TS, CRTE_USER_NAM, RPLCTN_UPDT_TS" +
                            ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    tranId,
                    senderParty.getOrDefault("FIRST_NAME", ""),
                    senderParty.getOrDefault("LAST_NAME", ""),
                    senderParty.getOrDefault("EMAIL", ""),
                    senderParty.getOrDefault("PHONE", ""),
                    senderParty.getOrDefault("CITY", ""),
                    senderParty.getOrDefault("COUNTRY", ""),
                    recipientParty.getOrDefault("FIRST_NAME", ""),
                    recipientParty.getOrDefault("LAST_NAME", ""),
                    recipientParty.getOrDefault("EMAIL", ""),
                    recipientParty.getOrDefault("PHONE", ""),
                    recipientParty.getOrDefault("CITY", ""),
                    recipientParty.getOrDefault("COUNTRY", ""),
                    currentTime, currentTime, systemUser, currentTime
            );

            // ================= INSERT ADDRESSES (CONFIG BASED) =================
            String[] types = {"SENDER", "RECIPIENT"};

            for (String type : types) {

                Map<String, Object> values = type.equals("SENDER")
                        ? mappingService.extractAllValues(json, mappingService.getSenderAddressMappings())
                        : mappingService.extractAllValues(json, mappingService.getRecipientAddressMappings());

                jdbc.update(
                        "INSERT INTO SEND_TRAN_ADDR_DTL (" +
                                "ID, TRAN_ID, ADDR_TYPE, ST_LINE1, ST_LINE2, " +
                                "CITY, ST, CNTRY_NAM, POST_CD, " +
                                "CRTE_TS, CRTE_USER_NAM, RPLCTN_UPDT_TS" +
                                ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                        values.get("ID"),
                        tranId,
                        values.get("ADDR_TYPE"),
                        values.get("STREET_LINE_1"),
                        values.get("STREET_LINE_2"),
                        values.get("CITY"),
                        values.get("STATE"),
                        values.get("COUNTRY"),
                        values.get("POSTAL_CODE"),
                        currentTime, systemUser, currentTime
                );
            }

            System.out.println("✅ Transaction inserted using CONFIG mappings: " + tranId);

        } catch (Exception e) {
            System.err.println("❌ Config mapping failed → " + e.getMessage());
            insertAllHardcoded(json);
        }
    }

    private boolean areMappingsLoaded() {
        try {
            return mappingService.getSenderPartyMappings() != null &&
                    !mappingService.getSenderPartyMappings().isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    // ========== ORIGINAL HARDCODED METHOD (FALLBACK) ==========
    private void insertAllHardcoded(String json) {
        System.err.println("⚠️ Using fallback hardcoded inserts (Config missing)");
        // Keep your original fallback here
    }
}
