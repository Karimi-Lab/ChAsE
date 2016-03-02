package org.sfu.chase.core;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Map;
import java.util.Random;

import javax.swing.JFileChooser;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.sfu.chase.collab.BioReader;
import org.sfu.chase.collab.Feature;
import org.sfu.chase.input.DataModel;
import org.sfu.chase.util.Utils;

import still.data.MemoryTable;
import still.data.Table;
import still.data.TableFactory;

public class ClustFramework
{
    private Table       m_Table;
    private Table       m_UnNormTable;
    private GroupInfo[] m_Groups; 
    private int[]		m_GroupOrder;
    private ClustInfo   m_RootClustInfo;
    
    private String[]	m_RegionNames;
    private int			m_MaxRegionSize = 0;
    private boolean     m_bEqualRegionSize = true;
    private Feature[]   m_Features;
    
    private FavoriteList	m_Favorites; // the root node is always the first one
    private FavoriteList	m_WorkingSet; // the root node is always the first one
    
    private double[][]	m_PeakValues; // peak value per data per experiment. size: [iNumGroups][iDataSize];
    private double[][]	m_PeakOffsets; // peak offset per data per experiment. size: [iNumGroups][iDataSize];

    private ChangeListener  m_ChangeListener;
    
    private FrameworkHistory m_History;
    
    private DataModel		m_DataModel;
    
    public ClustFramework() {
    	m_History = new FrameworkHistory(this);
	}
    
    public String getRegionName(int index)
    {
    	if (m_RegionNames != null && index < m_RegionNames.length)
    	{
			String name = m_Features[index].getSeqName() + " " + m_Features[index].getStart() + " .. "+ m_Features[index].getEnd()
						+ " ; " + m_Features[index].getGroup();
    		return name;
    	}
    	return "";
    }
    
    public int getMaxRegionSize()
    {
    	return m_MaxRegionSize; 
    }
    
    public boolean isEqualRegionSize()
    {
        return m_bEqualRegionSize;
    }
    
    public ClustInfo getRoot()
    {
    	return m_RootClustInfo;
    }
    
    public FrameworkHistory getHistory()
    {
    	return m_History;
    }
    
    protected void setRoot(ClustInfo rootInfo)
    {
    	m_RootClustInfo = rootInfo;
    }
    
    public void setTable(Table table, int iNumGroups)
    {
    	m_Table = table;
    	
    	createUniformGroups(iNumGroups); 

    	//m_RootClustInfo = createFakeClusters(3, 3, Utils.intSequence(0, m_Table.rows() - 1, 1));
    	m_RootClustInfo = new ClustInfo();
    	m_RootClustInfo.m_Indices = Utils.intSequence(0, m_Table.rows() - 1, 1);
    	
    	calcClustStats(m_RootClustInfo);
    	
    	m_Favorites = new FavoriteList(this);
    	m_Favorites.add(m_RootClustInfo, false);
    	
    	m_WorkingSet = new FavoriteList(this);
    	m_WorkingSet.add(m_RootClustInfo, false);
    	
    	getHistory().resetHistory();
    	calcPeaks();
    }
    
    void updateAllClustStats()
    {
		for (int i = 0; i < m_WorkingSet.size(); i++) {
			calcClustStats(m_WorkingSet.get(i).getClustInfo());
		}
		for (int i = 1; i < m_Favorites.size(); i++) { // no need to recompute the master cluster again
			calcClustStats(m_Favorites.get(i).getClustInfo());
		}
    }
    
    public void setTable(DataModel dataModel, boolean resetClusters)
    {
    	m_DataModel = dataModel;
    	Table table = new MemoryTable(dataModel.getDataTable().getData(), null);
    	m_UnNormTable = new MemoryTable(dataModel.getUnNormDataTable().getData(), null);
    	
    	int numGroups = dataModel.getDataTable().getNumSamples();
    	if (resetClusters) {
    		setTable(table, numGroups);
    	}
    	else {
    		m_Table = table;
    		createUniformGroups(numGroups);
    		updateAllClustStats();
    		calcPeaks();
    	}
    	
    	readRegions(dataModel);
    	for (int i = 0; i < getNumGroups(); i++)
    	{
    		getGroup(i, false).m_Name = dataModel.getDataTable().getSampleName(i);
    		getGroup(i, false).m_Experiment = dataModel.getVisibleExperiment(i);
    	}
    }
    
    public DataModel getDataModel()
    {
    	return m_DataModel;
    }
    
    public double getData(int row, int col)
    {
    	int group = col / getGroupDim();
    	double value = m_Table.getMeasurement(row, col);// / m_Groups[group].m_CutOffMax;
    	//return value > 1 ? 1 : value;
    	return value > m_Groups[group].m_CutOffMax ? m_Groups[group].m_CutOffMax : value;
    }
    
    public double getData(int row, int col, int group)
    {
    	double value = m_Table.getMeasurement(row, group * getGroupDim() + col);// / m_Groups[group].m_CutOffMax;
    	//return value > 1 ? 1 : value;
    	return value > m_Groups[group].m_CutOffMax ? m_Groups[group].m_CutOffMax : value;
    }
    
