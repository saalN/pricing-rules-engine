package com.priceforge.app.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.ApplicationContext;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import com.redis.testcontainers.RedisContainer;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * The Auditor's segregation-of-duties guarantee (funcionalidad.md, "Roles y Permisos"): no write
 * path anywhere in the system accepts the AUDITOR role. Rather than hand-listing the write use
 * cases — which would silently miss the next one — this walks every application service in every
 * context and requires each either to deny the AUDITOR or to be on the explicit read-only /
 * internal list below, so adding a service forces a deliberate decision here.
 */
@Testcontainers
@SpringBootTest
@WithMockUser(roles = "AUDITOR")
class AuditorIsReadOnlyEverywhereTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static final RedisContainer REDIS = new RedisContainer(RedisContainer.DEFAULT_IMAGE_NAME);

    static {
        REDIS.start();
    }

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }

    /** Use cases that never change state in any context, so the Auditor may (or must be able to) call them. */
    private static final Set<String> READ_ONLY = Set.of(
        "GetProductService",
        "FindCheapestActiveBasePriceService",
        "CalculateWorstCasePricingDiscountService",
        "ReplayPriceCalculationService",
        "QueryPriceCalculationHistoryService",
        "QueryEntityChangeHistoryService",
        "ReplayStoredPriceCalculationService",
        "ExportAuditTrailService");

    /** Driven only by the outbox relay or the scheduler, never by an HTTP caller. */
    private static final Set<String> INTERNAL = Set.of(
        "RecordAuditEntryService",
        "ActivateAndExpirePromotionsService",
        // A port implementation, not a use case: only reachable through CalculatePriceService,
        // which the other test case proves denies the AUDITOR.
        "PromotionApplierService");

    @Autowired
    private ApplicationContext context;

    @Autowired
    private RequestMappingHandlerMapping handlerMapping;

    @Test
    void everyWriteUseCaseDeniesTheAuditor() throws Exception {
        List<String> checked = new ArrayList<>();

        for (Object bean : context.getBeansOfType(Object.class).values()) {
            Class<?> type = AopUtils.getTargetClass(bean);
            if (!isApplicationService(type) || READ_ONLY.contains(type.getSimpleName())
                    || INTERNAL.contains(type.getSimpleName())) {
                continue;
            }

            Method execute = executeMethod(type);
            Object[] nullArguments = new Object[execute.getParameterCount()];
            assertThatThrownBy(() -> execute.invoke(bean, nullArguments))
                .as("%s must deny the AUDITOR role", type.getSimpleName())
                .hasRootCauseInstanceOf(AccessDeniedException.class);
            checked.add(type.getSimpleName());
        }

        // Guards against the scan silently finding nothing: these are known writing use cases.
        assertThat(checked).contains(
            "CreateProductService", "UpdateProductBasePriceService", "CreatePricingRuleService",
            "CalculatePriceService", "CreatePromotionService");
    }

    @Test
    void theAuditContextExposesNoWriteEndpoint() {
        Map<RequestMappingInfo, HandlerMethod> mappings = handlerMapping.getHandlerMethods();

        List<String> auditEndpoints = mappings.entrySet().stream()
            .filter(entry -> entry.getValue().getBeanType().getPackageName().startsWith("com.priceforge.audit"))
            .flatMap(entry -> entry.getKey().getMethodsCondition().getMethods().stream())
            .map(RequestMethod::name)
            .toList();

        assertThat(auditEndpoints).isNotEmpty().containsOnly("GET");
    }

    private static boolean isApplicationService(Class<?> type) {
        return type.getName().matches("com\\.priceforge\\.\\w+\\.application\\.service\\.[A-Za-z]+Service")
            && type.getSimpleName().endsWith("Service");
    }

    private static Method executeMethod(Class<?> type) {
        for (Method method : type.getDeclaredMethods()) {
            if (method.getName().equals("execute")) {
                return method;
            }
        }
        throw new IllegalStateException(type.getSimpleName() + " has no execute method — classify it in this test");
    }
}
