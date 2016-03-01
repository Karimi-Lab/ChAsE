package heidi.application;

import heidi.dimreduction.PrincipalComponent;
import heidi.plot.ScatterPlot;
import heidi.project.DataFileDim;
import heidi.project.Dim;
import heidi.project.Group;
import heidi.project.Plot;
import heidi.project.Project;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.IOException;
import java.util.HashMap;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import prefuse.data.io.DataIOException;


public class ProjectView extends JPanel {

	private Project                   m_project;
	private JSplitPane                m_splitPane;
	
	private Group                     m_selGroup;
	private JTabbedPane               m_groupPane;
	private HashMap<Group, GroupView> m_groupViews;
	
	private Plot                      m_selPlot;
	private JTabbedPane               m_plotPane;
	private HashMap<Plot, PlotView>   m_plotViews;
	
	
	private static final long serialVersionUID = 9165622525991405715L;
	
	public ProjectView(Project project) {
		super();
		setBorder(BorderFactory.createEmptyBorder());
		
		m_project = project;
		
		m_splitPane = new JSplitPane();
		m_splitPane.setBorder(BorderFactory.createEmptyBorder());
		m_splitPane.setOneTouchExpandable(true);
		m_splitPane.setDividerSize(15);
		m_splitPane.setContinuousLayout(true);

		// layout
		GridBagLayout layout = new GridBagLayout();
		setLayout(layout);
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.weightx = 1.0;
		constraints.weighty = 1.0;
		constraints.fill = GridBagConstraints.BOTH;
		add(m_splitPane, constraints);
		
		// tab folder of groups
		m_groupViews = new HashMap<Group, GroupView>();
		m_groupPane = new JTabbedPane();
		m_groupPane.setBorder(BorderFactory.createEmptyBorder());
		m_groupPane.setTabPlacement(JTabbedPane.LEFT);
		m_groupPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		m_splitPane.setLeftComponent(m_groupPane);
		m_groupPane.addChangeListener(new ChangeListener() {
	        // This method is called whenever the selected tab changes
	        public void stateChanged(ChangeEvent evt) {
	        	JTabbedPane pane = (JTabbedPane)evt.getSource();
	            // Get current tab
	            int sel = pane.getSelectedIndex();
	            
	            Group[] groups = m_project.getGroups();
	            if (sel >= 0 && sel < groups.length) {
	            	m_selGroup = groups[sel];
	            	updatePlotViews();
	            }
	        }
	    });
		
		// tab folder of plots
		m_plotViews = new HashMap<Plot, PlotView>();
		m_plotPane = new JTabbedPane();
		m_plotPane.setBorder(BorderFactory.createEmptyBorder());
		m_plotPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		m_splitPane.setRightComponent(m_plotPane);
		m_plotPane.addChangeListener(new ChangeListener() {
	        // This method is called whenever the selected tab changes
	        public void stateChanged(ChangeEvent evt) {
	            JTabbedPane pane = (JTabbedPane)evt.getSource();
	            // Get current tab
	            int sel = pane.getSelectedIndex();
	            
	            Plot[] plots = m_selGroup.getPlots();
	            if (sel >= 0 && sel < plots.length) {
	            	m_selPlot = plots[sel];
	            }
	        }
	    });
		
		updateGroupViews();
	}
	
	void addGroup(JFrame frame) {
		
		Group group = new Group("Untitled", m_project);
		m_project.addGroup(group);
		
		GroupView groupControl = new GroupView(group);
		m_groupViews.put(group, groupControl);
		
		showGroupProperties(group, frame);
		
		JScrollPane scrollPane = new JScrollPane(groupControl);
		scrollPane.setBorder(BorderFactory.createEmptyBorder());
		
		m_groupPane.addTab(group.getName(), scrollPane);
	}
	
