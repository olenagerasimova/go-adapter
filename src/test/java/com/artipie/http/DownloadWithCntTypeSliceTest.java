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
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.http.hm.RsHasBody;
import com.artipie.http.hm.RsHasHeaders;
import com.artipie.http.rq.RequestLine;
import io.reactivex.Flowable;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.cactoos.list.ListOf;
import org.cactoos.map.MapEntry;
import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.AllOf;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link DownloadWithCntTypeSlice}.
 * @since 0.3
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
class DownloadWithCntTypeSliceTest {

    @Test
    void responsesWithZip() throws ExecutionException, InterruptedException, IOException {
        final Storage storage = new InMemoryStorage();
        final String path = "example.com/foo/bar/@v/v0.0.1.zip";
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            zos.putNextEntry(new ZipEntry("bar.go"));
            zos.write("hello world".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }
        storage.save(new Key.From(path), new Content.From(baos.toByteArray())).get();
        final String cnttype = "application/zip";
        MatcherAssert.assertThat(
            new DownloadWithCntTypeSlice(storage, cnttype).response(
                new RequestLine("GET", path, "HTTP/1.1").toString(),
                Collections.emptyList(), Flowable.empty()
            ),
            new AllOf<>(
                new ListOf<Matcher<? super Response>>(
                    new RsHasBody(baos.toByteArray()),
                    new RsHasHeaders(
                        new ListOf<Map.Entry<String, String>>(
                            new MapEntry<>("content-type", cnttype)
                        )
                    )
                )
            )
        );
    }

}
