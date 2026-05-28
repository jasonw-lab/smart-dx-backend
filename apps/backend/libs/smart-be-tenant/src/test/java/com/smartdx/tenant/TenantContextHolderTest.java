package com.smartdx.tenant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TenantContextHolder Unit Tests
 *
 * @author jason.w
 */
@DisplayName("TenantContextHolder Unit Tests")
class TenantContextHolderTest {

    @BeforeEach
    void setUp() {
        TenantContextHolder.clear();
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Nested
    @DisplayName("TenantId Management")
    class TenantIdManagement {

        @Test
        @DisplayName("setTenantId stores tenant ID in thread local")
        void setTenantId_storesValue() {
            Long tenantId = 123L;

            TenantContextHolder.setTenantId(tenantId);

            assertThat(TenantContextHolder.getTenantId()).isEqualTo(tenantId);
        }

        @Test
        @DisplayName("getTenantId returns null when not set")
        void getTenantId_returnsNull_whenNotSet() {
            assertThat(TenantContextHolder.getTenantId()).isNull();
        }

        @Test
        @DisplayName("setTenantId with null does not set value")
        void setTenantId_withNull_doesNotSetValue() {
            TenantContextHolder.setTenantId(1L);
            TenantContextHolder.setTenantId(null);

            // Should still be 1L because null is ignored
            assertThat(TenantContextHolder.getTenantId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("setTenantId overwrites previous value")
        void setTenantId_overwritesPreviousValue() {
            TenantContextHolder.setTenantId(1L);
            TenantContextHolder.setTenantId(2L);

            assertThat(TenantContextHolder.getTenantId()).isEqualTo(2L);
        }

        @Test
        @DisplayName("clear removes tenant ID")
        void clear_removesTenantId() {
            TenantContextHolder.setTenantId(123L);

            TenantContextHolder.clear();

            assertThat(TenantContextHolder.getTenantId()).isNull();
        }
    }

    @Nested
    @DisplayName("IgnoreTenant Flag Management")
    class IgnoreTenantManagement {

        @Test
        @DisplayName("isIgnoreTenant returns false by default")
        void isIgnoreTenant_returnsFalse_byDefault() {
            assertThat(TenantContextHolder.isIgnoreTenant()).isFalse();
        }

        @Test
        @DisplayName("setIgnoreTenant(true) sets ignore flag")
        void setIgnoreTenant_true_setsFlag() {
            TenantContextHolder.setIgnoreTenant(true);

            assertThat(TenantContextHolder.isIgnoreTenant()).isTrue();
        }

        @Test
        @DisplayName("setIgnoreTenant(false) clears ignore flag")
        void setIgnoreTenant_false_clearsFlag() {
            TenantContextHolder.setIgnoreTenant(true);
            TenantContextHolder.setIgnoreTenant(false);

            assertThat(TenantContextHolder.isIgnoreTenant()).isFalse();
        }

        @Test
        @DisplayName("clear removes ignore flag")
        void clear_removesIgnoreFlag() {
            TenantContextHolder.setIgnoreTenant(true);

            TenantContextHolder.clear();

            assertThat(TenantContextHolder.isIgnoreTenant()).isFalse();
        }
    }

    @Nested
    @DisplayName("Thread Isolation")
    class ThreadIsolation {

        @Test
        @DisplayName("tenant ID is isolated per thread")
        void tenantId_isIsolatedPerThread() throws InterruptedException {
            Long mainThreadTenantId = 1L;
            Long childThreadTenantId = 2L;
            AtomicReference<Long> childThreadObserved = new AtomicReference<>();
            CountDownLatch latch = new CountDownLatch(1);

            TenantContextHolder.setTenantId(mainThreadTenantId);

            Thread childThread = new Thread(() -> {
                TenantContextHolder.setTenantId(childThreadTenantId);
                childThreadObserved.set(TenantContextHolder.getTenantId());
                latch.countDown();
            });
            childThread.start();
            latch.await();

            // Main thread should still have its own tenant ID
            assertThat(TenantContextHolder.getTenantId()).isEqualTo(mainThreadTenantId);
            // Child thread should have its own tenant ID
            assertThat(childThreadObserved.get()).isEqualTo(childThreadTenantId);
        }

        @Test
        @DisplayName("clear in one thread does not affect other threads")
        void clear_inOneThread_doesNotAffectOtherThreads() throws InterruptedException {
            Long mainThreadTenantId = 1L;
            AtomicReference<Long> childThreadObserved = new AtomicReference<>();
            CountDownLatch setLatch = new CountDownLatch(1);
            CountDownLatch clearLatch = new CountDownLatch(1);
            CountDownLatch readLatch = new CountDownLatch(1);

            TenantContextHolder.setTenantId(mainThreadTenantId);

            Thread childThread = new Thread(() -> {
                TenantContextHolder.setTenantId(2L);
                setLatch.countDown();
                try {
                    clearLatch.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                childThreadObserved.set(TenantContextHolder.getTenantId());
                readLatch.countDown();
            });
            childThread.start();

            setLatch.await();
            TenantContextHolder.clear();
            clearLatch.countDown();
            readLatch.await();

            // Main thread should be cleared
            assertThat(TenantContextHolder.getTenantId()).isNull();
            // Child thread should still have its value
            assertThat(childThreadObserved.get()).isEqualTo(2L);
        }

        @Test
        @DisplayName("concurrent access from multiple threads is safe")
        void concurrentAccess_isSafe() throws InterruptedException {
            int threadCount = 10;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(threadCount);
            AtomicReference<AssertionError> error = new AtomicReference<>();

            for (int i = 0; i < threadCount; i++) {
                final long tenantId = i + 1;
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        TenantContextHolder.setTenantId(tenantId);

                        // Simulate some work
                        Thread.sleep(10);

                        // Verify thread still has its own tenant ID
                        Long observed = TenantContextHolder.getTenantId();
                        if (observed == null || tenantId != observed.longValue()) {
                            error.set(new AssertionError(
                                    "Expected " + tenantId + " but got " + observed));
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        TenantContextHolder.clear();
                        doneLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            doneLatch.await();
            executor.shutdown();

            if (error.get() != null) {
                throw error.get();
            }
        }
    }
}
