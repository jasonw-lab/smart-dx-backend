package com.smartdx.tenant.filter;

import com.smartdx.tenant.TenantContextHolder;
import com.smartdx.tenant.TenantProperties;
import com.smartdx.tenant.resolver.TenantResolver;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * TenantContextFilter Unit Tests
 *
 * @author jason.w
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TenantContextFilter Unit Tests")
class TenantContextFilterTest {

    @Mock
    private FilterChain filterChain;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private TenantProperties properties;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        properties = new TenantProperties();
        TenantContextHolder.clear();
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Nested
    @DisplayName("Basic Filter Behavior")
    class BasicFilterBehavior {

        @Test
        @DisplayName("filter chain is always invoked")
        void filterChain_isAlwaysInvoked() throws ServletException, IOException {
            TenantContextFilter filter = new TenantContextFilter(Collections.emptyList(), properties);

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain, times(1)).doFilter(request, response);
        }

        @Test
        @DisplayName("context is cleared after filter execution")
        void context_isClearedAfterFilterExecution() throws ServletException, IOException {
            TenantResolver resolver = req -> 123L;
            TenantContextFilter filter = new TenantContextFilter(List.of(resolver), properties);

            filter.doFilterInternal(request, response, filterChain);

            assertThat(TenantContextHolder.getTenantId()).isNull();
        }

