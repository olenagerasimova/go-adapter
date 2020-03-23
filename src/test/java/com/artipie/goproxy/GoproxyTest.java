/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Yegor Bugayenko
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
package com.artipie.goproxy;

import com.artipie.asto.Content;
import com.artipie.asto.Remaining;
import io.reactivex.Single;
import java.nio.ByteBuffer;
import java.time.Instant;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

/**
 * Unit test for Goproxy class.
 *
 * @since 0.3
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
public class GoproxyTest {
    @Test
    public void generatesVersionedJson() {
        final Instant timestamp = Instant.parse("2020-03-17T08:05:12.32496732Z");
        final Single<Content> content = Goproxy.generateVersionedJson(
            "0.0.1", timestamp
        );
        final ByteBuffer data = content.flatMap(Goproxy::readCompletely).blockingGet();
        MatcherAssert.assertThat(
            "Content does not match",
            "{\"Version\":\"v0.0.1\",\"Time\":\"2020-03-17T08:05:12Z\"}",
            Matchers.equalTo(new String(new Remaining(data).bytes()))
        );
    }
}
