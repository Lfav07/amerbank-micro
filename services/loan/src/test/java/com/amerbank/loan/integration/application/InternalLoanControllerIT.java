package com.amerbank.loan.integration.application;

import com.amerbank.loan.dto.request.ServiceDisbursementRequest;
import com.amerbank.loan.dto.request.ServiceRepaymentRequest;
import com.amerbank.loan.integration.persistence.AbstractIntegrationTest;
import com.amerbank.loan.model.Loan;
import com.amerbank.loan.model.LoanStatus;
import com.amerbank.loan.model.LoanType;
import com.amerbank.loan.repository.LoanRepository;
import com.amerbank.loan.util.TestJwtFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import com.amerbank.loan.client.AccountServiceClient;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(
        properties = {
                "spring.cloud.config.enabled=false",
                "eureka.client.enabled=false",
                "spring.kafka.bootstrap-servers=localhost:9092"
        },
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
class InternalLoanControllerIT extends AbstractIntegrationTest {

    @TestConfiguration
    static class JwtTestConfig extends TestJwtFactory {
    }

    @MockitoBean
    private AccountServiceClient accountServiceClient;

    @Autowired
    private TestJwtFactory testJwtFactory;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private LoanRepository loanRepository;

    @AfterEach
    void clearDatabase() {
        loanRepository.deleteAllInBatch();
    }

    private Loan saveTestLoan(Long customerId, LoanStatus status) {
        Loan loan = Loan.builder()
                .loanNumber("LN-" + System.nanoTime())
                .customerId(customerId)
                .accountNumber("ACC0000000001")
                .principalAmount(BigDecimal.valueOf(50000.00))
                .interestRate(BigDecimal.valueOf(5.5))
                .termMonths(60)
                .monthlyPayment(BigDecimal.valueOf(955.06))
                .totalAmount(BigDecimal.valueOf(57303.60))
                .remainingBalance(BigDecimal.valueOf(57303.60))
                .type(LoanType.PERSONAL)
                .status(status)
                .build();
        return loanRepository.save(loan);
    }

    @Nested
    @DisplayName("Get Loan By Number (Internal)")
    class GetLoanByNumberTests {

        @Test
        @DisplayName("Should get loan by number with service token")
        void shouldGetLoanByNumber() {
            Loan loan = saveTestLoan(1L, LoanStatus.ACTIVE);

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(testJwtFactory.generateServiceToken());
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    "/loan/internal/" + loan.getLoanNumber(),
                    HttpMethod.GET,
                    entity,
                    Map.class
            );

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(loan.getLoanNumber(), response.getBody().get("loanNumber"));
        }

        @Test
        @DisplayName("Should return 404 when loan not found")
        void shouldReturn404WhenLoanNotFound() {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(testJwtFactory.generateServiceToken());
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    "/loan/internal/LN-9999999999",
                    HttpMethod.GET,
                    entity,
                    Map.class
            );

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        }

        @Test
        @DisplayName("Should not allow user token for internal endpoints")
        void shouldNotAllowUserToken() {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(testJwtFactory.generateUserToken(1L, "user@email.com"));
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    "/loan/internal/LN-1234567890",
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        }
    }

    @Nested
    @DisplayName("Disburse Loan (Internal)")
    class DisburseLoanTests {

        @Test
        @DisplayName("Should disburse loan internally")
        void shouldDisburseLoanInternally() {
            Loan loan = saveTestLoan(1L, LoanStatus.APPROVED);

            ServiceDisbursementRequest request = new ServiceDisbursementRequest(
                    1L,
                    loan.getLoanNumber(),
                    BigDecimal.valueOf(50000.00),
                    "ACC0000000001"
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(testJwtFactory.generateServiceToken());
            HttpEntity<ServiceDisbursementRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<Void> response = restTemplate.exchange(
                    "/loan/internal/disburse",
                    HttpMethod.POST,
                    entity,
                    Void.class
            );

            assertEquals(HttpStatus.OK, response.getStatusCode());
        }

        @Test
        @DisplayName("Should return 404 when disbursing non-existent loan")
        void shouldReturn404WhenLoanNotFound() {
            ServiceDisbursementRequest request = new ServiceDisbursementRequest(
                    1L,
                    "LN-9999999999",
                    BigDecimal.valueOf(50000.00),
                    "ACC0000000001"
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(testJwtFactory.generateServiceToken());
            HttpEntity<ServiceDisbursementRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    "/loan/internal/disburse",
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        }

        @Test
        @DisplayName("Should return 400 when disbursing non-approved loan")
        void shouldReturn400WhenLoanNotApproved() {
            Loan loan = saveTestLoan(1L, LoanStatus.PENDING);

            ServiceDisbursementRequest request = new ServiceDisbursementRequest(
                    1L,
                    loan.getLoanNumber(),
                    BigDecimal.valueOf(50000.00),
                    "ACC0000000001"
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(testJwtFactory.generateServiceToken());
            HttpEntity<ServiceDisbursementRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    "/loan/internal/disburse",
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        }
    }

    @Nested
    @DisplayName("Process Repayment (Internal)")
    class ProcessRepaymentTests {

        @Test
        @DisplayName("Should process repayment internally")
        void shouldProcessRepaymentInternally() {
            Loan loan = saveTestLoan(1L, LoanStatus.ACTIVE);

            ServiceRepaymentRequest request = new ServiceRepaymentRequest(
                    1L,
                    loan.getLoanNumber(),
                    BigDecimal.valueOf(955.06)
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(testJwtFactory.generateServiceToken());
            HttpEntity<ServiceRepaymentRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<Void> response = restTemplate.exchange(
                    "/loan/internal/repay",
                    HttpMethod.POST,
                    entity,
                    Void.class
            );

            assertEquals(HttpStatus.OK, response.getStatusCode());
        }

        @Test
        @DisplayName("Should return 404 when repaying non-existent loan")
        void shouldReturn404WhenLoanNotFound() {
            ServiceRepaymentRequest request = new ServiceRepaymentRequest(
                    1L,
                    "LN-9999999999",
                    BigDecimal.valueOf(955.06)
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(testJwtFactory.generateServiceToken());
            HttpEntity<ServiceRepaymentRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    "/loan/internal/repay",
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        }

        @Test
        @DisplayName("Should return 400 when repaying non-active loan")
        void shouldReturn400WhenLoanNotActive() {
            Loan loan = saveTestLoan(1L, LoanStatus.PENDING);

            ServiceRepaymentRequest request = new ServiceRepaymentRequest(
                    1L,
                    loan.getLoanNumber(),
                    BigDecimal.valueOf(955.06)
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(testJwtFactory.generateServiceToken());
            HttpEntity<ServiceRepaymentRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    "/loan/internal/repay",
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        }
    }

    @Nested
    @DisplayName("Has Active Loans")
    class HasActiveLoansTests {

        @Test
        @DisplayName("Should return true when customer has active loans")
        void shouldReturnTrueWhenHasActiveLoans() {
            saveTestLoan(1L, LoanStatus.ACTIVE);

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(testJwtFactory.generateServiceToken());
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Boolean> response = restTemplate.exchange(
                    "/loan/internal/customer/1/active",
                    HttpMethod.GET,
                    entity,
                    Boolean.class
            );

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertTrue(response.getBody());
        }

        @Test
        @DisplayName("Should return false when customer has no active loans")
        void shouldReturnFalseWhenNoActiveLoans() {
            saveTestLoan(1L, LoanStatus.PAID_OFF);

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(testJwtFactory.generateServiceToken());
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Boolean> response = restTemplate.exchange(
                    "/loan/internal/customer/1/active",
                    HttpMethod.GET,
                    entity,
                    Boolean.class
            );

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertFalse(response.getBody());
        }
    }
}
