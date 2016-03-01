/**
 * BioReader.java
 * Abstract: Provides the functionality to read gff and wig files, bin and create a double[][] matrix of data
 * Author: Hamidreza Younesy
 * Date:
 */

package collab;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.log4j.Logger;
import org.broad.igv.bbfile.BBFileReader;
import org.broad.igv.bbfile.BigWigIterator;
import org.broad.igv.bbfile.WigItem;
import org.broad.tribble.util.SeekableBufferedStream;
import org.broad.tribble.util.SeekableStream;
import org.broad.tribble.util.SeekableStreamFactory;

import chase.input.ProgressDialog;

//- import parameters.Parameters;
//- import util.MessageUtils;

/**
 * Provides the functionality to read gff and wig files, bin and create a double[][] matrix of data
 * The wiggle (WIG) format is for display of dense, continuous data such as GC percent, probability scores, and transcriptome data.
 */
public class BioReader
{
	private static Logger log = Logger.getLogger(BioReader.class);
	
	// Stores the features for all chromosomes
	protected ChromFeatures 	m_ChromFeatures;
	
	// toggles, message output (debug) 
	protected static boolean m_bVerbose 	= false;
	protected boolean		 m_bPrintProgress = false;
	
	// Track line for the wig file
	protected WigTrack				m_WigTrack = null;
	protected String				m_FileName = null;

	protected DataStats				m_GlobalStats = null; // global stats for one wig file (based on all reads in the wig file)
	protected DataStats 			m_RegionStats = null; // region stats for one wig file (based on the regions specified in the gff file)
	
	protected double [][] 			m_DataTable = null;   // final computed data table. size: rows=#regions. columns=#bins
	protected double [][] 			m_ReadCountTable = null;   // number of reads in each bin. size: rows=#regions. columns=#bins
		
	// protected int m_Progress;
	protected PropertyChangeSupport m_PropertyChangeSupport;
	protected static String PROGRESS_PREFIX = ProgressDialog.PROGRESS_PREFIX;
	protected String m_ProgressMessage = PROGRESS_PREFIX;
	
	public static final String NORM_NONE = "none";
	public static final String NORM_LINEAR = "linear";
	public static final String NORM_GAUSSIAN = "exp";
	
	// binning types
	public static final String DIVIDE_BY_SIZE = "bysize";
	public static final String DIVIDE_BY_COUNT = "bycount";
	
	protected int m_MinRegionSize = Integer.MAX_VALUE;
	private int m_MaxRegionSize = Integer.MIN_VALUE;

	
	public BioReader() {
		m_ChromFeatures = new ChromFeatures();
		m_PropertyChangeSupport = new PropertyChangeSupport(this);
	}


	public int getNumFeatures()
	{
		return m_ChromFeatures.getNumFeatures();
	}
	
	void computeDataTable()
	{
		//DataStats st = regionStats != null ? regionStats : new DataStats();
		m_RegionStats = new DataStats();
		
		int iNumCols = m_ChromFeatures.getNumBins();
    	int iNumRows = m_ChromFeatures.getNumFeatures();
    	m_DataTable = new double[iNumRows][iNumCols];
    	m_ReadCountTable = new double[iNumRows][iNumCols];
    	
    	m_RegionStats.m_dMin = Double.MAX_VALUE; // min > 0 value
    	m_RegionStats.m_dMax = -Double.MAX_VALUE; // max > 0 value 
    	m_RegionStats.m_dStdDev = 0;
    	m_RegionStats.m_dMean = 0;
    	m_RegionStats.m_iCount = 0; // number of > 0 values
    	
		// fill in the table with the bin values
    	Feature[] features = getFeatures();
		for (int i = 0; i < features.length; i++)
		{
			Feature f = features[i];
			if (f.m_DataBins != null)
			{
				boolean bFlip = f.m_cStrand == '-';
				for (int j = 0; j < iNumCols; j++)
				{
					int iBinIndex = bFlip ? iNumCols - j - 1 : j;
					double dValue = f.m_DataBins[iBinIndex];
					double dCount = f.m_BinCount[iBinIndex];
					
					/* this division will be done later in applyBinningType, to allow user to choose binning type in runtime
					if (dCount > 0)
					{
						// if (m_GlobalStats.m_iNumZeros > 0)
						// {
						dValue /= dCount;  // divide the bin value by the number of reads for that bin
						// }
						// else
						// {
						// 	dValue /= f.getBinSize(); // divide the bin value by the bin-size
						// }
					}
					*/
					
					m_DataTable[i][j] = dValue;
					m_ReadCountTable[i][j] = dCount;
					m_RegionStats.addValue(dValue, 1);
				}
			}
		}
		m_RegionStats.calcFinalStats();
	}
	
