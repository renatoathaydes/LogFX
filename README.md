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

### Fat Jar (requires Java 16+ with JavaFX)

* download the jar from the command-line or your browser:

> Find the latest version on [Maven Central](https://search.maven.org/search?q=a:logfx).

```
VERSION=<latest_version> && \
  curl https://repo1.maven.org/maven2/com/athaydes/logfx/logfx/$VERSION/logfx-$VERSION-all.jar \
  -o logfx.jar
```

> Size of the jar as of version `0.6.1`: 289 KB. *Not MB!*
> UPDATE: Version 1.0's jar size: 316Kb. 

Run with:

```
java -jar logfx.jar
```

> Hint: to get Java 16 with JavaFX included, use [SDKMAN!](https://sdkman.io/)

## Screenshots

- Linux KDE

![LogFX running on Linux KDE](https://raw.githubusercontent.com/renatoathaydes/LogFX/next/docs/images/screenshots/logfx-1.0rc2-linux.png)

More in the [Wiki](https://github.com/renatoathaydes/LogFX/wiki/Screenshots) and on the
[Website](https://renatoathaydes.github.io/LogFX/).