    public int getDataSize()
    {
    	return m_Table.rows();
    }
    
    public int getDataDim()
    {
    	return m_Table.columns();
    }
    
    public int getGroupDim()
    {
    	return  m_Groups[0].m_iCols.length; 
    }
    
    public int getGroupOrder(int unorderedIndex, boolean bIncludeInvisible)
    {
    	if (bIncludeInvisible)
    		return m_GroupOrder[unorderedIndex];
    	
    	int index = 0;
    	for (int i = 0; i < m_GroupOrder.length; i++)
    	{
    		if (m_Groups[m_GroupOrder[i]].m_Visible)
    		{
        		if (index == unorderedIndex)
        			return m_GroupOrder[i];
    			index++;
    		}
    	}
    	return -1;
    }
    
    public int getNumGroups()
    {
    	if (m_Groups != null)
    		return m_Groups.length;
    	return 0;
    }
    
    public int getNumVisibleGroups()
    {
    	int numVisible = 0;
    	if (m_Groups != null)
    		for (int gi = 0; gi < m_Groups.length; gi++)
    			numVisible += m_Groups[gi].m_Visible ? 1 : 0;
    	return numVisible;
    }
    
    public int[] getVisibleColumns(boolean bIncludeInvisible, boolean ordered)
    {
    	int numVisibleColumns = 0;
		int[] visibleCols = null;
    	if (m_Groups != null)
    	{
    		for (int gi = 0; gi < m_Groups.length; gi++)
    			numVisibleColumns += (m_Groups[gi].m_Visible || bIncludeInvisible) ? m_Groups[gi].m_iCols.length : 0;
    		
    		visibleCols = new int[numVisibleColumns];
    		int vcol = 0;
    		for (int gi = 0; gi < m_Groups.length; gi++)
    		{
    			int igroup = ordered ? m_GroupOrder[gi] : gi;
    			if (m_Groups[igroup].m_Visible || bIncludeInvisible)
    			{
    				for (int col = 0; col < m_Groups[igroup].m_iCols.length; col++)
    					visibleCols[vcol++] = m_Groups[igroup].m_iCols[col];
    			}
    		}
    	}
    	return visibleCols;
    }
    
    public GroupInfo getGroup(int index, boolean ordered)
    {
    	if (m_Groups != null && index >= 0 && index < m_Groups.length)
    		return ordered ? m_Groups[m_GroupOrder[index]] : m_Groups[index];
    	return null;
    }
    
    public void swapGroups(int g1, int g2)
    {
    	if (g1 >= 0 && g1 < m_Groups.length && g2 >= 0 && g2 < m_Groups.length)
    	{
    		int tmp = m_GroupOrder[g1];
    		m_GroupOrder[g1] = m_GroupOrder[g2];
    		m_GroupOrder[g2] = tmp;
    	}
    }
    
    public void setGroupCuttOff(int groupIndex, double cuttoffVal)
    {
		getGroup(groupIndex, true).m_CutOffMax = cuttoffVal;
		calcPeaks();
		calcClustStats(m_RootClustInfo);
		for (int i = 1; i < m_Favorites.size(); i++) {
			calcClustStats(m_Favorites.get(i).getClustInfo());
		}
    }
    
    void calcPeaks()
    {
    	int iDataSize  = getDataSize();
    	int iDataDim   = getDataDim();
    	int iNumGroups = getNumGroups();
    	int iGroupDim  = getGroupDim();

    	m_PeakValues  = new double[iNumGroups][iDataSize];
    	m_PeakOffsets = new double[iNumGroups][iDataSize];
    	
    	for (int g = 0; g < iNumGroups; ++g)
    	{
    		Arrays.fill(m_PeakValues[g], Double.NEGATIVE_INFINITY);
    		Arrays.fill(m_PeakOffsets[g], 0);
    	}
    	
    	for (int row = 0; row < iDataSize; ++row)
    	{
    		for (int col = 0; col < iDataDim; ++col)
	    	{
    			double dValue = getData(row, col);
    			
    			int iGroup = col / iGroupDim;
	    		if (m_PeakValues[iGroup][row] < dValue)
	    		{
	    			m_PeakValues[iGroup][row] = dValue;
	    			m_PeakOffsets[iGroup][row] = col % iGroupDim;
	    		}
	    	}
    	}
    }
    
