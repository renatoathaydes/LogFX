![LogFX Logo](docs/images/lofx-logo.png)

[ ![Download](https://api.bintray.com/packages/renatoathaydes/maven/logfx/images/download.svg) ](https://bintray.com/renatoathaydes/maven/logfx/_latestVersion)

LogFX is a multi-platform, free and open-source log viewer designed to handle very large files without a performance hit.

It is written in JavaFX so it can run in any Operating System.

[LogFX Website](https://renatoathaydes.github.io/LogFX/)

[Documentation](https://renatoathaydes.github.io/LogFX/docs/index.html)

[Report Issue](https://github.com/renatoathaydes/LogFX/issues/new)

[Send private feedback](https://renatoathaydes.github.io/LogFX/contact.html)

## Getting LogFX

Please choose one of the two alternatives below:

### Stand-alone distributions

LogFX is distributed as a stand-alone application (arond 35MB download, 55MB unpacked).

Simply download the zip file for your distribution, unzip it, then run it with:

```
logfx/bin/logfx
```

[Linux](https://bintray.com/renatoathaydes/linux/logfx)

[Mac](https://bintray.com/renatoathaydes/mac/logfx)

[Windows](https://bintray.com/renatoathaydes/win/logfx)

### Fat Jar (requires Java 11+ with JavaFX)

* download the jar from the command-line:

> Find the latest version on [Bintray](https://bintray.com/renatoathaydes/maven/logfx).

```
VERSION=<latest_version> && \
curl -sSfL https://jcenter.bintray.com/com/athaydes/logfx/logfx/$VERSION/logfx-$VERSION-all.jar -o logfx.jar
```

> Size of the jar as of version `0.6.1`: 289 KB. *Not MB!*
> UPDATE: Version 1.0's jar size: 304KB. 

Run with:

```
java -jar logfx.jar
```

> Hint: to get Java 11 with JavaFX included, use [SDKMAN!](https://sdkman.io/)
> (e.g. `sdk use java 11.0.8.fx-zulu`).

## Screenshots

![LogFX running on Linux KDE](https://raw.githubusercontent.com/renatoathaydes/LogFX/next/docs/images/screenshots/logfx-1.0rc2-linux.png)

More in the [Wiki](https://github.com/renatoathaydes/LogFX/wiki/Screenshots) and on the
[Website](https://renatoathaydes.github.io/LogFX/).
