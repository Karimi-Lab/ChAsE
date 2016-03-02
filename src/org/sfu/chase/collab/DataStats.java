package org.sfu.chase.collab;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.sfu.chase.collab.InputStreamParser;

public class DataStats {
	
	public long   m_iCount = 0;
	public double m_dMin = 0;//Double.MAX_VALUE;
	public double m_dMax = Double.MIN_VALUE;
	public double m_dMean = 0;
	public double m_dStdDev = 0;
	public double m_dFirstQuartile = Double.MIN_VALUE;
	public double m_dMedian = Double.MIN_VALUE;
	public double m_dThirdQuartile = Double.MIN_VALUE;

	public long	  m_iNumZeros = 0;
	public double m_HistMin = -10000;
	public double m_HistMax = 10000.0;
	public int	  m_HistBins = 1000000;
	public long[]  m_Histogram = new long[m_HistBins];
	
	void addValue(double val, int count)
	{
		// if (val != 0)
		// {
			int iBinIndex = Math.min(Math.max((int)(m_HistBins * (val - m_HistMin)/(m_HistMax - m_HistMin)), 0), m_HistBins - 1);
			m_Histogram[iBinIndex] += count;
			m_dMin = Math.min(val, m_dMin);
			m_dMax = Math.max(val, m_dMax);
			m_dMean += val * count;
			m_dStdDev += (val*val) * count;
			m_iCount += count;
		// }
		//  else
		// {
		//  	m_iNumZeros += count;
		// }
	}

	void calcFinalStats()
	{
		m_dMean /= m_iCount;
		m_dStdDev = Math.sqrt(m_dStdDev / m_iCount - (m_dMean * m_dMean)); //var = E(x^2) - E(x)^2
		int iSum = 0;
		for (int i = 0; i < m_HistBins; i++)
		{
			iSum += m_Histogram[i];
			if (m_dFirstQuartile == Double.MIN_VALUE && iSum >= m_iCount / 4)
			{
				m_dFirstQuartile = i * (m_HistMax - m_HistMin) / m_HistBins + m_HistMin;
			}
			if (m_dMedian == Double.MIN_VALUE && iSum >= m_iCount / 2)
			{
				m_dMedian = i * (m_HistMax - m_HistMin) / m_HistBins + m_HistMin;
			}
			if (m_dThirdQuartile == Double.MIN_VALUE && iSum >= 3 * m_iCount / 4)
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
	
	public void readStats(String fn)
	{
		try
		{
			InputStream streamStats = new FileInputStream(fn);
			InputStreamParser parser = new InputStreamParser(streamStats);
			String strLine = null;
			
			// read the features
	    	while ((strLine = parser.readLine()) != null)
			{
				if (!strLine.startsWith("sample"))
				{
					System.out.println("ERROR: Unexpected stats line format: " + strLine);
					return;
				}
				String[] tokens = strLine.split(",");
				if (tokens.length != 10)
				{
					System.out.println("ERROR: Expect 10 fields in the stats file; found " + tokens.length);
					return;
				}
				// String sampleName = tokens[0].split("=")[1];
				// float padValue = Float.parseFloat(tokens[1].split("=")[1]);
				m_iCount = Long.parseLong(tokens[2].split("=")[1]);
				m_dMin = Float.parseFloat(tokens[3].split("=")[1]);
				m_dMax = Float.parseFloat(tokens[4].split("=")[1]);
				m_dMean = Double.parseDouble(tokens[5].split("=")[1]);
				m_dStdDev = Double.parseDouble(tokens[6].split("=")[1]);
				m_dFirstQuartile = Double.parseDouble(tokens[7].split("=")[1]);
				m_dMedian = Double.parseDouble(tokens[8].split("=")[1]);
				m_dThirdQuartile = Double.parseDouble(tokens[9].split("=")[1]);	
			}
			
			streamStats.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
			System.err.println("Unable to read " + fn);
		}
	}
	
	public void writeStats(String fn, String sampleName) 
	{
		try
		{
			// System.out.println("[Started] writing stats");
			FileOutputStream statFile = new FileOutputStream(fn);
			String s = "";
			s += "sample=" + sampleName + ",";
			s += "padValue=0.0,";
			s += "count=" + m_iCount + ",";
			s += "min=" + m_dMin + ",";
			s += "max=" + m_dMax + ",";
			s += "mean=" + m_dMean + ",";
			s += "std=" + m_dStdDev + ",";
			s += "firstQuartile=" + m_dFirstQuartile + ",";
			s += "median=" + m_dMedian + ",";
			s += "thirdQuartile=" + m_dThirdQuartile + "\n";
			statFile.write(s.getBytes());
			statFile.close();
			// System.out.println("[Done] writing stats");
			// updateProgress();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

}
