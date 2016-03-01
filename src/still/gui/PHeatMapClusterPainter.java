//TODO: optimize the plot_bounds calculation (currently d^2 * n), and call less often.
package still.gui;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Comparator;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import processing.core.*;
import still.data.Operator;
import still.data.TableEvent;
import still.operators.HeatMapClusterOp;
import still.gui.metadataviewer.MetadataImageViewer;
import still.gui.metadataviewer.MetadataViewer;

public class PHeatMapClusterPainter extends OPApplet implements ChangeListener, MouseWheelListener {
	
	public int 				m_iGroupNum = 8;
	public int 				m_iGroupDim = 30;
	public double 			m_dSortValues[] = null;
	public Integer			m_iSortIndex[] = null;

	boolean 				selectionOn = false; // whether doing a selection by dragging mouse
	int 					m_iSelectYMin = -1;
	int 					m_iSelectYMax = -1;

	final int 				BORDER_HM_L = 25; // heatmap left border
	final int 				BORDER_HM_R = 25; // heatmap right border
	final int 				BORDER_HM_T = 30; // heatmap top border
	final int 				BORDER_HM_B = 60; // heatmap bottom border
	final int				BORDER_SORT_T = 5; // sort region top border
	
	int[] 					m_HMViewCoords = new int[4]; ///< heatmap view coordinates {left, top, right, bottom}
	int						m_iHMViewW = 0; ///< heatmap view width
	int 					m_iHMViewH = 0; ///< heatmap view height
	
	double 					m_dViewOffset = 0.0; // the offset ratio of the starting row in the current heatmap view
	double 					m_dViewRange  = 1.0; // ratio of the entire table rows, shown in the current heatmap view
	PGraphics 				m_PGxHeatMapCluster = null; // caches rendered heatmap (performance)
	boolean					m_bUpdatePGxHeatmapCluster = true; // whether the heatmap requires updating
	double []				m_HistMouseOver = null;	// histogram for the rows under the cursor
	double []				m_HistSelected = null; // histogram for the selected rows
	double 					m_dMinVal = 0; // minimum value across the whole table
	double 					m_dMaxVal = 0; // maximum value across the whole table
	
	boolean 				m_bIsUpdating = false;

	int 					m_iMagnifiedPoint 		= -1;
	
	public int 		  	  	m_iMetaDataColIndex  	= -1;
	public boolean 		  	m_bShowMetaData  		= true;
	public MetadataViewer 	m_Viewer 				= new MetadataImageViewer(); //TODO: will be replaced with a general Factory.
	public JPanel  		  	m_MetaDataPanel = null;
	
	class ClusterView
	{
		ClusterView(int dims)
		{
			m_dMean = new double[dims];
			m_dStdDev = new double[dims];
			m_iCount = 0;
			m_bActive = true;
		}
		
		public void reset()
		{
			for (int i = 0; i < m_dMean.length; i++)
			{
				m_dMean[i] = 0;
				m_dStdDev[i] = 0;
			}
			m_iCount = 0;
		}
		
		double  m_dMean[]; 		// [dim] mean profile for each dim of points in this cluster
		double  m_dStdDev[];	// [dim] stddev for each dim of points in this cluster
		boolean m_bActive;  	// if this cluster is active
		int		m_iCount; 		// number of points in this cluster
	}
	
	ClusterView[][] m_ClusterViews = null;
	ClusterView[] m_ClusterViewSum = null;
	

	void initClusterViews()
	{
		numerics = getNumerics();
		m_ClusterViews = new ClusterView[m_iGroupNum][];
		m_ClusterViewSum = new ClusterView[m_iGroupNum];
		
		int iNumClusters[] = new int[m_iGroupNum];
		// finding the maximum cluster id for each group and using that as the number of clusters
		// TODO: find the actual number of unique clusters.
		for (int i = 0; i < getOp().rows(); i++)
		{
			for (int grp = 0; grp < m_iGroupNum; grp++)
			{
				int dim = numerics.get(m_iGroupNum * m_iGroupDim + grp);
				iNumClusters[grp] = (int) Math.max(iNumClusters[grp], getOp().getMeasurement(i, dim) + 1);
			}
		}
		
		for (int grp = 0; grp < m_iGroupNum; grp++)
		{
			m_ClusterViews[grp] = new ClusterView[iNumClusters[grp]];
			for (int clst = 0; clst < iNumClusters[grp]; clst++)
			{
				m_ClusterViews[grp][clst] = new ClusterView(m_iGroupDim);
			}
			
			m_ClusterViewSum[grp] = new ClusterView(m_iGroupDim);
		}	
	}
	
