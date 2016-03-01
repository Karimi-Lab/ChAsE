package still.operators;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.io.Serializable;
import java.util.Arrays;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import still.data.FloatIndexer;
import still.data.Function;
import still.data.Group;
import still.data.Map;
import still.data.Operator;
import still.data.Table;
import still.data.TableEvent;
import still.data.TableEvent.TableEventType;
import still.gui.EnumUtils;
import still.gui.OPAppletViewFrame;
import still.gui.OperatorView;
import still.gui.PHeatMapPainter;

public class HeatMapOp extends Operator implements Serializable
{
	private static final long serialVersionUID = -6874872676907052211L;

	class ColumnInfo
	{
		int			m_iIndex;
		boolean 	m_bSelected;
		boolean		m_bCulled;
		ColType		m_NewColType;
	}
	
	ColumnInfo[] 	m_ColumnInfo	= null; //[input.cols()]  additional ColumnInfo for each column/dim
	int[] 			m_SortedIndex = null;   //[input.rows()]  index of each row after sorting
	public int 		m_iGroupDims  = 60;
	public int 		m_iNumGroups = 1;
	public int		m_iSortGroupIndex = -1;
	
	/**
	 * Additional information for each group of columns
	 */
	public class GroupInfo
	{
		public class Clustering // contains one clustering instance
		{
			public int 	  	m_iNumClusters; 		///< number of clusters in this group
			public int 	  	m_ClusterSize[]; 		///< [m_iNumClusters] size of each cluster
			public int 	  	m_ClusterID[];         	///< [input.rows()]: group cluster id for each row
			public double 	m_iClusterProfile[][]; 	///< [m_iNumClusters][m_iGroupDims]: average profile for each cluster
			
			public Clustering clone()
			{
				Clustering result = new Clustering();
				result.m_iNumClusters    = m_iNumClusters;
				result.m_ClusterSize 	 = m_ClusterSize.clone();
				result.m_ClusterID 		 = m_ClusterID.clone();
				result.m_iClusterProfile = m_iClusterProfile.clone();
				return result;
			}
			
			public ClusterSimilarityInfo m_PrevSimilarity; // similarity to previous clustering snapshot
		}
		
		Clustering createClustering() { return new Clustering(); }
		public Clustering 	m_Clusterings[];
		public int 			m_iCurrentClustering = 0;
		public boolean		m_bIsSelected = true;
	};
	public GroupInfo m_GroupInfo[] = null;  	///< [m_iNumGroups]  A GroupInfo object per group.

	public enum SimilarityMetric
	{
		HEURISTIC,
		MALLOWS_DISTANCE,
		FDIVERGENCE,
		FMEASURE
	};
	
	public SimilarityMetric m_SimilarityMetric = SimilarityMetric.FMEASURE;
	
	public class ClusterSimilarityInfo
	{
		public int	m_Count[][] = null;  ///< number of points shared between every pair of clusters in two groups
		public double m_Similarity[][] = null; ///< similarity between every pair of clusters in two groups
	}
	public ClusterSimilarityInfo[][] m_GroupSimilarityInfo = null; // similarity between every pair of groups

	
	public enum GroupSortType
	{
		NONE,
		AVERAGE,
		PEAK,
		PEAK_OFFSET
	}
	public GroupSortType m_GroupSortType = GroupSortType.AVERAGE;
	

	public HeatMapOp( Table newTable, boolean isActive, String paramString )
	{
		this( newTable, isActive );
	}
	
	public HeatMapOp( Table newTable, boolean isActive )
	{
		super( newTable );
		
		if (input.columns() != m_iNumGroups * m_iGroupDims)
		{//TODO: Hack
			m_iNumGroups = 1;
			m_iGroupDims = input.columns();
		}
		
		initColumnIndex();
		initGroupClusters();
		this.isActive = isActive;
		if( isActive )
		{
			this.updateMap();
			this.updateFunction();
			isLazy  		= true;
			setView( new HeatMapView( this ) );
		}
	}

	public static String getMenuName()
	{
		return "View:HeatMap";
	}

	public void setMeasurement( int point_idx, int dim, double value )
	{
		input.setMeasurement(m_SortedIndex[point_idx], m_ColumnInfo[dim].m_iIndex, value);
	}

	public ColType getColType( int dim )
	{
		return m_ColumnInfo[dim].m_NewColType;
	}

	public String getColName( int dim )
	{
		return input.getColName(m_ColumnInfo[dim].m_iIndex);
	}

	public String toString() {
		
		return "[View:HeatMap]";
	}
	
	public String getSaveString( ) {
		
		return "";
	}
	
	void initColumnIndex()
	{
		if (m_ColumnInfo == null || m_ColumnInfo.length != input.columns())
		{
			m_ColumnInfo = new ColumnInfo[input.columns()];
			for (int i = 0; i < input.columns(); i++)
			{
				m_ColumnInfo[i] = new ColumnInfo();
				m_ColumnInfo[i].m_iIndex = i;
				m_ColumnInfo[i].m_bSelected = false;
				m_ColumnInfo[i].m_bCulled   = false;
				m_ColumnInfo[i].m_NewColType = input.getColType(i);
			}
		}
		
		if (m_SortedIndex == null || m_SortedIndex.length != input.rows())
		{
			m_SortedIndex = new int[input.rows()];
			for (int i = 0; i < input.rows(); i++)
			{
				m_SortedIndex[i] = i;
			}
		}
	}
	
	public void setSelectedCols(int[] selectedCols)
	{
		for (int i = 0; i < m_ColumnInfo.length; i++)
		{
			m_ColumnInfo[i].m_bSelected = false;
		}
		
		if (selectedCols != null)
		{
			for (int i = 0; i < selectedCols.length; i++)
			{
				m_ColumnInfo[selectedCols[i]].m_bSelected = true;
			}
		}
	}
	
