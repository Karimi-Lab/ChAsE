package org.sfu.chase;

import java.util.ArrayList;

public class FavoriteList
{
    private ArrayList<Favorite>	m_Favorites; // the root node is always m_Favorites[0], and can not be removed
	
    private ClustFramework m_Framework;
	
    int m_FavoriteIndex = 0;
	
	FavoriteList(ClustFramework framework)
	{
		m_Framework = framework;
    	m_Favorites = new ArrayList<Favorite>();
	}
	public int size()
	{
		return m_Favorites.size();
	}
	
	public void add(ClustInfo cInfo, boolean createCopy)
	{
		ClustInfo newClust = cInfo;
		if (createCopy)
		{
			newClust = new ClustInfo();
			newClust.m_Indices = cInfo.m_Indices;
	    	if (cInfo.m_InfoLabel != null)
	    		newClust.m_InfoLabel = new String(cInfo.m_InfoLabel);
			m_Framework.calcClustStats(newClust);
		}
		Favorite fav = new Favorite();
		fav.setClustInfo(newClust);
		m_Favorites.add(fav);
	}
	
	public void createNew(int[] indices)
	{
		ClustInfo newClust = new ClustInfo();
		newClust.m_Indices = indices;
		m_Framework.calcClustStats(newClust);
		Favorite fav = new Favorite();
		fav.setClustInfo(newClust);
		m_Favorites.add(fav);
	}
	
	public Favorite get(int index)
	{
		return m_Favorites.get(index);
	}
	
	public int getIndexOf(ClustInfo cInfo)
	{
		for (int i = 0; i < m_Favorites.size(); i++)
		{
			if (m_Favorites.get(i).getClustInfo() == cInfo)
				return i;
		}
		return -1;
	}
	
	public int getNumSelected()
	{
		int numSelected = 0;
		for (int i = 0; i < m_Favorites.size(); i++)
			numSelected += m_Favorites.get(i).isSelected() ? 1 : 0;
		return numSelected;
	}

	public Favorite[] getSelected()
	{
		int numSelected = getNumSelected();
		if (numSelected == 0)
			return null;
		
		Favorite[] selectedFavorites = new Favorite[numSelected];
		int currIndex = 0;
		for (int i = 0; i < m_Favorites.size(); i++)
		{
			if (m_Favorites.get(i).isSelected())
				selectedFavorites[currIndex++] = m_Favorites.get(i);
		}
		return selectedFavorites;
	}

	public ClustInfo[] getSelectedClusters()
	{
		Favorite[] selectedFavorites = getSelected();
		if (selectedFavorites == null)
			return null;

		ClustInfo[] selectedClusters = new ClustInfo[selectedFavorites.length];
		for (int i = 0; i < selectedFavorites.length; i++)
			selectedClusters[i] = selectedFavorites[i].getClustInfo();
		
		return selectedClusters;
	}
	
	public void select(int index)
	{
		m_FavoriteIndex = index < m_Favorites.size() ? index : 0;
	}
	
	public void remove(int index)
	{
		if (index == 0)
			return; // should not remove the main root node.
		
		m_Favorites.remove(index);
		if (m_FavoriteIndex == index)
			m_FavoriteIndex = 0;
		else if (m_FavoriteIndex > index)
			m_FavoriteIndex--;
	}
	
	public void clear()
	{
		m_Favorites.clear();
	}
}
