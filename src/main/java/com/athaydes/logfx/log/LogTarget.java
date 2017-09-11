package com.athaydes.logfx.log;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collection;

import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.WRITE;

/**
 * Log targets.
 */
public interface LogTarget {

    void write( Collection<String> messages );

    class FileLogTarget implements LogTarget {
        @Override
        public void write( Collection<String> messages ) {
            try {
                Files.write( LogFXLogFactory.INSTANCE.getLogFilePath(),
                        messages, StandardCharsets.UTF_8,
                        WRITE, CREATE, APPEND );
            } catch ( IOException e ) {
                e.printStackTrace();
            }
        }
    }

    class PrintStreamLogTarget implements LogTarget {

        private final PrintStream printStream;

        public PrintStreamLogTarget( PrintStream printStream ) {
            this.printStream = printStream;
        }

        @Override
        public void write( Collection<String> messages ) {
            messages.forEach( printStream::println );
        }
    }

}
