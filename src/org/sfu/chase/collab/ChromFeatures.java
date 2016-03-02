package org.sfu.chase.collab;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class ChromFeatures {
	
	/**
	 * Stores the features for all chromosomes
	 */
	protected HashMap<String, List<Feature>> m_ChromMap;
	
	/**
	 * Current array of features inside one chromosome
	 */
	protected List<Feature> m_CurrFeatures;
	protected String m_CurrChrom;
	
	/**
	 * Number of bins to use for Features
	 */
	protected int m_NumBins;
	
	/**
	 * Indices into current feature list (used for detecting overlaps)
	 */
	protected int currIndex = 0;
	protected int prevFirstMatchIndex = 0;
	
	/**
	 * Constructor
	 */
	public ChromFeatures()
	{
		m_CurrFeatures = new ArrayList<Feature>();
	}
	
	/**
	 * Adds a feature to the array of features.
	 * @param f		feature to add to the m_Features array
	 */
	public void add(Feature f)
	{
		String chrom = f.m_sSeqName;
		if (m_ChromMap == null) {
			m_ChromMap = new HashMap<String, List<Feature>>();
		}
		if (!m_ChromMap.containsKey(chrom)) {
			m_ChromMap.put(chrom, new ArrayList<Feature>());
		}
		m_ChromMap.get(chrom).add(f);
	}
	
	public boolean addDataValue(String chrom, int iStart, int iEnd, double value) 
	{
		// make sure feature's chrom is currently selected
		selectChrom(chrom);
		return addDataValue(iStart, iEnd, value);
	}
	
	/**
	 * Adds the data value to the the feature corresponding the range [iStart, iEnd]  
	 * @param iStart	start of the data value span range
	 * @param iEnd		end of the data value span range
	 * @param value	data value to be added
	 */
	
	public boolean addDataValue(int iStart, int iEnd, double value)
	{
		// assumes correct chrom is currently selected
		int index; 
		int count = 0;
		// start search for overlaps based on previous first match index
		currIndex = prevFirstMatchIndex;
		while ((index = findFeatureIndex(iStart, iEnd)) >= 0) {
			Feature cf = m_CurrFeatures.get(index);
			// attempt to add this data value to the feature
			if (!cf.addDataValue(iStart, iEnd, value)) 
			{
				return false;
			}
			if (count == 0) {
				// first match to this feature
				prevFirstMatchIndex = index;
			}
			count++;
		}
		return true;
	}
	
	/**
	 * Clears all features.
	 */
	public void clearDataValues()
	{
		if (m_ChromMap != null)
		{
			for (String chrom: m_ChromMap.keySet()) {
				for (Feature f: m_ChromMap.get(chrom)) {
					f.clearDataValues();
				}
			}
		}
		m_CurrFeatures = null;
		m_CurrChrom = null;
		currIndex = 0;
		prevFirstMatchIndex = 0;
	}
	
	/**
	 * Finds the index of a feature in m_Features array, that overlaps the [start, end] range. 
	 */
	public int findFeatureIndex(int iStart, int iEnd) 
	{
		int index = 0;
		// search has reached the end of the feature set
		Feature currFeature = null;
		if (currIndex == m_CurrFeatures.size()) { return -1; }
		try {
			currFeature = m_CurrFeatures.get(currIndex);
		} catch (IndexOutOfBoundsException e) {
			e.printStackTrace();
		}
		if (iStart > currFeature.m_iEnd) {
			// case 1: query start is downstream of current feature
			// continue search for overlap
			currIndex++;
			index = findFeatureIndex(iStart, iEnd);
		} else if (iEnd < currFeature.m_iStart) {
			// case 2: query end is upstream of current feature
			// discontinue search for overlap
			index = -1;
		} else {
			// case 3: overlap
			index = currIndex;
			currIndex++;
		}
		return index;
	}
	
	public boolean hasFeatures() {
		if (m_CurrFeatures == null) {
			return false;
		} else {
			return true;
		}
	}
	
	public Set<String> getChroms() {
		return m_ChromMap.keySet();
	}
	
	public int getNumBins() {
		return m_NumBins;
	}
	
	public List<Feature> getFeatures(String chrom) {
		if (m_ChromMap == null || chrom == null) { return null; }
		return m_ChromMap.get(chrom);
	}
	
	public Feature[] getFeatures() {
		int iNumFeatures = getNumFeatures();
		if (iNumFeatures == 0) { return null; }
		Feature[] features = new Feature[iNumFeatures];
		
    	int iRow = 0;
    	for (List<Feature> currFeatures:m_ChromMap.values())
    	{
    		for (int i = 0; i < currFeatures.size(); i++)
    		{
    			features[iRow++] = currFeatures.get(i);
    		}
		}
    	Arrays.sort(features);
    	return features;
	}
	
	public int getNumFeatures() {
		if (m_ChromMap == null) { return 0; }

		int iNumFeatures = 0;
    	for (List<Feature> cf:m_ChromMap.values())
    	{
    		iNumFeatures += cf.size();
		}
    	return iNumFeatures;
	}
	
	public void selectChrom(String chrom) {
		assert(chrom != null);
		if (chrom.equals(m_CurrChrom)) { return; }
		m_CurrFeatures = m_ChromMap.get(chrom);
		m_CurrChrom = chrom;
		currIndex = 0;
		prevFirstMatchIndex = 0;
	}
	
	/**
	 * Number of bins is currently the same for every feature
	 * @param n
	 */
	public void setNumBins(int n) {
		if (n != m_NumBins) {
			// need to re-initialize features with new number of bins
			if (m_ChromMap != null) {
				for (String chrom: m_ChromMap.keySet()) {
					for (Feature f: m_ChromMap.get(chrom)) {
						f.setNumBins(n);
					}
				}
			}
		}
		m_NumBins = n;
	}
	
	/**
	 * Sorts the features in m_Features, by their "start" and then "end"
	 */
	public void sortFeatures()
	{
		// sort the features for all chromosomes
		for (List<Feature> cf:m_ChromMap.values())
		{
			Collections.sort(cf, new Comparator<Feature>() {
				public int compare(Feature f1, Feature f2)
				{
					return f1.m_iStart < f2.m_iStart ? -1 : (f1.m_iStart > f2.m_iStart ? 1 : 0);
				}
			});
		}
	}

}
