package com.googlesource.gerrit.plugins.redirect;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;

public class RedirectChangeRequestFilter implements Filter {
  static final String ECLIPSE_PROJECT_HOST = "git.eclipse.org";
  static final String GERRIT_HUB_URL = "https://review.gerrithub.io"; // TODO injected ?
  private final Map<Integer, String> changesProjectKeyValueStore;

  RedirectChangeRequestFilter(Map<Integer, String> changesProjectKeyValueStore) {
    this.changesProjectKeyValueStore = changesProjectKeyValueStore;
  }

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    // Initialization code goes here, if needed
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    HttpServletRequest httpRequest = (HttpServletRequest) request;
    HttpServletResponse httpResponse = (HttpServletResponse) response;
    Optional<String> maybeEclipseProjectHost =
        Optional
            .ofNullable(httpRequest.getHeader("Referer"))
            .filter(referer -> referer.equals(ECLIPSE_PROJECT_HOST));
    if (maybeEclipseProjectHost.isPresent()) {
      String requestURI = httpRequest.getRequestURI();
      Optional<Integer> maybeChangeNumber = extractChangeNumberFromURI(requestURI);
      if (maybeChangeNumber.isPresent()) {
        Optional<String> maybeProject = findProjectByChangeNumber(maybeChangeNumber.get());
        if (maybeProject.isPresent()) {
          httpResponse.sendRedirect(buildRedirectURL(maybeProject.get(), maybeChangeNumber.get()));
        } else {
          httpResponse.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
      } else {
        httpResponse.sendError(HttpServletResponse.SC_BAD_REQUEST);
      }
    } else {
      chain.doFilter(request, response);
    }
  }

  @Override
  public void destroy() {
    // Cleanup code goes here, if needed
  }

  private Optional<Integer> extractChangeNumberFromURI(String url) { // TODO MAYBE USE REGULAR EXPRESSION
    try {
      return Optional.of(Integer.parseInt(url));
    } catch (NumberFormatException e) {
      return Optional.empty();
    }
  }

  private Optional<String> findProjectByChangeNumber(int changeNumber) {
    return Optional.ofNullable(changesProjectKeyValueStore.get(changeNumber));
  }

  private String buildRedirectURL(String project, int change) {
    return GERRIT_HUB_URL + "/" + project + "+" + change;
  }
}

