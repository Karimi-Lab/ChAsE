package chase;

import java.util.ArrayList;
import java.util.Arrays;

public class ClustInfo extends Object
{
    public int[]        m_Indices = null; // cluster member indices (rows)
    
    public ClustInfo    m_Child   = null; // first child
    public ClustInfo    m_Sibling = null; // right sibling
    public ClustInfo    m_Clone  = null;
    public ClustInfo	m_Parent  = null;
    public boolean		m_bShowChildren = true; // whether the cluster is collapsed or expanded
    private ClustStats  m_Stats   		= null; // stats for normalized values
    private ClustStats  m_UnNormStats   = null; // stats for unnormalized data values
    private String		m_Title   = "";
    
    String	    m_InfoLabel = null; // on/off information label icons to be shown above the cluster if it was created through on/off thresholding
	long  		m_ChildOnOffGroups = 0; // a bit field representing which groups have a threshold set
	int   		m_NumOnOffChildren = 0;
	int[] 		m_OnOffChildOrder = null;
	Threshold[] m_Thresholds = null;
	
	public static  int[] m_GroupOrder = null;
	boolean[]   m_KmeansActiveGroups = null; // which groups are active in the kmeans clustering
    
	public class Threshold
	{
		public boolean on = false;
		public boolean off = false;
		public float value = 0;
	}
   
    public void copyFrom(ClustInfo cInfo)
    {
    	m_Indices 	= cInfo.m_Indices;
    	m_Child 	= cInfo.m_Child;
    	//m_Sibling 	= cInfo.m_Sibling;
    	//m_Clone	= cInfo.m_Clone;
    	m_bShowChildren = cInfo.m_bShowChildren;
    	if (cInfo.m_InfoLabel != null)
    		m_InfoLabel = new String(cInfo.m_InfoLabel);
    	setStats(cInfo.getStats());
    	setUnNormStats(cInfo.getUnNormStats());
    }
    
    public int size()
    {
    	if (m_Indices != null)
    		return m_Indices.length;
    	return 0;
    }
    
    /**
     * Total number of visible leaf nodes under this children
     * @return
     */
    public int visibleLeaves()
    {
    	int count = 0;
    	if (m_bShowChildren && m_Child != null)
    	{
	    	for (ClustInfo childInfo = m_Child; childInfo != null; childInfo = childInfo.m_Sibling)
	    	{
	    		count += childInfo.visibleLeaves();
	    	}
    	}
    	else
    	{
    		count = 1;
    	}
    	
    	if (m_Clone != null)
    		count += m_Clone.visibleLeaves();
    	
    	return count;
    }
    
    public int visibleDepth()
    {
    	int depth = 0;
    	if (m_bShowChildren && m_Child != null)
    	{
	    	for (ClustInfo childInfo = m_Child; childInfo != null; childInfo = childInfo.m_Sibling)
	    	{
	    		depth = Math.max(depth, childInfo.visibleDepth() + 1);
	    	}
    	}
    	
    	if (m_Clone != null)
    		depth = Math.max(depth, m_Clone.visibleDepth());
    		
    	return depth;
    }
    
    public int getNumChildren()
    {
    	int numChildren = 0;
    	if (m_Child != null)
    	{
	    	for (ClustInfo childInfo = m_Child; childInfo != null; childInfo = childInfo.m_Sibling)
	    		numChildren++;
    	}
    	return numChildren;
    }
    
    public void deleteChildren()
    {
    	m_Child = null;
    	m_ChildOnOffGroups = 0;
    	m_NumOnOffChildren = 0;
    	m_OnOffChildOrder = null;
    	//m_Thresholds = null;
    }
    
    public ClustInfo[] getChildren()
    {
    	ClustInfo[] childArray = null;
    	int numChildren = getNumChildren();
    	if (numChildren > 0)
    	{
    		childArray = new ClustInfo[numChildren];
    		int index = 0;
	    	for (ClustInfo childInfo = m_Child; childInfo != null; childInfo = childInfo.m_Sibling)
	    		childArray[index++] = childInfo;
    	}
    	
    	return childArray;
    }
    
