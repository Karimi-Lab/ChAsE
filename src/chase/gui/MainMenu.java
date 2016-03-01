package chase.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.io.File;

import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.KeyStroke;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import chase.ChaseOp;
import chase.ClustInfo;
import chase.ClustFramework.SortCriteria;

public class MainMenu {

	protected JMenuBar m_MenuBar;

	
	// File menu
	protected JMenu 	m_MenuFile;
	protected JMenuItem m_ItemEdit; // Option to edit the current analysis
	protected JMenuItem m_ItemOpen; // Option to open an existing analysis directory
	protected JMenuItem m_ItemSave; //
	protected JMenuItem m_ItemQuit; // Option to quit the application
	
	// Edit menu
	protected JMenu 	m_MenuEdit;
	protected JMenuItem m_ItemUndo;
	protected JMenuItem m_ItemRedo;
	
	protected JMenu 	m_MenuWorkspace;
	protected JMenu 	m_MenuHeatmap;
    protected JMenu     m_MenuPlot;
	protected JMenu 	m_MenuMethod;
	
	// Workspace Menu
	protected JMenu     m_MenuCluster;
	protected JMenuItem m_ItemEditClusterTitle;
	protected JMenuItem m_ItemAddClusterToFavorites;
	protected JMenuItem m_ItemRemoveCluster;
	protected JMenuItem	m_ItemExportClusterRegions;
	protected JMenuItem	m_ItemViewRegions;
	
	protected JMenu     m_MenuProfilePlots;
	protected JRadioButtonMenuItem[] m_ItemProfileTypes;
//	protected JMenuItem m_ItemExportPlot;
	
	protected JMenu     m_MenuFavorites;
	protected JMenuItem m_ItemAddFavoriteToWorkspace;
	protected JMenuItem m_ItemRemoveFavorite;
	
	// Heat map
	protected JMenu		m_MenuHMSort;
	protected JMenuItem m_ItemHMReverseSort;
	protected JRadioButtonMenuItem[] m_ItemHMSortTypes;
	protected JMenuItem m_ItemHMCreateCluster;
	protected JMenuItem m_ItemHMExportImage;
	protected JMenuItem m_ItemHMExportRegions;
	
	//plot
	protected JCheckBoxMenuItem m_ItemSeparateCluster; 
	protected JCheckBoxMenuItem m_ItemRPKM; 
	
	
	//Method
	protected JMenuItem m_ItemMethodKmeans; 
	protected JMenuItem m_ItemMethodQuerySignal;
	protected JMenuItem m_ItemMethodComparison;
    protected JMenuItem m_ItemMethodClusterByGroup; 
	
//	protected JMenuItem closeItem;  // Option to close the current analysis
//	protected JMenuItem newItem;    // Option to start a new analysis
//	protected JMenuItem saveAsItem; // Option to save the current clusters;
//	protected JMenuItem exportItem; // Option to export image as PDF
//	protected JMenuItem helpItem;   // Option to view help documentations
//	protected JMenuItem viewExampleItem; // Option to view example data

	PChasePainter m_ChasePainter;
	
	public MainMenu(PChasePainter painter) 
	{
		m_ChasePainter = painter;
		
		m_MenuBar = new JMenuBar();
		
		addFileMenu();
		addEditMenu();
		addWorkspaceMenu();
		addHeatmapMenu();
		addPlotMenu();
		addMethodMenu();
		//addHelpMenu();
    }
	
	void addGenericMenuListener(JMenu menu)
	{
        menu.addMenuListener(new MenuListener() {
            @Override
            public void menuSelected(MenuEvent e) {
                updateMenuItems();
            }
            
            @Override
            public void menuDeselected(MenuEvent e) {
            }
            
            @Override
            public void menuCanceled(MenuEvent e) {
            }
        });
	}
	
	/*
	public void addCloseListener(ActionListener l) {
		closeItem.addActionListener(l);
	}
	
	public void addEditListener(ActionListener l) {
		editItem.addActionListener(l);
	}
	
	public void addNewAnalysisListener(ActionListener l) {
		newItem.addActionListener(l);
	}
	
	public void addOpenListener(ActionListener l) {
		openItem.addActionListener(l);
	}

	public void addSaveAsListener(ActionListener l) {
		saveAsItem.addActionListener(l);
	}
	
	public void addExportListener(ActionListener l) {
		exportItem.addActionListener(l);
	}
	
	public void addHelpListener(ActionListener l) {
		helpItem.addActionListener(l);
	}
	
	public void addQuitListener(ActionListener l) {
		quitItem.addActionListener(l);
	}
	
	public void addViewExampleListener(ActionListener l) {
		viewExampleItem.addActionListener(l);
	}

	public void enableSaveAsItem() {
		saveAsItem.setEnabled(true);
	}*/
	
