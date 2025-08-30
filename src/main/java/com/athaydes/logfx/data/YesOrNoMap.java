package com.athaydes.logfx.data;

import java.util.LinkedHashMap;
import java.util.Map;

public record YesOrNoMap( Runnable onYes, Runnable onNo ) {

    public YesOrNoMap( Runnable onYes ) {
        this( onYes, YesOrNoMap::doNothing );
    }

    public Map<String, Runnable> toMap() {
        var map = new LinkedHashMap<String, Runnable>();
        map.put( "Yes", onYes );
        map.put( "No", onNo );
        return map;
    }

    private static void doNothing() {
    }

}
