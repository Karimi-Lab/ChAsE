/**
 * Downloader.java
 * Abstract: Utility for copying files from the Internet to local disk
 * inspired by: http://www.javaworld.com/javatips/jw-javatip19.html
 * Date:
 */

package org.sfu.chase.collab;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.*;
import java.net.*;
import java.sql.Date;

public class Downloader {
	
	protected int m_Progress;
	protected PropertyChangeSupport m_PropertyChangeSupport;
	protected String PROGRESS_PREFIX = "Progress:";
	protected String m_ProgressMessage = PROGRESS_PREFIX;

	public Downloader() {
		m_Progress = 0;
		m_PropertyChangeSupport = new PropertyChangeSupport(this);
	}
	
	public static void main(String args[]) {
		if (args.length < 1)
		{
			System.err.println("usage: java Downloader URL [LocalPath]");
			System.exit(1);
		}
		try {
			Downloader d = new Downloader();
			d.download(new URL(args[0]), args.length > 1 ? new File(args[1]) : null);
		} 
		catch (MalformedURLException e)
		{
			e.printStackTrace();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * Returns true if file was downloaded or already exists
	 * 
	 * @param url
	 * @param path
	 * @return
	 */
	public void download(URL url, File path) throws IOException
	{
		// System.out.println("Opening connection to " + url + "...");
		URLConnection urlC = url.openConnection();
		int slashIndex = url.getPath().lastIndexOf('/');
		String baseName = url.getPath().substring(slashIndex + 1);
		if (baseName.endsWith("wig.gz")) {
			m_ProgressMessage = PROGRESS_PREFIX + "Downloading data file";
		} else if (baseName.endsWith(".dat")) {
			m_ProgressMessage = PROGRESS_PREFIX + "Downloading preprocessed data file";
		} else if (baseName.endsWith(".stats")) {
			m_ProgressMessage = PROGRESS_PREFIX + "Downloading statistics file";
		} else {
			m_ProgressMessage = PROGRESS_PREFIX + "Downloading " + baseName;
		}
		firePropertyChange(m_ProgressMessage, m_Progress, 0);
		m_Progress = 0;

		// urlC.getContentLength() returns an int, so will not work for getting size of large files;
		long iSize = 0;
		String candidateSize = urlC.getHeaderField("content-length");
		if (candidateSize != null) {
			try {
				iSize = Long.parseLong(candidateSize);
			} catch (NumberFormatException ex) {
				throw new IOException("Problem downloading file from " + url.toString());	
			}
		} else {
			throw new IOException("File for download has size: " + candidateSize + " (" + url.toString() + ")");
		}
		// System.out.print("Copying resource (type: " + urlC.getContentType());
		Date date = new Date(urlC.getLastModified());
		// System.out.println(", size:" + iSize + ", modified on: " + date.toString() + ")...");
		// System.out.flush();

		// create local file
		File localFile;
		File sourceFile = new File(url.getFile());
		if (path != null)
		{
			// check if the local directory exists, and create if it doesn't
			if (!path.exists()) 
			{
				path.mkdir();
			}
			else 
			{
				if (!path.isDirectory()) 
				{
					throw new IOException("local path must be a directory (not a file)");
				} 
			}
			localFile = new File(path, sourceFile.getName());
		}
		else
		{
			localFile = new File(sourceFile.getName());
		}

		// check if the local file exists and has the same size/date as the url. 
		if (localFile.exists()) 
		{
			if (localFile.length() == iSize)
			{
				if ((new Date(localFile.lastModified())).equals(date))
				{
					// System.out.println("file exists already and has same size and date");
					return;
				}
			}
		}

		// create input buffer
		final int BUFFER_SIZE = 1 << 20; // 1MB buffer
		byte[] buffer = new byte[BUFFER_SIZE];
		int iNumRead = 0;
		double count = 0;

		// start downloading and writing to file
		InputStream is = url.openStream();
		FileOutputStream fos = new FileOutputStream(localFile);
		while ((iNumRead = is.read(buffer)) != -1)
		{
			fos.write(buffer, 0, iNumRead);
			count += iNumRead;
			int progress = (int) ((count*100)/iSize);
			firePropertyChange(m_ProgressMessage, m_Progress, progress);
			// System.out.println(m_Progress + " " + progress);
			m_Progress = progress;
		}
		is.close();
		fos.close();
		// preserve modification date
		localFile.setLastModified(date.getTime());
		// System.out.println(count + " byte(s) downloaded");
	}
	
	
    public void addPropertyChangeListener(PropertyChangeListener p) {
    	m_PropertyChangeSupport.addPropertyChangeListener(p);
    }
    
    public void removePropertyChangeListener(PropertyChangeListener p) {
    	m_PropertyChangeSupport.removePropertyChangeListener(p);
    }
    
    protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
    	m_PropertyChangeSupport.firePropertyChange(propertyName, oldValue, newValue);
    }
}