	public JMenuBar getMenuBar() {
		return m_MenuBar;
	}
	
	/**
	 * Activate menu items after initial file loading
	 */
	protected void activateMenuItems() {
		// saveAsItem.setEnabled(true);
		// editItem.setEnabled(true);
		// dataItem.setEnabled(true);
	}
	
	protected void addEditMenu() {
		m_MenuEdit = new JMenu("Edit");
		
		m_ItemUndo = new JMenuItem("Undo");
		//itemUndo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		m_ItemUndo.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				m_ChasePainter.getFramework().getHistory().undo();
			}
		});
		m_MenuEdit.add(m_ItemUndo);
		
		m_ItemRedo = new JMenuItem("Redo");
		//itemRedo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Y, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		m_ItemRedo.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				m_ChasePainter.getFramework().getHistory().redo();
			}
		});
		m_MenuEdit.add(m_ItemRedo);
		
		m_MenuBar.add(m_MenuEdit);
		
		addGenericMenuListener(m_MenuEdit);
	}

	protected void addFileMenu() 
	{
		m_MenuFile = new JMenu("File");

//		newItem = new JMenuItem("New...");
//		fileMenu.add(newItem);
		
		m_ItemEdit = new JMenuItem("Edit Input Parameters...");
		m_MenuFile.add(m_ItemEdit);
		m_ItemEdit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				((ChaseOp)m_ChasePainter.getOp()).modifyInput();
			}
		});
		
		m_MenuFile.addSeparator();
		
		m_ItemOpen = new JMenuItem("Open...");
		m_MenuFile.add(m_ItemOpen);
		m_ItemOpen.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				((ChaseOp)m_ChasePainter.getOp()).openDialog();
			}
		});
		
		m_MenuFile.addSeparator();
		
		m_ItemSave = new JMenuItem("Save");
		m_MenuFile.add(m_ItemSave);
		m_ItemSave.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				((ChaseOp)m_ChasePainter.getOp()).saveWorkspace();
			}
		});
		
		m_MenuFile.addSeparator();
		
