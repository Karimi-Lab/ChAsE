/**
 * Imported from Spark. May 29, 2012
 */

package chase.input;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.swing.tree.DefaultMutableTreeNode;

import org.apache.log4j.Logger;

import chase.gui.ColorPalette;
import chase.input.xml.ResourceData;

import chase.input.DataLoader;


/**
 * Class to store global parameters.
 * 
 * @author Cydney Nielsen
 *
 */
public class Parameters {
	
	private static Logger log = Logger.getLogger(Parameters.class);
	
	// Directory for storing downloaded files
	protected static File CACHE_DIR = new File(System.getProperty("user.home"), "ChAsE_Cache");
	
	protected static int MIN_SAMPLE_NAME_LEN = 11;
	protected static int MIN_REGION_NAME_LEN = 10;
	protected static int MAX_RECOMMENDED_FILE_COUNT = 100;
	
	// normalization types
	public static final String NONE = "none";
	public static final String LINEAR = "linear";
	public static final String GAUSSIAN = "exp";
	
	// stats types used for normalization
	public static final String GLOBAL = "global";
	public static final String REGIONAL = "regional"; 
	
	// binning types
	public static final String DIVIDE_BY_SIZE = "bysize";
	public static final String DIVIDE_BY_COUNT = "bycount";
	
	protected static int DEFAULT_BIN_COUNT = 20;
	protected static int DEFAULT_K = 3;
	public static String DEFAULT_STATS_TYPE = GLOBAL;
	public static String DEFAULT_NORM_TYPE = GAUSSIAN;
	public static String DEFAULT_BINNING_TYPE = DIVIDE_BY_SIZE;
	protected static int DEFAULT_RANDOM_SEED = 15;
	
	// moved to ColorPalette.java
	//protected static List<String> COL_NAMES; // color names
	
	/**
	 * Analysis directory path where output files are created
	 */
	protected File fAnalysisDir;
	
	/**
	 * Directory where cluster files are generated
	 * (a sub-directory of analysis dir)
	 */
	protected File fClusterDir;
	
	/**
	 * Auto-generated statistics files 
	 * (operating system independent)
	 */
	protected String[] fGlobalStatsFileNames;
	
	/**
	 * Index of next available track color
	 */
	protected int fNextColIndex;
	
	/**
	 * Auto-generated processed data dir
	 * (operating system independent)
	 */
	protected File fProcessedDataDir;
	
	/**
	 * Auto-generated processed (binned and normalized) data file
	 * (operating system independent)
	 */
	protected String[] fProcessedDataFileNames;
	
	/**
	 * Stores the number of reads per bin
	 */
	protected String[] fReadCountFileNames;
	
	/**
	 * Parameters stored as a Properties object
	 * Includes the following:
	 *   - cache (directory where to download cached files)
	 *   - colLabels (e.g. blue,blue - one per data file)
	 *   - dataFiles
	 *   - k (e.g. 5)
	 *   - normType (one of 'exp' or 'none')
	 *   - numBins (e.g. 20)
	 *   - org (e.g. hg19 (optional))
	 *   - randomSeed (e.g. 15 (hidden from user))
	 *   - regionsFile (can be a URL)
	 *   - regionsLabel (e.g. TSS (optional))
	 *   - sampleNames (e.g. "H3K4me3,H3K9me3" (extracted from data file by default))
	 *   - statsFiles (can provide URLs to precomputed .stats files)
	 *   - statsType (one of 'global' or 'regional')
	 *   - tables (can provide URLs to precomputed .dat files)
	 */
	protected Properties fProps;
	
	/**
	 * Auto-generated statistics files 
	 * (operating system independent)
	 */
	protected String[] fRegionalStatsFileNames;
	
	/**
	 * Epigenome Atlas and ENCODE resources (URLs, etc.)
	 */
	protected ResourceData fResourceData;
	
	protected File fStatsDir;

	/**
	 * Unique colLabels (useful for legends)
	 */
	protected List<String> fUniqueColLabels;
	
	/**
	 * Generate a Parameters object
	 * 
	 * @param props
	 * @param analysisDir
	 */
	public Parameters() {
//		setConstants();
		fProps = new Properties();
		setDefaults(fProps);
	}
	