        @Test
        @DisplayName("context is cleared even when exception occurs")
        void context_isClearedEvenWhenExceptionOccurs() throws ServletException, IOException {
            TenantResolver resolver = req -> 123L;
            TenantContextFilter filter = new TenantContextFilter(List.of(resolver), properties);
            doThrow(new RuntimeException("Test exception")).when(filterChain).doFilter(any(), any());

            try {
                filter.doFilterInternal(request, response, filterChain);
            } catch (RuntimeException e) {
                // Expected
            }

            assertThat(TenantContextHolder.getTenantId()).isNull();
        }
    }

    @Nested
    @DisplayName("Tenant Resolution")
    class TenantResolution {

        @Test
        @DisplayName("sets tenant ID from resolver")
        void setsTenantId_fromResolver() throws ServletException, IOException {
            Long expectedTenantId = 42L;
            TenantResolver resolver = req -> expectedTenantId;
            TenantContextFilter filter = new TenantContextFilter(List.of(resolver), properties);
            AtomicLong observedTenantId = new AtomicLong();

            doAnswer(invocation -> {
                observedTenantId.set(TenantContextHolder.getTenantId());
                return null;
            }).when(filterChain).doFilter(any(), any());

            filter.doFilterInternal(request, response, filterChain);

            assertThat(observedTenantId.get()).isEqualTo(expectedTenantId);
        }

        @Test
        @DisplayName("uses first resolver that returns value")
        void usesFirstResolver_thatReturnsValue() throws ServletException, IOException {
            TenantResolver resolver1 = req -> null;
            TenantResolver resolver2 = req -> 100L;
            TenantResolver resolver3 = req -> 200L;
            TenantContextFilter filter = new TenantContextFilter(
                    Arrays.asList(resolver1, resolver2, resolver3), properties);
            AtomicLong observedTenantId = new AtomicLong();

            doAnswer(invocation -> {
                observedTenantId.set(TenantContextHolder.getTenantId());
                return null;
            }).when(filterChain).doFilter(any(), any());

            filter.doFilterInternal(request, response, filterChain);

            assertThat(observedTenantId.get()).isEqualTo(100L);
        }

        @Test
        @DisplayName("resolvers are sorted by order")
        void resolvers_areSortedByOrder() throws ServletException, IOException {
            TenantResolver lowPriority = new TenantResolver() {
                @Override
                public Long resolve(jakarta.servlet.http.HttpServletRequest request) {
                    return 100L;
                }

                @Override
                public int getOrder() {
                    return 200;
                }
            };
            TenantResolver highPriority = new TenantResolver() {
                @Override
                public Long resolve(jakarta.servlet.http.HttpServletRequest request) {
                    return 50L;
                }

                @Override
                public int getOrder() {
                    return 10;
                }
            };
            TenantContextFilter filter = new TenantContextFilter(
                    Arrays.asList(lowPriority, highPriority), properties);
            AtomicLong observedTenantId = new AtomicLong();

            doAnswer(invocation -> {
                observedTenantId.set(TenantContextHolder.getTenantId());
                return null;
            }).when(filterChain).doFilter(any(), any());

            filter.doFilterInternal(request, response, filterChain);

            assertThat(observedTenantId.get()).isEqualTo(50L);
        }

        @Test
        @DisplayName("falls back to default tenant ID when no resolver matches")
        void fallsBackToDefault_whenNoResolverMatches() throws ServletException, IOException {
            Long defaultTenantId = 999L;
            properties.setDefaultTenantId(defaultTenantId);
            TenantResolver resolver = req -> null;
            TenantContextFilter filter = new TenantContextFilter(List.of(resolver), properties);
            AtomicLong observedTenantId = new AtomicLong();

            doAnswer(invocation -> {
                observedTenantId.set(TenantContextHolder.getTenantId());
                return null;
            }).when(filterChain).doFilter(any(), any());

            filter.doFilterInternal(request, response, filterChain);

            assertThat(observedTenantId.get()).isEqualTo(defaultTenantId);
        }

        @Test
        @DisplayName("does not set tenant ID when no resolver and no default")
        void doesNotSetTenantId_whenNoResolverAndNoDefault() throws ServletException, IOException {
            properties.setDefaultTenantId(null);
            TenantContextFilter filter = new TenantContextFilter(Collections.emptyList(), properties);
            AtomicLong observedTenantId = new AtomicLong(-1);

            doAnswer(invocation -> {
                Long tenantId = TenantContextHolder.getTenantId();
                observedTenantId.set(tenantId != null ? tenantId : -1);
                return null;
            }).when(filterChain).doFilter(any(), any());

            filter.doFilterInternal(request, response, filterChain);

            assertThat(observedTenantId.get()).isEqualTo(-1);
        }
    }

    @Nested
    @DisplayName("Force Default Mode")
    class ForceDefaultMode {

        @Test
        @DisplayName("uses default tenant ID when forceDefault is true")
        void usesDefaultTenantId_whenForceDefaultIsTrue() throws ServletException, IOException {
            Long defaultTenantId = 1L;
            properties.setForceDefault(true);
            properties.setDefaultTenantId(defaultTenantId);

            TenantResolver resolver = req -> 999L; // Should be ignored
            TenantContextFilter filter = new TenantContextFilter(List.of(resolver), properties);
            AtomicLong observedTenantId = new AtomicLong();

            doAnswer(invocation -> {
                observedTenantId.set(TenantContextHolder.getTenantId());
                return null;
            }).when(filterChain).doFilter(any(), any());

            filter.doFilterInternal(request, response, filterChain);

            assertThat(observedTenantId.get()).isEqualTo(defaultTenantId);
        }

        @Test
        @DisplayName("ignores resolvers when forceDefault is true")
        void ignoresResolvers_whenForceDefaultIsTrue() throws ServletException, IOException {
            properties.setForceDefault(true);
            properties.setDefaultTenantId(1L);

            TenantResolver resolver = mock(TenantResolver.class);
            TenantContextFilter filter = new TenantContextFilter(List.of(resolver), properties);

            filter.doFilterInternal(request, response, filterChain);

            verify(resolver, never()).resolve(any());
        }
    }

    @Nested
    @DisplayName("Null Handling")
    class NullHandling {

        @Test
        @DisplayName("handles null resolver list gracefully")
        void handlesNullResolverList_gracefully() throws ServletException, IOException {
            TenantContextFilter filter = new TenantContextFilter(null, properties);

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain, times(1)).doFilter(request, response);
        }

        @Test
        @DisplayName("handles empty resolver list gracefully")
        void handlesEmptyResolverList_gracefully() throws ServletException, IOException {
            TenantContextFilter filter = new TenantContextFilter(Collections.emptyList(), properties);

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain, times(1)).doFilter(request, response);
        }
    }
}