//		fileMenu.addSeparator();
//		
//		closeItem = new JMenuItem("Close");
//		fileMenu.add(closeItem);
//		
//		fileMenu.addSeparator();
//		
//		saveAsItem = new JMenuItem("Save As...");
//		// initially disabled
//		saveAsItem.setEnabled(false);
//		fileMenu.add(saveAsItem);
//		
//		fileMenu.addSeparator();

		m_ItemQuit = new JMenuItem("Quit");
		m_MenuFile.add(m_ItemQuit);
		m_ItemQuit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				System.exit(0);
			}
		});

		m_MenuBar.add(m_MenuFile);
		
        addGenericMenuListener(m_MenuFile);
	}

	protected JMenuItem createMenuItem(String text, JMenu parentMenu)
	{
		JMenuItem item = new JMenuItem(text);
		parentMenu.add(item);
		return item;
	}
	
	protected void addWorkspaceMenu()
	{
		m_MenuWorkspace = new JMenu("Workspace");
		m_MenuBar.add(m_MenuWorkspace);
		
		m_MenuCluster = new JMenu("Cluster");
		m_MenuWorkspace.add(m_MenuCluster);
		
		m_ItemEditClusterTitle = createMenuItem("Edit Title...", m_MenuCluster);
		m_ItemEditClusterTitle.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				m_ChasePainter.editClusterTitle();
			}
		});
		
		m_MenuCluster.addSeparator();
		
		m_ItemAddClusterToFavorites = createMenuItem("Add to Favorites", m_MenuCluster);
		m_ItemAddClusterToFavorites.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				m_ChasePainter.addClusterToFavorites();
			}
		});
		
		m_ItemRemoveCluster = createMenuItem("Remove from Workspace", m_MenuCluster);
		m_ItemRemoveCluster.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				m_ChasePainter.removeClusterFromWorkspace();
			}
		});
		
		m_MenuCluster.addSeparator();

		m_ItemExportClusterRegions = createMenuItem("Export Regions...", m_MenuCluster);
		m_ItemExportClusterRegions.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				m_ChasePainter.saveSelectedCluster();
			}
		});
		
		m_ItemViewRegions = createMenuItem("Show Regions...", m_MenuCluster);
		m_ItemViewRegions.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				m_ChasePainter.showRegionsDialog();
			}
		});

		JMenuItem itemSaveCSV = createMenuItem("Save Average...", m_MenuCluster);
		itemSaveCSV.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				m_ChasePainter.getFramework().saveAsCSV(m_ChasePainter.m_CDisplay.getAllSelectedClusters());
			}
		});

		m_MenuProfilePlots = new JMenu("Profile Plots");
		m_MenuWorkspace.add(m_MenuProfilePlots);
		
		JMenuItem itemProfileBigger = new JMenuItem("Bigger", new ImageIcon(getClass().getResource("/resources/icon_zoom_in.png")));
		m_MenuProfilePlots.add(itemProfileBigger);
		itemProfileBigger.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, 0));
		itemProfileBigger.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				m_ChasePainter.zoomPlots(1);
			}
		});
		
		JMenuItem itemProfileSmaller = new JMenuItem("Smaller", new ImageIcon(getClass().getResource("/resources/icon_zoom_out.png")));
		m_MenuProfilePlots.add(itemProfileSmaller);
		itemProfileSmaller.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, 0));
		itemProfileSmaller.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				m_ChasePainter.zoomPlots(-1);
			}
		});
		
		m_MenuProfilePlots.addSeparator();
		
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
			m_MenuProfilePlots.add(m_ItemProfileTypes[i]);
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

		m_MenuFavorites = new JMenu("Favorites");
		m_MenuWorkspace.add(m_MenuFavorites);
		
		m_ItemAddFavoriteToWorkspace = createMenuItem("Add Selected to Workspace", m_MenuFavorites);
		m_ItemAddFavoriteToWorkspace.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				m_ChasePainter.addFavoriteToWorkspace();
			}
		});
		
		m_ItemRemoveFavorite = createMenuItem("Remove Selected", m_MenuFavorites);
		m_ItemRemoveFavorite.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				m_ChasePainter.removeFavorite();
			}
		});
		
        addGenericMenuListener(m_MenuWorkspace);
	}
	
	protected void addHeatmapMenu()
	{
		m_MenuHeatmap = new JMenu("Heatmap");
		m_MenuBar.add(m_MenuHeatmap);

		m_MenuHMSort = new JMenu("Sort");
		m_MenuHeatmap.add(m_MenuHMSort);

		m_ItemHMReverseSort = createMenuItem("Reverse Sort", m_MenuHMSort);
		m_ItemHMReverseSort.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				m_ChasePainter.reverseHeatmapSort();
			}
		});

		m_MenuHMSort.addSeparator();
		
		String criteriaName[] = {
				"By Genomic Location",
				"By Average",
				"By Median",
				"By Min",
				"By Max Peak",
				"By Peak Location",
				"By Input Order",
				"By Input Group Label"};

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
			m_MenuHMSort.add(m_ItemHMSortTypes[i]);
		}
		m_ItemHMSortTypes[0].setSelected(true);

		m_MenuHeatmap.addSeparator();
		
		m_ItemHMCreateCluster = createMenuItem("Add Selection to Workspace", m_MenuHeatmap);
		m_ItemHMCreateCluster.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				m_ChasePainter.createClusterFromSelection();
			}
		});

		m_ItemHMExportImage = createMenuItem("Export Image...", m_MenuHeatmap);
		m_ItemHMExportImage.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				m_ChasePainter.exportHeatmap();
			}
		});
		
        addGenericMenuListener(m_MenuHeatmap);
        
		//TODO
