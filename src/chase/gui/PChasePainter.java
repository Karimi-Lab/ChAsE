package chase.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowStateListener;
import java.util.EventObject;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.event.ChangeEvent;
import javax.swing.filechooser.FileNameExtensionFilter;

import controlP5.*;
import controlP5.PanelController.TransformListener;
import processing.core.*;

import still.data.Operator;
import still.gui.MouseState;
import still.gui.PBasicPainter;
import chase.*;
import chase.ClustFramework.SortCriteria;

public class PChasePainter extends PBasicPainter implements ControlListener, TransformListener, WindowStateListener
{
	
	private static final long serialVersionUID = -4468727316635587950L;

	ClusterDisplay m_CDisplay;
	HeatmapDisplay m_HMDisplay;
	DisplayParams  dp;
	
	controlP5.PanelController m_PanelMain;
	SplitPanelController 	  m_SplitPanelH;
	SplitPanelController	  m_SplitPanelV;
	PanelController           m_LeftPane;
	
	controlP5.Scrollbar		  m_ScrollWorkspaceX;  // horizontal scrollbar to scroll the workspace
	controlP5.Scrollbar		  m_ScrollWorkspaceY;  // vertical scrollbar to scroll the workspace
	//controlP5.Scrollbar		  m_ScrollCombineY;  // vertical scrollbar to scroll the Combine View
	controlP5.Scrollbar		  m_ScrollHeatmapY; // vertical resizable scrollbar to pan and zoom heatmap
	controlP5.Scrollbar		  m_ScrollFavorites;
	
	PanelController			  m_OperationPane;
	PanelController			  m_FavoritesPane;

	
	ControlPanel			  m_WorkspacePane;
	ControlPanel			  m_HeatmapPane;
	ControlPanelDetailPlot	  m_ControlDetailPlot;
	ControlPanelOnOff 		  m_ControlOnOff;
	ControlPanelIntersection  m_ControlIntersection;
	ControlPanelKmeans 		  m_ControlKmeans;
	
	ControlPanel			  m_CurrentControlOperator;
	
	boolean					  m_bForceRefreshCoordinates;
	
	private ClustFramework	  m_Framework;
	
	MainMenu				  m_MainMenu;
	PopupMenu 				  m_PopupMenu;
	

	public PChasePainter(Operator op)
	{
		super(op);
		BIG_BORDER_H_L = 0;
		BIG_BORDER_H_R = 0;
		BIG_BORDER_V_T = 0;
		BIG_BORDER_V_B = 0;
	}
	
	@Override
	public void actionPerformed(ActionEvent e)
	{
		super.actionPerformed(e);
	}
	
	public void setFramework(ClustFramework framework)
	{
		m_Framework = framework;
		m_Framework.setChangeListener(this);
	}
	
	public ClustFramework getFramework()
	{
		return m_Framework;
	}
	
	@Override
	public void setup() 
	{
		super.setup();
		
		size(1000, 700);
		setPreferredSize(new Dimension(1000, 700));

		m_CDisplay = new ClusterDisplay(this);
		m_CDisplay.setMouseState(m_DrawMouseState);
		m_HMDisplay = new HeatmapDisplay(this);
		//imagePlotType = loadImage("resources/icon_type.png"));  
		//imagePlotZoom = loadImage("resources/icon_zoom.png"));  
		dp = new DisplayParams(this);

//		selectThresholdSet(m_Framework.getThresholdingRoot());
		selectCluster(m_Framework.getRoot());

		addMenues();
		
		//heavyResize();

		guiCreateMain();
		
		m_bGuiCreated = true;
		
		heavyResize();
		
		guiCreateScrollbars();
		
		guiCreateDetailPlot();
		
		m_SplitPanelH.addDivider();
		m_SplitPanelV.addDivider();

		//selectCluster(m_Framework.getRoot());
		setCurrentControlOperator(null);
		frame.addWindowStateListener(this);
		
		showControlDetailPlot(m_Framework.getRoot(), 0);
		m_ControlDetailPlot.showSummaryView(true);

	//	frame.setExtendedState(Frame.MAXIMIZED_BOTH);
	}
	
	ControlP5	  controlP5;
	boolean       m_bGuiCreated = false;

	@Override
	public void windowStateChanged(WindowEvent arg0) 
	{
		heavyResize();
	}

	@Override
	public void heavyResize()
	{
		super.heavyResize();
		if (controlP5 != null)
		{
			Controller c = controlP5.controller("panelMain");
			if (c != null)
			{
				c.setSize(frame.getWidth(), frame.getHeight() - (int)c.position().y() - 40);
			}
		}
		refreshCoordinates();
		invokeRedraw();
	}
	
