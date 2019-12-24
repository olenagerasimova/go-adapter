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

import com.yegor256.asto.Storage;
import io.reactivex.rxjava3.core.Completable;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;
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
 * {@link Goproxy#update(String,String)}. This method will update all the
 * necessary meta-data files. Right after this, your clients will be able to use
 * the sources, via {@code go get}:
 *
 * <pre> goproxy.update("example.com/foo/bar", "0.0.1").blockingAwait();</pre>
 *
 * You can also do the same in an async way:
 *
 * <pre> goproxy.update("example.com/foo/bar", "0.0.1").subscribe();</pre>
 *
 * That's it.
 *
 * @author Yegor Bugayenko (yegor256@gmail.com)
 * @version $Id$
 * @since 0.1
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 * @checkstyle LineLengthCheck (500 lines)
 * @checkstyle ReturnCountCheck (500 lines)
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
     * @return Completion or error signal.
     */
    public Completable update(final String repo, final String version) {
        return Completable.defer(
            () -> {
                final Path mod = Files.createTempFile("", ".rpm");
                final String[] parts = repo.split("/", 2);
                final Path zip = Files.createTempFile("", ".zip");
                final Path json = Files.createTempFile("", ".json");
                final Path list = Files.createTempFile("", ".list");
                final String lkey = String.format("%s/@v/list", repo);
                return this.storage.load(
                    String.format("%s/go.mod", parts[1]),
                    mod
                ).andThen(
                    this.storage.save(
                        String.format("%s/@v/v%s.mod", repo, version),
                        mod
                    )
                ).andThen(
                    this.archive(
                        String.format("%s/", parts[1]),
                        String.format("%s@v%s", repo, version),
                        zip
                    )
                ).andThen(
                    this.storage.save(
                        String.format("%s/@v/v%s.zip", repo, version),
                        zip
                    )
                ).andThen(
                    Completable.fromAction(
                        () -> Files.write(
                            json,
                            String.format(
                                "{\"Version\":\"v%s\",\"Time\":\"2019-06-28T10:22:31Z\"}",
                                version
                            ).getBytes()
                        )
                    )).andThen(
                    this.storage.save(
                        String.format("%s/@v/v%s.info", repo, version),
                        json
                    )
                ).andThen(this.storage.exists(lkey))
                    .flatMapCompletable(
                        exists -> {
                            if (exists) {
                                return this.storage.load(lkey, list);
                            } else {
                                return Completable.complete();
                            }
                        })
                    .andThen(
                        Completable.fromAction(
                            () -> Files.write(
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
                            )
                        )
                    ).andThen(this.storage.save(lkey, list));
            });
    }

    /**
     * Make ZIP archive.
     * @param prefix The prefix
     * @param target The path in the ZIP archive to place files to
     * @param zip The ZIP archive
     * @return Completion or error signal.
     */
    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    private Completable archive(final String prefix,
        final String target, final Path zip) {
        return Completable.fromAction(
            () -> {
                if (zip.toFile().exists()) {
                    Files.delete(zip);
                }
            }).andThen(this.storage.list(prefix))
            .flatMapCompletable(
                keys -> {
                    final ZipOutputStream out =
                        new ZipOutputStream(new FileOutputStream(zip.toFile()));
                    final Path tmp = Files.createTempFile("", ".tmp");
                    return Completable.concat(
                        keys.stream().map(
                            key -> this.storage.load(key, tmp)
                                .andThen(
                                    Completable.fromAction(
                                        () -> {
                                            final String path = String.format(
                                                "%s/%s",
                                                target,
                                                key.substring(prefix.length())
                                            );
                                            final ZipEntry entry =
                                                new ZipEntry(path);
                                            out.putNextEntry(entry);
                                            new LengthOf(
                                                new TeeInput(
                                                    new InputOf(tmp),
                                                    new OutputTo(out)
                                                )
                                            ).intValue();
                                            out.closeEntry();
                                        }
                                    )
                                )
                        ).collect(Collectors.toList())
                    ).doOnTerminate(out::close);
                });
    }

}