	public void sortRowsByGroupIndex(int iGroup, GroupSortType sortType)
	{
		m_GroupSortType = sortType;
		m_iSortGroupIndex = iGroup;
		int selectedCols[] = new int[Math.min(m_iGroupDims, input.columns() - iGroup * m_iGroupDims)];
		for (int i = 0; i < selectedCols.length; i++)
		{
			selectedCols[i] = i + iGroup * m_iGroupDims;
		}
		setSelectedCols(selectedCols);
		sortRowsBySelectedColumns(m_GroupSortType);
	}

	public void sortRowsBySelectedColumns(GroupSortType sortType)
	{
		m_GroupSortType = sortType;
		boolean bAnyColSelected = false;
		double dVal[] = null;
		
		boolean bSortByCluster = true;
		int[] sortedClusterIndex = null;
		GroupInfo.Clustering gic = null;
		if (bSortByCluster)
		{
			int iGroupIndex = -1;
			for (int col = 0; col < m_ColumnInfo.length; col++)
			{
				if (m_ColumnInfo[col].m_bSelected)
				{
					iGroupIndex = col / m_iGroupDims;
				}
			}
			gic = getCurrGroupInfoClustering(iGroupIndex);
			if (gic != null)
			{
				double[] clusterSizes = new double[ gic.m_iNumClusters];
				for (int c = 0; c < gic.m_iNumClusters; c++)
				{
					clusterSizes[c] = gic.m_ClusterSize[c];
				}
				int sorted[] = FloatIndexer.sortFloats(clusterSizes);
				sortedClusterIndex = new int[sorted.length];
				for (int i = 0; i < sorted.length; i++)
					sortedClusterIndex[sorted[i]] = i;
			}
		}
		
		if (sortType != GroupSortType.NONE)
		{
			dVal = new double[input.rows()];
			for (int i = 0; i < input.rows(); i++)
			{
				double dPeakVal = Double.NEGATIVE_INFINITY;
				for (int col = 0; col < m_ColumnInfo.length; col++)
				{
					if (m_ColumnInfo[col].m_bSelected)
					{
						bAnyColSelected = true;
						double dValue = input.getMeasurement(i, m_ColumnInfo[col].m_iIndex);
						switch (sortType)
						{
							case AVERAGE:
								dVal[i] += dValue;
								break;
							case PEAK:
								dVal[i] = Math.max(dVal[i], dValue);
								break;
							case PEAK_OFFSET:
								if (dPeakVal < dValue)
								{
									dPeakVal = dValue;
									dVal[i] = (1 + m_ColumnInfo.length - col) * 100 + dPeakVal;
								}
								break;
						}
					}
				}
				
				if (gic != null)
				{
					dVal[i] += 1000000*(sortedClusterIndex[gic.m_ClusterID[i]]);
					//dVal[i] = sortedClusterIndex[gic.m_ClusterID[i]];
				}
			}
		}
		
		if (sortType == GroupSortType.NONE || !bAnyColSelected)
		{
			for (int i = 0; i < m_SortedIndex.length; i++)
			{
				m_SortedIndex[i] = i;
			}
		}
		else
		{
			for (int i = 0; i < dVal.length; i++)
			{
				dVal[i] = -dVal[i]; // to inverse the sort order from high to low
			}
			
			m_SortedIndex = FloatIndexer.sortFloats(dVal);
		}
		
		super.tableChanged( new TableEvent(this, TableEvent.TableEventType.TABLE_CHANGED ), true);
	}
	
	public void maskSelectedColumns()
	{
		for (int col = 0; col < m_ColumnInfo.length; col++)
		{
			if (m_ColumnInfo[col].m_bSelected)
			{
				if (m_ColumnInfo[col].m_NewColType == ColType.METADATA)
				{
					m_ColumnInfo[col].m_NewColType = ColType.NUMERIC;
				}
				else
				{
					m_ColumnInfo[col].m_NewColType = ColType.METADATA;
				}
			}
		}
		super.tableChanged( new TableEvent(this, TableEvent.TableEventType.TABLE_CHANGED ), true);
	}
	
	public void activate()
	{
		this.isActive = true;
		this.updateMap();
		this.updateFunction();
		isLazy  		= true;
		setView( new HeatMapView( this ) );
	}
	
	@Override
	public void updateFunction()
	{
		function = new SortFunction(this);
	}
	
	public class SortFunction implements Function
	{
		private HeatMapOp m_Operator = null;
		
		public SortFunction(HeatMapOp op)
		{
			m_Operator = op;
		}
		
		@Override
		public Table apply(Group group) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public double compute(int row, int col) 
		{
			if (m_Operator.m_SortedIndex.length > row && m_Operator.m_ColumnInfo.length > col)
				return m_Operator.input.getMeasurement(m_Operator.m_SortedIndex[row], m_Operator.m_ColumnInfo[col].m_iIndex);
			else
				return 0;
		}

		@Override
		public Group inverse(Table dims) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public double[] invert( Map map, int row, int col, double value )
		{
			double[] ret = new double[1];
			ret[0] = value;
			return ret;
		}

		@Override
		public int[] outMap() {
			// TODO Auto-generated method stub
			return null;
		}
	}

	@Override
	public void updateMap()
	{
		int dims = input.columns();
		assert(dims == m_ColumnInfo.length);
		boolean[][] bmap = new boolean[dims][dims];
		for( int i = 0; i < dims; i++ )
		{
			bmap[m_ColumnInfo[i].m_iIndex][i] = true;
		}
		map = new Map(bmap);
	}