	void refreshCoordinates()
	{
		if (dp != null & m_bGuiCreated)
		{
			//int numGroups = m_Framework.getNumGroups();
			//float tabHeight = 0;
			//iconTypeRect = new Utils.Rect(10, 10 + dp.toolbarRect.top(), imageIconType.width, imageIconType.height);
			//iconZoomRect = new Utils.Rect(iconTypeRect.right() + 10, iconTypeRect.top(), imageIconZoom.width, imageIconZoom.height);

			// refresh coordinates
			dp.viewRect     = new Utils.Rect(0, 0, frame.getWidth(), frame.getHeight());
					//m_ViewCoords[0], m_ViewCoords[1], m_ViewCoords[2] - m_ViewCoords[0], m_ViewCoords[3] - m_ViewCoords[1]);

			dp.checkBoxSize = Math.min(Math.max(dp.plotW / 3, 5), 25);
			
			dp.plotGapX  = dp.plotW / 15; // horizontal gap between cluster plots
			dp.plotGapY  = dp.plotH / 2; // horizontal gap between cluster plots
			
			dp.favoriteRect = new Utils.Rect(m_PanelMain.position().x, m_PanelMain.position().y, 
					dp.plotW * 1.4f, m_PanelMain.getHeight());

			m_OperationPane.setWidth(m_LeftPane.getWidth() - (int)dp.favoriteRect.width());
			m_OperationPane.setPosition(dp.favoriteRect.width(), m_OperationPane.position().y());
			if (m_CurrentControlOperator != null)
				m_OperationPane.setHeight(m_CurrentControlOperator.getPrefferedHeight());
			else
				m_OperationPane.setHeight(1);
			
			m_WorkspacePane.setWidth(m_OperationPane.getWidth());
			m_WorkspacePane.setHeight(m_LeftPane.getHeight() - m_OperationPane.getHeight() - (int)m_OperationPane.position().y);
			m_WorkspacePane.setPosition(m_OperationPane.position().x, m_LeftPane.getHeight() - m_WorkspacePane.getHeight());
			

			//int treeDepth = Math.max(m_CDisplay.m_SelectedThresholdSet.visibleDepth(), 2);
			int treeDepth = 3;
			dp.treeWidth = (treeDepth + 1) * dp.treeGapX;
			dp.treeWidth = Math.max(dp.treeWidth, dp.favoriteRect.width() + dp.plotGapX);
			dp.treeRect     = new Utils.Rect(m_WorkspacePane.position().x + 25, m_WorkspacePane.position().y + m_WorkspacePane.paneTitleH,
											 dp.treeWidth, m_WorkspacePane.getHeight() - m_WorkspacePane.paneTitleH);
			
			dp.summaryLeft  = dp.treeRect.right();
			
			float clustLeft = dp.summaryLeft + dp.plotW + dp.plotW / 2;//Math.min(dp.plotW / 2 , 2 * dp.clustGapX);
			dp.clustRect = new Utils.Rect(clustLeft, dp.treeRect.top(),
					m_WorkspacePane.getWidth() + m_WorkspacePane.position().x - clustLeft - dp.plotW / 2, 
					dp.treeRect.height());
			
			
			dp.combineRect = new Utils.Rect(dp.clustRect.left(), m_OperationPane.position().y + m_ControlOnOff.getHeight(),
					dp.clustRect.width(), m_OperationPane.getHeight() - m_ControlOnOff.getHeight() - 20);
			
			dp.hmRect = new Utils.Rect(m_HeatmapPane.position().x + 20, m_HeatmapPane.position().y + dp.clustCaptionH,
					m_HeatmapPane.getWidth() - 50, m_HeatmapPane.getHeight() - dp.clustCaptionH - dp.hmLegendH);
			
			if (m_ScrollWorkspaceX != null)
			{
				m_ScrollWorkspaceX.position().x = dp.clustRect.left();
				m_ScrollWorkspaceX.setWidth((int)dp.clustRect.width());
			}

			if (m_ScrollWorkspaceY != null)
			{
				m_ScrollWorkspaceY.setHeight((int)dp.clustRect.height() - 15);
			}
			
			if (m_ScrollFavorites != null)
			{
				m_ScrollFavorites.setHeight((int)dp.favoriteRect.height());
			}
			
			int iTotalSelectedSize = m_CDisplay.getSelectedClustersTotalSize();
			if (m_ScrollHeatmapY != null && iTotalSelectedSize > 0)
			{
				m_ScrollHeatmapY.setMinVisibleAmount(1.f * dp.hmRect.height() / iTotalSelectedSize);
			}
			m_bForceRefreshCoordinates = false;
			
			dp.clustOffsetX = 0;
		}
	}
		
	
	/**
	 * Creates the GUI elements and scrollbars
	 */
	void guiCreateMain()
	{
		// color and font
		controlP5 = new ControlP5(this);
		PFont p = createFont("Verdana",16); 
		controlP5.setControlFont(p, 16);
		controlP5.setColorLabel(color(0));
		controlP5.setColorBackground(200);
		controlP5.setColorForeground(140);
		controlP5.setColorActive(80);
				
		// create the main panel
		int frameWidth = width;//frame.getWidth();
		int frameHeight = height;//frame.getHeight();
		m_PanelMain = new PanelController(controlP5, "panelMain", 0, 0, frameWidth, frameHeight);
		m_PanelMain.setColorBackground(0x01000000); // transparent background
		m_PanelMain.setColorForeground(0x01000000); // transparent background
		
		guiCreateSplitPanels();
		
		guiCreateControlPanels();
	}
	
	void guiCreateSplitPanels()
	{
		int colorTransparent = 0x01000000;
		// split the main view horizontally to left and right
		m_SplitPanelH = new SplitPanelController(controlP5, "splitH", 0, 0, 
				m_PanelMain.getWidth(), m_PanelMain.getHeight());
		m_SplitPanelH.setDividerLocation(0.5);
		m_SplitPanelH.getPane(SplitPanelController.LEFT).setColorBackground(colorTransparent);
		m_SplitPanelH.getPane(SplitPanelController.RIGHT).setColorBackground(colorTransparent);
		m_SplitPanelH.getPane(SplitPanelController.LEFT).setColorForeground(colorTransparent);
		m_SplitPanelH.getPane(SplitPanelController.RIGHT).setColorForeground(colorTransparent);
		m_SplitPanelH.setColorForeground(colorTransparent);
		m_SplitPanelH.setFastUpdate(true);
		m_PanelMain.addToLayout(m_SplitPanelH, PanelController.ANCHOR_ALL);
		m_LeftPane = m_SplitPanelH.getPane(SplitPanelController.LEFT);
		
		// split the left view vertically to top and bottom
		m_SplitPanelV = new SplitPanelController(controlP5, "splitV", 0, 0, 
				m_SplitPanelH.getPane(SplitPanelController.LEFT).getWidth(), m_SplitPanelH.getPane(SplitPanelController.LEFT).getHeight());
		m_SplitPanelV.setSplitType(SplitPanelController.VERTICAL);
		m_SplitPanelV.setDividerLocation(0.5);
		m_SplitPanelV.getPane(SplitPanelController.TOP).setColorBackground(colorTransparent);
		m_SplitPanelV.getPane(SplitPanelController.BOTTOM).setColorBackground(colorTransparent);
		m_SplitPanelV.getPane(SplitPanelController.TOP).setColorForeground(colorTransparent);
		m_SplitPanelV.getPane(SplitPanelController.BOTTOM).setColorForeground(colorTransparent);
		m_SplitPanelV.setColorForeground(colorTransparent);
		m_SplitPanelV.setFastUpdate(false);

		m_SplitPanelH.getPane(SplitPanelController.RIGHT).addToLayout(m_SplitPanelV, PanelController.ANCHOR_ALL);
		m_SplitPanelV.getPane(SplitPanelController.TOP).addTransformListener(this);
	}
	
	void showControlDetailPlot(ClustInfo cInfo, int group)
	{
		if (cInfo != null && cInfo.size() > 0)
		{
			m_ControlDetailPlot.setPlotInfo(cInfo, group);
			m_ControlDetailPlot.setMouseState(m_DrawMouseState);
			m_ControlDetailPlot.m_EnableThresholdLine = false;
			m_ControlDetailPlot.showSummaryView(false);
			m_ControlDetailPlot.show();
		}
	}
	
	void setCurrentControlOperator(ControlPanel panel)
	{
		if (m_CurrentControlOperator != null) {
			selectCluster(m_Framework.getRoot());
		}
		
		if (m_CurrentControlOperator != panel)
		{
			if (m_CurrentControlOperator != null)
				m_CurrentControlOperator.hide();
			m_CurrentControlOperator = panel;
			if (m_CurrentControlOperator != null)
			{
				m_CurrentControlOperator.show();
				m_OperationPane.setHeight(m_CurrentControlOperator.getPrefferedHeight());
			}
			heavyResize();
		}
	}
	
