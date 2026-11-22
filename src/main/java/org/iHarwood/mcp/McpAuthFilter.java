package org.iHarwood.mcp;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Optional bearer-token guard for the MCP endpoint. Registered only when
 * {@code app.mcp.api-key} (env {@code MCP_API_KEY}) is set; see {@link McpConfig}.
 */
class McpAuthFilter extends OncePerRequestFilter {

    private final byte[] expected;

    McpAuthFilter(String apiKey) {
        this.expected = ("Bearer " + apiKey).getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        byte[] actual = header == null ? new byte[0] : header.getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(expected, actual)) {
            response.setHeader("WWW-Authenticate", "Bearer");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing or invalid MCP API key");
            return;
        }
        chain.doFilter(request, response);
    }
}