	void addPlot(JFrame frame) {
		if (m_selGroup == null) {
			return;
		}
		
		Plot plot = new Plot("Untitled", ScatterPlot.getType(), m_selGroup);
		m_selGroup.addPlot(plot);
		
		PlotView plotControl = new PlotView(plot);
		m_plotViews.put(plot, plotControl);
		
		showPlotProperties(plot, frame);
		
		JScrollPane scrollPane = new JScrollPane(plotControl);
		scrollPane.setBorder(BorderFactory.createEmptyBorder());
		m_plotPane.addTab(plot.getName(), scrollPane);
	}
	
	void showDimProperties(JFrame frame) {
		if (m_selGroup == null) {
			return;
		}
		showDimProperties(m_selGroup, frame);
	}
	
	void showDimProperties(Group group, JFrame frame) {
		Dim[] dims = group.getDims();
		if (dims == null || dims.length == 0) {
			return;
		}
		
		DimPropertiesDialog dialog = new DimPropertiesDialog(group.getDims(), frame);
		int result = dialog.showDialog();
		if (result == GroupPropertiesDialog.OK) {
			for (Dim dim : group.getDims()) {
				String name = dialog.getName(dim);
				if (name != null) {
					dim.setName(name);
				}
				int type = dialog.getType(dim);
				if (type != -1  && dim instanceof DataFileDim) {
					((DataFileDim)dim).setType(type);
				}
			}
			for (GroupView groupView : m_groupViews.values()) {
				groupView.updateGroup();
			}
			
			for (PlotView plotView : m_plotViews.values()) {
				plotView.updatePlot();
			}
		}
	}
	
	void showGroupProperties(JFrame frame) {
		if (m_selGroup == null) {
			return;
		}
		showGroupProperties(m_selGroup, frame);
		
		int index = m_groupPane.getSelectedIndex();
		m_groupPane.setTitleAt(index, m_selGroup.getName());
	}
	
	private void showGroupProperties(Group group, JFrame frame) {
		
		// TODO - support editing other groups - not just the selected one
		
		GroupPropertiesDialog dialog = new GroupPropertiesDialog(group, frame); 
		int result = dialog.showDialog();
		if (result == GroupPropertiesDialog.OK) {
			boolean updateGroup = false;
			String name = dialog.getName();
			if (name != null) {
				group.setName(name);
				updateGroup = true;
			}
			Dim[] dims = dialog.getDimensions();
			if (dims != null && dims.length > 0) {
				// If Principal Components are selected,
				// add these to project and table
				Project project = group.getProject();
				for (Dim dim : dims) {
					if (dim instanceof PrincipalComponent) {
						PrincipalComponent pc = (PrincipalComponent)dim;
						pc.addToTable();
						project.addDim(pc);
					}
				}
				group.setDims(dims);
				updateGroup = true;
			}
			
			// TODO - there needs to be a cleanup phase when unused
			// principal components are removed from data
			
			if (updateGroup) {
				GroupView groupView = m_groupViews.get(group);
				groupView.updateGroup();
			}
			
		}
		
	}

	void showPlotProperties(JFrame frame) {
		if (m_selPlot == null) {
			return;
		}
		showPlotProperties(m_selPlot, frame);
		int index = m_plotPane.getSelectedIndex();
		m_plotPane.setTitleAt(index, m_selPlot.getName());
	}
	
	private void showPlotProperties(Plot plot, JFrame frame) {
		
		// TODO - support editing other plots - not just the selected one
		
		PlotPropertiesDialog dialog = new PlotPropertiesDialog(plot, frame);
		int result = dialog.showDialog();
		if (result == PlotPropertiesDialog.OK) {
			boolean updatePlot = false;
			
			String name = dialog.getName();
			if (name != null) {
				plot.setName(name);
				updatePlot = true;
			}
			String type = dialog.getType();
			if (type != null) {
				plot.setType(type);
				updatePlot = true;
			}
			Dim xDim = dialog.getXDimension();
			if (xDim != null) {
				plot.setXDimension(xDim);
				updatePlot = true;
			}
			Dim yDim = dialog.getYDimension();
			if (yDim != null) {
				plot.setYDimension(yDim);
				updatePlot = true;
			}
			Dim[] dims = dialog.getDims();
			if (dims != null) {
				plot.setDimensions(dims);
			}
			
			if (updatePlot) {
				PlotView plotView = m_plotViews.get(plot);
				plotView.updatePlot();
			}
		}
	}
	
