import java.util.ArrayList;
import java.util.HashMap;

public class Series
{
	private String name;
	private ArrayList<String> volumes;
	private ArrayList<String> chapters;
	private HashMap<String,String> chapter_volume;
	private HashMap<Integer,String> page_chapter;
	private int pages;
	
	public Series( String name )
	{
		this.name = name;
		volumes = new ArrayList<String>();
		chapters = new ArrayList<String>();
		chapter_volume = new HashMap<String,String>();
		page_chapter = new HashMap<Integer,String>();
	}
	
	public String getName()
	{
		return name;
	}
	
	public String getVolume( int page )
	{
		String chapter = page_chapter.get( page );
		String volume = chapter_volume.get( page );
		
		return volume;
	}
	
	public ArrayList<String> getVolumes()
	{
		return volumes;
	}
	
	public String getChapter( int page )
	{
		String chapter = page_chapter.get( page );
		return chapter;
	}
	
	public ArrayList<String> getChapters()
	{
		return chapters;
	}
	
	public int getNumPages()
	{
		return pages;
	}
	
	public void addPage()
	{
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
	
	public String toString()
	{
		String output = "Volumes\n";
		
		for( String volume : volumes )
		{
			output += volume + "\n";
		}
		
		output += "\nChapters\n";
		
		for( String chapter : chapters )
		{
			output += chapter + "\n";
		}
		
		output += "\nLast Page: " + pages;
		
		return output;
	}
}
