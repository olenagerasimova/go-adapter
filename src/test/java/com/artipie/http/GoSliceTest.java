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
import com.artipie.http.hm.RsHasBody;
import com.artipie.http.hm.RsHasHeaders;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.slice.KeyFromPath;
import io.reactivex.Flowable;
import java.util.concurrent.ExecutionException;
import org.cactoos.list.ListOf;
import org.cactoos.map.MapEntry;
import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.AllOf;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link GoSlice}.
 * @since 0.3
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class GoSliceTest {

    @Test
    void returnsInfo() throws ExecutionException, InterruptedException {
        final String path = "news.info/some/day/@v/v0.1.info";
        final String body = "{\"Version\":\"0.1\",\"Time\":\"2020-01-24T00:54:14Z\"}";
        MatcherAssert.assertThat(
            new GoSlice(GoSliceTest.storage(path, body)).response(
                GoSliceTest.line(path), Headers.EMPTY, Flowable.empty()
            ),
            matchers(body, "application/json")
        );
    }

    @Test
    void returnsMod() throws ExecutionException, InterruptedException {
        final String path = "example.com/mod/one/@v/v1.mod";
        final String body = "bla-bla";
        MatcherAssert.assertThat(
            new GoSlice(GoSliceTest.storage(path, body)).response(
                GoSliceTest.line(path), Headers.EMPTY, Flowable.empty()
            ),
            matchers(body, "text/plain")
        );
    }

    @Test
    void returnsZip() throws ExecutionException, InterruptedException {
        final String path = "modules.zip/foo/bar/@v/v1.0.9.zip";
        final String body = "smth";
        MatcherAssert.assertThat(
            new GoSlice(GoSliceTest.storage(path, body)).response(
                GoSliceTest.line(path), Headers.EMPTY, Flowable.empty()
            ),
            matchers(body, "application/zip")
        );
    }

    @Test
    void returnsList() throws ExecutionException, InterruptedException {
        final String path = "example.com/list/bar/@v/list";
        final String body = "v1.2.3";
        MatcherAssert.assertThat(
            new GoSlice(GoSliceTest.storage(path, body)).response(
                GoSliceTest.line(path), Headers.EMPTY, Flowable.empty()
            ),
            matchers(body, "text/plain")
        );
    }

    @Test
    void fallbacks() throws ExecutionException, InterruptedException {
        final String path = "example.com/abc/def";
        final String body = "v1.8.3";
        MatcherAssert.assertThat(
            new GoSlice(GoSliceTest.storage(path, body)).response(
                GoSliceTest.line(path), Headers.EMPTY, Flowable.empty()
            ),
            new RsHasStatus(RsStatus.NOT_FOUND)
        );
    }

    @Test
    void returnsLatest() throws ExecutionException, InterruptedException {
        final String body = "{\"Version\":\"1.1\",\"Time\":\"2020-01-24T00:54:14Z\"}";
        MatcherAssert.assertThat(
            new GoSlice(GoSliceTest.storage("example.com/latest/bar/@v/v1.1.info", body)).response(
                GoSliceTest.line("example.com/latest/bar/@latest"), Headers.EMPTY, Flowable.empty()
            ),
            matchers(body, "application/json")
        );
    }

    /**
     * Composes matchers.
     * @param body Body
     * @param type Content-type
     * @return List of matchers
     */
    private static AllOf<Response> matchers(final String body,
        final String type) {
        return new AllOf<>(
            new ListOf<Matcher<? super Response>>(
                new RsHasBody(body.getBytes()),
                new RsHasHeaders(new MapEntry<>("content-type", type))
            )
        );
    }

    /**
     * Request line.
     * @param path Path
     * @return Proper request line
     */
    private static String line(final String path) {
        return new RequestLine("GET", path, "HTTP1/1").toString();
    }

    /**
     * Composes storage.
     * @param path Where to store
     * @param body Body to store
     * @return Storage
     * @throws ExecutionException On error
     * @throws InterruptedException On error
     */
    private static Storage storage(final String path, final String body)
        throws ExecutionException, InterruptedException {
        final Storage storage = new InMemoryStorage();
        storage.save(
            new KeyFromPath(path),
            new Content.From(body.getBytes())
        ).get();
        return storage;
    }

}