	/**
	 * Creates a new normalized table with the specified normalization type and stats
	 * @param table			table to be normalized
	 * @param normType		normalization type: NONE, GAUSSIAN, LINEAR
	 * @param normStats		stats used to normalize the data.
	 * @return 				new table with the normalized values.
	 */
	public static double[][] createNormalizedTable(double[][] table, String normType, DataStats normStats)
	{
		// create a copy of input table
    	int iRows = table.length;
    	int iCols = table[0].length;
    	double[][] result = new double[iRows][iCols];
		for (int iR = 0; iR < iRows; iR++)
		{
			System.arraycopy(table[iR], 0, result[iR], 0, iCols);
		}
		
		// normalize
		normalizeTable(result, normType, normStats);
		
		return result;
	}

	/**
	 * Applies the binning by dividing either to the bin size or count of each bin. Also computing the regional stats
	 * @param dataTable
	 * @param countTable
	 * @param binSizes
	 * @param binningType
	 * @param outputRegionalStats  optional output: if not null, will return calculated regional stats
	 * @return
	 */
	public static double[][] applyBinningType(double[][] dataTable, double[][] countTable, double[] binSizes, String binningType, DataStats outputRegionalStats)
	{
    	int iRows = dataTable.length;
    	int iCols = dataTable[0].length;
    	double[][] resultTable = new double[iRows][iCols];
    	
    	boolean bDivideBySize = binningType.equalsIgnoreCase(DIVIDE_BY_SIZE);
    	
		for (int iR = 0; iR < iRows; iR++)
		{
			for (int iC = 0; iC < iCols; iC++)
			{
				double dValue = dataTable[iR][iC];
				if (!bDivideBySize)
				{
					double dCount = countTable[iR][iC];
					if (dCount > 0)
					{
						dValue /= dCount;  // divide the bin value by the number of reads for that bin
					}
				}
				else
				{
					dValue /= binSizes[iR]; // divide the bin value by the bin-size
				}
				resultTable[iR][iC] = dValue;
				
				if (outputRegionalStats != null) {
					outputRegionalStats.addValue(dValue, 1);
				}
			}
		}
		
		if (outputRegionalStats != null) {
			outputRegionalStats.calcFinalStats();
		}
		
		return resultTable;
	}
	
	/**
	 * Returns the values without any normalization
	 * @return		values in the double[][] matrix form
	 */
	public double [][] getDataTable()
	{
		return m_DataTable;
	}
	
	/**
	 * Returns the read counts per bin
	 * @return		read counts in the double[][] matrix form (can be non-integer when regionSize/numBins is not integer)
	 */
	public double [][] getReadCountTable()
	{
		return m_ReadCountTable;
	}
	

	/**
	 * Returns the array of all features/regions.
	 * @return	array of references to all features.
	 */
	public Feature[] getFeatures()
	{
		return m_ChromFeatures.getFeatures();
	}
	
	public DataStats getGlobalStats() {
		return m_GlobalStats;
	}
	
	public DataStats getRegionStats() {
		return m_RegionStats;
	}

	/**
	 * Returns the data file's sample name.
	 * @return  sample name string
	 */
	public String getSampleName() 
	{
		if (m_WigTrack != null) {
			return m_WigTrack.m_sName;
		} else {
			return null;
		}
	}
	
	/**
	 * Returns the array of all sequence names.
	 * @return	string array of all sequence names for the features.
	 */
	String[] getSeqNames()
	{
		Feature[] features = getFeatures();
    	String [] seqNames = new String[features.length];
		for (int i = 0; i < features.length; i++)
		{
			seqNames[i] = features[i].m_sSeqName;
		}
    	return seqNames;
	}
	