	void guiCreateControlPanels()
	{
		{
			int favoritesPaneW = 50;
			m_FavoritesPane = new PanelController(controlP5, "favoritesPane", 0, 0, favoritesPaneW, m_LeftPane.getHeight());
			m_FavoritesPane.setColorBackground(0x01000000); // transparent
			m_FavoritesPane.setColorForeground(0x01000000); // transparent
			m_LeftPane.addToLayout(m_FavoritesPane, PanelController.ANCHOR_LEFT | PanelController.ANCHOR_BOTTOM | PanelController.ANCHOR_TOP);
		}

		m_OperationPane = new PanelController(controlP5, "operationPane", 
				m_FavoritesPane.getWidth(), (int)dp.clustCaptionH,
				m_LeftPane.getWidth() - m_FavoritesPane.getWidth(),	1);//m_LeftPane.getHeight()/ 2);
		m_LeftPane.addToLayout(m_OperationPane, PanelController.ANCHOR_LEFT | PanelController.ANCHOR_RIGHT | PanelController.ANCHOR_TOP);
		m_OperationPane.setColorBackground(0x01000000); // transparent
		m_OperationPane.setColorForeground(0x01000000); // transparent

		
		m_WorkspacePane = new ControlPanel(controlP5, "cpWorkspace", 
				(int)m_OperationPane.position().x, 
				m_OperationPane.getHeight() + (int)m_OperationPane.position().y,
				m_OperationPane.getWidth(),
				m_LeftPane.getHeight() - m_OperationPane.getHeight() - (int)m_OperationPane.position().y,
				m_LeftPane,
				PanelController.ANCHOR_ALL);
		m_WorkspacePane.setTitle("Workspace");
		m_WorkspacePane.setDisplayParams(dp);
		
		PanelController panelTopRight = m_SplitPanelV.getPane(SplitPanelController.TOP);
		m_HeatmapPane = new ControlPanel(controlP5, "cpHeatmap", 0, 0,
				panelTopRight.getWidth(), panelTopRight.getHeight(), panelTopRight,
				PanelController.ANCHOR_ALL);
		m_HeatmapPane.setTitle("Heat Map");
		m_HeatmapPane.setDisplayParams(dp);
		
		// combine control panel
		//int panelOnOffH = 80;
		m_ControlOnOff = new ControlPanelOnOff(controlP5, "cpOnOff", 0, 0,
				m_OperationPane.getWidth(), m_OperationPane.getHeight(), m_OperationPane,
				PanelController.ANCHOR_ALL);
		m_ControlOnOff.setFramework(m_Framework);
		m_ControlOnOff.setDisplayParams(dp);
		m_ControlOnOff.addChangeListener(this);
		m_ControlOnOff.setMouseState(m_DrawMouseState);
		m_ControlOnOff.setClusterDisplay(m_CDisplay);
		m_ControlOnOff.setClustInfo(m_Framework.getRoot());
		m_ControlOnOff.hide();
		
		m_ControlIntersection = new ControlPanelIntersection(controlP5, "cpIntersection", 0, 0,
				m_OperationPane.getWidth(), m_OperationPane.getHeight(), m_OperationPane,
				PanelController.ANCHOR_ALL);
		m_ControlIntersection.setFramework(m_Framework);
		m_ControlIntersection.setDisplayParams(dp);
		m_ControlIntersection.addChangeListener(this);
		m_ControlIntersection.setMouseState(m_DrawMouseState);
		m_ControlIntersection.setClusterDisplay(m_CDisplay);
		m_ControlIntersection.hide();
		

		// kmeans control panel
		//int panelKmeansH = 50;
		m_ControlKmeans = new ControlPanelKmeans(controlP5, "cpKmeans", 0, 0,
				m_OperationPane.getWidth(), m_OperationPane.getHeight(), m_OperationPane,
				PanelController.ANCHOR_ALL);;
		m_ControlKmeans.setFramework(m_Framework);
		m_ControlKmeans.setDisplayParams(dp);
		m_ControlKmeans.addChangeListener(this);
		m_ControlKmeans.setMouseState(m_DrawMouseState);
		m_ControlKmeans.setClustInfo(m_Framework.getRoot());
		m_ControlKmeans.hide();
	}
	
	void guiCreateScrollbars()
	{
		int scrollbarW = 15; 
		ControllerGroup defaultGroup = (ControllerGroup) controlP5.controlWindow.tabs().get(1);
		// cluster horizontal scrollbar
		m_ScrollWorkspaceX = new controlP5.Scrollbar(controlP5, defaultGroup, "sbClustX",
				m_WorkspacePane.getWidth() - dp.clustRect.width(), 
				m_WorkspacePane.getHeight() - scrollbarW,
				dp.clustRect.iwidth(),	scrollbarW);
		controlP5.register(m_ScrollWorkspaceX);
		m_WorkspacePane.addToLayout(m_ScrollWorkspaceX, PanelController.ANCHOR_LEFT | PanelController.ANCHOR_BOTTOM | PanelController.ANCHOR_RIGHT);
		m_ScrollWorkspaceX.setValues(Scrollbar.HORIZONTAL, 0, 1000, 0, 1000);
		m_ScrollWorkspaceX.color().setForeground(color(0, 64));
		m_ScrollWorkspaceX.color().setBackground(color(128, 64));
		
		// cluster view vertical scrollbar
		m_ScrollWorkspaceY = new controlP5.Scrollbar(controlP5, defaultGroup, "sbClustY", 
				m_WorkspacePane.getWidth() - scrollbarW, dp.clustRect.top() - m_WorkspacePane.position().y, 
				scrollbarW, dp.clustRect.iheight());
		controlP5.register(m_ScrollWorkspaceY);
		m_WorkspacePane.addToLayout(m_ScrollWorkspaceY, PanelController.ANCHOR_TOP | PanelController.ANCHOR_BOTTOM | PanelController.ANCHOR_RIGHT);
		m_ScrollWorkspaceY.setValues(Scrollbar.VERTICAL, 0, 1000, 0, 1000);
		m_ScrollWorkspaceY.color().setForeground(color(0, 64));
		m_ScrollWorkspaceY.color().setBackground(color(128, 64));
		
		// heatmap scrollbar
		m_ScrollHeatmapY = new controlP5.Scrollbar(controlP5, defaultGroup, "sbHeatmap", 
				m_HeatmapPane.getWidth() -25, dp.hmRect.top() - m_HeatmapPane.position().y, 
				20, dp.hmRect.iheight());
		controlP5.register(m_ScrollHeatmapY);
		m_HeatmapPane.addToLayout(m_ScrollHeatmapY, PanelController.ANCHOR_TOP | PanelController.ANCHOR_BOTTOM | PanelController.ANCHOR_RIGHT);
		m_ScrollHeatmapY.setValues(Scrollbar.VERTICAL, 0, 1, 0, 1);
		m_ScrollHeatmapY.setResizable(true);
		m_ScrollHeatmapY.color().setForeground(color(190));
		m_ScrollHeatmapY.color().setBackground(color(220));
	}
		
	void guiCreateDetailPlot()
	{
		PanelController panelRightBottom = m_SplitPanelV.getPane(SplitPanelController.BOTTOM);
		m_ControlDetailPlot = new ControlPanelDetailPlot(controlP5, "panelDetailPlot", 0, 0,
				panelRightBottom.getWidth(), panelRightBottom.getHeight(), 
				panelRightBottom, PanelController.ANCHOR_ALL);
		m_ControlDetailPlot.setFramework(m_Framework);
		m_ControlDetailPlot.setDisplayParams(dp);
		m_ControlDetailPlot.addChangeListener(this);
		m_ControlDetailPlot.setMouseState(m_DrawMouseState);
		m_ControlDetailPlot.setClusterDisplay(m_CDisplay);
		m_ControlDetailPlot.setup();
		m_ControlDetailPlot.hide();
	}