	void invokeRedraw(boolean bUpdatePGxHeatmap)
	{
    	PHeatMapPainter pHMPainter = ((PHeatMapPainter)((OPAppletViewFrame)((HeatMapView)getView()).getViewFrame()).procApp); 
		pHMPainter.invokeRedraw(bUpdatePGxHeatmap);
		
//		if( this.isActive() )
//		{
//			SwingUtilities.invokeLater(new Runnable()
//			{
//		        public void run()
//		        {
//		        	PHeatMapPainter pHMPainter = ((PHeatMapPainter)((OPAppletViewFrame)((HeatMapView)getView()).getViewFrame()).procApp); 
//		    		pHMPainter.redraw();
//		        }
//			});
//		}
	}
	
	public void tableChanged( TableEvent te ) 
	{
		initColumnIndex();
		initGroupClusters();
		super.tableChanged(te);
		if (te.type != TableEventType.ATTRIBUTE_CHANGED)
		{
			((HeatMapView)getView()).buildGUI();
		}
		sortRowsBySelectedColumns(m_GroupSortType);
		//loadOperatorView();
		invokeRedraw(true);
	}

	public void loadOperatorView()
	{
		setView( new HeatMapView( this ) );
	}
	
	int getSelectionCol()
	{
		int i = 0;
		for (ColType type : input.getColTypes())
		{
			if (type == ColType.ATTRIBUTE && input.getColName(i).equalsIgnoreCase("selection") )
			{
				return i;
			}
			i++;
		}
		return -1;
	}
	
	public void setSelectedValue(double dValue)
	{
		int iSelectionCol = getSelectionCol();
		if (iSelectionCol == -1)
		{// no selection column
			return;
		}
		
		
		for (int i = 0; i < input.rows(); i++)
		{
			if (input.getMeasurement(i, iSelectionCol) > 0)
			{
				for (int col = 0; col < m_ColumnInfo.length; col++)
				{
					if (m_ColumnInfo[col].m_bSelected)
					{
						input.setMeasurement(i, m_ColumnInfo[col].m_iIndex, dValue);
					}
				}
			}
		}
		tableChanged( new TableEvent(this, TableEvent.TableEventType.TABLE_CHANGED ), true);
	}
	
	public GroupInfo getGroupInfo(int iGroup)
	{
		if (iGroup < 0 || iGroup >= m_iNumGroups || m_GroupInfo == null || m_GroupInfo.length < iGroup)
		{
			return null;
		}
		return m_GroupInfo[iGroup];
	}
	
	public GroupInfo.Clustering getCurrGroupInfoClustering(int iGroup)
	{
		GroupInfo gi = getGroupInfo(iGroup);
		if (gi != null)
		{
			return getGroupInfoClustering(iGroup, gi.m_iCurrentClustering);
		}
		return null;
	}
	
	public void setCurrGroupInfoClustering(int iGroup, int theClustering)
	{
		GroupInfo gi = getGroupInfo(iGroup);
		if (gi != null)
		{
			gi.m_iCurrentClustering = theClustering;
			calcAllClusterSimilarity();
		}
	}

	public GroupInfo.Clustering getGroupInfoClustering(int iGroup, int iClustering)
	{
		GroupInfo gi = getGroupInfo(iGroup);
		if (gi != null && gi.m_Clusterings != null && iClustering >= 0 && gi.m_Clusterings.length > iClustering)
		{
			return gi.m_Clusterings[iClustering];
		}
		return null;
	}
	
	
	public void initGroupCluster(int iGroup)
	{
		GroupInfo gi = getGroupInfo(iGroup);
		if (gi != null)
		{
			gi.m_Clusterings = new GroupInfo.Clustering[32];
			gi.m_Clusterings[0] = gi.createClustering();
			gi.m_Clusterings[0].m_iNumClusters = 1;
			gi.m_Clusterings[0].m_ClusterID = new int[input.rows()];
			calcGroupClusterProfile(iGroup);
		}
	}

	public void initGroupClusters()
	{
		m_GroupInfo = new GroupInfo[m_iNumGroups];
		for (int g = 0; g < m_iNumGroups; g++)
		{
			m_GroupInfo[g] = new GroupInfo();
			initGroupCluster(g);
		}
		computeClusterCombinations();
	}
	
	public enum GroupClusteringMethod
	{
		PEAK_OFFSET,
		PEAK_VALUE,
		KMEANS
	};
	public GroupClusteringMethod m_GroupClusteringMethod = GroupClusteringMethod.PEAK_OFFSET;
	
	public void clusterGroup(int iGroup, int iNumClusters)
	{
		GroupInfo.Clustering gic = getCurrGroupInfoClustering(iGroup);
		if (gic == null)
			return;

		gic.m_iNumClusters = iNumClusters;
		
		if (m_GroupClusteringMethod == GroupClusteringMethod.PEAK_OFFSET)
		{
			for (int irow = 0; irow < input.rows(); irow++)
			{
				double dPeakVal = 0;
				int iPeakCol = -1;
				for (int icol = 0; icol < m_iGroupDims; icol++)
				{
					double dValue = input.getMeasurement(irow, m_ColumnInfo[icol + iGroup * m_iGroupDims].m_iIndex);
					if (dValue > dPeakVal)
					{
						dPeakVal = dValue;
						iPeakCol = icol;
					}
				}
				
				int iClusterID = 0;
				if (iPeakCol > -1)
				{
					// cluster based on the peak offset
					iClusterID = 1 + iPeakCol * (iNumClusters - 1) / m_iGroupDims;
				}
				gic.m_ClusterID[irow] = iClusterID;
			}
		}
		// else if (m_GroupClusteringMethod = ) .... //TODO: add more clustering options
		
		calcGroupClusterProfile(iGroup);
		computeClusterCombinations();
	}
	
	public void insertSnapShotAfter(int iGroup, int iCurrIndex)
	{
		GroupInfo gi = getGroupInfo(iGroup);
		GroupInfo.Clustering gic = getGroupInfoClustering(iGroup, iCurrIndex);
		if (gi == null || gic == null)
			return;
		
		for (int i = iCurrIndex + 1; i < gi.m_Clusterings.length; i++)
		{// delete everything after
			gi.m_Clusterings[i] = null;
		}
		gi.m_Clusterings[iCurrIndex + 1] = gic.clone();
		gi.m_iCurrentClustering = iCurrIndex + 1;
	}
	