	public Object clone() {
		Parameters nParam = new Parameters();
		nParam.setProperties((Properties)fProps.clone());
		
		nParam.setAnalysisDir(new File(fAnalysisDir.getAbsolutePath()));
		nParam.setClusterDir(new File(fClusterDir.getAbsolutePath()));
		
		String[] nGlobalStatsFileNames = new String[fGlobalStatsFileNames.length];
		System.arraycopy(fGlobalStatsFileNames, 0, nGlobalStatsFileNames, 0, fGlobalStatsFileNames.length);
		nParam.setGlobalStatsFileNames(nGlobalStatsFileNames);
		
		nParam.setProcessedDataDir(new File(fProcessedDataDir.getAbsolutePath()));
		
		String[] nProcessedDataFileNames = new String[fProcessedDataFileNames.length];
		System.arraycopy(fProcessedDataFileNames, 0, nProcessedDataFileNames, 0, fProcessedDataFileNames.length);
		nParam.setProcessedDataFileNames(nProcessedDataFileNames);
		
		String[] nReadCountFileNames = new String[fReadCountFileNames.length];
		System.arraycopy(fReadCountFileNames, 0, nReadCountFileNames, 0, fReadCountFileNames.length);
		nParam.setReadCountFileNames(nReadCountFileNames);
		
		
		String[] nRegionalStatsFileNames = new String[fRegionalStatsFileNames.length];
		System.arraycopy(fRegionalStatsFileNames, 0, nRegionalStatsFileNames, 0, fRegionalStatsFileNames.length);
		nParam.setRegionalStatsFileNames(nRegionalStatsFileNames);
		
		nParam.setStatsDir(new File(fStatsDir.getAbsolutePath()));
		
		List<String> nUniqColLabels = new ArrayList<String>(fUniqueColLabels.size());
		for (String c: fUniqueColLabels) {
			nUniqColLabels.add(c);
		}
		nParam.setUniqColLabels(nUniqColLabels);
		
		return nParam;
	}
	
	/**
	 * Determines whether there is a pre-computed 
	 * binary data file for any of the data files
	 * (depends on regions file and number of bins)
	 */
	protected void checkAtlasArchive() {
		// this check protects against a local file by the same name
		if (getRegionsFileName().startsWith(getRegionsUrlRoot())) {
			String[] fns = fProps.getProperty("dataFiles").split(",");
			for (int i=0; i<fns.length; i++) {
				String dataFileName = fns[i];
				if (dataFileName.startsWith(getAtlasUrlRoot())) {
					// ensure using default bin count - currently only one being preprocessed
					if (getNumBins() == DEFAULT_BIN_COUNT) {
						// construct expected processed data and stats file URLs
						String urlRoot = fResourceData.getProcessedUrlRoot();
						String path = dataFileName.split(getAtlasUrlRoot())[1];
						int slashindex = path.lastIndexOf("/");
						path = path.substring(0, slashindex+1);
						String pDataFileName = new File(fProcessedDataFileNames[i]).getName();
						String pDataFileUrl = urlRoot + path + pDataFileName;
						fProcessedDataFileNames[i] = pDataFileUrl;

						String pReadCountFileName = new File(fReadCountFileNames[i]).getName();
						fReadCountFileNames[i] = urlRoot + path + pReadCountFileName;
						
						String gStatsFileName = new File(fGlobalStatsFileNames[i]).getName();
						String gStatsFileUrl = urlRoot + path + gStatsFileName;
						fGlobalStatsFileNames[i] = gStatsFileUrl;
						String rStatsFileName = new File(fRegionalStatsFileNames[i]).getName();
						String rStatsFileUrl = urlRoot + path + rStatsFileName;
						fRegionalStatsFileNames[i] = rStatsFileUrl;
					}
				}
			}
		}
	}
	
	protected void checkDataFileNames(String fn) throws IllegalArgumentException {
		checkDataFileNames(fProps.getProperty("dataFiles").split(","));
	}
	
