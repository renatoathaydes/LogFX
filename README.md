![LogFX Logo](docs/images/lofx-logo.png)

[ ![Download](https://api.bintray.com/packages/renatoathaydes/maven/logfx/images/download.svg) ](https://bintray.com/renatoathaydes/maven/logfx/_latestVersion)


A log viewer capable of reading large files without a performance hit.

It is written in JavaFX so it can run in any Operating System.

Visit the [LogFX Website](https://renatoathaydes.github.io/LogFX/) for the full documentation.

## Getting LogFX

To get LogFX:

* click on the `Download` button near the top of this page, then open the `Files` tab, 
choose the logfx-x.x-all.jar file... notice the `all` qualifier...

OR

* download the jar from the command-line:

> Find the latest version on [Bintray](https://bintray.com/renatoathaydes/maven/logfx).

```
VERSION=1.0-RC3
curl -sSfL https://jcenter.bintray.com/com/athaydes/logfx/logfx/$VERSION/logfx-$VERSION-all.jar -o logfx.jar
```

> Size of the jar as of version `0.6.1`: 289 KB. *Not MB!*

> UPDATE: Version 0.9.0's jar size: 320KB.
> 
> UPDATE: Version 1.0's jar size: 284KB. 

## Running LogFX

> Java 8+ is required to run LogFX

Run LogFX with the following command:

```
java -jar logfx.jar
```

### Java 9+

On Java 9+, there are two ways to run LogFX:

#### Using an OpenJDK distribution that includes JavaFX
 
If you use a JDK distribution that includes JavaFX (e.g. with [SDKMAN](https://sdkman.io/), `sdk use java 14.0.0.fx-librca`),
the same command as with Java 8 can be used to run LogFX:

> You can also download a OpenJDK version which includes JavaFX from [BellSoft](https://bell-sw.com/pages/java-14/)
> or [Azul](https://www.azul.com/downloads/zulu-community/).

```
java -jar logfx.jar
```

#### Using a standard Java 9+ JVM and a separate JavaFX distribution

If you want to use a standard Java 9+ JVM, as it does not include JavaFX by default since Java 9,
you will need to download the JavaFX runtime from [openjfx.io](https://openjfx.io/).

Unpack the contents of the zip file into some directoy (`JAVAFX_DIST`), then run LogFX with the following command:

```
java --module-path $JAVAFX_DIST --add-modules javafx.controls,javafx.fxml -jar logfx.jar
```

### Limit RAM used

If you don't want it to use the default hundreds of MB of RAM, ask `java` to use at most 50MB and it will run fine:

```
java -Xmx50m -jar logfx.jar
```

## Screenshots

See screenshots in the [Wiki](https://github.com/renatoathaydes/LogFX/wiki/Screenshots).

## System properties

LogFX allows customizing certain behaviours using system properties.

The following properties are currently recognized at startup:

* `logfx.home` - home directory (`~/.logfx/`) by default.
* `logfx.stylesheet.file` - custom stylesheet file location.
* `logfx.stylesheet.norefresh` - set this to any value to stop LogFX from watching the custom stylesheet file.
* `logfx.log.target` - where to send LogFX's own log (`file|sysout|syserr`).
* `logfx.log.level` - log level for LogFX's own log (`trace|debug|info|warn|error`).

To specify a different home for LogFX (say, `/temp/logfx`), for example, start LogFX with this command:

```
java -Dlogfx.home=/temp/logfx -jar logfx.jar
```

This allows you to store several different LogFX customizations in the same machine. 

### Specifying a custom stylesheet

Notice that the `logfx.stylesheet.file` system property allows you to specify your own stylesheet to customize
the looks of LogFX.

The default stylesheet can be found at [src/main/resources/css/LogFX.css](src/main/resources/css/LogFX.css).

The most interesting element is the `.root`, which lets you set the theme-colour as well as the UI icons' colours:

```css
.root {
    -fx-base: #1d1d1d;
    -icons-color: rgb(61, 114, 144);
}
```

You can also increase the padding between log lines, as another example:

```css
.log-line {
    -fx-padding: 2, 5, 2, 5;
}

```

To see some possibilities, check the [Screenshots](https://github.com/renatoathaydes/LogFX/wiki/Screenshots) in the Wiki.
