package org.sfu.chase;

public class Favorite 
{
	private ClustInfo m_ClustInfo;
	private boolean	  m_bSelected = false;
	
	public void setClustInfo(ClustInfo clustInfo) {
		m_ClustInfo = clustInfo;
	}
	
	public ClustInfo getClustInfo() {
		return m_ClustInfo;
	}
	
	public void setSelected(boolean selected) {
		this.m_bSelected = selected;
	}
	
	public boolean isSelected() {
		return m_bSelected;
	}
	
}
