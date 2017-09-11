package com.athaydes.logfx.log;

import com.athaydes.logfx.config.Properties;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MarkerIgnoringBase;
import org.slf4j.helpers.MessageFormatter;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

class LogFXLogger extends MarkerIgnoringBase {

    private static final DateTimeFormatter formatter = new DateTimeFormatterBuilder()
            .appendValue( ChronoField.YEAR )
            .appendLiteral( '-' )
            .appendValue( ChronoField.MONTH_OF_YEAR )
            .appendLiteral( '-' )
            .appendValue( ChronoField.DAY_OF_MONTH )
            .appendLiteral( 'T' )
            .appendValue( ChronoField.HOUR_OF_DAY )
            .appendLiteral( ':' )
            .appendValue( ChronoField.MINUTE_OF_HOUR )
            .appendLiteral( ':' )
            .appendValue( ChronoField.SECOND_OF_MINUTE )
            .appendLiteral( '.' )
            .appendValue( ChronoField.MILLI_OF_SECOND )
            .appendOffsetId()
            .toFormatter();

    private static final LogTarget logTarget = Properties.getLogTarget()
            .orElse( new LogTarget.FileLogTarget() );

    private final String name;

    LogFXLogger( String name ) {
        this.name = name;
    }

    @Override
    public boolean isTraceEnabled() {
        return LogFXLogFactory.INSTANCE.isLogLevelEnabled( LogLevel.TRACE );
    }

    @Override
    public void trace( String msg ) {
        log( LogLevel.TRACE, msg, null );
    }

    @Override
    public void trace( String format, Object arg ) {
        formatAndLog( LogLevel.TRACE, format, arg );
    }

    @Override
    public void trace( String format, Object arg1, Object arg2 ) {
        formatAndLog( LogLevel.TRACE, format, arg1, arg2 );
    }

    @Override
    public void trace( String format, Object... arguments ) {
        formatAndLog( LogLevel.TRACE, format, arguments );
    }

    @Override
    public void trace( String msg, Throwable t ) {
        log( LogLevel.TRACE, msg, t );
    }

    @Override
    public boolean isDebugEnabled() {
        return LogFXLogFactory.INSTANCE.isLogLevelEnabled( LogLevel.DEBUG );
    }

    @Override
    public void debug( String msg ) {
        log( LogLevel.DEBUG, msg, null );
    }

    @Override
    public void debug( String format, Object arg ) {
        formatAndLog( LogLevel.DEBUG, format, arg );
    }

    @Override
    public void debug( String format, Object arg1, Object arg2 ) {
        formatAndLog( LogLevel.DEBUG, format, arg1, arg2 );
    }

    @Override
    public void debug( String format, Object... arguments ) {
        formatAndLog( LogLevel.DEBUG, format, arguments );
    }

    @Override
    public void debug( String msg, Throwable t ) {
        log( LogLevel.DEBUG, msg, t );
    }

    @Override
    public boolean isInfoEnabled() {
        return LogFXLogFactory.INSTANCE.isLogLevelEnabled( LogLevel.INFO );
    }

    @Override
    public void info( String msg ) {
        log( LogLevel.INFO, msg, null );
    }

    @Override
    public void info( String format, Object arg ) {
        formatAndLog( LogLevel.INFO, format, arg );
    }

    @Override
    public void info( String format, Object arg1, Object arg2 ) {
        formatAndLog( LogLevel.INFO, format, arg1, arg2 );
    }

    @Override
    public void info( String format, Object... arguments ) {
        formatAndLog( LogLevel.INFO, format, arguments );
    }

    @Override
    public void info( String msg, Throwable t ) {
        log( LogLevel.INFO, msg, t );
    }

    @Override
    public boolean isWarnEnabled() {
        return LogFXLogFactory.INSTANCE.isLogLevelEnabled( LogLevel.WARN );
    }

    @Override
    public void warn( String msg ) {
        log( LogLevel.WARN, msg, null );
    }

    @Override
    public void warn( String format, Object arg ) {
        formatAndLog( LogLevel.WARN, format, arg );
    }

    @Override
    public void warn( String format, Object... arguments ) {
        formatAndLog( LogLevel.WARN, format, arguments );
    }

    @Override
    public void warn( String format, Object arg1, Object arg2 ) {
        formatAndLog( LogLevel.WARN, format, arg1, arg2 );
    }

    @Override
    public void warn( String msg, Throwable t ) {
        log( LogLevel.WARN, msg, t );
    }

    @Override
    public boolean isErrorEnabled() {
        return LogFXLogFactory.INSTANCE.isLogLevelEnabled( LogLevel.ERROR );
    }

    @Override
    public void error( String msg ) {
        log( LogLevel.ERROR, msg, null );
    }

    @Override
    public void error( String format, Object arg ) {
        formatAndLog( LogLevel.ERROR, format, arg );
    }

    @Override
    public void error( String format, Object arg1, Object arg2 ) {
        formatAndLog( LogLevel.ERROR, format, arg1, arg2 );
    }

    @Override
    public void error( String format, Object... arguments ) {
        formatAndLog( LogLevel.ERROR, format, arguments );
    }

    @Override
    public void error( String msg, Throwable t ) {
        log( LogLevel.ERROR, msg, t );
    }

    private void formatAndLog( LogLevel logLevel, String format, Object... arguments ) {
        if ( !LogFXLogFactory.INSTANCE.isLogLevelEnabled( logLevel ) ) {
            return;
        }

        FormattingTuple tp = MessageFormatter.arrayFormat( format, arguments );
        log( logLevel, tp.getMessage(), tp.getThrowable() );
    }

    private void log( LogLevel logLevel, String message, Throwable throwable ) {
        if ( !LogFXLogFactory.INSTANCE.isLogLevelEnabled( logLevel ) ) {
            return;
        }

        StringBuilder builder = new StringBuilder( 128 );
        builder.append( formatter.format( Instant.now().atZone( ZoneId.systemDefault() ) ) )
                .append( " [" ).append( Thread.currentThread().getName() ).append( ']' )
                .append( ' ' ).append( logLevel.name() )
                .append( ' ' ).append( name )
                .append( " - " ).append( message );

        Collection<String> messageLines;

        if ( throwable != null ) {
            StackTraceElement[] stackTrace = throwable.getStackTrace();
            messageLines = new ArrayList<>( 2 + stackTrace.length );

            messageLines.add( builder.toString() );
            messageLines.add( throwable.toString() );

            for ( StackTraceElement element : stackTrace ) {
                messageLines.add( "  at " + element );
            }
        } else {
            messageLines = Collections.singleton( builder.toString() );
        }

        logTarget.write( messageLines );
    }

}