	public void refreshHeatmap()
	{
		dp.hmGx = null;
		invokeRedraw();
	}
	
	// captures the mouse input if the cursor is inside the panel, to avoid affecting the controls underneath
	void captureMouseInput(ControlPanel panel)
	{
		if (panel.isVisible() && m_DrawMouseState.isIn(panel.position().x, panel.position().y, panel.getWidth(), panel.getHeight()))
		{
			MouseState m = new MouseState();
			m.copy(m_DrawMouseState);
			panel.setMouseState(m);// pass all the mouse activity to the large plot view
			m_DrawMouseState.setPaused(true); // disable mouse activity on the main view
		}
	}
	
	@Override
	protected void drawPlot()
	{
		if (m_bForceRefreshCoordinates)
			refreshCoordinates();
		
		g.pushStyle();

		m_DrawMouseState.setPaused(false);
		captureMouseInput(m_ControlDetailPlot);
		
		GuiUtils.setMouseState(m_DrawMouseState);
		GuiUtils.setDisplayParams(dp);
		
		if (m_ControlDetailPlot.isVisible() && m_ControlDetailPlot.m_EnableThresholdLine) {
			dp.hmThreshold = m_ControlDetailPlot.m_ThresholdCursor;
		} else {
			dp.hmThreshold = -1;
		}
		
		if (m_DrawMouseState.isReleased(MouseState.LEFT) && m_CDisplay.m_ActivePlotClust != null) {
			m_DrawMouseState.reset();
			showControlDetailPlot(m_CDisplay.m_ActivePlotClust, m_CDisplay.getActivePlotGroup());
		}

		m_CDisplay.setFramework(m_Framework);
		m_HMDisplay.setFramework(m_Framework);
		
		m_CDisplay.m_UIRegionMouseOver = null;
		
		if (m_ControlDetailPlot.m_ActivePlotGroup != -1)
		{
			m_CDisplay.setActivePlot(m_ControlDetailPlot.m_ClustInfo, m_ControlDetailPlot.m_ActivePlotGroup);
		}
		else
		{
			m_CDisplay.setActivePlot(null, -1);
		}
		
 		if (m_ScrollWorkspaceX != null)
		{
			// horizontal cluster view scrollbar
			m_ScrollWorkspaceX.setMax(Math.max(m_Framework.getNumGroups() * (dp.plotW + dp.plotGapX), 0.1f));
			m_ScrollWorkspaceX.setVisibleAmount( dp.clustRect.width());
			m_ScrollWorkspaceX.setVisible(m_ScrollWorkspaceX.getVisibleAmount() < m_ScrollWorkspaceX.max() - m_ScrollWorkspaceX.min());
			if (!m_ScrollWorkspaceX.isVisible() && m_ScrollWorkspaceX.value() != 0)
				m_ScrollWorkspaceX.setValue(0);
			
 			dp.clustOffsetX = m_ScrollWorkspaceX.value();
		}
		
 		int numVisibleRows = 0;
		for (int i = 0; i < m_Framework.getWorkingSet().size(); i++) {
 			numVisibleRows += m_Framework.getWorkingSet().get(i).getClustInfo().visibleLeaves();
		}
 		
		if (m_ScrollWorkspaceY != null)
		{
			// vertical tree view scrollbar
			m_ScrollWorkspaceY.setMax(Math.max(numVisibleRows * (dp.plotH + dp.plotGapY) + dp.plotGapY, 0.1f));
			m_ScrollWorkspaceY.setVisibleAmount( dp.clustRect.height());
			m_ScrollWorkspaceY.setVisible(m_ScrollWorkspaceY.getVisibleAmount() < m_ScrollWorkspaceY.max() - m_ScrollWorkspaceY.min());
			if (!m_ScrollWorkspaceY.isVisible() && m_ScrollWorkspaceY.value() != 0)
				m_ScrollWorkspaceY.setValue(0);
			
 			dp.clustOffsetY = m_ScrollWorkspaceY.value();
		}

		if (m_ScrollHeatmapY != null)
		{
			// heatmap scrollbar
			boolean realTimeHMUpdate = true;
			if ((realTimeHMUpdate || !m_ScrollHeatmapY.isDragging()) && 
				(dp.hmScrollYOffset != m_ScrollHeatmapY.value() || 
				dp.hmScrollYRange  != m_ScrollHeatmapY.getVisibleAmount()))
			{
				dp.hmScrollYOffset = m_ScrollHeatmapY.value();
				dp.hmScrollYRange  = m_ScrollHeatmapY.getVisibleAmount();
				refreshHeatmap();
			}
		}
		
		// clear the entire view rect
 		g.fill(255);
 		g.noStroke();
		g.rect(dp.viewRect.left(), dp.viewRect.top(), dp.viewRect.width(), dp.viewRect.height());
		
		// draw vertical stripes
		//float lx = dp.clustRect.left() - dp.clustOffsetX; // label x
		//float dlx = dp.plotW + dp.clustGapX;
		g.noStroke();
		
		if (m_DrawMouseState.x() > dp.clustRect.left() && m_DrawMouseState.x() < dp.clustRect.right())
		{
			int activeGroup =(int) ((m_DrawMouseState.x() - dp.clustRect.left() - dp.clustOffsetX) / (dp.plotGapX + dp.plotW));
			if (activeGroup >= 0 && activeGroup < m_Framework.getNumGroups()) {
				m_CDisplay.setActivePlotGroup(activeGroup);
			}
		}
		
		/*
		if (m_CDisplay.getActivePlotGroup() >= 0)
		{
			g.fill(0x220000ff);
			g.rect(lx + dlx * m_CDisplay.getActivePlotGroup(), dp.viewRect.top(), dlx, dp.viewRect.height());
		}*/

 		g.smooth();
 		
 		{// draw the cluster view plots
 			//float fTop = dp.treeRect.top() - dp.clustOffsetY + dp.plotH/2 + dp.clustGapY;
 			float fTop = dp.clustRect.top() - dp.clustOffsetY;
 			for (int i = 0; i < m_Framework.getWorkingSet().size(); i++)
			{
 	 			ClustInfo clustViewRoot = m_Framework.getWorkingSet().get(i).getClustInfo();
				
				try {
					m_CDisplay.drawMultipleClusters(g, dp, clustViewRoot.getVisibleNodes(true), dp.clustRect.left() - dp.clustOffsetX, fTop, dp.clustRect);
				} catch (Exception e) {
					// to make sure an error in rendering one cluster, won't break the entire rendering
					e.printStackTrace(); 
				}
				
		 		int rows = m_CDisplay.drawTree(g, dp, clustViewRoot, dp.treeRect.left(), fTop + dp.plotH/2 + dp.plotGapY);
		 		fTop += rows * (dp.plotGapY + dp.plotH);
			}
		}

 		g.fill(dp.groupLabelColor);
 		//dp.groupLabelFontSize = 10;
		g.textSize(dp.groupLabelFontSize);
		Utils.Rect labelsRect = new Utils.Rect(dp.clustRect.left(), 0, dp.clustRect.width(), dp.clustCaptionH);
		m_CDisplay.drawGroupLabels(g, dp, dp.clustRect.left() - dp.clustOffsetX, dp.clustCaptionH, dp.plotW + dp.plotGapX, labelsRect, false);
		
		drawFavorites();

		try
		{// draw the heatmap and labels
			g.fill(255);
			g.noStroke();
			g.rect(m_HeatmapPane.position().x, m_HeatmapPane.position().y, m_HeatmapPane.getWidth(), m_HeatmapPane.getHeight());
			
			if (m_CDisplay.getSelectedClustersTotalSize() > 0)
			{
				//dp.hmOffsetX = dp.clustOffsetX;
				dp.hmW =  dp.hmRect.width() / (m_Framework.getNumVisibleGroups());
				dp.hmGapX = dp.hmW / 15;
				dp.hmW -= dp.hmGapX;
				m_HMDisplay.m_TopSelected = m_CDisplay.m_TopSelected;
				m_HMDisplay.drawHeatMap(g, m_CDisplay.getAllSelectedClusters(), dp, m_DrawMouseState);
				
				g.fill(dp.groupLabelColor);
				g.textSize(dp.groupLabelFontSize);
				float dx = (dp.hmW + dp.hmGapX);
				labelsRect = new Utils.Rect(dp.hmRect.left(), dp.hmRect.top() - 40, dp.hmRect.width(), 40);
				m_CDisplay.drawGroupLabels(g, dp, dp.hmRect.left(), dp.clustCaptionH, dx, labelsRect, true);
				
				if (m_DrawMouseState.isReleased(MouseState.LEFT) && labelsRect.isInside(m_DrawMouseState.endX(), m_DrawMouseState.endY()) 
						&& labelsRect.isInside(m_DrawMouseState.startX(), m_DrawMouseState.startY()))
				{// set the sorted mark
					int clickedGroup = (int)Math.min(Math.max(((m_DrawMouseState.x() - dp.hmRect.left() + dp.clustOffsetX) / dx), 0), m_Framework.getNumGroups() - 1);
					clickedGroup = m_Framework.getGroupOrder(clickedGroup, false);
					if (clickedGroup != dp.hmSortGroup)	{
						dp.hmSortGroup = clickedGroup;
					} else {
						dp.hmSortAscending = !dp.hmSortAscending;
					}
					
					refreshHeatmap();
					invokeRedraw();
				}
				
				if (m_ScrollHeatmapY != null) {
					m_ScrollHeatmapY.setVisible(m_CDisplay.getSelectedClustersTotalSize() > dp.hmRect.height());
				}
			}
		}
		catch (Exception e) 
		{
			System.out.println("Exception: PHeatMapPainter.drawHeatMap()");
			e.printStackTrace();
		}
 		
		g.textSize(12);
 		drawToolbar();
 		
		g.popStyle();
		strokeWeight(1);
		//g.noStroke();
		stroke(0);
		g.textAlign(PConstants.LEFT, PConstants.BOTTOM); // default text align for controlP5

		m_ControlDetailPlot.m_ActivePlotGroup = -1;
	}
	
