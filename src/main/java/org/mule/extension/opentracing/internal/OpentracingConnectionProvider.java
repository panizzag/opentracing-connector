package org.mule.extension.opentracing.internal;

import org.mule.extension.opentracing.api.OpentracingConfiguration;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.connection.PoolingConnectionProvider;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.CachedConnectionProvider;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;

import org.mule.runtime.http.api.HttpService;
import org.mule.runtime.http.api.client.HttpClient;
import org.mule.runtime.http.api.domain.message.request.HttpRequestBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;


/**
 * This class (as it's name implies) provides connection instances and the funcionality to disconnect and validate those
 * connections.
 * <p>
 * All connection related parameters (values required in order to create a connection) must be
 * declared in the connection providers.
 * <p>
 * This particular example is a {@link PoolingConnectionProvider} which declares that connections resolved by this provider
 * will be pooled and reused. There are other implementations like {@link CachedConnectionProvider} which lazily creates and
 * caches connections or simply {@link ConnectionProvider} if you want a new connection each time something requires one.
 */
public class OpentracingConnectionProvider implements PoolingConnectionProvider<OpentracingConnection> {

  private final Logger LOGGER = LoggerFactory.getLogger(OpentracingConnectionProvider.class);

  private HttpClient httpClient;
  private HttpRequestBuilder httpRequestBuilder;

    @ParameterGroup(name = "Connection")
    OpentracingConfiguration config;

    @Inject
    private HttpService httpService;

  @Override
  public OpentracingConnection connect() throws ConnectionException {
    return new OpentracingConnection(httpService, config);
  }

  @Override
  public void disconnect(OpentracingConnection connection) {
      try {
          connection.invalidate();
      } catch (Exception e) {
          e.printStackTrace();
      }
  }

  @Override
  public ConnectionValidationResult validate(OpentracingConnection connection) {
      ConnectionValidationResult result;
      try {
          if(connection.isConnected()){
              result = ConnectionValidationResult.success();
          } else {
              result = ConnectionValidationResult.failure("Connection Failed", new Exception());
          }
      } catch (Exception e) {
          result = ConnectionValidationResult.failure("Connection Failed", new Exception());
      }
      return result;
  }
}
