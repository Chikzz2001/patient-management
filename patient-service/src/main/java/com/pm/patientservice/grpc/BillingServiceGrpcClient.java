package com.pm.patientservice.grpc;

import billing.BillingRequest;
import billing.BillingResponse;
import billing.BillingServiceGrpc;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

@Slf4j
@Service
public class BillingServiceGrpcClient {

    @GrpcClient("billing-service")
    private BillingServiceGrpc.BillingServiceBlockingStub blockingStub;

    //localhost:9001/BillingService/CreatePatientAccount
//    public BillingServiceGrpcClient(
//            @Value("${billing.service.address:localhost}") String serverAddress,
//            @Value("${billing.service.grpc.port:9001}") int serverPort
//    ) {
//        log.info("Connecting to Billing Service at {}:{}", serverAddress, serverPort);
//
//        ManagedChannel channel = ManagedChannelBuilder.forAddress(serverAddress, serverPort).usePlaintext().build();
//
//        blockingStub = BillingServiceGrpc.newBlockingStub(channel);
//    }

    @CircuitBreaker(name = "billing-service", fallbackMethod = "fallbackMethod")
    @TimeLimiter(name = "billing-service")
    public CompletableFuture<BillingResponse> createBillingAccount(String patientId, String name, String email) {
        BillingRequest request = BillingRequest.newBuilder()
                .setPatientId(patientId)
                .setName(name)
                .setEmail(email)
                .build();

        BillingResponse response = blockingStub.createBillingAccount(request);
        log.info("Received response from Billing Service via gRPC: {}", response);

        return CompletableFuture.supplyAsync(() -> response);
    }

    public CompletableFuture<BillingResponse> fallbackMethod(String patientId, String name, String email,
                                                             RuntimeException runtimeException) {
        BillingResponse response = BillingResponse.newBuilder().setStatus("ERROR").build();
        log.info("Could not connect to Billing Service via gRPC: {}", response);

        return CompletableFuture.supplyAsync(() -> response);
    }
}
