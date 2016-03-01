package chase.input;

import java.io.FileOutputStream;
import java.io.ObjectOutputStream;

public class BinaryDataTable extends DataTable {

	protected double[][] m_Matrix; // size:[rows][columns]  rows = num_regions. columns = num_samples*num_bins.
	protected String[] m_SampleNames;
	//protected String[] m_FeatureCoords;
	//protected String[] m_FeatureIds;
	//protected String[] m_FeatureNames;
	
	/**
	 * Adds a sample's data values to this table
	 * assuming sampleTable size[rows][columns] where
	 * rows = num_regions, columns = num_bins
	 * 
	 * @param sampleTable
	 */
	public void addSample(int offset, double[][] sampleTable) {
		if (sampleTable == null) { return; }
		int iRows = sampleTable.length;    // total number of regions
		int iCols = sampleTable[0].length; // number of columns to add
		int iCurrCol = offset * iCols;     // number of columns in current m_Matrix
		for (int iR = 0; iR < iRows; iR++) {
			System.arraycopy(sampleTable[iR], 0, m_Matrix[iR], iCurrCol, iCols);
		}
	}
	
	@Override
	public double[] getFeatureValues(int gIndex) {
		if (gIndex < 0 || m_Matrix == null || gIndex >= m_Matrix.length) {
			return null;
		} else {
			return m_Matrix[gIndex];
		}
	}
	
	@Override
	public int getNumBins() {
		if (m_Matrix == null || getNumSamples() == 0) {
			return 0;
		} else {
			return m_Matrix[0].length/getNumSamples();
		}
	}
	
	@Override
	public int getNumFeatures() {
		if (m_Matrix == null) {
			return 0;
		} else {
			return m_Matrix.length;
		}
	}
	
	@Override
	public int getNumSamples() {
		return m_SampleNames.length;
	}
	
	@Override
	public String[] getSampleNames() {
		return m_SampleNames;
	}
	
	@Override
	public String getSampleName(int i) {
		if (i < 0 || i >= m_SampleNames.length) {
			return null;
		} else {
			return m_SampleNames[i];
		}
	}

	// @Override
	/*public void populate() {
		try {
			assert(m_DataFiles.length == m_ProcessedDataFiles.length);
			for (int i=0; i<m_DataFiles.length; i++) {
				if (m_ProcessedDataFiles[i].exists()) {
					// get data from the preprocessed file
					readTable(m_ProcessedDataFiles[i].getAbsolutePath());
				} else {
					// get data from data file and regions file
					
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}*/
	
	// @Override
	/*public void populate() {
		try {
			int n = m_BinaryFileNames.length;
			if (n > 0) {
				double[][] firstTable = readTable(m_BinaryFileNames[0]);
				if (n == 1) {
					m_Matrix = firstTable;
				} else if (n > 1) {
					// need to merge other tables
					int iRows = firstTable.length;
					int iCols = firstTable[0].length;
					double[][] mergedTable = new double[iRows][iCols * n];
					
					// add first table (already parsed)
					int iCurrCol = 0;
					for (int iR = 0; iR < iRows; iR++) {
						System.arraycopy(firstTable[iR], 0, mergedTable[iR], iCurrCol, iCols);
					}
					iCurrCol += iCols;
					
					// add remaining tables
					for (int iT = 1; iT < n; iT++) {
						double[][] table = readTable(m_BinaryFileNames[iT]);
						for (int iR = 0; iR < iRows; iR++) {
							System.arraycopy(table[iR], 0, mergedTable[iR], iCurrCol, iCols);
						}
						iCurrCol += iCols;
					}
					m_Matrix = mergedTable;
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}*/

	/**
	 * Reads the table data from a binary file
	 * @param filename
	 */
	/*protected double[][] readTable(String filename) {
		double[][] table = null;
		try {
            FileInputStream fis = new FileInputStream(filename);
            ObjectInputStream ois = new ObjectInputStream(fis);
            table = (double[][])ois.readObject();
            ois.close();
            fis.close();
	    } catch (Exception e) {
			//TODO
			e.printStackTrace();
	    }
        return table;
	}*/
	
	/*public void setOutputFileNames(String[] bFiles) {
		m_BinaryFileNames = bFiles;
	}*/
	
	public double[][] getData()
	{
		return m_Matrix;
	}
	
	public void setData(double[][] values) {
		m_Matrix = values;
	}
	
	
	/**
	 * Set the sample names
	 * @param sNames
	 */
	@Override
	public void setSampleNames(String[] sNames) {
		m_SampleNames = sNames;
	}
	
	public void setSize(int nRows, int nCols) {
		m_Matrix = new double[nRows][nCols];
	}
	
	/**
	 * Outputs the table data into a set of binary
	 * files, one per row (data track)
	 * @param filename
	 */
	public void writeTables(String[] filenames) {
		// setOutputFileNames(filenames);
		try {
			int n = filenames.length;
			if (n == 1) {
				writeTable(m_Matrix, filenames[0]);
			} else if (n > 1) {
				int iRows = m_Matrix.length;
				int iCols = m_Matrix[0].length/n;
				int iCurrCol = 0;
				for (int i=0; i<n; i++) {
					String fn = filenames[i];
					double[][] table = new double[iRows][iCols];
					for (int iR = 0; iR < iRows; iR++) {
						System.arraycopy(m_Matrix[iR], iCurrCol, table[iR], 0, iCols);
					}
					iCurrCol += iCols;
					writeTable(table, fn);
				}
			}
	    } catch (Exception e) {
			//TODO
			e.printStackTrace();
	    }
	}
	
	public static void writeTable(double[][] table, String filename) {
		try {
			FileOutputStream fos = new FileOutputStream(filename);
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeObject(table);
			oos.close();
			fos.close();
		} catch (Exception e) {
			//TODO
			e.printStackTrace();
	    }
	}

}