	public void clusterSelected(int iGroup, int iNumClusters)
	{
		GroupInfo gi = getGroupInfo(iGroup);
		insertSnapShotAfter(iGroup, gi.m_iCurrentClustering);

		GroupInfo.Clustering gic = getCurrGroupInfoClustering(iGroup);
		if (gic == null)
			return;
		
		int iSelectionCol = getSelectionCol();
		//if (iSelectionCol == -1) // no selection column
		//	return;

		
		int iNumPrevClusters = Math.max(1, gic.m_iNumClusters);
		int iPrevClusterCount[] = new int[iNumPrevClusters];
		int iNewClusterID[] = new int[iNumPrevClusters];
		int iNumSelected = 0;
		// reassign new cluster ids
		int cBaseID = 0; // new clusters ids will be cBaseID + 0, cBaseID + 1, ...
		
		boolean bClusterAll = false; // whether to re-cluster every thing

		if (iSelectionCol != -1)
		{
			for (int irow = 0; irow < input.rows(); irow++)
			{
				if (input.getMeasurement(irow, iSelectionCol) > 0)
				{
					iNumSelected++;
				}
				else
				{
					iPrevClusterCount[gic.m_ClusterID[irow]]++;
				}
			}
			for (int c1 = 0; c1 < iNumPrevClusters; c1++)
			{
				if (iPrevClusterCount[c1] > 0)
				{
					iNewClusterID[c1] = cBaseID;
					cBaseID++;
				}
			}
		}
		
		if (iNumSelected == 0)
		{// nothing is selected. what should be the default behavior? cluster all, or nothing. I am doing all:
			bClusterAll = true;
			iNumSelected = input.rows();
			cBaseID = 0;
		}
		
		int iMinPeakCol = m_iGroupDims;
		int iMaxPeakCol = 0;
		double dMinPeakVal = Double.MAX_VALUE;
		double dMaxPeakVal = 0;
		// gather stats first
		for (int irow = 0; irow < input.rows(); irow++)
		{
			if (bClusterAll || input.getMeasurement(irow, iSelectionCol) > 0)
			{
				double dPeakVal = 0;
				int iPeakCol = -1;
				for (int icol = 0; icol < m_iGroupDims; icol++)
				{
					double dValue = input.getMeasurement(irow, m_ColumnInfo[icol + iGroup * m_iGroupDims].m_iIndex);
					if (dValue > dPeakVal)
					{
						dPeakVal = dValue;
						iPeakCol = icol;
					}
				}
				if (iPeakCol >= 0)
				{
					iMinPeakCol = Math.min(iMinPeakCol, iPeakCol);
					iMaxPeakCol = Math.max(iMaxPeakCol, iPeakCol);
				}
				dMinPeakVal = Math.min(dMinPeakVal, dPeakVal);
				dMaxPeakVal = Math.max(dMaxPeakVal, dPeakVal);
			}
		}

		for (int irow = 0; irow < input.rows(); irow++)
		{
			if (bClusterAll || input.getMeasurement(irow, iSelectionCol) > 0)
			{
				double dPeakVal = 0;
				int iPeakCol = -1;
				for (int icol = 0; icol < m_iGroupDims; icol++)
				{
					double dValue = input.getMeasurement(irow, m_ColumnInfo[icol + iGroup * m_iGroupDims].m_iIndex);
					if (dValue > dPeakVal)
					{
						dPeakVal = dValue;
						iPeakCol = icol;
					}
				}
				
				int iClusterID = 0;
				
				switch (m_GroupClusteringMethod)
				{
					case PEAK_OFFSET: // cluster based on the peak offset 
						if (iPeakCol > -1)
						{
							iClusterID = iMaxPeakCol > iMinPeakCol ? (iPeakCol - iMinPeakCol) * iNumClusters / (iMaxPeakCol + 1 - iMinPeakCol) : 0;
						}
						break;
					case PEAK_VALUE: 
						iClusterID = dMaxPeakVal > dMinPeakVal ? (int)((dPeakVal - dMinPeakVal) * iNumClusters / (dMaxPeakVal - dMinPeakVal)) : 0;
						break;
				}
				if (iClusterID > 0)
					iClusterID = iClusterID + 0;
				gic.m_ClusterID[irow] = cBaseID + Math.min(iClusterID, iNumClusters - 1);  // making sure we won't overflow
			}
			else
			{
				gic.m_ClusterID[irow] = iNewClusterID[gic.m_ClusterID[irow]];
			}
		}
		
		gic.m_iNumClusters = cBaseID + iNumClusters;
		calcGroupClusterProfile(iGroup);
		calcAllClusterSimilarity();
		
		int iCurr = gi.m_iCurrentClustering;
		if (iCurr > 0)
		{// calculate the similarities, used in the clustering hierarchy view
			gi.m_Clusterings[iCurr].m_PrevSimilarity = calcClusteringSimilarities(gi.m_Clusterings[iCurr - 1], gi.m_Clusterings[iCurr]);
		}

		computeClusterCombinations();
	}
	
	
	void calcGroupClusterProfile(int iGroup)
	{
		GroupInfo.Clustering gic = getCurrGroupInfoClustering(iGroup);
		if (gic == null)
			return;

		gic.m_iClusterProfile = new double[gic.m_iNumClusters][m_iGroupDims];
		gic.m_ClusterSize = new int[gic.m_iNumClusters];
		
		// add the rows that are in one cluster
		for (int irow = 0; irow < input.rows(); irow++)
		{
			int id = gic.m_ClusterID[irow];
			gic.m_ClusterSize[id]++;
			for (int icol = 0; icol < m_iGroupDims; icol++)
			{
				gic.m_iClusterProfile[id][icol] += input.getMeasurement(irow, m_ColumnInfo[icol + iGroup * m_iGroupDims].m_iIndex);
			}
		}
		
		// take average by deviding by the size
		for (int c = 0; c < gic.m_iNumClusters; c++)
		{
			for (int icol = 0; icol < m_iGroupDims; icol++)
			{
				gic.m_iClusterProfile[c][icol] /= gic.m_ClusterSize[c];
			}
		}
	}
	
