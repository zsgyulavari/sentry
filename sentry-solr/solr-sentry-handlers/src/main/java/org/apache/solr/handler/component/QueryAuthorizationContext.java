package org.apache.solr.handler.component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public class QueryAuthorizationContext {
  private final String userName;
  private final Supplier<Set<String>> rolesSupplier;

  private final Map<String, Supplier<Object>> additionalParams;

  public QueryAuthorizationContext(String userName, Supplier<Set<String>> rolesSupplier) {
    this.userName = userName;
    this.rolesSupplier = rolesSupplier;
    this.additionalParams = new HashMap<>();
  }

  public String getUserName() {
    return userName;
  }

  public Set<String> getRoles() {
    return rolesSupplier.get();
  }

  public void addParam(String name, Object value) {
    addParam(name, () -> value);
  }

  public void addParam(String name, Supplier<Object> value) {
    additionalParams.put(name, value);
  }

  public Object getParamValue(String name) {
    return additionalParams.get(name).get();
  }

  public static final class CachingSupplier<T> implements Supplier<T> {
    private final Supplier<? extends T> internalSupplier;
    private Supplier<T> cached = null;

    public CachingSupplier(Supplier<? extends T> internalSupplier) {
      this.internalSupplier = internalSupplier;
    }

    @Override
    public T get() {
      if (cached != null) {
        return cached.get();
      }
      synchronized (this) {
        if (cached != null) {
          return cached.get();
        }
        T result = internalSupplier.get();
        cached = () -> result;
        return result;
      }
    }
  }

}
