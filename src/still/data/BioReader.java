/**
 * BioReader.java
 * Abstract: Provides the functionality to read gff and wig files, bin and create a double[][] matrix of data
 * Author: Hamidreza Younesy
 * Date:
 */

package still.data;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Provides the functionality to read gff and wig files, bin and create a double[][] matrix of data
 * The wiggle (WIG) format is for display of dense, continuous data such as GC percent, probability scores, and transcriptome data.
 */
public class BioReader
{
	// Stores the features for all chromosomes
	HashMap<String, ChromFeatures> 	m_ChromMap 	= null;
	
	// toggles, message output (debug) 
	public static boolean		m_bVerbose 	= false;
	
	// Track line for the wig file
	WigTrack					m_WigTrack = null;

	public DataStats			m_GlobalStats = null; // global stats for one wig file (based on all reads in the wig file)
	public DataStats 			m_RegionStats = null; // region stats for one wig file (based on the regions specified in the gff file)
	
	double [][] 				m_DataTable = null;   // final computed data table. size: rows=#regions. columns=#bins
	
	/**
	 * Track lines define the display attributes for all lines in an annotation data set.
	 * more in: http://genome.ucsc.edu/goldenPath/help/customTrack.html#TRACK
	 */
	public static class WigTrack
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
	};
	
	public static class DataStats
	{
		public long   m_iCount = 0;
		public double m_dMin = Double.MAX_VALUE;
		public double m_dMax = Double.NEGATIVE_INFINITY;
		public double m_dMean = 0;
		public double m_dStdDev = 0;
		public double m_dFirstQuartile = Double.NEGATIVE_INFINITY;
		public double m_dMedian = Double.NEGATIVE_INFINITY;
		public double m_dThirdQuartile = Double.NEGATIVE_INFINITY;
		
		public long	  m_iNumZeros = 0;
		public double m_HistMin = 0;
		public double m_HistMax = 10000.0;
		public int	  m_HistBins = 1000000;
		public long[]  m_Histogram = new long[m_HistBins];
		void addValue(double val, int count)
		{
			if (val < 0)
			{
				val = 0;
			}
			
			if (val != 0)
			{
				int iBinIndex = Math.min(Math.max((int)(m_HistBins * (val - m_HistMin)/(m_HistMax - m_HistMin)), 0), m_HistBins - 1);
				m_Histogram[iBinIndex] += count;
				m_dMin = Math.min(val, m_dMin);
				m_dMax = Math.max(val, m_dMax);
				m_dMean += val * count;
				m_dStdDev += (val*val) * count;
				m_iCount += count;
			}
			else
			{
				m_iNumZeros += count;
			}
		}
		
		void calcFinalStats()
		{
	    	m_dMean /= m_iCount;
	    	m_dStdDev = Math.sqrt(m_dStdDev / m_iCount - (m_dMean * m_dMean)); //var = E(x^2) - E(x)^2
	    	int iSum = 0;
	    	for (int i = 0; i < m_HistBins; i++)
	    	{
	    		iSum += m_Histogram[i];
	    		if (m_dFirstQuartile == Double.NEGATIVE_INFINITY && iSum >= m_iCount / 4)
	    		{
	    			m_dFirstQuartile = i * (m_HistMax - m_HistMin) / m_HistBins + m_HistMin;
	    		}
	    		if (m_dMedian == Double.NEGATIVE_INFINITY && iSum >= m_iCount / 2)
	    		{
	    			m_dMedian = i * (m_HistMax - m_HistMin) / m_HistBins + m_HistMin;
	    		}
	    		if (m_dThirdQuartile == Double.NEGATIVE_INFINITY && iSum >= 3 * m_iCount / 4)
	    		{
	    			m_dThirdQuartile = i * (m_HistMax - m_HistMin) / m_HistBins + m_HistMin;
	    			break;
	    		}
	    	}
		}
		
		public void printStats()
		{
			System.out.printf("min:%f,max:%f,mean:%f,stddev:%f,1stQ:%f,median:%f,3rdQ:%f,count:%d,zeros:%d", 
					   m_dMin, m_dMax, m_dMean, m_dStdDev, m_dFirstQuartile, m_dMedian, m_dThirdQuartile, m_iCount, m_iNumZeros);
		}
		
	}

	/**
	 * Stores the features/regions for one chromosome
	 */
	public static class ChromFeatures
	{

		/**
		 * Stores the information for a single feature/region
		 * http://genome.ucsc.edu/FAQ/FAQformat.html#format3
		 */
		public static class Feature implements Comparable<Feature>
		{
			public String	m_sStrLine;	// original string read from file

			public String 	m_sSeqName; // e.g. 1 or chr1
			public String	m_sSource;	// e.g. UCSC
			public String 	m_sFeature; // e.g. TSS
			public int  	m_iStart;	// The starting position of the feature in the sequence. e.g. 1000000
			public int  	m_iEnd;		// The ending position of the feature (inclusive). e.g. 1001000
			public int  	m_iScore;	// 0..1000 or '.'
			public char 	m_cStrand;	// '+', '-', or '.' (for don't know/don't care).
			public int 		m_iFrame;	// 0..2 or '.' if the feature is not a coding exon
			public String	m_sGroup;	// e.g. NR_028269 (UCSC Genome Browser: All lines with the same group are linked together into a single item.)
			
			public static int s_iNumBins = 30;  // number of bins used to store the values.
			public double 	m_DataBins[] = null;	// data value bins
			public double	m_BinCount[] = null;	// counts the number of values added to a bin (used to average the bin value later).
			
			protected int	m_iSeqIndex; // an index calculated from the sequence name used for sorting features.
			
			/**
			 * converts the information for this feature back to a string
			 */
			public String toString()
			{
//				return m_sSeqName + "\t" +
//				       m_sSource + "\t" +
//				       m_sFeature + "\t" +
//				       m_iStart + "\t" +
//				       m_iEnd + "\t" +
//				       (m_iScore == -1 ? "." : Integer.toString(m_iScore)) + "\t" +
//				       m_cStrand + "\t" +
//				       (m_iFrame == -1 ? "." : Integer.toString(m_iFrame))+ "\t" +
//				       m_sGroup;
				return m_sStrLine;
			}

			/**
			 * Compare if this feature's range is before, after or overlapping the input range (returns -1 / +1 / 0 respectively)
			 * @param iStart	start of range
			 * @param iEnd		end of range
			 * @return  		(-1) when feature before range, (1) when feature after range, (0) when feature intersecting the range
			 */
			public int compareTo(int iStart, int iEnd)
			{//
				return m_iEnd < iStart ? -1 : (m_iStart > iEnd ? 1 : 0);
			}
			
			/**
			 * Compare if this feature's start is before, after or same as another feature's start (returns -1 / +1 / 0 respectively)
			 * @param	f		the other feature to compare to
			 * @return  		(-1) when feature before input, (1) when feature after input, (0) when equal
			 */
			public int compareTo(Feature f) {
				if (this.m_iSeqIndex >= 0 && f.m_iSeqIndex >= 0)
				{
					return this.m_iSeqIndex == f.m_iSeqIndex ? this.m_iStart - f.m_iStart : this.m_iSeqIndex - f.m_iSeqIndex;
				}
				return this.m_sSeqName.compareTo(f.m_sSeqName);
			}

			/**
			 * Returns the size of each bin
			 * @return	bin size; can be a real non-integer value when num-bins is not a factor of (start - end + 1)
			 */
			public double getBinSize()
			{
				return 1.0 * (m_iEnd - m_iStart + 1) / s_iNumBins;
			}
			
			/**
			 * Adds a data value to this feature; stored in the proper bin(s) 
			 * @param iStart	start of the range that the data values spans
			 * @param iEnd		end of the range that the data values spans
			 * @param value	the data value
			 */
			void addDataValue(int iStart, int iEnd, double value)
			{
				// check if the range overlaps this feature
				if (compareTo(iStart, iEnd) != 0)
				{
					return;
				}
				
				if (m_DataBins == null)
				{
					m_DataBins = new double[s_iNumBins];
					m_BinCount = new double[s_iNumBins];
				}
				assert (m_DataBins.length == s_iNumBins);
				
				int b0 = (int)Math.floor(1.0 * (iStart - m_iStart) * s_iNumBins / (m_iEnd - m_iStart + 1)); //bin index for iStart  
				int b1 = (int)Math.floor(1.0 * (iEnd   - m_iStart) * s_iNumBins / (m_iEnd - m_iStart + 1)); //bin index for iEnd
				
				if (b0 == b1)
				{// start and end fall on the same bin
					m_BinCount[b0] += (iEnd - iStart + 1);
					m_DataBins[b0] += (iEnd - iStart + 1) * value;
				}
				else
				{
					double binSize = getBinSize(); 
					if (b0 >= 0)
					{
						double dRange = ((b0 + 1) * binSize + m_iStart - iStart); // portion of the bin
						m_BinCount[b0] += dRange;
						m_DataBins[b0] += dRange * value;
					}
					
					if (b1 < s_iNumBins)
					{
						double dRange = (iEnd  - m_iStart + 1 - b1 * binSize);
						m_BinCount[b1] += dRange;
						m_DataBins[b1] += dRange * value;
					}
					
					b0 = Math.max(b0, 0);
					b1 = Math.min(b1, s_iNumBins);
					for (int b = b0 + 1; b < b1; b++)
					{
						m_BinCount[b] += binSize;
						m_DataBins[b] += binSize * value;
					}
				}
			}
		}
		
		/**
		 * Array of features inside one chromosome. 
		 */
		public ArrayList<Feature> m_Features;
		
		/**
		 * Constructor
		 */
		ChromFeatures()
		{
			m_Features = new ArrayList<Feature>();
		}
		
		/**
		 * Adds a feature to the array of features.
		 * @param f		feature to add to the m_Features array
		 */
		void add(Feature f)
		{
			m_Features.add(f);
		}
		
		/**
		 * Clears all the values filled in the features.
		 */
		void clearValues()
		{
			for (int i = 0; i < m_Features.size(); i++)
			{
				Feature f = m_Features.get(i);
				f.m_DataBins = null;
				f.m_BinCount = null;
			}
		}
		
		/**
		 * Sorts the features in m_Features, by their "start" and then "end"
		 */
		void sortFeatures()
		{
			Collections.sort(m_Features, new Comparator<Feature>() {
				public int compare(Feature f1, Feature f2)
				{
					return f1.m_iStart < f2.m_iStart ? -1 : (f1.m_iStart > f2.m_iStart ? 1 : 0);
				}
			});
			
//			for (int i = 0; i < m_Features.size() - 1; i++)
//			{// debug
//				m_Features.get(i).println();
//				assert (m_Features.get(i).m_iStart <= m_Features.get(i + 1).m_iStart);
//			}
		}
		
		static Feature s_TmpFeature = null; // stored here to avoid allocation on every findFeatureIndex call
		/**
		 * Finds the index of a feature in m_Features array, that overlaps the [start, end] range. 
		 */
		int findFeatureIndex(int iStart, int iEnd)
		{
			if (s_TmpFeature == null)
			{
				s_TmpFeature = new Feature();
			}
			s_TmpFeature.m_iStart = iStart;
			s_TmpFeature.m_iEnd   = iEnd;
			
			int index = Collections.binarySearch(m_Features, s_TmpFeature, new Comparator<Feature>() {
								public int compare(Feature f1, Feature f2)
								{
									return f1.compareTo(f2.m_iStart, f2.m_iEnd);
								}
							});
			return index;
		}
		
		/**
		 * Adds the data value to the the feature corresponsing the range [iStart, iEnd]  
		 * @param iStart	start of the data value span range
		 * @param iEnd		end of the data value span range
		 * @param value	data value to be added
		 * TODO: the values int the wig files are soreted -> can skip the O(logN) binary search and find the features in O(1)
		 */
		void addDataValue(int iStart, int iEnd, double value)
		{
			//if (value == 0)
			//	return;
			int index = findFeatureIndex(iStart, iEnd);
			if (index > 0)
			{
				Feature cf = m_Features.get(index);
				cf.addDataValue(iStart, iEnd, value);
				if (iStart < cf.m_iStart)
				{
					addDataValue(iStart, cf.m_iStart - 1, value);
				}
				if (iEnd > cf.m_iEnd)
				{
					addDataValue(cf.m_iEnd + 1, iEnd, value);
				}
			}
		}
	}

	/**
	 * Reads a GFF file  and populates the feature information in the m_ChromMap
	 * @param filename	gff filename
	 */
	public void readGFF(String filename)
	{
		try
		{
			InputStream streamGFF = readURL(filename); // first try if the filename is a net url
			if (streamGFF == null)
			{
				streamGFF = new FileInputStream(filename);
			}
			readGFF(streamGFF);
			streamGFF.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
			System.err.println("Unable to read " + filename);
		}
	}
	
	InputStream readURL(String url)
	{
		if (url.contains("http://") || url.contains("ftp://") || url.contains("www."))
		{
			try {
				String urlStr = url;
				if (urlStr.startsWith("www."))
				{
					urlStr = "http://"+url;
				}
				
				URL u = new URL(urlStr);
				return u.openStream();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return null;
	}
	
	/**
	 * Reads a GFF file from an input stream and populates the feature information in the m_ChromMap
	 * @param streamIn	input stream
	 */
	public void readGFF(InputStream streamIn)
	{	
		// http://genome.ucsc.edu/FAQ/FAQformat.html#format3
		// 	seqname	source	feature	start	end	score	strand	frame	group
		//	1	ucsc	TSS	4502	10503	.	-	.	NR_028269

		m_ChromMap = new HashMap<String, ChromFeatures>();
		//for (int i = 0; i < 22; i++)
		//	m_ChromMap.put("chr" + (i + 1), new Integer(i));
		//m_ChromMap.put("chrX", new Integer(22));
		//m_ChromMap.put("chrY", new Integer(23));
		
		InputStreamParser parser = new InputStreamParser(streamIn);
		String strLine = null;
		
		// read the features
		int minRegionSize = Integer.MAX_VALUE; 
		int maxRegionSize = Integer.MIN_VALUE;
    	while ((strLine = parser.readLine()) != null)
		{
			if (strLine.startsWith("track"))
			{
				continue;
			}
			if (strLine.startsWith("#"))
			{
				continue;
			}
			
			// According to the spec, GFF lines have nine required fields that must be tab-separated.
        	String[] tokens = strLine.split("[\t]");
        	
			if (tokens.length != 9)
			{//TODO: ? allow the fields to be toggled through UI ?
	        	tokens = strLine.split("[\t ]");// To be more graceful on some invalid files I have, allow for spaces as well as tabs
	        	if (tokens.length < 9 && tokens.length != 5 && tokens.length != 4)
	        	{
	        		System.out.println("ERROR: Expect 4, 5 or 9 fields in the GFF file; found " + tokens.length);
					return;
	        	}
			} 
			
			try
			{
				ChromFeatures.Feature f = new ChromFeatures.Feature();
				f.m_sStrLine = strLine;
				
				if (tokens.length == 4)
				{// to handle a special (invalid) case
					f.m_sGroup		= tokens[0];
					f.m_sSeqName 	= tokens[1].length() > 3 ? tokens[1] : "chr"+tokens[1];
					f.m_iStart		= Integer.valueOf(tokens[2]);
					f.m_iEnd		= Integer.valueOf(tokens[3]);
				}
				else if (tokens.length == 5)
				{// to handle a special (invalid) case
					f.m_sSeqName 	= tokens[0].length() > 3 ? tokens[0] : "chr"+tokens[0];
					f.m_iStart		= Integer.valueOf(tokens[1]);
					f.m_iEnd		= Integer.valueOf(tokens[2]);
					f.m_cStrand		= tokens[3].equals(".") | tokens[3].equals("1") ? '+' : tokens[3].charAt(0);
					f.m_sGroup		= tokens[4];
				}
				else
				{// 
					f.m_sSeqName 	= tokens[0].length() > 3 ? tokens[0] : "chr"+tokens[0];
					f.m_sSource 	= tokens[1];
					f.m_sFeature 	= tokens[2];
					f.m_iStart		= Integer.valueOf(tokens[3]);
					f.m_iEnd		= Integer.valueOf(tokens[4]);
					f.m_iScore		= tokens[5].equals(".") ? -1 : Integer.getInteger(tokens[5]);
					f.m_cStrand		= tokens[6].equals(".") | tokens[6].equals("1") ? '+' : tokens[6].charAt(0);
					f.m_iFrame		= tokens[7].equals(".") ? -1 : Integer.getInteger(tokens[7]);
					f.m_sGroup		= tokens[8];
				}
				
				try
				{
					f.m_iSeqIndex = Integer.parseInt(f.m_sSeqName.replaceFirst("chr", ""));
				} catch (NumberFormatException e) {
					f.m_iSeqIndex = -1;
				}
				
				int regionSize = f.m_iEnd - f.m_iStart;
				//System.out.println(regionSize);
				minRegionSize = Math.min(regionSize, minRegionSize);
				maxRegionSize = Math.max(regionSize, maxRegionSize);
				
				ChromFeatures a = m_ChromMap.get(f.m_sSeqName); 
				if (a == null)
				{// create a ChromFeatures entry for this chromosome.
					a = new ChromFeatures();
					m_ChromMap.put(f.m_sSeqName, a);
				}
				a.add(f);

				if (m_bVerbose)
				{
					System.out.println(strLine);
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}

    	//System.out.printf("minRegionSize = %d, maxRegionSize = %d\n", minRegionSize, maxRegionSize);
    	
    	// sort the features for all chromosomes
    	for (ChromFeatures cf:m_ChromMap.values())
    	{
    		cf.sortFeatures();
		}
	}
	
	/**
	 * Sets the (global) number of bins
	 * TODO: this might differ for each feature.
	 * @param iBins 	number of bins
	 */
	public void setNumBins(int iBins)
	{
		ChromFeatures.Feature.s_iNumBins = iBins;
	}

	/**
	 * Returns the maximum number of bins that can be specified. End - Start (from the first feature line of the gff file)
	 * @return	maximum number of bins
	 */
	public int getMaxNumBins()
	{
		if (m_ChromMap.size() > 0)
		{
			ChromFeatures [] features = new ChromFeatures[m_ChromMap.size()];
			m_ChromMap.values().toArray(features);
			ChromFeatures.Feature f = features[0].m_Features.get(0);
			return f.m_iEnd - f.m_iStart + 1;
		}
		return 0;
	}
	
	/**
	 * Returns the total number of features read from the GFF file.
	 * @return		total number of features.
	 */
	public int getNumTotalFeatures()
	{
		int iNumFeatures = 0;
    	for (ChromFeatures cf:m_ChromMap.values())
    	{
    		iNumFeatures += cf.m_Features.size();
		}
    	return iNumFeatures;
	}
	
	/**
	 * Normalization Types
	 */
	public enum NormType
	{
		NONE,
		EXP,
		LINEAR
	};
	
	/**
	 * Returns the values without any normalization
	 * @return		values in the double[][] matrix form
	 */
	double [][] getDataTable()
	{
		return m_DataTable;
	}
	

	/**
	 * Returns the array of all features/regions.
	 * @return	array of references to all features.
	 */
	public ChromFeatures.Feature[] getFeatures()
	{
		int iNumFeatures = getNumTotalFeatures();
		ChromFeatures.Feature [] features = new ChromFeatures.Feature[iNumFeatures];
		
    	int iRow = 0;
    	for (ChromFeatures cf:m_ChromMap.values())
    	{
    		for (int i = 0; i < cf.m_Features.size(); i++)
    		{
    			features[iRow++] = cf.m_Features.get(i);
    		}
		}
    	Arrays.sort(features);
    	return features;
	}
	
	/**
	 * Returns the array of all sequence names.
	 * @return	string array of all sequence names for the features.
	 */
	String[] getSeqNames()
	{
		ChromFeatures.Feature[] features = getFeatures();
    	String [] seqNames = new String[features.length];
		for (int i = 0; i < features.length; i++)
		{
			seqNames[i] = features[i].m_sSeqName;
		}
    	return seqNames;
	}
	
	
	
	void computeDataTable()
	{
		//DataStats st = regionStats != null ? regionStats : new DataStats();
		m_RegionStats = new DataStats();
		
    	int iNumCols = ChromFeatures.Feature.s_iNumBins;
    	int iNumRows = getNumTotalFeatures();
    	
    	//double [][] table = new double[iNumRows][iNumCols];
    	m_DataTable = new double[iNumRows][iNumCols];
    	
    	
    	m_RegionStats.m_dMin = Double.MAX_VALUE; // min > 0 value
    	m_RegionStats.m_dMax = Double.NEGATIVE_INFINITY; // max > 0 value 
    	m_RegionStats.m_dStdDev = 0;
    	m_RegionStats.m_dMean = 0;
    	m_RegionStats.m_iCount = 0; // number of > 0 values
    	
		// fill in the table with the bin values
    	ChromFeatures.Feature[] features = getFeatures();
		for (int i = 0; i < features.length; i++)
		{
			ChromFeatures.Feature f = features[i];
			if (f.m_DataBins != null)
			{
				boolean bFlip = f.m_cStrand == '-';
				for (int j = 0; j < iNumCols; j++)
				{
					int iBinIndex = bFlip ? iNumCols - j - 1 : j;
					double dValue = f.m_DataBins[iBinIndex];
					if (dValue > 0)
					{
						if (m_GlobalStats.m_iNumZeros > 0)
						{
							dValue /= f.m_BinCount[iBinIndex];  // divide the bin value by the number of reads for that bin
						}
						else
						{
							dValue /= f.getBinSize(); // divide the bin value by the bin-size
						}
						
						m_DataTable[i][j] = dValue;
						m_RegionStats.addValue(dValue, 1);
					}
				}
			}
		}
		m_RegionStats.calcFinalStats();
	}
		
	/**
	 * Normalizes the table with the specified normalization type and stats
	 * @param table			table to be normalized
	 * @param normType		normalization type: NONE, GAUSSIAN, LINEAR
	 * @param normStats		stats used to normalize the data.
	 * @return 				the normalized values will replace the values in the input table
	 */
	public void normalizeTable(double[][] table, NormType normType, DataStats normStats)
	{
    	int iNumRows = table.length;
    	int iNumCols = table[0].length;
    	
    	if (normType == NormType.LINEAR)
    	{
    		double dRange = normStats.m_dMax - normStats.m_dMin;
    		dRange = dRange == 0 ? 1.0 : dRange;
    			
          	for (int i = 0; i < iNumRows; i++)
          	{
          		for (int j = 0; j < iNumCols; j++)
          		{
          			table[i][j] = Math.max((table[i][j] - normStats.m_dMin), 0) / dRange;
          		}
          	}
    	}
    	else if (normType == NormType.EXP)
    	{
          	for (int i = 0; i < iNumRows; i++)
          	{
          		for (int j = 0; j < iNumCols; j++)
          		{
          			double dValue = table[i][j];
          			if (dValue > 0)
          			{
        				double exponent = -(dValue - normStats.m_dMedian) / normStats.m_dStdDev;
        				table[i][j] = 1.0 / (1 + Math.exp(exponent));
          			}
          		}
          	}
    	}
	}
	
	/**
	 * Creates a new normalized table with the specified normalization type and stats
	 * @param table			table to be normalized
	 * @param normType		normalization type: NONE, GAUSSIAN, LINEAR
	 * @param normStats		stats used to normalize the data.
	 * @return 				new table with the normalized values.
	 */
	public double[][] createNormalizedTable(double[][] table, NormType normType, DataStats normStats)
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
	 * reads a wig file from an input file and adds the track data values to the feature vectores (gff) 
	 * @param sFilename		filename (currently supports .wig, .wig.gz, .wig.zip)
	 */
	void readWIG(String sFilename)
	{
		long iStreamSize = 0;
		InputStream streamWIG = null;
		InputStream webStream = readURL(sFilename);
		try
		{
			File wigFile = null;
			if (webStream == null)
			{
				wigFile = new File(sFilename);
			}
			
			if (sFilename.endsWith(".wig.gz"))
			{
				System.out.println("Reading " + sFilename);
				
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
			else if (sFilename.endsWith(".wig.zip"))
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
					System.out.println("Reading " + entry.getName());
					streamWIG = zin;
					iStreamSize = entry.getSize();
				}
			}
			else if (sFilename.endsWith(".wig"))
			{
				System.out.println("Reading " + sFilename);
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
				readWIG(streamWIG, iStreamSize);
				streamWIG.close();
			}
			else
			{
				System.err.println("Unable to read " + sFilename);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	/**
	 * reads a wig file from an input stream and adds the track data values to the feature vectores (gff) 
	 * @param streamIn  input stream
	 * @param iStreamSize (optional) stream size (if known) used for estimating the progress. pass 0 if unknown.
	 */
	void readWIG(InputStream streamIn, long iStreamSize)
	{	
    	for (ChromFeatures cf:m_ChromMap.values())
    	{
    		cf.clearValues();
		}
    	m_WigTrack = new WigTrack();
    	
    	m_GlobalStats = new DataStats();
    	m_RegionStats = null;
    	m_DataTable   = null;
    	
    	//http://genome.ucsc.edu/goldenPath/help/wiggle.html
		InputStreamParser parser = new InputStreamParser(streamIn);
		String strLine = null;
		
		ChromFeatures currFeatures = null; // current chromosome features
    	int iChrStart = -1; // current start position
    	int iChrStep = 1; // current step
    	int iChrSpan = 1; // current span
    	boolean bValid = true; // if the latest entry had valid values
    	boolean bFixed = true; // if the latest entry was fixedStep or variableStep
    	double 	dProgressReportInterval = 0.1;
    	long 	iReportedRead = 0;

    	while ((strLine = parser.readLine()) != null)
		{
    		if (iStreamSize > 0 && 1.0 * (parser.m_iTotalRead - iReportedRead) / iStreamSize > dProgressReportInterval)
    		{
				System.out.printf("[%%%d]", 100 * parser.m_iTotalRead / iStreamSize);
				iReportedRead = parser.m_iTotalRead;
    		}
    		else if (iStreamSize == 0 && parser.m_iTotalRead - iReportedRead > 1024*1024)
    		{// print a "." for every megabyte read, when size is unknown
    			System.out.print(".");
				iReportedRead = parser.m_iTotalRead;
    		}
    		
			if (strLine.length() == 0)
			{
				System.out.println("ERROR in readWIG: 0 length string");
				continue;
			}
			if (strLine.charAt(0)=='f' || strLine.charAt(0)=='v')
			{   //fixedStep  chrom=chrN  start=position  step=stepInterval  [span=windowSize]
			    //variableStep  chrom=chrN  [span=windowSize]
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
	        		currFeatures = null;
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
			        				currFeatures = m_ChromMap.get(param[1]);
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
		        	bValid = (currFeatures != null) && (!bFixed || iChrStart != -1) && (!bFixed || iChrStep != -1);
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
			}
			else
			{
				try
				{
					double fValue = -1;
					if (bValid)
					{
						if (bFixed)
						{//fixedStep
							fValue = Double.valueOf(strLine);
							currFeatures.addDataValue(iChrStart, iChrStart + iChrSpan - 1, fValue);
							iChrStart += iChrStep;
						}
						else
						{// variableStep
			        		String[] param = strLine.split("[\t ]");
	        				iChrStart = Integer.valueOf(param[0]);
							fValue = Double.valueOf(param[1]);
							currFeatures.addDataValue(iChrStart, iChrStart + iChrSpan - 1, fValue);
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
    	
		computeDataTable();
    	m_GlobalStats.calcFinalStats();
    	
		System.out.print("[%100]\nSuccessfully read " + parser.m_iTotalRead + " bytes.");
				
	}
	
	/**
	 * General simple parser to read lines of strings from a stream.
	 * It buffers the input for performace.
	 * TODO: Add support for threading.
	 */
	public static class InputStreamParser
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
					if (m_Buffer[m_iEnd] == 10 || m_Buffer[m_iEnd] == 13)
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
				} catch (Exception e)
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
	
	
	// Spatial Pyramid Matching: "Beyond Bags of Features: Spatial Pyramid Matching for Recognizing Natural Scene Categories"
	// "Multiresolution histograms and their use for recognition"
	enum SPMMethod
	{
		PEAK_VALUE,
		PEAK_OFFSET,
		MEAN,
		HISTOGRAM
	}
	
	public double[][] calcSPM(double[][] table, SPMMethod method, int iMaxLevel, int iBranch, int iHistBins)
	{
		
		int iNumRows = table.length;
		int iNumCols = table[0].length;
		
		double dMaxValue = Double.NEGATIVE_INFINITY; // maximum value, used for histogram
		double[] dHist = new double[iHistBins];
		if (method == SPMMethod.HISTOGRAM)
		{
			for (int iRow = 0; iRow < iNumRows; iRow++)
			{
				for (int iCol = 0; iCol < iNumCols; iCol++)
				{
					dMaxValue = Math.max(table[iRow][iCol], dMaxValue);
				}
			}
		}
		
		int iCellDim = method == SPMMethod.HISTOGRAM ? iHistBins : 1; // number of features required per cell
		int iTotalCells = (int)(Math.pow(iBranch, iMaxLevel+1) - 1) / (iBranch - 1);
		
		double[][] result = new double[iNumRows][iTotalCells * iCellDim];
		
		int iLevelCells = 1; // number of cells per level
		int iCurrCell = 0;
		for (int l = 0; l <= iMaxLevel; l++)
		{
			double fW = Math.pow(2, l - iMaxLevel); 
			int col1 = 0; // start column index for this cell 
			int col2 = 0; // end column index for this cell
			for (int cell = 0; cell < iLevelCells; cell++)
			{
				col1 = col2;
				col2 = (cell + 1) * iNumCols / iLevelCells; 
				
				for (int iRow = 0; iRow < iNumRows; iRow++)
				{
					Arrays.fill(dHist, 0);
					double dPeakValue = 0;
					for (int col = col1; col < col2; col++)
					{
						double dValue = table[iRow][col];
						
						boolean bIgnoreZeros = false;
						if (method == SPMMethod.HISTOGRAM && (!bIgnoreZeros || dValue > 0))
						{
							int iHistBin = 0;
							if (bIgnoreZeros)
							{
								iHistBin = (int)(iHistBins * dValue / dMaxValue); // treat 0 value specially
							}
							else
							{
								//iHistBin = dValue <= 0 ? 0 : (int)((iHistBins - 1) * dValue / dMaxValue) + 1; // treat 0 value specially
								iHistBin = (int)((iHistBins+1) * dValue / dMaxValue) - 1;
							}
							
							if (iHistBin >= 0)
							{
								iHistBin = Math.min(iHistBin, iHistBins - 1); // clamp to avoid overflow
								dHist[iHistBin] += fW;
							}
						}
						else if (method == SPMMethod.PEAK_OFFSET)
						{
							if (dPeakValue < dValue)
							{
								dPeakValue = dValue;
								dHist[0] = fW*(1.0 * (col - col1 + 1)) / (col2 - col1);
							}
						}
						else if (method == SPMMethod.PEAK_VALUE)
						{
							dHist[0] = Math.max(fW*dValue, dHist[0]);
						}
						else if (method == SPMMethod.MEAN)
						{
							dHist[0] += fW*dValue/(col2 - col1);
						}
					}
					
					try
					{
						System.arraycopy(dHist, 0, result[iRow], iCurrCell * iCellDim, iCellDim);
					}
					catch(Exception e)
					{
						e.printStackTrace();
					}
					
				}
				
				iCurrCell++;
			}
				
			iLevelCells *= iBranch;
			
		}
		
		return result;
	}
	
}
