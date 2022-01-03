import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class MangaDownloader
{
	public static final File SETTINGS_FILE = new File( "manga.properties" );
	public static final String[] SUPPORTED_OUTPUT_EXTENSIONS = new String[]{ ".cbz", ".zip" };
	private static final MangaSettings settings = new MangaSettings( SETTINGS_FILE );
	private static final int error_blank_lines = 1;
	private static final int error_width_padding = 5;
	
	public static void main( String[] str )
	{
		MangaSettings properties = new MangaSettings( SETTINGS_FILE );
		
		String manga_file_extension = settings.get( "manga_file_extension" );
		int max_connections = settings.getInt( "max_connections" );
		String uuid = settings.getDebug( "uuid" );
		boolean clipboard = settings.getBool( "clipboard" );
		
		if( !arrayContains( MangaDownloader.SUPPORTED_OUTPUT_EXTENSIONS, manga_file_extension ) )
		{
			String formats = "";
			
			for( String format : MangaDownloader.SUPPORTED_OUTPUT_EXTENSIONS )
			{
				formats += format + " ";
			}
			
			fatalError( manga_file_extension + " is not a supported output format; only the following formats are supported: " + formats );
			System.exit( 0 );
		}
		
		if( max_connections > 10 )
		{
			fatalError( "More than 10 simultaneous connections not allowed!!!" );
			System.exit( 0 );
		}
		
		if( uuid == null )
		{
			if( clipboard )
			{
				Clipboard c = Toolkit.getDefaultToolkit().getSystemClipboard();
				Transferable t = c.getContents( null );
				String data = null;
				
				if( t.isDataFlavorSupported( DataFlavor.stringFlavor ) )
				{
					try
					{
						Object o = t.getTransferData( DataFlavor.stringFlavor );
						data = (String) t.getTransferData( DataFlavor.stringFlavor );
					}
					catch( IOException | UnsupportedFlavorException e )
					{
					}
				}
				
				if( isValidUUID( data ) )
				{
					uuid = data;
				}
				else
				{
					uuid = searchFor( data );
				}
			}
			
			if( uuid == null )
			{
				uuid = getUUIDFromCLI();
			}
		}
		
		Manga manga = new Manga( uuid, properties );
		manga.process();
	}
	
	private static boolean arrayContains( Object[] haystack, Object needle )
	{
		for( Object o : haystack )
		{
			if( o.equals( needle ) )
			{
				return true;
			}
		}
		
		return false;
	}
	
	private static String getUUIDFromCLI()
	{
		String uuid = null;
		boolean success = false;
		
		while( !success )
		{
			System.out.print( "Enter Manga UUID or Title ==> " );
			
			try
			{
				uuid = new BufferedReader( new InputStreamReader( System.in ) ).readLine();
			}
			catch( IOException e )
			{
				e.printStackTrace();
			}
			
			if( isValidUUID( uuid ) )
			{
				success = true;
			}
			else
			{
				uuid = searchFor( uuid );
				
				if( uuid != null )
				{
					success = true;
				}
			}
			
			if( !success )
			{
				System.out.println( "Invalid UUID" );
				continue;
			}
		}
		
		return uuid;
	}
	
	private static boolean isValidUUID( String uuid )
	{
		if( uuid == null )
		{
			return false;
		}
		
		try
		{
			UUID.fromString( uuid );
		}
		catch( IllegalArgumentException e )
		{
			return false;
		}
		
		return true;
	}
	
	public static void warning( String str )
	{
		System.out.println( "!Warning! " + str );
	}
	
	public static void error( String str )
	{
		System.out.println( "!!!ERROR!!! " + str );
	}
	
	public static void fatalError( String str )
	{
		String error_text = "FATAL ERROR";
		
		if( str.length() < error_text.length() )
		{
			str = padString( str, error_text.length() );
		}
		
		int total_width = str.length() + 2 + ( error_width_padding * 2 );
		int error_start = total_width / 2 - error_text.length() / 2;
		
		System.out.println( getHeader( str ) );
		blankLines( str );
		System.out.println( getError( error_text, error_start, total_width ) );
		System.out.println( getMessage( str ) );
		blankLines( str );
		System.out.println( getHeader( str ) );
		System.exit( 0 );
	}
	
	private static String getError( String error, int start, int total_width )
	{
		String prebuffer = getRepeating( start - 1, " " );
		String postbuffer = getRepeating( total_width - start - 2 - error_width_padding * 2, " " );
		
		return "|" + prebuffer + error + postbuffer + "|";
	}
	
	private static String getHeader( String str )
	{
		String header = "";
		
		for( int x = 0; x < str.length() + ( 2 * error_width_padding ) + 2; x++ )
		{
			header += "-";
		}
		
		return header;
	}
	
	private static String getMessage( String str )
	{
		return "|" + getPadding( error_width_padding ) + str + getPadding( error_width_padding ) + "|";
	}
	
	private static String getPadding( int num )
	{
		return getRepeating( num, " " );
	}
	
	private static String getPadding( String str )
	{
		return getPadding( str.length() );
	}
	
	private static String getRepeating( int num, String str )
	{
		String out = "";
		
		for( int x = 0; x < num; x++ )
		{
			out += str;
		}
		
		return out;
	}
	
	private static void blankLines( String str )
	{
		for( int x = 0; x < error_blank_lines; x++ )
		{
			System.out.println( "|" + getPadding( str.length() + ( 2 * error_width_padding ) ) + "|" );
		}
	}
	
	private static String padString( String str, int min_length )
	{
		int padding = (int) Math.ceil( (double) ( min_length - str.length() ) / 2.0 );
		
		return getRepeating( padding, " " ) + str + getRepeating( padding, " " );
	}
	
	public static void printProgress( long startTime, long current, long total )
	{
		double scalar = 0.5; //percentage of 100 in progress bar width
		
		long eta = current == 0 ? 0 :
		           ( total - current ) * ( System.currentTimeMillis() - startTime ) / current;
		
		String etaHms = current == 0 ? "N/A" :
		                String.format( "%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours( eta ),
		                               TimeUnit.MILLISECONDS.toMinutes( eta ) % TimeUnit.HOURS.toMinutes( 1 ),
		                               TimeUnit.MILLISECONDS.toSeconds( eta ) % TimeUnit.MINUTES.toSeconds( 1 ) );
		
		StringBuilder string = new StringBuilder( 140 );
		int progress = (int) ( ( current * 100 / total ) ); //fine
		
		int pre_spaces = progress == 0 ? 2 : 2 - (int) ( Math.log10( progress ) ); //fine
		int equals = (int) Math.ceil( ( progress ) * scalar );
		int post_spaces = (int) Math.floor( ( 100 - progress ) * scalar );
		
		string
				.append( '\r' )
				.append( String.join( "", Collections.nCopies( pre_spaces, " " ) ) )
				.append( String.format( " %d%% [", progress ) )
				.append( String.join( "", Collections.nCopies( equals, "=" ) ) )
				.append( '>' )
				.append( String.join( "", Collections.nCopies( post_spaces, " " ) ) )
				.append( ']' )
				.append( String.join( "", Collections.nCopies( current == 0 ? (int) ( Math.log10( total ) ) : (int) ( ( (int) ( Math.log10( total ) ) - (int) ( Math.log10( current ) ) ) ), " " ) ) )
				.append( String.format( " %d/%d, ETA: %s", current, total, etaHms ) );
		
		System.out.print( string );
		
		if( current == total )
		{
			System.out.println();
		}
	}
	
	public static JsonObject query( String url_in, MangaSettings settings )
	{
		boolean debug;
		
		if( settings.getDebug( "enabled" ) != null )
		{
			debug = settings.getDebug( "enabled" ).equals( "true" );
		}
		else
		{
			debug = false;
		}
		
		try
		{
			URL url = new URL( url_in );
			
			if( debug )
			{
				System.out.println( "Query: " + url );
			}
			
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod( "GET" );
			conn.setRequestProperty( "Accept", "application/json" );
			
			if( conn.getResponseCode() != 200 )
			{
				MangaDownloader.error( "HTTP error code : " + conn.getResponseCode() );
			}
			
			JsonReader reader = Json.createReader( conn.getInputStream() );
			JsonObject obj = reader.readObject();
			conn.disconnect();
			
			if( debug )
			{
				System.out.println( "Response: " + obj );
			}
			
			return obj;
		}
		catch( MalformedURLException e )
		{
			e.printStackTrace();
		}
		catch( IOException e )
		{
			e.printStackTrace();
		}
		
		throw new RuntimeException( "Could not get Json from URL" );
	}
	
	private static String searchFor( String manga )
	{
		JsonObject root = query( "https://api.mangadex.org/manga/?title=" + URLEncoder.encode( manga, Charset.defaultCharset() ) + "&order[relevance]=desc&limit=1&availableTranslatedLanguage[]=" + settings.get( "language" ), settings );
		int total = root.getInt( "total" );
		
		if( total <= 0 )
		{
			return null;
		}
		
		JsonArray data = root.getJsonArray( "data" );
		String uuid = data.get(0).asJsonObject().get( "id" ).toString();
		uuid = uuid.substring( 1, uuid.length() - 1 );
		return uuid;
	}
}
