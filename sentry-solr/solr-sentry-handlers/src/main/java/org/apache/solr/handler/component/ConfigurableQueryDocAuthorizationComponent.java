package org.apache.solr.handler.component;

import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ConfigurableQueryDocAuthorizationComponent extends QueryAuthorizationComponentBase {

  private final List<QueryAuthContextInitializer> contextInitializers = new ArrayList<>();
  private final List<QueryAuthParamsPreparator> paramsPreparators = new ArrayList<>();

  @Override
  public void init(NamedList args) {
    super.init(args);
    // SolrParams params = args.toSolrParams();
  }

  @Override
  protected void initContext(ResponseBuilder rb, QueryAuthorizationContext context) throws IOException {
    for (QueryAuthContextInitializer initializer : contextInitializers) {
      initializer.initContext(rb, context);
    }
  }

  @Override
  protected SolrParams prepareParams(SolrParams params, QueryAuthorizationContext context) throws IOException {
    ModifiableSolrParams newParams = new ModifiableSolrParams(params);
    for (QueryAuthParamsPreparator preparator : paramsPreparators) {
      preparator.prepareParams(params, context);
    }
    return newParams;
  }

  @Override
  protected void prepare(ResponseBuilder rb, QueryAuthorizationContext context) throws IOException {
    ModifiableSolrParams newParams = new ModifiableSolrParams(rb.req.getParams());
    prepareParams(newParams, context);
    rb.req.setParams(newParams);
  }


  @Override
  public String getDescription() {
    return null;
  }

  public interface QueryAuthContextInitializer {
    void initContext(ResponseBuilder rb, QueryAuthorizationContext context) throws IOException;
  }

  public interface QueryAuthParamsPreparator {
    void prepareParams(SolrParams params, QueryAuthorizationContext context) throws IOException;
  }

}
