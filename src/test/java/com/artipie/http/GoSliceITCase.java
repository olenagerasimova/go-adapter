/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 artipie.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.artipie.http;

import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithBody;
import com.artipie.http.rs.RsWithHeaders;
import com.artipie.http.rs.RsWithStatus;
import com.artipie.http.rt.RtRule;
import com.artipie.http.rt.SliceRoute;
import com.artipie.vertx.VertxSliceServer;
import io.reactivex.Flowable;
import io.vertx.reactivex.core.Vertx;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;
import org.cactoos.io.BytesOf;
import org.cactoos.list.ListOf;
import org.cactoos.map.MapEntry;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.GenericContainer;

/**
 * IT case for {@link GoSlice}: it runs Testcontainer with latest version of golang,
 * starts up Vertx server with {@link GoSlice} and sets up go module `time` using go adapter.
 * @since 0.3
 */
public class GoSliceITCase {

    /**
     * Vertx instance.
     */
    private static final Vertx VERTX = Vertx.vertx();

    /**
     * Vertx instance.
     */
    private static VertxSliceServer slice;

    /**
     * GoLang container to verify Go repository layout.
     */
    private static GenericContainer<?> golang;

    @Test
    void installsTimeModule() throws IOException, InterruptedException {
        MatcherAssert.assertThat(
            GoSliceITCase.golang
                .execInContainer("go", "get", "-x", "-insecure", "golang.org/x/time").getStderr(),
            new StringContains(
                "go: golang.org/x/time upgrade => v0.0.0-20191024005414-555d28b269f0"
            )
        );
    }

    @BeforeAll
    static void startContainer() {
        GoSliceITCase.slice = new VertxSliceServer(
            GoSliceITCase.VERTX,
            new SliceRoute(
                new SliceRoute.Path(
                    new RtRule.ByPath(".*"),
                    new Fake()
                )
            )
        );
        final int port = GoSliceITCase.slice.start();
        Testcontainers.exposeHostPorts(port);
        GoSliceITCase.golang = new GenericContainer<>("golang:latest")
            .withEnv(
                "GOPROXY",
                String.format("http://host.testcontainers.internal:%d", port)
            )
            .withEnv("GO111MODULE", "on")
            .withCommand("tail", "-f", "/dev/null");
        GoSliceITCase.golang.start();
    }

    @AfterAll
    static void stopContainer() {
        GoSliceITCase.slice.close();
        GoSliceITCase.VERTX.close();
        GoSliceITCase.golang.stop();
    }

    /**
     * Fake implementation of slice.
     * @since 0.3
     * @checkstyle IllegalCatchCheck (500 lines)
     */
    @SuppressWarnings({"PMD.AvoidCatchingGenericException",
        "PMD.AvoidThrowingRawExceptionTypes", "PMD.AvoidDuplicateLiterals"})
    static class Fake implements Slice {

        @Override
        public Response response(final String line,
            final Iterable<Map.Entry<String, String>> headers,
            final Publisher<ByteBuffer> body) {
            final Response res;
            if (line.contains("latest") || line.contains("info")) {
                res = new RsWithBody(
                    new RsWithHeaders(
                        new RsWithStatus(RsStatus.OK),
                        new ListOf<Map.Entry<String, String>>(
                            new MapEntry<>("content-type", "application/json")
                        )
                    ),
                    //@checkstyle LineLengthCheck (1 line)
                    Flowable.fromArray(ByteBuffer.wrap("{\"Version\":\"v0.0.0-20191024005414-555d28b269f0\",\"Time\":\"2019-10-24T00:54:14Z\"}".getBytes()))
                );
            } else if (line.contains("list")) {
                res = Response.EMPTY;
            } else if (line.contains("mod")) {
                res = new RsWithBody(
                    new RsWithHeaders(
                        new RsWithStatus(RsStatus.OK),
                        new ListOf<Map.Entry<String, String>>(
                            new MapEntry<>("content-type", "text/plain; charset=UTF-8")
                        )
                    ),
                    Flowable.fromArray(ByteBuffer.wrap("module golang.org/x/time".getBytes()))
                );
            } else if (line.contains("zip")) {
                try {
                    res = new RsWithBody(
                        new RsWithHeaders(
                            new RsWithStatus(RsStatus.OK),
                            new ListOf<Map.Entry<String, String>>(
                                new MapEntry<>("content-type", "application/zip")
                            )
                        ),
                        ByteBuffer.wrap(
                            new BytesOf(
                                //@checkstyle LineLengthCheck (1 line)
                                new File("src/test/resources/v0.0.0-20191024005414-555d28b269f0.zip")
                            ).asBytes()
                        )
                    );
                } catch (final Exception ex) {
                    throw new RuntimeException(ex);
                }
            } else {
                res = Response.EMPTY;
            }
            return res;
        }
    }
}
