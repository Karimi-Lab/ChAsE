package org.sfu.chase.input;

public abstract class DataTable {
	
	protected String[] m_FeatureCoords;
	protected String[] m_FeatureIds;
	protected String[] m_FeatureNames;

	public String getFeatureCoord(int gIndex) {
		if (m_FeatureCoords == null) {
			return null;
		} else {
			return m_FeatureCoords[gIndex];
		}
	}
	
	public String getFeatureName(int gIndex) {
		if (m_FeatureNames == null) {
			return null;
		} else {
			return m_FeatureNames[gIndex];
		}
	}
	
	public String[] getFeatureNames() {
		return m_FeatureNames;
	}
	
	public abstract double[] getFeatureValues(int gIndex);
	
	public abstract double[][] getData();
	
	/**
	 * Number of bins used to represent each feature
	 * @return
	 */
	public abstract int getNumBins();
	
	public abstract int getNumFeatures();
	
	public abstract int getNumSamples();
	
	public abstract String[] getSampleNames();
	
	public abstract String getSampleName(int i);
	
	// public abstract void populate();
	
	/**
	 * Set the feature coords (e.g. "chr1:1000-3000")
	 * @param coords
	 */
	public void setFeatureCoords(String[] coords) {
		m_FeatureCoords = coords;
	}
	
	/**
	 * Set the feature ids (e.g. 'NM_0000001')
	 * @param ids
	 */
	public void setFeatureIds(String[] ids) {
		m_FeatureIds = ids;
	}
	
	/**
	 * Set the feature names (e.g. "Hoxa")
	 * @param names
	 */
	public void setFeatureNames(String[] fNames) {
		m_FeatureNames = fNames;
	}

	public abstract void setSampleNames(String[] sNames);
	
	public abstract void writeTables(String[] fNames);
	
}
