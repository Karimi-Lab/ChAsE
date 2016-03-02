package org.sfu.chase.core;

import java.util.Arrays;

import still.data.Table;

public class ClustStats
{
	public double[]    	m_ColMean;     // average of each column.       size:[m_NumCols]
	public double[]    	m_ColStdDev;   // stddev for each column.       size:[m_NumCols]
	public double[][]  	m_ColHist;     // a 1D histogram per column.    size:[m_NumCols][m_NumHistBins]
	public double[][]  	m_ColQuantile; // quantiles per column.         size:[m_NumQuantiles][m_NumCols]
	public double[][]	m_ColPeaks;    // peaks per column.             size:[m_NumCols][m_NumHistBins]
	public double[][]	m_MeanHist;    // histogram of row average      size:[numGroups][m_NumHistBins]
	public double[][]	m_PeakHist;    // histogram of peaks per group  size:[numGroups][m_NumHistBins]
	public double[][][]	m_MeanVsPeak;  // histogram of peaks vs Mean  per group size:[numGroups][m_NumHistBins][m_NumHistBins]
	
	              
	public int         	m_Count;	   // number of data points (rows) in this cluster
	public int         	m_NumCols;     // number of columns (dimensionality) of the clusters
	public int 			m_NumHistBins = 10; // histogram bins
	public int 			m_NumQuantiles = 5; // number of quantiles. for 5, quantiles are: {min, 1stQ (%25), median, 3rdQ(75%), max}
    public int			m_NumGroups;
    public int medianIndex()
    {
    	return (m_NumQuantiles - 1) / 2;
    }
    
    public void calcStats(Table table, int[] rows, int cols[], GroupInfo[] groups)
    {
    	if (rows == null || rows.length == 0)
    	{
    		m_Count = 0;
    		return;
    	}
    	
    	m_NumGroups = groups.length;
    	m_Count = rows.length;
    	m_NumCols = cols == null ? table.columns() : cols.length;
    	m_ColMean = new double[m_NumCols];
    	m_ColStdDev = new double[m_NumCols];
    	m_ColHist  = new double[m_NumCols][m_NumHistBins];
    	m_ColPeaks = new double[m_NumCols][m_NumHistBins];
    	m_ColQuantile = new double[m_NumQuantiles][m_NumCols];
    	m_MeanHist = new double[m_NumGroups][m_NumHistBins];
    	m_PeakHist = new double[m_NumGroups][m_NumHistBins];
    	m_MeanVsPeak = new double[m_NumGroups][m_NumHistBins][m_NumHistBins];
    	
		for (int q = 1; q < m_NumQuantiles - 1; q++)
			Arrays.fill(m_ColQuantile[q], Double.NEGATIVE_INFINITY);
		
		Arrays.fill(m_ColQuantile[0], Double.MAX_VALUE);
		Arrays.fill(m_ColQuantile[m_NumQuantiles - 1], Double.NEGATIVE_INFINITY);
    	
    	// TODO: these min and max values are valid only for normalized data. otherwise should be calculated.
//    	double dMax = 1;
//    	double dMin = 0;

        int[]    iGroupPeakCol = new int[m_NumGroups];    // peak column(offset) for each group
		double[] dGroupPeakVal = new double[m_NumGroups]; // max peak value for each group
        double[] dGroupMeanVal = new double[m_NumGroups]; // mean (average) value for each group
        
		int groupDim = m_NumCols/m_NumGroups;
		
    	for (int r = 0; r < rows.length; ++r)
    	{
    		Arrays.fill(dGroupPeakVal, Double.NEGATIVE_INFINITY);
    		Arrays.fill(dGroupMeanVal, 0);
    		for (int c = 0; c < m_NumCols; ++c)
    		{
    			int gi = c / groupDim; // group index
    			double val = table.getMeasurement(rows[r], cols == null ? c : cols[c]);// / groups[gi].m_CutOffMax;
    			//val = val > 1 ? 1 : val; // normalize between 0 and 1
    			val = val > groups[gi].m_CutOffMax ? groups[gi].m_CutOffMax : val;
    			val = val < 0 ? 0 : val;
    			
    			m_ColMean[c] += val;
    			m_ColStdDev[c] += val*val;
    			int ibin = Math.min(m_NumHistBins - 1, (int) (m_NumHistBins * val));
    			m_ColHist[c][ibin]++;
    			m_ColQuantile[0][c] = Math.min(m_ColQuantile[0][c], val);
    			m_ColQuantile[m_NumQuantiles - 1][c] = Math.max(m_ColQuantile[m_NumQuantiles - 1][c], val);
    			
    			int group = c * m_NumGroups / m_NumCols;
    			if (dGroupPeakVal[group] < val)
    			{
    				dGroupPeakVal[group] = val;
    				iGroupPeakCol[group] = c;
    			}
    			dGroupMeanVal[group] += val / groupDim; 
    		}
    		
			for (int g = 0; g < m_NumGroups; g++)
			{
				int iPeakBin = Math.min(m_NumHistBins - 1, (int) (m_NumHistBins * dGroupPeakVal[g]));
				m_ColPeaks[iGroupPeakCol[g]][iPeakBin]++;
				m_PeakHist[g][iPeakBin]++;
				
				int iMeanBin = Math.min(m_NumHistBins - 1, (int) (m_NumHistBins * dGroupMeanVal[g]));
				m_MeanHist[g][iMeanBin]++;
				
				m_MeanVsPeak[g][iMeanBin][iPeakBin]++;
			}
    	}
    	
		for (int c = 0; c < m_NumCols; ++c)
		{
			m_ColMean[c] /= m_Count;
			m_ColStdDev[c] = m_ColStdDev[c] / m_Count - m_ColMean[c] * m_ColMean[c]; // stddev = E(x^2) - E(x)^2
			
			// approximately calculate the quantiles, by accumulating the histogram values.
			int iSum = 0;
			for (int b = 0; b < m_NumHistBins; b++)
			{
				iSum += m_ColHist[c][b];
				for (int q = 1; q < m_NumQuantiles - 1; q++)
				{
					if (m_ColQuantile[q][c] == Double.NEGATIVE_INFINITY && iSum >= q*m_Count / m_NumQuantiles)
					{
						m_ColQuantile[q][c] = 1.f * b / m_NumHistBins;
					}
				}
			}
		}
    }
}