	protected void checkDataFileNames(String[] fns) throws IllegalArgumentException {
		// check that max number of data files is not exceeded
		if (fns.length > MAX_RECOMMENDED_FILE_COUNT) {
			log.error("Input data file count (" + fns.length + ") exceeds recommended max of " + MAX_RECOMMENDED_FILE_COUNT);
			throw new IllegalArgumentException("Your current selection of " + fns.length + 
					" input data files exceeds the recommended maximum of " +
					MAX_RECOMMENDED_FILE_COUNT + ". Please select a subset.");
		}
		// check that file extensions are acceptable
		for (String fn: fns) {
			DataLoader.checkDataFileExt(fn);
		}
	}
	
	protected void checkForURLs(String[] defaultNames, File defaultDir, String[] userProvidedNames) {
		assert(defaultNames.length == userProvidedNames.length);
		// check whether user provided names are URLS
		for (int i=0; i<userProvidedNames.length; i++) {
			try {
				URL url = new URL(userProvidedNames[i]);
				// check that name is the same as the auto-generated one
				int slashIndex = url.getPath().lastIndexOf('/');
				String providedName = url.getPath().substring(slashIndex + 1);
				String defaultName = new File(defaultNames[i]).getName();
				if (!providedName.equals(defaultName)) {
					System.out.println("ERROR: unexpected output data file name:\n" + 
							providedName + "\n" + defaultName + "\n");
				} else {
					// make the URL the default
					defaultNames[i] = userProvidedNames[i];
				}
			} catch (MalformedURLException e) {
			}
		}
	}
	
	/**
	 * Check that properties are valid values.
	 * 
	 * @throws IllegalArgumentException
	 */
	protected void checkProperties() throws IllegalArgumentException {
		// ensure have specified input files
		if (fProps.getProperty("dataFiles") == null) {
			String err = "No input data file names provided (property 'dataFiles')";
			log.error(err);
			throw new IllegalArgumentException(err);
		}
		
		// ensure data file names are ok
		checkDataFileNames(fProps.getProperty("dataFiles"));
		
		// ensure have specified regions file
		if (fProps.getProperty("regionsFile") == null) {
			String err = "No regions file name provided (property 'regionsFile'). Must be in GFF format.";
			log.error(err);
			throw new IllegalArgumentException(err);
		}
		
		// ensure regions file name is ok
		checkRegionsFileName(fProps.getProperty("regionsFile"));
		
		// ensure have same number of color labels as data files
		if (fProps.getProperty("colLabels") != null) {
			String[] cLabels = fProps.getProperty("colLabels").split(",");
			String[] dFiles = fProps.getProperty("dataFiles").split(",");
			if (cLabels.length != dFiles.length) {
				// if only a single label specified, use for all data files
				if (cLabels.length == 1 && dFiles.length > 1) {
					String colLabelString = "";
					for (int i=0; i<dFiles.length-1; i++) {
						colLabelString += cLabels[0] + ",";
					}
					colLabelString += cLabels[0];
					fProps.setProperty("colLabels", colLabelString);
				} else {
					log.error("Different number of dataFiles and colLabels: " + 
							fProps.getProperty("dataFiles").split(",").length + " " + cLabels.length);
					throw new IllegalArgumentException("Different number of dataFiles and colLabels:\n" + 
							fProps.getProperty("dataFiles").split(",").length + " " + cLabels.length);
				}
			}
			// ensure entries are in list of expected color labels
			for (String c: cLabels) {
				if (!ColorPalette.COLOR_LIST.contains(c)) {
					String err = "Unknown colour label: '" + c + "'";
					log.error(err);
					//HY:TODO
					//throw new IllegalArgumentException(err);
				}
			}
			// build set of unique labels - useful later for figure legend generation
			fUniqueColLabels = new ArrayList<String>();
			for (String c: cLabels) {
				if (!fUniqueColLabels.contains(c)) {
					fUniqueColLabels.add(c);
				}
			}
		}
	}
	
	public void checkRegionsFileName(String fn) {
		DataLoader.checkRegionsFileExt(fn);
	}
	
