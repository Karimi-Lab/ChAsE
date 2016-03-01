package collab;

/**
 * Track lines define the display attributes for all lines in an annotation data set.
 * more in: http://genome.ucsc.edu/goldenPath/help/customTrack.html#TRACK
 */
public class WigTrack
{
	public String	m_sName = null;
	public String 	m_sDescription = null;
	// not parsing more information from the track lines as I don't have a use for it yet.

	/**
	 * Reads the track data from a line
	 * @param strLine	string containing a track line.
	 */
	void read(String strLine)
	{
		String[] tokens = strLine.split("name\\s*=\\s*\"");
		if (tokens.length >= 2)
		{
			m_sName = tokens[1].split("\"")[0];
		}
		tokens = strLine.split("description\\s*=\\s*\"");
		if (tokens.length >= 2)
		{
			m_sDescription = tokens[1].split("\"")[0];
		}
	}
}