	public class GroupProfile
	{
		public double[] m_ProfileMean;
		public double[] m_ProfileStdDev;
		public int m_iCount;
	}
	
	public GroupProfile calcGroupProfile(int iGroup, boolean[] bActiveRows)
	{
		GroupInfo gi = getGroupInfo(iGroup);
		if (gi == null)
			return null;
		
		GroupProfile profile = new GroupProfile();
		profile.m_ProfileMean = new double[m_iGroupDims];
		Arrays.fill(profile.m_ProfileMean, 0);
		profile.m_ProfileStdDev = new double[m_iGroupDims];
		Arrays.fill(profile.m_ProfileStdDev, 0);
		profile.m_iCount = 0;

		// compute mean:
		for (int irow = 0; irow < input.rows(); irow++)
		{
			if (bActiveRows == null || bActiveRows[irow])
			{
				profile.m_iCount++;
				for (int icol = 0; icol < m_iGroupDims; icol++)
				{
					profile.m_ProfileMean[icol] += input.getMeasurement(irow, m_ColumnInfo[icol + iGroup * m_iGroupDims].m_iIndex);
				}
			}
		}
		for (int icol = 0; icol < m_iGroupDims; icol++)
		{
			profile.m_ProfileMean[icol] /= profile.m_iCount;
		}
		
		// compute standard deviation:
		for (int irow = 0; irow < input.rows(); irow++)
		{
			if (bActiveRows == null || bActiveRows[irow])
			{
				for (int icol = 0; icol < m_iGroupDims; icol++)
				{
					double diff = profile.m_ProfileMean[icol] - input.getMeasurement(irow, m_ColumnInfo[icol + iGroup * m_iGroupDims].m_iIndex);
					profile.m_ProfileStdDev[icol] += diff * diff;
				}
			}
		}
		for (int icol = 0; icol < m_iGroupDims; icol++)
		{
			profile.m_ProfileStdDev[icol] = Math.sqrt(profile.m_ProfileStdDev[icol] / profile.m_iCount);
		}
		
		return profile;
	}

	/* ***********************************************
	 * 				Similarity Calculation
	 * ***********************************************/
	
	public double calcGroupSimilarity(int g1, int g2)
	{
		if (m_GroupSimilarityInfo == null)
			return 0;
		
		GroupInfo.Clustering gic1 = getCurrGroupInfoClustering(g1);
		GroupInfo.Clustering gic2 = getCurrGroupInfoClustering(g2);
		if (gic1 == null || gic2 == null)
			return 0;
		
		
		ClusterSimilarityInfo gsi = m_GroupSimilarityInfo[g1][g2];
		
		int N1 = gsi.m_Similarity != null ? gsi.m_Similarity.length : 0;
		int N2 = N1 > 0 && gsi.m_Similarity[0] != null ? gsi.m_Similarity[0].length : 0;
		if (N1 == 0 || N2 == 0)
			return 0;
		
		if (m_SimilarityMetric == SimilarityMetric.FMEASURE)
		{
			double dPrecisionSum = 0; // used in f-measure
			double dPrecisionSum1 = 0;
			double dPrecisionSum2 = 0;
			
			double dB = 1.0; // (Beta in the F_B Measure)
			
			for (int c1 = 0; c1 < N1; c1++)
			{
				double dMaxPrecision = 0;
				for (int c2 = 0; c2 < N2; c2++)
				{
					gsi.m_Similarity[c1][c2] = (1 + dB*dB) * gsi.m_Count[c1][c2] / (dB * dB * gic1.m_ClusterSize[c1] + gic2.m_ClusterSize[c2]);
					dPrecisionSum += gsi.m_Similarity[c1][c2];
					dMaxPrecision = Math.max(dMaxPrecision, gsi.m_Similarity[c1][c2]);
				}
				dPrecisionSum1 += dMaxPrecision;
			}
			
			for (int c2 = 0; c2 < N2; c2++)
			{
				double dMaxPrecision = 0;
				for (int c1 = 0; c1 < N1; c1++)
				{
					dMaxPrecision = Math.max(dMaxPrecision, gsi.m_Similarity[c1][c2]);
				}
				dPrecisionSum2 += dMaxPrecision;
			}
			
			//return dPrecisionSum / (N1 * N2);
			//return dPrecisionSum1 / (N1);
			return Math.max(dPrecisionSum1/N1, dPrecisionSum2/N2);
			//return (dPrecisionSum1/N1 + dPrecisionSum2/N2) / 2;
			//return Math.max(dPrecisionSum1/N1, dPrecisionSum2/N2) - (dPrecisionSum1/N1 + dPrecisionSum2/N2) / 2;
		}
		else if (m_SimilarityMetric == SimilarityMetric.HEURISTIC)
		{
			double dColSim = 0;
			for (int c1 = 0; c1 < N1; c1++)
			{
				double dMax = 0;
				double dSum = 0;
				for (int c2 = 0; c2 < N2; c2++)
				{
					gsi.m_Similarity[c1][c2] = 1.0 * gsi.m_Count[c1][c2] / input.rows();
					
					dMax = Math.max(gsi.m_Similarity[c1][c2], dMax);
					dSum += gsi.m_Similarity[c1][c2];
				}
				dColSim += N2 > 1 ? (dMax - (dSum - dMax) / (N2 - 1)) : dMax; // maxX - avg(x!=maxX)
			}
			double dRowSim = 0;
			for (int c2 = 0; c2 < N2; c2++)
			{
				double dMax = 0;
				double dSum = 0;
				for (int c1 = 0; c1 < N1; c1++)
				{
					dMax = Math.max(gsi.m_Similarity[c1][c2], dMax);
					dSum += gsi.m_Similarity[c1][c2];
				}
				dRowSim += N1 > 1 ? (dMax - (dSum - dMax) / (N1 - 1)) : dMax;
			}
			//return (dColSim + dRowSim) / 2.0;
			return Math.max(dColSim, dRowSim);
		}
		
		return 0;
	}
	
