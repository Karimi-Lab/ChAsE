//TODO: optimize the plot_bounds calculation (currently d^2 * n), and call less often.
package still.gui;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.text.NumberFormat;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import processing.core.PFont;
import processing.core.PGraphics;
import processing.core.PImage;
import processing.core.PVector;
import still.data.FloatIndexer;
import still.data.Operator;
import still.data.TableEvent;
import still.gui.metadataviewer.MetadataImageViewer;
import still.gui.metadataviewer.MetadataViewer;
import still.operators.HeatMapOp;
import still.operators.HeatMapOp.GroupClusteringMethod;
import still.operators.HeatMapOp.GroupInfo;
import still.operators.HeatMapOp.GroupProfile;

import controlP5.*;

public class PHeatMapPainter extends OPApplet implements ChangeListener, MouseWheelListener {
	
	class MouseState implements Cloneable
	{
		static final int BUTTON_LEFT 	= 0;
		static final int BUTTON_RIGHT 	= 1;
		static final int STATE_PRESSED 	= 0;
		static final int STATE_RELEASED = 1;
		static final int STATE_DRAGGING = 2;
		
		int 	m_StartX = -1;
		int 	m_StartY = -1;
		int 	m_EndX   = -1;
		int 	m_EndY   = -1;
		int 	m_Button = -1;
		int		m_State  = -1;
		
		public Object clone() {
			try {
				return super.clone();
			} catch(Exception e) {
				return null; 
			}
		}
		
		public boolean isStartIn(double x, double y, double w, double h)
		{
			return (m_StartX >= Math.min(x, x + w) && m_StartX <= Math.max(x, x + w) &&
					m_StartY >= Math.min(y, y + h) && m_StartY <= Math.max(y, y + h));
		}
		
		public boolean isEndIn(double x, double y, double w, double h)
		{
			return (m_EndX >= Math.min(x, x + w) && m_EndX <= Math.max(x, x + w) &&
					m_EndY >= Math.min(y, y + h) && m_EndY <= Math.max(y, y + h));
		}
	};
	MouseState	m_CurrMouseState;

	final int 				BORDER_HM_L = 75; // heatmap left border
	final int 				BORDER_HM_R = 25; // heatmap right border
	final int 				BORDER_HM_T = 70; // heatmap top border
	final int 				BORDER_HM_B = 60; // heatmap bottom border
	final int				BORDER_SORT_T = 5; // sort region top border
	
	int[] 					m_HMViewCoords = new int[4]; ///< heatmap view coordinates {left, top, right, bottom}
	int						m_iHMViewW = 0; ///< heatmap view width
	int 					m_iHMViewH = 0; ///< heatmap view height
	
	double 					m_dViewOffset = 0.0; // the offset ratio of the starting row in the current heatmap view
	double 					m_dViewRange  = 1.0; // ratio of the entire table rows, shown in the current heatmap view
	PGraphics 				m_PGxHeatMap = null; // caches rendered heatmap (performance)
	boolean					m_bUpdatePGxHeatmap = true; // whether the heatmap requires updating
	double []				m_MouseOverProfile = null;	// profile for the rows under the cursor
	double []				m_SelectedProfile = null; // profile for the selected rows
	double 					m_dMinVal = 0; // minimum value across the whole table
	double 					m_dMaxVal = 0; // maximum value across the whole table 

	int 					m_iMagnifiedPoint 		= -1;
	
	public int 		  	  	m_iMetaDataColIndex  	= -1;
	public boolean 		  	m_bShowMetaData  		= true;
	public MetadataViewer 	m_Viewer 				= new MetadataImageViewer(); //TODO: will be replaced with a general Factory.
	public JPanel  		  	m_MetaDataPanel = null;
	public double 			m_dContrast = 0;

	enum HeatMapUIMode
	{
		PAN,
		ZOOM,
		SELECT_RANGE,
		SELECT_CLUSTER,
	};
	
	public PHeatMapPainter(Operator op) {
		super(op);		
				
		addMouseWheelListener(this);
		
		updateColorSelectionCol();
		updateHeatMapViewer();
		
		m_CurrMouseState = new MouseState();
	}
	
	public void invokeRedraw(boolean bUpdatePGxHeatmap)
	{// to be able to monitor where and when the redraw is being invoked.
		m_bUpdatePGxHeatmap = m_bUpdatePGxHeatmap || bUpdatePGxHeatmap;
		if (bUpdatePGxHeatmap)
		{
			loop();   // making sure the redraw will be called even in the case draw() is being executed already.
					  // as redraw() has no effect when draw() is already being executed.
		}
		redraw(); 
	}
	
	public void showMetaData(boolean bShow)
	{
		m_bShowMetaData = bShow;
		invokeRedraw(true);
	}
	
	public void heavyResize()
	{
		if (controlP5 != null)
		{
			Controller c = controlP5.controller("panelMain");
			if (c != null)
			{
				c.setSize(width, height - (int)c.position().y());
			}
		}

		calcHMViewCoords();
	}
	
	public void componentResized(ComponentEvent e)
	{
	    SwingUtilities.invokeLater(new Runnable() {
		   public void run() {
			   // Set the preferred size so that the layout managers can handle it
			   heavyResize();
			   invokeRedraw(true);
		   }
    	});
	}
	
	public void setMetaDataPanel(JPanel panel)
	{
		m_MetaDataPanel = panel; 
		if (m_MetaDataPanel != null)
		{
			m_Viewer.buildGUI(m_MetaDataPanel);
		}
	}
	
	public void updateHeatMapViewer()
	{
		m_iMetaDataColIndex = m_Viewer.processData(getOp(), this);
		if (m_MetaDataPanel != null)
		{
			m_Viewer.buildGUI(m_MetaDataPanel);
		}
	}
	
	@Override
	public void actionPerformed(ActionEvent e)
	{
		numerics = getNumerics();
		countNumerics();
		updateColorSelectionCol();
		updateHeatMapViewer();
		updateSelectedProfile();
		updateMinMax();
		
	    SwingUtilities.invokeLater(new Runnable() {
								   public void run() {
								   // Set the preferred size so that the layout managers can handle it
								   heavyResize();
								   invokeRedraw(true);
								   }
								   });
	}
	
	public void setup() 
	{
		textFont(createFont("Helvetica",10),10);
		
		countNumerics();
		heavyResize();
		
		if( this.getOp() instanceof HeatMapOp )
		{
			numerics = getNumerics();
			// count the number of dimensions
			countNumerics();
			// compute the minimum size
			size(OPAppletViewFrame.MINIMUM_VIEW_WIDTH, OPAppletViewFrame.MINIMUM_VIEW_HEIGHT);
			this.setPreferredSize(new Dimension(OPAppletViewFrame.MINIMUM_VIEW_WIDTH, OPAppletViewFrame.MINIMUM_VIEW_HEIGHT));
		}
		
		heavyResize();
		
		guiCreateMain();

		this.finished_setup = true;
		
	    SwingUtilities.invokeLater(new Runnable() {
								   public void run() {
								   // Set the preferred size so that the layout managers can handle it
								   invalidate();
								   getParent().validate();
								   }
								   });

	    // prevent thread from starving everything else
		noLoop();
	}
	
	controlP5.ControlP5			controlP5;
	controlP5.PanelController 	m_GUIHeatMapPanel;
	controlP5.PanelController 	m_GUIClusterHierarchyPanel;
	controlP5.Slider			m_GUIClusterHierarchySlider;
	void guiCreateMain()
	{
		// color and font
		controlP5 = new ControlP5(this);
		PFont p = createFont("Verdana",10); 
		controlP5.setControlFont(p, 10);
		controlP5.setColorLabel(color(0));
		controlP5.setColorBackground(200);
		controlP5.setColorForeground(140);

		// main panel
		PanelController parentPanel = new PanelController(controlP5, "panelMain", 0, 20, width, height - 20);
		parentPanel.setColorBackground(0x01000000); // transparent background
		
		// left and right panels
		SplitPanelController heatmapSplit = new controlP5.SplitPanelController(controlP5, "heatmapSplit", 0, 0, parentPanel.getWidth(), parentPanel.getHeight());
		heatmapSplit.setDividerLocation(0.2);
		heatmapSplit.getPane(SplitPanelController.LEFT).setColorBackground(0x01000000);
		heatmapSplit.getPane(SplitPanelController.RIGHT).setColorBackground(0x01000000);
		parentPanel.addToLayout(heatmapSplit, PanelController.ANCHOR_ALL);
		m_GUIClusterHierarchyPanel = heatmapSplit.getPane(SplitPanelController.LEFT);
		m_GUIHeatMapPanel = heatmapSplit.getPane(SplitPanelController.RIGHT);
		
		// scroller on the left panel.
		m_GUIClusterHierarchySlider = controlP5.addSlider("scrollCH", 0, 100, 0, 	10, m_GUIClusterHierarchyPanel.getHeight() - 30, m_GUIClusterHierarchyPanel.getWidth() - 20, 15);
		m_GUIClusterHierarchySlider.setSliderMode(Slider.FLEXIBLE);
		m_GUIClusterHierarchySlider.captionLabel().setVisible(false);
		m_GUIClusterHierarchySlider.valueLabel().setVisible(false);
		m_GUIClusterHierarchyPanel.addToLayout(m_GUIClusterHierarchySlider, PanelController.ANCHOR_ALL & ~PanelController.ANCHOR_TOP);
		
		//guiCreateNavigation();
		
//		controlP5
//		m_GUIHeatMapPanel.addTab("Clusters");
		
		guiCreateTabs();
	}
	
	void guiCreateTabs()
	{
		controlP5.tab("default").setLabel("HeatMap");
		controlP5.tab("default").activateEvent(true);
		controlP5.tab("default").setId(DrawMode.HEAT_MAP.ordinal());
		controlP5.tab("default").setWidth(70);
		controlP5.tab("default").setHeight(20);
		
		controlP5.addTab("Group");
		controlP5.tab("Group").activateEvent(true);
		controlP5.tab("Group").setId(DrawMode.GROUP_SIMILARITY.ordinal());
		controlP5.tab("Group").setWidth(70);
		controlP5.tab("Group").setHeight(20);
		
		controlP5.addTab("Cluster");
		controlP5.tab("Cluster").activateEvent(true);
		controlP5.tab("Cluster").setId(DrawMode.CLUSTER_SIMILARITY.ordinal());
		controlP5.tab("Cluster").setWidth(70);
		controlP5.tab("Cluster").setHeight(20);

		controlP5.addTab("Sorted");
		controlP5.tab("Sorted").setLabel("Clusters");
		controlP5.tab("Sorted").activateEvent(true);
		controlP5.tab("Sorted").setId(DrawMode.SORTED_SIMILARITY.ordinal());
		controlP5.tab("Sorted").setWidth(70);
		controlP5.tab("Sorted").setHeight(20);
	}

//	controlP5.DropdownList 		m_DropDownNavigate;
//	void guiCreateNavigation()
//	{
//		// drop down list for the navigation modes
//		m_DropDownNavigate = controlP5.addDropdownList("Mode",					400, 20, 70, 120);
//		m_DropDownNavigate.setItemHeight(15);
//		m_DropDownNavigate.setBarHeight(15);
//		m_DropDownNavigate.captionLabel().set("Navigation");
//		m_DropDownNavigate.addItem("select",0);
//		m_DropDownNavigate.addItem("pan",1);
//		m_DropDownNavigate.addItem("zoom",2);
//		m_DropDownNavigate.moveTo("default");
//	}

