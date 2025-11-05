package com.camunda.academy;

import io.camunda.client.CamundaClient;
import io.camunda.client.api.response.ActivateJobsResponse;
import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.api.response.ProcessInstanceEvent;
import io.camunda.client.api.search.response.UserTask;
import io.camunda.process.test.api.CamundaProcessTest;
import io.camunda.process.test.api.CamundaProcessTestContext;
import static io.camunda.process.test.api.assertions.UserTaskSelectors.*;

import io.camunda.process.test.api.assertions.UserTaskSelector;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static io.camunda.process.test.api.CamundaAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@CamundaProcessTest
public class HardwareRequestProcessTest {

    private CamundaProcessTestContext processTestContext;
    private CamundaClient client;


    @BeforeEach
    public void setup(){
        client
            .newDeployResourceCommand()
            .addResourceFromClasspath("hardwarerequest.bpmn")
            .send()
            .join();
    }

    @DisplayName("Happy path when the hardware is available")
    @Test
    public void testHappyPath() throws Exception {
        // given
        double PRICE = 240;
        Map<String, Object> startVars = Map.of("price", PRICE);

        // when
        ProcessInstanceEvent processInstance = startInstance("HardwareRequestProcess", startVars);
        completeJob("check-availability", 1, Map.of("available", true));
        completeJob("send-hardware", 1, Map.of());

        // then
        assertThat(processInstance)
            .hasCompletedElements("EndEvent_HardwareSent")
            .isCompleted();
    }

    @DisplayName("Path when the hardware is not available")
    @Test
    public void testHardwareNotAvailable() throws Exception {
        // given
        boolean AVAILABLE = false;
        String ORDER_ID = "test";
        Map<String, Object> startVars = Map.of("available", AVAILABLE, "orderId", ORDER_ID);

        // when
        ProcessInstanceEvent processInstance = startInstanceBefore("HardwareRequestProcess", startVars, "Gateway_HardwareAvailable");
        completeJob("order-hardware", 1, Map.of());

        client
            .newPublishMessageCommand()
            .messageName("hardwareReceived")
            .correlationKey("test")
            .send()
            .join();

        // then
        assertThat(processInstance)
            .hasActiveElement("ServiceTask_SendHardware", 1);
    }

    @DisplayName("Path when the supplier is not delivering the hardware in time.")
    @Test
    public void testSupplierDelay() throws Exception {
        // given
        String ORDER_ID = "test";
        Map<String, Object> startVars = Map.of("orderId", ORDER_ID);

        // when
        ProcessInstanceEvent processInstance = startInstanceBefore("HardwareRequestProcess", startVars, "Gateway_WaitForHardware");

        // advance engine time and wait for processing
        processTestContext.increaseTime(Duration.ofDays(7));

        // complete the user task that should be active after the delay
        processTestContext.completeUserTask("UserTask_CallWithSupplier");

        // then
        assertThat(processInstance)
            .hasActiveElement("Gateway_WaitForHardware", 1);
    }

    @DisplayName("Path when the order is approved.")
    @Test
    public void testApproval() throws Exception {
        // given
        double PRICE = 1230;
        List<String> approvers = new LinkedList<>();
        approvers.add("john");
        approvers.add("lisa");
        approvers.add("max");
        Map<String, Object> startVars = Map.of("price", PRICE, "approvers", approvers);

        // when all approvers approve the order
        ProcessInstanceEvent processInstance = startInstance("HardwareRequestProcess", startVars);
        processTestContext.completeUserTask(byElementId("UserTask_ApproveOrder").and(byAssignee("john")), Map.of("approved", true));
        processTestContext.completeUserTask(byElementId("UserTask_ApproveOrder").and(byAssignee("lisa")), Map.of("approved", true));
        processTestContext.completeUserTask(byElementId("UserTask_ApproveOrder").and(byAssignee("max")), Map.of("approved", true));

        //then the process is waiting for hardware availability check
        assertThat(processInstance)
            .hasActiveElement("ServiceTask_CheckAvailability", 1);
    }

    @DisplayName("Path when the order is rejected.")
    @Test
    public void testRejection() throws Exception {
        // given
        double PRICE = 1230;
        List<String> approvers = new LinkedList<>();
        approvers.add("john");
        approvers.add("lisa");
        approvers.add("max");
        Map<String, Object> startVars = Map.of("price", PRICE, "approvers", approvers);

        // when
        ProcessInstanceEvent processInstance = startInstance("HardwareRequestProcess", startVars);
 
        // then there are three user tasks for approval
        Assertions.assertThat(
            client
                .newUserTaskSearchRequest()
                .filter(userTaskFilter -> userTaskFilter.elementId("UserTask_ApproveOrder"))
                .send().join().items()
            )
            .hasSize(3)
            .extracting(UserTask::getAssignee)
            .contains("john", "lisa", "max");

        // when at least one approver rejects the order
        processTestContext.completeUserTask(byElementId("UserTask_ApproveOrder").and(byAssignee("john")), Map.of("approved", true));
        processTestContext.completeUserTask(byElementId("UserTask_ApproveOrder").and(byAssignee("lisa")), Map.of("approved", true));
        processTestContext.completeUserTask(byElementId("UserTask_ApproveOrder").and(byAssignee("max")), Map.of("approved", false));

        // then the process is ended with rejection
        assertThat(processInstance)
            .hasCompletedElement("Gateway_1dpgaqe", 1)
            .hasVariable("approved", List.of(true, true, false))
            .hasCompletedElement("EndEvent_OrderRejected", 1)
            .isCompleted();
    }

    private static UserTaskSelector byAssignee(String assignee){
        return userTask -> userTask.getAssignee().equals(assignee);
    }

    @DisplayName("Path when the hardware is stolen.")
    @Test
    public void testErrorPath() throws Exception {
        // given

        // when
        ProcessInstanceEvent processInstance =
                startInstanceBefore("HardwareRequestProcess", Map.of(), "ServiceTask_SendHardware");
        completeJobWithError("send-hardware", 1, "stolen");
        completeJob("inform-requester", 1, Map.of());

        // then
        assertThat(processInstance)
            .hasCompletedElement("EndEvent_HardwareStolen",1)
            .isCompleted();
    }

    public void completeJob(String type, int count, Map<String, Object> variables) throws Exception {
        ActivateJobsResponse activateJobsResponse = 
            client
                .newActivateJobsCommand()
                .jobType(type)
                .maxJobsToActivate(count)
                .send()
                .join();

        List<ActivatedJob> activatedJobs = activateJobsResponse.getJobs();
        if(activatedJobs.size() != count){
            fail("No task found for " + type);
        }

        for (ActivatedJob job:activatedJobs) {
            client
                .newCompleteCommand(job)
                .variables(variables)
                .send()
                .join();
        }
    }

    public void completeJobWithError(String type, int count, String errorCode) throws Exception {
        ActivateJobsResponse activateJobsResponse = 
            client
                .newActivateJobsCommand()
                .jobType(type)
                .maxJobsToActivate(count)
                .send()
                .join();

        List<ActivatedJob> activatedJobs = activateJobsResponse.getJobs();
        if(activatedJobs.size() != count){
            fail("No task found for " + type);
        }

        for (ActivatedJob job:activatedJobs) {
            client
                .newThrowErrorCommand(job)
                .errorCode(errorCode)
                .send()
                .join();
        }
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

        assertThat(processInstance).isCreated();
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

        assertThat(processInstance).isCreated();
        return processInstance;
    }
}