	ClusterSimilarityInfo calcClusteringSimilarities(GroupInfo.Clustering gic1, GroupInfo.Clustering gic2)
	{
		// initialize
		ClusterSimilarityInfo result = new ClusterSimilarityInfo();
		result.m_Similarity = new double[gic1.m_iNumClusters][gic2.m_iNumClusters];
		result.m_Count = new int[gic1.m_iNumClusters][gic2.m_iNumClusters];
		
		double dOneRow = 1.0/input.rows();
		for (int irow = 0; irow < input.rows(); irow++)
		{
			int c1 = gic1.m_ClusterID[irow];
			int c2 = gic2.m_ClusterID[irow];
			result.m_Similarity[c1][c2] += dOneRow;
			result.m_Count[c1][c2]++;
		}

		return result;
	}
	
	void calcAllClusterSimilarity()
	{
		m_GroupSimilarityInfo = new ClusterSimilarityInfo[m_iNumGroups][m_iNumGroups];
		for (int g1 = 0; g1 < m_iNumGroups; g1++)
		{
			GroupInfo.Clustering gic1 = getCurrGroupInfoClustering(g1);
			for (int g2 = g1; g2 < m_iNumGroups; g2++)
			{
				GroupInfo.Clustering gic2 = getCurrGroupInfoClustering(g2);
				m_GroupSimilarityInfo[g1][g2] = calcClusteringSimilarities(gic1, gic2);
			}
		}
		/*
		// initialize
		for (int g1 = 0; g1 < m_iNumGroups; g1++)
		{
			GroupInfo.Clustering gic1 = getCurrGroupInfoClustering(g1);
			int iNum1 = gic1.m_iNumClusters;
			for (int g2 = g1; g2 < m_iNumGroups; g2++)
			{
				GroupInfo.Clustering gic2 = getCurrGroupInfoClustering(g2);

				m_GroupSimilarityInfo[g1][g2] = new ClusterSimilarityInfo();
				int iNum2 = gic2.m_iNumClusters;
				m_GroupSimilarityInfo[g1][g2].m_Similarity = new double[iNum1][iNum2];
				m_GroupSimilarityInfo[g1][g2].m_Count = new int[iNum1][iNum2];
			}
		}
		
		double dOneRow = 1.0/input.rows();
		for (int irow = 0; irow < input.rows(); irow++)
		{
			for (int g1 = 0; g1 < m_iNumGroups; g1++)
			{
				GroupInfo.Clustering gic1 = getCurrGroupInfoClustering(g1);
				int c1 = gic1.m_ClusterID[irow];
				for (int g2 = g1; g2 < m_iNumGroups; g2++)
				{
					GroupInfo.Clustering gic2 = getCurrGroupInfoClustering(g2);
					int c2 = gic2.m_ClusterID[irow];
					m_GroupSimilarityInfo[g1][g2].m_Similarity[c1][c2] += dOneRow;
					m_GroupSimilarityInfo[g1][g2].m_Count[c1][c2]++;
				}
			}
		}
		*/
	}
	
	public void selectCluster(int g, int c, boolean bAdd, boolean bRemove)
	{
		int iSelectionCol = getSelectionCol();
		if (iSelectionCol == -1)
		{// no selection column
			return;
		}
		
		GroupInfo.Clustering gic = getCurrGroupInfoClustering(g);
		if (gic == null)
			return;
		
		for (int irow = 0; irow < input.rows(); irow++)
		{
			if (c == gic.m_ClusterID[irow])
			{
				input.setMeasurement(irow, iSelectionCol, bRemove ? 0 : 1);				
			}
			else
			{
				if (!bAdd && !bRemove)
				{
					input.setMeasurement(irow, iSelectionCol, 0);
				}
			}
		}
	}

	public void groupClusterSelection(int g1, int g2, int c1, int c2)
	{
		int iSelectionCol = getSelectionCol();
		if (iSelectionCol == -1)
		{// no selection column
			return;
		}
		
		GroupInfo.Clustering gic1 = getCurrGroupInfoClustering(g1);
		GroupInfo.Clustering gic2 = getCurrGroupInfoClustering(g2);

		for (int irow = 0; irow < input.rows(); irow++)
		{
			if (c1 == gic1.m_ClusterID[irow] &&
				c2 == gic2.m_ClusterID[irow])
			{
				input.setMeasurement(irow, iSelectionCol, 1);				
			}
			else
			{
				input.setMeasurement(irow, iSelectionCol, 0);				
			}
		}
	}
	
	public String getGroupName(int iGroup)
	{// hack: find a common group name by comparing the first two columns of a group
		if (m_iGroupDims == 1)
		{
			return input.getColName(iGroup);
		}
		String sCommon = "[" + Integer.toString(iGroup) + "]";
		int col1 = iGroup * m_iGroupDims;
		if (col1 + m_iGroupDims - 1 < input.columns())
		{
			int c = 0;
			String sCol1 = input.getColName(col1);
			String sCol2 = input.getColName(col1 + m_iGroupDims - 1);
			while (sCol1.length() < c && sCol2.length() < c && sCol1.charAt(c) == sCol2.charAt(c))
			{
				c++;
			}
			if (c > 0)
			{
				sCommon = sCol1.substring(0, c);
			}
		}
		return sCommon;
	}
	