	public void controlEvent(controlP5.ControlEvent e)
	{
		
		ControllerInterface ci = e.isGroup() ? e.group() : e.isController() ? e.controller() : e.tab();
		if (e.isTab())
		{
			//println("tab : "+theControlEvent.tab().id()+" / "+theControlEvent.tab().name());
			m_CurrDrawMode = DrawMode.values()[e.tab().id()];
		}
		else
		{
//			if (e.isGroup() && e.group() == m_DropDownNavigate)
//			{
//				controlEventsNavigation(e);
//			} else
			if (ci.getWindow() == m_GUIClusterControlWindow)
			{
				controlEventsCluster(e);
			}
			else if (ci.getWindow() == m_GUIHeatMapControlWindow)
			{
				controlEventsHeatMap(e);
			}
			//println("controller : "+theControlEvent.controller().id());
		}
	}
	
	public void controlEventsCluster(controlP5.ControlEvent e)
	{
		HeatMapOp opHM = (HeatMapOp) this.getOp();
		if (e.isController() && e.controller() == m_GUIClusterButton && m_iActiveGroup >= 0)
		{
			switch ((int)m_GUIClusterDropDown.value())
			{
				case 0:
					opHM.m_GroupClusteringMethod = GroupClusteringMethod.PEAK_OFFSET;
					opHM.initGroupCluster(m_iActiveGroup);
					opHM.clusterGroup(m_iActiveGroup, 2);						
					break;
				case 1:
					opHM.m_GroupClusteringMethod = GroupClusteringMethod.PEAK_OFFSET;
					opHM.clusterSelected(m_iActiveGroup, (int)m_GUINumClusterSlider.value());
					break;
				case 2:
					opHM.m_GroupClusteringMethod = GroupClusteringMethod.PEAK_VALUE;
					opHM.clusterSelected(m_iActiveGroup, (int)m_GUINumClusterSlider.value());
					break;
			}
			invokeRedraw(false);
		}
	}
	
	public void controlEventsHeatMap(controlP5.ControlEvent e)
	{
		HeatMapOp opHM = (HeatMapOp) this.getOp();
		if (e.isGroup() && e.group() == m_GUIHeatMapSortDropDown && m_iActiveGroup >= 0)
		{
			opHM.sortRowsByGroupIndex(m_iActiveGroup, HeatMapOp.GroupSortType.values()[(int)m_GUIHeatMapSortDropDown.value()]);
		}
		else if (e.controller() == m_GUIHeatMapContrastSlider)
		{
			m_dContrast = m_GUIHeatMapContrastSlider.value();
			invokeRedraw(true);
		}
	}

//	public void controlEventsNavigation(controlP5.ControlEvent e)
//	{
//		m_NavigationMode = NavigationMode.values()[(int)m_DropDownNavigate.value()];
//	}
	

	ControlWindow 				m_GUIClusterControlWindow;
	controlP5.Button 			m_GUIClusterButton;
	controlP5.DropdownList 		m_GUIClusterDropDown;
	controlP5.Slider			m_GUINumClusterSlider;
	void guiShowClusterControlWindow(int x, int y)
	{
		//HeatMapOp opHM = (HeatMapOp) this.getOp();

		if (m_GUIClusterControlWindow != null && (m_GUIClusterControlWindow.currentTab() == null || !m_GUIClusterControlWindow.isVisible()))
		{
			m_GUIClusterControlWindow.clear();
			m_GUIClusterControlWindow = null;
		}
		int windowX = x + 10 + getLocationOnScreen().x;
		int windowY = y + getLocationOnScreen().y;
		
		if (m_GUIClusterControlWindow == null)
		{
			m_GUIClusterControlWindow = controlP5.addControlWindow("ClusterControlWindow", windowX, windowY, 200, 100);
			m_GUIClusterControlWindow.hideCoordinates();
			m_GUIClusterControlWindow.setBackground(color(128));
			
			m_GUINumClusterSlider = controlP5.addSlider("numClusters", 1, 50, 2,	10, 40, 120, 15);
			m_GUINumClusterSlider.moveTo(m_GUIClusterControlWindow);
			m_GUINumClusterSlider.captionLabel().setVisible(false);
			m_GUINumClusterSlider.setColorValueLabel(0);
			m_GUINumClusterSlider.setNumberOfTickMarks(50);
			m_GUINumClusterSlider.setSliderMode(Slider.FLEXIBLE);
			m_GUINumClusterSlider.setDecimalPrecision(0);

			m_GUIClusterButton  = controlP5.addButton("clusterSelected", 0.f, 	60, 80, 70, 15);
			m_GUIClusterButton.setCaptionLabel("Cluster");
			m_GUIClusterButton.moveTo(m_GUIClusterControlWindow);
			
			m_GUIClusterDropDown  = controlP5.addDropdownList("clusterMethod",		10, 30,  120, 120);
			m_GUIClusterDropDown.moveTo(m_GUIClusterControlWindow);
			m_GUIClusterDropDown.captionLabel().set("Clustering");
			m_GUIClusterDropDown.setItemHeight(15);
			m_GUIClusterDropDown.setBarHeight(15);
			m_GUIClusterDropDown.addItem("Zero Out",0);
			m_GUIClusterDropDown.addItem("Peak Offset",1);
			m_GUIClusterDropDown.addItem("Peak Value",2);
			m_GUIClusterDropDown.addItem("Kmeans",3);
			m_GUIClusterDropDown.addItem("Chromosome",4);			
		}
		m_GUIClusterControlWindow.show();
		m_GUIClusterControlWindow.setTitle("Cluster");
		//m_GUIClusterControlWindow.setLocation(windowX, windowY);
	}
	
	ControlWindow 				m_GUIHeatMapControlWindow;
	controlP5.Slider			m_GUIHeatMapContrastSlider;
	controlP5.DropdownList 		m_GUIHeatMapSortDropDown;
	void guiShowHeatMapControlWindow(int iGroup, int x, int y)
	{
		HeatMapOp opHM = (HeatMapOp) this.getOp();
		//GroupInfo gi = opHM.getGroupInfo(iGroup);

		if (m_GUIHeatMapControlWindow != null && (m_GUIHeatMapControlWindow.currentTab() == null || !m_GUIHeatMapControlWindow.isVisible()))
		{
			m_GUIHeatMapControlWindow.clear();
			m_GUIHeatMapControlWindow = null;
		}
		
		if (m_GUIHeatMapControlWindow == null)
		{
			m_GUIHeatMapControlWindow = controlP5.addControlWindow("HeatMapControlWindow", x + 10 + getLocationOnScreen().x, y + getLocationOnScreen().y, 200, 100);
			m_GUIHeatMapControlWindow.hideCoordinates();
			m_GUIHeatMapControlWindow.setBackground(color(128));
			
			m_GUIHeatMapContrastSlider = controlP5.addSlider("contrast", -2.f, 2.f, (float)m_dContrast, 	10, 40, 120, 15);
			m_GUIHeatMapContrastSlider.setSliderMode(Slider.FLEXIBLE);
			m_GUIHeatMapContrastSlider.setColorValueLabel(0);
			m_GUIHeatMapContrastSlider.moveTo(m_GUIHeatMapControlWindow);
			
			m_GUIHeatMapSortDropDown  = controlP5.addDropdownList("sortType",		10, 30,  120, 120);
			m_GUIHeatMapSortDropDown.moveTo(m_GUIHeatMapControlWindow);
			m_GUIHeatMapSortDropDown.captionLabel().set("Sort");
			m_GUIHeatMapSortDropDown.setItemHeight(15);
			m_GUIHeatMapSortDropDown.setBarHeight(15);
			m_GUIHeatMapSortDropDown.addItem("None",0);
			m_GUIHeatMapSortDropDown.addItem("Average",1);
			m_GUIHeatMapSortDropDown.addItem("Peak Value",2);
			m_GUIHeatMapSortDropDown.addItem("Peak Offset",3);
		}
		m_GUIHeatMapControlWindow.show();
		m_GUIHeatMapControlWindow.setTitle(opHM.getGroupName(iGroup));
		m_GUIHeatMapContrastSlider.setBroadcast(false);
		m_GUIHeatMapContrastSlider.setValue((float)m_dContrast);
		m_GUIHeatMapContrastSlider.setBroadcast(true);
		
	}
	
	public void calcHMViewCoords()
	{
		int mainx = (int)(m_GUIHeatMapPanel != null ? m_GUIHeatMapPanel.position().x() : 0);
		int mainy = (int)(m_GUIHeatMapPanel != null ? m_GUIHeatMapPanel.position().y() : 0);
		int mainw = (int)(m_GUIHeatMapPanel != null ? m_GUIHeatMapPanel.getWidth() : width);
		int mainh = (int)(m_GUIHeatMapPanel != null ? m_GUIHeatMapPanel.getHeight() : height);
		
		m_HMViewCoords[0] = mainx + BORDER_HM_L; 
		m_HMViewCoords[1] = mainy + BORDER_HM_T;
		m_HMViewCoords[2] = mainx + mainw - BORDER_HM_R;
		m_HMViewCoords[3] = mainy + mainh - BORDER_HM_B;
		m_iHMViewW = m_HMViewCoords[2] - m_HMViewCoords[0];
		m_iHMViewH = m_HMViewCoords[3] - m_HMViewCoords[1];
	}
	
	boolean m_bIsDrawing = false;
	
	enum DrawMode
	{
		HEAT_MAP,
		GROUP_SIMILARITY, 
		CLUSTER_SIMILARITY,
		SORTED_SIMILARITY
	};
	DrawMode m_CurrDrawMode = DrawMode.HEAT_MAP;
	
	enum NavigationMode
	{
		SELECT_RANGE,
		PAN,
		ZOOM,
		SELECT_CLUSTER
	};
	NavigationMode m_NavigationMode = NavigationMode.SELECT_RANGE;
	
	MouseState m_DrawMouseState;
	public synchronized void draw()
	{
		noLoop();
		
		m_DrawMouseState = (MouseState) m_CurrMouseState.clone();
		m_CurrMouseState.m_State = -1; // to indicate that the mouse state has been processed
		
		m_bIsDrawing = true;
		//background(128 + 64 + 32);
		background(255);

		while (this.getOp().isUpdating()) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		try
		{
			switch (m_CurrDrawMode)
			{
				case HEAT_MAP:
					drawHeatMap();
					break;
				case GROUP_SIMILARITY:
					drawGroupSimilarities();
					break;
				case CLUSTER_SIMILARITY:
					drawClusterSimilarities();
					break;
				case SORTED_SIMILARITY:
					drawClusterCombinations();
					//drawSortedSimilarities();
					break;
			}
		}
		catch (Exception e)
		{
			System.out.println("Exception: PHeatMapPainter.draw()");
			e.printStackTrace();
		}

		m_bIsDrawing = false;
		textAlign(LEFT, BOTTOM);
	}
	
	int m_iActiveGroup = 0; // the active group on which the cluster operations will be performed
	
