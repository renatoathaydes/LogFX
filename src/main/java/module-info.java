import com.athaydes.logfx.log.LogFXLogFactory;
import org.slf4j.ILoggerFactory;

module com.athaydes.logfx {
    requires jdk.unsupported;
    requires org.slf4j;
    requires javafx.controls;
    requires javafx.fxml;
    exports com.athaydes.logfx;
    exports com.athaydes.logfx.log to org.slf4j;
    opens com.athaydes.logfx.ui to javafx.fxml;
    provides ILoggerFactory with LogFXLogFactory;
}