//		itemHMExportRegions = createMenuItem("Export Selected Regions...", menuHeatmap);
//		itemHMExportRegions.addActionListener(new ActionListener() {
//			public void actionPerformed(ActionEvent e) {
//			}
//		});
		
	}
	
    protected void addPlotMenu()
    {
        m_MenuPlot = new JMenu("Plot");
        m_MenuBar.add(m_MenuPlot);

        m_ItemRPKM = new JCheckBoxMenuItem("Use RPKM");
        m_ItemRPKM.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                m_ChasePainter.m_ControlDetailPlot.setRPKM(m_ItemRPKM.isSelected());
                m_ChasePainter.invokeRedraw();
            }
        });
        m_MenuPlot.add(m_ItemRPKM);

        m_ItemSeparateCluster = new JCheckBoxMenuItem("Separate Cluster Plots");
        m_ItemSeparateCluster.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                m_ChasePainter.m_ControlDetailPlot.setSeparateClusterPlots(m_ItemSeparateCluster.isSelected());
                m_ChasePainter.invokeRedraw();
            }
        });
        m_MenuPlot.add(m_ItemSeparateCluster);
        
        m_MenuPlot.addSeparator();
        
        JMenuItem itemExportPlot = new JMenuItem("Export Plot...");
        itemExportPlot.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                m_ChasePainter.exportDetailedPlot();
            }
        });
        m_MenuPlot.add(itemExportPlot);
        
        addGenericMenuListener(m_MenuPlot);
    }
    
	protected void addMethodMenu()
	{
		m_MenuMethod = new JMenu("Methods");
		m_MenuBar.add(m_MenuMethod);
		
		m_ItemMethodKmeans = createMenuItem("Kmeans Clustering...", m_MenuMethod);
		m_ItemMethodKmeans.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) 
			{
				m_ChasePainter.showOperatorCluster();
			}
		});
		
		m_ItemMethodQuerySignal = createMenuItem("Signal Query...", m_MenuMethod);
		m_ItemMethodQuerySignal.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				m_ChasePainter.showOperatorQuerySignal();
			}
		});
		
		m_ItemMethodComparison = createMenuItem("Cluster Comparison...", m_MenuMethod);
		m_ItemMethodComparison.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				m_ChasePainter.showOperatorComparison();
			}
		});
        m_ItemMethodClusterByGroup = createMenuItem("Cluster by Group Label", m_MenuMethod);
        m_ItemMethodClusterByGroup.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) 
            {
                ClustInfo[] cInfos = m_ChasePainter.m_CDisplay.getAllSelectedClusters();
                if (cInfos == null || cInfos.length != 1) {
                    JOptionPane.showMessageDialog(null, "Please select a single cluster", "Information", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    m_ChasePainter.getFramework().clusterByInputGroupLabel(cInfos[0]);
                }
            }
        });
        
        addGenericMenuListener(m_MenuMethod);
	}
	
	/*
	protected void addHelpMenu() {
		helpMenu = new JMenu("Help");
		
		helpItem = new JMenuItem("User Guide");
		helpMenu.add(helpItem);
		
		helpMenu.addSeparator();
		
		viewExampleItem = new JMenuItem("View Example");
		helpMenu.add(viewExampleItem);
		
		menuBar.add(helpMenu);
	}
	
	public File chooseFileToOpen(File currentDir) {
		File fileToOpen = null;
		JFileChooser chooser;
		if (currentDir == null) {
			chooser = new JFileChooser();
		} else {
			chooser = new JFileChooser(currentDir);
		}
		chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
		JFrame frame = new JFrame();
		Integer returnVal = chooser.showOpenDialog(frame);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			fileToOpen = new File(chooser.getSelectedFile().getAbsolutePath());
		}
		return fileToOpen;
	}*/
	
	public File[] chooseFilesToOpen() {
		return chooseFilesToOpen(null);
	}
	
	public File[] chooseFilesToOpen(File currentDir) {
		File[] filesToOpen = null;
		JFileChooser chooser;
		if (currentDir == null) {
			chooser = new JFileChooser();
		} else {
			chooser = new JFileChooser(currentDir);
		}
		chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
		chooser.setMultiSelectionEnabled(true);
		JFrame frame = new JFrame();
		Integer returnVal = chooser.showOpenDialog(frame);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			filesToOpen = chooser.getSelectedFiles();
		}
		return filesToOpen;
	}
	
	public File nameFileToWrite() {
		return nameFileToWrite(null);
	}
	
	public File nameFileToWrite(File currentDir) {
		File fileToWrite = null;
		JFileChooser chooser;
		if (currentDir == null) {
			chooser = new JFileChooser();
		} else {
			chooser = new JFileChooser(currentDir);
		}
		chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
		JFrame frame = new JFrame();
		Integer returnVal = chooser.showSaveDialog(frame);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			fileToWrite = new File(chooser.getSelectedFile().getAbsolutePath());
		}
		return fileToWrite;
	}

	// updates the menu items based on the current state
	protected void updateMenuItems()
	{
		m_ItemHMSortTypes[m_ChasePainter.dp.hmSortCriteria.ordinal()].setSelected(true);
		m_ItemProfileTypes[m_ChasePainter.dp.plotType.ordinal()].setSelected(true);
		m_ItemUndo.setEnabled(m_ChasePainter.getFramework().getHistory().canUndo());
		m_ItemUndo.setText("Undo " + m_ChasePainter.getFramework().getHistory().getUndoText());
		m_ItemRedo.setEnabled(m_ChasePainter.getFramework().getHistory().canRedo());
		m_ItemRedo.setText("Redo " + m_ChasePainter.getFramework().getHistory().getRedoText());
        if (m_ChasePainter.m_ControlDetailPlot != null)
        {
    		m_ItemRPKM.setSelected(m_ChasePainter.m_ControlDetailPlot.m_bRPKM);
            m_ItemSeparateCluster.setSelected(m_ChasePainter.m_ControlDetailPlot.m_bSeparateClusterPlots);
        }
	}
}
