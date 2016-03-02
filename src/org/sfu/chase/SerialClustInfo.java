package org.sfu.chase;

import java.util.Arrays;

/**
 * Stores the minimal information required to store a ClustInfo object
 */
public class SerialClustInfo extends Object
{
	private boolean m_bShowChildren = true;
	private String	m_Title = "";    
	private String	m_InfoLabel = null;
    private SerialClustInfo m_Sibling = null;
    private SerialClustInfo m_Child = null;
	private int[] m_Indices = null;
	private int m_Size = 0;
    private boolean [] m_KmeansActiveGroups = null;

	public boolean isShowChildren() {
		return m_bShowChildren;
	}

	public void setShowChildren(boolean showChildren) {
		m_bShowChildren = showChildren;
	}

	public String getTitle() {
		return m_Title;
	}

	public void setTitle(String title) {
		m_Title = title;
	}

	public String getInfoLabel() {
		return m_InfoLabel;
	}

	public void setInfoLabel(String infoLabel) {
		m_InfoLabel = infoLabel;
	}

	public SerialClustInfo getSibling() {
		return m_Sibling;
	}

	public void setSibling(SerialClustInfo sibling) {
		m_Sibling = sibling;
	}

	public SerialClustInfo getChild() {
		return m_Child;
	}

	public void setChild(SerialClustInfo child) {
		m_Child = child;
	}

	public int[] getIndices() {
		return m_Indices;
	}

	public void setIndices(int[] indices) {
		m_Indices = indices;
	}

	public int getSize() {
		return m_Size;
	}

	public void setSize(int size) {
		m_Size = size;
	}

    public boolean [] getKmeansActiveGroups() {
        return m_KmeansActiveGroups;
    }

    public void setKmeansActiveGroups(boolean [] kmeansActiveGroup) {
        m_KmeansActiveGroups = kmeansActiveGroup;
    }

    SerialClustInfo()
	{
	}
	
	SerialClustInfo(ClustInfo cInfo)
	{
		copyFrom(cInfo);
	}
	
	ClustInfo createClustInfo()
	{
		ClustInfo cInfo = new ClustInfo();
		copyTo(cInfo);
		return cInfo;
	}
	
	void copyFrom(ClustInfo cInfo)
	{
		m_bShowChildren = cInfo.m_bShowChildren;
		m_Title = cInfo.getTitle();
		m_InfoLabel = cInfo.m_InfoLabel;
		
		if (cInfo.m_Child == null) {
			m_Indices = cInfo.m_Indices; // only store indices for the leaf nodes
			m_Child = null;
		} else {
			m_Indices = null;
			m_Child = new SerialClustInfo(cInfo.m_Child);
			ClustInfo childInfo = cInfo.m_Child;
			SerialClustInfo childSInfo = m_Child;
			while (childInfo.m_Sibling != null)
			{
				childSInfo.m_Sibling = new SerialClustInfo(childInfo.m_Sibling);
				childInfo = childInfo.m_Sibling;
				childSInfo = childSInfo.m_Sibling;
			}
		}
		m_Size = cInfo.size();
		
		m_KmeansActiveGroups = cInfo.m_KmeansActiveGroups;
	}
	
	void copyTo(ClustInfo cInfo)
	{
		cInfo.m_bShowChildren = m_bShowChildren;
		cInfo.setTitle(m_Title);
		cInfo.m_InfoLabel = m_InfoLabel;

		if (m_Child == null) {
			cInfo.m_Indices = m_Indices;
			cInfo.m_Child = null;
		} else {
			cInfo.m_Child = m_Child.createClustInfo();
			ClustInfo childInfo = cInfo.m_Child;
			SerialClustInfo childSInfo = m_Child;

			cInfo.m_Indices = new int[m_Size];
			int indexOffset = 0;
			while (childSInfo != null) {
				System.arraycopy(childInfo.m_Indices, 0, cInfo.m_Indices, indexOffset, childInfo.m_Indices.length);
				indexOffset += childInfo.m_Indices.length;
				childInfo.m_Parent = cInfo;
				
				childSInfo = childSInfo.m_Sibling;
				if (childSInfo != null) {
					childInfo.m_Sibling = childSInfo.createClustInfo();
					childInfo = childInfo.m_Sibling;
				}
			}
			Arrays.sort(cInfo.m_Indices);
		}
		
		if (m_KmeansActiveGroups != null) {
		    cInfo.m_KmeansActiveGroups = m_KmeansActiveGroups;
		}
		
	}
}