	void updateClusterViews(boolean bSort)
	{
		// reset values
		for (int grp = 0; grp < m_iGroupNum; grp++)
		{
			for (int clst = 0; clst < m_ClusterViews[grp].length; clst++)
			{
				m_ClusterViews[grp][clst].reset();
			}
			m_ClusterViewSum[grp].reset();
		}
		
		boolean bTableChanged = false;
		
		int iGroupCluster[] = new int[m_iGroupNum]; // the cluster ids of the groups of a point (cached for performance)
		
		
		for (int ipass = 0; ipass < 2; ipass++) 
		{// two passes: 0: initial mean calculation. 1: stddev calculation
			for (int i = 0; i < getOp().rows(); i++)
			{
				int iDeselected = 0; // number of deselected 
				for (int grp = 0; grp < m_iGroupNum; grp++)
				{
					int dim = numerics.get(m_iGroupNum*m_iGroupDim + grp);
					iGroupCluster[grp]  = (int) getOp().getMeasurement(i, dim); // the cluster id of this group
					if (!m_ClusterViews[grp][iGroupCluster[grp]].m_bActive)
					{
						iDeselected++;
					}
				}
				
				if (m_iSelectionCol != -1)
				{
					double dNewSelect = iDeselected == 0 ? 1 : 0;
					if (getOp().getMeasurement(i, m_iSelectionCol) != dNewSelect)
					{
						getOp().setMeasurement(i, m_iSelectionCol, dNewSelect);
						bTableChanged = true;
					}
				}
				
				for (int grp = 0; grp < m_iGroupNum; grp++)
				{
					if (iDeselected == 0 || (iDeselected == 1 && !m_ClusterViews[grp][iGroupCluster[grp]].m_bActive))
					{
						ClusterView clView = m_ClusterViews[grp][iGroupCluster[grp]];
						for (int d = 0; d < m_iGroupDim; d++)
						{
							int dim = numerics.get(grp * m_iGroupDim + d);
							double dValue = getOp().getMeasurement(i, dim);
							if (dValue < 0)
							{//TODO: investigate why there is a negative value?
								dValue = 0;
							}
							
							if (ipass == 0)
							{
								clView.m_dMean[d] += dValue;
								m_ClusterViewSum[grp].m_dMean[d] += dValue;
							}
							else
							{
								double dDiff = dValue - clView.m_dMean[d] / clView.m_iCount;
								clView.m_dStdDev[d] += dDiff * dDiff;
								
								dDiff = dValue - m_ClusterViewSum[grp].m_dMean[d] / m_ClusterViewSum[grp].m_iCount;
								m_ClusterViewSum[grp].m_dStdDev[d] += dDiff * dDiff;
							}
						}
						
						if (ipass == 0)
						{
							m_ClusterViews[grp][iGroupCluster[grp]].m_iCount++;
							m_ClusterViewSum[grp].m_iCount++;
						}
					}
				}
			}
		}

		for (int grp = 0; grp < m_iGroupNum; grp++)
		{
			for (int clst = 0; clst < m_ClusterViews[grp].length; clst++)
			{
				ClusterView clView = m_ClusterViews[grp][clst];
				for (int d = 0; d < m_iGroupDim; d++)
				{
					clView.m_dMean[d] /= clView.m_iCount; 
					clView.m_dStdDev[d] = Math.sqrt(clView.m_dStdDev[d] / clView.m_iCount); 
					if (clst == 0)
					{// need to do this only once per group
						m_ClusterViewSum[grp].m_dMean[d] /= m_ClusterViewSum[grp].m_iCount;
						m_ClusterViewSum[grp].m_dStdDev[d] = Math.sqrt(m_ClusterViewSum[grp].m_dStdDev[d] / m_ClusterViewSum[grp].m_iCount);
					}
				}
			}
		}

		bSort = false;
		if (bSort)
		{
			for (int grp = 0; grp < m_iGroupNum; grp++)
			{
				Arrays.sort(m_ClusterViews[grp], new Comparator<ClusterView>() {
					public int compare(ClusterView c1, ClusterView c2)
					{
						int n1 = c1.m_iCount;
						int n2 = c2.m_iCount;
						return n1 > n2 ? -1 : (n1 < n2 ? 1 : 0);//(i1 < i2 ? -1 : (i1 > i2 ? 1 : 0)));
					}
				});
			}
		}
		
		if (bTableChanged)
		{
			getOp().tableChanged(new TableEvent( getOp(), TableEvent.TableEventType.ATTRIBUTE_CHANGED, "selection", null, false), true); // downstream
			//getOp().tableChanged(new TableEvent( getOp(), TableEvent.TableEventType.ATTRIBUTE_CHANGED, "selection", null, true), true); // upstream
		}
	}

