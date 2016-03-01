package chase.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;

import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.KeyStroke;

import chase.ClustFramework.SortCriteria;

public class PopupMenu 
{
	private JPopupMenu m_PopupHeatmap;
	private JPopupMenu m_PopupDetailPlot;
	private JPopupMenu m_PopupWorkspace;
	private JPopupMenu m_PopupFavorite;
	
	private PChasePainter m_ChasePainter;
	
	private JRadioButtonMenuItem[] m_ItemHMSortTypes;
	private JRadioButtonMenuItem[] m_ItemProfileTypes; 
	
	// detail plot
	private JCheckBoxMenuItem m_ItemSeparateCluster; 
    private JCheckBoxMenuItem m_ItemRPKM; 
	
	public PopupMenu(PChasePainter painter)
	{
		m_ChasePainter = painter;
		addPopUpMenus();
	}
	
	public void addPopUpMenus() 
	{	
		addPopupHeatmap();
		addPopupDetailPlot();
		addPopupWorkspace();
		addPopupFavorite();
	}
	
	JPopupMenu getPopupHeatmap()
	{
		return m_PopupHeatmap;
	}
	
	JPopupMenu getPopupDetailPlot()
	{
        m_ItemRPKM.setSelected(m_ChasePainter.m_ControlDetailPlot.m_bRPKM);
	    m_ItemSeparateCluster.setSelected(m_ChasePainter.m_ControlDetailPlot.m_bSeparateClusterPlots);
		return m_PopupDetailPlot;
	}
	
	JPopupMenu getPopupWorkspace()
	{
		return m_PopupWorkspace;
	}
	
	JPopupMenu getPopupFavorite()
	{
		return m_PopupFavorite;
	}
	
