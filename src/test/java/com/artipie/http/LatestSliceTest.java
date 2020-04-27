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
 * Test for {@link LatestSlice}.
 * @since 0.3
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
public class LatestSliceTest {

    @Test
    void returnsLatestVersion() throws ExecutionException, InterruptedException {
        final Storage storage = new InMemoryStorage();
        storage.save(
            new KeyFromPath("example.com/latest/news/@v/v0.0.1.zip"), new Content.From(new byte[]{})
        ).get();
        storage.save(
            new KeyFromPath("example.com/latest/news/@v/v0.0.1.mod"), new Content.From(new byte[]{})
        ).get();
        storage.save(
            new KeyFromPath("example.com/latest/news/@v/v0.0.1.info"),
            new Content.From(new byte[]{})
        ).get();
        storage.save(
            new KeyFromPath("example.com/latest/news/@v/v0.0.2.zip"), new Content.From(new byte[]{})
        ).get();
        storage.save(
            new KeyFromPath("example.com/latest/news/@v/v0.0.2.mod"), new Content.From(new byte[]{})
        ).get();
        final String info = "{\"Version\":\"v0.0.2\",\"Time\":\"2019-06-28T10:22:31Z\"}";
        storage.save(
            new KeyFromPath("example.com/latest/news/@v/v0.0.2.info"),
            new Content.From(info.getBytes())
        ).get();
        MatcherAssert.assertThat(
            new LatestSlice(storage).response(
                "GET example.com/latest/news/@latest?a=b HTTP/1.1", Headers.EMPTY, Flowable.empty()
            ),
            new AllOf<>(
                new ListOf<Matcher<? super Response>>(
                    new RsHasBody(info.getBytes()),
                    new RsHasHeaders(new MapEntry<>("content-type", "application/json"))
                )
            )
        );
    }

    @Test
    void returnsNotFondWhenModuleNotFound() {
        MatcherAssert.assertThat(
            new LatestSlice(new InMemoryStorage()).response(
                "GET example.com/first/@latest HTTP/1.1", Headers.EMPTY, Flowable.empty()
            ),
            new RsHasStatus(RsStatus.NOT_FOUND)
        );
    }

}
