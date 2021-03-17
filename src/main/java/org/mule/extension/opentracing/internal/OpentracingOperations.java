package org.mule.extension.opentracing.internal;

import static org.mule.runtime.extension.api.annotation.param.MediaType.ANY;

import org.mule.extension.opentracing.internal.typeResolvers.InjectTraceResolver;
import org.mule.runtime.extension.api.annotation.metadata.OutputResolver;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.extension.opentracing.api.enums.*;

import java.util.Map;

/**
 * This class is a container for operations, every public method in this class will be taken as an extension operation.
 */
public class OpentracingOperations {

  /**
   * Document this
   */
  @MediaType(value = ANY, strict = false)
  public void beginSpan(@Connection OpentracingConnection connection,
                        @Optional(defaultValue="muleTracer") String traceName,
                        @Optional(defaultValue="#[attributes.requestUri]") String spanName,
                        @Optional(defaultValue="http_url") TagType tagName,
                        @Optional(defaultValue="#[attributes.listenerPath]") String tagValue,
                        @Optional(defaultValue="event") OpenTraceLogType logType,
                        @Optional(defaultValue="Enter your log message here") String logMessage){
    connection.beginSpan(traceName, spanName, tagName, tagValue, logType, logMessage);
  }

  /**
   * Document this
   */
  @MediaType(value = ANY, strict = false)
  public void endSpan(@Connection OpentracingConnection connection,
                      @Optional(defaultValue="#[attributes.requestUri]") String spanName,
                      @Optional(defaultValue="event") OpenTraceLogType logType,
                      @Optional(defaultValue="Enter your log message here") String logMessage) {
    connection.endSpan(spanName, logType, logMessage);
  }

  /**
   * Document this
   */
  @MediaType(value = ANY, strict = false)
  @OutputResolver(output = InjectTraceResolver.class)
  public Map<String, String> injectTrace(@Connection OpentracingConnection connection,
                          String remoteHost,
                          String remotePort,
                          String remotePath,
                          String scheme,
                          HttpMethod remoteServiceHttpMethod) {
    return connection.injectTrace(remoteHost, remotePort, remotePath, scheme, remoteServiceHttpMethod);
  }

  /**
   * Document this
   */
  @MediaType(value = ANY, strict = false)
  public void extractTrace(@Connection OpentracingConnection connection,
                           @Optional(defaultValue="#[attributes.headers.'trace-id']") String traceID,
                           @Optional(defaultValue="trace-id") String headerName,
                           @Optional(defaultValue="#[attributes.requestUri]") String resourceName,
                           @Optional(defaultValue="http_url") TagType tagName,
                           @Optional(defaultValue="#[attributes.listenerPath]") String tagValue,
                           @Optional(defaultValue="event") OpenTraceLogType logType,
                           @Optional(defaultValue="Enter your log message here") String logMessage) {
    connection.extractTrace(traceID, headerName, resourceName, tagName, tagValue, logType, logMessage);
  }

  /**
   * Document this
   */
  @MediaType(value = ANY, strict = false)
  public void writeTraceLog(@Connection OpentracingConnection connection,
                            @Optional(defaultValue="#[attributes.requestUri]") String spanName,
                            @Optional(defaultValue="event") OpenTraceLogType logType,
                            @Optional(defaultValue="Enter your log message here") String logMessage) {

    connection.writeTraceLog(spanName, logType, logMessage);
  }

  /**
   * Document this
   */
  @MediaType(value = ANY, strict = false)
  public void addTag(@Connection OpentracingConnection connection,
                     @Optional(defaultValue="#[attributes.requestUri]") String spanName,
                     @Optional(defaultValue="component") TagType tagType,
                     @Optional(defaultValue="Enter your tag Value here") String tagvalue) {

    connection.addTag(spanName, tagType, tagvalue);
  }
}