	void drawHeatMap()
	{
		boolean bEventOccured = false;
    	if( this.getOp() instanceof HeatMapOp)
    	{
    		calcHMViewCoords();
    		//fill(0);
			//drawMagnifiedPlot();
    		stroke(0);
	 		fill(255);
	 		beginShape();
	 		vertex(m_HMViewCoords[0] - 1, m_HMViewCoords[1] - 1);
	 		vertex(m_HMViewCoords[2] + 1, m_HMViewCoords[1] - 1);
	 		vertex(m_HMViewCoords[2] + 1, m_HMViewCoords[3] + 1);
	 		vertex(m_HMViewCoords[0] - 1, m_HMViewCoords[3] + 1);
	 		endShape(CLOSE);
			
	 		if (m_iHMViewW <= 0 || m_iHMViewH <= 0)
	 			return;
	 		
	 		PGraphics pg = createGraphics(m_iHMViewW, m_iHMViewH, P3D);
			pg.beginDraw();
			pg.translate(-m_HMViewCoords[0], -m_HMViewCoords[1]);
			
	    	float fTextHeight = textAscent() + textDescent();
	 		textAlign(CENTER);
			fill(255, 0, 0);
			
			if (m_iMagnifiedPoint != -1 && m_iMetaDataColIndex != -1 && m_bShowMetaData)
			{// draw the picked point in detailed view
				m_Viewer.drawPointDetailed(pg, m_iMagnifiedPoint, m_HMViewCoords[0] , m_HMViewCoords[1], m_iHMViewW, m_iHMViewH);
				String sMetaData = op.getMetaData(m_iMetaDataColIndex, m_iMagnifiedPoint);
				PVector xlabelpos = new PVector(0.5f*(m_HMViewCoords[0] + m_HMViewCoords[2]) ,m_HMViewCoords[3] + 1*fTextHeight);
				text(sMetaData, xlabelpos.x, xlabelpos.y);
			}
			
			drawCluserHierarchy();

			try
			{
				updateHeatMapGx(pg);
			}
			catch (Exception e)
			{
				System.out.println("Exception: PHeatMapPainter.drawHeatMap()");
				e.printStackTrace();
			}
			
			pg.endDraw();
			imageMode(CORNER);
			noSmooth();
			image(pg, m_HMViewCoords[0], m_HMViewCoords[1]);
			
			boolean bMouseStartInHeatMap = m_DrawMouseState.isStartIn(m_HMViewCoords[0], m_HMViewCoords[1], m_iHMViewW, m_iHMViewH); 
			
			if (bMouseStartInHeatMap && m_DrawMouseState.m_State == MouseState.STATE_DRAGGING && m_NavigationMode == NavigationMode.SELECT_RANGE)
			{// draw the selection box
				fill(255, 0, 0, 64);
				stroke(255, 0, 0, 128);
				rect(m_HMViewCoords[0], m_DrawMouseState.m_StartY, m_HMViewCoords[2] - m_HMViewCoords[0], m_DrawMouseState.m_EndY - m_DrawMouseState.m_StartY);
			}
			
			HeatMapOp opHM = (HeatMapOp) this.getOp();
			float fGroupW = 1.0f * m_iHMViewW / opHM.m_iNumGroups;

			int iMouseOverCol   = -1; // index of the column under the cursor
			int iMouseOverGroup = -1; // index of the group under the cursor
			
			if (m_DrawMouseState.m_StartX >= m_HMViewCoords[0] && m_DrawMouseState.m_StartX < m_HMViewCoords[2])
			{
				iMouseOverCol = getNumHeatMapCols() * (m_DrawMouseState.m_StartX - m_HMViewCoords[0]) / (m_HMViewCoords[2] - m_HMViewCoords[0]);
				iMouseOverGroup = getHeatMapColDim(iMouseOverCol) / opHM.m_iGroupDims;
			}
			
			// draw the cursor position and the dimension name
			if (bMouseStartInHeatMap && m_DrawMouseState.m_State != MouseState.STATE_DRAGGING)
			{
				textAlign(CENTER, BOTTOM);
				int xCol = m_HMViewCoords[0] + iMouseOverCol * (m_HMViewCoords[2] - m_HMViewCoords[0]) / getNumHeatMapCols();
				String colName = this.getOp().input.getColName(getHeatMapColDim(iMouseOverCol));
				if (colName != null)
				{
					String sGroupName = opHM.getGroupName(iMouseOverGroup);
					if (sGroupName != null && sGroupName.length() < colName.length())
					{
						colName = colName.substring(sGroupName.length()); // subtract the common group name from the column name
					}
					text(colName, xCol, m_HMViewCoords[1] - 2);
				}
				stroke(0, 64);
				line(xCol, m_HMViewCoords[1], xCol, m_HMViewCoords[3]);
			}
			
			if (m_iActiveGroup >= 0)
			{
				stroke(255, 0, 0); //red
				noFill();
				rect(m_HMViewCoords[0] +  m_iActiveGroup * fGroupW, m_HMViewCoords[1], fGroupW, m_iHMViewH); 
			}
			
			if (opHM.m_iSortGroupIndex != -1)
			{// draw an arrow indicating the group sorted by
				float fArrowX = m_HMViewCoords[0] + (opHM.m_iSortGroupIndex + 0.5f) * fGroupW;
				float fArrowY = m_HMViewCoords[1] - 5;
	    		stroke(128, 0, 0);
		 		fill(255, 0, 0);
		 		beginShape();
		 		vertex(fArrowX, fArrowY);
		 		vertex(fArrowX + 10, fArrowY - 10);
		 		vertex(fArrowX - 10, fArrowY - 10);
		 		endShape(CLOSE);
			}
			
			if (iMouseOverGroup != -1)
			{
				float fArrowX = m_HMViewCoords[0] + (iMouseOverGroup + 0.5f) * fGroupW;
				float fArrowY = m_HMViewCoords[1] - 5;
	    		stroke(128, 0, 0);
		 		noFill();
		 		beginShape();
		 		vertex(fArrowX, fArrowY);
		 		vertex(fArrowX + 10, fArrowY - 10);
		 		vertex(fArrowX - 10, fArrowY - 10);
		 		endShape(CLOSE);
			}

			// draw the group labels on the top
			for (int g1 = 0; g1 < opHM.m_iNumGroups; g1++)
			{
				String sGroupName = opHM.getGroupName(g1);
				if (sGroupName == null)
					continue;
				
				textAlign(LEFT, BOTTOM);
				if (g1 == m_iActiveGroup)
					fill(255, 0, 64);
				else
					fill(0, 0, 64);
				
				float gw = 1.f * (m_HMViewCoords[2] - m_HMViewCoords[0]) / opHM.m_iNumGroups;
				float gx = m_HMViewCoords[0] + g1 * gw;
				
				pushMatrix();
					translate(gx + 0.1f * gw, m_HMViewCoords[1] - 15);
					rotate(-PI/10);
					text(sGroupName, 0, 0);
				popMatrix();

				//unfocus the non-active groups
				boolean bUnfocus = false;
				if (bUnfocus && g1 != m_iActiveGroup)
				{
					noStroke();
					fill(64, 128);
					rect(gx, m_HMViewCoords[1], gw, m_iHMViewH);
				}
			}
			
			
			// draw the scroll bar
			stroke(64);
			fill(160);
			rect(m_HMViewCoords[0] - 10, m_HMViewCoords[1] - 1, 7, m_iHMViewH + 2);
			fill(220);
			rect(m_HMViewCoords[0] - 9, m_HMViewCoords[1] - 1 + (float)m_dViewOffset * m_iHMViewH, 5, m_iHMViewH * (float)m_dViewRange + 2);
			
			
//			if (selectionOn)
//			{// done with the selection
//				selectionOn = false;
//				m_iSelectYMin = -1;
//				m_iSelectYMax = -1;
//			}			
		}
    	
    	m_bUpdatePGxHeatmap = false;
    	if (bEventOccured)
    	{
    		invokeRedraw(true);
    	}
	}
	
	
	PImage createHeatmapImage(int[] rows, int[] cols, int imageWidth, int imageHeight)// , options?
	{
		int iw = Math.min(imageWidth, cols.length);
		int ih = Math.min(imageHeight, rows.length);
		PImage img = new PImage(iw, ih, RGB);
		
		img.loadPixels(); // is this required?
		
//		for (int y = 0; y < ih; y++)
//		for (int x = 0; x < iw; x++)
//		{
//			img.pixels[y * iw + x] = 0xFF00FF;
//		}
//		if (false)
//		{
//			img.updatePixels();
//			return img;
//		}

		
		double [][][] dColor = new double[iw][ih][3];
		double fBlend[] = new double[2]; // blend ratios when a row falls in between two pixels, to avoid aliasing artifacts.
		for( int irow = 0; irow < rows.length; irow++)
		{
			double fY1 = 1.0 * irow * ih/rows.length;
			double fY2 = 1.0 * (irow+1) * ih/rows.length;
			int iY = ((int)fY1);
			if (iY == ((int)fY2))
			{
				fBlend[0] = 1.0;
				fBlend[1] = 0.0;
			}
			else
			{
				fBlend[0] = (((int)fY2) - fY1) / (fY2 - fY1);
				fBlend[1] = 1.0 - fBlend[0];
			}
			fBlend[0] *= (1.0 * ih / rows.length);
			fBlend[1] *= (1.0 * ih / rows.length);
			
			//colorCol
			int clr = color(0, 0, 0);
			if (m_iSelectionCol != -1 && getOp().getMeasurement(rows[irow], m_iSelectionCol) > 0)
			{
				clr = color(255, 0, 0);
			}
			else if (m_iColorCol != -1)
			{
				clr = (int)getOp().getMeasurement(rows[irow], m_iColorCol);
			}
			
			for (int icol = 0; icol < cols.length; icol++)
			{
				double dVal = getOp().getMeasurement(rows[irow], cols[icol]);
				if (dVal < 0)
				{//TODO: investigate why there might ever be a negative value?
					dVal = 0;
				}

				int iX = icol * iw / cols.length;

				for (int ch = 0; ch < 3; ch++) // color channels
				{
					float chVal = ch == 0 ? red(clr) : ch == 1 ? green(clr) : blue(clr);
					for (int r = 0; r < 2; r++)
					{
						if (iY + r < ih)
							dColor[iX][iY + r][ch] += dVal * fBlend[r] * (255 - chVal);
					}
				}
			}
		}
		m_dMaxVal = 10.0;
		boolean bLogScale = false;
		double dScaleCoeff = 255.0 / (bLogScale ? Math.log(m_dMaxVal) : m_dMaxVal);
    	double dMultiplier = Math.pow(100, m_dContrast);

		img.loadPixels();
	    for (int y = 0; y < ih; y++)
	    {
		    for (int x = 0; x < iw; x++)
		    {
		    	int clr = bLogScale ? color(255-(int)(dScaleCoeff * Math.log(dColor[x][y][0]+0.00001)),
		    				    		    255-(int)(dScaleCoeff * Math.log(dColor[x][y][1]+0.00001)),
		    				    		    255-(int)(dScaleCoeff * Math.log(dColor[x][y][2]+0.00001)))
		    				    	: color(255-(int)(dScaleCoeff * (dMultiplier * dColor[x][y][0]+0.00001)),
		    				    		    255-(int)(dScaleCoeff * (dMultiplier * dColor[x][y][1]+0.00001)),
		    				    		    255-(int)(dScaleCoeff * (dMultiplier * dColor[x][y][2]+0.00001)));
		    	img.pixels[y * iw + x] = clr;
		    }
		}
		img.updatePixels();
		
		//img.resize(arg0, arg1);
		return img;
	}
	
	int iMinViewIndexNext = -1;
	int iNumViewNext = -1;

