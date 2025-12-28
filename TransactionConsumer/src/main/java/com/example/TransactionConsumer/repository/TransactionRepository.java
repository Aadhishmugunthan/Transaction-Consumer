package com.example.TransactionConsumer.repository;

import com.example.TransactionConsumer.service.MappingService;
import com.jayway.jsonpath.JsonPath;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.Map;
import java.util.UUID;

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

        // Check if mappings are loaded
        if (!areMappingsLoaded()) {
            System.err.println("WARNING: Mappings not loaded from Config Server, using fallback hardcoded values");
            insertAllHardcoded(json);
            return;
        }

        try {
            // ============= Extract values using EXTERNALIZED MAPPINGS =============
            Map<String, Object> senderParty = mappingService.extractAllValues(
                    json,
                    mappingService.getSenderPartyMappings()
            );

            Map<String, Object> senderAddress = mappingService.extractAllValues(
                    json,
                    mappingService.getSenderAddressMappings()
            );

            Map<String, Object> recipientParty = mappingService.extractAllValues(
                    json,
                    mappingService.getRecipientPartyMappings()
            );

            Map<String, Object> recipientAddress = mappingService.extractAllValues(
                    json,
                    mappingService.getRecipientAddressMappings()
            );

            // Extract main transaction fields
            String tranId = JsonPath.read(json, "$.transactionId");
            String tranType = JsonPath.read(json, "$.transactionType");
            Integer tranAmt = JsonPath.read(json, "$.amount");
            String tranCurr = JsonPath.read(json, "$.currency");
            String custRefNum = JsonPath.read(json, "$.customerReferenceNumber");
            String origInstNam = JsonPath.read(json, "$.originatingInstitution");
            String tranfrAcptNam = JsonPath.read(json, "$.transferAcceptorName");

            // ============= STEP 1: Insert SEND_TRANSACTIONS =============
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

            // ============= STEP 2: Insert SEND_TRAN_DTL =============
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

            // ============= STEP 3: Insert SEND_RECIP_DTL =============
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

            // ============= STEP 4: Insert SENDER Address =============
            String senderAddrId = UUID.randomUUID().toString();

            jdbc.update(
                    "INSERT INTO SEND_TRAN_ADDR_DTL (" +
                            "ID, TRAN_ID, ADDR_TYPE, ST_LINE1, ST_LINE2, " +
                            "CITY, ST, CNTRY_NAM, POST_CD, " +
                            "CRTE_TS, CRTE_USER_NAM, RPLCTN_UPDT_TS" +
                            ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    senderAddrId,
                    tranId,
                    senderAddress.getOrDefault("ADDR_TYPE", "SENDER_BILLING"),
                    senderAddress.getOrDefault("STREET_LINE_1", ""),
                    senderAddress.getOrDefault("STREET_LINE_2", ""),
                    senderAddress.getOrDefault("CITY", ""),
                    senderAddress.getOrDefault("STATE", ""),
                    senderAddress.getOrDefault("COUNTRY", ""),
                    senderAddress.getOrDefault("POSTAL_CODE", ""),
                    currentTime, systemUser, currentTime
            );

            // ============= STEP 5: Insert RECIPIENT Address =============
            String recipientAddrId = UUID.randomUUID().toString();

            jdbc.update(
                    "INSERT INTO SEND_TRAN_ADDR_DTL (" +
                            "ID, TRAN_ID, ADDR_TYPE, ST_LINE1, ST_LINE2, " +
                            "CITY, ST, CNTRY_NAM, POST_CD, " +
                            "CRTE_TS, CRTE_USER_NAM, RPLCTN_UPDT_TS" +
                            ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    recipientAddrId,
                    tranId,
                    recipientAddress.getOrDefault("ADDR_TYPE", "RECIPIENT_SHIPPING"),
                    recipientAddress.getOrDefault("STREET_LINE_1", ""),
                    recipientAddress.getOrDefault("STREET_LINE_2", ""),
                    recipientAddress.getOrDefault("CITY", ""),
                    recipientAddress.getOrDefault("STATE", ""),
                    recipientAddress.getOrDefault("COUNTRY", ""),
                    recipientAddress.getOrDefault("POSTAL_CODE", ""),
                    currentTime, systemUser, currentTime
            );

            System.out.println("Transaction inserted successfully using externalized mappings: " + tranId);

        } catch (Exception e) {
            System.err.println("Error with externalized mappings, falling back to hardcoded: " + e.getMessage());
            insertAllHardcoded(json);
        }
    }

    /**
     * Check if mappings are properly loaded from Config Server
     */
    private boolean areMappingsLoaded() {
        try {
            Map<String, ?> senderMappings = mappingService.getSenderPartyMappings();
            return senderMappings != null && !senderMappings.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Fallback method with hardcoded mappings (your original code)
     */
    private void insertAllHardcoded(String json) {
        Timestamp currentTime = new Timestamp(System.currentTimeMillis());
        String systemUser = "SYSTEM";

        // Extract using hardcoded JSONPath
        String tranId = JsonPath.read(json, "$.transactionId");
        String tranType = JsonPath.read(json, "$.transactionType");
        Integer tranAmt = JsonPath.read(json, "$.amount");
        String tranCurr = JsonPath.read(json, "$.currency");
        String custRefNum = JsonPath.read(json, "$.customerReferenceNumber");
        String origInstNam = JsonPath.read(json, "$.originatingInstitution");
        String tranfrAcptNam = JsonPath.read(json, "$.transferAcceptorName");

        jdbc.update(
                "INSERT INTO SEND_TRANSACTIONS (TRAN_ID, TRAN_TYPE, TRAN_AMT, TRAN_CURR, TRAN_CRTE_DT, " +
                        "CUR_STAT, ORIG_STAT, CUST_REF_NUM, ORIG_INST_NAM, TRANFR_ACPT_NAM, " +
                        "CRTE_TS, CRTE_USER_NAM, RPLCTN_UPDT_TS, NON_FIN_TXN) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                tranId, tranType, tranAmt, tranCurr, currentTime,
                "COMPLETED", "NEW", custRefNum, origInstNam, tranfrAcptNam,
                currentTime, systemUser, currentTime, 0
        );

        String paymtRef = JsonPath.read(json, "$.paymentReference");
        String fundSrc = JsonPath.read(json, "$.fundingSource");
        String paymtType = JsonPath.read(json, "$.paymentType");

        jdbc.update(
                "INSERT INTO SEND_TRAN_DTL (TRAN_ID, PAYMT_REF, FUND_SRC, PAYMT_TYPE, " +
                        "TRAN_CRTE_DT, CRTE_TS, CRTE_USER_NAM, RPLCTN_UPDT_TS) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                tranId, paymtRef, fundSrc, paymtType, currentTime, currentTime, systemUser, currentTime
        );

        String sendFirstNam = JsonPath.read(json, "$.sender.firstName");
        String sendLastNam = JsonPath.read(json, "$.sender.lastName");
        String sendEmail = JsonPath.read(json, "$.sender.email");
        String sendPhn = JsonPath.read(json, "$.sender.phone");
        String sendCity = JsonPath.read(json, "$.sender.city");
        String sendCntryNam = JsonPath.read(json, "$.sender.country");

        String recipFirstNam = JsonPath.read(json, "$.recipient.firstName");
        String recipLastNam = JsonPath.read(json, "$.recipient.lastName");
        String recipEmail = JsonPath.read(json, "$.recipient.email");
        String recipPhn = JsonPath.read(json, "$.recipient.phone");
        String recipCity = JsonPath.read(json, "$.recipient.city");
        String recipCntryNam = JsonPath.read(json, "$.recipient.country");

        jdbc.update(
                "INSERT INTO SEND_RECIP_DTL (TRAN_ID, " +
                        "SEND_FIRST_NAM, SEND_LST_NAM, SEND_EMAIL, SEND_PHN, SEND_CITY, SEND_CNTRY_NAM, " +
                        "RECIP_FIRST_NAM, RECIP_LST_NAM, RECIP_EMAIL, RECIP_PHN, RECIP_CITY, RECIP_CNTRY_NAM, " +
                        "TRAN_CRTE_DT, CRTE_TS, CRTE_USER_NAM, RPLCTN_UPDT_TS) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                tranId,
                sendFirstNam, sendLastNam, sendEmail, sendPhn, sendCity, sendCntryNam,
                recipFirstNam, recipLastNam, recipEmail, recipPhn, recipCity, recipCntryNam,
                currentTime, currentTime, systemUser, currentTime
        );

        String senderAddrId = UUID.randomUUID().toString();
        String senderStLine1 = JsonPath.read(json, "$.sender.address.streetLine1");
        String senderStLine2 = JsonPath.read(json, "$.sender.address.streetLine2");
        String senderAddrCity = JsonPath.read(json, "$.sender.address.city");
        String senderState = JsonPath.read(json, "$.sender.address.state");
        String senderAddrCntry = JsonPath.read(json, "$.sender.address.country");
        String senderPostCd = JsonPath.read(json, "$.sender.address.postalCode");

        jdbc.update(
                "INSERT INTO SEND_TRAN_ADDR_DTL (ID, TRAN_ID, ADDR_TYPE, ST_LINE1, ST_LINE2, " +
                        "CITY, ST, CNTRY_NAM, POST_CD, CRTE_TS, CRTE_USER_NAM, RPLCTN_UPDT_TS) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                senderAddrId, tranId, "SENDER_BILLING", senderStLine1, senderStLine2,
                senderAddrCity, senderState, senderAddrCntry, senderPostCd,
                currentTime, systemUser, currentTime
        );

        String recipientAddrId = UUID.randomUUID().toString();
        String recipStLine1 = JsonPath.read(json, "$.recipient.address.streetLine1");
        String recipStLine2 = JsonPath.read(json, "$.recipient.address.streetLine2");
        String recipAddrCity = JsonPath.read(json, "$.recipient.address.city");
        String recipState = JsonPath.read(json, "$.recipient.address.state");
        String recipAddrCntry = JsonPath.read(json, "$.recipient.address.country");
        String recipPostCd = JsonPath.read(json, "$.recipient.address.postalCode");

        jdbc.update(
                "INSERT INTO SEND_TRAN_ADDR_DTL (ID, TRAN_ID, ADDR_TYPE, ST_LINE1, ST_LINE2, " +
                        "CITY, ST, CNTRY_NAM, POST_CD, CRTE_TS, CRTE_USER_NAM, RPLCTN_UPDT_TS) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                recipientAddrId, tranId, "RECIPIENT_SHIPPING", recipStLine1, recipStLine2,
                recipAddrCity, recipState, recipAddrCntry, recipPostCd,
                currentTime, systemUser, currentTime
        );

        System.out.println("Transaction inserted successfully using hardcoded mappings (fallback): " + tranId);
    }
}