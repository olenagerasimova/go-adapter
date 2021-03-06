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

import com.artipie.asto.Content;
import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.http.auth.Authentication;
import com.artipie.http.auth.BasicIdentities;
import com.artipie.http.auth.Identities;
import com.artipie.http.auth.Permissions;
import com.artipie.http.rt.RtRule;
import com.artipie.http.rt.RtRulePath;
import com.artipie.http.rt.SliceRoute;
import com.artipie.http.slice.KeyFromPath;
import com.artipie.vertx.VertxSliceServer;
import io.vertx.reactivex.core.Vertx;
import java.io.File;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.cactoos.io.BytesOf;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.GenericContainer;

/**
 * IT case for {@link GoSlice}: it runs Testcontainer with latest version of golang,
 * starts up Vertx server with {@link GoSlice} and sets up go module `time` using go adapter.
 * @since 0.3
 * @todo #62:30min Make this test work with authorization, for now go refuses to send username and
 *  password parameters to insecure url with corresponding error: "refusing to pass credentials
 *  to insecure URL".
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.StaticAccessToStaticFields")
@DisabledOnOs(OS.WINDOWS)
public final class GoSliceITCase {

    /**
     * Vertx instance.
     */
    private static final Vertx VERTX = Vertx.vertx();

    /**
     * Test module version.
     */
    private static final String VERSION = "v0.0.0-20191024005414-555d28b269f0";

    /**
     * Test user.
     */
    private static final Pair<String, String> USER = new ImmutablePair<>("Alice", "wonderland");

    /**
     * Vertx instance.
     */
    private static VertxSliceServer slice;

    /**
     * GoLang container to verify Go repository layout.
     */
    private static GenericContainer<?> golang;

    @ParameterizedTest
    @ValueSource(booleans = {true})
    void installsTimeModule(final boolean anonymous) throws Exception {
        GoSliceITCase.startContainer(anonymous);
        MatcherAssert.assertThat(
            GoSliceITCase.golang
                .execInContainer("go", "get", "-insecure", "golang.org/x/time").getStderr(),
            new StringContains(String.format("go: golang.org/x/time upgrade => %s", VERSION))
        );
    }

    static void startContainer(final boolean anonymous) throws Exception {
        GoSliceITCase.slice = new VertxSliceServer(
            GoSliceITCase.VERTX,
            new SliceRoute(
                new RtRulePath(
                    new RtRule.ByPath(".*"),
                    new GoSlice(
                        GoSliceITCase.create(), GoSliceITCase.perms(anonymous),
                        GoSliceITCase.users(anonymous)
                    )
                )
            )
        );
        final int port = GoSliceITCase.slice.start();
        Testcontainers.exposeHostPorts(port);
        final String template = "http://%shost.testcontainers.internal:%d";
        final String url;
        if (anonymous) {
            url = String.format(template, "", port);
        } else {
            url = String.format(
                template,
                String.format("%s:%s@", GoSliceITCase.USER.getKey(), GoSliceITCase.USER.getValue()),
                port
            );
        }
        GoSliceITCase.golang = new GenericContainer<>("golang:latest")
            .withEnv("GOPROXY", url)
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
     * Permissions.
     * @param anonymous Is auth required?
     * @return Permissions instance
     */
    private static Permissions perms(final boolean anonymous) {
        final Permissions res;
        if (anonymous) {
            res = Permissions.FREE;
        } else {
            res = new Permissions.Single(GoSliceITCase.USER.getKey(), "download");
        }
        return res;
    }

    /**
     * Identities.
     * @param anonymous Is auth required?
     * @return Identities instance
     */
    private static Identities users(final boolean anonymous) {
        final Identities res;
        if (anonymous) {
            res = Identities.ANONYMOUS;
        } else {
            res = new BasicIdentities(
                new Authentication.Single(
                    GoSliceITCase.USER.getKey(), GoSliceITCase.USER.getValue()
                )
            );
        }
        return res;
    }

    /**
     * Creates test storage.
     * @return Storage
     * @throws Exception If smth wrong
     */
    private static Storage create() throws Exception {
        final Storage res = new InMemoryStorage();
        final String path = "/golang.org/x/time/@v/%s%s";
        final String zip = ".zip";
        //@checkstyle LineLengthCheck (4 lines)
        res.save(new KeyFromPath(String.format(path, "", "list")), new Content.From(VERSION.getBytes())).get();
        res.save(new KeyFromPath(String.format(path, VERSION, ".info")), new Content.From(String.format("{\"Version\":\"%s\",\"Time\":\"2019-10-24T00:54:14Z\"}", VERSION).getBytes())).get();
        res.save(new KeyFromPath(String.format(path, VERSION, ".mod")), new Content.From("module golang.org/x/time".getBytes())).get();
        res.save(new KeyFromPath(String.format(path, VERSION, zip)), new Content.From(new BytesOf(new File(String.format("src/test/resources/%s%s", VERSION, zip))).asBytes())).get();
        return res;
    }
}
