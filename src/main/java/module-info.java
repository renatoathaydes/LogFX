module logfx {
    requires javafx.base;
    requires javafx.controls;
    requires javafx.web;

    // TODO use a FileWatcher.Modifier that is not unsupported one day...
    requires jdk.unsupported;

    requires slf4j.api;

    // must be exported to the JavaFX runtime
    exports com.athaydes.logfx;

    // must be exported to our main package
    exports org.slf4j.impl;
}