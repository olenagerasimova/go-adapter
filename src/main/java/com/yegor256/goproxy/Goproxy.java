/**
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
package com.yegor256.goproxy;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.cactoos.io.InputOf;
import org.cactoos.io.OutputTo;
import org.cactoos.io.TeeInput;
import org.cactoos.list.Joined;
import org.cactoos.list.ListOf;
import org.cactoos.scalar.LengthOf;

/**
 * The Go front.
 *
 * First, you make an instance of this class, providing
 * your storage as an argument:
 *
 * <pre> Goproxy goproxy = new Goproxy(storage);</pre>
 *
 * Then, you put your Go sources to the storage and call
 * {@link Goproxy#update(String,String)}. This method will parse the RPM package
 * and update all the necessary meta-data files. Right after this,
 * your clients will be able to use the package, via {@code yum}:
 *
 * <pre> rpm.update("nginx.rpm");</pre>
 *
 * That's it.
 *
 * @author Yegor Bugayenko (yegor256@gmail.com)
 * @version $Id$
 * @since 0.1
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
public final class Goproxy {

    /**
     * The storage.
     */
    private final Storage storage;

    /**
     * Ctor.
     * @param stg The storage
     */
    public Goproxy(final Storage stg) {
        this.storage = stg;
    }

    /**
     * Update the meta info by this artifact.
     *
     * @param repo The name of the repo just updated, e.g. "example.com/foo/bar"
     * @param version The version of the repo, e.g. "0.0.1"
     * @throws IOException If fails
     */
    public void update(final String repo, final String version)
        throws IOException {
        synchronized (this.storage) {
            final Path mod = Files.createTempFile("", ".rpm");
            final String[] parts = repo.split("/", 2);
            this.storage.load(
                String.format("%s/go.mod", parts[1]),
                mod
            );
            this.storage.save(
                String.format("%s/@v/v%s.mod", repo, version),
                mod
            );
            final Path zip = Files.createTempFile("", ".zip");
            this.archive(
                String.format("%s/", parts[1]),
                String.format("%s@v%s", repo, version),
                zip
            );
            this.storage.save(
                String.format("%s/@v/v%s.zip", repo, version),
                zip
            );
            final Path json = Files.createTempFile("", ".json");
            Files.write(
                json,
                String.format(
                    "{\"Version\":\"v%s\",\"Time\":\"2019-06-28T10:22:31Z\"}",
                    version
                ).getBytes()
            );
            this.storage.save(
                String.format("%s/@v/v%s.info", repo, version),
                json
            );
            final Path list = Files.createTempFile("", ".list");
            final String lkey = String.format("%s/@v/list", repo);
            if (this.storage.exists(lkey)) {
                this.storage.load(lkey, list);
            }
            Files.write(
                list,
                new org.cactoos.text.Joined(
                    "\n",
                    new Joined<String>(
                        new ListOf<>(
                            new String(Files.readAllBytes(list)).split("\n")
                        ),
                        new ListOf<>(String.format("v%s", version))
                    )
                ).asString().getBytes()
            );
            this.storage.save(lkey, list);
        }
    }

    /**
     * Make ZIP archive.
     * @param prefix The prefix
     * @param target The path in the ZIP archive to place files to
     * @param zip The ZIP archive
     * @throws IOException If fails
     */
    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    private void archive(final String prefix,
        final String target, final Path zip) throws IOException {
        final Map<String, String> env = new HashMap<>(1);
        env.put("create", "true");
        if (zip.toFile().exists()) {
            Files.delete(zip);
        }
        try (final ZipOutputStream out =
            new ZipOutputStream(new FileOutputStream(zip.toFile()))) {
            final Path tmp = Files.createTempFile("", ".tmp");
            for (final String key : this.storage.list(prefix)) {
                this.storage.load(key, tmp);
                final String path = String.format(
                    "%s/%s", target, key.substring(prefix.length())
                );
                final ZipEntry entry = new ZipEntry(path);
                out.putNextEntry(entry);
                new LengthOf(
                    new TeeInput(
                        new InputOf(tmp),
                        new OutputTo(out)
                    )
                ).intValue();
                out.closeEntry();
            }
        }
    }

}