	// caches the heatmap to improve performance
	void updateHeatMapGx(PGraphics pgHM)
	{
		if (pgHM == null|| getNumHeatMapCols() == 0)
			return;
				
		boolean bUpdatePGxHeatmap = m_bUpdatePGxHeatmap;

		boolean bMouseDragInHeatMap = (m_DrawMouseState.m_StartX > m_HMViewCoords[0] &&
		                               m_DrawMouseState.m_StartX < m_HMViewCoords[2] &&
		                               m_DrawMouseState.m_StartY > m_HMViewCoords[1] &&
		                               m_DrawMouseState.m_StartY < m_HMViewCoords[3]);

		int iDataSize = getOp().rows();

		int iMinViewIndexCurr = Math.max(0, (int) (m_dViewOffset * iDataSize));
		int iNumViewCurr = Math.min(Math.max((int) (m_dViewRange * iDataSize), m_iHMViewH), iDataSize - iMinViewIndexCurr);
		
		iMinViewIndexNext = iMinViewIndexCurr;
		iNumViewNext = iNumViewCurr;

		boolean bSelectRange = false;
		int iSelectYMin = -1;
		int iSelectYMax = -1;
		
		if (bMouseDragInHeatMap && 
			(m_DrawMouseState.m_State == MouseState.STATE_DRAGGING || m_DrawMouseState.m_State == MouseState.STATE_RELEASED))
		{
			bUpdatePGxHeatmap = bUpdatePGxHeatmap | m_DrawMouseState.m_State == MouseState.STATE_RELEASED;
			
			double dViewRange = m_dViewRange;
			double dViewOffset = m_dViewOffset;
			//m_NavigationMode = NavigationMode.ZOOM;
			if (m_NavigationMode == NavigationMode.PAN)
			{
				dViewOffset -= dViewRange * (m_DrawMouseState.m_EndY - m_DrawMouseState.m_StartY) / m_iHMViewH;
			}
			else if (m_NavigationMode == NavigationMode.ZOOM)
			{
				float fScale = (float)Math.pow(2, (m_DrawMouseState.m_EndY - m_DrawMouseState.m_StartY)/100.0);
				dViewRange = Math.min(Math.max(dViewRange * fScale, 1.0 * m_iHMViewH / getOp().rows()), 1.0);
				dViewOffset = dViewOffset + (m_dViewRange - dViewRange) * (m_DrawMouseState.m_StartY - m_HMViewCoords[1]) / m_iHMViewH;
			}
			
			dViewOffset = Math.min(1.0 - dViewRange, Math.max(0.0, dViewOffset));
			iMinViewIndexNext = Math.max(0, (int) (dViewOffset * iDataSize));
			iNumViewNext = Math.min(Math.max((int) (dViewRange * iDataSize), m_iHMViewH), iDataSize - iMinViewIndexNext);
			
			if (bUpdatePGxHeatmap)
			{
				if (m_NavigationMode == NavigationMode.SELECT_RANGE)
				{
					bSelectRange = true;
					iSelectYMin = Math.min(m_DrawMouseState.m_EndY, m_DrawMouseState.m_StartY);
					iSelectYMax = Math.max(m_DrawMouseState.m_EndY, m_DrawMouseState.m_StartY);
				}
				m_dViewRange = dViewRange;
				m_dViewOffset = dViewOffset;
				iMinViewIndexCurr = iMinViewIndexNext;
				iNumViewCurr = iNumViewNext;
			}
		}
		
		
		if (iNumViewCurr <= 0)
			return;
		
		HeatMapOp opHM = (HeatMapOp) this.getOp();
	
		PGradient.init(this);
		int iMapWidth = Math.min(getNumHeatMapCols(), m_iHMViewW);
		int iMapHeight  = Math.min(iNumViewCurr, m_iHMViewH);
		
		boolean bSelectionUpdated = false;
		
		m_MouseOverProfile = new double[getNumHeatMapCols()];
		
		if (m_PGxHeatMap == null || m_PGxHeatMap.width != m_iHMViewW || m_PGxHeatMap.height != iMapHeight)
		{
			m_PGxHeatMap = createGraphics(m_iHMViewW, iMapHeight, P2D);
			bUpdatePGxHeatmap = true;
		}
		
		double 	dMaxHMVal = 0; // maximum value in the current view of the heatmap
		if (bUpdatePGxHeatmap)
		{
			dMaxHMVal = 0;
			if (bSelectRange && bMouseDragInHeatMap && m_iSelectionCol != -1 && !m_bKeyShiftPressed && !m_bKeyAltPressed)
			{
				for (int i = 0; i < iDataSize; i++)
				{
					getOp().setMeasurement(i, m_iSelectionCol, 0.0);
				}
				bSelectionUpdated = true;
			}
		}
			
		double [][][] dColor = null;
		if (bUpdatePGxHeatmap)
		{	
			dColor = new double[iMapWidth][iMapHeight][3];
		}
		double fBlend[] = new double[2]; // blend ratios when a row falls in between two pixels, to avoid aliasing artifacts.
		double dMouseOverCount = 0;
		
		for( int kk = 0; kk < iNumViewCurr; kk++)
		{
			int iPointIndex = kk + iMinViewIndexCurr;
			double fY1 = 1.0 * kk * iMapHeight/iNumViewCurr;
			double fY2 = 1.0 * (kk+1) * iMapHeight/iNumViewCurr;
			int iY = ((int)fY1);
			if (iY == ((int)fY2))
			{
				fBlend[0] = 1.0;
				fBlend[1] = 0.0;
			}
			else
			{
				fBlend[0] = (((int)fY2) - fY1) / (fY2 - fY1);
				fBlend[1] = 1.0 - fBlend[0];
			}

//			fBlend[1] = fY - iY;
//			fBlend[0] = 1.0 - fBlend[1]; 

			if (bSelectRange && bMouseDragInHeatMap && m_iSelectionCol != -1)
			{
				if (iY + m_HMViewCoords[1] < iSelectYMax && iY + m_HMViewCoords[1] > iSelectYMin)
				{
					getOp().setMeasurement(iPointIndex, m_iSelectionCol, m_bKeyAltPressed ? 0.0 : 1.0);
					bSelectionUpdated = true;
				}
				else {
					if (!m_bKeyShiftPressed && !m_bKeyAltPressed)
					{
						getOp().setMeasurement(iPointIndex, m_iSelectionCol, 0.0);
						bSelectionUpdated = true;
					}
				}
			}
			
			//colorCol
			int col = color(0, 0, 0);
			if (bUpdatePGxHeatmap)
			{
				if (m_iSelectionCol != -1 && getOp().getMeasurement(iPointIndex, m_iSelectionCol) > 0)
				{
					col = color(255, 0, 0);
				}
				else if (m_iColorCol != -1)
				{
					col = (int)getOp().getMeasurement(iPointIndex, m_iColorCol);
				}
			}
			
			int iNumCols = getNumHeatMapCols();
			
			// update the profile for rows under mouse cursor
			for (int r = 0; r < 2; r++)
			{
				if (iY + r + m_HMViewCoords[1] == mouseY)
				{// negative over cursor position
					for (int d = 0; d < iNumCols; d++)
					{
						double dVal = getOp().getMeasurement(iPointIndex, getHeatMapColDim(d));
						if (dVal < 0)
						{//TODO: investigate why there might ever be a negative value?
							dVal = 0;
						}
	
						m_MouseOverProfile[d] += fBlend[r] * dVal;
						dMouseOverCount += fBlend[r] /  getNumHeatMapCols();
					}
				}
			}
			
			if (bUpdatePGxHeatmap)
			{
				for (int d = 0; d < iNumCols; d++)
				{
					double dVal = getOp().getMeasurement(iPointIndex, getHeatMapColDim(d));
					if (dVal < 0)
					{//TODO: investigate why there might ever be a negative value?
						dVal = 0;
					}

					int iX = d * iMapWidth/getNumHeatMapCols();

					for (int ch = 0; ch < 3; ch++) // color channels
					{
						float chVal = ch == 0 ? red(col) : ch == 1 ? green(col) : blue(col);
						for (int r = 0; r < 2; r++)
						{
							if (iY + r < iMapHeight)
								dColor[iX][iY + r][ch] += dVal * fBlend[r] * (255 - chVal);
						}
						dMaxHMVal = Math.max(dMaxHMVal, dColor[iX][iY][ch]);
					}
				}
			}
		}
			
		boolean bLogScale = false;
		if (bUpdatePGxHeatmap)
		{
			double dScaleCoeff = 255.0 / (bLogScale ? Math.log(dMaxHMVal) : dMaxHMVal);
	    	double dMultiplier = Math.pow(100, m_dContrast);
			m_PGxHeatMap.loadPixels();
		    for (int y = 0; y < iMapHeight; y++)
		    {
			    for (int x = 0; x < m_iHMViewW; x++)
			    {
			    	int k = x * iMapWidth/m_iHMViewW;
			    	int clr = bLogScale ? color(255-(int)(dScaleCoeff * Math.log(dColor[k][y][0]+0.00001)),
			    				    		    255-(int)(dScaleCoeff * Math.log(dColor[k][y][1]+0.00001)),
			    				    		    255-(int)(dScaleCoeff * Math.log(dColor[k][y][2]+0.00001)))
			    				    	: color(255-(int)(dScaleCoeff * (dMultiplier * dColor[k][y][0]+0.00001)),
			    				    		    255-(int)(dScaleCoeff * (dMultiplier * dColor[k][y][1]+0.00001)),
			    				    		    255-(int)(dScaleCoeff * (dMultiplier * dColor[k][y][2]+0.00001)));
			        m_PGxHeatMap.pixels[y * m_PGxHeatMap.width + x] = clr;
			    }
			}
			m_PGxHeatMap.updatePixels();
		}
		
		if (bSelectionUpdated)
		{
			updateSelectedProfile();
		}
		
		pgHM.imageMode(CORNER);
		pgHM.noSmooth();
		pgHM.smooth = false;
		
		if (!bUpdatePGxHeatmap)
		{
			float fScale = 1.f * iNumViewCurr / iNumViewNext;
			pgHM.image(m_PGxHeatMap,  
					   m_HMViewCoords[0],
					   m_HMViewCoords[1] - (iMinViewIndexNext - iMinViewIndexCurr) * 1.f * iMapHeight / iNumViewNext,
					   m_iHMViewW,
					   iMapHeight * fScale);
			
			//pgHM.image(m_PGxHeatMap,  m_HMViewCoords[0],  m_HMViewCoords[1] + iMapHeight * fScale * 0.5f, m_iHMViewW, iMapHeight * fScale);
			
		}
		else
		{
			pgHM.image(m_PGxHeatMap,  m_HMViewCoords[0],  m_HMViewCoords[1], m_iHMViewW, iMapHeight);
		}
		
		// draw the cursor
		if (mouseY > m_HMViewCoords[1] && mouseY < m_HMViewCoords[3]) //mouseX > m_HMViewCoords[0] && mouseX < m_HMViewCoords[2]
		{
			pgHM.stroke(0);
			pgHM.line(m_HMViewCoords[0], mouseY, m_HMViewCoords[2], mouseY);
		}

    	// draw the profile at the cursor position at the bottom
		// labels
		int iProfileBaseY = m_HMViewCoords[3] + 55;
		int iProfileMaxH = 50;
		fill(255);
		stroke(0);
		rect(m_HMViewCoords[0], iProfileBaseY - iProfileMaxH, m_iHMViewW, iProfileMaxH);
		NumberFormat nf = NumberFormat.getInstance();
		nf.setMinimumFractionDigits(0);
		nf.setMaximumFractionDigits(2);
		fill(0);
		textAlign(RIGHT, TOP);
		text(nf.format(m_dMaxVal), m_HMViewCoords[0], iProfileBaseY - iProfileMaxH);

		// draw the group profiles
		if (m_dMaxVal > 0)
		{
			for (int iX = 0; iX < m_iHMViewW; iX++)
			{
				int i = iX * getNumHeatMapCols() / m_iHMViewW;
				if (m_SelectedProfile != null && i < m_SelectedProfile.length)
				{
					stroke(255, 0, 0, 128);
					line(m_HMViewCoords[0] + iX, iProfileBaseY,
						 m_HMViewCoords[0] + iX, iProfileBaseY - (float)(iProfileMaxH * Math.min(m_SelectedProfile[i], m_dMaxVal) / m_dMaxVal));
				}
				stroke(0, 200);
				line(m_HMViewCoords[0] + iX, iProfileBaseY,
					 m_HMViewCoords[0] + iX, iProfileBaseY - (float)((iProfileMaxH * m_MouseOverProfile[i]) / (m_dMaxVal * dMouseOverCount)));
			}
		}
		
		// draw vertical lines to seperate the groups
		pgHM.stroke(128);
		stroke(128);
		for (int d = 0; d < getNumHeatMapCols(); d += opHM.m_iGroupDims)
		{
			int iX = m_HMViewCoords[0] +  (d + opHM.m_iGroupDims) * m_iHMViewW / getNumHeatMapCols();
			pgHM.line (iX, m_HMViewCoords[1], iX, m_HMViewCoords[3]);
			line(iX, iProfileBaseY - iProfileMaxH, iX, iProfileBaseY);
		}
		
		// highlight the groups selected in the group cluster view
		if (opHM.m_iNumGroups > 0 && opHM.m_GroupSimilarityInfo != null)
		{
			stroke(255, 0, 0);
			float fGroupW = m_iHMViewW / opHM.m_iNumGroups;
			line(m_HMViewCoords[0] +  m_iSimilarityGroup1 * fGroupW, m_HMViewCoords[1] - 3, 
				 m_HMViewCoords[0] +  (m_iSimilarityGroup1 + 1)* fGroupW, m_HMViewCoords[1] - 3); 
			line(m_HMViewCoords[0] +  m_iSimilarityGroup2 * fGroupW, m_HMViewCoords[1] - 3, 
					 m_HMViewCoords[0] +  (m_iSimilarityGroup2 + 1)* fGroupW, m_HMViewCoords[1] - 3); 
		}

		// send the table change event to other operators
		if (bSelectionUpdated)
		{
			getOp().tableChanged(new TableEvent( getOp(), TableEvent.TableEventType.ATTRIBUTE_CHANGED, "selection", null, false), true); // downstream
			getOp().tableChanged(new TableEvent( getOp(), TableEvent.TableEventType.ATTRIBUTE_CHANGED, "selection", null, true), true); // upstream
		}
	}
	
