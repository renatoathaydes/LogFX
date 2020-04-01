package com.athaydes.logfx.log;

import com.athaydes.logfx.config.Properties;

import java.nio.file.Path;

public enum LogConfigFile {
    INSTANCE;
    public final Path logFilePath = Properties.LOGFX_DIR.resolve( "logfx.log" );
}