	void drawFavorites()
	{
		g.pushStyle();
		g.fill(255);
		g.stroke(0);
		g.strokeWeight(1);
		g.rect(dp.favoriteRect.left(), dp.favoriteRect.top(), dp.favoriteRect.width(), dp.favoriteRect.height());
		for (int i = 0; i < m_Framework.getFavorites().size(); i++)
		{
			Utils.Rect plotRect = new Utils.Rect(dp.favoriteRect.left() + dp.plotW * 0.2f, 
					dp.favoriteRect.top() + i * (dp.plotGapY + dp.plotH) + dp.plotGapY, 
					dp.plotW, dp.plotH); 
			g.strokeWeight(1);
			boolean isMouseInside = plotRect.isInside(m_DrawMouseState.x(), m_DrawMouseState.y());
			g.stroke(isMouseInside ? 0xFFFF0000 : 0);
			g.rect(plotRect.left(), plotRect.top(), plotRect.width(), plotRect.height());
			
			if (m_DrawMouseState.isReleased(MouseState.LEFT) && dp.favoriteRect.isInside(m_DrawMouseState.x(), m_DrawMouseState.y()))
			{
				if (isShiftPressed() && isMouseInside)
					m_Framework.getFavorites().get(i).setSelected(!m_Framework.getFavorites().get(i).isSelected());
				if (!isShiftPressed())
					m_Framework.getFavorites().get(i).setSelected(isMouseInside);
			}
			
			if (m_Framework.getFavorites().get(i).isSelected())
			{
				g.fill(0x22000000);
				g.noStroke();
				g.rect(dp.favoriteRect.left(), plotRect.top() - dp.plotGapY/2, dp.favoriteRect.width(), plotRect.height() +  dp.plotGapY);
			}
			
			g.stroke(128);
			g.strokeWeight(2);
			m_CDisplay.drawSummaryPlot(g, dp, m_Framework.getFavorites().get(i).getClustInfo(), plotRect, m_CDisplay.getActivePlotGroup(), true);
		}
		g.popStyle();
	}
	
	
	boolean selectCluster(ClustInfo cInfo)
	{
		//if (m_CDisplay.m_ClustSelected != cInfo)
		if (!(m_CDisplay.getNumSelectedClusters() == 1 && m_CDisplay.isClusterSelected(cInfo)))
		{
			if (!isShiftPressed()) {
				m_CDisplay.selectCluster(cInfo);
			} else {
				m_CDisplay.addClusterToSelection(cInfo);
			}
			
			
			dp.hmScrollYOffset = 0;
			dp.hmScrollYRange = 1;
			dp.hmForceUpdate = true;
			
			if (m_ScrollHeatmapY != null)
			{
				m_ScrollHeatmapY.setValue(0);
				m_ScrollHeatmapY.setVisibleAmount(1);
			}
			
			if (m_ControlKmeans != null) {
				m_ControlKmeans.setClustInfo(cInfo);
			}

			if (m_ControlDetailPlot != null && m_ControlDetailPlot.isVisible())
			{
				showControlDetailPlot(cInfo, 0);
				m_ControlDetailPlot.showSummaryView(true);
			}
			
			return true;
		}
		else
		{
			showControlDetailPlot(cInfo, 0);
			m_ControlDetailPlot.showSummaryView(true);
		}
		return false;
	}
	
	public void refreshWorkspace(boolean reset)
	{
		setCurrentControlOperator(null);
		selectCluster(m_Framework.getRoot());
//		m_ControlDetailPlot.setClustInfo(m_CDisplay.m_ClustSelected);
//		m_ControlKmeans.setClustInfo(m_CDisplay.m_ClustSelected);
//		m_ControlOnOff.setClustInfo(m_CDisplay.m_ClustSelected);
//		m_ControlIntersection.setClustInfo(m_CDisplay.m_ClustSelected);
		refreshHeatmap();
	}
	