	/**
	 * Normalizes the table with the specified normalization type and stats
	 * @param table			table to be normalized
	 * @param normType		normalization type: NONE, GAUSSIAN, LINEAR
	 * @param normStats		stats used to normalize the data.
	 * @return 				the normalized values will replace the values in the input table
	 */
	public static void normalizeTable(double[][] table, String normType, DataStats normStats)
	{
		if (normType.equals(NORM_GAUSSIAN) && normStats.m_dStdDev == 0) {
			log.error("StdDev == 0, Gaussian normalization won't be applied.");
		}
		
    	int iNumRows = table.length;
    	int iNumCols = table[0].length;
    	
    	if (normType.equals(NORM_LINEAR))
    	{
    		double dRange = normStats.m_dMax - normStats.m_dMin;
    		dRange = dRange == 0 ? 1.0 : dRange;

          	for (int i = 0; i < iNumRows; i++)
          	{
          		for (int j = 0; j < iNumCols; j++)
          		{
          			table[i][j] = Math.max(table[i][j] - normStats.m_dMin, 0) / dRange;
          		}
          	}
    	}
    	else if (normType.equals(NORM_GAUSSIAN))
    	{
    	    //double minNorm = Double.MAX_VALUE;
    	    //double maxNorm = -Double.MAX_VALUE;
    	    boolean isNegative = (normStats.m_dMean < 0);
          	for (int i = 0; i < iNumRows; i++)
          	{
          		for (int j = 0; j < iNumCols; j++)
          		{
          			double dValue = table[i][j];
                    if (isNegative) {
                        dValue *= -1;
                    }
          			
          			if (dValue != 0)
          			{
          				if (normStats.m_dStdDev > 0) {
	        				double exponent = -(dValue - normStats.m_dMedian) / normStats.m_dStdDev;
	        				table[i][j] = 1.0 / (1 + Math.exp(exponent));
	          			} else {
                            //double exponent = -(dValue - normStats.m_dMedian);
                            //table[i][j] = 1.0 / (1 + Math.exp(exponent));
	          				table[i][j] = 1.0;
	          			}
          			}
          			
                    if (isNegative) {
                        table[i][j] = 1 - table[i][j];
                    }
          			
                    //minNorm = Math.min(table[i][j], minNorm);
                    //maxNorm = Math.max(table[i][j], maxNorm);
          		}
          	}
          	
          	// normalize the normalized values between 0 and 1
          	/*if (maxNorm > minNorm) {
                for (int i = 0; i < iNumRows; i++)
                {
                    for (int j = 0; j < iNumCols; j++)
                    {
                        table[i][j] = (table[i][j] - minNorm) / (maxNorm - minNorm);
                    }
                }
          	}*/
    	}
	}

	
	/**
	 * Reads a GFF file  and populates the feature information in the m_ChromMap
	 * @param fileName	gff filename
	 */
	public void readGFF(String fileName) throws InterruptedException, IOException
	{
		m_FileName = fileName;
		InputStream streamGFF = readURL(fileName); // first try if the filename is a net url
		if (streamGFF == null)
		{
			streamGFF = new FileInputStream(fileName);
		}
		readGFF(streamGFF);
		streamGFF.close();
	}
	
