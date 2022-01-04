import java.util.ArrayList;
import java.util.HashMap;

public class Series
{
	private final String name;
	private final ArrayList<String> volumes;
	private final ArrayList<String> chapters;
	private final HashMap<String, String> chapter_volume;
	private final HashMap<Integer, String> page_chapter;
	private int pages;
	
	public Series( String name )
	{
		this.name = name;
		volumes = new ArrayList<>();
		chapters = new ArrayList<>();
		chapter_volume = new HashMap<>();
		page_chapter = new HashMap<>();
	}
	
	@SuppressWarnings( "unused" )
	public String getName()
	{
		return name;
	}
	
	public String getVolume( int page )
	{
		return chapter_volume.get( page_chapter.get( page ) );
	}
	
	@SuppressWarnings( "unused" )
	public ArrayList<String> getVolumes()
	{
		return volumes;
	}
	
	@SuppressWarnings( "unused" )
	public String getChapter( int page )
	{
		return page_chapter.get( page );
	}
	
	@SuppressWarnings( "unused" )
	public ArrayList<String> getChapters()
	{
		return chapters;
	}
	
	public int getNumPages()
	{
		return pages;
	}
	
	public void addPage( String chapter )
	{
		page_chapter.put( pages, chapter );
		pages++;
	}
	
	public void addVolume( String volume )
	{
		if( volumes.contains( volume ) )
		{
			return;
		}
		
		volumes.add( volume );
	}
	
	public void addChapter( String chapter, String volume )
	{
		if( chapters.contains( chapter ) )
		{
			return;
		}
		
		chapters.add( chapter );
		chapter_volume.put( chapter, volume );
	}
	
	public String getLastVolume()
	{
		return volumes.get( volumes.size() - 1 );
	}
	
	public int getNumChapters()
	{
		return chapters.size();
	}
	
	@Override
	public String toString()
	{
		StringBuilder output = new StringBuilder( "Volumes\n" );
		
		for( String volume : volumes )
		{
			output.append( volume ).append( "\n" );
		}
		
		output.append( "\nChapters\n" );
		
		for( String chapter : chapters )
		{
			output.append( chapter ).append( "\n" );
		}
		
		output.append( "\nLast Page: " ).append( pages );
		
		return output.toString();
	}
}
