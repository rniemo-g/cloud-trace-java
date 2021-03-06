// Copyright 2016 Google Inc. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.cloud.trace.util;

import com.google.common.primitives.UnsignedLongs;
import java.math.BigInteger;

/**
 * A class that generates trace contexts.
 *
 * @see IdFactory
 * @see RandomSpanIdFactory
 * @see RandomTraceIdFactory
 * @see SpanId
 * @see TraceContext
 * @see TraceId
 * @see TraceOptionsFactory
 */
public class TraceContextFactory {
  private final TraceOptionsFactory traceOptionsFactory;
  private final IdFactory<TraceId> traceIdFactory;
  private final IdFactory<SpanId> spanIdFactory;

  /**
   * Returns the name of the trace context header.
   *
   * @return the name of the trace context header.
   */
  public static String headerKey() {
    return "X-Cloud-Trace-Context";
  }

  /**
   * Creates a trace context factory that uses the given trace options factory and new random trace
   * and span identifier factory to generate trace contexts.
   *
   * @param traceOptionsFactory a trace options factory used to generate trace options.
   */
  public TraceContextFactory(TraceOptionsFactory traceOptionsFactory) {
    this(traceOptionsFactory, new RandomTraceIdFactory(), new RandomSpanIdFactory());
  }

  /**
   * Creates a trace context factory that uses the given trace options factory and trace and span
   * identifier factories to generate trace contexts.
   *
   * @param traceOptionsFactory a trace options factory used to generate trace options.
   * @param traceIdFactory      a trace identifier factory used to generate trace identifiers.
   * @param spanIdFactory       a span identifier factory used to generate span identifiers.
   */
  public TraceContextFactory(TraceOptionsFactory traceOptionsFactory,
      IdFactory<TraceId> traceIdFactory, IdFactory<SpanId> spanIdFactory) {
    this.traceOptionsFactory = traceOptionsFactory;
    this.traceIdFactory = traceIdFactory;
    this.spanIdFactory = spanIdFactory;
  }

  /**
   * Generates a new trace context based on the parent context with a new span identifier. If the
   * parent context has an invalid trace identifier, the new trace context will also have a new
   * trace identifier.
   *
   * @param parentContext a trace context that is the parent of the new trace context.
   * @return the new trace context.
   */
  public TraceContext childContext(TraceContext parentContext) {
    if (parentContext.getTraceId().isValid()) {
      return new TraceContext(parentContext.getTraceId(), spanIdFactory.nextId(),
          traceOptionsFactory.create(parentContext.getTraceOptions()));
    }
    return new TraceContext(traceIdFactory.nextId(), spanIdFactory.nextId(),
        traceOptionsFactory.create(parentContext.getTraceOptions()));
  }

  /**
   * Generates a new trace context with invalid trace and span identifiers and default trace
   * options. This method does not invoke the trace options factory, so no sampling decision is
   * made.
   *
   * @return the new trace context.
   */
  public TraceContext initialContext() {
    return new TraceContext(traceIdFactory.invalid(), spanIdFactory.invalid(), new TraceOptions());
  }

  /**
   * Generates a new trace context with invalid trace and span identifiers and new trace options.
   * This method invokes the trace options factory, so a sampling decision is made.
   *
   * @return the new trace context.
   */
  public TraceContext rootContext() {
    return new TraceContext(traceIdFactory.invalid(), spanIdFactory.invalid(),
        traceOptionsFactory.create());
  }

  /**
   * Generates a new trace context based on the value of a trace context header.
   *
   * @param header a string that is the value of a trace context header.
   * @return the new trace context.
   */
  public TraceContext fromHeader(String header) {
    int index = header.indexOf('/');
    if (index == -1) {
      TraceId traceId = new TraceId(new BigInteger(header, 16));
      return new TraceContext(traceId, spanIdFactory.invalid(), traceOptionsFactory.create());
    }

    TraceId traceId = new TraceId(new BigInteger(header.substring(0, index), 16));

    String[] afterTraceId = header.substring(index + 1).split(";");
    SpanId spanId = new SpanId(UnsignedLongs.parseUnsignedLong(afterTraceId[0]));
    TraceOptions traceOptions = null;
    for (int i = 1; i < afterTraceId.length; i++) {
      if (afterTraceId[i].startsWith("o=")) {
        String optionsString = afterTraceId[i].substring(2);
        int options = Integer.parseInt(optionsString);
        traceOptions = traceOptionsFactory.create(new TraceOptions(options));
      }
    }

    if (traceOptions == null) {
      traceOptions = traceOptionsFactory.create();
    }

    return new TraceContext(traceId, spanId, traceOptions);
  }
}