	public PHeatMapClusterPainter(Operator op) {
		super(op);		
				
		addMouseWheelListener(this);
		updateColorSelectionCol();
		initClusterViews();
		updateClusterViews(true);		
		updateHeatMapClusterViewer();
	}
	public void invokeRedraw(boolean bUpdatePGxHeatmapCluster)
	{// to be able to monitor where and when the redraw is being invoked.
		m_bUpdatePGxHeatmapCluster = bUpdatePGxHeatmapCluster;
		redraw();
	}
	
	public void showMetaData(boolean bShow)
	{
		m_bShowMetaData = bShow;
		invokeRedraw(true);
	}
	
	public void heavyResize()
	{
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
	
	public void updateHeatMapClusterViewer()
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
		m_bIsUpdating = true;
		numerics = getNumerics();
		countNumerics();
		updateColorSelectionCol();
		updateHeatMapClusterViewer();
		updateHistSelected();
		updateMinMax();
		initClusterViews();
		updateClusterViews(true);
		
	    SwingUtilities.invokeLater(new Runnable() {
								   public void run() {
								   // Set the preferred size so that the layout managers can handle it
								   heavyResize();
								   invokeRedraw(true);
								   }
								   });
		m_bIsUpdating = false;
	}
	
	public void setup() 
	{
		textFont(createFont("Helvetica",10),10);

		countNumerics();
		heavyResize();
		
		if( this.getOp() instanceof HeatMapClusterOp )
		{
			numerics = getNumerics();
			// count the number of dimensions
			countNumerics();
			// compute the minimum size
			size(OPAppletViewFrame.MINIMUM_VIEW_WIDTH, OPAppletViewFrame.MINIMUM_VIEW_HEIGHT);
			this.setPreferredSize(new Dimension(OPAppletViewFrame.MINIMUM_VIEW_WIDTH, OPAppletViewFrame.MINIMUM_VIEW_HEIGHT));
		}
		
		heavyResize();
		
		// prevent thread from starving everything else
		noLoop();
		
		this.finished_setup = true;
		
	    SwingUtilities.invokeLater(new Runnable() {
								   public void run() {
								   // Set the preferred size so that the layout managers can handle it
								   invalidate();
								   getParent().validate();
								   }
								   });
	}
	
	public void calcHMViewCoords()
	{
		m_HMViewCoords[0] = BORDER_HM_L; 
		m_HMViewCoords[1] = BORDER_HM_T;
		m_HMViewCoords[2] = width - BORDER_HM_R;
		m_HMViewCoords[3] = height - BORDER_HM_B;
		m_iHMViewW = m_HMViewCoords[2] - m_HMViewCoords[0];
		m_iHMViewH = m_HMViewCoords[3] - m_HMViewCoords[1];
	}
	
	boolean m_bIsDrawing = false;
	public synchronized void draw()
	{
		m_bIsDrawing = true;
		background(128 + 64 + 32);
		while (m_bIsUpdating || this.getOp().isUpdating()) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
    	if( this.getOp() instanceof HeatMapClusterOp)
    	{
			fill(0);
			//drawMagnifiedPlot();
	 		fill(224);
	 		beginShape();
	 		vertex(m_HMViewCoords[0] - 1, m_HMViewCoords[1] - 1);
	 		vertex(m_HMViewCoords[2] + 1, m_HMViewCoords[1] - 1);
	 		vertex(m_HMViewCoords[2] + 1, m_HMViewCoords[3] + 1);
	 		vertex(m_HMViewCoords[0] - 1, m_HMViewCoords[3] + 1);
	 		endShape(CLOSE);
			
	 		if (m_iHMViewW <= 0 || m_iHMViewH <= 0)
	 			return;
	 		
			try
			{
				drawHeatMapCluster();
			}
			catch (Exception e)
			{
				System.out.println("Exception: PHeatMapPainter.drawHeatMap()");
				e.printStackTrace();
			}
		}
    	m_bUpdatePGxHeatmapCluster = false;
		m_bIsDrawing = false;
	}

