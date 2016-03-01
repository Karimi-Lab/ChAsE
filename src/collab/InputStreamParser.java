package collab;

import java.io.IOException;
import java.io.InputStream;

/**
 * General simple parser to read lines of strings from a stream.
 * It buffers the input for performace.
 * TODO: Add support for threading.
 */
public class InputStreamParser
{
	final int 	BUFFER_SIZE 	= 1 << 20;// maximum read buffer size
	final int 	BUFFER_CARRY_SIZE = 1 << 10; // maximum size to carry over incomplete lines to next read
	InputStream m_InputStream 	= null;
	byte[] 		m_Buffer 		= new byte[BUFFER_SIZE + BUFFER_CARRY_SIZE];
	int 		m_iNumRead 		= 0;
	int 		m_iCarryStart 	= 0;
	int 		m_iEnd 			= 0;
	int 		m_iStart 		= 0;
	public long m_iTotalRead 	= 0;
	
	/**
	 * Constructor
	 * @param in	input stream
	 */
	InputStreamParser(InputStream in)
	{
		m_InputStream = in;
	}
	
	/**
	 * Returns the next line from the stream
	 * @return		next line in the stream. null if it has reached the end of the stream
	 */
	String readLine()
	{
		while (m_iNumRead != -1) 
		{
			while(m_iEnd < m_iCarryStart + m_iNumRead)
			{
				if (m_Buffer[m_iEnd] == 13 || m_Buffer[m_iEnd] == 10) // carriage return or line feed.
				{
					String strLine = new String(m_Buffer, m_iStart, m_iEnd - m_iStart);
					m_iEnd++;
					m_iStart = m_iEnd;
					return strLine;
				}
				m_iEnd++;
			}
			
			m_iCarryStart = m_iEnd - m_iStart;
			if (m_iCarryStart > 0)
			{
				System.arraycopy(m_Buffer, m_iStart, m_Buffer, 0, m_iEnd - m_iStart);
			}
			
			try
			{
				if ((m_iNumRead = m_InputStream.read(m_Buffer, m_iCarryStart, BUFFER_SIZE)) == -1)
				{
					if (m_iEnd > m_iStart)
					{
						return new String(m_Buffer, m_iStart, m_iEnd - m_iStart); // return last line
					}
					return null;
				}
			} catch (IOException e)
			{
				m_iNumRead = -1;
				e.printStackTrace();
			}
			
			m_iEnd = 0;
			m_iStart = 0;
			if (BioReader.m_bVerbose)
			{
				System.out.println("iTotalRead = " + m_iTotalRead + " + " + m_iNumRead);
			}
			m_iTotalRead += m_iNumRead;
		}
		return null;
	}
}