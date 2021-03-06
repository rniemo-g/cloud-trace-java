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

package com.google.cloud.trace.guice.v1;

import com.google.cloud.trace.RawTracer;
import com.google.cloud.trace.v1.RawTracerV1;
import com.google.cloud.trace.v1.sink.TraceSink;
import com.google.cloud.trace.v1.source.TraceSource;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;

public class RawTracerV1Module extends AbstractModule {
  @Override
  protected void configure() {
    Multibinder<RawTracer> setBinder = Multibinder.newSetBinder(binder(), RawTracer.class);
    setBinder.addBinding().toProvider(RawTracerV1Provider.class).in(Singleton.class);
  }

  private static class RawTracerV1Provider implements Provider<RawTracerV1> {
    private final String projectId;
    private final TraceSink traceSink;

    @Inject
    RawTracerV1Provider(@ProjectId String projectId, TraceSink traceSink) {
      this.projectId = projectId;
      this.traceSink = traceSink;
    }

    @Override
    public RawTracerV1 get() {
      return new RawTracerV1(projectId, new TraceSource(), traceSink);
    }
  }
}