	void drawCluserHierarchy()
	{
		HeatMapOp opHM = (HeatMapOp) this.getOp();
		GroupInfo gi = opHM.getGroupInfo(m_iActiveGroup);
		if (gi == null)
			return;

		int iconW = 20;
		int iconH = 20;
		
//		float viewL = 10;
		float viewR = m_GUIClusterHierarchyPanel.position().x() + m_GUIClusterHierarchyPanel.getWidth();
//		float viewT = m_HMViewCoords[1];
//		float viewB = m_HMViewCoords[3];
		
		float x = 10 - 10 * m_GUIClusterHierarchySlider.value();
		float y = m_HMViewCoords[1];
		float snapW = 34; // width of the snap shot
		float snapWG = 60; // gap between snap shots
		int iconBorder = 2; 
		
		int iChangeCurrClustering = gi.m_iCurrentClustering;

		float yValues[] = null;
		float yValuesPrev[] = null;
		int sortedIndex[] = null;
		int sortedIndexPrev[] = null;
		GroupInfo.Clustering gicPrev = null;
		
		int mouseOverCi = -1; // index of the cluster with mouse over it
		int mouseOverGCi = -1;
		float mouseOverCx = -1; // left coordinate of cluster with mouse over it
		float mouseOverCy = -1; // top coordinate of cluster with mouse over it

		smooth();
		for (int gc = 0; gc < gi.m_Clusterings.length; gc++)
		{
			GroupInfo.Clustering gic = opHM.getGroupInfoClustering(m_iActiveGroup, gc);
			if (gic == null)
				break;
	

			double[] clusterSizes = new double[gic.m_iNumClusters];
			//opHM.rows();
			for (int c = 0; c < gic.m_iNumClusters; c++)
			{
				clusterSizes[c] = 1.0/((gic.m_ClusterSize != null ? gic.m_ClusterSize[c] : 0) + 1.0); // so that the sort is from large size to small size
			}
			sortedIndex = FloatIndexer.sortFloats(clusterSizes);

			if (gc == gi.m_iCurrentClustering)
			{// highlight the current clustering
				stroke(0, 128, 0);
				noStroke();
				fill(128, 255, 128);
				rect(x - 10, m_HMViewCoords[1] - 10, snapW + 20, m_iHMViewH + 20);
				if (iNumViewNext != -1 && iMinViewIndexNext != -1)
				{
					fill(0, 128, 0);
					rect(x - 10, m_HMViewCoords[1] + m_iHMViewH * iMinViewIndexNext / opHM.rows(), snapW + 20, m_iHMViewH * iNumViewNext / opHM.rows());
				}
			}
			fill(255);

			y = m_HMViewCoords[1];
			yValues = new float[gic.m_iNumClusters + 1];
			yValues[0] = y;
			for (int c = 0; c < gic.m_iNumClusters; c++)
			{
				int iClusterSize = gic.m_ClusterSize != null ? gic.m_ClusterSize[sortedIndex[c]] : 0;
				float clusterH = (1.f * iClusterSize * m_iHMViewH) / opHM.rows();
				
				//int x = c * (fW + 5) + 10;
				//int y = (int)(height - fH - 10) + 5;
	
				fill(240);
				stroke(0);
				
//				if (mouseX >= x && mouseX <= x + snapW && mouseY >= y && mouseY < y + clusterH)
				if (m_DrawMouseState.isStartIn(x, y, snapW, clusterH))
				{// highlight the cluster under cursor
					mouseOverCi = c;
					mouseOverGCi = gc;
					mouseOverCx = x;
					mouseOverCy = y;
					if (gc == gi.m_iCurrentClustering)
					{
						stroke(0, 0, 200);
						fill(255, 160, 160);
					}
					
//					if (selectionOn)
					if (m_DrawMouseState.m_State == MouseState.STATE_RELEASED)
					{
						if (gc == gi.m_iCurrentClustering)
						{
							if (mouseEvent.getClickCount() == 2)
							{
								guiShowClusterControlWindow(mouseX, mouseY);
							}
							opHM.selectCluster(m_iActiveGroup, sortedIndex[c], m_bKeyShiftPressed, m_bKeyAltPressed);
							m_bUpdatePGxHeatmap = true;
							getOp().tableChanged(new TableEvent( getOp(), TableEvent.TableEventType.ATTRIBUTE_CHANGED, "selection", null, false), true); // downstream
							getOp().tableChanged(new TableEvent( getOp(), TableEvent.TableEventType.ATTRIBUTE_CHANGED, "selection", null, true), true); // upstream
							updateSelectedProfile();
							invokeRedraw(true);
						}
						else
						{
							iChangeCurrClustering = gc;
						}
					}
				}
				
				float r = 15;
				if (clusterH < r)
				{
					rect(x, y, snapW, clusterH);
				}
				else
				{// round rect:
					PUtils.drawRoundRect(g, x, y, snapW, clusterH, r, r);
				} 				
				//stroke(128);
				
				if (mouseY >= m_HMViewCoords[1] && mouseY <= m_HMViewCoords[3] && gc == gi.m_iCurrentClustering)
				{
					stroke(0);
					line(x, mouseY, x + snapW, mouseY);
				}
				
				if (clusterH >= iconH / 2 + iconBorder * 2)
				{
					int minH = (int)Math.min(iconH, clusterH - iconBorder * 2);
					rect(x + iconBorder, y + iconBorder, iconW, minH);
					PUtils.drawProfile(g, gic.m_iClusterProfile[sortedIndex[c]], 0.0, 0.5, (int)x+iconBorder, (int)y+iconBorder, iconW, minH, true);
				}
				
				y+= clusterH;
				yValues[c+1] = y; 
			}
			
			if (mouseOverGCi == gc && mouseOverCi != -1)
			{
				rect(mouseOverCx + iconBorder, mouseOverCy + iconBorder, iconW, iconH);
				PUtils.drawProfile(g, gic.m_iClusterProfile[sortedIndex[mouseOverCi]], 0.0, 0.5, (int)mouseOverCx+iconBorder, (int)mouseOverCy+iconBorder, iconW, iconH, true);
			}
			
			if (gc > 0)
			{
				noFill();
				//GroupInfo.Clustering gic0 = opHM.getGroupInfoClustering(m_iActiveGroup, gc - 1);
				for (int c1 = 0; c1 < yValuesPrev.length - 1; c1++)
				{
					for (int c2 = 0; c2 < yValues.length - 1; c2++)
					{
						int iNumShared = gic.m_PrevSimilarity.m_Count[sortedIndexPrev[c1]][sortedIndex[c2]];
						if (iNumShared > 0)
						{
							stroke(0);
							strokeWeight(2);
							if (gic.m_ClusterSize[sortedIndex[c2]] == gicPrev.m_ClusterSize[sortedIndexPrev[c1]])
							{
								stroke(64);
								strokeWeight(1);
							}
							if ((gc == mouseOverGCi && mouseOverCi == c2) ||
								((gc - 1) == mouseOverGCi && mouseOverCi == c1))
							{
								stroke(255, 0, 0);
							}
							
							float x1 = x - snapWG;
							float y1 = (yValuesPrev[c1] + yValuesPrev[c1 + 1]) / 2;
							float x2 = x;
							float y2 = (yValues[c2] + yValues[c2 + 1]) / 2;
							bezier(x1, y1, x1 + 45, y1, x2 - 45, y2, x2, y2);
							PUtils.drawArrow(g, x1, y1, x2, y2, 8, 3, false);
							strokeWeight(1);
						}
					}
				}
			}
			yValuesPrev = yValues;
			sortedIndexPrev = sortedIndex;
			gicPrev = gic;
			x += snapW + snapWG;
			if (x + snapW > viewR)
				break;
		}
		noSmooth();
		
		if (iChangeCurrClustering != gi.m_iCurrentClustering)
		{
			opHM.setCurrGroupInfoClustering(m_iActiveGroup, iChangeCurrClustering);
			opHM.sortRowsByGroupIndex(m_iActiveGroup, opHM.m_GroupSortType);
			invokeRedraw(true);
		}
	}
	
	
	int m_iSimilarityGroup1 = 0;
	int m_iSimilarityGroup2 = 0;
	
	/**
	 *  draws the similarity between all groups
	 */
	void drawGroupSimilarities()
	{
		HeatMapOp opHM = (HeatMapOp) this.getOp();
		if (opHM.m_GroupSimilarityInfo == null)
		{
			return;
		}
		
		float fLeftBorder = 100;
		float fTopBorder = 10;
		float fRightBorder = 10;
		float fBottomBorder = 100;

		float fW = 1.f * (width - fRightBorder - fLeftBorder) / opHM.m_iNumGroups; 
		float fH = 1.f * (height - fBottomBorder - fTopBorder) / opHM.m_iNumGroups; 
		
		if (fW < 2 || fH < 2)
		{// viewport too small
			return;
		}
		
		NumberFormat nf = NumberFormat.getInstance();
		nf.setMinimumFractionDigits(0);
		nf.setMaximumFractionDigits(2);

		int iMouseOver1 = -1;
		int iMouseOver2 = -1;
		for (int g1 = 0; g1 < opHM.m_iNumGroups; g1++)
		{
			for (int g2 = g1; g2 < opHM.m_iNumGroups; g2++)
			{
				double dSimilarity = opHM.calcGroupSimilarity(g1,g2);
				fill((int)(255 * dSimilarity));
				float fLeft = g1 * fW + fLeftBorder;
				float fTop = g2 * fH + fTopBorder;
				
				stroke(0);
				rect(fLeft+2, fTop+2, fW-4, fH-4);
				
				fill(0, 128, 255);
				textAlign(CENTER, CENTER);
				text(nf.format(dSimilarity), fLeft + fW/2, fTop + fH/2);
				
				//if (mouseX >= fLeft && mouseX <= fLeft + fW && mouseY >= fTop && mouseY <= fTop + fH)
				if (m_DrawMouseState.isStartIn(fLeft, fTop, fW, fH))
				{
					iMouseOver1 = g1;
					iMouseOver2 = g2;
//					if (selectionOn)
					if (m_DrawMouseState.m_State == MouseState.STATE_RELEASED)
					{
						m_iSimilarityGroup1 = g1;
						m_iSimilarityGroup2 = g2;
						invokeRedraw(false);
					}
				}
				
				if (m_iSimilarityGroup1 == g1 && m_iSimilarityGroup2 == g2)
				{
					stroke(255, 0, 0);
					noFill();
					rect(fLeft+1, fTop+1, fW-2, fH-2);
				}
			}
		}
		
		// draw the group labels
		for (int g1 = 0; g1 < opHM.m_iNumGroups; g1++)
		{
			String sGroupName = opHM.getGroupName(g1);
			if (sGroupName == null)
				continue;
			textAlign(RIGHT, CENTER);

			// labels on the bottom
			fill(0, 0, 64);
			if (m_iSimilarityGroup1 == g1) 
				fill(200, 0, 0);
			if (iMouseOver1 == g1)
				fill(0, 64, 255);

			pushMatrix();
				translate(fLeftBorder + (g1 + 0.5f) * fW, fTopBorder + opHM.m_iNumGroups * fH + 10);
				rotate(-PI/4);
				text(sGroupName, 0, 0);
			popMatrix();

			// labels on the left
			fill(0, 0, 64);
			if (m_iSimilarityGroup2 == g1) 
				fill(200, 0, 0);
			if (iMouseOver2 == g1)
				fill(0, 64, 255);
				
			pushMatrix();
				translate(fLeftBorder - 10, fTopBorder + (g1 + 0.5f) * fH);
				rotate(-PI/4);
				text(sGroupName, 0, 0);
			popMatrix();
		}
		
//		selectionOn = false;
		textAlign(LEFT, BOTTOM);
	}
	
