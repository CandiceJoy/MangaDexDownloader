import java.io.File;
import java.util.Comparator;

public class SortFileByNumber implements Comparator<File>
{
	private MangaSettings settings;
	
	public SortFileByNumber( MangaSettings settings_in )
	{
		settings = settings_in;
	}
	
	public int compare( File f1, File f2 )
	{
			int i1 = Manga.getNumberFromFile( f1, settings );
			int i2 = Manga.getNumberFromFile( f2, settings );
			return i1 - i2;
	}
}
