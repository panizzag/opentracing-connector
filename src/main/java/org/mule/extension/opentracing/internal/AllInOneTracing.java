package org.mule.extension.opentracing.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.http.client.methods.RequestBuilder;

import com.uber.jaeger.Configuration;
import com.uber.jaeger.Configuration.ReporterConfiguration;
import com.uber.jaeger.Configuration.SamplerConfiguration;

import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.Format.Builtin;
import io.opentracing.propagation.TextMapExtractAdapter;
import io.opentracing.tag.Tags;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.Request;
import org.mule.runtime.extension.api.annotation.Ignore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AllInOneTracing {

    private static final Logger LOGGER = LoggerFactory.getLogger(AllInOneTracing.class);

    private Tracer tracer = null;
    private String traceName = null;
    private static String AGENT_HOST;
    private static Integer AGENT_PORT;
    private io.opentracing.Scope scope = null;
    private static int FLUSH_INTERVAL_IN_MS = 100;
    private static int MAX_QUEUE_SIZE = 10;

    private AllInOneTracing() {}

    public static void setConfiguration(String agentHost,
                                        Integer agentPort,
                                        Integer flushIntervalInMs,
                                        Integer maxQSize) {
        AGENT_HOST = agentHost;
        AGENT_PORT = agentPort;
        FLUSH_INTERVAL_IN_MS = flushIntervalInMs;
        MAX_QUEUE_SIZE = maxQSize;
    }

    public static com.uber.jaeger.Tracer init(String service) {
        SamplerConfiguration samplerConfig = new SamplerConfiguration("const", 1);
        ReporterConfiguration reporterConfig = new ReporterConfiguration(true, AGENT_HOST, AGENT_PORT, FLUSH_INTERVAL_IN_MS, MAX_QUEUE_SIZE);
        Configuration config = new Configuration(service, samplerConfig, reporterConfig);
        return (com.uber.jaeger.Tracer) config.getTracer();
    }

    public AllInOneTracing(String traceName,
                           String agentHost,
                           Integer agentPort,
                           Integer flushIntervalInMs,
                           Integer maxQSize) {
        setConfiguration(agentHost, agentPort, flushIntervalInMs, maxQSize);
        tracer = init(traceName);
        this.traceName = traceName;
    }

    public void beginSpan(String spanName,
                          String tagName,
                          String tagValue,
                          String spanLogFieldName,
                          String spanLogFieldValue) {
        LOGGER.debug("beginSpan operation. Building new span with spanName: {}", spanName);
        scope = tracer.buildSpan(spanName).startActive(true);
        scope.span().setTag(tagName, tagValue);
        scope.span().log(com.google.common.collect.ImmutableMap.of(spanLogFieldName, "string-format", "value", spanLogFieldValue));
    }

    public void endTrace(String spanName,
                         String spanLogFieldName,
                         String spanLogFieldValue) {
        if (scope == null) {
            LOGGER.debug("endSpan operation. Building new span with spanName: {}", spanName);
            io.opentracing.Scope scope = tracer.buildSpan(spanName).startActive(true);
        }
        scope.span().log(com.google.common.collect.ImmutableMap.of(spanLogFieldName, "string-format", "value", spanLogFieldValue));
        scope.span().finish();
    }

    public void writeTraceLog(String spanName,
                              String spanLogFieldName,
                              String spanLogFieldValue) {
        if (scope == null) {
            LOGGER.debug("writeTraceLog operation. Building new span with spanName: {}", spanName);
            io.opentracing.Scope scope = tracer.buildSpan(spanName).startActive(true);
        }
        scope.span().log(com.google.common.collect.ImmutableMap.of(spanLogFieldName, "string-format", "value", spanLogFieldValue));
    }

    public java.util.Map injectTrace(String targetHost,
                                     String targetPort,
                                     String targetPath,
                                     String httpMethod,
                                     String scheme) {
        String traceID;
        try {
            int port = new Integer(targetPort).intValue();
            HttpUrl url = new HttpUrl.Builder()
                                .scheme(scheme)
                                .host(targetHost)
                                .port(port)
                                .addPathSegment(targetPath)
                                .build();
            Request.Builder requestBuilderTrace = new Request.Builder().url(url);
            scope = tracer.buildSpan(url.toString()).startActive(true);
            Tags.SPAN_KIND.set(tracer.activeSpan(), Tags.SPAN_KIND_CLIENT);
            Tags.HTTP_METHOD.set(tracer.activeSpan(), httpMethod);
            Tags.HTTP_URL.set(tracer.activeSpan(), url.toString());
            tracer.inject(tracer.activeSpan().context(),
                          Builtin.HTTP_HEADERS,
                          new RequestBuilderCarrier(requestBuilderTrace));
            Request request = requestBuilderTrace.build();
            Headers headers = request.headers();
            Map h = headers.toMultimap();
            Set keys = h.keySet();
            RequestBuilder rb = RequestBuilder.create(targetPath);
            for (Iterator iterator = keys.iterator(); iterator.hasNext(); ) {
                String keyName = (String) iterator.next();
                LOGGER.debug("injectTrace: \nKeyName: {} \nValue: {}", keyName, h.get(keyName));
                ArrayList al = (ArrayList) h.get(keyName);
                rb.addHeader(keyName, al.toString());
                traceID = al.toString();
            }
            return getHeadersMap(headers);
        } catch (Exception e) {
            LOGGER.error("Error trying to inject Trace " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // METHOD 5 = Extract Trace
    // Pass the traceID from the http inbound property
    // resource name is span name here
    public void extractTrace(String traceID,
                             String headerName,
                             String resourceName,
                             String spanLogFieldName,
                             String spanLogFieldValue) {
        java.util.Map headersMap = new HashMap();
        headersMap.put(headerName, traceID);
        Tracer.SpanBuilder spanBuilder;
        try {
            SpanContext parentSpanCtx = tracer.extract(Format.Builtin.HTTP_HEADERS, new TextMapExtractAdapter(headersMap));
            LOGGER.debug("Parent Span context "
                            + parentSpanCtx
                            + "  OperationName "
                            + resourceName);
            if (parentSpanCtx == null) {
                LOGGER.debug("extractTrace operation. Building new span with spanName: {}", resourceName);
                spanBuilder = tracer.buildSpan(resourceName);
            } else {
                LOGGER.debug("extractTrace operation. Building new span as child of {} with spanName: {}", parentSpanCtx, resourceName);
                spanBuilder = tracer.buildSpan(resourceName).asChildOf(parentSpanCtx);
            }
            scope = spanBuilder.withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_SERVER).startActive(true);
            scope.span().log(com.google.common.collect.ImmutableMap.of(spanLogFieldName, "string-format", "value", spanLogFieldValue));
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            spanBuilder = tracer.buildSpan(resourceName);
        }
    }

    /*
     * Call this to build your request to get the Tracing ID
     */
    private static class RequestBuilderCarrier implements io.opentracing.propagation.TextMap {
        private final Request.Builder builder;

        RequestBuilderCarrier(Request.Builder builder) {
            this.builder = builder;
        }

        @Override
        public Iterator<Entry<String, String>> iterator() {
            throw new UnsupportedOperationException("carrier is write-only");
        }

        @Override
        public void put(String key, String value) {
            builder.addHeader(key, value);
        }
    }

    public void addTag(String spanName, String tagName, String tagvalue) {

        if (scope == null) {
            LOGGER.debug("addTag operation. Building new span with spanName: {}", spanName);
            io.opentracing.Scope scope = tracer.buildSpan(spanName).startActive(true);
        }
        scope.span().setTag(tagName, tagvalue);
        scope.span().log(com.google.common.collect.ImmutableMap.of("tagAdd","string-format","value",tagName + ":" + tagvalue));
    }

    @Ignore
    private Map<String, String> getHeadersMap(Headers headers) {

        Map<String, String> map = new HashMap<String, String>();
        for(int i=0;i<headers.size();i++) {
            String key = headers.name(i);
            String value = headers.value(i);
            map.put(key, value);
        }

        return map;
    }
}

