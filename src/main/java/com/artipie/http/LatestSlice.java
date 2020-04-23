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

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.rq.RequestLineFrom;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithBody;
import com.artipie.http.rs.RsWithHeaders;
import com.artipie.http.rs.RsWithStatus;
import com.artipie.http.rs.StandardRs;
import com.artipie.http.slice.KeyFromPath;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.reactivestreams.Publisher;

/**
 * Go mod slice: this slice returns json-formatted metadata about go module as
 * described in "JSON-formatted metadata(.info file body) about the latest known version"
 * section of readme.
 * @since 0.3
 * @todo #33:30min Make `latest` replacement in response method more clear, for example
 *  by using `URI` and removing last part of the path or something similar.
 */
public final class LatestSlice implements Slice {

    /**
     * Storage.
     */
    private final Storage storage;

    /**
     * Ctor.
     * @param storage Storage
     */
    public LatestSlice(final Storage storage) {
        this.storage = storage;
    }

    @Override
    public Response response(
        final String line, final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body) {
        return new AsyncResponse(
            CompletableFuture.supplyAsync(
                () -> new RequestLineFrom(line.replace("latest ", "v ")).uri().getPath()
            ).thenCompose(
                path -> this.storage.list(new KeyFromPath(path)).thenCompose(this::resp)
            )
        );
    }

    /**
     * Composes response. It filters .info files from module directory, chooses the greatest
     * version and returns content from the .info file.
     * @param module Module file names list from repository
     * @return Response
     */
    private CompletableFuture<Response> resp(final Collection<Key> module) {
        final Optional<String> info = module.stream().map(Key::string)
            .filter(item -> item.endsWith("info"))
            .max(Comparator.naturalOrder());
        final CompletableFuture<Response> res;
        if (info.isPresent()) {
            res = this.storage.value(new KeyFromPath(info.get()))
                .thenApply(RsWithBody::new)
                .thenApply(rsp -> new RsWithHeaders(rsp, "content-type", "application/json"))
                .thenApply(rsp -> new RsWithStatus(rsp, RsStatus.OK));
        } else {
            res = CompletableFuture.completedFuture(StandardRs.NOT_FOUND);
        }
        return res;
    }
}
