package com.camunda.academy.handlers;

import com.camunda.academy.services.CustomerService;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;
import java.util.Map;

import io.camunda.zeebe.client.api.worker.JobHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreditDeductionHandler implements JobHandler {

    Logger LOGGER = LoggerFactory.getLogger(CreditDeductionHandler.class);

    CustomerService customerService;

    public CreditDeductionHandler(CustomerService customerService) {
        this.customerService = customerService;
    }

    public CreditDeductionHandler() {
        this.customerService = new CustomerService();
    }

    @Override
    public void handle(JobClient client, ActivatedJob job) {
        LOGGER.info("Task definition type: " + job.getType());

        Map<String, Object> variables = job.getVariablesAsMap();

        double customerCredit = (double) variables.get("customerCredit");
        double orderTotal = (double) variables.get("orderTotal");

        double openAmount = customerService.deductCredit(customerCredit, orderTotal);

        variables.put("openAmount", openAmount);

        client.newCompleteCommand(job)
                .variables(variables)
                .send().exceptionally(throwable -> {
                    throw new RuntimeException("Could not complete job " + job, throwable);
                });
    }
}
