package org.apache.solr.handler.component;

import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;

import java.util.Set;
import java.util.function.Supplier;

public class ConfigurableQueryDocAuthorizationComponent extends QueryAuthorizationComponentBase {

  public static final String AUTH_FIELD_PROP = "sentryAuthField";
  public static final String DEFAULT_AUTH_FIELD = "sentry_auth";
  public static final String ALL_ROLES_TOKEN_PROP = "allRolesToken";
  private String authField;
  private String allRolesToken;

  @Override
  public void init(NamedList args) {
    super.init(args);
    SolrParams params = args.toSolrParams();
    this.authField = params.get(AUTH_FIELD_PROP, DEFAULT_AUTH_FIELD);
    LOG.info("QueryDocAuthorizationComponent authField: " + this.authField);
    this.allRolesToken = params.get(ALL_ROLES_TOKEN_PROP, "");
    LOG.info("QueryDocAuthorizationComponent allRolesToken: " + this.allRolesToken);
  }

  protected SolrParams prepareParams(SolrParams params, String userName, Supplier<Set<String>> rolesSupplier) {
    ModifiableSolrParams newParams = new ModifiableSolrParams(params);
    // TODO
    return newParams;
  }

  @Override
  protected void prepare(ResponseBuilder rb, String userName, Supplier<Set<String>> rolesSupplier) {
      ModifiableSolrParams newParams = new ModifiableSolrParams(rb.req.getParams());
      prepareParams(newParams, userName, rolesSupplier);
      rb.req.setParams(newParams);
  }


  @Override
  public String getDescription() {
    return null;
  }

}