	void drawHeatMapCluster()
	{
		if (m_ClusterViews == null)
			return;
		
		int iDataSize = getOp().rows();
		
		boolean bSelectionChanged = false;
		
		int iMaxNumClusters = 0;
		
		int iNumGroups = m_ClusterViews.length;
		for (int grp = 0; grp < m_ClusterViews.length; grp++)
		{
			iMaxNumClusters = Math.max(m_ClusterViews[grp].length, iMaxNumClusters);
		}

		float fGap = 5;
		float fClusterWidth = 1.f * m_iHMViewW / iNumGroups - fGap; // each column for a group
		float fClusterHeight = 1.f * m_iHMViewH / (iMaxNumClusters + 2) - fGap;
		
		for (int grp = 0; grp < m_ClusterViews.length; grp++)
		{
			//ClusterView clViewSum = new ClusterView();
			//clViewSum.m_dMean = new double[m_iGroupDim];
			//clViewSum.m_iCount = 0;
			//clViewSum.m_bActive = true;
			
			for (int clst = 0; clst < m_ClusterViews[grp].length + 1;clst++)
			{
				ClusterView clView = null;
				boolean bViewSum = false;
				if (clst < m_ClusterViews[grp].length)
				{
					clView = m_ClusterViews[grp][clst];
				}
				else
				{
					clView = m_ClusterViewSum[grp];
					bViewSum = true;
					clst = iMaxNumClusters + 1;
				}

				float fLeft   = 1.f * grp * (fClusterWidth + fGap) + m_HMViewCoords[0];
				float fTop    = 1.f * clst * (fClusterHeight + fGap) + m_HMViewCoords[1];
			    float fRight  = fLeft + fClusterWidth;
				float fBottom = fTop + fClusterHeight;
				
				boolean bMouseIn = false;
				
				if (mouseX >= fLeft && mouseX <= fRight && mouseY >= fTop && mouseY <= fBottom)
				{
					bMouseIn = true;
				}

				stroke(clView.m_bActive ? 0 : 128);
				if (bMouseIn && !bViewSum)
					stroke(255, 0, 0);
				fill(clView.m_bActive ? 255 : 224);
				rect(fLeft, fTop, fRight - fLeft, fBottom - fTop);

				if (!bViewSum && selectionOn && bMouseIn)
				{
					clView.m_bActive = !clView.m_bActive;
					bSelectionChanged = true;
				}

				float xBar = fLeft;
				float dxBar = fClusterWidth / m_iGroupDim;
				for (int dim = 0; dim < m_iGroupDim; dim++)
				{
					noStroke();

					fill(clView.m_bActive ? 64 : 128);
					float fMeanHeight = (float) (fClusterHeight * clView.m_dMean[dim]);
					rect(xBar, fBottom - fMeanHeight, dxBar, fMeanHeight);

					fill(clView.m_bActive ? 32 : 64, 128);
					float fStdDevHeight = (float) (fClusterHeight * clView.m_dStdDev[dim]);
					//rect(xBar, fBottom - fMeanHeight + fStdDevHeight, dxBar, -fStdDevHeight * 2);
					
					if (mouseX > xBar && mouseX < xBar + dxBar)
					{// draw the vertical line on the cursor
						stroke(128, 128);
						line(xBar + dxBar/2, fTop, xBar + dxBar / 2, fBottom);
						if (clst == 0) // do it once
						{// show the column name on the cursor
							fill(0);
							textAlign(CENTER, BOTTOM);
							text(getOp().input.getColName(numerics.get(grp * m_iGroupDim + dim)), xBar + dxBar /2, m_HMViewCoords[1]);
						}
					}
					
					xBar += dxBar;
				}
				
				// draw the horizonal bar showing the cluster size
				fill(0, 0, 128);
				rect(fLeft, fTop, fClusterWidth * clView.m_iCount / iDataSize, 5);
				// draw the ratio of the cluster size to the data size
				NumberFormat nf = NumberFormat.getInstance();
				nf.setMinimumFractionDigits(0);
				nf.setMaximumFractionDigits(2);
				String st = nf.format(100.0 * clView.m_iCount / iDataSize) + "%";
				textAlign(LEFT, TOP);
				text(st, fLeft + 5, fTop + 5);
			}
		}
		
		if (selectionOn)
		{// done with the selection
			selectionOn = false;
			m_iSelectYMin = -1;
			m_iSelectYMax = -1;
		}
		
		if (bSelectionChanged)
		{
			m_bIsUpdating = true;
			updateClusterViews(false);
			m_bIsUpdating = false;
			draw();
		}
	}

