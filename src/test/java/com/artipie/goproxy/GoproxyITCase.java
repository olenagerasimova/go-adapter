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

import com.artipie.asto.Storage;
import com.artipie.asto.fs.FileStorage;
import com.jcabi.log.Logger;
import io.vertx.reactivex.core.Vertx;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.cactoos.io.OutputTo;
import org.cactoos.io.ResourceOf;
import org.cactoos.io.TeeInput;
import org.cactoos.scalar.LengthOf;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Integration case for {@link Goproxy}.
 *
 * @since 0.1
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class GoproxyITCase {
    /**
     * Test GoProxy works.
     * @param folder Temporary folder for repo storage.
     * @throws Exception If some problem inside
     */
    @Test
    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    public void savesAndLoads(@TempDir final Path folder) throws Exception {
        final Path repo = folder.resolve("repo");
        for (final String file
            : new String[] {"bar.go", "go.mod", "texts/test.txt"}) {
            new LengthOf(
                new TeeInput(
                    new ResourceOf(String.format("bar/%s", file)),
                    new OutputTo(Paths.get(repo.toString(), "foo", "bar", file))
                )
            ).intValue();
        }
        final Vertx vertx = Vertx.vertx();
        final Storage storage = new FileStorage(repo, vertx.fileSystem());
        final Goproxy goproxy = new Goproxy(storage, vertx);
        goproxy.update("example.com/foo/bar", "0.0.123").blockingAwait();
        goproxy.update("example.com/foo/bar", "0.0.124").blockingAwait();
        final Path home = folder.resolve("home");
        Files.createDirectory(home);
        Files.write(
            Paths.get(home.toString(), "go.mod"),
            String.join(
                "\n",
                "module example.com/foo/xxx",
                "require example.com/foo/bar v0.0.124",
                "go 1.11"
            ).getBytes()
        );
        Files.write(
            Paths.get(home.toString(), "test.go"),
            String.join(
                "\n",
                "package main",
                "import \"fmt\"",
                "import \"example.com/foo/bar\"",
                "func main() {",
                    "fmt.Println(\"Hey, you!\")",
                    "bar.SayHello()",
                "}"
            ).getBytes()
        );
        final ProcessBuilder pbuilder = new ProcessBuilder()
            .directory(home.toFile())
            .redirectOutput(folder.resolve("stdout.txt").toFile())
            .redirectErrorStream(true);
        pbuilder.environment().putAll(this.prepareProcessEnvironment(repo));
        this.validateProcessOutput(
            pbuilder.command("go", "install", "-v"),
            "go: downloading example.com/foo/bar v0.0.124"
        );
        this.validateProcessOutput(
            pbuilder.command("go", "run", "test.go"),
            "Hey, you!",
            "Works!!!"
        );
        pbuilder.command("go", "clean", "-modcache").start().waitFor();
    }

    /**
     * Make sure Go is here.
     * @throws Exception If fails
     */
    @BeforeAll
    static void goExists() throws Exception {
        MatcherAssert.assertThat(
            "Go is NOT present at the build machine",
            new ProcessBuilder()
                .command("which", "go")
                .start()
                .waitFor(),
            Matchers.equalTo(0)
        );
    }

    /**
     * Prepare system environment for Go process.
     * See https://github.com/golang/go/issues/27698 for GOPROXY env
     * @param repo Path to repo folder
     * @return System environment variables
     */
    private Map<String, String> prepareProcessEnvironment(final Path repo) {
        final Map<String, String> env = new HashMap<>();
        env.put("GOSUMDB", "off");
        env.put("GOPATH", String.format("%s/gopath", repo));
        env.put("GOPROXY", repo.toUri().toString().replace("///C:", "//C:"));
        return env;
    }

    /**
     * Run command and find substrings in the process output.
     * @param pbuilder ProcessBuilder that pre-configured for Go run
     * @param substrings Templates to be found in the process output
     * @throws Exception
     */
    private void validateProcessOutput(final ProcessBuilder pbuilder,
        final String... substrings) throws Exception {
        pbuilder.start().waitFor();
        final String log = new String(
            Files.readAllBytes(
                pbuilder.redirectOutput().file().toPath()
            )
        );
        Logger.debug(this, "Full stdout/stderr:\n%s", log);
        MatcherAssert.assertThat(
            log,
            Matchers.allOf(
                Arrays.stream(substrings)
                    .map(Matchers::containsString)
                    .collect(Collectors.toList())
            )
        );
    }
}
