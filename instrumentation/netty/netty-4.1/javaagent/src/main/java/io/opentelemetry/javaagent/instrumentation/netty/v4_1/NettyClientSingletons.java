/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.netty.v4_1;

import static io.opentelemetry.instrumentation.netty.v4.common.internal.client.NettyConnectionInstrumentationFlag.enabledOrErrorOnly;

import io.netty.handler.codec.http.HttpResponse;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.netty.v4.common.HttpRequestAndChannel;
import io.opentelemetry.instrumentation.netty.v4.common.internal.client.NettyClientInstrumenterFactory;
import io.opentelemetry.instrumentation.netty.v4.common.internal.client.NettyConnectionInstrumenter;
import io.opentelemetry.instrumentation.netty.v4.common.internal.client.NettySslInstrumenter;
import io.opentelemetry.instrumentation.netty.v4_1.internal.client.NettyClientHandlerFactory;
import io.opentelemetry.javaagent.bootstrap.internal.CommonConfig;
import io.opentelemetry.javaagent.bootstrap.internal.InstrumentationConfig;
import java.util.Collections;
import java.util.function.Function;

public final class NettyClientSingletons {

  private static final boolean connectionTelemetryEnabled =
      InstrumentationConfig.get()
          .getBoolean("otel.instrumentation.netty.connection-telemetry.enabled", false);
  private static final boolean sslTelemetryEnabled =
      InstrumentationConfig.get()
          .getBoolean("otel.instrumentation.netty.ssl-telemetry.enabled", false);

  private static final Instrumenter<HttpRequestAndChannel, HttpResponse> INSTRUMENTER;
  private static final NettyConnectionInstrumenter CONNECTION_INSTRUMENTER;
  private static final NettySslInstrumenter SSL_INSTRUMENTER;
  private static final NettyClientHandlerFactory CLIENT_HANDLER_FACTORY;

  static {
    NettyClientInstrumenterFactory factory =
        new NettyClientInstrumenterFactory(
            GlobalOpenTelemetry.get(),
            "io.opentelemetry.netty-4.1",
            enabledOrErrorOnly(connectionTelemetryEnabled),
            enabledOrErrorOnly(sslTelemetryEnabled),
            CommonConfig.get().getPeerServiceResolver(),
            CommonConfig.get().shouldEmitExperimentalHttpClientTelemetry());
    INSTRUMENTER =
        factory.createHttpInstrumenter(
            builder ->
                builder
                    .setCapturedRequestHeaders(CommonConfig.get().getClientRequestHeaders())
                    .setCapturedResponseHeaders(CommonConfig.get().getClientResponseHeaders())
                    .setKnownMethods(CommonConfig.get().getKnownHttpRequestMethods()),
            builder -> builder.setKnownMethods(CommonConfig.get().getKnownHttpRequestMethods()),
            Function.identity(),
            Collections.emptyList());
    CONNECTION_INSTRUMENTER = factory.createConnectionInstrumenter();
    SSL_INSTRUMENTER = factory.createSslInstrumenter();
    CLIENT_HANDLER_FACTORY =
        new NettyClientHandlerFactory(
            INSTRUMENTER, CommonConfig.get().shouldEmitExperimentalHttpClientTelemetry());
  }

  public static Instrumenter<HttpRequestAndChannel, HttpResponse> instrumenter() {
    return INSTRUMENTER;
  }

  public static NettyConnectionInstrumenter connectionInstrumenter() {
    return CONNECTION_INSTRUMENTER;
  }

  public static NettySslInstrumenter sslInstrumenter() {
    return SSL_INSTRUMENTER;
  }

  public static NettyClientHandlerFactory clientHandlerFactory() {
    return CLIENT_HANDLER_FACTORY;
  }

  private NettyClientSingletons() {}
}