	/**
	 *  draws the cluster similarity between two groups
	 */
	void drawClusterSimilarities()
	{
		int g1 = m_iSimilarityGroup1;
		int g2 = m_iSimilarityGroup2;
		HeatMapOp opHM = (HeatMapOp) this.getOp();
		if (!(opHM.m_GroupSimilarityInfo != null &&
			opHM.m_GroupSimilarityInfo.length > g1 &&
			opHM.m_GroupSimilarityInfo[g1] != null &&
			opHM.m_GroupSimilarityInfo[g1].length > g2 &&
			opHM.m_GroupSimilarityInfo[g1][g2] != null))
		{
			return;
		}
		int N1 = opHM.m_GroupSimilarityInfo[g1][g2].m_Similarity != null ? opHM.m_GroupSimilarityInfo[g1][g2].m_Similarity.length : 0;
		int N2 = N1 > 0 && opHM.m_GroupSimilarityInfo[g1][g2].m_Similarity[0] != null ? opHM.m_GroupSimilarityInfo[g1][g2].m_Similarity[0].length : 0;
		if (N1 == 0 || N2 == 0)
			return;
		
		float fLeftBorder = 120;
		float fTopBorder = 20;
		float fRightBorder = 20;
		float fBottomBorder = 120;

		float fW = 1.f * (width - fRightBorder - fLeftBorder) / N1; 
		float fH = 1.f * (height - fBottomBorder - fTopBorder) / N2; 
		
		if (fW < 2 || fH < 2)
		{// viewport too small
			return;
		}

		NumberFormat nf = NumberFormat.getInstance();
		nf.setMinimumFractionDigits(0);
		nf.setMaximumFractionDigits(2);
		
		int iMouseOver1 = -1;
		int iMouseOver2 = -1;
		for (int c1 = 0; c1 < N1; c1++)
		{
			for (int c2 = 0; c2 < N2; c2++)
			{
				if (g1 == g2 && c1 != c2)
					continue;
				
				float fLeft = c1 * fW + fLeftBorder;
				float fTop = c2 * fH + fTopBorder;

				if (g1 == g2)
					fLeft = fLeftBorder;

//				boolean bIsMouseOver = mouseX >= fLeft && mouseX <= fLeft + fW && mouseY >= fTop && mouseY <= fTop + fH;
				boolean bIsMouseOver = m_DrawMouseState.isStartIn(fLeft, fTop, fW, fH);
				if (bIsMouseOver)
				{
					iMouseOver1 = c1;
					iMouseOver2 = c2;
					noFill();
					stroke(255, 0, 0);
					rect(fLeft+1, fTop+1, fW-2, fH-2);
//					if (selectionOn)
					if (m_DrawMouseState.m_State == MouseState.STATE_RELEASED)
					{
						opHM.groupClusterSelection(m_iSimilarityGroup1, m_iSimilarityGroup2, c1, c2);
						m_bUpdatePGxHeatmap = true;
						getOp().tableChanged(new TableEvent( getOp(), TableEvent.TableEventType.ATTRIBUTE_CHANGED, "selection", null, false), true); // downstream
						getOp().tableChanged(new TableEvent( getOp(), TableEvent.TableEventType.ATTRIBUTE_CHANGED, "selection", null, true), true); // upstream
						updateSelectedProfile();
					}
				}

				double dValue = opHM.m_GroupSimilarityInfo[g1][g2].m_Similarity[c1][c2];
				double dMaxVisibleValue = 0.1;
				if (dValue > dMaxVisibleValue)
				{
					fill((int)(255 * dValue));
					stroke(0);
					rect(fLeft + 2, fTop + 2, fW - 4, fH - 4);
					
					fill(0, 255, 0);
					textAlign(CENTER, CENTER);
					text(nf.format(dValue), fLeft + fW/2, fTop + fH/2);
				}
				else if (bIsMouseOver)
				{// only draw
					fill(0, 100, 0);
					textAlign(CENTER, CENTER);
					text(nf.format(dValue), fLeft + fW/2, fTop + fH/2);
				}
			}
		}
//		selectionOn = false;
		
		int profileW = (int)Math.min(50, fW - 5);
		int profileH = (int)Math.min(50, fH - 5);
		
		// draw label and profiles on the bottom 
		GroupInfo.Clustering gic1 = opHM.getCurrGroupInfoClustering(g1);
		GroupInfo.Clustering gic2 = opHM.getCurrGroupInfoClustering(g2);

    	if (g1 != g2 && gic1 != null)
		{
	    	for (int c1 = 0; c1 < N1; c1++)
			{
				int x = (int)(c1*fW + (fW - profileW)/2  + fLeftBorder);
				int y = (int)(height - fBottomBorder) + 5;
	
				fill(255);
				stroke(0);
				if (iMouseOver1 == c1)
					stroke(255, 0, 0);
				rect(x, y, profileW, profileH);
				
				//stroke(128);
				PUtils.drawProfile(g, gic1.m_iClusterProfile[c1], 0.0, 1.0, x, y, profileW, profileH, true);
			}
		}
		
    	if (gic2 != null)
    	{
	    	// draw profiles on the left 
			for (int c2 = 0; c2 < N2; c2++)
			{
				int x = (int)(fLeftBorder - profileW - 5);
				int y = (int)(c2*fH + (fH - profileH)/2 + fTopBorder);
	
				fill(255);
				stroke(0);
				if (iMouseOver2 == c2)
					stroke(255, 0, 0);
				rect(x, y, profileW, profileH);
				
				//stroke(128);
				PUtils.drawProfile(g, gic2.m_iClusterProfile[c2], 0.0, 1.0, x, y, profileW, profileH, true);
			}
    	}
		
		// draw the group label on the bottom
		fill(0, 0, 200);
		String sGroupName1 = opHM.getGroupName(g1); 
		text(sGroupName1 != null ? sGroupName1 : "", (fLeftBorder + width - fRightBorder) / 2, height - fBottomBorder + 100);
		
		// draw the group label on the left
		if (g1 != g2)
		{
			String sGroupName2 = opHM.getGroupName(g2); 
			fill(0, 0, 200);
			pushMatrix();
				translate(fLeftBorder - 100, (fTopBorder + height - fBottomBorder) / 2);
				rotate(-PI/2);
				text(sGroupName2 != null ? sGroupName2 : "", 0, 0);
			popMatrix();
		}
		
		textAlign(LEFT, BOTTOM);
	}
	
	
	/**
	 * Stores the cluster pairs similarity info
	 */
	class SimilarityInfo
	{
		public int m_iNumPairs     = 0;
		public int m_Pairs[][]     = null;
		public double m_Scores[]   = null;
		public int m_SortedIndex[] = null;
	}
	
	SimilarityInfo m_Similarity = null;
	
	/**
	 * Computes the cluster pairs similarity info
	 */
	void computeSimilarityInfo()
	{
		
		HeatMapOp opHM = (HeatMapOp) this.getOp();
		if (opHM.m_GroupSimilarityInfo == null)
		{
			return;
		}

		m_Similarity = new SimilarityInfo();
		
		m_Similarity.m_iNumPairs = 0;
		// make sure all the similarities are up to date
		for (int g1 = 0; g1 < opHM.m_iNumGroups; g1++)
		{
			GroupInfo.Clustering gic1 = opHM.getCurrGroupInfoClustering(g1);
			for (int g2 = g1; g2 < opHM.m_iNumGroups; g2++)
			{
				GroupInfo.Clustering gic2 = opHM.getCurrGroupInfoClustering(g2);
				opHM.calcGroupSimilarity(g1,g2);
				m_Similarity.m_iNumPairs += gic1.m_iNumClusters * gic2.m_iNumClusters;
			}
		}
		
		m_Similarity.m_Pairs = new int[m_Similarity.m_iNumPairs][4]; // []{g1, g2, c1, c2}
		m_Similarity.m_Scores = new double[m_Similarity.m_iNumPairs];
		
		int p1 = 0;
		for (int g1 = 0; g1 < opHM.m_iNumGroups; g1++)
		{
			for (int g2 = g1 + 1; g2 < opHM.m_iNumGroups; g2++)
			{
				int N1 = opHM.m_GroupSimilarityInfo[g1][g2].m_Similarity != null ? opHM.m_GroupSimilarityInfo[g1][g2].m_Similarity.length : 0;
				int N2 = N1 > 0 && opHM.m_GroupSimilarityInfo[g1][g2].m_Similarity[0] != null ? opHM.m_GroupSimilarityInfo[g1][g2].m_Similarity[0].length : 0;
				
				for (int c1 = 0; c1 < N1; c1++)
				{
					for (int c2 = 0; c2 < N2; c2++)
					{
						m_Similarity.m_Pairs[p1][0] = g1;
						m_Similarity.m_Pairs[p1][1] = g2;
						m_Similarity.m_Pairs[p1][2] = c1;
						m_Similarity.m_Pairs[p1][3] = c2;
						m_Similarity.m_Scores[p1] = opHM.m_GroupSimilarityInfo[g1][g2].m_Similarity[c1][c2];
						p1++;
					}
				}
			}
		}
		
		m_Similarity.m_SortedIndex = FloatIndexer.sortFloatsRev(m_Similarity.m_Scores);
	}
	