	void showProjectProperties(JFrame frame) {
		ProjectPropertiesDialog dialog = new ProjectPropertiesDialog(m_project, frame);
		int result = dialog.showDialog();
		if (result == ProjectPropertiesDialog.OK) {
			
			boolean updateGroups = false;
			boolean updatePlots = false;
			
			String name = dialog.getName();
			if (name != null) {
				m_project.setName(name);
				frame.setTitle(m_project.getName());
			}
			
			String dataFile = dialog.getDataFile();
			if (dataFile != null) {
				try {
					m_project.setDataFile(dataFile);
					m_project.updateProject();
					updateGroups = true;
				} catch (IOException e) {
					String title = "Error loading data file";
					String message = "Unable to load data from file"+dataFile;
					JOptionPane.showMessageDialog(this, message, title, JOptionPane.ERROR_MESSAGE);
					
				} catch (DataIOException e) {
					String title = "Error loading data file";
					String message = "Unable to load data from file"+dataFile;
					JOptionPane.showMessageDialog(this, message, title, JOptionPane.ERROR_MESSAGE);
				}
			}
			
			
			int highlightRGB = dialog.getHighlightRGB();
			if (highlightRGB != -1) {
				m_project.setHighlight(highlightRGB);
				updatePlots = true;
				updateGroups = true;
			}
			
			Dim colorDim = dialog.getColorDimension();
			if (colorDim != null) {
				m_project.setColorDimension(colorDim);
				updatePlots = true;
				updateGroups = true;
			}
			
			int[] palette = dialog.getPalette();
			if (palette != null) {
				m_project.setPalette(palette);
				updatePlots = true;
				updateGroups = true;
			}
			
			Dim shapeDim = dialog.getShapeDimension();
			if (shapeDim != null) {
				m_project.setShapeDimension(shapeDim);
				updatePlots = true;
			}
			
			if (updateGroups) {
				updateGroupViews();
			}
			
			if (updatePlots) {
				updatePlotViews();
			}
		}
	}
	
	private void updateGroupViews() {
		// TODO - Optimize - don't create everything from scratch
		// only update things that have changed
		
		m_groupPane.removeAll();
		m_groupViews.clear();
		m_selGroup = null;
		
		Group[] groups = m_project.getGroups();
		if (groups == null) {
			return;
		}
		
		for (Group group : groups) {
			GroupView groupControl = new GroupView(group);
			m_groupViews.put(group, groupControl);
			
			JScrollPane scrollPane = new JScrollPane(groupControl);
			scrollPane.setBorder(BorderFactory.createEmptyBorder());
			m_groupPane.addTab(group.getName(), scrollPane);
		}
		
		m_splitPane.setDividerLocation(0.5);
		
		if (groups.length > 0) {
			m_groupPane.setSelectedIndex(0);
			m_selGroup = groups[0];
			updatePlotViews();
		}
	}
	
	private void updatePlotViews() {
		// Clear out previous plots
		m_plotPane.removeAll();
		m_plotViews.clear();
		m_selPlot = null;
		
		if (m_selGroup == null) {
			return;
		}
		
		Plot[] plots = m_selGroup.getPlots();
		if (plots == null) {
			return;
		}
		
		for (Plot plot : plots) {
			PlotView plotControl = new PlotView(plot);
			m_plotViews.put(plot, plotControl);
			
			JScrollPane scrollPane = new JScrollPane(plotControl);
			scrollPane.setBorder(BorderFactory.createEmptyBorder());
			m_plotPane.addTab(plotControl.getName(), scrollPane);
			
		}
		if (plots.length > 0) {
			m_plotPane.setSelectedIndex(0);
			m_selPlot = plots[0];
		}
	}
}
