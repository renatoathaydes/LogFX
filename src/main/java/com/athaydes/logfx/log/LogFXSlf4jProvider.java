package com.athaydes.logfx.log;

import org.slf4j.ILoggerFactory;
import org.slf4j.helpers.SubstituteServiceProvider;

public class LogFXSlf4jProvider extends SubstituteServiceProvider {
    @Override
    public ILoggerFactory getLoggerFactory() {
        return new LogFXLogFactory();
    }

    @Override
    public String getRequestedApiVersion() {
        return "2.0";
    }
}
