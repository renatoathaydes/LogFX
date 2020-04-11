![LogFX Logo](docs/images/lofx-logo.png)

![Download](https://github.com/renatoathaydes/LogFX/workflows/Build%20Release%20Artifacts/badge.svg)

LogFX is a multi-platform, free and open-source log viewer designed to handle very large files without a performance hit.

It is written in JavaFX so it can run in any Operating System.

Visit the [LogFX Website](https://renatoathaydes.github.io/LogFX/) for the full documentation.

## Getting LogFX

To get LogFX:

* click on the `Build Release Artifacts` button near the top of this page and download the appropriate zip file
for your platform. This zip file contains a self-contained LogFX distribution which includes its own minimal JVM. 

OR

* download the jar from the command-line:

> Find the latest version on [Bintray](https://bintray.com/renatoathaydes/maven/logfx).

```
VERSION=1.0-RC3 && \
curl -sSfL https://jcenter.bintray.com/com/athaydes/logfx/logfx/$VERSION/logfx-$VERSION-all.jar -o logfx.jar
```

> Size of the jar as of version `0.6.1`: 289 KB. *Not MB!*

> UPDATE: Version 0.9.0's jar size: 320KB.
> 
> UPDATE: Version 1.0's jar size: 304KB. 

## Screenshots

![LogFX running on Linux KDE](https://raw.githubusercontent.com/renatoathaydes/LogFX/next/docs/images/screenshots/logfx-1.0rc2-linux.png)

See more screenshots in the [Wiki](https://github.com/renatoathaydes/LogFX/wiki/Screenshots).