	private void addPopupHeatmap()
	{
		m_PopupHeatmap = new JPopupMenu();
		
		JMenu sortMenu = new JMenu("Sort by");
		sortMenu.setMnemonic(KeyEvent.VK_S);
		
		String criteriaName[] = {
				"Genomic Location",
				"Average",
				"Median",
				"Min",
				"Max Peak",
				"Peak Location",
				"Input Order",
				"Input Group Label"};
		
		ButtonGroup hmSortGroup = new ButtonGroup();
		m_ItemHMSortTypes = new JRadioButtonMenuItem[criteriaName.length];
		for (int i = 0; i < criteriaName.length; i++)
		{
			m_ItemHMSortTypes[i] = new JRadioButtonMenuItem(criteriaName[i]);
			final int i2 = i;
			m_ItemHMSortTypes[i].addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					m_ChasePainter.setHMSortCriteria((SortCriteria.values()[i2]));
				}
			});
			hmSortGroup.add(m_ItemHMSortTypes[i]);
			sortMenu.add(m_ItemHMSortTypes[i]);
		}
		m_ItemHMSortTypes[0].setSelected(true);
		
		m_PopupHeatmap.add(sortMenu);
		
		m_PopupHeatmap.addSeparator();
		
		JMenuItem createClusterItem = new JMenuItem("Add Selection to Workspace");
		createClusterItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				m_ChasePainter.createClusterFromSelection();
			}
		});
		m_PopupHeatmap.add(createClusterItem);

		JMenuItem exportHMClusterItem = new JMenuItem("Export Image...");
		exportHMClusterItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				m_ChasePainter.exportHeatmap();
			}
		});
		m_PopupHeatmap.add(exportHMClusterItem);
	}
	
	private void addPopupDetailPlot()
	{
		m_PopupDetailPlot = new JPopupMenu();
		
        m_ItemRPKM = new JCheckBoxMenuItem("Use RPKM");
        m_ItemRPKM.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                m_ChasePainter.m_ControlDetailPlot.setRPKM(m_ItemRPKM.isSelected());
                m_ChasePainter.invokeRedraw();
            }
        });
        m_PopupDetailPlot.add(m_ItemRPKM);

        m_ItemSeparateCluster = new JCheckBoxMenuItem("Separate Cluster Plots");
		m_ItemSeparateCluster.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                m_ChasePainter.m_ControlDetailPlot.setSeparateClusterPlots(m_ItemSeparateCluster.isSelected());
                m_ChasePainter.invokeRedraw();
            }
		});
		m_PopupDetailPlot.add(m_ItemSeparateCluster);
		
		m_PopupDetailPlot.addSeparator();
		
        JMenuItem itemExportPlot = new JMenuItem("Export Plot...");
        itemExportPlot.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                m_ChasePainter.exportDetailedPlot();
            }
        });
        m_PopupDetailPlot.add(itemExportPlot);
	}
	
	private void addPopupWorkspace() 
	{
		m_PopupWorkspace = new JPopupMenu();

		JMenu plotTypeMenu = new JMenu("Plots");
		
		JMenuItem zoomInItem = new JMenuItem("Bigger");
		zoomInItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				m_ChasePainter.zoomPlots(1);
			}
		});
		zoomInItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, 0));//ActionEvent. ALT_MASK));		
		plotTypeMenu.add(zoomInItem);
		
		JMenuItem zoomOutItem = new JMenuItem("Smaller");
		zoomOutItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				m_ChasePainter.zoomPlots(-1);
			}
		});
		zoomOutItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, 0));//ActionEvent. ALT_MASK));		
		plotTypeMenu.add(zoomOutItem);
		plotTypeMenu.addSeparator();
		
		//iconTypeMenu.setMnemonic(KeyEvent.VK_T);
		
		String profileTypeNames[] = {
				"Std-Dev",
				"Median",
				"Signal Scatter",
				"Peak Scatter"};
		
		String profileIcons[] = {
				"/resources/icon_profile_mean.png",
				"/resources/icon_profile_median.png",
				"/resources/icon_profile_signal.png",
				"/resources/icon_profile_peak.png"};
		
		ButtonGroup profileTypeGroup = new ButtonGroup();
		m_ItemProfileTypes = new JRadioButtonMenuItem[profileTypeNames.length];
		
		for (int i = 0; i < profileTypeNames.length; i++)
		{
			m_ItemProfileTypes[i] = new JRadioButtonMenuItem(profileTypeNames[i], new ImageIcon(getClass().getResource(profileIcons[i])));
			plotTypeMenu.add(m_ItemProfileTypes[i]);
			profileTypeGroup.add(m_ItemProfileTypes[i]);
			m_ItemProfileTypes[i].setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_1 + i, 0));
			final int i2 = i;
			m_ItemProfileTypes[i].addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					m_ChasePainter.setProfileType(i2);
				}
			});
		}
		m_ItemProfileTypes[0].setSelected(true);
		
		m_PopupWorkspace.add(plotTypeMenu);
		m_PopupWorkspace.addSeparator();
		
		
		JMenuItem addFavoriteItem = new JMenuItem("Add Cluster to Favorites");
		addFavoriteItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				m_ChasePainter.addClusterToFavorites();
			}
		});

		JMenuItem itemRemoveCluster = new JMenuItem("Remove Cluster");
		itemRemoveCluster.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				m_ChasePainter.removeClusterFromWorkspace();
			}
		});
		
		JMenuItem saveClusterItem = new JMenuItem("Export Regions in Cluster...");
		saveClusterItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				m_ChasePainter.saveSelectedCluster();
			}
		});
	
		JMenuItem showRegionsItem = new JMenuItem("Show Regions in Cluster...");
		showRegionsItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				m_ChasePainter.showRegionsDialog();
			}
		});
		
		JMenuItem editRegionTitleItem = new JMenuItem("Edit Cluster Title...");
		editRegionTitleItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				m_ChasePainter.editClusterTitle();
			}
		});
		
		m_PopupWorkspace.add(addFavoriteItem);
		m_PopupWorkspace.add(itemRemoveCluster);
		m_PopupWorkspace.addSeparator();
		m_PopupWorkspace.add(saveClusterItem);
		m_PopupWorkspace.add(showRegionsItem);
		m_PopupWorkspace.addSeparator();
		m_PopupWorkspace.add(editRegionTitleItem);
	}

	private void addPopupFavorite()
	{
		m_PopupFavorite = new JPopupMenu();
		
		JMenuItem addWorkspaceItem = new JMenuItem("Add to Workspace");
		addWorkspaceItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				m_ChasePainter.addFavoriteToWorkspace();
			}
		});
		m_PopupFavorite.add(addWorkspaceItem);
		
		JMenuItem removeFavorite = new JMenuItem("Remove Selected");
		removeFavorite.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				m_ChasePainter.removeFavorite();
			}
		});
		m_PopupFavorite.add(removeFavorite);
	}
	
	// updates the menu items based on the current state
	protected void updateMenuItems()
	{
		m_ItemHMSortTypes[m_ChasePainter.dp.hmSortCriteria.ordinal()].setSelected(true);
		m_ItemProfileTypes[m_ChasePainter.dp.plotType.ordinal()].setSelected(true);
	}
}