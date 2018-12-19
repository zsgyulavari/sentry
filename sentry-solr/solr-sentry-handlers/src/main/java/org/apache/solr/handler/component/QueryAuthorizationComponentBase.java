package org.apache.solr.handler.component;

import org.apache.solr.handler.component.QueryAuthorizationContext.CachingSupplier;
import org.apache.sentry.binding.solr.authz.SentrySolrPluginImpl;
import org.apache.sentry.core.common.exception.SentryUserException;
import org.apache.solr.common.SolrException;
import org.apache.solr.core.SolrCore;
import org.apache.solr.request.LocalSolrQueryRequest;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.security.AuthorizationPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Set;

public abstract class QueryAuthorizationComponentBase extends SearchComponent {

  protected final Logger LOG = LoggerFactory.getLogger(this.getClass());

  public static final String ENABLED_PROP = "enabled";

  protected static final String superUser = System.getProperty("solr.authorization.superuser", "solr");
  private boolean enabled;

  @Override
  public void init(NamedList args) {
    SolrParams params = args.toSolrParams();
    this.enabled = params.getBool(ENABLED_PROP, false);
    LOG.info("QueryDocAuthorizationComponent enabled: " + this.enabled);
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void prepare(ResponseBuilder rb) throws IOException {
    if (!isEnabled()) {
      return;
    }

    String userName = getUserName(rb.req);
    if (superUser.equals(userName)) {
      return;
    }

    QueryAuthorizationContext context = new QueryAuthorizationContext(userName, new CachingSupplier<>(() -> getRoles(rb.req, userName)));
    initContext(rb, context);
    prepare(rb, context);
  }

  protected void initContext(ResponseBuilder rb, QueryAuthorizationContext context) throws IOException {
  }

  protected void prepare(ResponseBuilder rb, QueryAuthorizationContext context) throws IOException {
    SolrParams newParams = prepareParams(rb.req.getParams(), context);
    rb.req.setParams(newParams);
  }

  protected SolrParams prepareParams(SolrParams params, QueryAuthorizationContext context) throws IOException {
    return params;
  }

  @Override
  public void process(ResponseBuilder rb) throws IOException {
  }

  protected boolean isLocalRequest(SolrQueryRequest req) {
    return req instanceof LocalSolrQueryRequest;
  }

  protected String getUserName(SolrQueryRequest req) {
    // If a local request, treat it like a super user request; i.e. it is equivalent to an
    // http request from the same process.
    if (isLocalRequest(req)) {
      return superUser;
    }

    SolrCore solrCore = req.getCore();

    HttpServletRequest httpServletRequest = (HttpServletRequest) req.getContext().get("httpRequest");
    if (httpServletRequest == null) {
      StringBuilder builder = new StringBuilder("Unable to locate HttpServletRequest");
      if (solrCore != null && solrCore.getSolrConfig().getBool(
          "requestDispatcher/requestParsers/@addHttpRequestToContext", true) == false) {
        builder.append(", ensure requestDispatcher/requestParsers/@addHttpRequestToContext is set to true in solrconfig.xml");
      }
      throw new SolrException(SolrException.ErrorCode.UNAUTHORIZED, builder.toString());
    }

    String userName = httpServletRequest.getRemoteUser();
    if (userName == null) {
      userName = SentrySolrPluginImpl.getShortUserName(httpServletRequest.getUserPrincipal());
    }
    if (userName == null) {
      throw new SolrException(SolrException.ErrorCode.UNAUTHORIZED, "This request is not authenticated.");
    }

    return userName;
  }

  /**
   * This method returns the roles associated with the specified <code>userName</code>
   */
  protected Set<String> getRoles(SolrQueryRequest req, String userName) {
    SolrCore solrCore = req.getCore();

    AuthorizationPlugin plugin = solrCore.getCoreContainer().getAuthorizationPlugin();
    if (!(plugin instanceof SentrySolrPluginImpl)) {
      throw new SolrException(SolrException.ErrorCode.UNAUTHORIZED, getClass().getSimpleName() +
          " can only be used with Sentry authorization plugin for Solr");
    }
    try {
      return ((SentrySolrPluginImpl) plugin).getRoles(userName);
    } catch (SentryUserException e) {
      throw new SolrException(SolrException.ErrorCode.UNAUTHORIZED,
          "Request from user: " + userName +
              " rejected due to SentryUserException: ", e);
    }
  }
}
