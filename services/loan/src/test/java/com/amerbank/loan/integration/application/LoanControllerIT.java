package com.amerbank.loan.integration.application;

import com.amerbank.loan.dto.request.LoanApplicationRequest;
import com.amerbank.loan.dto.request.LoanRepaymentRequest;
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

import com.amerbank.loan.client.AccountServiceClientInterface;
import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest(
        properties = {
                "spring.cloud.config.enabled=false"
        },
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
class LoanControllerIT extends AbstractIntegrationTest {

    @TestConfiguration
    static class JwtTestConfig extends TestJwtFactory {
    }

    @MockitoBean
    private AccountServiceClientInterface accountServiceClient;

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
    @DisplayName("Apply for Loan")
    class ApplyLoanTests {

        @Test
        @DisplayName("Should create loan application successfully")
        void shouldCreateLoanApplication() {
            Long customerId = 100L;
            when(accountServiceClient.isAccountOwned(eq(customerId), anyString())).thenReturn(true);

            LoanApplicationRequest request = new LoanApplicationRequest(
                    LoanType.PERSONAL,
                    BigDecimal.valueOf(50000.00),
                    BigDecimal.valueOf(5.5),
                    60,
                    "ACC0000000001"
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(testJwtFactory.generateUserToken(customerId, "test@email.com"));
            HttpEntity<LoanApplicationRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    "/loan/apply",
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            assertEquals(HttpStatus.CREATED, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("PENDING", response.getBody().get("status"));
            assertEquals("LN-" , ((String) response.getBody().get("loanNumber")).substring(0, 3));
        }

        @Test
        @DisplayName("Should not apply for loan when JWT is invalid")
        void shouldNotApplyWhenInvalidJwt() {
            LoanApplicationRequest request = new LoanApplicationRequest(
                    LoanType.PERSONAL,
                    BigDecimal.valueOf(50000.00),
                    BigDecimal.valueOf(5.5),
                    60,
                    "ACC0000000001"
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth("FakeToken");
            HttpEntity<LoanApplicationRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    "/loan/apply",
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        }

        @Test
        @DisplayName("Should not apply for loan when no JWT provided")
        void shouldNotApplyWhenNoJwt() {
            LoanApplicationRequest request = new LoanApplicationRequest(
                    LoanType.PERSONAL,
                    BigDecimal.valueOf(50000.00),
                    BigDecimal.valueOf(5.5),
                    60,
                    "ACC0000000001"
            );

            HttpEntity<LoanApplicationRequest> entity = new HttpEntity<>(request);

            ResponseEntity<String> response = restTemplate.exchange(
                    "/loan/apply",
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        }
    }

    @Nested
    @DisplayName("Get My Loans")
    class GetMyLoansTests {

        @Test
        @DisplayName("Should get my loans successfully")
        void shouldGetMyLoans() {
            Long customerId = 101L;
            saveTestLoan(customerId, LoanStatus.ACTIVE);

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(testJwtFactory.generateUserToken(customerId, "test@email.com"));
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Object[]> response = restTemplate.exchange(
                    "/loan/me",
                    HttpMethod.GET,
                    entity,
                    Object[].class
            );

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(1, response.getBody().length);
        }

        @Test
        @DisplayName("Should return empty list when no loans exist")
        void shouldReturnEmptyListWhenNoLoans() {
            Long customerId = 102L;

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(testJwtFactory.generateUserToken(customerId, "test@email.com"));
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Object[]> response = restTemplate.exchange(
                    "/loan/me",
                    HttpMethod.GET,
                    entity,
                    Object[].class
            );

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(0, response.getBody().length);
        }
    }

    @Nested
    @DisplayName("Get My Loan By Number")
    class GetMyLoanByNumberTests {

        @Test
        @DisplayName("Should get specific loan by number")
        void shouldGetLoanByNumber() {
            Long customerId = 103L;
            Loan loan = saveTestLoan(customerId, LoanStatus.ACTIVE);

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(testJwtFactory.generateUserToken(customerId, "test@email.com"));
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    "/loan/me/" + loan.getLoanNumber(),
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
            Long customerId = 104L;

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(testJwtFactory.generateUserToken(customerId, "test@email.com"));
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    "/loan/me/LN-9999999999",
                    HttpMethod.GET,
                    entity,
                    Map.class
            );

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        }

        @Test
        @DisplayName("Should return 403 when loan belongs to another customer")
        void shouldReturn403WhenLoanNotOwned() {
            Long customerId = 105L;
            Long otherCustomerId = 999L;
            Loan loan = saveTestLoan(otherCustomerId, LoanStatus.ACTIVE);

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(testJwtFactory.generateUserToken(customerId, "test@email.com"));
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    "/loan/me/" + loan.getLoanNumber(),
                    HttpMethod.GET,
                    entity,
                    Map.class
            );

            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        }
    }

    @Nested
    @DisplayName("Get Loan Payments")
    class GetLoanPaymentsTests {

        @Test
        @DisplayName("Should get loan payments")
        void shouldGetLoanPayments() {
            Long customerId = 106L;
            Loan loan = saveTestLoan(customerId, LoanStatus.ACTIVE);

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(testJwtFactory.generateUserToken(customerId, "test@email.com"));
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Object[]> response = restTemplate.exchange(
                    "/loan/me/" + loan.getLoanNumber() + "/payments",
                    HttpMethod.GET,
                    entity,
                    Object[].class
            );

            assertEquals(HttpStatus.OK, response.getStatusCode());
        }

        @Test
        @DisplayName("Should return 404 when loan not found for payments")
        void shouldReturn404WhenLoanNotFound() {
            Long customerId = 107L;

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(testJwtFactory.generateUserToken(customerId, "test@email.com"));
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    "/loan/me/LN-9999999999/payments",
                    HttpMethod.GET,
                    entity,
                    Map.class
            );

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        }
    }

    @Nested
    @DisplayName("Make Repayment")
    class MakeRepaymentTests {

        @Test
        @DisplayName("Should make repayment successfully")
        void shouldMakeRepayment() {
            Long customerId = 108L;
            Loan loan = saveTestLoan(customerId, LoanStatus.ACTIVE);

            LoanRepaymentRequest request = new LoanRepaymentRequest(
                    loan.getLoanNumber(),
                    BigDecimal.valueOf(955.06)
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(testJwtFactory.generateUserToken(customerId, "test@email.com"));
            HttpEntity<LoanRepaymentRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<Void> response = restTemplate.exchange(
                    "/loan/repay",
                    HttpMethod.POST,
                    entity,
                    Void.class
            );

            assertEquals(HttpStatus.OK, response.getStatusCode());
        }

        @Test
        @DisplayName("Should return 404 when loan not found for repayment")
        void shouldReturn404WhenLoanNotFoundForRepayment() {
            Long customerId = 109L;

            LoanRepaymentRequest request = new LoanRepaymentRequest(
                    "LN-9999999999",
                    BigDecimal.valueOf(955.06)
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(testJwtFactory.generateUserToken(customerId, "test@email.com"));
            HttpEntity<LoanRepaymentRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    "/loan/repay",
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        }
    }
}
