package org.sfu.chase.core;

import java.util.ArrayList;

public class SerialClustFramework extends Object
{
	public SerialClustInfo m_RootClustInfo = null;
	public ArrayList<SerialClustInfo> m_WorkingSet;
	public ArrayList<SerialClustInfo> m_Favorites;
	
	void copyFrom(ClustFramework framework)
	{
		m_RootClustInfo = new SerialClustInfo(framework.getRoot());

		m_WorkingSet = new ArrayList<SerialClustInfo>();
		for (int i = 1; i < framework.getWorkingSet().size(); i++) {// no need to keep a second copy of root
			m_WorkingSet.add(new SerialClustInfo(framework.getWorkingSet().get(i).getClustInfo()));
		}
		
		m_Favorites = new ArrayList<SerialClustInfo>();
		for (int i = 1; i < framework.getFavorites().size(); i++) {// no need to keep a second copy of root
			m_Favorites.add(new SerialClustInfo(framework.getFavorites().get(i).getClustInfo()));
		}
	}
	
	void copyTo(ClustFramework framework)
	{
		if (framework.getDataSize() != m_RootClustInfo.getSize()) {
			// invalid save file. will ignore for now. 
			//TODO: output any warnings?
			return;
		}
		
		framework.setRoot(m_RootClustInfo.createClustInfo());
		
		framework.getWorkingSet().clear();
		framework.getWorkingSet().add(framework.getRoot(), false);
		for (int i = 0; i < m_WorkingSet.size(); i++) {
			framework.getWorkingSet().add(m_WorkingSet.get(i).createClustInfo(), false);
		}
		
		framework.getFavorites().clear();
		framework.getFavorites().add(framework.getRoot(), false);
		for (int i = 0; i < m_Favorites.size(); i++) {
			framework.getFavorites().add(m_Favorites.get(i).createClustInfo(), false);
		}
		
		framework.majorUpdate();
	}
}