	/**
	 * Reads a GFF file from an input stream and populates the feature information in the m_ChromMap
	 * @param streamIn	input stream
	 * @throws InterruptedException 
	 */
	public void readGFF(InputStream streamIn) throws IOException, InterruptedException
	{	
		// http://genome.ucsc.edu/FAQ/FAQformat.html#format3
		// http://www.sequenceontology.org/gff3.shtml
		// 	seqname	source	feature	start	end	score	strand	frame	group
		//	1	ucsc	TSS	4502	10503	.	-	.	NR_028269

		//for (int i = 0; i < 22; i++)
		//m_ChromMap.put("chr" + (i + 1), new Integer(i));
		//m_ChromMap.put("chrX", new Integer(22));
		//m_ChromMap.put("chrY", new Integer(23));
		
		BufferedReader reader = new BufferedReader(new InputStreamReader(streamIn));
		String strLine = null;
		
		m_MinRegionSize = Integer.MAX_VALUE;
		m_MaxRegionSize = Integer.MIN_VALUE;
		
		int inputOrder = 0;
		
		// read the features
    	while ((strLine = reader.readLine()) != null)
		{
    		if (checkForInterrupt()) {
    			return;
    		}
    		
    		if (strLine.startsWith("track")) {
				continue;
			}
    		
			if (strLine.startsWith("#")) {
				continue;
			}
			
			if (strLine.replace(" ", "").equals("")) {
				continue;
			}

			// According to the spec, GFF lines have nine required fields that must be tab-separated.
			String[] tokens = strLine.split("[\t]");
			
			boolean bEnforceGFFStandard = false;
			
			if (bEnforceGFFStandard && tokens.length != 8 && tokens.length != 9) 
			{
				String err = "Expect either 8 or 9 fields in the GFF file; found " + tokens.length;
				log.error(err);
				throw new IOException(err);
			}
			
			if (!bEnforceGFFStandard && tokens.length < 3) 
			{
				String err = "Expect at least 3 fields in the region file (chr# start end); found " + tokens.length;
				log.error(err);
				throw new IOException(err);
			}

			Feature f = new Feature();
			f.m_sStrLine = strLine;

			if (tokens.length <= 7)
			{// to handle a special (invalid) case
				f.m_sSeqName 	= tokens[0].length() > 3 ? tokens[0] : "chr"+tokens[0];
				f.m_iStart		= Integer.valueOf(tokens[1]);
				f.m_iEnd		= Integer.valueOf(tokens[2]);
				if (tokens.length > 3)
					f.m_cStrand		= tokens[3].equals(".") | tokens[3].equals("1") ? '+' : tokens[3].charAt(0);
				if (tokens.length > 4)
					f.m_sGroup		= tokens[4];
				if (tokens.length > 5)
					f.m_sGroup		+= " " + tokens[5];
				if (tokens.length > 6)
					f.m_sGroup		+= " " + tokens[6];
			}
			else
			{// 
				f.m_sSeqName 	= tokens[0].length() > 3 ? tokens[0] : "chr"+tokens[0];
				f.m_sSource 	= tokens[1];
				f.m_sFeature 	= tokens[2];
				f.m_iStart		= Integer.valueOf(tokens[3]);
				f.m_iEnd		= Integer.valueOf(tokens[4]);
				f.m_fScore		= tokens[5].equals(".") ? null : Float.parseFloat(tokens[5]);
				f.m_cStrand		= tokens[6].equals(".") | tokens[6].equals("1") ? '+' : tokens[6].charAt(0);
				f.m_iFrame		= tokens[7].equals(".") ? null : Integer.parseInt(tokens[7]);
				if (tokens.length == 9) 
				{
					f.m_sGroup		= tokens[8];
				}
				else 
				{
					f.m_sGroup = null;
				}
			}

			f.m_InputOrder = inputOrder++;
			try
			{
				f.m_iSeqIndex = Integer.parseInt(f.m_sSeqName.replaceFirst("chr", ""));
			} catch (NumberFormatException e) {
				f.m_iSeqIndex = -1;
			}
			
			try
			{
				f.m_GroupOrder = Integer.parseInt(f.m_sGroup);
			} catch (NumberFormatException e) {
				f.m_GroupOrder = -1;
			}

			m_MinRegionSize = Math.min(m_MinRegionSize, f.m_iEnd - f.m_iStart);
			m_MaxRegionSize = Math.max(getMaxRegionSize(), f.m_iEnd - f.m_iStart);

			m_ChromFeatures.add(f);

			if (m_bVerbose)
			{
				System.out.println(strLine);
			}
		}

    	if (m_bPrintProgress)
    	{
			System.out.println(" min region size = " + getMinRegionSize());
			System.out.println(" max region size = " + getMaxRegionSize());
    	}
    	
    	// sort the features for all chromosomes
    	m_ChromFeatures.sortFeatures();
	}
	
