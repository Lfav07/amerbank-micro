package com.amerbank.loan.integration.application;

import com.amerbank.loan.dto.request.LoanDecisionRequest;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import com.amerbank.loan.client.AccountServiceClient;
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
class LoanAdminControllerIT extends AbstractIntegrationTest {

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
    @DisplayName("Get All Loans")
    class GetAllLoansTests {

        @Test
        @DisplayName("Should get all loans as admin")
        void shouldGetAllLoans() {
            saveTestLoan(1L, LoanStatus.ACTIVE);
            saveTestLoan(2L, LoanStatus.PENDING);

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(testJwtFactory.generateAdminToken("admin@email.com"));
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Object[]> response = restTemplate.exchange(
                    "/loan/admin/all",
                    HttpMethod.GET,
                    entity,
                    Object[].class
            );

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(2, response.getBody().length);
        }

        @Test
        @DisplayName("Should not allow non-admin to access admin endpoints")
        void shouldNotAllowNonAdmin() {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(testJwtFactory.generateUserToken(1L, "user@email.com"));
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    "/loan/admin/all",
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        }
    }

    @Nested
    @DisplayName("Get Loan By Number")
    class GetLoanByNumberTests {

        @Test
        @DisplayName("Should get loan by number as admin")
        void shouldGetLoanByNumber() {
            Loan loan = saveTestLoan(1L, LoanStatus.ACTIVE);

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(testJwtFactory.generateAdminToken("admin@email.com"));
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    "/loan/admin/" + loan.getLoanNumber(),
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
            headers.setBearerAuth(testJwtFactory.generateAdminToken("admin@email.com"));
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    "/loan/admin/LN-9999999999",
                    HttpMethod.GET,
                    entity,
                    Map.class
            );

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        }
    }

    @Nested
    @DisplayName("Approve Loan")
    class ApproveLoanTests {

        @Test
        @DisplayName("Should approve pending loan")
        void shouldApproveLoan() {
            Loan loan = saveTestLoan(1L, LoanStatus.PENDING);

            LoanDecisionRequest request = new LoanDecisionRequest(loan.getLoanNumber(), null);

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(testJwtFactory.generateAdminToken("admin@email.com"));
            HttpEntity<LoanDecisionRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    "/loan/admin/approve",
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("APPROVED", response.getBody().get("status"));
        }

        @Test
        @DisplayName("Should return 400 when approving non-pending loan")
        void shouldReturn400WhenApprovingNonPendingLoan() {
            Loan loan = saveTestLoan(1L, LoanStatus.ACTIVE);

            LoanDecisionRequest request = new LoanDecisionRequest(loan.getLoanNumber(), null);

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(testJwtFactory.generateAdminToken("admin@email.com"));
            HttpEntity<LoanDecisionRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    "/loan/admin/approve",
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        }

        @Test
        @DisplayName("Should return 404 when approving non-existent loan")
        void shouldReturn404WhenApprovingNonExistentLoan() {
            LoanDecisionRequest request = new LoanDecisionRequest("LN-9999999999", null);

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(testJwtFactory.generateAdminToken("admin@email.com"));
            HttpEntity<LoanDecisionRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    "/loan/admin/approve",
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        }
    }

    @Nested
    @DisplayName("Reject Loan")
    class RejectLoanTests {

        @Test
        @DisplayName("Should reject pending loan with reason")
        void shouldRejectLoan() {
            Loan loan = saveTestLoan(1L, LoanStatus.PENDING);

            LoanDecisionRequest request = new LoanDecisionRequest(loan.getLoanNumber(), "Insufficient credit score");

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(testJwtFactory.generateAdminToken("admin@email.com"));
            HttpEntity<LoanDecisionRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    "/loan/admin/reject",
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("REJECTED", response.getBody().get("status"));
        }

        @Test
        @DisplayName("Should return 400 when rejecting non-pending loan")
        void shouldReturn400WhenRejectingNonPendingLoan() {
            Loan loan = saveTestLoan(1L, LoanStatus.ACTIVE);

            LoanDecisionRequest request = new LoanDecisionRequest(loan.getLoanNumber(), "Some reason");

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(testJwtFactory.generateAdminToken("admin@email.com"));
            HttpEntity<LoanDecisionRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    "/loan/admin/reject",
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        }
    }

    @Nested
    @DisplayName("Disburse Loan")
    class DisburseLoanTests {

        @Test
        @DisplayName("Should return 400 when disbursing non-approved loan")
        void shouldReturn400WhenDisbursingNonApprovedLoan() {
            Loan loan = saveTestLoan(1L, LoanStatus.PENDING);

            LoanDecisionRequest request = new LoanDecisionRequest(loan.getLoanNumber(), null);

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(testJwtFactory.generateAdminToken("admin@email.com"));
            HttpEntity<LoanDecisionRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    "/loan/admin/disburse",
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        }
    }

    @Nested
    @DisplayName("Get Loans By Customer ID")
    class GetLoansByCustomerIdTests {

        @Test
        @DisplayName("Should get loans by customer ID")
        void shouldGetLoansByCustomerId() {
            Long customerId = 200L;
            saveTestLoan(customerId, LoanStatus.ACTIVE);
            saveTestLoan(customerId, LoanStatus.PAID_OFF);

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(testJwtFactory.generateAdminToken("admin@email.com"));
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Object[]> response = restTemplate.exchange(
                    "/loan/admin/customer/" + customerId,
                    HttpMethod.GET,
                    entity,
                    Object[].class
            );

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(2, response.getBody().length);
        }
    }
}
