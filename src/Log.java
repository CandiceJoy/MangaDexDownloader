import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Log
{
	private static final Properties properties = new Properties();
	
	static
	{
		properties.put( "log4j.rootLogger", "ERROR, stdout, FILE" );
		properties.put( "log4j.appender.stdout", "org.apache.log4j.ConsoleAppender" );
		
		properties.put( "log4j.appender.stdout.Target", "System.out" );
		properties.put( "log4j.appender.stdout.Threshold", "INFO" );
		properties.put( "log4j.appender.stdout.layout", "org.apache.log4j.PatternLayout" );
		properties.put( "log4j.appender.stdout.layout.ConversionPattern", "%-5p %m%n" );
		
		properties.put( "log4j.appender.FILE", "org.apache.log4j.FileAppender" );
		properties.put( "log4j.appender.FILE.Threshold", "TRACE" );
		properties.put( "log4j.appender.FILE.File", "./log.log" );
		properties.put( "log4j.appender.FILE.ImmediateFlush", "true" );
		properties.put( "log4j.appender.FILE.Append", "false" );
		properties.put( "log4j.appender.FILE.layout", "org.apache.log4j.PatternLayout" );
		properties.put( "log4j.appender.FILE.layout.conversionPattern", "%d{yyyy-MM-dd HH:mm:ss.SSS} [%t] %-5p %m%n" );
		
		PropertyConfigurator.configure( properties );
		Logger.getRootLogger().setLevel( Level.TRACE );
	}
	
	public static void debug( Object message )
	{
		Logger logger = Logger.getLogger( getCallingClass() );
		logger.debug( "- " + message );
	}
	
	@SuppressWarnings( "unused" )
	public static void trace( Object message )
	{
		Logger logger = Logger.getLogger( getCallingClass() );
		logger.trace( "- " + message );
	}
	
	public static void warn( Object message )
	{
		Logger logger = Logger.getLogger( getCallingClass() );
		logger.warn( "- " + message );
	}
	
	public static void error( Object message )
	{
		Logger logger = Logger.getLogger( getCallingClass() );
		logger.error( getCallingLocation() + " - " + message );
	}
	
	public static void info( Object message )
	{
		Logger logger = Logger.getLogger( getCallingClass() );
		logger.info( "- " + message );
	}
	
	public static void fatal( Object message )
	{
		Logger logger = Logger.getLogger( getCallingClass() );
		logger.fatal( getCallingLocation() + " - " + message );
		System.exit( 0 );
	}
	
	@SuppressWarnings( "rawtypes" )
	private static Class getCallingClass()
	{
		StackWalker walker = StackWalker.getInstance( StackWalker.Option.RETAIN_CLASS_REFERENCE );
		final List<StackWalker.StackFrame> stack = walker.walk(
				( Stream<StackWalker.StackFrame> frames ) -> frames.collect( Collectors.toList() )
		                                                      );
		return stack.get( 2 ).getClass();
	}
	
	private static String getCallingLocation()
	{
		StackWalker walker = StackWalker.getInstance( StackWalker.Option.RETAIN_CLASS_REFERENCE );
		final List<StackWalker.StackFrame> stack = walker.walk(
				( Stream<StackWalker.StackFrame> frames ) -> frames.collect( Collectors.toList() )
		                                                      );
		return stack.get( 2 ).toString();
	}
}