	/********************************************************************************
	 * 		Cluster Combinations
	 ********************************************************************************/
	
	public class ClusterCombinationInfo
	{
		public int[] 		m_GroupsNumClusters; 	// [m_iNumGroups]: number of clusters per group
		public int[]		m_CombinationsIndex;	// [rows()] combination index for each row
		public double[] 	m_CombinationsSize;  	// [num_combinations]: size of each combination
		public int[] 		m_SortedIndices; 		// [num_combinations]: combination indices sorted by size from largest to smallest
		public boolean[]	m_CombinationsSelected;	// [num_combinations]: selected combinations
		public int			m_iNumNoneZero; 		// total number of nonezero combinations
		public double[] 	m_HistNumClusters;		// [max_combination_size + 1] histogram for number of clusters per combination size
		public double[] 	m_HistNumPoints;		// [max_combination_size + 1] histogram for number of poinsts per combination size
	}
	
	public ClusterCombinationInfo m_CombinationInfo = null;
	
	public void computeClusterCombinations()
	{
		m_CombinationInfo = new ClusterCombinationInfo();
		
		int iNumCombinations = 1;
		
		m_CombinationInfo.m_GroupsNumClusters = new int[m_iNumGroups];
		GroupInfo.Clustering[] gic = new GroupInfo.Clustering[m_iNumGroups];
		boolean[] bIsGroupSelected = new boolean[m_iNumGroups];
		for (int g = 0; g < m_iNumGroups; g++)
		{
			gic[g] = getCurrGroupInfoClustering(g);
			m_CombinationInfo.m_GroupsNumClusters[g] = gic[g].m_iNumClusters;
			if (getGroupInfo(g).m_bIsSelected)
			{
				bIsGroupSelected[g] = true;
				iNumCombinations *= gic[g].m_iNumClusters;
			}
		}
		
		// compute the size of each combination
		m_CombinationInfo.m_CombinationsSize = new double[iNumCombinations];
		m_CombinationInfo.m_CombinationsSelected = new boolean[iNumCombinations];
		Arrays.fill(m_CombinationInfo.m_CombinationsSize, 0);
		m_CombinationInfo.m_iNumNoneZero = 0;// number of none-zero combinations
		m_CombinationInfo.m_CombinationsIndex = new int[rows()];
		for (int i = 0; i < rows(); i++)
		{
			// compute combination index
			int iCombIndex = 0;
			for (int g = 0; g < m_iNumGroups; g++)
			{
				if (bIsGroupSelected[g])
				{
					iCombIndex = iCombIndex * m_CombinationInfo.m_GroupsNumClusters[g] + gic[g].m_ClusterID[i];
				}
			}
			
			m_CombinationInfo.m_CombinationsIndex[i] = iCombIndex;
			//assert(iCombIndex < m_CombinationInfo.m_CombinationsSize.length);
			if (m_CombinationInfo.m_CombinationsSize[iCombIndex] == 0)
				m_CombinationInfo.m_iNumNoneZero++;
			m_CombinationInfo.m_CombinationsSize[iCombIndex]++;
		}
		
		// now sort high to low
		m_CombinationInfo.m_SortedIndices = FloatIndexer.sortFloatsRev(m_CombinationInfo.m_CombinationsSize);
		
		int iMaxSize = (int)m_CombinationInfo.m_CombinationsSize[m_CombinationInfo.m_SortedIndices[0]];
		m_CombinationInfo.m_HistNumClusters = new double[iMaxSize+1];
		m_CombinationInfo.m_HistNumPoints   = new double[iMaxSize+1];
		//m_CombinationInfo.m_HistNumClusters[0] = m_CombinationInfo.m_CombinationsSize.length - m_CombinationInfo.m_iNumNoneZero;
		for (int i = 0; i < m_CombinationInfo.m_iNumNoneZero; i++)
		{
			int iSize = (int)m_CombinationInfo.m_CombinationsSize[m_CombinationInfo.m_SortedIndices[i]];
			m_CombinationInfo.m_HistNumClusters[iSize]++;
			m_CombinationInfo.m_HistNumPoints[iSize]+=iSize; 
		}
	}
	
	
	
	/********************************************************************************
	 * 		HeatMapView
	 ********************************************************************************/

	public class HeatMapView extends OperatorView implements ChangeListener, ListSelectionListener
	{
		private static final long serialVersionUID = -5919900799213575720L;

		PHeatMapPainter m_HeatMapPainter = null;
		//JCheckBox m_CheckBoxMetaData = null;
		
		HeatMapOp m_Operator;
		
		JList m_ListCols = null;
		JPanel m_PanelMain = null;
		JComboBox m_ComboSortType  = null;
		JTextField m_TextGroupDims = null;
		JTextField m_TextNumGroups = null;
		JTextField m_TextSetValue = null;
		
