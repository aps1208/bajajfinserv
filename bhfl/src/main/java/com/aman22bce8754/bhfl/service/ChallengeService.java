package com.aman22bce8754.bhfl.service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.aman22bce8754.bhfl.dto.FinalQueryPayload;
import com.aman22bce8754.bhfl.dto.GenerateWebhookRequest;
import com.aman22bce8754.bhfl.dto.GenerateWebhookResponse;
import com.aman22bce8754.bhfl.model.Solution;
import com.aman22bce8754.bhfl.repo.SolutionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChallengeService {

    private final RestTemplate restTemplate;
    private final SolutionRepository solutionRepository;

    // TODO: Replace with YOUR details
    private static final String NAME = "Aman Pratap Singh";
    private static final String REG_NO = "22BCE8754";
    private static final String EMAIL = "aman.22bce8754@vitapstudent.ac.in";

    private static final String GENERATE_URL =
            "https://bfhldevapigw.healthrx.co.in/hiring/generateWebhook/JAVA";

    public void run() {
        // 1. Generate webhook + token
        GenerateWebhookResponse resp = generateWebhook();
        log.info("Webhook: {}", resp.getWebhook());

        // 2. Decide which SQL question based on regNo
        boolean isOdd = isLastTwoDigitsOdd(REG_NO);
        log.info("regNo odd? {}", isOdd);

        // 3. Build SQL
        String finalSql = buildFinalSql(isOdd);

        // 4. Save to DB
        Solution saved = solutionRepository.save(Solution.builder().finalQuery(finalSql).build());
        log.info("Saved SQL id={} length={}", saved.getId(), finalSql.length());

        // 5. Submit to webhook
        submitFinalQuery(resp.getWebhook(), resp.getAccessToken(), finalSql);
    }

    private GenerateWebhookResponse generateWebhook() {
        GenerateWebhookRequest body = new GenerateWebhookRequest(NAME, REG_NO, EMAIL);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<GenerateWebhookRequest> entity = new HttpEntity<>(body, headers);

        ResponseEntity<GenerateWebhookResponse> response = restTemplate.exchange(
                GENERATE_URL, HttpMethod.POST, entity, GenerateWebhookResponse.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new IllegalStateException("Failed to generate webhook: " + response.getStatusCode());
        }
        return response.getBody();
    }

    private boolean isLastTwoDigitsOdd(String regNo) {
        Pattern p = Pattern.compile("(\\d{2})(?!.*\\d)");
        Matcher m = p.matcher(regNo);
        if (m.find()) {
            int lastTwo = Integer.parseInt(m.group(1));
            return lastTwo % 2 != 0;
        }
        throw new IllegalArgumentException("Invalid regNo: " + regNo);
    }

    private String buildFinalSql(boolean oddQuestion) {
    if (oddQuestion) {
        return """
            SELECT 
    p.AMOUNT AS SALARY,
    CONCAT(e.FIRST_NAME, ' ', e.LAST_NAME) AS NAME,
    TIMESTAMPDIFF(YEAR, e.DOB, CURDATE()) AS AGE,
    d.DEPARTMENT_NAME
FROM PAYMENTS p
JOIN EMPLOYEE e ON p.EMP_ID = e.EMP_ID
JOIN DEPARTMENT d ON e.DEPARTMENT = d.DEPARTMENT_ID
WHERE DAY(p.PAYMENT_TIME) <> 1
ORDER BY p.AMOUNT DESC
LIMIT 1;

            """;
    } else {
        return """
SELECT 
    e.EMP_ID,
    e.FIRST_NAME,
    e.LAST_NAME,
    d.DEPARTMENT_NAME,
    COUNT(e2.EMP_ID) AS YOUNGER_EMPLOYEES_COUNT
FROM EMPLOYEE e
JOIN DEPARTMENT d 
    ON e.DEPARTMENT = d.DEPARTMENT_ID
LEFT JOIN EMPLOYEE e2 
    ON e.DEPARTMENT = e2.DEPARTMENT 
   AND e2.DOB > e.DOB   -- younger employees have later DOB
GROUP BY 
    e.EMP_ID, e.FIRST_NAME, e.LAST_NAME, d.DEPARTMENT_NAME
ORDER BY 
    e.EMP_ID DESC;

            """;
    }
}

    private void submitFinalQuery(String webhookUrl, String accessToken, String finalSql) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", accessToken);

        FinalQueryPayload payload = new FinalQueryPayload(finalSql);
        HttpEntity<FinalQueryPayload> entity = new HttpEntity<>(payload, headers);

        ResponseEntity<String> resp = restTemplate.exchange(
                webhookUrl, HttpMethod.POST, entity, String.class);

        if (!resp.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("Submit failed: " + resp.getStatusCode() + " body=" + resp.getBody());
        }
        log.info("Submission successful: {}", resp.getBody());
    }
}