    // returns an array of references the children or the object itself if no children.
    public ClustInfo[] getVisibleNodes(boolean bRecursive)
    {
    	ArrayList<ClustInfo> list = new ArrayList<ClustInfo>();
    	if (m_Child == null || !m_bShowChildren)
    	{
			list.add(this);
    	}
    	else
    	{
	    	ClustInfo nextChild = m_Child;
	    	while (nextChild != null)
	    	{
	    		if (bRecursive)
	    		{
		    		ClustInfo[] childArray = nextChild.getVisibleNodes(bRecursive);
		    		for (int i = 0; i < childArray.length; ++i)
		    			list.add(childArray[i]);
	    		}
	    		else
	    		{
	    			list.add(nextChild);
	    		}
	    		nextChild = nextChild.m_Sibling;
	    	}
    	}
    	
    	if (bRecursive && m_Clone != null)
    	{
    		ClustInfo[] cloneArray = m_Clone.getVisibleNodes(bRecursive);
    		for (int i = 0; i < cloneArray.length; ++i)
    			list.add(cloneArray[i]);
    	}

    	ClustInfo[] a = new ClustInfo[list.size()];
    	return list.toArray(a);
    }
    
    public void initThresholds(int numGroups)
    {
    	m_Thresholds = new Threshold[numGroups];
    	
    	for (int i = 0; i < m_Thresholds.length; ++i)
    		m_Thresholds[i] = new Threshold();
    }
    
    public int getNumThresholds()
    {
    	return m_Thresholds != null ? m_Thresholds.length : 0;
    }
    
    public Threshold getThreshold(int index)
    {
    	if (m_Thresholds != null && index >= 0 && index < m_Thresholds.length)
    		return m_Thresholds[m_GroupOrder[index]];
    	return null;
    }
    
    public int getNumInfoLabels()
    {
    	return m_InfoLabel != null ? m_InfoLabel.length() : 0;
    }
    
    public char getInfoLabel(int index)
    {
    	return m_InfoLabel != null ? m_InfoLabel.charAt(index) : ' ';
    }
    
    public int getNumKmeansActiveGroups()
    {
    	return m_KmeansActiveGroups != null ? m_KmeansActiveGroups.length : 0;
    }
    
    public void initKmeansActiveGroups(int numGroups)
    {
		m_KmeansActiveGroups = new boolean[numGroups];
		Arrays.fill(m_KmeansActiveGroups, true);
    }
    
    public boolean getKmeansActiveGroup(int index)
    {
    	if (index >= 0 && index < m_GroupOrder.length && m_GroupOrder[index] < m_KmeansActiveGroups.length)
    		return m_KmeansActiveGroups[m_GroupOrder[index]];
    	return false;
    }
    
    public void setKmeansActiveGroup(int index, boolean enabled)
    {
    	m_KmeansActiveGroups[m_GroupOrder[index]] = enabled;
    }
/*
    public void swapGroups(int g1, int g2, boolean bRecursive)
    {

    	if (m_Thresholds != null && g1 >= 0 && g2 >= 0 && g1 < m_Thresholds.length && g2 < m_Thresholds.length)
    	{
    		Threshold tmp = m_Thresholds[g1];
    		m_Thresholds[g1] = m_Thresholds[g2];
    		m_Thresholds[g2] = tmp;
    	}
    	
    	if (m_InfoLabel != null && g1 >= 0 && g2 >= 0 && g1 < m_InfoLabel.length() && g2 < m_InfoLabel.length())
    	{
    		m_InfoLabel.get
    	}
    	
        public String	    m_InfoLabel = null; // on/off information label icons to be shown above the cluster if it was created through on/off thresholding
    	long  m_ChildOnOffGroups = 0; // a bit field representing which groups have a threshold set
    	int   m_NumOnOffChildren = 0;
    	int[] m_OnOffChildOrder = null;
    	public Threshold[]     m_Thresholds = null;
    	
    	public boolean[]   	   m_KmeansActiveGroups = null; // which groups are active in the kmeans clustering
    	
    	if (m_Clone != null)
    		m_Clone.swapGroups(g1, g2, bRecursive && m_Clone.m_Child != m_Child);
    	
    	if (bRecursive)
    	{
	    	for (ClustInfo nextChild = m_Child; nextChild != null; nextChild = nextChild.m_Sibling)
	    	{
	    		nextChild.swapGroups(g1, g2, bRecursive);
	    	}
    	}
    }
*/
    
    public void createClone()
    {
    	ClustInfo prevClone = m_Clone;
		m_Clone = new ClustInfo();
		m_Clone.copyFrom(this);
		m_Clone.m_Clone = prevClone;
    }

	public void setStats(ClustStats stats) {
		this.m_Stats = stats;
	}

	public ClustStats getStats() {
		return m_Stats;
	}

	public void setUnNormStats(ClustStats stats) {
		this.m_UnNormStats = stats;
	}

	public ClustStats getUnNormStats() {
		return m_UnNormStats;
	}

	public void setTitle(String m_Title) {
		this.m_Title = m_Title;
	}

	public String getTitle() {
		return m_Title;
	}
}