	/**
	 * Removes all previous files from the analysis subdirs
	 */
	public void cleanDir(File dir) {
		File[] children = dir.listFiles();
		if (children != null) {
			for (int i=0; i<children.length; i++) {
				if (children[i].isDirectory()) {
					// remove files in each subdirectory
					File[] toRemove = children[i].listFiles();
					for (File r: toRemove) {
						if (r.isFile()) {
							// System.out.println("deleting " + r.getAbsolutePath());
							r.delete();
						}
					}
				}
			}
		}
	}
	
	public String convertStreamToString(InputStream is) throws IOException {
		/*
		 * To convert the InputStream to String we use the
		 * Reader.read(char[] buffer) method. We iterate until the
		 * Reader return -1 which means there's no more data to
		 * read. We use the StringWriter class to produce the string.
		 */
		if (is != null) {
			Writer writer = new StringWriter();

			char[] buffer = new char[1024];
			try {
				Reader reader = new BufferedReader(
						new InputStreamReader(is, "UTF-8"));
				int n;
				while ((n = reader.read(buffer)) != -1) {
					writer.write(buffer, 0, n);
				}
			} finally {
				is.close();
			}
			return writer.toString();
		} else {       
			return "";
		}
	}
	
	/**
	 * Makes a copy of the current analysis directory 
	 * in the specified directory
	 * 
	 * @param newAnalysisDir
	 */
	public void copyAnalysisDir(File newAnalysisDir) throws IOException {
		copyAnalysisDir(newAnalysisDir, null);
	}
	
	/**
	 * Makes a copy of the current analysis directory
	 * in the specified directory skipping any of the 
	 * named sub-directories
	 * 
	 * @param newAnalysisDir
	 * @param toSkip
	 */
	public void copyAnalysisDir(File newAnalysisDir, String[] toSkip) throws IOException {
		if (!newAnalysisDir.isDirectory()) {
			newAnalysisDir.mkdir();
		}
		for (File aFile: getAnalysisDir().listFiles()) {
			boolean processThis = true;
			if (toSkip != null) { 
				for (String s: toSkip) {
					if (s.equals(aFile.getName())) {
						processThis = false;
					}
				}
			}
			if (!processThis) { continue; }
			File nFile = new File(newAnalysisDir, aFile.getName());
			if (aFile.isFile()) {
				copyFile(aFile, nFile);
			} else {
				if (!nFile.isDirectory()) {
					nFile.mkdir();
				}
				if (aFile.listFiles() != null) {
					for (File sFile: aFile.listFiles()) {
						File dFile = new File(nFile, sFile.getName());
						copyFile(sFile, dFile);
					}
				}
			}
		}
	}
	
	protected void copyFile(File sourceFile, File destFile) throws IOException {
		FileChannel source = null;
		FileChannel destination = null;
		try {
			source = new FileInputStream(sourceFile).getChannel();
			destination = new FileOutputStream(destFile).getChannel();
			destination.transferFrom(source, 0, source.size());
		}
		finally {
			if(source != null) {
				source.close();
			}
			if (destination != null) {
				destination.close();
			}
		}
	}
	
	public File getAnalysisDir() {
		return fAnalysisDir;
	}
	
	public String getAtlasReference() {
		return fResourceData.getAtlasReference();
	}
	
	public DefaultMutableTreeNode getAtlasTree(String type) {
		return fResourceData.getResourceTree(type);
	}
	
	public String getAtlasUrlRoot() {
		return fResourceData.getAtlasUrlRoot();
	}
	
	public static File getCacheDir() {
		return CACHE_DIR;
	}
	
	public File getClusterDir() {
		if (fClusterDir == null) {
			initializeOutputFiles("clusters", "clusters");
		}
		return fClusterDir;
	}

	public String getClusterMemberFileName() {
		if (fClusterDir == null) {
			getClusterDir();
		}
		return fClusterDir + "/" + fClusterDir.getName() + ".gff";
	}
	
	public String getClusterValuesFileName() {
		if (fClusterDir == null) {
			getClusterDir();
		}
		return fClusterDir + "/" + fClusterDir.getName() + ".values";
	}
	
	public String getWorkspaceFileName() {
		if (fClusterDir == null) {
			getClusterDir();
		}
		return fClusterDir + "/" + fClusterDir.getName() + ".json";
	}
	
	
	public String[] getColorLabels() {
		String[] labels = null;
		if (fProps.getProperty("colLabels") != null) {
			labels = fProps.getProperty("colLabels").split(",");
		}
		return labels;
	}
	