	public void resizeAllRegions(int newRegionSize)
	{
		System.out.println("resizing all regions to:" + newRegionSize);
		
		Feature[] features = getFeatures();
		for (int i = 0; i < features.length; ++i)
		{
			int regionSize = features[i].m_iEnd - features[i].m_iStart;
//			if (regionSize > newRegionSize)
//			{
//				System.out.print(features[i].m_sStrLine);
//				System.out.println(" region size = " + regionSize);
//			}
			
			if (regionSize != newRegionSize)
			{
				features[i].m_iStart -= Math.floor(0.5 * (newRegionSize - regionSize));
				features[i].m_iEnd += Math.ceil(0.5 * (newRegionSize - regionSize));
				//assert(features[i].m_iEnd - features[i].m_iStart == newRegionSize);
			}
		}
		
    	// sort the features for all chromosomes
    	m_ChromFeatures.sortFeatures();
	}
	
	/**
	 * sets The number of bins for all features
	 * @param numBins
	 */
	public void setNumBins(int numBins)
	{
	    m_ChromFeatures.setNumBins(numBins);
	}
	
	public void readBigWig(String sFileName, int numBins, boolean includeZeros, boolean headerOnly) throws IOException, InterruptedException
	{
		// String path = "http://hgdownload.cse.ucsc.edu/goldenPath/hg19/encodeDCC/wgEncodeRegMarkH3k4me3/wgEncodeBroadHistoneH1hescH3k4me3StdSig.bigWig";
		// String path = "/Users/cydneyn/Downloads/wgEncodeBroadHistoneH1hescH3k4me3StdSig.bigWig";
		// SeekableStream ss = new SeekableBufferedStream(SeekableStreamFactory.getStreamFor(path), 64000);
		
		m_FileName = sFileName;
		
		SeekableStream ss = new SeekableBufferedStream(SeekableStreamFactory.getStreamFor(sFileName), 64000);
		BBFileReader bbReader = new BBFileReader(sFileName, ss);

        // check file type
		// BBFileHeader bbFileHdr = bbReader.getBBFileHeader();
		if (!bbReader.isBigWigFile()) 
            throw new IOException("File type is not BigWig as expected: " + sFileName);
		
		m_ProgressMessage = PROGRESS_PREFIX + "Retrieving data from bigWig file";
		// set progress to 1% in order to trigger an update (0 will not achieve this)
		firePropertyChange(m_ProgressMessage, 0, 1);
		
		m_ChromFeatures.clearDataValues();
		m_ChromFeatures.setNumBins(numBins);
    	m_WigTrack = new WigTrack();
    	
    	m_GlobalStats = new DataStats();
    	m_RegionStats = null;
    	m_DataTable   = null;
    	m_ReadCountTable = null;

    	// allows for overlaps (does not have to be fully contained)
    	boolean contained = false;
    	
    	int numFeatures = m_ChromFeatures.getNumFeatures();
    	double 	dProgressReportInterval = 0.05;
    	int prevProgress = 0;
    	int currProgress = 0;
    	
    	BigWigIterator wigIterator;
    	int fCounter = 0;
    	for (String chrom: m_ChromFeatures.getChroms()) {
    		// System.out.println("\n" + chrom);
    		List<Feature> features = m_ChromFeatures.getFeatures(chrom);
    		// System.out.println(features.size() + " " + numFeatures + " " + fCounter);
    		// 	System.out.println("here");
    		// }
    		for (Feature f: features) 
    		{
        		if (checkForInterrupt())
        			return;

    			currProgress = ((int) (fCounter*100/numFeatures));
    			if ((currProgress - prevProgress) > dProgressReportInterval) {
    				firePropertyChange(m_ProgressMessage, prevProgress, currProgress);
    				prevProgress = currProgress;
    			}
    			wigIterator = bbReader.getBigWigIterator(chrom, f.getStart(), chrom, f.getEnd(), contained);
        		WigItem nextWig = null;
        		while (wigIterator.hasNext()) {
        			nextWig = wigIterator.next();
        			if (nextWig == null)
        				break;
        			// add this wig data to the current feature
        			float fValue = nextWig.getWigValue();
					// ignore zero data values if requested
					if (!includeZeros && fValue == 0) { continue;}
					int start = nextWig.getStartBase();
					int end = nextWig.getEndBase();
					// TODO: check if -1 or not
					// determine how many positions have fValue
					int vStart = start;
					if (start < f.getStart()) {
						vStart = f.getStart();
					}
					int vEnd = end;
					if (end > f.getEnd()) {
						vEnd = f.getEnd();
					}
					m_GlobalStats.addValue(fValue, vEnd-vStart+1);
        			f.addDataValue(vStart, vEnd, fValue);

        		}
        		fCounter++;
    		}
    	}
		computeDataTable();
    	m_GlobalStats.calcFinalStats();
	}
	
