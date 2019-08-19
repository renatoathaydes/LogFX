![LogFX Logo](docs/images/lofx-logo.png)

[ ![Download](https://api.bintray.com/packages/renatoathaydes/maven/logfx/images/download.svg) ](https://bintray.com/renatoathaydes/maven/logfx/_latestVersion)


A log viewer capable of reading large files without a performance hit.

It is written in JavaFX so it can run in any Operating System.

## Features

* extremely fast to open and navigate large files.
* tail file(s) with option to pause at any time.
* go to date-time in any log file (or all opened files).
* highlight text using regular expressions rules.
* filter content based on highlight expressions.
* highly customizable look via JavaFX CSS (refreshes instantly).
* keyboard friendly (shortcuts for everything).

## Getting LogFX

To get LogFX:

* click on the `Download` button near the top of this page, then open the `Files` tab, 
choose the logfx-x.x-all.jar file... notice the `all` qualifier...

OR

* download the jar from the command-line:

```
curl -sSfL https://jcenter.bintray.com/com/athaydes/logfx/logfx/0.9.1/logfx-0.9.1-all.jar -o logfx.jar
```

> Size of the jar as of version `0.6.1`: 289 KB. *Not MB!*

> UPDATE: Version 0.9.0's jar size: 320KB. 

## Running LogFX

> Java 8+ is required to run LogFX

Run it with:

```
java -jar logfx.jar
```

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

Notice that the `logfx.stylesheet.file` allows you to specify your own stylesheet to customize the looks of LogFX.

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