	public int m_iSimilarityViewStartIndex = 0;
	/**
	 * Draws the most similar patterns sorted.
	 */
	void drawSortedSimilarities()
	{
		computeSimilarityInfo();
	
		if (m_Similarity == null)
			return;
		
		HeatMapOp opHM = (HeatMapOp) this.getOp();

		float fLeftBorder = 20;
		float fTopBorder = 120;
		float fRightBorder = 20;
		float fBottomBorder = 20;

		float fScoreBarW = 50;
		float fScoreBarH = 10;
		
		float fRowH = 50;
		float fMarkW = ((width - fRightBorder - fLeftBorder) - fScoreBarW) / opHM.m_iNumGroups;
		int iNumToShow = (int)((height - fBottomBorder - fTopBorder) / fRowH);
		
		for (int row = 0; row < iNumToShow; row++)
		{
			if (row + m_iSimilarityViewStartIndex >= m_Similarity.m_iNumPairs)
				break;
			
			int g1 = m_Similarity.m_Pairs[m_Similarity.m_SortedIndex[row + m_iSimilarityViewStartIndex]][0];
			int g2 = m_Similarity.m_Pairs[m_Similarity.m_SortedIndex[row + m_iSimilarityViewStartIndex]][1];
			int c1 = m_Similarity.m_Pairs[m_Similarity.m_SortedIndex[row + m_iSimilarityViewStartIndex]][2];
			int c2 = m_Similarity.m_Pairs[m_Similarity.m_SortedIndex[row + m_iSimilarityViewStartIndex]][3];

			GroupInfo.Clustering gic1 = opHM.getCurrGroupInfoClustering(g1);
			GroupInfo.Clustering gic2 = opHM.getCurrGroupInfoClustering(g2);
			
			float fGap = 5;
			
			int iRowY1 = (int)(row * fRowH + fTopBorder + fGap);
			int iRowY2 = (int)(iRowY1 + fRowH - 2 * fGap);
			
//			boolean bIsMouseOver = mouseX >= fLeftBorder && mouseX <= width - fRightBorder && mouseY >= iRowY1 && mouseY <= iRowY2;
			boolean bIsMouseOver = m_DrawMouseState.isStartIn(fLeftBorder, iRowY1, width - fRightBorder - fLeftBorder, iRowY2 - iRowY1);
			if (bIsMouseOver)
			{
				fill(255, 0, 0, 64);
				stroke(255, 0, 0);
				rect(fLeftBorder, iRowY1 - fGap + 1, width - fRightBorder - fLeftBorder, iRowY2 - iRowY1 + 2 * fGap - 2);
//				if (selectionOn)
				if (m_DrawMouseState.m_State == MouseState.STATE_RELEASED)
				{
					m_iSimilarityGroup1 = g1;
					m_iSimilarityGroup2 = g2;
					opHM.groupClusterSelection(g1, g2, c1, c2);
					m_bUpdatePGxHeatmap = true;
					getOp().tableChanged(new TableEvent( getOp(), TableEvent.TableEventType.ATTRIBUTE_CHANGED, "selection", null, false), true); // downstream
					getOp().tableChanged(new TableEvent( getOp(), TableEvent.TableEventType.ATTRIBUTE_CHANGED, "selection", null, true), true); // upstream
					updateSelectedProfile();
				}
			}			
			
			fill(255);
			stroke(0);
			strokeWeight(1);
			rect(g1 * fMarkW + fLeftBorder + fGap, iRowY1, fMarkW - 2*fGap, iRowY2 - iRowY1);
			PUtils.drawProfile(g, gic1.m_iClusterProfile[c1], 0.0, 0.2, 
						(int)(g1 * fMarkW + fLeftBorder + fGap), iRowY1, (int)(fMarkW - 2*fGap), iRowY2 - iRowY1, true);

			rect(g2 * fMarkW + fLeftBorder + fGap, iRowY1, fMarkW - 2*fGap, iRowY2 - iRowY1);
			PUtils.drawProfile(g, gic2.m_iClusterProfile[c2], 0.0, 0.2, 
						(int)(g2 * fMarkW + fLeftBorder + fGap), iRowY1, (int)(fMarkW - 2*fGap), iRowY2 - iRowY1, true);
			
			
			// draw the score bar
			rect(width - fRightBorder - fScoreBarW, (iRowY1 + iRowY2 - fScoreBarH) / 2, fScoreBarW, fScoreBarH);
			fill(0, 0, 100);
			rect(width - fRightBorder - fScoreBarW, (iRowY1 + iRowY2 - fScoreBarH) / 2, fScoreBarW * (float)m_Similarity.m_Scores[m_Similarity.m_SortedIndex[row + m_iSimilarityViewStartIndex]], fScoreBarH);
			
			line(fLeftBorder, iRowY1 - fGap, width - fRightBorder, iRowY1 - fGap);
		}
//		selectionOn = false;
		
		strokeWeight(3.0f);
		for (int g1 = 0; g1 < opHM.m_iNumGroups; g1++)
		{
			float fX = fLeftBorder + g1 * fMarkW;
			line(fX, fTopBorder, fX, height - fBottomBorder);
			
			String sGroupName = opHM.getGroupName(g1);
			if (sGroupName != null)
			{
				textAlign(LEFT, BOTTOM);
				fill(0, 0, 64);
				pushMatrix();
					translate(fX + 10, fTopBorder - 10);
					rotate(-PI/10);
					text(sGroupName, 0, 0);
				popMatrix();
			}

		}
		strokeWeight(1.0f);

	}
	
	void drawClusterCombinations()
	{
		HeatMapOp opHM = (HeatMapOp) this.getOp();

		if (opHM.m_CombinationInfo == null)
		{
			opHM.computeClusterCombinations();
		}

		NumberFormat nf = NumberFormat.getInstance();
		nf.setMaximumFractionDigits(1);

		float fTableLeftBorder = 50;
		float fTabelTopBorder = 250;
		float fTabelRightBorder = 20;
		float fTabelBottomBorder = 20;
		float fTableWidth = (width - fTabelRightBorder - fTableLeftBorder);
		float fTableHeight = (height - fTabelBottomBorder - fTabelTopBorder);
		
		// draw the histograms
		{
			String[] xLabels= new String[2];
			xLabels[0] = "0";
			xLabels[1] = nf.format(100.0 * opHM.m_CombinationInfo.m_HistNumClusters.length / opHM.rows()) + "%";
			
			float fHistTop = 50;
			float fHistHeight = 100;
			String xLabel = "Cluster Size"; 
			
			float fHistLeft = fTableLeftBorder;
			float fHistWidth = fTableWidth * 0.4f;
			stroke(0); fill(254);
			rect(fHistLeft, fHistTop, fHistWidth, fHistHeight);
			noStroke();	fill(64, 128, 64);
			String yLabel = "Number of Clusters\n(total: " + opHM.m_CombinationInfo.m_iNumNoneZero + ")"; 
			PUtils.drawHistogram(g, opHM.m_CombinationInfo.m_HistNumClusters, 25, fHistLeft, fHistTop, fHistWidth, fHistHeight, -1, xLabel, yLabel, xLabels);
	
			fHistLeft = fTableLeftBorder + fTableWidth * 0.55f;
			fHistWidth = fTableWidth * 0.4f;
			stroke(0); fill(254);
			rect(fHistLeft, fHistTop, fHistWidth, fHistHeight);
			noStroke();	fill(64, 128, 64);
			yLabel = "Number of Points\n(total: "+opHM.rows()+")"; 
			PUtils.drawHistogram(g, opHM.m_CombinationInfo.m_HistNumPoints, 25, fHistLeft, fHistTop, fHistWidth, fHistHeight, -1, xLabel, yLabel, xLabels);
		}
		
		float fScoreBarW = 50;
		float fScoreBarH = 10;
		
		float fRowH = 50;
		float fColW = (fTableWidth - fScoreBarW) / opHM.m_iNumGroups;
		int iNumRowsToShow = (int)(fTableHeight / fRowH); // number of rows to show 
		boolean[] bActiveRows = new boolean[opHM.rows()]; // tmp, updated for each combination: used in calcGroupProfile
		final double dProfileMax = 0.2;
		smooth();
		for (int row = 0; row < iNumRowsToShow; row++)
		{
			int iRowIndex = row + m_iSimilarityViewStartIndex;
			if (iRowIndex >= opHM.m_CombinationInfo.m_iNumNoneZero)
				break;
			
			
			int iCombIndex = opHM.m_CombinationInfo.m_SortedIndices[iRowIndex];
			for (int i = 0; i < opHM.rows(); i++)
			{
				bActiveRows[i] = opHM.m_CombinationInfo.m_CombinationsIndex[i] == iCombIndex;
			}

			float fGap = 5; // gap between rows and columns
			int iRowY1 = (int)(row * fRowH + fTabelTopBorder + fGap); // y of this row
			int iRowY2 = (int)(iRowY1 + fRowH - 2 * fGap); // y of next row
			
			boolean bIsMouseOver = m_DrawMouseState.isStartIn(fTableLeftBorder, iRowY1, width - fTabelRightBorder - fTableLeftBorder, iRowY2 - iRowY1);
			if (bIsMouseOver)
			{
				fill(255, 255, 128);
				stroke(255, 128, 0);
				rect(fTableLeftBorder, iRowY1 - fGap + 1, width - fTabelRightBorder - fTableLeftBorder + fGap, iRowY2 - iRowY1 + 2 * fGap - 2);
				if (m_DrawMouseState.m_State == MouseState.STATE_RELEASED)
				{// select/unselect the combination
					opHM.m_CombinationInfo.m_CombinationsSelected[iCombIndex] = !opHM.m_CombinationInfo.m_CombinationsSelected[iCombIndex]; 
				}
			}
			
			final float FSEL_BOX_SIZE = 10;
			stroke(0);
			noFill();
			if (opHM.m_CombinationInfo.m_CombinationsSelected[iCombIndex])
			{
				fill(0);
			}
			rect(fTableLeftBorder - FSEL_BOX_SIZE, (iRowY1+iRowY2 - FSEL_BOX_SIZE)/2, FSEL_BOX_SIZE, FSEL_BOX_SIZE);
			
			strokeWeight(1);
			// draw the row of cluster profiles for this combination
			for (int g1 = 0; g1 < opHM.m_iNumGroups; g1++)
			{
				if (opHM.getGroupInfo(g1).m_bIsSelected) // only draw the profile for the selected groups/marks
				{
					strokeWeight(1);
					GroupProfile profile = opHM.calcGroupProfile(g1, bActiveRows);
					stroke(0);
					fill(220);
					rect(g1 * fColW + fTableLeftBorder + fGap, iRowY1, fColW - 2*fGap, iRowY2 - iRowY1);
					
					strokeWeight(1);
					noStroke();
					fill(256, 128, 128);
					PUtils.drawProfile(g, profile.m_ProfileMean , 0.0, dProfileMax, 
								(int)(g1 * fColW + fTableLeftBorder + fGap), iRowY1, (int)(fColW - 2*fGap), iRowY2 - iRowY1, true);
					noFill();
					stroke(128, 0, 0);
					PUtils.drawProfile(g, profile.m_ProfileMean , 0.0, dProfileMax, 
							(int)(g1 * fColW + fTableLeftBorder + fGap), iRowY1, (int)(fColW - 2*fGap), iRowY2 - iRowY1, false);

					stroke(0, 0, 128);
					PUtils.drawProfile(g, profile.m_ProfileStdDev , 0.0, dProfileMax, 
								(int)(g1 * fColW + fTableLeftBorder + fGap), iRowY1, (int)(fColW - 2*fGap), iRowY2 - iRowY1, false);
				}
			}

			strokeWeight(1);
			
			// draw the size meter and the size %
			double dSize = opHM.m_CombinationInfo.m_CombinationsSize[iCombIndex] / opHM.rows();
			double dSizeRatio = opHM.m_CombinationInfo.m_CombinationsSize[iCombIndex] / opHM.m_CombinationInfo.m_CombinationsSize[opHM.m_CombinationInfo.m_SortedIndices[0]]; 
			// draw the score bar
			float fScoreBarX = width - fTabelRightBorder - fScoreBarW;
			float fScoreBarY = (iRowY1 + iRowY2 - fScoreBarH) / 2;
			stroke(0);
			fill(255);
			rect(fScoreBarX, fScoreBarY, fScoreBarW, fScoreBarH);
			fill(100, 100, 255);
			rect(fScoreBarX, fScoreBarY, fScoreBarW * (float)dSizeRatio, fScoreBarH);
			textAlign(LEFT, BOTTOM);
			text(nf.format(dSize * 100) + "%", fScoreBarX, fScoreBarY);
			//line(fTabelLeftBorder, iRowY1 - fGap, width - fTabelRightBorder, iRowY1 - fGap);
		}
		noSmooth();
		
		// draw the group titles
		strokeWeight(3.0f);
		for (int g1 = 0; g1 < opHM.m_iNumGroups; g1++)
		{
			float fX = fTableLeftBorder + g1 * fColW;
			//line(fX, fTopBorder, fX, height - fBottomBorder);

			fill(0, 0, 64);
			if (!opHM.getGroupInfo(g1).m_bIsSelected)
				fill(200, 200, 255);
				
			boolean bIsMouseOver = m_DrawMouseState.isStartIn(fX, fTabelTopBorder - 50, fColW, 50);
			if (bIsMouseOver)
			{
				fill(255, 0, 0);
				if (m_DrawMouseState.m_State == MouseState.STATE_RELEASED)
				{
					opHM.getGroupInfo(g1).m_bIsSelected = !opHM.getGroupInfo(g1).m_bIsSelected;
					invokeRedraw(true);
				}
			}
					
			String sGroupName = opHM.getGroupName(g1);
			if (sGroupName != null)
			{
				textAlign(LEFT, BOTTOM);
				pushMatrix();
					translate(fX + 10, fTabelTopBorder - 10);
					rotate(-PI/10);
					text(sGroupName, 0, 0);
				popMatrix();
			}

		}
		strokeWeight(1.0f);		
	}
	

		
//		for (int x = 0; x < fWidth; x++)
//		{
//			double dVal = Math.min(fHeight, (values[(int)(values.length * x / fWidth)] - dMin) * fHeight / (dMax - dMin));
//			float fX = x + fLeft;
//			float fY = (float)(fTop + fHeight - dVal);
//			if (bFilled)
//			{
//				line(fX, fY, fX, fTop + fHeight);
//			}
//		}