	/**
	 * reads a wig file from an input file and adds the track data values to the feature vectores (gff) 
	 * can also limit to just the header (useful for parsing the 'name' field)
	 * @param sFileName		filename (currently supports .wig, .wig.gz, .wig.zip)
	 */
	public void readWIG(String sFileName, int numBins, boolean includeZeros, boolean headerOnly) throws IOException, InterruptedException
	{
		m_FileName = sFileName;
		long iStreamSize = 0;
		InputStream streamWIG = null;
		InputStream webStream = readURL(sFileName);
		File wigFile = null;
		if (webStream == null)
		{
			wigFile = new File(sFileName);
			m_ProgressMessage = PROGRESS_PREFIX + "Loading wig file";
		} 
		else 
		{
			m_ProgressMessage = PROGRESS_PREFIX + "Loading data file";
		}
		// set progress to 1% in order to trigger an update (0 will not achieve this)
		firePropertyChange(m_ProgressMessage, 0, 1);

		if (sFileName.endsWith(".wig.gz"))
		{
			if (m_bPrintProgress)
				System.out.println("Reading " + sFileName);
			firePropertyChange("Reading " + sFileName, 0, 1);
			

			if (webStream != null)
			{
				streamWIG = new GZIPInputStream(webStream);
			}
			else
			{
				RandomAccessFile raf = new RandomAccessFile(wigFile.getAbsolutePath(), "r");
				raf.seek(raf.length() - 4);
				int b4 = raf.read();
				int b3 = raf.read();
				int b2 = raf.read();
				int b1 = raf.read();
				iStreamSize = (b1 << 24) | (b2 << 16) + (b3 << 8) + b4;
				raf.close();
				if (iStreamSize < 0)
					iStreamSize = 0xffffffffl + iStreamSize;
				streamWIG = new GZIPInputStream(new FileInputStream(wigFile));
			}
		}
		else if (sFileName.endsWith(".wig.zip"))
		{
			ZipInputStream zin = null;
			if (webStream != null)
			{
				zin = new  ZipInputStream(new BufferedInputStream(webStream));
			}
			else
			{
				zin = new  ZipInputStream(new BufferedInputStream(new FileInputStream(wigFile)));
			}

			ZipEntry entry;
			if ((entry = zin.getNextEntry()) != null)
			{
				// extract data
				if (m_bPrintProgress)
					System.out.println("Reading " + entry.getName());
				firePropertyChange("Reading " + entry.getName(), 0, 1);
				
				streamWIG = zin;
				iStreamSize = entry.getSize();
			}
		}
		else if (sFileName.endsWith(".wig"))
		{
			if (m_bPrintProgress)
				System.out.println("Reading " + sFileName);
			firePropertyChange("Reading " + sFileName, 0, 1);
			
			if (webStream != null)
			{
				streamWIG = webStream;
			}
			else
			{
				iStreamSize = wigFile.length();
				streamWIG = new FileInputStream(wigFile);
			}
		}

		if (streamWIG != null)
		{
			readWIG(streamWIG, iStreamSize, numBins, includeZeros, headerOnly);
			streamWIG.close();
		}
		else
		{
			String err = "Unable to read '" + sFileName + "'";
			log.error(err);
			throw new IOException(err);
		}
	}

