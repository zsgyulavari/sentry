/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.solr.handler.component;


import org.apache.solr.common.SolrException;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;

import java.util.Set;
import java.util.function.Supplier;

public class QueryDocAuthorizationComponent extends QueryAuthorizationComponentBase {
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

  private void addRawClause(StringBuilder builder, String authField, String value) {
    // requires a space before the first term, so the
    // default lucene query parser will be used
    builder.append(" {!raw f=").append(authField).append(" v=")
        .append(value).append("}");
  }

  private String getFilterQueryStr(Set<String> roles) {
    if (roles != null && roles.size() > 0) {
      StringBuilder builder = new StringBuilder();
      for (String role : roles) {
        addRawClause(builder, authField, role);
      }
      if (allRolesToken != null && !allRolesToken.isEmpty()) {
        addRawClause(builder, authField, allRolesToken);
      }
      return builder.toString();
    }
    return null;
  }

  @Override
  protected void prepare(ResponseBuilder rb, String userName, Supplier<Set<String>> rolesSupplier) {
    Set<String> roles = rolesSupplier.get();
    if (roles != null && !roles.isEmpty()) {
      String filterQuery = getFilterQueryStr(roles);

      ModifiableSolrParams newParams = new ModifiableSolrParams(rb.req.getParams());
      newParams.add("fq", filterQuery);
      rb.req.setParams(newParams);
      if (LOG.isDebugEnabled()) {
        LOG.debug("Adding filter query {} for user {} with roles {}", new Object[]{filterQuery, userName, roles});
      }

    } else {
      throw new SolrException(SolrException.ErrorCode.UNAUTHORIZED,
          "Request from user: " + userName +
              " rejected because user is not associated with any roles");
    }
  }

  @Override
  public String getDescription() {
    return "Handle Query Document Authorization";
  }

}