	boolean collapseCluster(ClustInfo cInfo)
	{
		if (cInfo != null && cInfo.m_Child != null)
		{
			cInfo.m_bShowChildren = !cInfo.m_bShowChildren;
			refreshHeatmap();
			return true;
		}
		return false;
	}
	
	void drawToolbar()
	{
	}

	@Override
	public synchronized void mouseClicked()
	{
		super.mouseClicked();
		
		invokeRedraw();
	}

	@Override
	public synchronized void mousePressed()
	{
		super.mousePressed();
		if (m_CDisplay.m_UIRegionMouseOver != null)
		{
			if (m_CDisplay.m_UIRegionMouseOver.type == ClusterDisplay.UIRegionType.TREE_NODE)
			{
				ClustInfo cInfo = (ClustInfo) m_CDisplay.m_UIRegionMouseOver.object;
				
				if (mouseButton == PConstants.LEFT)
				{
					if (m_CDisplay.isClusterSelected(cInfo)) {
						collapseCluster(cInfo);
					} else {
						selectCluster(((ClustInfo) m_CDisplay.m_UIRegionMouseOver.object));
					}
					
					heavyResize();
				}
			}
		}
		
		invokeRedraw();
	}
	
	public void mousePressed(MouseEvent e) {
		super.mousePressed(e);
		if (e.isPopupTrigger()) {
			showPopup(e);
		}
	}
	
	public void mouseReleased(MouseEvent e) {
		// popup trigger is set on MOUSE_RELEASED in Windows
		super.mouseReleased(e);
		if (e.isPopupTrigger()) {
			showPopup(e);
		}
	}
	
	@Override
	public synchronized void mouseReleased()
	{
		super.mouseReleased();

		if (m_CDisplay.m_UIRegionMouseOver != null)
		{
			if (m_CDisplay.m_UIRegionMouseOver.type == ClusterDisplay.UIRegionType.CLUSTER)
			{
				ClustInfo clustInfo = (ClustInfo) m_CDisplay.m_UIRegionMouseOver.object;
				if (mouseButton == LEFT)
				{
					m_CDisplay.dropCluster(clustInfo);
					selectCluster(clustInfo);
				}
				else
				{
					//if (clustInfo != m_CDisplay.m_ClustSelected)
					//	selectCluster(clustInfo);
					m_PopupMenu.getPopupWorkspace().show(this, mouseX, mouseY);
				}
			}
		}
		else
		{
			m_CDisplay.dropCluster(null);
		}
		invokeRedraw();
	}

	
	@Override
	public synchronized void mouseDragged()
	{
		super.mouseDragged();
		if (mouseButton == LEFT && m_CDisplay.m_UIRegionMouseOver != null)
		{
			if (m_CDisplay.m_UIRegionMouseOver.type == ClusterDisplay.UIRegionType.CLUSTER)
			{
				if (m_CDisplay.dragCluster((ClustInfo) m_CDisplay.m_UIRegionMouseOver.object))
				{
					m_CDisplay.m_ClustDragX = mouseX - m_CDisplay.m_UIRegionMouseOver.rect.left();
					m_CDisplay.m_ClustDragY = mouseY - m_CDisplay.m_UIRegionMouseOver.rect.top();
				}
			}
		}
		invokeRedraw();
	}
	
	@Override
	public synchronized void mouseMoved()
	{
		super.mouseMoved();
		invokeRedraw();
    }

	@Override
	public synchronized void mouseWheelMoved (MouseWheelEvent e)
	{
		super.mouseWheelMoved(e);
		int iWheelRotation = e.getWheelRotation();
		
		if (m_ScrollWorkspaceY != null && (dp.treeRect.isInside(mouseX, mouseY) || dp.clustRect.isInside(mouseX, mouseY)))
			m_ScrollWorkspaceY.setValue(m_ScrollWorkspaceY.value() + iWheelRotation * dp.plotH/2);
		else if (m_ScrollHeatmapY != null && dp.hmRect.isInside(mouseX, mouseY))
			m_ScrollHeatmapY.setValue(m_ScrollHeatmapY.value() + 1.f*iWheelRotation/dp.hmCurrHeight);
	}

	@Override
	public void keyPressed()
	{
		super.keyPressed();
		
		if (key == '+' || key == '=' || key == '-')
		{
			dp.plotW = dp.plotH = Math.min(Math.max(dp.plotW + (key == '-' ? -10 : 10), dp.plotSizeMin), dp.plotSizeMax);
			heavyResize();
		}

		if (key == PConstants.CODED)
		{
			if ((keyCode == PConstants.UP || keyCode == PConstants.DOWN) && m_CDisplay.getNumSelectedClusters() == 1)
			{
				m_Framework.reorderClusterNode(m_CDisplay.getSelectedCluster(0), keyCode == PConstants.UP ? -1 : +1);
			}
		}
		
		if (key >= '1' && key < '1' + DisplayParams.PlotType.values().length && key <= '4') //key <= '9')
		{
			setProfileType(key - '1');
		}

		/*{
		 	// WARNING: this gets called when user hits delete key within kmeans k value text box...
			if ((keyCode == PConstants.DELETE || keyCode == PConstants.BACKSPACE) && m_CDisplay.m_ClustSelected != null)
			{
				m_CDisplay.m_ClustSelected.deleteChildren();
				heavyResize();
			}

			if (key == 'c' && m_CDisplay.m_ClustSelected != null)
			{
				m_CDisplay.m_ClustSelected.createClone();
				heavyResize();
			}
			
			if (key == PConstants.CODED)
			{
				if ((keyCode == PConstants.UP || keyCode == PConstants.DOWN) && m_CDisplay.m_ClustSelected != null)
				{
					m_Framework.reorderClusterNode(m_CDisplay.m_ClustSelected, keyCode == PConstants.UP ? -1 : +1);
				}
			}
		}*/
		
		invokeRedraw();
	}

	@Override
	public void stateChanged(ChangeEvent e)
	{
		if (e instanceof ControlPanel.ControlChangeEvent)
		{
			String eventName = ((ControlPanel.ControlChangeEvent)e).name;
			if (eventName.equals("close"))
			{
				if (e.getSource() == m_CurrentControlOperator)
				{
					setCurrentControlOperator(null);
				}
			}
			
			ClustInfo clustResult = null;
			String actionName = "";
			if (e.getSource() == m_ControlOnOff)
			{
				clustResult = m_ControlOnOff.getCurrentResult();
				actionName = "Add Query";
			}
			if (e.getSource() == m_ControlIntersection)
			{
				clustResult = m_ControlIntersection.getCurrentResult();
				actionName = "Add Comparison";
			}
			
			if (clustResult != null)
			{ 
				if (eventName.equals("add"))
				{
					m_Framework.getHistory().saveSnapShot(actionName);
					m_Framework.getWorkingSet().add(clustResult, true);
				}
				else if (eventName.equals("detailedPlot"))
				{
					m_DrawMouseState.reset();
					ControlPanel.ControlChangeEvent ct = (ControlPanel.ControlChangeEvent) e;
					int iGroup = (Integer)ct.value;
					//selectCluster(m_Framework.getClusteringRoot());
					if (iGroup >= 0)
					{
						showControlDetailPlot(clustResult, iGroup);
						//m_Framework.getClusteringRoot().m_bShowChildren = false;
						dp.hmSortGroup = iGroup;
						dp.hmSortAscending = false;
						setHMSortCriteria(SortCriteria.SIGNAL_PEAK);
						m_ControlDetailPlot.m_EnableThresholdLine = false;
					}
				}
				else if (eventName.equals("summaryPlot"))
				{
					showControlDetailPlot(clustResult, 0);
					m_ControlDetailPlot.showSummaryView(true);
					selectCluster(clustResult);
				}
			}
		}
		
		if (e.getSource() == m_ControlKmeans) {
			refreshHeatmap();
		}
		else if (e.getSource() == m_Framework) {
			selectCluster(m_Framework.getRoot());
			refreshHeatmap();
		}
		
		heavyResize();
	}

