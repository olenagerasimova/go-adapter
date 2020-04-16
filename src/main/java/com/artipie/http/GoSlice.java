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

import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithStatus;
import com.artipie.http.rt.RtRule;
import com.artipie.http.rt.SliceRoute;
import com.artipie.http.slice.LoggingSlice;
import com.artipie.http.slice.SliceSimple;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.regex.Pattern;
import org.reactivestreams.Publisher;

/**
 * Go slice.
 * @since 0.3
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
public final class GoSlice implements Slice {

    /**
     * Origin.
     */
    private final Slice origin;

    /**
     * Ctor.
     */
    public GoSlice() {
        this.origin = new SliceRoute(
            new SliceRoute.Path(
                new RtRule.Multiple(
                    new RtRule.ByPath(Pattern.compile(".+/@v/v.*\\.info .+")),
                    new RtRule.ByMethod(RqMethod.GET)
                ),
                new LoggingSlice(new InfoSlice())
            ),
            new SliceRoute.Path(
                new RtRule.Multiple(
                    new RtRule.ByPath(Pattern.compile(".+/@v/v.*\\.mod .+")),
                    new RtRule.ByMethod(RqMethod.GET)
                ),
                new LoggingSlice(new ModSlice())
            ),
            new SliceRoute.Path(
                new RtRule.Multiple(
                    new RtRule.ByPath(Pattern.compile(".+/@v/v.*\\.zip .+")),
                    new RtRule.ByMethod(RqMethod.GET)
                ),
                new LoggingSlice(new ZipSlice())
            ),
            new SliceRoute.Path(
                new RtRule.Multiple(
                    new RtRule.ByPath(Pattern.compile(".+/@v/list .+")),
                    new RtRule.ByMethod(RqMethod.GET)
                ),
                new LoggingSlice(new ListSlice())
            ),
            new SliceRoute.Path(
                new RtRule.Multiple(
                    new RtRule.ByPath(Pattern.compile(".+/@v/latest .+")),
                    new RtRule.ByMethod(RqMethod.GET)
                ),
                new LoggingSlice(new LatestSlice())
            ),
            new SliceRoute.Path(
                RtRule.FALLBACK,
                new SliceSimple(
                    new RsWithStatus(RsStatus.NOT_FOUND)
                )
            )
        );
    }

    @Override
    public Response response(
        final String line, final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body) {
        return this.origin.response(line, headers, body);
    }
}