		public HeatMapView(Operator op)
		{
			super(op);
			m_Operator = (HeatMapOp) op;
			m_HeatMapPainter = new PHeatMapPainter(op);
			vframe = new OPAppletViewFrame("E"+op.getExpressionNumber()+":"+op, m_HeatMapPainter );			
			vframe.addComponentListener(this);
			m_PanelMain = new JPanel(new BorderLayout(5,5));
			this.setLayout(new BorderLayout(5,5));
			this.add(m_PanelMain, BorderLayout.CENTER);
			this.setBorder(	BorderFactory.createEmptyBorder(10, 10, 10, 10));
			buildGUI();
		}
		
		
		void buildGUI()
		{
			m_PanelMain.removeAll();
//			JPanel  panelMain = null;
//			panelMain = new JPanel();
//			this.add(panelMain, BorderLayout.CENTER);

			DefaultListModel listModelSelected = new DefaultListModel();
			for (int i = 0; i < m_Operator.input.columns(); i++ )
			{
				listModelSelected.addElement(Integer.toString(i));
			}
			
			m_ListCols = new JList(listModelSelected);
			m_ListCols.addListSelectionListener(this);
			updateListCols();
			
			JButton buttonSort = new JButton("Sort Selected By:");
			buttonSort.setActionCommand("SORT");
			buttonSort.addActionListener(this);

			JButton buttonMask = new JButton("Toggle Type");
			buttonMask.setActionCommand("MASK");
			buttonMask.addActionListener(this);

			JButton buttonReset = new JButton("Reset");
			buttonReset.setActionCommand("RESET");
			buttonReset.addActionListener(this);

			JButton buttonCluster = new JButton("Cluster 0's");
			buttonCluster.setActionCommand("CLUSTER");
			buttonCluster.addActionListener(this);
			
			JButton buttonNumGroups = new JButton("Num Groups:");
			buttonNumGroups.setActionCommand("NumGroups");
			buttonNumGroups.addActionListener(this);

			JButton buttonGroupDims = new JButton("Group Dimension:");
			buttonGroupDims.setActionCommand("GroupDims");
			buttonGroupDims.addActionListener(this);

			JButton buttonSetValue = new JButton("Set Value:");
			buttonSetValue.setActionCommand("SETVALUE");
			buttonSetValue.addActionListener(this);

			m_TextGroupDims = new JTextField();
			m_TextGroupDims.setText(Integer.toString(m_Operator.m_iGroupDims));

			m_TextNumGroups = new JTextField();
			m_TextNumGroups.setText(Integer.toString(m_Operator.m_iNumGroups));
			
			m_TextSetValue = new JTextField();
			m_TextSetValue.setText("0");
			
			JPanel panelButtons = new JPanel(new GridLayout(6, 2));
			m_ComboSortType  = EnumUtils.getComboBox(GroupSortType.values(), m_GroupSortType, "sort selection by:", this);
			
			panelButtons.add(buttonSort);
			panelButtons.add(m_ComboSortType);
			
			panelButtons.add(buttonNumGroups);
			panelButtons.add(m_TextNumGroups);

			panelButtons.add(buttonGroupDims);
			panelButtons.add(m_TextGroupDims);

			panelButtons.add(buttonSetValue);
			panelButtons.add(m_TextSetValue);
			
			panelButtons.add(buttonMask);
			panelButtons.add(buttonReset);
			panelButtons.add(buttonCluster);

			m_PanelMain.add(new JScrollPane(m_ListCols), BorderLayout.CENTER);
			m_PanelMain.add(panelButtons, BorderLayout.EAST);
		}
		
		void updateListCols()
		{
			for (int i = 0; i < m_Operator.input.columns(); i++ )
			{
				((DefaultListModel) m_ListCols.getModel()).set(i, 
						"[" + Integer.toString(m_Operator.m_ColumnInfo[i].m_iIndex) + "] " +
						m_Operator.getColName(i) + " : " +
						m_Operator.getColType(i).toString());
			}
		}
		
		public void actionPerformed(ActionEvent e)
		{
			if( e.getActionCommand().equalsIgnoreCase("SORT") )
			{
				m_Operator.sortRowsBySelectedColumns(m_GroupSortType);
				//m_Operator.tableChanged( new TableEvent(m_Operator, TableEvent.TableEventType.TABLE_CHANGED ), true);
			}
			else if( e.getActionCommand().equalsIgnoreCase("MASK") )
			{
				m_Operator.maskSelectedColumns();
				updateListCols();
				//m_Operator.tableChanged( new TableEvent(m_Operator, TableEvent.TableEventType.TABLE_CHANGED ), true);
			}
			else if( e.getActionCommand().equalsIgnoreCase("RESET") )
			{
				m_Operator.m_ColumnInfo = null;
				m_Operator.m_SortedIndex = null;
				m_Operator.initColumnIndex();
				m_Operator.initGroupClusters();
				m_Operator.tableChanged( new TableEvent(m_Operator, TableEvent.TableEventType.TABLE_CHANGED ), true);
				m_Operator.setSelectedCols(m_ListCols.getSelectedIndices());
				updateListCols();
			}
			else if( e.getActionCommand().equalsIgnoreCase("CLUSTER") )
			{
				initGroupClusters();
				clusterGroup(0, 2);
				clusterGroup(1, 2);
				clusterGroup(2, 2);
				clusterGroup(3, 2);
				clusterGroup(4, 2);
				clusterGroup(5, 2);
				clusterGroup(6, 2);
				clusterGroup(7, 2);
				calcAllClusterSimilarity();
			}
			else if( e.getActionCommand().equalsIgnoreCase("GroupDims") )
			{
				m_Operator.m_iGroupDims = Integer.parseInt(m_TextGroupDims.getText());
				m_Operator.invokeRedraw(true);
			}
			else if( e.getActionCommand().equalsIgnoreCase("NumGroups") )
			{
				m_Operator.m_iNumGroups = Integer.parseInt(m_TextNumGroups.getText());
				m_Operator.invokeRedraw(true);
			}
			else if( e.getActionCommand().equalsIgnoreCase("SETVALUE") )
			{
				setSelectedValue(Double.parseDouble(m_TextSetValue.getText()));
				m_Operator.invokeRedraw(true);
			}
			else if (e.getSource() instanceof JComboBox)
			{
				m_GroupSortType = GroupSortType.values()[m_ComboSortType.getSelectedIndex()];
			}
		}

		@Override
		public void stateChanged(ChangeEvent e)
		{
		}

		@Override
		public void valueChanged(ListSelectionEvent e)
		{
			m_Operator.setSelectedCols(m_ListCols.getSelectedIndices());
			//int [] m_SelectedIndices = m_ListCols.getSelectedIndices();
			//m_ListCols.setSelectedIndices(m_SelectedIndices);
		}
	}
}