	@Override
	public void controlEvent(ControlEvent theEvent)
	{
		if (theEvent.isController())
		{
		} 		
	}
	
	void setHMSortCriteria(ClustFramework.SortCriteria criteria)
	{
		dp.hmSortCriteria = criteria;
		m_PopupMenu.updateMenuItems();
		refreshHeatmap();
		invokeRedraw();
	}
	
	void zoomPlots(int step)
	{
		dp.plotW = dp.plotH = Math.min(Math.max(dp.plotW + 10*step, dp.plotSizeMin), dp.plotSizeMax);
		heavyResize();
	}
	
	void setProfileType(int index)
	{
		dp.plotType = DisplayParams.PlotType.values()[index];
		m_PopupMenu.updateMenuItems();
		invokeRedraw();
	}
	
	protected void addMenues()
	{
		m_MainMenu = new MainMenu(this);
		frame.add(m_MainMenu.getMenuBar(), BorderLayout.PAGE_START);
		frame.pack();
		
		m_PopupMenu = new PopupMenu(this);
	}
	
	
	void showPopup(MouseEvent e)
	{
		if (e.getX() > m_HeatmapPane.position().x && e.getX() < m_HeatmapPane.position().x + m_HeatmapPane.getWidth() &&
			e.getY() > m_HeatmapPane.position().y && e.getY() < m_HeatmapPane.position().y + m_HeatmapPane.getHeight()) {
			m_PopupMenu.getPopupHeatmap().show(e.getComponent(), e.getX(), e.getY());
		}
		else if (e.getX() > m_ControlDetailPlot.position().x && e.getX() < m_ControlDetailPlot.position().x + m_ControlDetailPlot.getWidth() &&
				e.getY() > m_ControlDetailPlot.position().y && e.getY() < m_ControlDetailPlot.position().y + m_ControlDetailPlot.getHeight()) {
				m_PopupMenu.getPopupDetailPlot().show(e.getComponent(), e.getX(), e.getY());
			}
		//else if (dp.clustRect.isInside(e.getX(), e.getY()) || dp.combineRect.isInside(e.getX(), e.getY())) {
		else if (e.getX() > m_WorkspacePane.position().x && e.getX() < m_WorkspacePane.position().x + m_WorkspacePane.getWidth() &&
		         e.getY() > m_WorkspacePane.position().y && e.getY() < m_WorkspacePane.position().y + m_WorkspacePane.getHeight()) {
			m_PopupMenu.getPopupWorkspace().show(e.getComponent(), e.getX(), e.getY());
		}
		else if (m_ControlOnOff.isVisible() &&
				e.getX() > m_ControlOnOff.position().x && e.getX() < m_ControlOnOff.position().x + m_ControlOnOff.getWidth() &&
				 e.getY() > m_ControlOnOff.position().y && e.getY() < m_ControlOnOff.position().y + m_ControlOnOff.getHeight()) {
			m_ControlOnOff.popupOnOff.show(e.getComponent(), e.getX(), e.getY());
		}
		else if (m_ControlKmeans.isVisible() &&
				e.getX() > m_ControlKmeans.position().x && e.getX() < m_ControlKmeans.position().x + m_ControlKmeans.getWidth() &&
				 e.getY() > m_ControlKmeans.position().y && e.getY() < m_ControlKmeans.position().y + m_ControlKmeans.getHeight()) {
			m_ControlKmeans.popupCluster.show(e.getComponent(), e.getX(), e.getY());
		}
		else if (dp.favoriteRect.isInside(e.getX(), e.getY()))	{
			m_PopupMenu.getPopupFavorite().show(e.getComponent(), e.getX(), e.getY());
		}
	}
	
	void saveSelectedCluster()
	{
		if (m_CDisplay.getNumSelectedClusters() == 1) {
			JFileChooser fc = new JFileChooser();
			fc.setFileFilter(new FileNameExtensionFilter("GFF file", "gff"));
			if (fc.showSaveDialog(null) == JFileChooser.APPROVE_OPTION)
			{
				m_Framework.saveClusterRegions(fc.getSelectedFile(), m_CDisplay.getSelectedCluster(0), dp.hmSortGroup, dp.hmSortCriteria, dp.hmSortAscending);
			}
		}
	}
	
	void editClusterTitle()
	{
		if (m_CDisplay.getNumSelectedClusters() != 1)
			return;
		
		String s = (String)JOptionPane.showInputDialog (
                frame, "Title:\n", "Change Title", JOptionPane.PLAIN_MESSAGE, null, null,
                m_CDisplay.getSelectedCluster(0).getTitle());

		if (s != null) {
			m_CDisplay.getSelectedCluster(0).setTitle(s);
			invokeRedraw();
		}
	}
	
	void reverseHeatmapSort()
	{
		dp.hmSortAscending = !dp.hmSortAscending;
		refreshHeatmap();
		invokeRedraw();
	}
	
