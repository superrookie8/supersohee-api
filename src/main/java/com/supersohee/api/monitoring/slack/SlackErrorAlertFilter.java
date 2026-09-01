package com.supersohee.api.monitoring.slack;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SlackErrorAlertFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(SlackErrorAlertFilter.class);

    private final SlackErrorAlertService alertService;

    public SlackErrorAlertFilter(SlackErrorAlertService alertService) {
        this.alertService = alertService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        boolean reported = false;
        try {
            filterChain.doFilter(request, response);
        } catch (RuntimeException | Error | ServletException | IOException failure) {
            reported = true;
            reportSafely(request, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, failure);
            throw failure;
        } finally {
            if (!reported && response.getStatus() >= HttpServletResponse.SC_INTERNAL_SERVER_ERROR) {
                reportSafely(request, response.getStatus(), null);
            }
        }
    }

    private void reportSafely(HttpServletRequest request, int status, Throwable failure) {
        try {
            alertService.report(request, status, failure);
        } catch (RuntimeException alertFailure) {
            log.warn("Slack error alert reporting failed ({})", alertFailure.getClass().getSimpleName());
        }
    }
}
