package com.lingring.infrastructure.lambda;

import com.lingring.domain.call.domain.TranscriptionStarter;
import com.lingring.domain.call.domain.vo.RecordingReference;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvocationType;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class AwsLambdaTranscriptionStarter implements TranscriptionStarter {

    private final LambdaClient lambdaClient;
    private final LambdaProperties lambdaProperties;
    private final ObjectMapper objectMapper;

    @Override
    public void requestTranscript(final Long callId, final List<RecordingReference> recordings) {
        final String payload = objectMapper.writeValueAsString(
                new TranscriptInvocationPayload(callId, recordings));
        final InvokeRequest request = InvokeRequest.builder()
                .functionName(lambdaProperties.transcriptFunctionName())
                .invocationType(InvocationType.EVENT)
                .payload(SdkBytes.fromUtf8String(payload))
                .build();
        lambdaClient.invoke(request);
    }

    private record TranscriptInvocationPayload(Long callId, List<RecordingReference> recordings) {
    }
}
