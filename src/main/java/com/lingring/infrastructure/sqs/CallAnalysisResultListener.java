package com.lingring.infrastructure.sqs;

import com.lingring.domain.review.facade.CallAnalysisCompletionFacade;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CallAnalysisResultListener {

    private final CallAnalysisCompletionFacade callAnalysisCompletionFacade;

    /**
     * 결과 큐를 소비한다. 메시지 본문(JSON)은 프레임워크가 {@link CallAnalysisResultMessage}로 역직렬화한다.
     * 메서드가 정상 반환하면(성공) 프레임워크가 메시지를 ack(삭제)하고,
     * 예외가 전파되면 ack하지 않아 visibility timeout 후 재전달 → maxReceiveCount 초과 시 DLQ로 이행한다.
     */
    @SqsListener("${aws.sqs.call-analysis-result-queue-url}")
    public void handle(final CallAnalysisResultMessage message) {
        callAnalysisCompletionFacade.complete(message);
    }
}
