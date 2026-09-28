package com.supersohee.api.monitoring.slack;

import com.supersohee.api.admin.error.AdminApiExceptionHandler;
import com.supersohee.api.schedule.controller.ScheduleController;
import com.supersohee.api.schedule.service.ScheduleService;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.HandlerMapping;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
    void reportsExceptionResolvedIntoFiveHundredResponse() throws ServletException, IOException {
        SlackErrorAlertService service = mock(SlackErrorAlertService.class);
        SlackErrorAlertFilter filter = new SlackErrorAlertFilter(service);
        MockHttpServletRequest request = request();
        MockHttpServletResponse response = new MockHttpServletResponse();
        IllegalStateException resolved = new IllegalStateException("resolved");

        filter.doFilterInternal(request, response, (handledRequest, handledResponse) -> {
            handledRequest.setAttribute(DispatcherServlet.EXCEPTION_ATTRIBUTE, resolved);
            ((MockHttpServletResponse) handledResponse).setStatus(500);
        });

        verify(service, times(1)).report(request, 500, resolved);
    }

    @Test
    void reportsExceptionHandledByAdviceForPublicScheduleList() throws Exception {
        SlackErrorAlertService alertService = mock(SlackErrorAlertService.class);
        ScheduleService scheduleService = mock(ScheduleService.class);
        IllegalStateException failure = new IllegalStateException("database unavailable");
        when(scheduleService.findActiveSchedules(null, null)).thenThrow(failure);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new ScheduleController(scheduleService))
                .setControllerAdvice(new AdminApiExceptionHandler())
                .addFilters(new SlackErrorAlertFilter(alertService))
                .build();

        mockMvc.perform(get("/api/schedules")).andExpect(status().isInternalServerError());

        verify(alertService, times(1)).report(any(), eq(500), same(failure));
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
