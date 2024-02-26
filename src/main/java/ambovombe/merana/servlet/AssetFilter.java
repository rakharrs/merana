package ambovombe.merana.servlet;

import java.io.IOException;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;

public class AssetFilter implements Filter{
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // Initialization logic, if needed
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        // Get the requested URL
        String url = ((HttpServletRequest) request).getRequestURI();

        // Check if the requested URL ends with a specific file extension or path
        if (url.endsWith(".css") || url.startsWith("/assets/")) {
            // Skip the servlet and continue the filter chain
            chain.doFilter(request, response);
        } else {
            // Process the request by the servlet
            request.getRequestDispatcher("/").forward(request, response);
        }
    }

    @Override
    public void destroy() {
        // Cleanup logic, if needed
    }
}
