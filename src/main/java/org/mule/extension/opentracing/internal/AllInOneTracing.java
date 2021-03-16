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

public final class AllInOneTracing {

    private Tracer tracer = null;
    private static String AGENT_HOST; // ="localhost";
    private static Integer AGENT_PORT; // =6831;
    private static AllInOneTracing allinOneTracing;
    private io.opentracing.Scope scope = null;
    private static int FLUSH_INTERVAL_IN_MS = 100;
    private static int MAX_QUEUE_SIZE = 10;

    private AllInOneTracing() {}

    public static void setConfiguration(
            String agentHost, Integer agentPort, Integer flushIntervalInMs, Integer maxQSize) {
        AGENT_HOST = agentHost;
        AGENT_PORT = agentPort;
        FLUSH_INTERVAL_IN_MS = flushIntervalInMs;
        MAX_QUEUE_SIZE = maxQSize;
    }

    // Initiatlize the Tracer - Everyone needs this
    // TODO : Exlpore Different configurations
    public static com.uber.jaeger.Tracer init(String service) {
        SamplerConfiguration samplerConfig = new SamplerConfiguration("const", 1);
        ReporterConfiguration reporterConfig =
                new ReporterConfiguration(
                        true, AGENT_HOST, AGENT_PORT, FLUSH_INTERVAL_IN_MS, MAX_QUEUE_SIZE);
        Configuration config = new Configuration(service, samplerConfig, reporterConfig);
        return (com.uber.jaeger.Tracer) config.getTracer();
    }

    public AllInOneTracing(String traceName) {
        this(traceName, AGENT_HOST, AGENT_PORT, FLUSH_INTERVAL_IN_MS, MAX_QUEUE_SIZE);
    }

    public AllInOneTracing(String traceName,
                           String agentHost,
                           Integer agentPort,
                           Integer flushIntervalInMs,
                           Integer maxQSize) {
        setConfiguration(agentHost, agentPort, flushIntervalInMs, maxQSize);
        tracer = init(traceName);
    }

    public void beginSpan(
            String traceName,
            String spanName,
            String tagName,
            String tagValue,
            String spanLogFieldName,
            String spanLogFieldValue) {
        scope = tracer.buildSpan(spanName).startActive(true);
        scope.span().setTag(tagName, tagValue);
        scope.span()
                .log(
                        com.google.common.collect.ImmutableMap.of(
                                spanLogFieldName, "string-format", "value", spanLogFieldValue));
    }

    // METHOD NAME = 3)Trace End  ? Do I Need this?
    public void endTrace(String spanName, String spanLogFieldName, String spanLogFieldValue) {
        if (scope == null) {
            io.opentracing.Scope scope = tracer.buildSpan(spanName).startActive(true);
        }
        scope.span()
                .log(
                        com.google.common.collect.ImmutableMap.of(
                                spanLogFieldName, "string-format", "value", spanLogFieldValue));
        scope.span().finish();
    }

    // METHOD NAME = 2)Inside the service - Log span messages
    public void writeTraceLog(String spanName, String spanLogFieldName, String spanLogFieldValue) {
        if (scope == null) {
            io.opentracing.Scope scope = tracer.buildSpan(spanName).startActive(true);
        }
        scope.span()
                .log(
                        com.google.common.collect.ImmutableMap.of(
                                spanLogFieldName, "string-format", "value", spanLogFieldValue));
    }

    // METHOD NAME = 4)Inject Trace
    public java.util.Map injectTrace(
            String targetHost, String targetPort, String targetPath, String httpMethod) {
        String traceID = "thisisdefaulrtraceid";
        try {

            int port = new Integer(targetPort).intValue();
            HttpUrl url =
                    new HttpUrl.Builder()
                            .scheme("http")
                            .host(targetHost)
                            .port(port)
                            .addPathSegment(targetPath)
                            .build();
            System.out.println("INSIDE INJECT - URL is " + url);
            Request.Builder requestBuilderTrace = new Request.Builder().url(url);
            Tags.SPAN_KIND.set(tracer.activeSpan(), Tags.SPAN_KIND_CLIENT);
            Tags.HTTP_METHOD.set(tracer.activeSpan(), httpMethod);
            Tags.HTTP_URL.set(tracer.activeSpan(), url.toString());
            tracer.inject(
                    tracer.activeSpan().context(),
                    Builtin.HTTP_HEADERS,
                    new RequestBuilderCarrier(requestBuilderTrace));
            Request request = requestBuilderTrace.build();
            // Ravis stuff
            Headers headers = request.headers();
            Map h = headers.toMultimap();
            Set keys = h.keySet();
            RequestBuilder rb =
                    RequestBuilder.create(
                            targetPath); // test is a resource name - replace with targetPath
            for (Iterator iterator = keys.iterator(); iterator.hasNext(); ) {
                String keyName = (String) iterator.next();
                System.out.println(
                        "INJECT TRACE - Ravis test - KeyName "
                                + keyName
                                + " Value "
                                + h.get(keyName));
                ArrayList al = (ArrayList) h.get(keyName);
                System.out.println(al.toString());
                rb.addHeader(keyName, al.toString());
                traceID = al.toString();
            }

            return h;
        } catch (Exception e) {
            System.out.println("Error trying to inject Trace " + e.getMessage());
            e.printStackTrace();
            return null; // Should we supply some dummy trace more like DLQ...all orphans can land
            // in one trace?
        }
    }

    // METHOD 5 = Extract Trace
    // Pass the traceID from the http inbound property
    // resource name is span name here
    public void extractTrace(
            String traceID,
            String resourceName,
            String tagName,
            String tagValue,
            String spanLogFieldName,
            String spanLogFieldValue) {
        java.util.Map headersMap = new HashMap();
        headersMap.put("uber-trace-id", traceID);
        Tracer.SpanBuilder spanBuilder;
        try {
            SpanContext parentSpanCtx =
                    tracer.extract(
                            Format.Builtin.HTTP_HEADERS, new TextMapExtractAdapter(headersMap));
            System.out.println(
                    " **INSIDE extractTrace-AiOne -  Parent Span context "
                            + parentSpanCtx
                            + "  OperationName "
                            + resourceName);
            if (parentSpanCtx == null) {
                spanBuilder = tracer.buildSpan(resourceName);
            } else {
                spanBuilder = tracer.buildSpan(resourceName).asChildOf(parentSpanCtx);
            }
            scope =
                    spanBuilder
                            .withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_SERVER)
                            .startActive(true);
            scope.span()
                    .log(
                            com.google.common.collect.ImmutableMap.of(
                                    spanLogFieldName, "string-format", "value", spanLogFieldValue));
        } catch (Exception e) {
            System.out.println(" &&&&&& inside exception trying to extractTrace " + e.getMessage());
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
            System.out.println(
                    "******* INSIDE THE RequestBuilderCarrier : key = "
                            + key
                            + " Value = "
                            + value);
        }
    }

    public void addTag(String spanName, String tagName, String tagvalue) {

        if (scope == null) {
            io.opentracing.Scope scope = tracer.buildSpan(spanName).startActive(true);
        }
        scope.span().setTag(tagName, tagvalue);
        scope.span()
                .log(
                        com.google.common.collect.ImmutableMap.of(
                                "tagAdd",
                                "string-format",
                                "value",
                                tagName + ":" + tagvalue + " Added to span " + spanName));
    }
}