	void updateMinMax()
	{
		m_dMinVal = Double.MAX_VALUE;
		m_dMaxVal = Double.NEGATIVE_INFINITY;
		for (int i = 0; i < getOp().rows(); i++)
		{
			for (int d = 0; d < numerics.size(); d++)
			{
				double dVal = getOp().getMeasurement(i, numerics.get(d));
				m_dMinVal = Math.min(m_dMinVal, dVal);
				m_dMaxVal = Math.max(m_dMaxVal, dVal);
			}
		}
	}
	
	void updateHistSelected()
	{
		m_HistSelected  = new double[numerics.size()];
		if (m_iSelectionCol != -1)
		{
			int iNumSelected = 0;
			for (int i = 0; i < getOp().rows(); i++)
			{
				if (getOp().getMeasurement(i, m_iSelectionCol) > 0)
				{
					iNumSelected++;
					for (int d = 0; d < numerics.size(); d++)
					{
						m_HistSelected[d] += getOp().getMeasurement(i, numerics.get(d));
					}
				}
			}
			
			if (iNumSelected > 0)
			{
				for (int d = 0; d < numerics.size(); d++)
				{
					m_HistSelected[d] /= iNumSelected;
				}
			}
		}
	}
		
	public void mouseReleased()
	{
		
		// are we creating a box?
//		if (mouseButton == LEFT  && m_iSelectYMin != -1)
//		{
//			selectionOn = true;
//			m_iSelectYMax = Math.max(m_iSelectYMin, mouseY);			
//			m_iSelectYMin = Math.min(m_iSelectYMin, mouseY);			
//			invokeRedraw(true);
//		}
	}
	
	public void mouseDragged()
	{
//		if (m_bIsDrawing)
//			return;
//		
//		if (mouseButton == RIGHT)// && m_iSelectYMin != -1)
//		{
//			m_dViewOffset -= m_dViewRange * (mouseY - pmouseY) / m_iHMViewH;
//			m_dViewOffset = Math.min(1.0 - m_dViewRange, Math.max(0.0, m_dViewOffset));
//			invokeRedraw(true);
//			return;
//		}
//		
//		if (mouseButton == LEFT)
//		{
//			m_iSelectYMax = mouseY;			
//			invokeRedraw(false);
//		}
		
	}
	
	public void mouseMoved()
	{
		invokeRedraw(false);    	 
    }
	
	public void mousePressed()
	{
		if (mouseButton == LEFT)  // inside the heatmap cluster
		{
			selectionOn = true;
			invokeRedraw(true);
		}
		/*
		if (mouseButton == LEFT && mouseEvent.getClickCount() == 2 && // double clicked
			mouseX >= m_HMViewCoords[0] && mouseX <= m_HMViewCoords[2] &&
			mouseY <= m_HMViewCoords[1] && mouseY >= BORDER_SORT_T)  // inside the heatmap
		{// sort
			if( this.getOp() instanceof HeatMapClusterOp )
			{
				HeatMapClusterOp opHM = (HeatMapClusterOp) this.getOp();
				int iGroup = (mouseX - m_HMViewCoords[0]) * numerics.size() / ((m_HMViewCoords[2] - m_HMViewCoords[0]) * m_iGroupDim);
				int selectedCols[] = new int[Math.min(m_iGroupDim, numerics.size() - iGroup * m_iGroupDim)];
				for (int i = 0; i < selectedCols.length; i++)
				{
					selectedCols[i] = i + iGroup * m_iGroupDim;
				}
				opHM.setSelectedCols(selectedCols);
				opHM.sortRowsBySelectedColumns(opHM.m_SortType);
				invokeRedraw(true);
			}
		}
		else if (mouseButton == LEFT &&
			mouseX >= m_HMViewCoords[0] && mouseX <= m_HMViewCoords[2] &&
			mouseY >= m_HMViewCoords[1] && mouseY <= m_HMViewCoords[3])  // inside the heatmap
		{// start selecion box
			m_iSelectYMin = mouseY;
			m_iSelectYMax = -1;
		}
		*/
	}

	public void mouseWheelMoved (MouseWheelEvent e)
	{
		//println( e.toString() );
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

	boolean m_bKeyAltPressed = false;
	boolean m_bKeyShiftPressed = false;
	public void keyPressed()
	{
		if (key == CODED)
		{
			if (keyCode == ALT)
			{
				m_bKeyAltPressed = true;
			} else if (keyCode == SHIFT)
			{
				m_bKeyShiftPressed = true;
			} 
		}
	}
	
	public void keyReleased()
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
