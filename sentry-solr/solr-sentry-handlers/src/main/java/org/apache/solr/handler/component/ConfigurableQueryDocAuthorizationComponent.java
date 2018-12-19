package org.apache.solr.handler.component;

import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ConfigurableQueryDocAuthorizationComponent extends QueryAuthorizationComponentBase {

  private final List<QueryAuthContextInitializer> contextInitializers = new ArrayList<>();
  private final List<QueryAuthParamsPreparator> paramsPreparators = new ArrayList<>();

//  private final Map<String, BiConsumer<String, Object>> configParsers = new HashMap<>();

  protected void registerContextInitializer(QueryAuthContextInitializer initializer) {
    contextInitializers.add(initializer);
  }

  protected void registerParamsPreparator(QueryAuthParamsPreparator preparator) {
    paramsPreparators.add(preparator);
  }

//  {
//    configParsers.put("fq", (name, value) -> {  // <str name="fq">owner:${userName}</str>
//      registerParamsPreparator((rb, context) -> {
//
//      });
//    });
//
//    configParsers.put("expand", (name, value) -> { // <str name="expand">{"over":"${roles -> r}", "name":"fq", "value":"sentry_role:${r}"}</str>
//      registerParamsPreparator((rb, context) -> {
//
//      });
//    });
//
//  }
//
//  @Override
//  public void init(NamedList args) {
//    super.init(args);
//
//    BiConsumer<String, ?> parseArg = (name, value) -> {
//      BiConsumer<String, Object> parser = configParsers.get(name);
//      if (parser == null){
//        throw new SentryConfigurationException("Unsupported config parser: " + name);
//      }
//      parser.accept(name, value);
//    };
//
//    args.forEach(parseArg);
//  }

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