	public String getColorLabels(int i) {
		String label = null;
		if (fProps.getProperty("colLabels") != null) {
			String[] labels = fProps.getProperty("colLabels").split(",");
			if (labels.length > i) {
				label = labels[i];
			}
		}
		return label;
	}
	
	public String[] getDataFileNames() {
		String[] fNames = null;
		if (fProps.getProperty("dataFiles") != null) {
			fNames = fProps.getProperty("dataFiles").split(",");
		}
		return fNames;
	}

	public String getDataLabel() {
		return fProps.getProperty("dataLabel");
	}
	
	public int getDefaultK() {
		return DEFAULT_K;
	}
	
	public int getDefaultNumBins() {
		return DEFAULT_BIN_COUNT;
	}
	
	public String getDefaultStatsType() {
		return DEFAULT_STATS_TYPE;
	}
	
	public String getDefaultBinningType() {
		return DEFAULT_BINNING_TYPE;
	}

	public String getEncodeUrlRoot() {
		return fResourceData.getEncodeUrlRoot();
	}
	
	public String[] getGlobalStatsFileNames() {
		return fGlobalStatsFileNames;
	}
	
	public boolean getIncludeZerosState() {
		if (fProps.getProperty("includeZeros").equals("true")) {
			return true;
		} else {
			return false;
		}
	}
	
	public int getKvalue() throws IllegalArgumentException {
		if (fProps.getProperty("k") == null) {
			throw new IllegalArgumentException("No value of k found in properties file");
		}
		return Integer.parseInt(fProps.getProperty("k"));
	}
	
	public boolean[] getLogScales()
	{
		return getMultipleBoolean("logScale");
	}
	
	public int getMinRegionNameLen() {
		return MIN_REGION_NAME_LEN;
	}
	
	public int getMinSampleNameLen() {
		return MIN_SAMPLE_NAME_LEN;
	}
	
	public String[] getMultiple(String propName)
	{
		String prop = fProps.getProperty(propName);
		if (prop != null)
			return prop.split(",");
		return null;
	}
	
	public boolean[] getMultipleBoolean(String propName)
	{
		String props[] = getMultiple(propName);
		if (props != null)
		{
			boolean[] boolProps = new boolean[props.length];
			for (int i = 0; i < props.length; ++i)
				boolProps[i] = props[i].equalsIgnoreCase("true") ? true : false;
			return boolProps;
		}
		return null;
	}
	
//	public String getNormType() {
//		return fProps.getProperty("normType");
//	}

	public String[] getNormTypes() {
		return getMultiple("normType");
	}
	
	public String[] getBinningTypes() {
		return getMultiple("binningType");
	}

	public int getNumBins() {
		return Integer.parseInt(fProps.getProperty("numBins"));
	}
	
	public String getOrg() {
		if (fProps != null && fProps.containsKey("org")) {
			return (String) fProps.getProperty("org");
		} else {
			return null;
		}
	}
	
	public float getPadValue() {
		return Integer.parseInt(fProps.getProperty("padValue"));
	}
	
	public File getProcessedDataDir() {
		return fProcessedDataDir;
	}
	
	public String[] getProcessedDataFileNames() {
		return fProcessedDataFileNames;
	}
	
	public String[] getReadCountFileNames() {
		return fReadCountFileNames;
	}
	
	public Properties getProps() {
		return fProps;
	}
	
	public int getRandomSeed() {
		return Integer.parseInt(fProps.getProperty("randomSeed"));
	}
	
	public String[] getRegionalStatsFileNames() {
		return fRegionalStatsFileNames;
	}
	
	public String getRegionsFileName() {
		return fProps.getProperty("regionsFile");
	}
	
	public String getRegionsLabel() {
		return fProps.getProperty("regionsLabel");
	}
	
	public int getRegionsSize()
	{
		if (fProps.getProperty("regionsSize") != null)
			return Integer.parseInt(fProps.getProperty("regionsSize"));
		return -1;
	}
	