    public void readRegions(String gffFilePath)
    {
    	//TODO this is a hack, need to take the filename through ui or command line
    	
    	BioReader bioReader = new BioReader();
		InputStream streamGFF;
		try {
			streamGFF = new FileInputStream(gffFilePath);
			bioReader.readGFF(streamGFF);
			streamGFF.close();
			m_Features = bioReader.getFeatures();
			m_RegionNames = new String[m_Features.length];
			for (int i = 0; i < m_Features.length; ++i)
			{
				m_RegionNames[i] = m_Features[i].getSeqName() + ": " + m_Features[i].getStart() + " .. "+ m_Features[i].getEnd();
				m_MaxRegionSize = Math.max(m_Features[i].getEnd() - m_Features[i].getStart() + 1, m_MaxRegionSize);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    public void readRegions(DataModel dataModel)
    {
		m_Features = dataModel.getFeatures();
		m_RegionNames = dataModel.getDataTable().getFeatureNames();
		m_MaxRegionSize = dataModel.getParam().getRegionsSize();
		m_bEqualRegionSize = dataModel.getParam().getEqualRegionSize();
    }
    
    public void createUniformGroups(int iNumGroups)
    {
    	m_Groups = new GroupInfo[iNumGroups];
    	m_GroupOrder = new int[iNumGroups];
    	ClustInfo.m_GroupOrder = m_GroupOrder;
    	
    	int iGroupDim = m_Table.columns() / iNumGroups;
    	for (int i = 0; i < iNumGroups; ++i)
    	{
    		m_Groups[i] = new GroupInfo();
    		m_GroupOrder[i] = i;
    		//if (i < groupNames.length)
    		//	m_Groups[i].m_Name = groupNames[i];
    		if (i * iGroupDim < m_Table.columns())
    			m_Groups[i].m_Name = m_Table.getColName(i * iGroupDim);
    		
    		m_Groups[i].m_iCols = new int[iGroupDim];
    		for (int c = 0; c < iGroupDim; ++c)
    			m_Groups[i].m_iCols[c] = i * iGroupDim + c;
    	}
    }
    
    public static ClustInfo createFakeClusters(int iBranch, int iDepth, int[] indices)
    {
    	ClustInfo cInfo = new ClustInfo();
    	cInfo.m_Indices = indices;
    	if (iDepth > 0)
    	{
    		cInfo.m_bShowChildren = false;
    		ClustInfo prevChild = null;
    		for (int b = 0; b < iBranch; ++b)
    		{
    			int i1 = b*indices.length / iBranch;
    			int i2 = (b+1) * indices.length / iBranch;
    			
    			ClustInfo newChild = createFakeClusters(iBranch, iDepth - 1, Utils.intSequence(indices[i1], indices[i2 -1], 1));
    			newChild.m_Parent = cInfo;
    			
    			if (prevChild == null)
    				cInfo.m_Child = newChild;
    			else
    				prevChild.m_Sibling = newChild;
    			prevChild = newChild;
    		}
    	}
    	return cInfo;
    }
    
    public void calcClustStats(ClustInfo cInfo)
    {
		cInfo.setStats(new ClustStats());
		cInfo.getStats().calcStats(m_Table, cInfo.m_Indices, null, m_Groups);
		
		cInfo.setUnNormStats(new ClustStats());
		cInfo.getUnNormStats().calcStats(m_UnNormTable, cInfo.m_Indices, null, m_Groups);

		ClustInfo child = cInfo.m_Child;
		while (child != null)
		{
			calcClustStats(child);
			child = child.m_Sibling;
		}
    }

	/**
	 * return number of cluster members with a peak value higher than threshold
	 * @param cInfo       input cluster
	 * @param gi          group index
	 * @param threshold   the on/off threshold
	 * @return            number of "on" members
	 */
    public int countOnRegions(ClustInfo cInfo, int gi, float threshold)
	{
		int numOn = 0;
	    int iRows = cInfo.m_Indices.length;
	    double normThreshold = threshold;// / m_Groups[gi].m_CutOffMax;
		for( int idata = 0; idata < iRows; ++idata)
		{
			if (m_PeakValues[gi][cInfo.m_Indices[idata]] > normThreshold)
			{
				++numOn;
			}
		}
		return numOn;
	}

	/**
	 * Creates on/off cluster combinations, based on the input cInfo.m_Threshold
	 * @param cInfo        input cluster
	 */
	public void createThresholdClusters(ClustInfo cInfo)//, ClustInfo.Threshold[] thresholds)
	{
		if (cInfo.size() == 0)
			return;

		int numGroups = getNumGroups();
		assert(cInfo.m_Thresholds.length == numGroups);
		assert(cInfo.m_Thresholds.length < 64); // otherwize the m_ActiveOnOffGroups will overflow
		
		long activeOnOffGroups = 0;
		int numOnOffCombinations = 1;
		boolean bXOR = false; // whether there is any group with (on xor off) 
		for (int g = 0; g < numGroups; ++g)
		{
			//if (cInfo.m_Thresholds[g].value >= 0 && cInfo.m_Thresholds[g].value < 1)
			if (cInfo.m_Thresholds[g].on || cInfo.m_Thresholds[g].off)
			{
				if (cInfo.m_Thresholds[g].on && cInfo.m_Thresholds[g].off)
				{// this is a group having both on and off
					activeOnOffGroups |= 1 << g;
					numOnOffCombinations *= 2;
				}
				else if (cInfo.m_Thresholds[g].on || cInfo.m_Thresholds[g].off)
				{
					bXOR = true;
				}
			}
		}
		
		if (bXOR)
		{
			numOnOffCombinations++; // last group with all don't cares
		}
		
		if (numOnOffCombinations == 1)
		{
			cInfo.deleteChildren();
			return;
		}
		
		if (activeOnOffGroups != cInfo.m_ChildOnOffGroups || 
			cInfo.m_NumOnOffChildren != numOnOffCombinations)
		{
			cInfo.m_ChildOnOffGroups = activeOnOffGroups;
			cInfo.m_NumOnOffChildren = numOnOffCombinations;
			cInfo.m_OnOffChildOrder = new int[numOnOffCombinations];
			int reverseOrderNum = bXOR ? numOnOffCombinations - 1 : numOnOffCombinations;
			for (int i = 0; i < reverseOrderNum; ++i)
				cInfo.m_OnOffChildOrder[i] = reverseOrderNum - i - 1;
			if (bXOR)
				cInfo.m_OnOffChildOrder[numOnOffCombinations - 1] = numOnOffCombinations - 1;
		}
		
		char[][] infoLabels = new char[numOnOffCombinations][numGroups];
		int mask = 1;
		for (int g = 0; g <numGroups; ++g)
		{
			if ((cInfo.m_ChildOnOffGroups & (1 << g)) != 0)
			{// this is a group having both on and off
				for (int c = 0; c < numOnOffCombinations; ++c)
					infoLabels[c][g] = (mask & c) == 0 ? 'v' : '^';
				
				mask = mask << 1;
			} else
			{
				char label = !(cInfo.m_Thresholds[g].on ^ cInfo.m_Thresholds[g].off) ? ' ' : // when both on/off are true or false, label is don't care
							   cInfo.m_Thresholds[g].on ? '^' : 'v';
				for (int c = 0; c < numOnOffCombinations; ++c)
					infoLabels[c][g] = label;
			}
			if (bXOR)
				infoLabels[numOnOffCombinations - 1][g] = ' '; // the last group is all don't cares
		}
		
	    int iRows = cInfo.m_Indices.length;
		int[] clusterIds = new int[iRows];
		//int[] clusterSizes = new int[1 << cInfo.m_Threshold.length]; // maximum number of possible on/off combinations
		int[] clusterSizes = new int[numOnOffCombinations]; // maximum number of possible on/off combinations
		
		for( int idata = 0; idata < iRows; idata++ )
		{
			int iClustID = 0;
			
			int bit = 0;
			for (int g = 0; g < numGroups; ++g)
			{
				if ((cInfo.m_ChildOnOffGroups & (1 << g)) != 0)
				{
					if (m_PeakValues[g][cInfo.m_Indices[idata]] > cInfo.m_Thresholds[g].value)
					{
						iClustID += 1 << bit;
					}
					bit++;
				}
				if (cInfo.m_Thresholds[g].on ^ cInfo.m_Thresholds[g].off)
				{
					if ((cInfo.m_Thresholds[g].on && m_PeakValues[g][cInfo.m_Indices[idata]] <= cInfo.m_Thresholds[g].value) ||
						(cInfo.m_Thresholds[g].off && m_PeakValues[g][cInfo.m_Indices[idata]] > cInfo.m_Thresholds[g].value))
					{
						iClustID = numOnOffCombinations - 1;
						break; // this is a don't care. no need to continue further
					}
				}
			}
			
			clusterSizes[iClustID]++;
			clusterIds[idata] = iClustID;
		}
		
		boolean bIgnoreEmpty = false;
	    createChildren(cInfo, clusterIds, clusterSizes, bIgnoreEmpty);
	    cInfo.m_bShowChildren = true;
	    ClustInfo children[] = cInfo.getChildren();
	    assert(children.length == numOnOffCombinations);
	    
	    for (int c = 0; c < children.length; c++)
	    {
	    	try
	    	{
		    	children[c].m_InfoLabel = new String(infoLabels[c]);
		    	children[cInfo.m_OnOffChildOrder[c]].m_Sibling = (c < children.length - 1) ? children[cInfo.m_OnOffChildOrder[c+1]] : null;
	    	}
	    	catch (Exception e)
	    	{
	    		e.printStackTrace();
	    	}
	    }
	    cInfo.m_Child = children[cInfo.m_OnOffChildOrder[0]];
	    
	    /*
	    // label each group to indicate  ^(on) and v(off)
    	char[] cLabel = new char[getNumGroups()];
    	String[] clusterLabels = new String[clusterSizes.length];
		for (int c = 0; c < clusterSizes.length; ++c)
		{
			for (int gi = 0; gi < getNumGroups(); ++gi)
			{
				int bit = (1 << gi);
				//clusterSizes[c ^ bit] == 0 || clusterSizes[c ^ bit] == 0
				cLabel[gi] = ((c & bit) != 0) ? '^' : 'v';
			}
			clusterLabels[c] = new String(cLabel);
		}

	    ClustInfo childInfo = cInfo.m_Child;
	    while (childInfo != null)
	    {
	    	try
	    	{
	    		childInfo.m_InfoLabel = new String(infoLabels[clusterIds[childInfo.m_Indices[0]]]);
	    	}
	    	catch (Exception e)
	    	{
	    		e.printStackTrace();
	    	}
	    	childInfo = childInfo.m_Sibling;
	    }
		*/
	    
	}
	
	
	public void clusterByInputGroupLabel(ClustInfo cInfo)
	{
	    Hashtable<String, Integer> groupLabels = new Hashtable<String, Integer>();
        int iRows = cInfo.m_Indices.length;
        int numClusts = 0;
        
        // find the number of different labels
        for (int i = 0; i < iRows; ++i)
        {
            String sGroup = m_Features[cInfo.m_Indices[i]].getGroup();
            if (sGroup == null)
                sGroup = "";
            
            Integer clustID = groupLabels.get(sGroup);
            if (clustID == null) {
                groupLabels.put(sGroup, numClusts++);
            }
        }
        
        if (numClusts == 1) {
            // all regions have same group label. nothing to do.
            return;
        }
        
        int[] clusterSizes = new int[numClusts];
        int[] clusterIds = new int[iRows];
        
        // assign cluster ids to each region
        for (int i = 0; i < iRows; ++i)
        {
            String sGroup = m_Features[cInfo.m_Indices[i]].getGroup();
            clusterIds[i] = groupLabels.get(sGroup);
            clusterSizes[clusterIds[i]]++;
        }
       
        // create the clustering
        createChildren(cInfo, clusterIds, clusterSizes, true);
        cInfo.m_bShowChildren = true;
        
        // assign titles to clusters
        ClustInfo[] children = cInfo.getChildren();
        for (Map.Entry<String, Integer> entry : groupLabels.entrySet()) {
            children[entry.getValue()].setTitle(entry.getKey());
        }
        
        System.out.println("[Done] Cluster by GroupLabel");
	}
	
	public void kmeans(ClustInfo cInfo, int K) {
		// TODO Auto-generated method stub
		
		if (cInfo.size() == 0)
			return;
		
		getHistory().saveSnapShot("Kmeans(" + K + ")");
		
		if (K == 1)
		{
			cInfo.m_Child = null;
			cInfo.m_bShowChildren = false;
			return;
		}
		
		//cInfo.copyFrom(createFakeClusters(count, 1, cInfo.m_Indices));
		//cInfo.m_bShowChildren = true;
		//calcClustStats(cInfo);

		// System.out.println("[Started] K-Means for K=" + K);
    	final int MAX_ITERATIONS = 100;
		
	    int iRows = cInfo.m_Indices.length;
		int iCols = m_Table.columns(); 
		int[] clusterIds = new int[iRows];
		Arrays.fill(clusterIds, -1);
		double[][] clusterCentroids = new double[K][iCols];
		double[][] inputTable = m_Table.getTable();
		int[] clusterSizes = new int[K];
		
		// initialize the centroids randomly
		//long rSeed = 0x1EE7C0DE;
		Random rnd = new Random();//(rSeed);
		for (int c = 0; c < K; ++c)
		{
			int randIndex = rnd.nextInt(iRows);
			System.arraycopy(inputTable[cInfo.m_Indices[randIndex]] , 0, clusterCentroids[c], 0, iCols);
		}
		
	    int iIterations = 0;
		boolean bClustersChanged = true; // whether the clusters changed since last iteration
		int groupDim = getGroupDim();
		
	    while (bClustersChanged && iIterations++ < MAX_ITERATIONS)
	    {
	    	bClustersChanged = false;
	    	for (int idata = 0; idata < iRows; ++idata)
	    	{
	    		double minDist = Double.MAX_VALUE; // minimum distance from the point to cluster centroids
	    		int iClusterId = -1; // the cluster to which the point belongs
	    		double[] fValues = inputTable[cInfo.m_Indices[idata]];
	    		
				// calculate the distance from the centroids
	    		for (int c = 0; c < K; ++c)
	    		{
	    			double dist = 0; // the sum square distance
	    			for (int col = 0; col < iCols; ++col)
	    			{
	    				if (cInfo.m_KmeansActiveGroups != null && !cInfo.m_KmeansActiveGroups[col/groupDim])
	    					continue;
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
	    		for (int c = 0; c < K; ++c)
	    		{
	    			Arrays.fill(clusterCentroids[c], 0);
	    		}
	    		
	    		// sum
	    		for( int idata = 0; idata < iRows; idata++ )
	    		{
	    			double[] fValues = inputTable[cInfo.m_Indices[idata]];
	    			int iCluster = clusterIds[idata];
	    			clusterSizes[iCluster]++;
	    			for (int d = 0; d < iCols; ++d)
		    		{
	    				clusterCentroids[iCluster][d] += fValues[d];
		    		}
	    		}
	    		
	    		// calculate new centroids
	    		for (int c = 0; c < K; ++c)
	    		{
	    			if (clusterSizes[c] != 0)
	    			{
		    			for (int d = 0; d < iCols; ++d)
			    		{
		    				clusterCentroids[c][d] /= clusterSizes[c];
			    		}
	    			}
	    			else
	    			{// change the cluster centroid
	    				System.out.format("WARNING: Cluster %d has 0 members. Picking a new random centroid.\n", c);
	    				int randIndex = rnd.nextInt(iRows);
	    				for (int d = 0; d < iCols; ++d)
	    				{
	    	               System.arraycopy(inputTable[cInfo.m_Indices[randIndex]], 0, clusterCentroids[c], 0, iCols);
	    				}
	    				bClustersChanged = true;
	    			}
	    		}
	    	}
	    }

	    createChildren(cInfo, clusterIds, clusterSizes, true);
	    cInfo.m_bShowChildren = true;
	    
    	System.out.println("[Done] K-Means");

	}
	
	void createChildren(ClustInfo cInfo, int[] clusterIds, int[] clusterSizes, boolean ignoreEmpty)
	{
		int numNonZero = 0; // number of none-zero clusters
		int[] iNewClusterIds = new int[clusterSizes.length]; // maps the old cluster ids to new cluster ids
		for (int i = 0; i < clusterSizes.length; ++i)
		{
			iNewClusterIds[i] = numNonZero;
			if (!ignoreEmpty || clusterSizes[i] > 0) // ignore the clusters of size 0
				numNonZero++;
		}
		
	    ClustInfo childInfo[] = new ClustInfo[numNonZero];
	    int c = 0;
	    for (int i = 0; i < clusterSizes.length; ++i)
	    {
			if (!ignoreEmpty || clusterSizes[i] > 0)
			{
				childInfo[c] = new ClustInfo();
				childInfo[c].m_Parent = cInfo;
				if (clusterSizes[i] > 0)
					childInfo[c].m_Indices = new int[clusterSizes[i]];
				
		    	if (c > 0)
		    		childInfo[c - 1].m_Sibling = childInfo[c];
		    	c++;
			}
	    }
	    
	    // assign the data indices belonging to each cluster
	    int[] currIndex = new int[numNonZero];
	    for (int i = 0; i < clusterIds.length; ++i)
	    {
	    	c = iNewClusterIds[clusterIds[i]];
    		childInfo[c].m_Indices[currIndex[c]++] = cInfo.m_Indices[i];
	    }

	    for (c = 0; c < numNonZero; ++c)
	    {
    		calcClustStats(childInfo[c]);
	    }
	    
	    cInfo.m_Child = childInfo[0];
	}
	
	public void mergeClusters(ClustInfo cInfo1, ClustInfo cInfo2)
	{
		getHistory().saveSnapShot("Merge");
		
		if (cInfo1 != null && cInfo2 != null && cInfo1.m_Parent == cInfo2.m_Parent)
		{
			// need to find the previous sibling of cInfo2 before removing it.
			if (cInfo2.m_Parent.m_Child == cInfo2)
			{
				cInfo2.m_Parent.m_Child = cInfo2.m_Sibling;
			}
			else
			{
				ClustInfo cInfo = cInfo2.m_Parent.m_Child;
				while (cInfo != null && cInfo.m_Sibling != cInfo2)
					cInfo = cInfo.m_Sibling;
				if (cInfo != null)
					cInfo.m_Sibling = cInfo2.m_Sibling;
			}
			
			ClustInfo child2 = cInfo2.m_Child;
			while (child2 != null)
			{
				child2.m_Parent = cInfo1;
				child2 = child2.m_Sibling;
			}
			
			if (cInfo1.m_Child != null)
			{
				cInfo1.m_Child = cInfo2.m_Child;
			}
			else if (cInfo2.m_Child != null) 
			{
				ClustInfo lastChild1 = cInfo1.m_Child;
				while (lastChild1.m_Sibling != null)
					lastChild1 = lastChild1.m_Sibling;
				lastChild1.m_Sibling = cInfo2.m_Child;
			}
			
			cInfo1.m_Indices = ClustCombine.combine(cInfo1, cInfo2, ClustCombine.CombineOp.UNION).m_Indices;
			cInfo1.getStats().calcStats(m_Table, cInfo1.m_Indices, null, m_Groups);
			
			if (cInfo1.m_InfoLabel != null && cInfo2.m_InfoLabel != null && cInfo1.m_InfoLabel.length() == cInfo2.m_InfoLabel.length())
			{
				char[] newLabel = new char[cInfo1.m_InfoLabel.length()];
				for (int i = 0; i < cInfo1.m_InfoLabel.length(); ++i)
				{
					newLabel[i] = cInfo1.m_InfoLabel.charAt(i) == cInfo2.m_InfoLabel.charAt(i) ? cInfo1.m_InfoLabel.charAt(i) : ' ';
				}
				cInfo1.m_InfoLabel = new String(newLabel);
			}
		}
	}
	
	/**
	 * Changes the order of a node among the children of its parents, by a given offset 
	 * @param cInfo   the child node
	 * @param offset  the offset to move the order of the children by
	 */
	public void reorderClusterNode(ClustInfo cInfo, int offset)
	{
		if (cInfo == null || cInfo.m_Parent == null || offset == 0)
			return;
		
		ClustInfo[] siblings = cInfo.m_Parent.getChildren();
		for (int ci = 0; ci < siblings.length; ++ci)
		{
			if (siblings[ci] == cInfo)
			{
				int newIndex = ci + offset;
				if (newIndex >= 0 && newIndex < siblings.length)
				{
					// first take cInfo Out
					if (ci == 0)
						cInfo.m_Parent.m_Child = cInfo.m_Sibling;
					else
						siblings[ci-1].m_Sibling = cInfo.m_Sibling;
					
					// now insert back to the new location
					if (newIndex < ci)
					{
						cInfo.m_Sibling = siblings[newIndex];
						if (newIndex == 0)
							cInfo.m_Parent.m_Child = cInfo;
						else
							siblings[newIndex-1].m_Sibling = cInfo;
					}
					else
					{
						cInfo.m_Sibling = siblings[newIndex].m_Sibling; 
						siblings[newIndex].m_Sibling = cInfo;
					}
					int[] onOffOrder = cInfo.m_Parent.m_OnOffChildOrder;
					if (onOffOrder != null)
					{
						int tmp = onOffOrder[ci];
						onOffOrder[ci] = onOffOrder[newIndex];
						onOffOrder[newIndex] = tmp;
					}
				}
				break;
			}
		}
	}
	
	public void setAutomaticThresholds(ClustInfo cInfo)
	{
		int numGroups = getNumGroups();
		cInfo.initThresholds(numGroups);
		for (int gi = 0; gi < numGroups; ++gi)
		{
			double[] hist = cInfo.getStats().m_PeakHist[gi];
			for (int i = 1; i < hist.length; i++)
			{
				if (hist[i] > hist[i - 1]) // forward differencing
				{
					cInfo.m_Thresholds[gi].value = 1.f * (i-1) / hist.length;
					cInfo.m_Thresholds[gi].on = true;
					cInfo.m_Thresholds[gi].off = false;
					break;
				}
			}
		}
	}
	
	public enum SortCriteria
	{
		GENOMIC_LOCATION,
		SIGNAL_AVERAGE,
		SIGNAL_MEDIAN,
		SIGNAL_MIN,
		SIGNAL_PEAK,
		SIGNAL_PEAK_OFFSET,
		INPUT_ORDER,
		INPUT_GROUP_LABEL,
	};
	
	/**
	 * Returns the sorted indices of the members of a cluster based on the specified sorting criteria
	 * @param cInfo      reference to the cluster info object
	 * @param gi         index of the group (mark) to sort by
	 * @param criteria   the sorting criteria
	 * @param ascending  whether to to sort in an ascending or descending order
	 * @return  sorted indices of the members of a cluster
	 */
	public int[] getSortedIndices(ClustInfo cInfo, int gi, SortCriteria criteria, boolean ascending)
	{
		if (gi < 0 || gi >= m_Groups.length)
			return cInfo.m_Indices;
		
		double[] sortValue = new double[cInfo.size()];
		int numGroupCols = m_Groups[gi].m_iCols.length;
		double[] rowVals = new double[numGroupCols];
		
		for (int i = 0; i < cInfo.size(); i++)
		{
			double minVal = Double.POSITIVE_INFINITY;
			double maxVal = 0;
			double avgVal = 0;
			int peakOffset = -1;
            //for (int c = 70; c < 90; c++) // sort by middle columns only
			for (int c = 0; c < numGroupCols; c++)
			{
				double val = getData(cInfo.m_Indices[i], m_Groups[gi].m_iCols[c]);
				rowVals[c] = val;
				peakOffset = val > maxVal ? c : peakOffset;
				minVal = Math.min(minVal, val);
				maxVal = Math.max(maxVal, val);
				avgVal += val / numGroupCols;
			}
			
			
			switch(criteria)
			{
				case GENOMIC_LOCATION:
					sortValue[i] = cInfo.m_Indices[i];
					break;
				case SIGNAL_AVERAGE:
					sortValue[i] = avgVal;
					break;
				case SIGNAL_MEDIAN:
					Arrays.sort(rowVals);
					sortValue[i] = rowVals[rowVals.length / 2] +
					               avgVal*0.00000001; // just so that items with the same median sort based on average
					break;
				case SIGNAL_MIN:
					sortValue[i] = minVal;
					break;
				case SIGNAL_PEAK:
					sortValue[i] = maxVal;
					break;
				case SIGNAL_PEAK_OFFSET:
					sortValue[i] = peakOffset;
					break;
				case INPUT_ORDER:
					sortValue[i] = m_Features[cInfo.m_Indices[i]].getInputOrder();
					break;
				case INPUT_GROUP_LABEL:
					sortValue[i] = m_Features[cInfo.m_Indices[i]].getGroupOrder();
					break;
			}
		}
		
		int sortedIndex[] = ascending ? 
			still.data.FloatIndexer.sortFloats(sortValue) :
			still.data.FloatIndexer.sortFloatsRev(sortValue);

		for (int i = 0; i < sortedIndex.length; i++) {
			sortedIndex[i] = cInfo.m_Indices[sortedIndex[i]];
		}
		
		return sortedIndex;
	}
	
	public String getClusterRegionsString(ClustInfo cInfo, int sortGroupIndex, SortCriteria sortCriteria, boolean isAscendingSort)
	{
		ClustInfo[] nodes = cInfo.getVisibleNodes(true);
		StringBuffer strBuf = new StringBuffer();
		for (int n = 0; n < nodes.length; n++)
		{
			int[] sortedIndices = getSortedIndices(nodes[n], sortGroupIndex, sortCriteria, isAscendingSort);

			for (int idx = 0; idx < sortedIndices.length; ++idx)
			{
				Feature ftr = m_Features[sortedIndices[idx]];
				strBuf.append(ftr.toString());
				if (ftr.getGroup() == null || ftr.getGroup().isEmpty()) {
					strBuf.append("\t");
				} else {
					strBuf.append(";");
				}
					
				strBuf.append("cid=" + n);
				if (!nodes[n].getTitle().isEmpty()) {
					strBuf.append(";title=" + nodes[n].getTitle());
				}
				strBuf.append("\n");
			}
		}
		return strBuf.toString();
	}
	
	public void saveFramework(String filename) 
	{
		try {
			SerialClustFramework scFramework = new SerialClustFramework();
			scFramework.copyFrom(this);
			
			ObjectMapper mapper = new ObjectMapper(); // can reuse, share globally
			mapper.configure(SerializationConfig.Feature.INDENT_OUTPUT, true);
			mapper.writeValue(new File(filename), scFramework);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void loadFramework(String filename) 
	{
		try {
			ObjectMapper mapper = new ObjectMapper(); // can reuse, share globally
			SerialClustFramework scFramework = mapper.readValue(new File(filename), SerialClustFramework.class);
			scFramework.copyTo(this);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void majorUpdate()
	{
		updateAllClustStats();
		if (m_ChangeListener != null) {
			m_ChangeListener.stateChanged(new ChangeEvent(this));
		}
	}
	
	public void saveClusterRegions(File outputFile, ClustInfo cInfo, int sortGroupIndex, SortCriteria sortCriteria, boolean isAscendingSort)
	{
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile));
			bw.write(getClusterRegionsString(cInfo, sortGroupIndex, sortCriteria, isAscendingSort));
			bw.close();
		} catch (IOException e) {

			e.printStackTrace();
		}
	}
	
	public FavoriteList getFavorites()
	{
		return m_Favorites;
	}

	public FavoriteList getWorkingSet()
	{
		return m_WorkingSet;
	}
	
    public void setChangeListener(ChangeListener listener) 
    {
    	m_ChangeListener = listener;
    }
    /**
     * Saves the average values of each mark for each region as a CSV file
     * @param cInfo  array of input clusters
     * @return
     */
    public boolean saveAsCSV(ClustInfo[] cInfo)
    {
    	if (cInfo == null || cInfo.length == 0)
    		return false;
    	
		JFileChooser fc = new JFileChooser();
		fc.setFileFilter(new FileNameExtensionFilter("CSV file", "csv"));
		if (fc.showSaveDialog(null) == JFileChooser.APPROVE_OPTION)
		{
			int iNumRows = 0;
			// compute total number of rows
			for (int ci = 0; ci < cInfo.length; ++ci) {
				iNumRows += cInfo[ci].size();
			}
			
			double[][]  avgTable = new double[iNumRows][getNumGroups()];
	    	int row = 0;
	    	
	    	// compute average values for each column
			for (int ci = 0; ci < cInfo.length; ++ci) 
			{
				for (int i = 0; i < cInfo[ci].size(); i++)
				{
					for (int gi = 0; gi < getNumGroups(); ++gi)
					{
						int numGroupCols = m_Groups[gi].m_iCols.length;
						double avgVal = 0;
						for (int c = 0; c < numGroupCols; c++)
						{
							double val = getData(cInfo[ci].m_Indices[i], m_Groups[gi].m_iCols[c]);
							avgVal += val / numGroupCols;
						}
						avgTable[row][gi] = avgVal;
					}
					row++;
				}
			}
			
			// get column names
	    	String[] columnNames = new String[getNumGroups()];
			for (int gi = 0; gi < getNumGroups(); ++gi)
			{
				columnNames[gi] = m_Groups[gi].m_Name;
			}
			
			MemoryTable m = new MemoryTable(avgTable, columnNames);
			TableFactory.saveTableCSV(m, fc.getSelectedFile());
			return true;
		}
		return false;
    }
}