	/**
	 * reads a wig file from an input stream and adds the track data values to the feature vectores (gff) 
	 * @param streamIn  input stream
	 * @param iStreamSize (optional) stream size (if known) used for estimating the progress. pass 0 if unknown.
	 */
	void readWIG(InputStream streamIn, long iStreamSize, int numBins, boolean includeZeros, boolean headerOnly) throws IOException, InterruptedException
	{	
		m_ChromFeatures.clearDataValues();
		m_ChromFeatures.setNumBins(numBins);
    	m_WigTrack = new WigTrack();
    	
    	m_GlobalStats = new DataStats();
    	m_RegionStats = null;
    	m_DataTable   = null;
    	m_ReadCountTable = null;
    	
    	//http://genome.ucsc.edu/goldenPath/help/wiggle.html
    	BufferedReader reader = new BufferedReader(new InputStreamReader(streamIn));
    	
		String strLine = null;
		
    	int iChrStart = -1; // current start position
    	int iChrStep = 1; // current step
    	int iChrSpan = 1; // current span
    	boolean bValid = true; // if the latest entry had valid values
    	boolean bFixed = true; // if the latest entry was fixedStep or variableStep
    	double 	dProgressReportInterval = 0.01;//0.05;
    	long 	iReportedRead = 0;
    	long	iCurrentRead = 0;
    	
    	double currentProgress = 0;

    	while ((strLine = reader.readLine()) != null)
		{
    		if (checkForInterrupt())
    			return;
    		
    		if (iStreamSize > 0 && 1.0 * (iCurrentRead - iReportedRead) / iStreamSize > dProgressReportInterval)
    		{
    			// double prevProgress = m_Progress;
    			double prevProgress = currentProgress;
    			// m_Progress = (int) (100 * iCurrentRead / iStreamSize);
    			if (iCurrentRead > iStreamSize && iStreamSize < 0x100000000L) {
    				// due to a problem in .gz format, only the last 32 significant bits are stored in size.
    				// thus the size of files > 4GB will be incorrect. 
    				// So we add 4GB to the estimated streamsize hoping to fix it.
    				iStreamSize += 0x100000000L;
    			}
    			
    			currentProgress = (int) (100 * iCurrentRead / iStreamSize);
    			// firePropertyChange(m_ProgressMessage, prevProgress, m_Progress);
    			firePropertyChange(m_ProgressMessage, (int) prevProgress, (int) currentProgress);
    			
    			if (m_bPrintProgress)
    				System.out.printf("[%%%d]", (int)currentProgress);
    			iReportedRead = iCurrentRead;
    		}
    		else if (iStreamSize == 0 && iCurrentRead - iReportedRead > 1024*1024)
    		{// print a "." for every megabyte read, when size is unknown
    			if (m_bPrintProgress)
    				System.out.print(".");
				iReportedRead = iCurrentRead;
    		}
    		// this is not exactly correct, as strLine may have terminated in DOS '\r\n'
    		//iCurrentRead += (strLine + "\n").getBytes().length;
    		iCurrentRead += strLine.length() + 1;
    		
			if (strLine.length() == 0) {
				// new line only
				continue;
			}
	
			if (strLine.charAt(0)=='f' || strLine.charAt(0)=='v')
			{   //fixedStep  chrom=chrN  start=position  step=stepInterval  [span=windowSize]
			    //variableStep  chrom=chrN  [span=windowSize]
	        	if (headerOnly) { return; }
	        	String[] tokens = strLine.split("[\t ]");
	        	if (tokens.length > 0 && tokens[0].equals("fixedStep"))
	        	{
	        		bFixed = true;
	        		bValid = true;
	        	}
	        	else if (tokens.length > 0 && tokens[0].equals("variableStep"))
	        	{
	        		bFixed = false;
	        		bValid = true;
	        	}
	        	
	        	if (bValid)
	        	{
		        	iChrStart = -1;
		        	iChrStep = -1;
		        	iChrSpan = 1;
		        	try 
		        	{
			        	for (int i = 1; i < tokens.length; i++)
			        	{
			        		String[] param = tokens[i].split("=");
			        		if (param.length == 2)
			        		{
			        			if (param[0].equals("chrom"))
			        			{
			        				m_ChromFeatures.selectChrom(param[1]);
			        			}
			        			else if (param[0].equals("start"))
			        			{
			        				iChrStart = Integer.valueOf(param[1]);
			        			}
			        			else if (param[0].equals("step"))
			        			{
			        				iChrStep = Integer.valueOf(param[1]);
			        			}
			        			else if (param[0].equals("span"))
			        			{
			        				iChrSpan = Integer.valueOf(param[1]);
			        			}
			        		}
			        	}
		        	}
		        	catch (Exception e)
		        	{
		        		e.printStackTrace();
		        		bValid = false;
		        	}
		        	bValid = (m_ChromFeatures.hasFeatures()) && (!bFixed || iChrStart != -1) && (!bFixed || iChrStep != -1);
	        	}
	        	/*
	        	if (m_bVerbose && bValid)
	        	{
		        	if (bFixed)
		        		System.out.println("fixedStep chrom=" + " start=" + iChrStart + " step=" + iChrStep+" span=" + iChrSpan);
		        	else
		        		System.out.println("variableStep chrom=" + " span=" + iChrSpan);
					System.out.println(strLine);
	        	}*/
			}
			else if (strLine.charAt(0)=='t')//track
			{//track type=wiggle_0 name=track_label description=center_label ...
	        	if (m_bVerbose)
	        	{
	        		System.out.println(strLine);
	        	}
	        	m_WigTrack.read(strLine);
	        	if (headerOnly) { return; }
			}
			else
			{
	        	if (headerOnly) { return; }
				try
				{
					double fValue = -1;
					if (bValid)
					{
						if (bFixed)
						{//fixedStep
							fValue = Double.valueOf(strLine);
							// ignore zero data values if requested
							if (!includeZeros && fValue == 0) { 
								iChrStart += iChrStep;
							} else {
								m_ChromFeatures.addDataValue(iChrStart, iChrStart + iChrSpan - 1, fValue);
								iChrStart += iChrStep;
							}
						}
						else
						{// variableStep
			        		String[] param = strLine.split("[\t ]");
	        				iChrStart = Integer.valueOf(param[0]);
							fValue = Double.valueOf(param[1]);
							// ignore zero data values if requested
							if (!includeZeros && fValue == 0) { continue; }
							m_ChromFeatures.addDataValue(iChrStart, iChrStart + iChrSpan - 1, fValue);
						}
						// ignore zero data values if requested
						if (!includeZeros && fValue == 0) {
							continue;
						}
						m_GlobalStats.addValue(fValue, iChrSpan);
					}
					//System.out.println(f);
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
			}
		}
    	
    	if (m_WigTrack.m_sDescription == null) {
    		log.error("No track line found in wig file: '" + m_FileName + "'");
    		//throw new IOException("Error parsing wig file. No track line found in file:\n'" + m_FileName + "'");
    	}
    	
		computeDataTable();
    	m_GlobalStats.calcFinalStats();
    	
		// System.out.println("[%100]\nSuccessfully read " + iCurrentRead + " bytes.");
				
	}

	protected InputStream readURL(String url)
	{
		try 
		{
			URL u = new URL(url);
			return u.openStream();
		} catch (MalformedURLException e) 
		{
			// do nothing
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	public void enableVerbose(boolean bEnable)
	{
		m_bVerbose 	= bEnable;
	}
	
	public void enablePrintProgress(boolean bEnable)
	{
		m_bPrintProgress = bEnable;
	}
	
    public void addPropertyChangeListener(PropertyChangeListener p) {
    	m_PropertyChangeSupport.addPropertyChangeListener(p);
    }
    
    public void removePropertyChangeListener(PropertyChangeListener p) {
    	m_PropertyChangeSupport.removePropertyChangeListener(p);
    }
    
    protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) throws InterruptedException 
    {
		Thread.sleep(1L);
    	m_PropertyChangeSupport.firePropertyChange(propertyName, oldValue, newValue);
    }

    protected boolean checkForInterrupt() throws InterruptedException
    {
		if (Thread.currentThread().isInterrupted())
		{
			Thread.sleep(10L); // this will also throw an InterruptedException
			return true;
		}
		return false;
    }

	public int getMinRegionSize() {
		return m_MinRegionSize;
	}
	
	public int getMaxRegionSize() {
		return m_MaxRegionSize;
	}
}
