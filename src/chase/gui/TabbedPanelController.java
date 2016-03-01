package chase.gui;

import java.util.ArrayList;

import controlP5.ControlP5;
import controlP5.PanelController;

public class TabbedPanelController extends PanelController
{
	int btnX = 0;
	int btnY = 0;
	int btnW = 60;
	int btnH = 20;

	ArrayList<PanelController> m_Controllers;
	
	int m_VisibleIndex = -1;
	
	public TabbedPanelController(ControlP5 theControlP5, String theName,
			int theX, int theY, int theWidth, int theHeight) {
		super(theControlP5, theName, theX, theY, theWidth, theHeight);
		setColorBackground(0x01000000); // transparent background
		m_Controllers = new ArrayList<PanelController>();
	}
	
	public void addPanel(PanelController panel)
	{
		m_Controllers.add(panel);
		if (m_Controllers.size() > 1)
		{
			panel.setVisible(false);
		}
	}
	
	public void setVisibleTab(int iIndex)
	{
		m_VisibleIndex = iIndex;
		for (int i = 0; i < m_Controllers.size(); i++)
		{
			m_Controllers.get(i).setVisible(i == iIndex);
		}
	}
	
	public void setVisibleTab(String name)
	{
		for (int i = 0; i < m_Controllers.size(); i++)
		{
			if (m_Controllers.get(i).name().equals(name))
			{
				setVisibleTab(i);
				break;
			}
		}
	}

	public int getVisibleTabIndex()
	{
		return m_VisibleIndex;
	}
	
	public int getNumPanels()
	{
		return m_Controllers.size();
	}
	
	public PanelController getPanel(int index)
	{
		if (index >= 0 && index < m_Controllers.size())
			return m_Controllers.get(index);
		return null;
	}
	
	public PanelController getVisiblePanel()
	{
		return getPanel(m_VisibleIndex);
	}
	

	
}
