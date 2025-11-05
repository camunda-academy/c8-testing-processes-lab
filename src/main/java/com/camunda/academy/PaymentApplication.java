package com.camunda.academy;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.Properties;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.camunda.academy.handlers.CreditCardChargingHandler;
import com.camunda.academy.handlers.CreditDeductionHandler;

import io.camunda.client.CamundaClient;
import io.camunda.client.api.worker.JobWorker;
import io.camunda.client.impl.oauth.OAuthCredentialsProvider;
import io.camunda.client.impl.oauth.OAuthCredentialsProviderBuilder;

public class PaymentApplication {

    private static final Logger logger = LoggerFactory.getLogger(PaymentApplication.class);

    //Zeebe Client Credentials
    private static final String CAMUNDA_PROPERTIES_PATH = "src/main/resources/application.properties";
    private static String CAMUNDA_AUTHORIZATION_SERVER_URL;
    private static String CAMUNDA_CLIENT_ID;
    private static String CAMUNDA_CLIENT_SECRET;
    private static String CAMUNDA_TOKEN_AUDIENCE;
    private static String CAMUNDA_REST_ADDRESS;
    private static String CAMUNDA_GRPC_ADDRESS;

    //Payment Application Details
    private static final int WORKER_TIMEOUT = 10;

    public static void main(String[] args){
        loadProperties();
        final OAuthCredentialsProvider credentialsProvider = new OAuthCredentialsProviderBuilder()
            .authorizationServerUrl(CAMUNDA_AUTHORIZATION_SERVER_URL)
            .audience(CAMUNDA_TOKEN_AUDIENCE)
            .clientId(CAMUNDA_CLIENT_ID)
            .clientSecret(CAMUNDA_CLIENT_SECRET)
            .build();

        try (final CamundaClient  client = CamundaClient.newClientBuilder()
                .grpcAddress(URI.create(CAMUNDA_GRPC_ADDRESS))
                .restAddress(URI.create(CAMUNDA_REST_ADDRESS))
                .credentialsProvider(credentialsProvider)
                 .build()) {

            //Start the Credit Deduction Worker
            final JobWorker creditDeductionWorker =
                client.newWorker()
                    .jobType("credit-deduction")
                    .handler(new CreditDeductionHandler())
                    .timeout(Duration.ofSeconds(WORKER_TIMEOUT).toMillis())
                    .open();

            //Start the Credit Deduction Worker
            final JobWorker creditCardChargingWorker =
                client.newWorker()
                    .jobType("credit-card-charging")
                    .handler(new CreditCardChargingHandler())
                    .timeout(Duration.ofSeconds(WORKER_TIMEOUT).toMillis())
                    .open();

            //Wait for the Workers
            Scanner sc = new Scanner(System.in);
            sc.nextInt();
            sc.close();
            creditDeductionWorker.close();
            creditCardChargingWorker.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void loadProperties() {
        Properties properties = new Properties();
        try (FileInputStream input = new FileInputStream(CAMUNDA_PROPERTIES_PATH)) {
            properties.load(input);
            CAMUNDA_AUTHORIZATION_SERVER_URL = properties.getProperty("camunda.auth.server.url");
            CAMUNDA_CLIENT_ID = properties.getProperty("camunda.client.auth.client-id");
            CAMUNDA_CLIENT_SECRET = properties.getProperty("camunda.client.auth.client-secret");
            CAMUNDA_REST_ADDRESS = properties.getProperty("CAMUNDA_REST_ADDRESS");
            CAMUNDA_GRPC_ADDRESS = properties.getProperty("CAMUNDA_GRPC_ADDRESS");
            CAMUNDA_TOKEN_AUDIENCE = properties.getProperty("CAMUNDA_TOKEN_AUDIENCE");
        
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}