	ExportImageDialog m_ExportHeatmapDialog = null;
	void exportHeatmap()
	{
		try{
			if (m_ExportHeatmapDialog == null)
	    		m_ExportHeatmapDialog = new ExportImageDialog();
			
	    	if (m_ExportHeatmapDialog.showModal())
	    	{
	    		int iwidth = m_ExportHeatmapDialog.getInputWidth();
	    		int iheight = m_ExportHeatmapDialog.getInputHeight();
	    		
	    		Utils.Rect prevRect = dp.hmRect;
	    		int	prevFontSize = dp.hmLegendFontSize;
	    		
	    		float captionH = m_ExportHeatmapDialog.isShowLabels() ? dp.clustCaptionH * m_ExportHeatmapDialog.getFontSize()/20 : 20;
	    		float legendH = m_ExportHeatmapDialog.isShowLegend() ? dp.hmLegendH * m_ExportHeatmapDialog.getFontSize()/dp.hmLegendFontSize : 20;
	    		dp.hmLegendFontSize = m_ExportHeatmapDialog.getFontSize();
	    		dp.hmShowLegend = m_ExportHeatmapDialog.isShowLegend();
	    		dp.hmRect = new Utils.Rect(20, captionH, iwidth - 40, iheight - captionH - legendH);
				dp.hmW =  dp.hmRect.width() / (m_Framework.getNumVisibleGroups());
				dp.hmGapX = dp.hmW / 15;
				dp.hmW -= dp.hmGapX;
	    		
	    		PGraphics pdf = createGraphics(iwidth, iheight, PApplet.PDF, m_ExportHeatmapDialog.getFilename());
	    		pdf.beginDraw();
	    		
	    		m_HMDisplay.drawHeatMap(pdf, m_CDisplay.getAllSelectedClusters(), dp, new MouseState());
	
	    		if (m_ExportHeatmapDialog.isShowLabels())
	    		{
		    		pdf.fill(64);
		    		pdf.textSize(m_ExportHeatmapDialog.getFontSize());
		    		float dx = (dp.hmW + dp.hmGapX);
		    		Utils.Rect labelsRect = new Utils.Rect(dp.hmRect.left(), dp.hmRect.top() - 40, dp.hmRect.width(), 40);
		    		m_CDisplay.drawGroupLabels(pdf, dp, dp.hmRect.left(), dp.hmRect.top() - 10, dx, labelsRect, true);
	    		}
	    		
	    		dp.hmRect = prevRect;
	    		dp.hmLegendFontSize = prevFontSize;
	    		dp.hmShowLegend = true;
	
	    		pdf.dispose();
	    		pdf.endDraw();
	    		
	    	}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	ExportPlotDialog m_ExportPlotDialog = null;
	void exportDetailedPlot()
	{
		if (m_ExportPlotDialog == null)
			m_ExportPlotDialog = new ExportPlotDialog();
		
    	if (m_ExportPlotDialog.showModal())
    	{
    		int iwidth = m_ExportPlotDialog.getInputWidth();
    		int iheight = m_ExportPlotDialog.getInputHeight();
			
			PGraphics pdf = createGraphics(iwidth, iheight, PApplet.PDF, m_ExportPlotDialog.getFilename());
			pdf.beginDraw();
			m_ControlDetailPlot.setRenderSize(0, 0, iwidth, iheight);
			m_ControlDetailPlot.m_bShowLabels = m_ExportPlotDialog.isShowLabels();
			m_ControlDetailPlot.m_bShowLegend = m_ExportPlotDialog.isShowLegend();
			m_ControlDetailPlot.m_FontSize = m_ExportPlotDialog.getFontSize();
			m_ControlDetailPlot.m_StrokeWidth = m_ExportPlotDialog.getStrokeWidth();
			m_ControlDetailPlot.m_MouseState.setX(-1);
			m_ControlDetailPlot.m_MouseState.setY(-1);
			
			m_ControlDetailPlot.drawDetailPlot(pdf);
			
			pdf.dispose();
			pdf.endDraw();
			
			m_ControlDetailPlot.resetRenderState();
    	}
	}
	
	
	RegionsDialog m_DialogRegions;
	void showRegionsDialog()
	{
		if (m_CDisplay.getNumSelectedClusters() != 1) {
			return;
		}
		
		if (m_DialogRegions == null) {
			m_DialogRegions = new RegionsDialog();
		}
		
		m_DialogRegions.setText(m_Framework.getClusterRegionsString(m_CDisplay.getSelectedCluster(0), dp.hmSortGroup, dp.hmSortCriteria, dp.hmSortAscending));

    	if (m_DialogRegions.showModal())
    	{
    		saveSelectedCluster();
    	}
	}
	
	void createClusterFromSelection()
	{
		int[] selected = m_HMDisplay.getSelectedIndices();
		if (selected != null)
		{
			m_Framework.getHistory().saveSnapShot("Cluster from Selection");
			m_Framework.getWorkingSet().createNew(selected);
			invokeRedraw();
		}
	}
	
	@Override
	public void controlPositionChanged(EventObject e) {
		m_bForceRefreshCoordinates = true;
		invokeRedraw();
	}

	@Override
	public void controlSizeChanged(EventObject e) {
		m_bForceRefreshCoordinates = true;
		invokeRedraw();
	}
	
	void addClusterToFavorites()
	{
		if (m_CDisplay.getNumSelectedClusters() > 0)
		{
			for (int i = 0; i < m_CDisplay.getNumSelectedClusters(); ++i) {
				m_Framework.getFavorites().add(m_CDisplay.getSelectedCluster(i), true);
			}
		}
		invokeRedraw();
	}
	
	void addFavoriteToWorkspace()
	{
		for (int i = 0; i < m_Framework.getFavorites().size(); i++)
		{
			if (m_Framework.getFavorites().get(i).isSelected())
			{
				m_Framework.getWorkingSet().add(m_Framework.getFavorites().get(i).getClustInfo(), true);
			}
		}
		invokeRedraw();
	}

	void removeFavorite()
	{
		if (JOptionPane.showConfirmDialog(
				frame,
				"Remove selected favorites?",
				"Confirmation",
				JOptionPane.YES_NO_OPTION) == 0)
		{
			for (int i = m_Framework.getFavorites().size() - 1; i > 0; i--)
			{
				if (m_Framework.getFavorites().get(i).isSelected())
				{
					m_Framework.getFavorites().remove(i);
					//selectThresholdSet(m_Framework.getThresholdingRoot());
					//break;
				}
			}
			invokeRedraw();
		}
	}
	
	void removeClusterFromWorkspace()
	{
		if (m_CDisplay.getNumSelectedClusters() > 0)
		{
			if (JOptionPane.showConfirmDialog(
					frame,
					"Remove selected cluster?",
					"Confirmation",
					JOptionPane.YES_NO_OPTION) == 0)
			{
				m_Framework.getHistory().saveSnapShot("Remove Cluster");
				ClustInfo[] selectedClusters = m_CDisplay.getAllSelectedClusters();
				for (int i = 0; i < selectedClusters.length; ++i) {
					int index = m_Framework.getWorkingSet().getIndexOf(selectedClusters[i]);
					m_Framework.getWorkingSet().remove(index);
				}
				selectCluster(m_Framework.getRoot());
				invokeRedraw();
			}
		}
	}
	
	
	void showOperatorCluster()
	{
		setCurrentControlOperator(m_ControlKmeans);
	}
	
	void showOperatorQuerySignal()
	{
		ClustInfo cInfo = m_CDisplay.getSelectedCluster(0);
		if (cInfo == null)
			cInfo = m_Framework.getRoot();
		m_ControlOnOff.setClustInfo(cInfo);
		//m_ControlOnOff.setClustInfo(m_Framework.getRoot());
		setCurrentControlOperator(m_ControlOnOff);
	}
	
	void showOperatorComparison()
	{
		ClustInfo[] compareSet = m_CDisplay.getAllSelectedClusters(); 
				//m_Framework.getFavorites().getSelectedClusters();
		if (compareSet != null)
		{
			m_ControlIntersection.setComparisonSet(compareSet);
			setCurrentControlOperator(m_ControlIntersection);
			selectCluster(m_ControlIntersection.getNewComparison());
			refreshHeatmap();
		}
	}
}