	void updateMinMax()
	{
		m_dMinVal = Double.MAX_VALUE;
		m_dMaxVal = Double.NEGATIVE_INFINITY;
		for (int i = 0; i < getOp().rows(); i++)
		{
			for (int d = 0; d < getNumHeatMapCols(); d++)
			{
				double dVal = getOp().getMeasurement(i, getHeatMapColDim(d));
				m_dMinVal = Math.min(m_dMinVal, dVal);
				m_dMaxVal = Math.max(m_dMaxVal, dVal);
			}
		}
	}
	
	int getNumHeatMapCols()
	{
		HeatMapOp opHM = (HeatMapOp) this.getOp();
		return opHM.m_iGroupDims * opHM.m_iNumGroups;
	}
	
	int getHeatMapColDim(int col)
	{
		return col;
	}
	
	// calculates the profile of the selected rows
	void updateSelectedProfile()
	{
		if (getNumHeatMapCols() == 0)
			return;
		
		m_SelectedProfile  = new double[getNumHeatMapCols()];
		if (m_iSelectionCol != -1)
		{
			int iNumSelected = 0;
			for (int i = 0; i < getOp().rows(); i++)
			{
				if (getOp().getMeasurement(i, m_iSelectionCol) > 0)
				{
					iNumSelected++;
					for (int d = 0; d < getNumHeatMapCols(); d++)
					{
						m_SelectedProfile[d] += getOp().getMeasurement(i, getHeatMapColDim(d));
					}
				}
			}
			
			if (iNumSelected > 0)
			{
				for (int d = 0; d < getNumHeatMapCols(); d++)
				{
					m_SelectedProfile[d] /= iNumSelected;
				}
			}
		}
	}

	public synchronized void mouseReleased()
	{
		m_CurrMouseState.m_EndX = mouseX;
		m_CurrMouseState.m_EndY = mouseY;
		m_CurrMouseState.m_State = MouseState.STATE_RELEASED;

		// are we creating a box?
		if (m_CurrDrawMode == DrawMode.HEAT_MAP)
		{
			
//			if ((m_CurrMouseState.m_Button == MouseState.BUTTON_LEFT && 
//				 m_NavigationMode == NavigationMode.PAN) || m_CurrMouseState.m_Button == MouseState.BUTTON_RIGHT)
//			{
//				m_dViewOffset -= m_dViewRange * (m_CurrMouseState.m_EndY - m_CurrMouseState.m_StartY) / m_iHMViewH;
//				m_dViewOffset = Math.min(1.0 - m_dViewRange, Math.max(0.0, m_dViewOffset));
//			}

			
//			if (mouseButton == LEFT)
//			{
//				selectionOn = true;
//				if (m_iSelectYMin != -1 && m_NavigationMode == NavigationMode.SELECT_RANGE)
//				{
//					m_iSelectYMax = Math.max(m_iSelectYMin, mouseY);			
//					m_iSelectYMin = Math.min(m_iSelectYMin, mouseY);
//				}
//				//invokeRedraw(true);
//			}
		}
		//invokeRedraw(true);
		loop();
		invokeRedraw(false);    	 
	}
	
	public void mouseDragged()
	{
		m_CurrMouseState.m_EndX = mouseX;
		m_CurrMouseState.m_EndY = mouseY;
		m_CurrMouseState.m_State = MouseState.STATE_DRAGGING;

		//if (m_bIsDrawing)
		//	return;
		//m_NavigationMode = NavigationMode.PAN;
		
		if (m_CurrDrawMode == DrawMode.HEAT_MAP)
		{
			
			if (mouseButton == LEFT && m_NavigationMode == NavigationMode.SELECT_RANGE)
			{
//				m_iSelectYMax = mouseY;			
				invokeRedraw(false);
			}
			else if ((mouseButton == LEFT && m_NavigationMode == NavigationMode.PAN) || mouseButton == RIGHT)
			{
				//m_dViewOffset -= m_dViewRange * (mouseY - pmouseY) / m_iHMViewH;
				//m_dViewOffset = Math.min(1.0 - m_dViewRange, Math.max(0.0, m_dViewOffset));
				//invokeRedraw(true);
			}
			else if (mouseButton == LEFT && m_NavigationMode == NavigationMode.ZOOM)
			{
			}
		}
		invokeRedraw(false);
	}
	
	public synchronized void mouseMoved()
	{
		if (m_CurrMouseState.m_State == -1) // don't change the state if it hasn't been processed already
		{
			m_CurrMouseState.m_StartX = mouseX;
			m_CurrMouseState.m_StartY = mouseY;
		}
		
		invokeRedraw(false);    	 
    }
	
	public synchronized void mousePressed()
	{
		m_CurrMouseState.m_Button = mouseButton == LEFT ? MouseState.BUTTON_LEFT : MouseState.BUTTON_RIGHT;
		m_CurrMouseState.m_StartX = mouseX;
		m_CurrMouseState.m_StartY = mouseY;

		m_CurrMouseState.m_State = MouseState.STATE_PRESSED;
		
		if (m_CurrDrawMode == DrawMode.HEAT_MAP)
		{
			if (mouseButton == LEFT && // double clicked
				mouseX >= m_HMViewCoords[0] && mouseX <= m_HMViewCoords[2] &&
				mouseY <= m_HMViewCoords[1] && mouseY >= BORDER_SORT_T)  // above the heatmap
			{
				HeatMapOp opHM = (HeatMapOp) this.getOp();
				int iGroup = (mouseX - m_HMViewCoords[0]) * getNumHeatMapCols() / ((m_HMViewCoords[2] - m_HMViewCoords[0]) * opHM.m_iGroupDims);

				GroupInfo gi = opHM.getGroupInfo(iGroup);
				
		    	if (gi != null)
		    	{
					m_iActiveGroup = iGroup;
					guiShowHeatMapControlWindow(iGroup, mouseX, mouseY);
					if (mouseEvent.getClickCount() == 2)
					{
						opHM.sortRowsByGroupIndex(iGroup, opHM.m_GroupSortType);
					}
		    	}
		    	
//				if (mouseEvent.getClickCount() == 2)
//				{// sort
//					if( this.getOp() instanceof HeatMapOp )
//					{
//						opHM.sortRowsByGroupIndex(iGroup, opHM.m_GroupSortType);
//						//invokeRedraw(true);
//					}
//				}
			}
//			else if (mouseButton == LEFT &&
//				mouseX >= m_HMViewCoords[0] && mouseX <= m_HMViewCoords[2] &&
//				mouseY >= m_HMViewCoords[1] && mouseY <= m_HMViewCoords[3])  // inside the heatmap
//			{// start selecion box
//				m_iSelectYMin = mouseY;
//				m_iSelectYMax = -1;
//			}
		}
		else
		{
//			selectionOn = true;
			invokeRedraw(false);
		}
		invokeRedraw(false);
	}

	public synchronized void mouseWheelMoved (MouseWheelEvent e)
	{
		if (m_CurrDrawMode == DrawMode.HEAT_MAP)
		{
			if (getOp().rows() > 0)
			{
				int iWheelRotation = e.getWheelRotation();
				double dNewViewRange = Math.max(1.0 * m_iHMViewH / getOp().rows(), Math.min(1.0, m_dViewRange + 0.001 * iWheelRotation));
				
		 		
				double dNewViewOffset = m_dViewOffset + (m_dViewRange - dNewViewRange) * (mouseY - m_HMViewCoords[1]) / m_iHMViewH;
		
				if (dNewViewRange == 1.0)
					dNewViewOffset = 0.0;
				m_dViewRange = dNewViewRange;
				m_dViewOffset = dNewViewOffset;
				m_dViewOffset = Math.min(1.0 - m_dViewRange, Math.max(0.0, m_dViewOffset));
				invokeRedraw(true);
			}
		}
	}

	boolean m_bKeyAltPressed = false;
	boolean m_bKeyShiftPressed = false;
	public synchronized void keyPressed()
	{
		if(key == 27)
		{
			key = 0; // to prevent from quitting when hitting Esc
		}
		
		if (key == 'p')
		{
			m_NavigationMode = NavigationMode.PAN;
		}
		else if (key == 's')
		{
			m_NavigationMode = NavigationMode.SELECT_RANGE;
		}
		else if (key == 'z')
		{
			m_NavigationMode = NavigationMode.ZOOM;
		}
		
		if (key == CODED)
		{
			if (keyCode == ALT)
			{
				m_bKeyAltPressed = true;
			} else if (keyCode == SHIFT)
			{
				m_bKeyShiftPressed = true;
			} 
			else if (keyCode == RIGHT)
			{
				m_CurrDrawMode = DrawMode.values()[(m_CurrDrawMode.ordinal() + 1) % DrawMode.values().length];
				//invokeRedraw(false);
			}
			else if (keyCode == LEFT)
			{
				m_CurrDrawMode = DrawMode.values()[(m_CurrDrawMode.ordinal() + DrawMode.values().length - 1) % DrawMode.values().length];
				//invokeRedraw(false);
			}

			if (m_CurrDrawMode == DrawMode.SORTED_SIMILARITY)
			{
				if (keyCode == DOWN)
				{
					m_iSimilarityViewStartIndex++;
//					if (m_Similarity != null)
//						m_iSimilarityViewStartIndex = Math.min(m_iSimilarityViewStartIndex + 1, m_Similarity.m_Scores.length);
				}
				else if (keyCode == UP)
				{
					m_iSimilarityViewStartIndex = Math.max(m_iSimilarityViewStartIndex - 1, 0);
				}
			}
		}
		invokeRedraw(false);
	}
	
	public synchronized void keyReleased()
	{
		if (key == CODED)
		{
			if (keyCode == ALT)
			{
				m_bKeyAltPressed = false;
			} else if (keyCode == SHIFT)
			{
				m_bKeyShiftPressed = false;
			} 
		}
	}	

	
	/**
	 * 
	 */
	private static final long serialVersionUID = -8429666944296674336L;
	
	@Override
	public void stateChanged(ChangeEvent e)
	{
	}
}
