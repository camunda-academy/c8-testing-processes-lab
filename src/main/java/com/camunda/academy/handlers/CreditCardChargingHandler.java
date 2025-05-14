package com.camunda.academy.handlers;

import com.camunda.academy.exceptions.InvalidCreditCardException;
import com.camunda.academy.services.CreditCardService;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;
import io.camunda.zeebe.client.api.worker.JobHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class CreditCardChargingHandler implements JobHandler {

  Logger LOGGER = LoggerFactory.getLogger(CreditCardChargingHandler.class);

  private final CreditCardService creditCardService;

  public CreditCardChargingHandler(CreditCardService creditCardService) {
    this.creditCardService = creditCardService;
  }

  public CreditCardChargingHandler() {
    this.creditCardService = new CreditCardService();
  }

  @Override
  public void handle(JobClient client, ActivatedJob job) {
    LOGGER.info("Task definition type: " + job.getType());

    Map<String, Object> variables = job.getVariablesAsMap();
    String cardNumber = (String) variables.get("cardNumber");
    String cvc = (String) variables.get("cvc");
    String expiryDate = (String) variables.get("expiryDate");
    double openAmount = (double) variables.get("openAmount");

    try {
      creditCardService.chargeAmount(cardNumber, cvc, expiryDate, openAmount);
      client.newCompleteCommand(job)
              .send().exceptionally(throwable -> {
                throw new RuntimeException("Could not complete job " + job, throwable);
              });
    } catch (InvalidCreditCardException e) {
      client.newFailCommand(job)
              .retries(0)
              .errorMessage("Invalid expiry date: " + expiryDate)
              .send().exceptionally(throwable -> {
                throw new RuntimeException("Could not fail job " + job, throwable);
              });
    }
  }
}