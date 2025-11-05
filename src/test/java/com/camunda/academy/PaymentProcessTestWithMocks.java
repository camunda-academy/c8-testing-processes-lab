package com.camunda.academy;

import com.camunda.academy.handlers.CreditCardChargingHandler;
import com.camunda.academy.handlers.CreditDeductionHandler;
import com.camunda.academy.services.CreditCardService;
import com.camunda.academy.services.CustomerService;

import io.camunda.client.CamundaClient;
import io.camunda.client.api.response.ActivateJobsResponse;
import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.api.response.ProcessInstanceEvent;
import io.camunda.client.api.worker.JobHandler;
import io.camunda.process.test.api.CamundaProcessTest;
import io.camunda.process.test.api.CamundaProcessTestContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;

import static io.camunda.process.test.api.CamundaAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@CamundaProcessTest
public class PaymentProcessTestWithMocks {

    private CamundaProcessTestContext processTestContext;
    private CamundaClient client;

    @Mock
    CustomerService customerServiceMock = mock();

    @Mock
    CreditCardService creditCardServiceMock = mock();

    @BeforeEach
    public void setup(){
        client
            .newDeployResourceCommand()
            .addResourceFromClasspath("payment.bpmn")
            .send()
            .join();
    }
    
    @DisplayName("Path when the customer has sufficient credit")
    @Test
    public void testHappyPath() throws Exception {
        // given
        double ORDER_TOTAL = 42.0;
        double CUSTOMER_CREDIT = 50.0;
        Map<String, Object> startVars = Map.of(
            "orderTotal", ORDER_TOTAL, 
            "customerCredit", CUSTOMER_CREDIT);
        JobHandler creditDeductionHandler = new CreditDeductionHandler(customerServiceMock);
        Mockito
            .when(customerServiceMock.deductCredit(CUSTOMER_CREDIT, ORDER_TOTAL))
            .thenReturn(0.0);

        // when
        ProcessInstanceEvent processInstance = startInstance(
            "PaymentProcess",
            startVars);
        completeJob("credit-deduction", 1, creditDeductionHandler);

        // then
        Mockito.verify(customerServiceMock).deductCredit(CUSTOMER_CREDIT,ORDER_TOTAL );
        assertThat(processInstance)
            .hasVariable("openAmount", 0.0)
            .hasCompletedElements("EndEvent_PaymentCompleted")
            .hasNotActivatedElements("Task_ChargeCreditCard")
            .isCompleted();
    }

    @DisplayName("Path when credit card charging is needed")
    @Test
    public void testCreditCardPath() throws Exception {
        // given
        double OPEN_AMOUNT = 50.0;
        String CARD_NR = "TEST_NR";
        String CVC = "ABC";
        String EXPIRY_DATE = "01/99";
        Map<String, Object> startVars = Map.of(
                "openAmount", OPEN_AMOUNT,
                "expiryDate", EXPIRY_DATE,
                "cardNumber", CARD_NR,
                "cvc", CVC);
        JobHandler creditCardHandler = new CreditCardChargingHandler(creditCardServiceMock);

        // when
        ProcessInstanceEvent processInstance = startInstanceBefore(
            "PaymentProcess", 
            startVars, 
            "Gateway_CreditSufficient");
        processTestContext.completeUserTask("Task_VerifyCreditCardData");
        completeJob("credit-card-charging", 1, creditCardHandler);

        // then
        Mockito
            .verify(creditCardServiceMock)
            .chargeAmount(CARD_NR, CVC, EXPIRY_DATE, 50.0);
        assertThat(processInstance)
            .hasCompletedElements("Task_ChargeCreditCard")
            .isCompleted();

    }

    public void completeJob(String type, int count, JobHandler handler) throws Exception {
        ActivateJobsResponse activateJobsResponse = 
            client
                .newActivateJobsCommand()
                .jobType(type)
                .maxJobsToActivate(count)
                .send()
                .join();

        List<ActivatedJob> activatedJobs = activateJobsResponse.getJobs();
        if(activatedJobs.size() != count){
            fail("No job activated for type " + type);
        }

        for (ActivatedJob job:activatedJobs) {
            handler.handle(client, job);
        }

        // Optionally wait for async processing to complete
        Thread.sleep(1000);
    }

    public ProcessInstanceEvent startInstance(String id, Map<String, Object> variables){
        ProcessInstanceEvent processInstance = 
            client
                .newCreateInstanceCommand()
                .bpmnProcessId(id)
                .latestVersion()
                .variables(variables)
                .send()
                .join();

        assertThat(processInstance)
            .isCreated();
        return processInstance;
    }

    public ProcessInstanceEvent startInstanceBefore(String id, Map<String, Object> variables, String startingPoint){
        ProcessInstanceEvent processInstance = 
            client
                .newCreateInstanceCommand()
                .bpmnProcessId(id)
                .latestVersion()
                .variables(variables)
                .startBeforeElement(startingPoint)
                .send()
                .join();

        assertThat(processInstance)
            .isCreated();
        return processInstance;
    }
}