    public boolean getEqualRegionSize()
    {
        // default is true unless "false" is explicitly set
        if (fProps.getProperty("equalRegions") != null) {
            return (!fProps.getProperty("equalRegions").equals("false"));
        }
        return true;
    }

	public String getRegionsUrlRoot() {
		return fResourceData.getRegionsUrlRoot();
	}
	
	public String[] getSampleNames() {
		String[] sNames = null;
		if (fProps.getProperty("sampleNames") != null) {
			sNames = fProps.getProperty("sampleNames").split(",");
		}
		return sNames;
	}
	
	public File getStatsDir() {
		return fStatsDir;
	}
	
//	public String getStatsType() {
//		return fProps.getProperty("statsType");
//	}
	public String[] getStatsTypes() {
		return getMultiple("statsType");
	}
	
	public List<String> getUniqueColLabels() {
		return fUniqueColLabels;
	}
	
	public boolean[] getVisibles()
	{
		return getMultipleBoolean("visible");
	}
	
	protected void initializeAnalysisDir() {
		if (!fAnalysisDir.isDirectory()) {
			fAnalysisDir.mkdir();
		}
	}
	
	protected void initializeOutput() {
		initializeAnalysisDir();
		initializeOutputFiles("stats", "stats");
		initializeOutputFiles("tables", "dat");
	}

	protected void initializeOutputFiles(String dir, String fileExt) throws IllegalArgumentException {
		// create requested sub-directory
		File subDir = new File(fAnalysisDir, dir);
		if (!subDir.isDirectory()) { 
			subDir.mkdir(); 
		}
		String regionsFileName = new File(fProps.getProperty("regionsFile")).getName();
		regionsFileName = DataLoader.removeExt(regionsFileName);
		String[] dataFileNames = fProps.getProperty("dataFiles").split(",");
		// populate file names where necessary
		if (dir.equals("stats")) {
			// generate both global and regional default stats names
			fGlobalStatsFileNames = new String[dataFileNames.length];
			fRegionalStatsFileNames = new String[dataFileNames.length];
			fStatsDir = subDir;
			for (int i=0; i<dataFileNames.length; i++) {
				String defaultName = new File(dataFileNames[i]).getName();
				defaultName = DataLoader.removeExt(defaultName);
				defaultName += "__" + regionsFileName + "__b" + fProps.getProperty("numBins") + "__";// + fProps.getProperty("normType") + "__";
				String globalStatsName = defaultName + Parameters.GLOBAL + "." + fileExt;
				String regionStatsName = defaultName + Parameters.REGIONAL + "." + fileExt;
				fGlobalStatsFileNames[i] = new File(fStatsDir, globalStatsName).getAbsolutePath();
				fRegionalStatsFileNames[i] = new File(fStatsDir, regionStatsName).getAbsolutePath();
			}
			// user can provide URLs to their own stats files -- ensure base name is consistent with automated one
			if (fProps.getProperty("statsFiles") != null) {
				if (fProps.getProperty("statsType").equals("global")) {
					checkForURLs(fGlobalStatsFileNames, fStatsDir, fProps.getProperty("statsFiles").split(","));
				} else if (fProps.getProperty("statsType").equals("regional")) {
					checkForURLs(fRegionalStatsFileNames, fStatsDir, fProps.getProperty("statsFiles").split(","));
				}
			}
		} else if (dir.equals("tables")) {
			// generate default names
			fProcessedDataFileNames = new String[dataFileNames.length];
			fReadCountFileNames = new String[dataFileNames.length];
			fProcessedDataDir = subDir;
			for (int i=0; i<dataFileNames.length; i++) {
				String defaultName = new File(dataFileNames[i]).getName();
				defaultName = DataLoader.removeExt(defaultName);
				defaultName += "__" + regionsFileName;
				defaultName += "__b" + fProps.getProperty("numBins");
				fProcessedDataFileNames[i] = new File(fProcessedDataDir, defaultName + "." + fileExt).getAbsolutePath();
				fReadCountFileNames[i] = new File(fProcessedDataDir, defaultName + "." + "count").getAbsolutePath();
			}
			if (fProps.getProperty("tables") != null) {
				checkForURLs(fProcessedDataFileNames, fProcessedDataDir, fProps.getProperty("tables").split(","));
			}
		} else if (dir.equals("clusters")) {
			fClusterDir = subDir;
		} else {
			String err = "Unexpected argument: '" + dir + "'";
			log.error(err);
			throw new IllegalArgumentException(err);
		}
	}
	
