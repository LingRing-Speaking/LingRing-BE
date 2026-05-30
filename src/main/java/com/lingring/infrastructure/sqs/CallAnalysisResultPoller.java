package com.lingring.infrastructure.sqs;

import com.lingring.domain.review.facade.CallAnalysisCompletionFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class CallAnalysisResultPoller {

    private static final int MAX_MESSAGES = 10;
    private static final int LONG_POLLING_WAIT_SECONDS = 20;

    private final SqsClient sqsClient;
    private final SqsProperties sqsProperties;
    private final CallAnalysisCompletionFacade callAnalysisCompletionFacade;
    private final ObjectMapper objectMapper;

    @Scheduled(
            fixedDelayString = "${aws.sqs.poll-interval-ms:10000}",
            initialDelayString = "${aws.sqs.poll-initial-delay-ms:0}"
    )
    public void poll() {
        final ReceiveMessageResponse response;
        try {
            response = sqsClient.receiveMessage(ReceiveMessageRequest.builder()
                    .queueUrl(sqsProperties.callAnalysisResultQueueUrl())
                    .waitTimeSeconds(LONG_POLLING_WAIT_SECONDS)
                    .maxNumberOfMessages(MAX_MESSAGES)
                    .build());
        } catch (final RuntimeException e) {
            log.error("SQS receiveMessage 실패", e);
            return;
        }

        for (final Message message : response.messages()) {
            handle(message);
        }
    }

    private void handle(final Message message) {
        final CallAnalysisResultMessage payload;
        try {
            payload = objectMapper.readValue(message.body(), CallAnalysisResultMessage.class);
        } catch (final RuntimeException e) {
            log.error("call analysis SQS 메시지 파싱 실패: messageId={}, body={}",
                    message.messageId(), message.body(), e);
            return;
        }

        try {
            callAnalysisCompletionFacade.complete(payload);
            acknowledge(message.receiptHandle());
        } catch (final RuntimeException e) {
            log.error("call analysis 영속화 실패: callId={}", payload.callId(), e);
        }
    }

    private void acknowledge(final String receiptHandle) {
        sqsClient.deleteMessage(DeleteMessageRequest.builder()
                .queueUrl(sqsProperties.callAnalysisResultQueueUrl())
                .receiptHandle(receiptHandle)
                .build());
    }
}
