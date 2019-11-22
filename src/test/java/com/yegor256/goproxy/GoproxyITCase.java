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

import com.jcabi.log.Logger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Integration case for {@link Goproxy}.
 *
 * @author Yegor Bugayenko (yegor256@gmail.com)
 * @version $Id$
 * @since 0.1
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class GoproxyITCase {

    /**
     * Temp folder for all tests.
     */
    @Rule
    @SuppressWarnings("PMD.BeanMembersShouldSerialize")
    public TemporaryFolder folder = new TemporaryFolder();

    /**
     * RPM works.
     * @throws Exception If some problem inside
     */
    @Test
    public void savesAndLoads() throws Exception {
        final Path repo = this.folder.newFolder("repo").toPath();
        final Storage storage = new Storage.Simple(repo);
        final Goproxy goproxy = new Goproxy(storage);
        final Path mod = Paths.get(repo.toString(), "foo/bar/go.mod");
        mod.toFile().getParentFile().mkdirs();
        Files.write(
            mod,
            String.join(
                "\n",
                "module example.com/foo/bar",
                "go 1.13"
            ).getBytes()
        );
        goproxy.update("example.com/foo/bar", "0.0.123");
        final Path stdout = this.folder.newFile("stdout.txt").toPath();
        final Path home = this.folder.newFolder("home").toPath();
        Files.write(
            Paths.get(home.toString(), "go.mod"),
            String.join(
                "\n",
                "module example.com/foo/xxx",
                "require example.com/foo/bar v0.0.123",
                "go 1.13"
            ).getBytes()
        );
        Files.write(
            Paths.get(home.toString(), "test.go"),
            String.join(
                "\n",
                "package main",
                "import \"example.com/foo/bar\"",
                "func main() {",
                    "fmt.Println(\"Hello World!\")",
                "}"
            ).getBytes()
        );
        new ProcessBuilder()
            .directory(home.toFile())
            .command(
                "/bin/bash",
                "-c",
                String.join(
                    "\n",
                    "set -e",
                    "set -x",
                    String.format("export GOPROXY=file://%s", repo),
                    "go install -v"
                )
            )
            .redirectOutput(stdout.toFile())
            .redirectErrorStream(true)
            .start()
            .waitFor();
        final String log = new String(Files.readAllBytes(stdout));
        Logger.debug(this, "Full stdout/stderr:\n%s", log);
        MatcherAssert.assertThat(
            log,
            Matchers.allOf(
                Matchers.containsString("0.0.123"),
                Matchers.containsString("foo/bar")
            )
        );
    }

}
