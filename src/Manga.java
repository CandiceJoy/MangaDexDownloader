import kotlin.collections.SetsKt;

import javax.json.*;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.text.DecimalFormat;
import java.util.*;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class Manga
{
	private String uuid;
	private JsonObject json;
	private ArrayList<String> download_urls; //all URLs in series
	private String title;
	private int threads_finished; //# of threads finished, based on downloads.size()
	private ArrayList<MangaDownloaderThread> download_threads; //remaining downloads
	private int next_thread; //index of downloads
	private Series series;
	//private int max_chapter;
	private MangaSettings settings;
	private int start_download_at;
	private int current_download;
	private long start_time;
	private int num_chapters;
	
	//Settings
	private final boolean debug;
	private final String language;
	private final File temp_folder;
	private final File destination;
	private final String manga_ext;
	private final boolean multithreaded;
	private final int max_conns;
	private final String image_ext;
	private final boolean split;
	private final String format;
	private final char symbol;
	private final File library_updater;
	private final DecimalFormat percent_format;
	private final boolean datasaver;
	private final File series_folder;
	private final boolean debug_dont_download;
	private final boolean do_archive;
	
	public Manga( String uuid_in, MangaSettings properties_in )
	{
		uuid = uuid_in;
		settings = properties_in;
		
		if( settings.getDebug( "enabled" ) != null )
		{
			debug = settings.getDebug( "enabled" ).equals( "true" );
		}
		else
		{
			debug = false;
		}
		
		language = settings.get( "language" );
		temp_folder = settings.getFile( "temp_folder" );
		
		if( !temp_folder.exists() )
		{
			temp_folder.mkdirs();
		}
		
		destination = settings.getFile( "destination" );
		manga_ext = settings.get( "manga_file_extension" );
		multithreaded = settings.getBool( "multithreaded" );
		max_conns = settings.getInt( "max_connections" );
		image_ext = settings.get( "image_file_extension" );
		split = settings.getBool( "split_by_volume" );
		format = settings.get( "image_filename_format" );
		symbol = settings.getChar( "image_filename_format_symbol" );
		
		String command = settings.get( "library_updater_command" );
		
		if( command != null )
		{
			library_updater = ( command.equals( "" ) ) ? null : new File( destination + MangaSettings.SLASH + command );
		}
		else
		{
			library_updater = null;
		}
		
		percent_format = new DecimalFormat( settings.get( "percent_format" ) );
		datasaver = settings.getBool( "data_saver" );
		do_archive = settings.getBool( "archive" );
		
		if( settings.getDebug( "dont-download" ) != null )
		{
			debug_dont_download = settings.getDebug( "dont-download" ).equals( "true" );
		}
		else
		{
			debug_dont_download = false;
		}
		
		download_urls = new ArrayList<String>();
		download_threads = new ArrayList<MangaDownloaderThread>();
		fetchMetadata();
		this.title = sanitise( fetchTitle() );
		series = new Series( title );
		System.out.println( "Title: " + title );
		System.out.println( "UUID: " + uuid );
		fetchChapterURLs();
		threads_finished = 0;
		next_thread = 0;
		series_folder = new File( temp_folder + MangaSettings.SLASH + title );
		
		if( debug )
		{
			System.out.println( "Total downloads: " + download_urls.size() );
		}
	}
	
	private void fetchMetadata()
	{
		JsonObject obj = MangaDownloader.query( "https://api.mangadex.org/manga/" + uuid, settings );
		obj = obj.getJsonObject( "data" );
		
		json = obj;
	}
	
	private String sanitise( String in )
	{
		return in.replaceAll( "\\?", "" );
	}
	
	private void fetchChapterURLs()
	{
		JsonObject obj = MangaDownloader.query( "https://api.mangadex.org/manga/" + uuid + "/feed?order[chapter]=asc&order[volume]=asc&limit=500&translatedLanguage[]=" + language, settings );
		JsonArray data = obj.getJsonArray( "data" );
		boolean first = true;
		String baseurl = null;
		String datamode_json;
		String datamode_url;
		
		if( datasaver )
		{
			datamode_json = "dataSaver";
			datamode_url = "data-saver";
		}
		else
		{
			datamode_json = "data";
			datamode_url = "data";
		}

		//Chapter
		for( JsonValue data2 : data )
		{
			JsonObject obj2 = data2.asJsonObject();
			JsonObject attributes = obj2.getJsonObject( "attributes" );
			JsonArray files = attributes.getJsonArray( datamode_json );
			String hash = attributes.getString( "hash" );
			String chapter = attributes.get( "chapter" ).toString().replaceAll( "\"", "" );
			String volume = attributes.get( "volume" ).toString().replaceAll( "\"", "" );
			String chapter_uuid = obj2.getString( "id" );
			
			series.addVolume( volume );
			series.addChapter( chapter, volume );
			
			if( first )
			{
				JsonObject obj3 = MangaDownloader.query( "https://api.mangadex.org/at-home/server/" + chapter_uuid, settings );
				baseurl = obj3.getString( "baseUrl" );
				first = false;
			}
			
			//Page
			for( JsonValue val : files )
			{
				series.addPage();
				download_urls.add( baseurl + "/" + datamode_url + "/" + hash + "/" + val.toString().substring( 1, val.toString().length() - 1 ) );
			}
		}
		
		String lastchap = json.getJsonObject( "attributes" ).get( "lastChapter" ).toString().replaceAll( "\"", "" ).toLowerCase();
		
		try
		{
			int last_chapter = Integer.parseInt( lastchap );
			
			System.out.println( "Last Chapter: " + last_chapter );
			
			if( series.getNumChapters() < last_chapter && series.getNumChapters() < 500 )
			{
				MangaDownloader.warning( "Not all chapters are available" );
			}
		}
		catch( NumberFormatException e )
		{
		}
		
		if( series.getNumChapters() == 500 )
		{
			MangaDownloader.warning( "More than 500 chapters available; only the first 500 will be downloaded" );
		}
		else
		{
			System.out.println( "Available Chapters: " + series.getNumChapters() );
		}
	}
	
	private String fetchTitle()
	{
		JsonObject attributes = json.getJsonObject( "attributes" );
		String status = attributes.getString( "status" );
		
		if( !status.equals( "completed" ) )
		{
			MangaDownloader.warning( "This series is incomplete; status is not 'completed'" );
		}
		
		JsonObject title = attributes.getJsonObject( "title" );
		String title_language = title.getString( language );
		return title_language;
	}
	
	public String getTitle()
	{
		return title;
	}
	
	private void delete( File delete_me )
	{
		File[] allContents = delete_me.listFiles();
		
		if( allContents != null )
		{
			for( File file : allContents )
			{
				delete( file );
			}
		}
		
		while( delete_me.exists() )
		{
			delete_me.delete();
		}
	}
	
	private boolean shouldDoDownload() //true, false, or halt program if nothing to do
	{
		File manga_out = new File( destination + MangaSettings.SLASH + title + manga_ext ); //Output if not splitting
		File lastvol = new File( destination + MangaSettings.SLASH + title + " " + series.getLastVolume() + manga_ext ); //Last file if splitting
		
		if( split && lastvol != null && lastvol.exists() )
		{
			if( series_folder.exists() )
			{
				System.out.println( "Manga already exists, but so does the download...re-archiving" );
				return false;
			}
			else
			{
				System.out.println( "Manga already exists, no need to download it again :)" );
				System.exit( 0 );
			}
		}
		
		if( !split && manga_out.exists() )
		{
			if( series_folder.exists() )
			{
				System.out.println( "Manga already exists, but so does the download...re-archiving" );
				return false;
			}
			else
			{
				System.out.println( "Manga already exists, no need to download it again :)" );
				System.exit( 0 );
			}
		}
		
		return true;
	}
	
	private void populateDownloadStart()
	{
		int current = 0;
		
		if( !series_folder.exists() )
		{
			series_folder.mkdirs();
			start_download_at = 0;
			return;
		}
		
		for( int x = 0; x < download_urls.size(); x++ )
		{
			String filepath = temp_folder + MangaSettings.SLASH + title + MangaSettings.SLASH + getFilename( x ) + image_ext;
			File f = new File( filepath );
			
			if( !f.exists() )
			{
				current = getNumberFromFile( f, settings );
				System.out.println( "Download interrupt detected; resuming (the ETA may not be accurate)..." );
				start_download_at = current;
				return;
			}
		}
		
		System.out.println( "Entire download detected, skipping download..." );
		start_download_at = Integer.MIN_VALUE;
		current_download = start_download_at;
	}
	
	private void downloadMultithreaded()
	{
		populateDownloadStart();
		
		if( start_download_at == Integer.MIN_VALUE )
		{
			return;
		}
		
		for( int x = start_download_at; x < download_urls.size(); x++ )
		{
			String path = download_urls.get( x );
			String out = temp_folder + MangaSettings.SLASH + title + MangaSettings.SLASH + getFilename( x ) + image_ext;
			download_threads.add( new MangaDownloaderThread( this, path, out ) );
		}
		
		int start = 0;
		int end = Math.min( max_conns, download_threads.size() ) - 1;
		
		for( int x = start; x <= end; x++ )
		{
			download_threads.get( x ).start();
		}
		
		next_thread = end + 1;
		
		boolean still_alive;
		
		do
		{
			still_alive = false;
			
			for( MangaDownloaderThread mangaDownloaderThread : download_threads )
			{
				if( mangaDownloaderThread.isAlive() )
				{
					still_alive = true;
				}
			}
		}while( still_alive );
	}
	
	private void download()
	{
		populateDownloadStart();
		
		if( start_download_at == Integer.MIN_VALUE )
		{
			return;
		}
		
		for( int x = start_download_at; x < download_urls.size(); x++ )
		{
			String path = download_urls.get( x );
			
			try
			{
				URL url = new URL( path );
				InputStream in = new BufferedInputStream( url.openStream() );
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				byte[] buf = new byte[1024];
				int n = 0;
				while( -1 != ( n = in.read( buf ) ) )
				{
					out.write( buf, 0, n );
				}
				out.close();
				in.close();
				byte[] response = out.toByteArray();
				
				FileOutputStream fos = new FileOutputStream( temp_folder + MangaSettings.SLASH + title + MangaSettings.SLASH + getFilename( start_download_at ) + image_ext );
				fos.write( response );
				fos.close();
			}
			catch( IOException e )
			{
				e.printStackTrace();
			}
			
			current_download++;
			MangaDownloader.printProgress( start_time, current_download, download_urls.size() );
		}
	}
	
	public void process()
	{
		System.out.println( series );
		
		if( shouldDoDownload() ) //ends the program if nothing to be done
		{
			if( debug_dont_download )
			{
				MangaDownloader.fatalError( "Debug: Don't download" );
				System.exit( 0 );
			}
			
			System.out.println( "Downloading..." );
			start_time = System.currentTimeMillis();
			
			if( multithreaded )
			{
				downloadMultithreaded();
			}
			else
			{
				download();
			}
		}
		
		if( do_archive )
		{
			archive();
			updateLibrary();
		}
		
		if( !debug )
		{
			delete( series_folder );
		}
	}
	
	private void archive()
	{
		start_time = System.currentTimeMillis();
		
		if( !split )
		{
			//Single Archive
			System.out.println( "Archiving..." );
			String path = destination + MangaSettings.SLASH + title + manga_ext;
			File output = new File( path );
			
			File[] files = new File( temp_folder + MangaSettings.SLASH + title ).listFiles();
			archive( files, path, 0 );
		}
		else
		{
			//Multiple Archives
			int pages = series.getNumPages();
			
			String current_chapter = null;
			String current_volume = series.getVolume( 0 );
			
			File[] current_files = new File[0];
			int files_done = 0;
			
			for( int page = 0; page < pages; page++ )
			{
				String chapter = series.getChapter( page );
				String volume = series.getVolume( page );
				
				if( volume == current_volume )
				{
					File[] old = new File[current_files.length];
					System.arraycopy( current_files, 0, old, 0, current_files.length );
					current_files = new File[old.length + 1];
					System.arraycopy( old, 0, current_files, 0, old.length );
					current_files[current_files.length - 1] = new File( temp_folder + MangaSettings.SLASH + title + MangaSettings.SLASH + format.replaceAll( String.valueOf( symbol ), String.valueOf( page ) ) + image_ext );
				}
				else
				{
					archive( current_files, destination + MangaSettings.SLASH + title + " " + current_volume + manga_ext, files_done );
					current_volume = volume;
					files_done += current_files.length;
					current_files = new File[0];
				}
			}
			
			archive( current_files, destination + MangaSettings.SLASH + title + " " + current_volume + manga_ext, files_done );
		}
		
		System.out.println( "Archiving complete!  You can find your manga in " + destination.getAbsolutePath() );
	}
	
	private void updateLibrary()
	{
		if( library_updater != null && library_updater.exists() )
		{
			System.out.println( "Updating library..." );
			
			ProcessBuilder pb = new ProcessBuilder( "cmd", "/k", library_updater.getAbsolutePath() );
			File dir = destination;
			
			try
			{
				pb.inheritIO();
				Process p = pb.start();
			}
			catch( IOException e )
			{
				e.printStackTrace();
			}
		}
	}
	
	private void archive( File[] files, String path, int start_at )
	{
		File dest = new File( path );
		String temp_path = temp_folder + MangaSettings.SLASH + dest.getName();
		File src = new File( temp_path );
		
		Map<String, String> env = new HashMap<>();
		env.put( "create", "true" );
		
		try( FileSystem zipfs = FileSystems.newFileSystem( Paths.get( temp_path ), env ) )
		{
			int count = 0;
			
			for( File file : files )
			{
				Path externalTxtFile = Paths.get( file.getPath() );
				Path pathInZipfile = zipfs.getPath( MangaSettings.SLASH + file.getName() );
				// copy a file into the zip file
				Files.copy( externalTxtFile, pathInZipfile,
				            StandardCopyOption.REPLACE_EXISTING );
				
				count++;
				MangaDownloader.printProgress( start_time, start_at + count, download_urls.size() );
			}
		}
		catch( Exception e )
		{
			e.printStackTrace();
		}
		
		src.renameTo( dest );
	}
	
	public static int getNumberFromFile( File file, MangaSettings settings )
	{
		String format = settings.get( "image_filename_format" );
		String image_ext = settings.get( "image_file_extension" );
		char symbol = settings.getChar( "image_filename_format_symbol" );
		
		String pattern = format + image_ext;
		String name = file.getName();
		
		int difference = name.length() - pattern.length() + 1;
		
		int start = pattern.indexOf( symbol );
		int end = start + difference;
		
		return Integer.parseInt( name.substring( start, end ) );
	}
	
	private String getFilename( int number )
	{
		return format.replaceAll( String.valueOf( symbol ), String.valueOf( number ) );
	}
	
	synchronized void threadFinished()
	{
		if( !multithreaded )
		{
			return;
		}
		
		synchronized( this )
		{
			threads_finished++;
			
			MangaDownloader.printProgress( start_time, start_download_at + threads_finished, download_urls.size() );
			
			if( next_thread < download_threads.size() )
			{
				download_threads.get( next_thread ).start();
				next_thread++;
			}
		}
	}
}