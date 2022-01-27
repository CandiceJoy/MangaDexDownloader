import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Log
{
	public static void debug( Object message )
	{
		Logger logger = LogManager.getLogger( getCallingClass() );
		logger.debug( "- " + message );
	}
	
	@SuppressWarnings( "unused" )
	public static void trace( Object message )
	{
		Logger logger = LogManager.getLogger( getCallingClass() );
		logger.trace( "- " + message );
	}
	
	public static void warn( Object message )
	{
		Logger logger = LogManager.getLogger( getCallingClass() );
		logger.warn( "- " + message );
	}
	
	public static void error( Object message )
	{
		Logger logger = LogManager.getLogger( getCallingClass() );
		logger.error( getCallingLocation() + " - " + message );
	}
	
	public static void info( Object message )
	{
		Logger logger = LogManager.getLogger( getCallingClass() );
		logger.info( "- " + message );
	}
	
	public static void fatal( Object message )
	{
		Logger logger = LogManager.getLogger( getCallingClass() );
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