	public void load(File aDir) throws IOException {
		// find the properties.txt file
		setAnalysisDir(aDir);
		File propFileName = new File(aDir, "properties.txt");
		
		// load values from the properties file
		fProps = new Properties();
		FileInputStream in = new FileInputStream(propFileName);
		fProps.load(in);	
		in.close();
		setDefaults(fProps); // for fields not set in properties.txt
		
		// some sanity checks
		checkProperties();
		
		initializeOutput();
	}
	
	public DefaultMutableTreeNode lookupNode(String s) {
		return fResourceData.lookupNode(s);
	}
	
	public void prepareNewAnalysis() throws IOException, IllegalArgumentException {
		checkProperties();
		initializeOutput();
		if (fResourceData != null) {
			checkAtlasArchive();
		}
		writeToFile(); 
	}

	public void reinitializeDirectories() {
		initializeOutput();
		// cluster currently only generated on demand, so not included in initializeOutput() method
		initializeOutputFiles("clusters", "clusters");
	}
	
	public void setAnalysisDir(File analysisDir) {
		fAnalysisDir = analysisDir;
	}
	
	public void setClusterDir(File clusterDir) {
		fClusterDir = clusterDir;
	}
	
	/**
	 * Set global constants
	 */
//	protected void setConstants() {
//	}
	
	public void setColorLabels(String[] cLabels) {
		// store as a string in properties object
		String cString = "";
		int i=0;
		for (i=0; i<cLabels.length-1; i++) {
			cString += cLabels[i] + ",";
		}
		cString += cLabels[i];
		fProps.setProperty("colLabels", cString);
	}
	
	public void setDataFileNames(String[] fns) throws IllegalArgumentException {
		checkDataFileNames(fns);
		// store as a string in properties object
		String fnString = "";
		int i=0;
		for (i=0; i<fns.length-1; i++) {
			fnString += fns[i] + ",";
		}
		fnString += fns[i];
		fProps.setProperty("dataFiles", fnString);
	}
	
	protected void setDefaults(Properties p) {
		if (!p.containsKey("k")) {
			p.setProperty("k", Integer.toString(DEFAULT_K));
		}
		if (!p.containsKey("normType")) {
			p.setProperty("normType", DEFAULT_NORM_TYPE);
		}
		if (!p.containsKey("numBins")) {
			p.setProperty("numBins", Integer.toString(DEFAULT_BIN_COUNT));
		}
		if (!p.containsKey("org")) {
			p.setProperty("org", "");          // none by default
		}
		if (!p.containsKey("randomSeed")) {
			p.setProperty("randomSeed", Integer.toString(DEFAULT_RANDOM_SEED));
		}
		if (!p.containsKey("regionsLabel")) {
			p.setProperty("regionsLabel", ""); // none by default
		}
		if (!p.containsKey("statsType")) {
			p.setProperty("statsType", DEFAULT_STATS_TYPE);
		}
		if (!p.containsKey("binningType")) {
			p.setProperty("binningType", DEFAULT_BINNING_TYPE);
		}
		if (!p.containsKey("includeZeros")) {
			p.setProperty("includeZeros", "true");
		}
	}
	
	public void setGlobalStatsFileNames(String[] fNames) {
		fGlobalStatsFileNames = fNames;
	}
	
	public void setKvalue(int k) {
		fProps.setProperty("k", Integer.toString(k));
	}
	
	public void setLogScales(boolean[] values)
	{
		String props = "";
		for (int i = 0; i < values.length; ++i)
			props = props + (i > 0 ? "," : "") + (values[i] ? "true" : "false");
		fProps.setProperty("logScale", props);
	}
	
//	public void setNormType(String s) {
//		if (!s.equals(NONE) && !s.equals(LINEAR) && !s.equals(GAUSSIAN)) {
//			System.out.println("ERROR: Unknown normalization type: " + s);
//			return;
//		}
//		fProps.setProperty("normType", s);
//	}
	
