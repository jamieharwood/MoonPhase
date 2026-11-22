package org.iHarwood.mcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * Protects the MCP endpoint with a bearer token when {@code app.mcp.api-key} is set.
 * Without a key the endpoint is open, matching the rest of the (unauthenticated) dashboard API.
 */
@Configuration
@ConditionalOnWebApplication
public class McpConfig {

    private static final Logger logger = LoggerFactory.getLogger(McpConfig.class);

    @Bean
    public FilterRegistrationBean<McpAuthFilter> mcpAuthFilter(
            @Value("${app.mcp.api-key:}") String apiKey,
            @Value("${spring.ai.mcp.server.streamable-http.mcp-endpoint:/mcp}") String endpoint) {
        FilterRegistrationBean<McpAuthFilter> reg = new FilterRegistrationBean<>(new McpAuthFilter(apiKey));
        reg.addUrlPatterns(endpoint, endpoint + "/*");
        reg.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        boolean enabled = !apiKey.isBlank();
        reg.setEnabled(enabled);
        if (enabled) {
            logger.info("MCP endpoint {} requires a bearer token (MCP_API_KEY)", endpoint);
        } else {
            logger.warn("MCP endpoint {} is unauthenticated — set MCP_API_KEY to require a bearer token", endpoint);
        }
        return reg;
    }
}
