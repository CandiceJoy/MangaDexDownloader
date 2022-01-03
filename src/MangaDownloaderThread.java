import java.io.*;
import java.net.URL;

public class MangaDownloaderThread extends Thread
{
	private final Manga manga;
	private final String in_path;
	private final String out_path;
	
	public MangaDownloaderThread( Manga manga_in, String url_in, String out_in )
	{
		this.manga = manga_in;
		this.in_path = url_in;
		this.out_path = out_in;
	}
	
	public void run()
	{
		try
		{
			URL url = new URL( in_path );
			InputStream in = new BufferedInputStream( url.openStream() );
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			byte[] buf = new byte[1024];
			int n;
			while( -1 != ( n = in.read( buf ) ) )
			{
				out.write( buf, 0, n );
			}
			out.close();
			in.close();
			byte[] response = out.toByteArray();
			
			FileOutputStream fos = new FileOutputStream( out_path );
			fos.write( response );
			fos.close();
		}
		catch( IOException e )
		{
			e.printStackTrace();
		}
		
		manga.threadFinished();
	}
}
