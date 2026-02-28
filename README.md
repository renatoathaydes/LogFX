![LogFX Logo](docs/images/lofx-logo.png)

❇️ [Download](https://github.com/renatoathaydes/LogFX/releases/latest)

LogFX is a multi-platform, free and open-source log viewer designed to handle very large files without a performance hit.

It is written in JavaFX so it can run on most Operating Systems.

🌐 [LogFX Website](https://renatoathaydes.github.io/LogFX/)

📚 [Documentation](https://renatoathaydes.github.io/LogFX/docs/index.html)

🪲 [Report Issue](https://github.com/renatoathaydes/LogFX/issues/new)

📣 [Send private feedback](https://renatoathaydes.github.io/LogFX/contact.html)

## Getting LogFX

Please choose one of the two alternatives below:

### Stand-alone distributions

LogFX is distributed as a stand-alone application (arond 35MB download, 55MB unpacked).

Download the zip file for your distribution, unzip it, then run it with:

```
logfx/bin/logfx
```

[Linux](https://github.com/renatoathaydes/LogFX/releases/latest) - `logfx-<version>-linux.zip`

[Mac](https://github.com/renatoathaydes/LogFX/releases/latest) - `logfx-<version>-mac.zip`

[Windows](https://github.com/renatoathaydes/LogFX/releases/latest) - `logfx-<version>-windows.zip`

### Jar module (requires Java 25+ with JavaFX)

* download the jar from the command-line or your browser:

> Find the latest version on [Maven Central](https://search.maven.org/search?q=a:logfx).

```
VERSION=<latest_version> && \
  curl https://repo1.maven.org/maven2/com/athaydes/logfx/logfx/$VERSION/logfx-$VERSION-all.jar \
  -o logfx.jar
```

> Size of the jar as of version `0.6.1`: 289 KB. *Not MB!*
> UPDATE: Version 1.0's jar size: 272Kb. 

The only dependency is `slf4j-api` version `2.0.16` (see [build-properties.yaml](resources/build-properties.yaml) for the latest version).

Run with:

```shell
java --module-path runtime-libs/LogFX.jar:runtime-libs/slf4j-api-2.0.16.jar -m com.athaydes.logfx/com.athaydes.logfx.LogFX
```

> Hint: to get a Java version with JavaFX included, use [SDKMAN!](https://sdkman.io/)

If you want the splash screen to show up, use something like this:

```shell
LOGFX_SPLASH_IMAGE=image/bin/logfx-logo.png  java --module-path LogFX.jar:slf4j-api-2.0.16.jar -Djavafx.preloader=com.athaydes.logfx.SplashPreloader -Xms64m -m com.athaydes.logfx/com.athaydes.logfx.LogFX
```

## Screenshots

- Linux KDE

![LogFX running on Linux KDE](https://raw.githubusercontent.com/renatoathaydes/LogFX/next/docs/images/screenshots/logfx-1.0rc2-linux.png)

More in the [Wiki](https://github.com/renatoathaydes/LogFX/wiki/Screenshots) and on the
[Website](https://renatoathaydes.github.io/LogFX/).

## Building

This project is built with [jb](https://renatoathaydes.github.io/jb/) and a JDK (tested with Java 25, probably still works with 17+) that includes JavaFX.

To build LogFX:

```shell
jb jlink
```

The output image is located at `build/image/` unzipped, `build/logfx-<version>.zip` zipped.

The LogFX jar and its dependencies are located at `build/runtime-libs/`.

To test LogFX:

```shell
jb -p src/test compile test
```
