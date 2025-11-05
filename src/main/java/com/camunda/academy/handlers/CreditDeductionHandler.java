package com.camunda.academy.handlers;

import com.camunda.academy.services.CustomerService;
import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.api.worker.JobClient;
import java.util.Map;

import io.camunda.client.api.worker.JobHandler;
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

        double customerCredit = toDouble(variables.get("customerCredit"));
        double orderTotal = toDouble(variables.get("orderTotal"));

        double openAmount = customerService.deductCredit(customerCredit, orderTotal);

        variables.put("openAmount", openAmount);

        client.newCompleteCommand(job)
                .variables(variables)
                .send().exceptionally(throwable -> {
                    throw new RuntimeException("Could not complete job " + job, throwable);
                });
    }

    private double toDouble(Object value) {
        if (value == null) {
            throw new IllegalArgumentException("Variable is null");
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof String) {
            return Double.parseDouble((String) value);
        }
        throw new IllegalArgumentException("Cannot convert variable of type " + value.getClass() + " to double");
    }
}
