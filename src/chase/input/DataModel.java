/**
 * Modifield from spark.dataloader.DataModel.
 * Date: May 29, 2012
 */

package chase.input;
/**
 * Manages all data to be used by Spark, both
 * its PreProcesssor and GUI.
 * 
 * Can populate these data structures from input
 * files specified in the Parameters object, 
 * although most file readers are in separate
 * classes (thus potentially reusable).
 * 
 * @author Cydney Nielsen
 */
import java.awt.Toolkit;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.SwingWorker;

import org.apache.log4j.Logger;
import org.sfu.chase.Experiment;

import collab.BioReader;
import collab.Feature;
import collab.DataStats;
import collab.Downloader;

//HY import cluster.ClusterFileParser;
//HY import cluster.ClusterFileWriter;
//HY import cluster.ClusterSet;

//HY import parameters.Parameters;

public class DataModel {

	// core data structures to populate
	protected Parameters m_Param;
	protected DataTable m_DataTable;	// binned and normalized data table
	protected DataTable m_UnNormDataTable;  // unnormalized data table
	//HY protected ClusterSet m_ClusterSet;
	protected DataStats[] m_RegionalStats;
	protected DataStats[] m_GlobalStats;
	protected ArrayList<Experiment> m_Experiments;
	protected ArrayList<Integer> m_VisibleExperimentIndex;
	
	// for populating the above
	protected BioReader m_DataReader;
	//protected ClusterFileParser m_ClusterReader;
	protected Downloader m_Downloader;
	protected Feature[] m_Features;
	protected boolean m_ParseSampleNames = false;
	
	// for monitoring progress
	protected int m_Progress;
	protected PropertyChangeSupport m_PropertyChangeSupport;
	protected static String PROGRESS_PREFIX = ProgressDialog.PROGRESS_PREFIX;
	// for reporting progress e.g. "Downloading data file 1 of 4"
	protected int currentFileCount = 0;
	protected int currentFileIndex = 0;
	
	public DataModel() {
    	m_Experiments = new ArrayList<Experiment>();
		m_Progress = 0;
		m_PropertyChangeSupport = new PropertyChangeSupport(this);
		m_DataReader = new BioReader();
	}
	
	public ArrayList<Experiment> getExperiments() {
		return m_Experiments;
	}
	
	public Experiment getExperiment(int index) {
		if (index >= 0 && index < m_Experiments.size())
			return m_Experiments.get(index);
		return null;
	}
	
	public Experiment getVisibleExperiment(int index)
	{
		if (index >= 0 && index < m_VisibleExperimentIndex.size()) {
			return m_Experiments.get(m_VisibleExperimentIndex.get(index));
		}
		return null;
	}
	
	public DataStats getVisibleExperimentStats(int index, String statType)
	{
		if (index >= 0 && index < m_VisibleExperimentIndex.size()) {
			if (statType.equalsIgnoreCase(Parameters.GLOBAL)) {
				return m_GlobalStats[m_VisibleExperimentIndex.get(index)];
			} else {
				return m_RegionalStats[m_VisibleExperimentIndex.get(index)];
			}
		}
		
		return null;
	}
	
