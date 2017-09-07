package com.athaydes.logfx.file;

enum UnsuccessfulQueryResult implements FileContentReader.FileQueryResult {

    INSTANCE;

    @Override
    public boolean isSuccess() {
        return false;
    }

    @Override
    public int fileLineNumber() {
        return -1;
    }

    @Override
    public boolean isBeforeRange() {
        return false;
    }

    @Override
    public boolean isAfterRange() {
        return false;
    }

    @Override
    public String toString() {
        return "UnsuccessfulQueryResult{}";
    }
}
