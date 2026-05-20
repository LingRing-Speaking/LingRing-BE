package com.lingring.infrastructure.rekognition;

import com.lingring.domain.user.domain.ModerationVerdict;
import com.lingring.domain.user.domain.ProfileImageModerator;
import com.lingring.infrastructure.s3.S3Properties;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.DetectModerationLabelsRequest;
import software.amazon.awssdk.services.rekognition.model.DetectModerationLabelsResponse;
import software.amazon.awssdk.services.rekognition.model.Image;
import software.amazon.awssdk.services.rekognition.model.ModerationLabel;
import software.amazon.awssdk.services.rekognition.model.S3Object;

@Component
@RequiredArgsConstructor
public class RekognitionProfileImageModerator implements ProfileImageModerator {

    private static final int TOP_LEVEL_CATEGORY = 1;

    private final RekognitionClient rekognitionClient;
    private final RekognitionProperties rekognitionProperties;
    private final S3Properties s3Properties;

    @Override
    public ModerationVerdict moderate(final String key) {
        final DetectModerationLabelsResponse response = rekognitionClient.detectModerationLabels(buildRequest(key));
        final List<String> matched = findBlockedTopLevelNames(response);
        if (matched.isEmpty()) {
            return ModerationVerdict.acceptable();
        }
        return ModerationVerdict.reject(matched);
    }

    private DetectModerationLabelsRequest buildRequest(final String key) {
        final S3Object s3Object = S3Object.builder()
                .bucket(s3Properties.bucket())
                .name(key)
                .build();
        final Image image = Image.builder()
                .s3Object(s3Object)
                .build();
        return DetectModerationLabelsRequest.builder()
                .image(image)
                .minConfidence(rekognitionProperties.minConfidence())
                .build();
    }

    private List<String> findBlockedTopLevelNames(final DetectModerationLabelsResponse response) {
        return response.moderationLabels().stream()
                .filter(this::isTopLevel)
                .map(ModerationLabel::name)
                .filter(rekognitionProperties.blockedCategories()::contains)
                .toList();
    }

    private boolean isTopLevel(final ModerationLabel label) {
        final Integer level = label.taxonomyLevel();
        return level != null && level == TOP_LEVEL_CATEGORY;
    }
}
