package com.lingring.global.auth.resolver;

import com.lingring.global.auth.annotation.AuthUser;
import com.lingring.global.auth.context.AuthContext;
import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.UnauthorizedException;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
public class AuthUserArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(final MethodParameter parameter) {
        return parameter.hasParameterAnnotation(AuthUser.class)
                && parameter.getParameterType().equals(Long.class);
    }

    @Override
    public Object resolveArgument(
            final MethodParameter parameter,
            final ModelAndViewContainer mavContainer,
            final NativeWebRequest webRequest,
            final WebDataBinderFactory binderFactory
    ) {
        final Long userId = AuthContext.get();
        if (userId == null) {
            throw new UnauthorizedException(
                    ErrorCode.INVALID_TOKEN,
                    "인증된 사용자 정보가 없습니다."
            );
        }
        return userId;
    }
}
