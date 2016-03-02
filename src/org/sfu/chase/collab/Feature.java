package org.sfu.chase.collab;

/**
 * Stores the information for a single feature/region
 * http://genome.ucsc.edu/FAQ/FAQformat.html#format3
 */
public class Feature implements Comparable<Feature>
{	
	protected String	m_sStrLine;	// original string read from file

	protected String 	m_sSeqName; // e.g. 1 or chr1
	protected String	m_sSource;	// e.g. UCSC
	protected String 	m_sFeature; // e.g. TSS
	protected int  		m_iStart;	// The starting position of the feature in the sequence. e.g. 1000000
	protected int  		m_iEnd;		// The ending position of the feature (inclusive). e.g. 1001000
	protected Float  	m_fScore;	// floating point value or '.'
	protected char 		m_cStrand;	// '+', '-', or '.' (for don't know/don't care).
	protected Integer 	m_iFrame;	// 0..2 or '.' if the feature is not a coding exon
	protected String	m_sGroup;	// e.g. NR_028269 (UCSC Genome Browser: All lines with the same group are linked together into a single item.)
	
	protected int		m_InputOrder; // order of the feature in the input region file. used for sorting
	protected int 		m_GroupOrder; // order of the feature based on the value of the m_sGroup. can be used for custom grouping (e.g. gene type)
	protected double 	m_DataBins[] = null;	// data value bins
	protected double	m_BinCount[] = null;	// counts the number of values added to a bin (used to average the bin value later).
	
	protected int	m_iSeqIndex; // an index calculated from the sequence name used for sorting features.
	
	/**
	 * Adds a data value to this feature; stored in the proper bin(s) 
	 * @param iStart	start of the range that the data values spans
	 * @param iEnd		end of the range that the data values spans
	 * @param value	the data value
	 */
	public boolean addDataValue(int iStart, int iEnd, double value)
	{	
		// must first initialize the number of bins
		if (m_DataBins == null) {
			return false;
		}
		
		// check if the range overlaps this feature
		if (compareTo(iStart, iEnd) != 0)
		{
			return false;
		} 

		int numBins = getNumBins();
		int b0 = (int)Math.floor(1.0 * (iStart - m_iStart) * numBins / (m_iEnd - m_iStart + 1)); //bin index for iStart  
		int b1 = (int)Math.floor(1.0 * (iEnd   - m_iStart) * numBins / (m_iEnd - m_iStart + 1)); //bin index for iEnd
		
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
			
			if (b1 < numBins)
			{
				double dRange = (iEnd  - m_iStart + 1 - b1 * binSize);
				m_BinCount[b1] += dRange;
				m_DataBins[b1] += dRange * value;
			}
			
			b0 = Math.max(b0, 0);
			b1 = Math.min(b1, numBins);
			for (int b = b0 + 1; b < b1; b++)
			{
				m_BinCount[b] += binSize;
				m_DataBins[b] += binSize * value;
			}
		}
		return true;
	}

	public void clearDataValues() {
		if (m_DataBins == null) { return; }
		for (int i=0; i<m_DataBins.length; i++) {
			m_DataBins[i] = 0;
		}
		for (int i=0; i<m_BinCount.length; i++) {
			m_BinCount[i] = 0;
		}
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
		int numBins = getNumBins();
		if (numBins == 0) { 
			return 0;
		} else {
			return 1.0 * (m_iEnd - m_iStart + 1) / getNumBins();
		}
	}
	
	public String getGroup() {
		return m_sGroup;
	}
	
	public int getNumBins() {
		if (m_DataBins == null) {
			return 0;
		} else {
			return m_DataBins.length;
		}
	}
	
	public String getSeqName() {
		return m_sSeqName;
	}
	
	public int getStart() {
		return m_iStart;
	}
	
	public int getEnd() {
		return m_iEnd;
	}
	
	public int getInputOrder() {
		return m_InputOrder;
	}
	
	public int getGroupOrder() {
		return m_GroupOrder;
	}
	
	public void setNumBins(int numBins) {
		m_DataBins = new double[numBins];
		m_BinCount = new double[numBins];
	}
	
	/**
	 * converts the information for this feature back to a string
	 */
	public String toString()
	{
		return m_sStrLine;
	}
	
	public void setGroup(String s) {
		m_sGroup = s;
	}
	
	public void updateString() 
	{
		m_sStrLine = m_sSeqName +"\t"+ m_sSource +"\t" + m_sFeature +"\t"+ m_iStart +"\t"+ m_iEnd +"\t";
		
		if (m_fScore == null) {
			m_sStrLine += ".\t";
		} else {
			m_sStrLine += m_fScore +"\t";
		}
		
		m_sStrLine += m_cStrand +"\t";
		
		if (m_iFrame == null) {
			m_sStrLine += ".\t";
		} else {
			m_sStrLine += m_iFrame + "\t";
		}
		
		if (m_sGroup != null) {
			m_sStrLine += m_sGroup;
		}
	}
}