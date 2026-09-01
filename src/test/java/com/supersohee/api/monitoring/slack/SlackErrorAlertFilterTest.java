package com.supersohee.api.monitoring.slack;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.HandlerMapping;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class SlackErrorAlertFilterTest {

    @Test
    void reportsFinalFiveHundredResponseOnce() throws ServletException, IOException {
        SlackErrorAlertService service = mock(SlackErrorAlertService.class);
        SlackErrorAlertFilter filter = new SlackErrorAlertFilter(service);
        MockHttpServletRequest request = request();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, (ignoredRequest, handledResponse) ->
                ((MockHttpServletResponse) handledResponse).setStatus(502));

        verify(service, times(1)).report(request, 502, null);
    }

    @Test
    void reportsPropagatedExceptionOnceAndPreservesIt() {
        SlackErrorAlertService service = mock(SlackErrorAlertService.class);
        SlackErrorAlertFilter filter = new SlackErrorAlertFilter(service);
        MockHttpServletRequest request = request();
        MockHttpServletResponse response = new MockHttpServletResponse();
        IllegalStateException original = new IllegalStateException("original");

        assertThatThrownBy(() -> filter.doFilterInternal(request, response, (ignoredRequest, ignoredResponse) -> {
            throw original;
        })).isSameAs(original);

        verify(service, times(1)).report(request, 500, original);
    }

    @Test
    void ignoresSuccessfulAndFourHundredResponses() throws ServletException, IOException {
        SlackErrorAlertService service = mock(SlackErrorAlertService.class);
        SlackErrorAlertFilter filter = new SlackErrorAlertFilter(service);

        filter.doFilterInternal(request(), new MockHttpServletResponse(), (ignoredRequest, ignoredResponse) -> {
        });
        MockHttpServletRequest badRequest = request();
        MockHttpServletResponse badResponse = new MockHttpServletResponse();
        filter.doFilterInternal(badRequest, badResponse, (ignoredRequest, handledResponse) ->
                ((MockHttpServletResponse) handledResponse).setStatus(400));

        verifyNoInteractions(service);
    }

    private MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test/secret-value");
        request.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/api/test/{id}");
        return request;
    }
}
