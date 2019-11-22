[![DevOps By Rultor.com](http://www.rultor.com/b/yegor256/goproxy-java)](http://www.rultor.com/p/yegor256/goproxy-java)
[![We recommend IntelliJ IDEA](https://www.elegantobjects.org/intellij-idea.svg)](https://www.jetbrains.com/idea/)

[![Build Status](https://img.shields.io/travis/yegor256/goproxy-java/master.svg)](https://travis-ci.org/yegor256/goproxy-java)
[![Javadoc](http://www.javadoc.io/badge/com.yegor256/goproxy.svg)](http://www.javadoc.io/doc/com.yegor256/goproxy)
[![License](https://img.shields.io/badge/license-MIT-green.svg)](https://github.com/yegor256/goproxy/blob/master/LICENSE.txt)
[![Hits-of-Code](https://hitsofcode.com/github/yegor256/goproxy-java)](https://hitsofcode.com/view/github/yegor256/goproxy-java)
[![Maven Central](https://img.shields.io/maven-central/v/com.yegor256/goproxy.svg)](https://maven-badges.herokuapp.com/maven-central/com.yegor256/goproxy)
[![PDD status](http://www.0pdd.com/svg?name=yegor256/goproxy-java)](http://www.0pdd.com/p?name=yegor256/goproxy-java)

This Java library turns your storage
(files, S3 objects, anything) with Go sources into
a Go repository.

Similar solutions:

  * [Artifactory](https://www.jfrog.com/confluence/display/RTF/Go+Registry)

Some valuable references:

  * [Module proxy protocol](https://golang.org/cmd/go/#hdr-Module_proxy_protocol)
  * [Why you should use a Go module proxy](https://arslan.io/2019/08/02/why-you-should-use-a-go-module-proxy/)
  * [Go Modules Are Awesome, But There Is One Tiny Problem](https://jfrog.com/blog/go-modules-are-awesome-but-there-is-one-tiny-problem/)

This is the dependency you need:

```xml
<dependency>
  <groupId>com.yegor256</groupId>
  <artifactId>goproxy</artifactId>
  <version>[...]</version>
</dependency>
```

Then, you implement `com.yegor256.goproxy.Storage` interface
and pass it to the instance of `com.yegor256.goproxy.Goproxy`. Then, you
let it know when is the right moment to update certain artifact:

```java
Goproxy goproxy = new Goproxy(storage);
goproxy.update("foo/bar", "0.0.1");
```

Read the [Javadoc](http://www.javadoc.io/doc/com.yegor256/goproxy)
for more technical details.

Should work.

## How to contribute

Fork repository, make changes, send us a pull request. We will review
your changes and apply them to the `master` branch shortly, provided
they don't violate our quality standards. To avoid frustration, before
sending us your pull request please run full Maven build:

```
$ mvn clean install -Pqulice
```

To avoid build errors use Maven 3.2+.
