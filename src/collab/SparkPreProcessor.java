/**
 * Wraps BioReader functionality to be used by the Spark GUI:
 * 	- Uses the file input and normalization from BioReader
 * 	- Implements K-Means
 * 	- Writes output files as required by Spark GUI  
 */

package collab;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;

import javax.swing.SwingWorker;

import org.apache.log4j.Appender;
import org.apache.log4j.Logger;

import cluster.ClusterSet;

import parameters.Parameters;
import util.MessageUtils;

import dataLoader.DataModel;

public class SparkPreProcessor extends SwingWorker<Void, Void>
{
	private static Logger log = Logger.getLogger(SparkPreProcessor.class);
	
	public static int MAX_ITERATIONS = 100;  // maximum number of times to iterate k-means
	
	protected Parameters m_Param;
	protected DataModel m_DataModel;
    
    protected String m_RunType = "preprocessing"; // or "clustering"
    protected int m_CurrentJobLength;
    
	public static String PROGRESS_PREFIX = "Progress:";
	public static String ERROR_PREFIX = "Error:";
	
	/**
	 * Useful for adding a console appender when launched in commandline mode, etc.
	 * @param a
	 */
	public void addAppender(Appender a) {
		log.addAppender(a);
	}
	
	public void loadData() throws IOException, IllegalArgumentException {
		log.debug("Loading data");
		m_DataModel = new DataModel();
		m_DataModel.addPropertyChangeListener(new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				String name = evt.getPropertyName();
				if (name.startsWith(PROGRESS_PREFIX)) {
						int progress = (Integer) evt.getNewValue();
						firePropertyChange(name, getProgress(), progress);
				}
			}
		});
		m_DataModel.loadData(m_Param);
	}
	
	public DataModel getDataModel() {
		return m_DataModel;
	}
	
	public Parameters getParam() 
	{
		return m_DataModel.getParam();
	}
    
    /**
     * Performs a K-means on the m_Table data. Should be called only after readFiles() has populated m_Table 
     * output 	populated m_ClusterSet
     * @param K		number of clusters
     */
	public void kmeans(int K) 
	{
		Parameters param = m_DataModel.getParam();
		int rSeed = param.getRandomSeed(); // number to pass to random number generator
		ClusterSet clusterSet = new ClusterSet(m_DataModel.getDataTable(), K);
		kmeans(clusterSet, K, rSeed);
		m_DataModel.setClusterSet(clusterSet);
    	updateProgress();
	}
	
    public static void kmeans(ClusterSet clusterSet, int K, int rSeed)
	{
    	// System.out.println("[Started] K-Means for K=" + K);
    	
	    int iRows = clusterSet.getNumFeatures();
		int iCols = clusterSet.getNumSamples() * clusterSet.getNumBins();
		int[] clusterIds = new int[iRows];
		Arrays.fill(clusterIds, -1);
		double[][] clusterCentroids = new double[K][iCols];
		int[] clusterSizes = new int[K];
		
		// initialize the centroids randomly
		Random rnd = new Random(rSeed);
		for (int c = 0; c < K; c++)
		{
			int randIndex = rnd.nextInt(iRows);
			System.arraycopy(clusterSet.getFeatureValues(randIndex), 0, clusterCentroids[c], 0, iCols);
		}
		
	    int iIterations = 0;
		boolean bClustersChanged = true; // whether the clusters changed since last iteration
		
	    while (bClustersChanged && iIterations++ < MAX_ITERATIONS)
	    {
	    	bClustersChanged = false;
	    	for (int idata = 0; idata < iRows; idata++)
	    	{
	    		double minDist = Double.MAX_VALUE; // minimum distance from the point to cluster centroids
	    		int iClusterId = -1; // the cluster to which the point belongs
	    		double[] fValues = clusterSet.getFeatureValues(idata);
	    		
				// calculate the distance from the centroids
	    		for (int c = 0; c < K; c++)
	    		{
	    			double dist = 0; // the sum square distance
	    			for (int col = 0; col < iCols; col++)
	    			{
	    				double diff = fValues[col] - clusterCentroids[c][col];
	    				dist += diff * diff;
	    			}
					if (dist < minDist)
					{
						iClusterId = c;
						minDist = dist;
					}
	    		}
	    		
	    		if (clusterIds[idata] != iClusterId)
	    		{
	    			clusterIds[idata] = iClusterId;
	    			bClustersChanged = true;
	    		}
	    	}
	    	
	    	// calculate the new centroids
	    	if (bClustersChanged)
	    	{
	    		
	    		//reset
	    		Arrays.fill(clusterSizes, 0);
	    		for (int c = 0; c < K; c++)
	    		{
	    			Arrays.fill(clusterCentroids[c], 0);
	    		}
	    		
	    		// sum
	    		for( int idata = 0; idata < iRows; idata++ )
	    		{
	    			double[] fValues = clusterSet.getFeatureValues(idata);
	    			int iCluster = clusterIds[idata];
	    			clusterSizes[iCluster]++;
	    			for (int d = 0; d < iCols; d++)
		    		{
	    				clusterCentroids[iCluster][d] += fValues[d];
		    		}
	    		}
	    		
	    		// calculate new centroids
	    		for (int c = 0; c < K; c++)
	    		{
	    			if (clusterSizes[c] != 0)
	    			{
		    			for (int d = 0; d < iCols; d++)
			    		{
		    				clusterCentroids[c][d] /= clusterSizes[c];
			    		}
	    			}
	    			else
	    			{// change the cluster centroid
	    				System.out.format("WARNING: Cluster %d has 0 members. Picking a new random centroid.\n", c);
	    				int randIndex = rnd.nextInt(iRows);
	    				for (int d = 0; d < iCols; d++)
	    				{
	    	               System.arraycopy(clusterSet.getFeatureValues(randIndex), 0, clusterCentroids[c], 0, iCols);
	    				}
	    				bClustersChanged = true;
	    			}
	    		}
	    	}
	    }
	    boolean toSort = true;
	    clusterSet.setData(clusterCentroids, clusterSizes, clusterIds, toSort);
    	// System.out.println("[Done] K-Means");

	}
    
    public void setParam(String analysisDir) throws IOException {
    	m_Param = new Parameters();
    	log.debug("Loading properties from analysis dir '" + analysisDir + "'");
    	m_Param.load(new File(analysisDir));
    }
    
    public void setParam(Parameters param) {
    	m_Param = param;
    }
    
	@Override
	protected Void doInBackground() throws IOException {
		try {
			setProgress(0);
			loadData();
			kmeans(m_Param.getKvalue());
			writeOutput();
		} catch (IOException e) {
			firePropertyChange(ERROR_PREFIX, getProgress(), 100);
			MessageUtils.showMessage(e.getMessage());
		} catch (IllegalArgumentException e) {
			firePropertyChange(ERROR_PREFIX, getProgress(), 100);
			MessageUtils.showMessage(e.getMessage());
		}
		return null;
	}
	
	protected void updateProgress() {
		int prevProgress = getProgress();
		setProgress(prevProgress + m_CurrentJobLength);
	}
	
	public void writeOutput() {
		m_DataModel.writeOutput();
	}
	
	public static void main(final String[] args) {
		// preprocess specified analysis dir
		if (args.length == 1) {
			String aDir = args[0];
			SparkPreProcessor spp = new SparkPreProcessor();
			try {
				spp.setParam(aDir); 
				spp.loadData();
				spp.writeOutput();
			} catch (IOException e) {
				MessageUtils.showMessage(e.getMessage());
			} catch (IllegalArgumentException e) {
				MessageUtils.showMessage(e.getMessage());
			}
		}
    }
    
}