	public void load(File aDir) throws IOException, IllegalArgumentException, InterruptedException {
		m_Param = new Parameters();
		Logger.getLogger(getClass()).debug("Loading properties from analysis dir '" + aDir + "'");
		m_Param.load(aDir);
		//m_DataModel = new DataModel();
		loadData(m_Param);
		//buildNameLookups();
		//buildNameHash();
	}
	
	
	public void loadData(Parameters param) throws IOException, IllegalArgumentException, InterruptedException
	{
		m_Param = param;
		
		m_DataReader = new BioReader();
		m_DataReader.addPropertyChangeListener(new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				String name = evt.getPropertyName();
				if (name.startsWith(PROGRESS_PREFIX)) {
					name += " (" + currentFileIndex + " of " + currentFileCount + ")";
					//try {
						int progress = (Integer) evt.getNewValue();
						// fire a property change for this DataModel
						firePropertyChange(name, m_Progress, progress);
						m_Progress = progress;
					//} catch (Exception e) {
					//	e.printStackTrace();
					//}
				}
				else
				{// pass it as is
					firePropertyChange(evt.getPropertyName(), evt.getOldValue(), evt.getNewValue());
				}
			}
		});
		
		m_Downloader = new Downloader();
		m_Downloader.addPropertyChangeListener(new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				String name = evt.getPropertyName();
				if (name.startsWith(PROGRESS_PREFIX)) {
					name += " (file " + currentFileIndex + " of " + currentFileCount + ")";
					//try {
						int progress = (Integer) evt.getNewValue();
						// fire a property change for this DataModel
						firePropertyChange(name, m_Progress, progress);
						m_Progress = progress;
					//} catch (Exception e) {
					//	e.printStackTrace();
					//}
				}
			}
		});
		
		populate();
	}
	
	/**
	 * Returns the size of the largest region in the region set
	 */
	public int getMaxRegionSize()
	{
		return m_DataReader.getMaxRegionSize();
	}
	
	// saves the experimetns to the parameters
	void writeExperimentsToParams()
	{
		m_Param = new Parameters();
		
		int numXP = m_Experiments.size();
		String[] sNames = new String[numXP];
		String[] sFilenames = new String[numXP];
		String[] normTypes = new String[numXP];
		String[] statTypes = new String[numXP];
		String[] binTypes  = new String[numXP];
		boolean[] logs = new boolean[numXP];
		boolean[] visibles = new boolean[numXP];
		String[] colorLabels = new String[numXP];
		
		for (int i = 0; i < numXP; ++i)
		{
			sNames[i] = m_Experiments.get(i).getName();
			sFilenames[i] = m_Experiments.get(i).getFilename();
			normTypes[i] = m_Experiments.get(i).getNormType();
			statTypes[i] = m_Experiments.get(i).getStatType();
			binTypes[i] = m_Experiments.get(i).getBinningType();
			logs[i] = m_Experiments.get(i).isLogScale();
			visibles[i] = m_Experiments.get(i).isVisible();
			colorLabels[i] = m_Experiments.get(i).getColor().getColorString();
		}
		
		m_Param.setSampleNames(sNames);
		m_Param.setDataFileNames(sFilenames);
		m_Param.setNormTypes(normTypes);
		m_Param.setStatsTypes(statTypes);
		m_Param.setBinningTypes(binTypes);
		m_Param.setLogScales(logs);
		m_Param.setVisibles(visibles);
		m_Param.setColorLabels(colorLabels);
	}
	
	// load the experiments from the parameters file
	void writeParamsToExperiments()
	{
		if (m_Param == null)
			return;
		
		String[] sNames = m_Param.getSampleNames();
		String[] sFilenames = m_Param.getDataFileNames();
		String[] normTypes = m_Param.getNormTypes();
		String[] statTypes = m_Param.getStatsTypes();
		String[] binTypes = m_Param.getBinningTypes();
		boolean[] logs = m_Param.getLogScales();
		boolean[] visibles = m_Param.getVisibles();
		String[] colorLabels = m_Param.getColorLabels();
		int numXP = sFilenames.length;
		
		m_Experiments = new ArrayList<Experiment>();
		m_VisibleExperimentIndex = new ArrayList<Integer>();
		
		for (int i = 0; i < numXP; ++i)
		{
			Experiment newXP = new Experiment();
			if (visibles != null)
				newXP.setVisible(i < visibles.length ? visibles[i] : visibles[0]);
			newXP.setName(sNames[i]);
			newXP.setFilename(sFilenames[i]);
			newXP.setNormType(i < normTypes.length ? normTypes[i] : normTypes[0]);
			newXP.setStatType(i < statTypes.length ? statTypes[i] : statTypes[0]);
			newXP.setBinningType(i < binTypes.length ? binTypes[i] : binTypes[0]);
			if (logs != null)
				newXP.setLogScale(i < logs.length ? logs[i] : logs[0]);
			newXP.getColor().setColor(colorLabels[i]);
			
			m_Experiments.add(newXP);
			if (newXP.isVisible()) {
				m_VisibleExperimentIndex.add(i);
			}
		}
	}
	
	/**
	 * Returns the base filename from an input string,
	 * which can either be a URL or a file path.
	 * 
	 * @param fn
	 * @return
	 */
	protected String getBaseFileName(String path) {
		String fName = null;
		if (path == null) { return fName; }
		try {
			// try to get name from URL string
			URL url = new URL(path);
			int slashIndex = url.getPath().lastIndexOf('/');
			fName = url.getPath().substring(slashIndex + 1);
		} catch (MalformedURLException e) {
			// try to get name from path
			fName = (new File(path)).getName();
		}
		return fName;
	}
	
	/*//HY
	public double[] getCentroid(int cIndex) {
		return m_ClusterSet.getCentroid(cIndex);
	}
	
	public ClusterSet getClusterSet() {
		return m_ClusterSet;
	}
	
	public int getClusterSize(int cIndex) {
		return m_ClusterSet.getClusterSize(cIndex);
	}
	*/
	
	public DataTable getDataTable() {
		return m_DataTable;
	}
	
	public DataTable getUnNormDataTable() {
		return m_UnNormDataTable;
	}
	
	
	public Feature[] getFeatures() {
		return m_Features;
	}
	
	/*//HY
	public String getFeatureCoord(int cIndex, int fIndex) {
		return m_ClusterSet.getFeatureCoord(cIndex, fIndex);
	}
	
	public String getFeatureName(int cIndex, int fIndex) {
		return m_ClusterSet.getFeatureName(cIndex, fIndex);
	}
	
	public String[] getFeatureNames(int cIndex) {
		return m_ClusterSet.getFeatureNames(cIndex);
	}
	
	public String[] getFeatureNames() {
		return m_DataTable.getFeatureNames();
	}
	
	public String[] getFeatureIds(int cIndex) {
		return m_ClusterSet.getFeatureIds(cIndex);
	}
	
	public double[] getFeatureValues(int cIndex, int fIndex) {
		return m_ClusterSet.getFeatureValues(cIndex, fIndex);
	}*/
	
	protected void getFileFromURL(String possibleURL, File targetDir) throws IOException, MalformedURLException {
		if (targetDir == null) {
			throw new IOException("No cache directory");
		} else {
			// download file if it is a URL
			URL url = new URL(possibleURL);
			m_Downloader.download(url, targetDir);
		}
	}
	
	/*//HY
	public int getKvalue() {
		return m_Param.getKvalue();
	}
	
	public int getNumClusters() {
		return m_ClusterSet.getNumClusters();
	}
	
	public int getNumFeatures() {
		return m_DataTable.getNumFeatures();
	}
	
	public int getNumSamples() {
		return m_DataTable.getNumSamples();
	}*/
	
	public Parameters getParam() {
		return m_Param;
	}
	
	public int getRandomSeed() {
		return m_Param.getRandomSeed();
	}
	
	/*//HY
	public String[] getSampleNames() {
		return m_DataTable.getSampleNames();
	}
	
	public String getSampleName(int i) {
		return m_Experiments.get(i).getName();
	}*/
	
	protected double[][] handleProcessedData(String[] fileNames, int index) throws IOException {
		String fileName = fileNames[index];
		currentFileIndex = index+1;
		currentFileCount = fileNames.length;
		double[][] dataTable = null;
		// get the base file name
		String baseName = getBaseFileName(fileName);
		// first look for local preprocessed data file
		File localFile = new File(m_Param.getProcessedDataDir(), baseName);
		if (!localFile.exists()) {
			// attempt to connect as URL and download file
			try {
				getFileFromURL(fileName, m_Param.getProcessedDataDir());
				localFile = new File(m_Param.getProcessedDataDir(), baseName);
			} catch (MalformedURLException e) {
				// can happen if fName is a local file
				return null;
			}
		}
		
		m_Experiments.get(index).readTable(localFile.getAbsolutePath());
		dataTable = m_Experiments.get(index).getData();
		
		/*//HY normalization is moved to computeDataTable()
		dTable = readBinaryTable(localFile.getAbsolutePath());
		// record new local file name
		fNames[index] = localFile.getAbsolutePath();
		
		// normalize the data table
		DataStats normStats = null;
		if (m_Param.getStatsTypes().equals(Parameters.GLOBAL)) {
			normStats = m_GlobalStats[index];
		} else if (m_Param.getStatsTypes().equals(Parameters.REGIONAL)) {
			normStats = m_RegionalStats[index];
		}
		
		normStats = m_RegionalStats[index];
		String normType = Parameters.LINEAR; //HY m_Param.getNormType()
		
		if (normStats == null) {
			throw new IOException("ERROR: cannot process a .dat file without companion .stats file");
		} else {
			dTable = BioReader.createNormalizedTable(dTable, normType, normStats);
		}
		
		if (m_ParseSampleNames) {
			// the purpose of having these .dat files is that you do not require the local data file
			// therefore should not have to get sample names from the primary data file
			throw new IOException("ERROR: must provide sample names for each .dat file");
		}
		*/
		return dataTable;
	}
	
	protected double[][] handleReadCountFiles(String[] fileNames, int index) throws IOException {
		String fileName = fileNames[index];
		currentFileIndex = index+1;
		currentFileCount = fileNames.length;
		double[][] countTable = null;
		// get the base file name
		String baseName = getBaseFileName(fileName);
		// first look for local preprocessed data file
		File localFile = new File(m_Param.getProcessedDataDir(), baseName);
		if (!localFile.exists()) {
			// attempt to connect as URL and download file
			try {
				getFileFromURL(fileName, m_Param.getProcessedDataDir());
				localFile = new File(m_Param.getProcessedDataDir(), baseName);
			} catch (MalformedURLException e) {
				// can happen if fName is a local file
				return null;
			}
		}
		
		m_Experiments.get(index).readReadCountTable(localFile.getAbsolutePath());
		countTable = m_Experiments.get(index).getReadCountTable();
		
		return countTable;
	}	
	
	
	/**
	 * Get data values from input data file and regions file.
	 * Will create the preprocessed .dat and .stat files
	 * Note: regions were populated within m_Reader during processGFF()
	 * @param fName
	 * @param stats
	 * @return
	 * @throws IOException
	 */
	// read wig?
	protected boolean handlePrimaryData(String[] dataFileNames,          //".wig"
			                            String[] processedDataFileNames, //".dat"
			                            String[] readCountFilenames,     // ".count"
			                            String[] sampleNames,
			                            int index) throws IOException, InterruptedException 
	{
		String fName = dataFileNames[index];
		currentFileIndex = index+1;
		currentFileCount = dataFileNames.length;
		double[][] dataTable = null;
		double[][] readCountTable = null;
		
		String baseName = getBaseFileName(fName);

		File localFile = null;
		// first check for a URL (Downloader class will check for cached version)
		try {
			getFileFromURL(fName, Parameters.getCacheDir());
			localFile = new File(Parameters.getCacheDir(), baseName);
		} catch (MalformedURLException e) {
			// assume fName is a local file
			localFile = new File(fName);
			if (!localFile.exists()) {
				throw new IOException("ERROR: Cannot find file " + fName);
			}
		}
		if (fName.toLowerCase().endsWith(".bigwig") || fName.toLowerCase().endsWith(".bw")) {
			// process big wig
			m_DataReader.readBigWig(localFile.getAbsolutePath(), m_Param.getNumBins(), m_Param.getIncludeZerosState(), false);
		} else if (fName.toLowerCase().endsWith(".wig") || fName.toLowerCase().endsWith(".wig.gz") || fName.toLowerCase().endsWith(".wig.zip")) {
			m_DataReader.readWIG(localFile.getAbsolutePath(), m_Param.getNumBins(), m_Param.getIncludeZerosState(), false);
		} else {
			throw new IOException("ERROR: Unexpected file format in " + fName);
		}
		
		dataTable = m_DataReader.getDataTable();
		m_Experiments.get(index).setData(dataTable);
		m_Experiments.get(index).writeTable(processedDataFileNames[index]);
		
		readCountTable = m_DataReader.getReadCountTable();
		m_Experiments.get(index).setReadCountTable(readCountTable);
		m_Experiments.get(index).writeReadCountTable(readCountFilenames[index]);
		
		// IMPORTANT: write out the Un-Normalized version of the table (later lost)
		//BinaryDataTable.writeTable(dTable, pNames[index]);
		
		// normalize the data table
		DataStats gStats = m_DataReader.getGlobalStats();
		DataStats rStats = m_DataReader.getRegionStats();
		m_GlobalStats[index] = gStats;
		m_RegionalStats[index] = rStats;
		
		/*//HY normalization is done later in computeDataTable()
		String statsType = m_Param.getStatsType();
		DataStats normStats = statsType.equals(Parameters.GLOBAL) ? gStats : rStats;
		//dTable = m_DataReader.createNormalizedTable(dTable, m_Param.getNormType(), normStats);
		*/
		if (m_ParseSampleNames) {
			sampleNames[index] = m_DataReader.getSampleName();
		}
		
		String[] globalStatFilenames = m_Param.getGlobalStatsFileNames();
		assert(globalStatFilenames.length == m_GlobalStats.length);
		assert(sampleNames.length == m_GlobalStats.length);
		m_GlobalStats[index].writeStats(globalStatFilenames[index], sampleNames[index]);
		
		String[] regionalStatFilenames = m_Param.getRegionalStatsFileNames();
		assert(regionalStatFilenames.length == m_RegionalStats.length);
		assert(sampleNames.length == m_RegionalStats.length);
		m_RegionalStats[index].writeStats(regionalStatFilenames[index], sampleNames[index]);
		
		return true;
	}
	
	protected DataStats handleStats(String[] fNames, int index) throws IOException
	{
		String fName = fNames[index];
		currentFileIndex = index+1;
		currentFileCount = fNames.length;
		DataStats s = null;
		// get the base file name
		String baseName = getBaseFileName(fName);
		// first look for local stats data file
		File localFile = new File(m_Param.getStatsDir(), baseName);
		if (!localFile.exists()) {
			// attempt to connect as URL and download file
			try {	
				getFileFromURL(fName, m_Param.getStatsDir());
				localFile = new File(m_Param.getStatsDir(), baseName);
			} catch (MalformedURLException e) {
				// can happen if fName is a local file
				return null;
			}
		}
		s = new DataStats();
		s.readStats(localFile.getAbsolutePath());
		
		// record new local file name
		fNames[index] = localFile.getAbsolutePath();
		return s;
	}
	
	protected void populate() throws IOException, IllegalArgumentException, InterruptedException {

		String[] dataFileNames = m_Param.getDataFileNames();
		String[] processesDataFileNames = m_Param.getProcessedDataFileNames();
		String[] readCountFileNames = m_Param.getReadCountFileNames();
		
		// String[] sFileNames = m_Param.getStatsFileNames();
		String[] gStatsFileNames = m_Param.getGlobalStatsFileNames();
		String[] rStatsFileNames = m_Param.getRegionalStatsFileNames();

		m_DataTable = new BinaryDataTable();
		m_UnNormDataTable = new BinaryDataTable();
		m_GlobalStats = new DataStats[dataFileNames.length];
		m_RegionalStats = new DataStats[dataFileNames.length];

		// Step 1: get sample names
		String[] sampleNames = m_Param.getSampleNames();
		m_ParseSampleNames = false;
		if (sampleNames == null) {
			// parse sample names from data files if none provided by the user
			m_ParseSampleNames = true;
			sampleNames = new String[dataFileNames.length];
		}

		// Step 2 : get stats information (if any at this point)
		assert(dataFileNames.length == gStatsFileNames.length);
		assert(dataFileNames.length == rStatsFileNames.length);
		
		// HY: moved below 
		//for (int i=0; i<gStatsFileNames.length; i++) {
		//	DataStats s = handleStats(gStatsFileNames, i);
		//	if (s != null) { m_GlobalStats[i] = s; }
		//	s = handleStats(rStatsFileNames, i);
		//	if (s != null) { m_RegionalStats[i] = s; }
		//}
		
		// Step 3: get regions information (region ids, coords, and names)
		// from clusters files if they exist
		File cMembersFile = new File(m_Param.getClusterMemberFileName());
		if (cMembersFile.exists()) {
			// get this information from the clusters GFF file (together with cluster id for each region)
			File cValuesFile = new File(m_Param.getClusterValuesFileName());
			populateClusterSet(cValuesFile, cMembersFile);
		} else
		{
			// get this information from the regions GFF file
			firePropertyChange(PROGRESS_PREFIX + "Loading regions", m_Progress, 0);
			processGFF(m_Param.getRegionsFileName());
		}
		
		if (m_Param.getEqualRegionSize())
		{
		    m_DataReader.resizeAllRegions(m_Param.getRegionsSize());
		}
		m_DataReader.setNumBins(m_Param.getNumBins());
		writeParamsToExperiments();
		
		// initialize complete table
		//HY moved to computeDataTable()
		//((BinaryDataTable)m_DataTable).setSize(m_Features.length, m_Param.getNumBins() * dFileNames.length);

		// Step 4: get data values and output unnormalized version if necessary
		assert(dataFileNames.length == processesDataFileNames.length);
		for (int i = 0; i < processesDataFileNames.length; i++) {
			double[][] dataTable = handleProcessedData(processesDataFileNames, i);
			double[][] countTable = handleReadCountFiles(readCountFileNames, i);
			
			if (dataTable != null && countTable != null) {
				DataStats s = handleStats(gStatsFileNames, i);
				if (s != null) { 
					m_GlobalStats[i] = s; 
				}
				s = handleStats(rStatsFileNames, i);
				if (s != null) { 
					m_RegionalStats[i] = s; 
				}
				if (m_GlobalStats[i] == null || m_RegionalStats[i] == null) {
					// can't find the stat files. need to preprocess again.
					dataTable = null;
				}
			}
			
			if (dataTable == null || countTable == null) {
				// pFileNames may be URLs; must reset to local file names
				m_Param.reinitializeDirectories();
				handlePrimaryData(dataFileNames, m_Param.getProcessedDataFileNames(), 
						m_Param.getReadCountFileNames(), sampleNames, i);
			}
			// store values for this wig in mergedTable
			//HY moved to computeDataTable()
			//((BinaryDataTable)m_DataTable).addSample(i, dTable);
		}
		
		// get sample names from the data files
		//HY moved to computeDataTable();  
		//m_DataTable.setSampleNames(sampleNames);
		// ensure parameters are updated with local list
		m_Param.setDataFileNames(dataFileNames);
		m_Param.setSampleNames(sampleNames);
		// update written properties
		m_Param.writeToFile();
	}
	
	
	// Computes the final binned and normalized binary data table, stored in m_DataTable
	void computeDataTable() throws IOException
	{
	    System.gc();
	    
		int iNumVisible = 0;
		for (Experiment xp: m_Experiments)
		{
			iNumVisible += xp.isVisible() ? 1 : 0;
		}
		
		if (iNumVisible == 0) {
			throw new IOException("ERROR: At least one experiment should be visible");
		}
		
		// initialize complete table
		((BinaryDataTable)m_DataTable).setSize(m_Features.length, m_Param.getNumBins() * iNumVisible);
		((BinaryDataTable)m_UnNormDataTable).setSize(m_Features.length, m_Param.getNumBins() * iNumVisible);

		String[] sampleNames = new String[iNumVisible];
		int offset = 0;
		// normalize the data table
		m_VisibleExperimentIndex = new ArrayList<Integer>();
		
        double[] dBinSizes = new double[m_Features.length];
		if (m_Param.getEqualRegionSize()) { 
		    double dEqualBinSize = 1.0*m_Param.getRegionsSize()/m_Param.getNumBins();
		    Arrays.fill(dBinSizes, dEqualBinSize);
		} else {
            for (int f = 0; f < m_Features.length; ++f) {
                dBinSizes[f] = m_Features[f].getBinSize();
            }
		}
        
		for (int i = 0; i < m_Experiments.size(); i++)
		{
			Experiment xp = m_Experiments.get(i);
			if (!xp.isVisible()) {
				continue;
			}
			
			m_VisibleExperimentIndex.add(i);
			
			//DataStats normStats = xp.getStatType().equals(Parameters.GLOBAL) ? m_GlobalStats[i] : m_RegionalStats[i];
			
			boolean useGlobalStats = xp.getStatType().equals(Parameters.GLOBAL);
			DataStats normStats = null;
			if (useGlobalStats) {
				normStats = m_GlobalStats[i]; 
				if (normStats == null) {
					throw new IOException("ERROR: cannot process a .dat file without companion .stats file");
				}
			} else	{// regional stats will be recalculated later in applyBinningType
				normStats = new DataStats();
				m_RegionalStats[i] = normStats;
			}
			
			double[][] dTable = null;
			dTable = BioReader.applyBinningType(xp.getData(), xp.getReadCountTable(), dBinSizes, xp.getBinningType(), useGlobalStats ? null : normStats);
			
			BioReader.normalizeTable(dTable, xp.getNormType(), normStats);
			((BinaryDataTable)m_DataTable).addSample(offset, dTable);
			((BinaryDataTable)m_UnNormDataTable).addSample(offset, xp.getData());
			
			sampleNames[offset] = xp.getName();
			offset++;
		}
		
		m_DataTable.setSampleNames(sampleNames);
		m_UnNormDataTable.setSampleNames(sampleNames);
	}
	
	protected void populateClusterSet(File cValuesFile, File cMembersFile) throws IOException, InterruptedException
	{
		firePropertyChange(PROGRESS_PREFIX + "Loading regions", m_Progress, 0);
		processGFF(cMembersFile.getAbsolutePath());
		
/*// HY		
		m_ClusterReader = new ClusterFileParser();
		m_ClusterReader.parseClusters(cValuesFile, cMembersFile);
		m_Features = m_ClusterReader.getFeatures();
		// sanity check
//		if (m_ClusterReader.getKvalue() != m_Param.getKvalue()) {
//			throw new IOException("Cluster file contains different number of clusters than specified in properties.txt: " + 
//					m_ClusterReader.getKvalue() + " " + m_Param.getKvalue());
//		}
//		if (m_ClusterReader.getNumBins() != m_Param.getNumBins()) {
//			throw new IOException("Cluster file contains different number of bins than specified in properties.txt: " + 
//					m_ClusterReader.getNumBins() + " " + m_Param.getNumBins());
//		}
		// set relevant info in data table
		m_DataTable.setFeatureCoords(m_ClusterReader.getFeatureCoords());
		m_DataTable.setFeatureIds(m_ClusterReader.getFeatureIds());
		m_DataTable.setFeatureNames(m_ClusterReader.getFeatureNames());
		// make cluster set - uses data table
		m_ClusterSet = new ClusterSet(m_DataTable, m_ClusterReader.getKvalue());
		// set relevant info in cluster set
		boolean toSort = false; // use order in cluster file
		m_ClusterSet.setData(m_ClusterReader.getClusterCentroids(), m_ClusterReader.getClusterSizes(), 
				m_ClusterReader.getClusterIndices(), m_ClusterReader.getClusterNames(), toSort);
	*/
		
	}
	
	protected void processGFF(String fName) throws IOException, InterruptedException
	{
		String baseName = getBaseFileName(fName);
		File localFile = null;
		// first check for a URL - Downloader class will generate a cached version
		try {
			getFileFromURL(fName, Parameters.getCacheDir());
			localFile = new File(Parameters.getCacheDir(), baseName);
		} catch (IOException e) {
			// otherwise assume fName is a local file
			localFile = new File(fName);
			if (!localFile.exists()) {
				localFile = new File(m_Param.getAnalysisDir().getAbsolutePath() + "/" + fName);
				if (!localFile.exists())
					throw new IOException("ERROR: Cannot find file " + fName);
			}
		}
		
		// parse the region information         
		m_DataReader.readGFF(localFile.getAbsolutePath());
		// parser sorts regions genomically - assumed to be in the same order as features in fMatrix
		m_Features = m_DataReader.getFeatures();
		
		// several items to gather
		String[] featureNames = null;  // e.g. 'hoxa1'
		String[] featureIds = new String[m_Features.length];    // e.g. 'NM_063718'
		String[] featureCoords = new String[m_Features.length]; // e.g. 'chr1:1000-1500'

		// process all regions
		String[] attributes;
		int progress = 0;
		for (int i=0; i<m_Features.length; i++) {
			String attString = m_Features[i].getGroup();
			if (attString != null) {
				attributes = attString.split(";");
				// handle case where there is a single group value (convert it to an attribute)
				if (attributes.length == 1) {
					if (attributes[0].equals(".")) {
						m_Features[i].setGroup("null");
					} else {
						// assume that it is a meaningful group name
						m_Features[i].setGroup("group=" + attributes[0].replace(" ", ""));
						m_Features[i].updateString();
					}
				} else {
					for (String a: attributes) {
						String[] parts = a.replace(" ", "").split("=");
						if (parts[0].equals("Name") || parts[0].equals("name")) {
							if (featureNames == null) {
								featureNames = new String[m_Features.length];
							}
							featureNames[i] = parts[1];
						} else if (parts[0].equals("ID") || parts[0].equals("id") || parts[0].equals("Id")) {
							featureIds[i] = parts[1];
						}
					}
				}
			}
			featureCoords[i] = m_Features[i].getSeqName() + " : " + m_Features[i].getStart() + " - " + m_Features[i].getEnd();
			progress = (i*100)/m_Features.length;
			firePropertyChange(PROGRESS_PREFIX + "Loading regions", m_Progress, progress);
			m_Progress = progress;
		}
		
		//HY TODO
		m_DataTable.setFeatureNames(featureNames);
		m_DataTable.setFeatureIds(featureIds);
		m_DataTable.setFeatureCoords(featureCoords);
		
		// set gff base name in properties (no longer depend on the path)
		// m_Param.setRegionsFileName(baseName);
		firePropertyChange(PROGRESS_PREFIX + "Loading regions", m_Progress, 100);
	}
	
	/*//HY: moved to Experiment
	protected double[][] readBinaryTable(String fn) {
		double[][] table = null;
		try {
            FileInputStream fis = new FileInputStream(fn);
            ObjectInputStream ois = new ObjectInputStream(fis);
            table = (double[][])ois.readObject();
            ois.close();
            fis.close();
	    } catch (Exception e) {
			//TODO
			e.printStackTrace();
	    }
        return table;
	}
	*/

