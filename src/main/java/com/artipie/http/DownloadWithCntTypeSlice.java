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

import com.artipie.asto.Storage;
import com.artipie.http.rs.RsWithHeaders;
import com.artipie.http.slice.SliceDownload;
import java.nio.ByteBuffer;
import java.util.Map;
import org.cactoos.list.ListOf;
import org.cactoos.map.MapEntry;
import org.reactivestreams.Publisher;

/**
 * Download with content-type slice.
 * @since 0.3
 */
public final class DownloadWithCntTypeSlice implements Slice {

    /**
     * Origin.
     */
    private final SliceDownload origin;

    /**
     * Content-type.
     */
    private final String cnttype;

    /**
     * Ctor.
     * @param storage Storage.
     * @param cnttype Content-type
     */
    public DownloadWithCntTypeSlice(final Storage storage, final String cnttype) {
        this.origin = new SliceDownload(storage);
        this.cnttype = cnttype;
    }

    @Override
    public Response response(
        final String line, final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body
    ) {
        return new RsWithHeaders(
            this.origin.response(line, headers, body),
            new ListOf<Map.Entry<String, String>>(
                new MapEntry<>("content-type", this.cnttype)
            )
        );
    }
}