	public void setNormTypes(String[] values) {
		String props = "";
		for (int i = 0; i < values.length; i++)
		{
			if (i > 0)
				props += ",";
			if (!values[i].equals(NONE) && !values[i].equals(LINEAR) && !values[i].equals(GAUSSIAN)) {
				System.out.println("ERROR: Unknown normalization type: " + values[i]);
				props += DEFAULT_NORM_TYPE;
				continue;
			}
			props += values[i];
		}
		fProps.setProperty("normType", props);
	}
	
	public void setNumBins(int n) {
		fProps.setProperty("numBins", Integer.toString(n));
	}
	
	public void setOrg(String s) {
		fProps.setProperty("org", s);
	}
	
	public void setProcessedDataDir(File pDataDir) {
		fProcessedDataDir = pDataDir;
	}
	
	public void setProcessedDataFileNames(String[] fNames) {
		fProcessedDataFileNames = fNames;
	}
	
	public void setReadCountFileNames(String[] fNames) {
		fReadCountFileNames = fNames;
	}
	
	public void setProperties(Properties p) {
		fProps = p;
	}

	public void setRegionalStatsFileNames(String[] fNames) {
		fRegionalStatsFileNames = fNames;
	}
	
	public void setRegionsFileName(String s) {
		checkRegionsFileName(s);
		fProps.setProperty("regionsFile", s);
	}
	
	public void setRegionsLabel(String s) {
		fProps.setProperty("regionsLabel", s);
	}
	
    public void setEqualRegionSize(boolean bEqual) {
        fProps.setProperty("equalRegions", bEqual ? "true" : "false");
    }

	public void setRegionsSize(int size) {
		fProps.setProperty("regionsSize", Integer.toString(size));
	}
	
	public void setResourceData(ResourceData a) {
		fResourceData = a;
	}

	public void setSampleNames(String[] sNames) {
		// store as a string in properties object
		String sString = "";
		int i=0;
		for (i=0; i<sNames.length-1; i++) {
			sString += sNames[i] + ",";
		}
		sString += sNames[i];
		fProps.setProperty("sampleNames", sString);
	}
	
	public void setStatsDir(File s) {
		fStatsDir = s;
	}
	
//	public void setStatsType(String s) {
//		if (!s.equals(GLOBAL) && !s.equals(REGIONAL)) {
//			System.out.println("ERROR: Unknow stats type: " + s);
//			return;
//		}
//		fProps.setProperty("statsType", s);
//	}
	
	public void setStatsTypes(String[] values) {
		String props = "";
		for (int i = 0; i < values.length; ++i)
		{
			if (i > 0)
				props += ",";
			if (!values[i].equals(GLOBAL) && !values[i].equals(REGIONAL)) {
				System.out.println("ERROR: Unknow stats type: " + values[i]);
				props += DEFAULT_STATS_TYPE;
				continue;
			}
			props += values[i];
		}
		fProps.setProperty("statsType", props);
	}
	
	public void setBinningTypes(String[] values) {
		String props = "";
		for (int i = 0; i < values.length; ++i)
		{
			if (i > 0)
				props += ",";
			if (!values[i].equals(DIVIDE_BY_SIZE) && !values[i].equals(DIVIDE_BY_COUNT)) {
				System.out.println("ERROR: Unknow stats type: " + values[i]);
				props += DEFAULT_BINNING_TYPE;
				continue;
			}
			props += values[i];
		}
		fProps.setProperty("binningType", props);
	}
	
	public void setUniqColLabels(List<String> l) {
		fUniqueColLabels = l;
	}
	
	public void setVisibles(boolean[] values)
	{
		String props = "";
		for (int i = 0; i < values.length; ++i)
			props = props + (i > 0 ? "," : "") + (values[i] ? "true" : "false");
		fProps.setProperty("visible", props);
	}
	
	/**
	 * Writes the current Properties to a 'properties.txt' file
	 * 
	 * @throws IOException
	 */
	public void writeToFile() throws IOException {
		File propFileName = new File(getAnalysisDir(), "properties.txt");
		FileOutputStream out = new FileOutputStream(propFileName);
		fProps.store(out, "");
	}
	
}
