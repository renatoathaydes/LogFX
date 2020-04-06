import com.athaydes.logfx.log.LogFXSlf4jProvider;
import org.slf4j.spi.SLF4JServiceProvider;

module com.athaydes.logfx {
    requires jdk.unsupported;
    requires java.desktop;
    requires org.slf4j;
    requires javafx.controls;
    requires javafx.swing;
    exports com.athaydes.logfx;
    exports com.athaydes.logfx.log to org.slf4j;
    opens com.athaydes.logfx.ui to javafx.fxml;
    provides SLF4JServiceProvider with LogFXSlf4jProvider;
}