//HY	
//	public void setClusterSet(ClusterSet cSet) {
//		m_ClusterSet = cSet;
//	}
//    /**
//     * Writes the cluster ids and centroids, to the file formats required by Spark GUI
//     * @param gffFilename		the .gff filename, to contain features/regions and cluster ids
//     * @param valuesFilename	the .values filename to contain cluster centroids
//     */
//    public void writeClusters()
//    {
//    	String gffFilename = m_Param.getClusterMemberFileName();
//    	String valuesFilename = m_Param.getClusterValuesFileName();
//    	ClusterFileWriter w = new ClusterFileWriter();
//    	if (m_ClusterSet != null) {
//    		w.writeClusters(m_Features, m_ClusterSet, gffFilename, valuesFilename);
//    	}
//    }
	
	/**
	 * Outputs the table data into a binary file
	 * @param filename
	 */
	//HY
	/*
	public void writeDataTables()
	{
		m_DataTable.writeTables(m_Param.getProcessedDataFileNames());
	}*/
	
	/**
	 * Writes out both global and regional stats
	 */
	public void writeStats() 
	{
		//HY: moved to handlePrimaryData()
		/*
		String[] sampleNames = m_Param.getSampleNames();
		String[] globalStatFilenames = m_Param.getGlobalStatsFileNames();
		assert(globalStatFilenames.length == m_GlobalStats.length);
		assert(sampleNames.length == m_GlobalStats.length);
		for (int i = 0; i < globalStatFilenames.length; i++)
		{
			m_GlobalStats[i].writeStats(globalStatFilenames[i], sampleNames[i]);
		}
		
		String[] regionalStatFilenames = m_Param.getRegionalStatsFileNames();
		assert(regionalStatFilenames.length == m_RegionalStats.length);
		assert(sampleNames.length == m_RegionalStats.length);
		for (int i = 0; i < regionalStatFilenames.length; i++)
		{
			m_RegionalStats[i].writeStats(regionalStatFilenames[i], sampleNames[i]);
		}*/
	}
	
	public void writeOutput() {
		/*// HY: all the writing is now done in progress inside handlePrimaryData()
		String message = PROGRESS_PREFIX + "Writing output";
		firePropertyChange(message, m_Progress, 0);
		m_Progress = 0;
		writeDataTables();
		
		firePropertyChange(message, m_Progress, 0);
		m_Progress = 30;
		//HY writeClusters();
		
		firePropertyChange(message, m_Progress, 50);
		m_Progress = 60;
		writeStats();
		
		firePropertyChange(message, m_Progress, 100);
		m_Progress = 100;
		*/
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
    
    /**
     * SwingWorker class to read region file
     */
    public class WorkerReadRegions extends SwingWorker<Void, Void>
    {
    	String m_Path;
        public WorkerReadRegions(String path)
        {
        	m_Path = path;
        }
        
        @Override
        public Void doInBackground() throws IOException, InterruptedException
        {
        	firePropertyChange("activity:Processing Region File", 0, 1);
        	//m_DataReader.enablePrintProgress(true);
			m_DataReader.readGFF(m_Path);
            return null;
        }
        
        @Override
        public void done() 
        {//Executed in event dispatching thread
            Toolkit.getDefaultToolkit().beep();
        }
    }
    
    WorkerReadRegions getWorkerReadRegions(String path)
    {
    	return new WorkerReadRegions(path);
    }
    
    public class WorkerLoadData extends SwingWorker<Void, Void>
    {
    	File m_AnalysisDir; //input
    	WorkerLoadData(File aDir)
    	{
    		m_AnalysisDir = aDir;
    	}
    	
        @Override
        public Void doInBackground() // throws IOException, IllegalArgumentException, InterruptedException 
        {
        	try
        	{
				load(m_AnalysisDir);
				//writeOutput();
				computeDataTable();
        	}
        	catch (InterruptedException e)
        	{
	    		firePropertyChange("cancel", m_Progress, 0);
        	}
        	catch (Exception e)
        	{
        		String errorString = "error" + e.toString() + "\n";
        		StackTraceElement[] stackTrace = e.getStackTrace();
        		for (int i = 0; i < stackTrace.length; i++)
        			errorString += "\t" + stackTrace[i].toString() + "\n";
	    		firePropertyChange(errorString, 1, 0);
	    		cancel(true);
        	}
			return null;
        }
    }
    
    WorkerLoadData getWorkerLoadData(File aDir)
    {
    	return new WorkerLoadData(aDir);
    }
    
    
    public class WorkerProcessData extends SwingWorker<Void, Void>
    {
        @Override
        public Void doInBackground()// throws IOException, IllegalArgumentException, InterruptedException 
        {
        	try
        	{
				m_Param.prepareNewAnalysis();
				loadData(m_Param);
				//writeOutput();
				computeDataTable();
        	}
        	catch (InterruptedException e)
        	{
	    		firePropertyChange("cancel", m_Progress, 0);
        	}
        	catch (Exception e)
        	{
        		String errorString = "error" + e.toString() + "\n";
        		StackTraceElement[] stackTrace = e.getStackTrace();
        		for (int i = 0; i < stackTrace.length; i++)
        			errorString += "\t" + stackTrace[i].toString() + "\n";
	    		firePropertyChange(errorString, 1, 0);
	    		cancel(true);
        	}
        	return null;
        }
        
        @Override
        public void done()
        {
        }
    }
    
    WorkerProcessData getWorkerProcessData()
    {
    	return new WorkerProcessData();
    }
    
    
    /**
     * SwingWorker class to read experiments file
     */
    /*
    public class WorkerReadExperiments extends SwingWorker<Void, Void>
    {
        @Override
        public Void doInBackground() {
			int iNumBins = (Integer) m_SpinnerBins.getValue();
			int index = 0;
        	for (Experiment xp: m_InputXP)
        	{
    			try {
    	        	index++;
    	        	firePropertyChange("activity:Processing ("+index+" of "+ m_InputXP.size() + ") Files ...", index - 1, index);
					m_DataReader.readWIG(xp.m_Filename, iNumBins, true, false);
					
					if (xp.m_Filename.endsWith(".bigWig") || xp.m_Filename.endsWith(".bw")) {
						// process big wig
						m_DataReader.readBigWig(xp.m_Filename, iNumBins, true, false);
					} else if (xp.m_Filename.endsWith(".wig") || xp.m_Filename.endsWith(".wig.gz") || xp.m_Filename.endsWith(".wig.zip")) {
						// process wig
						m_DataReader.readWIG(xp.m_Filename, iNumBins, true, false);
					} else {
						throw new IOException("ERROR: Unexpected file format in " + xp.m_Filename);
					}

    			} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
        	}
			return null;
        }
        
        @Override
        public void done() 
        {//Executed in event dispatching thread
            Toolkit.getDefaultToolkit().beep();
        }
    }    */
}
