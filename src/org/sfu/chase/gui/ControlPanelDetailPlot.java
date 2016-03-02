package org.sfu.chase.gui;

import java.awt.event.KeyEvent;
import java.awt.geom.Line2D;
import java.text.NumberFormat;

import javax.swing.event.ChangeEvent;

import org.sfu.chase.collab.DataStats;
import org.sfu.chase.core.ClustInfo;
import org.sfu.chase.core.GroupInfo;
import org.sfu.chase.gui.DisplayParams.PlotType;
import org.sfu.chase.input.Parameters;
import org.sfu.chase.util.Utils;
import org.sfu.chase.util.Utils.Rect;

import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PGraphics;
import controlP5.ControlP5;
import controlP5.ControllerGroup;
import controlP5.PanelController;
import controlP5.Scrollbar;


public class ControlPanelDetailPlot extends ControlPanel
{
//	ClustFramework m_Framework;
//	ClustInfo      m_ClustInfo;
//	DisplayParams  dp;
//	MouseState     m_MouseState;
//	ChangeListener	m_ChangeListener;
	
	int            m_iGroupIndex;
	PGraphics      m_Gx;
	ClusterDisplay m_CDisplay;	

	Scrollbar       m_ScrollX;
	Scrollbar 	    m_ScrollY;
	
	Rect		    m_LargeRect; // large plot rect showing the main profile
	Rect		    m_SmallRect; // small plot rect showing the small plot on top right (sattelite view)
	Rect			m_CuttOffRect; // the rectangle for setting the maximum cutt off for the group
	Rect 			m_TitleRect;
	Rect			m_ZoomCoord;
	float 			m_TitleH  = 20;
	float 			m_ResizeH = 20; 
	float 			m_CuttOffW = 30;
	boolean 		m_bMoving = false;
	boolean 		m_bResizing = false;
	int             m_AxisLabelH = 50;
	public boolean 	m_EnableThresholdLine = false;
	public float 	m_ThresholdCursor = 0;
	private boolean	m_ShowSummary = false;
	int m_ActivePlotGroup = -1; // used to detect if a group is highlighted in this view
	
	// following are initialized in resetRenderState()
	int m_FontSize;
	float m_StrokeWidth;
	boolean m_bShowLegend;
	boolean m_bShowLabels;
	
	boolean m_bSeparateClusterPlots = false; // will draw the plot for children clusters separately
	boolean m_bRPKM = false; // uses rpkm normalization for drawing plots
	double m_MaxRPKM = 1.0;
	
	
	public ControlPanelDetailPlot(ControlP5 theControlP5, String theName, int theX,
			int theY, int theWidth, int theHeight, PanelController parent, int anchor) 
	{
		super(theControlP5, theName, theX, theY, theWidth, theHeight, parent, anchor);
		//color().setBackground(0x01000000);
		m_bShowCloseButton = false;
		m_Title = "Plot";
	}
	
	public void setup()
	{
		setSize(width, height);
		
		ControllerGroup defaultGroup = (ControllerGroup) controlP5.controlWindow.tabs().get(1);
		m_ScrollX = new controlP5.Scrollbar(controlP5, defaultGroup, "largeX", m_LargeRect.left() - position.x(), height - 25, (int)m_LargeRect.width(), 20);
		controlP5.register(m_ScrollX);
		addToLayout(m_ScrollX, ANCHOR_LEFT | ANCHOR_RIGHT | ANCHOR_BOTTOM);
		m_ScrollX.setResizable(true);
		m_ScrollX.setValues(Scrollbar.HORIZONTAL, 0, 1, 0, 1);
		m_ScrollX.setMinVisibleAmount(0.01f);
		
		m_ScrollY = new controlP5.Scrollbar(controlP5, defaultGroup, "largeY", width - 25, m_LargeRect.top() - position.y(), 20, (int)m_LargeRect.height());
		controlP5.register(m_ScrollY);
		addToLayout(m_ScrollY, ANCHOR_RIGHT | ANCHOR_TOP | ANCHOR_BOTTOM);
		m_ScrollY.setValues(Scrollbar.VERTICAL, 0, 1, 0, 1);
		m_ScrollY.setResizable(true);
		m_ScrollY.setMinVisibleAmount(0.01f);
	}
	
	public void setPlotInfo(ClustInfo cInfo, int group)
	{
		m_ClustInfo = cInfo;
		m_iGroupIndex = group;
	}
	
	public void setClusterDisplay(ClusterDisplay cdisplay)
	{
		m_CDisplay = cdisplay;
	}

	@Override
	public void setSize(int theWidth, int theHeight)
	{
		super.setSize(theWidth, theHeight);
		resetRenderState();
	}
	
	public void showSummaryView(boolean showSummary)
	{
		m_ShowSummary = showSummary;
	}
	
    void setRPKM(boolean enableRPKM)
    {
        m_bRPKM = enableRPKM;
    }
    
    void setSeparateClusterPlots(boolean separate)
    {
        m_bSeparateClusterPlots = separate;
    }
	
	void setRenderSize(float px, float py, int theWidth, int theHeight)
	{
		m_TitleH = paneTitleH;
		m_TitleRect = new Rect(px, py, theWidth, m_TitleH);
		m_LargeRect = new Rect(px + m_AxisLabelH + m_FontSize, py + m_TitleRect.height() + 10,
				               theWidth - 40 - m_AxisLabelH - m_FontSize, theHeight - m_TitleRect.height() - 40 - m_AxisLabelH);
		m_CuttOffRect = new Rect(m_LargeRect.left() - m_CuttOffW, m_LargeRect.top(), m_CuttOffW, m_LargeRect.height());
		try
		{
			if ((m_Gx == null || m_LargeRect.width() != m_Gx.width || m_LargeRect.height() != m_Gx.height) && m_LargeRect.width() > 0 && m_LargeRect.height() > 0)
			{
				// using PConstants.P2D results in problems drawing concave filled polygons
				m_Gx = ControlP5.papplet.createGraphics((int)m_LargeRect.width(), (int)m_LargeRect.height(), PConstants.JAVA2D); 
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	@Override
	public void draw(PApplet app)
	{
		super.draw(app);
		drawDetailPlot(app.g);
	}
	
	void drawDetailPlot(PGraphics gx)
	{
		if (m_LargeRect.width() <= 0 || m_LargeRect.height() <= 0)
			return;
		
		m_SmallRect = new Rect(m_LargeRect.right() - dp.plotW - 5, m_LargeRect.top() + dp.plotGapY, dp.plotW, dp.plotH);
		m_ZoomCoord = new Rect(m_ScrollX.value(), m_ScrollY.value(), m_ScrollX.getVisibleAmount(), m_ScrollY.getVisibleAmount());
		
		if (m_ClustInfo == null || m_ClustInfo.size() == 0)
			return;

		DisplayParams.PlotType prevPlotType = dp.plotType;
		if (m_EnableThresholdLine)
		{
			dp.plotType = PlotType.PEAK_HIST;
		}
		
		gx.pushStyle();

		try
		{
			gx.noStroke();
			
			// draw the large plot image
			drawLargePlot();
			gx.image(m_Gx, m_LargeRect.left(), m_LargeRect.top());
			gx.noFill();
			gx.stroke(0);
			gx.strokeWeight(1);
			gx.rect(m_LargeRect.left(), m_LargeRect.top(), m_LargeRect.width(), m_LargeRect.height()); // border
			
			if (m_EnableThresholdLine && m_ClustInfo.getNumThresholds() > 0)
			{
				float thresholdCurr = m_ClustInfo.getThreshold(m_iGroupIndex).value;
				if (1 - thresholdCurr >= m_ZoomCoord.top() && 1 - thresholdCurr <= m_ZoomCoord.bottom())
				{
					gx.stroke(0);
					gx.strokeWeight(3);
					gx.fill(0);
					float thresholdLineY = m_LargeRect.bottom() - m_LargeRect.height() * (thresholdCurr - 1 + m_ZoomCoord.bottom()) / (m_ZoomCoord.bottom() - m_ZoomCoord.top());  
					gx.line(m_LargeRect.left(), thresholdLineY, m_LargeRect.right(), thresholdLineY);
					m_CDisplay.drawNumberOfThresholdRegions(gx, m_ClustInfo, m_iGroupIndex, thresholdCurr, m_LargeRect.hcenter(), thresholdLineY, true);
				}
				
				if (m_LargeRect.isInside(m_MouseState.x(), m_MouseState.y()) && !m_bResizing && !m_bMoving)
				{
					gx.stroke(0, 0, 255);
					gx.fill(0, 0, 255);
					gx.strokeWeight(2);
					gx.line(m_LargeRect.left(), m_MouseState.y(), m_LargeRect.right(), m_MouseState.y());
					m_ThresholdCursor = (m_LargeRect.bottom() - m_MouseState.y()) * (m_ZoomCoord.bottom() - m_ZoomCoord.top()) / m_LargeRect.height() + 1 - m_ZoomCoord.bottom();  
					m_CDisplay.drawNumberOfThresholdRegions(gx, m_ClustInfo, m_iGroupIndex, m_ThresholdCursor, m_LargeRect.hcenter(), m_MouseState.y(), true);
				}
			}
	
			// draw the small plot image on the top right corner
			drawSmallPlot(gx);
			
			// draw the cut-off bar
			if (m_EnableThresholdLine) {
				drawCutOffMax(gx);
			}
			
			// draw the axis labels
			if (m_bShowLabels) 
			{
				drawAxisLabels(gx);
			
				// draw title text
				gx.fill(0);
				gx.textAlign(PConstants.LEFT, PConstants.BOTTOM);
				gx.textSize(m_FontSize);
				String title = "";
				if (m_ShowSummary)
				{
					title = "Summary";
					//m_CDisplay.getActivePlotGroup() >= 0 ? m_Framework.getGroup(m_CDisplay.getActivePlotGroup(), true).m_Name : "Summary";
				}
				else if (m_Framework.getGroup(m_iGroupIndex, true) != null)
				{
					title = m_Framework.getGroup(m_iGroupIndex, true).m_Name;
				}
				
				while (gx.textWidth(title) > m_TitleRect.width() - 40) {
					title = title.substring(0, title.length() - 1);
				}
				
				gx.text(title, m_LargeRect.left() + 10, m_TitleRect.bottom() + 2);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		gx.popStyle();
		dp.plotType = prevPlotType;
	}
	
	void resetRenderState()
	{
		m_FontSize = 15;
		m_StrokeWidth = 1.6f;
		m_bShowLegend = true;
		m_bShowLabels = true;
		setRenderSize(position.x(), position.y(), width, height);
	}
	
	void drawSmallPlot(PGraphics gx)
	{
		// draw the small plot image on the top right corner
		gx.fill(255);
		gx.stroke(0);
		gx.strokeWeight(1);
		gx.rect(m_SmallRect.left()-1, m_SmallRect.top()-1, m_SmallRect.width()+2, m_SmallRect.height()+2); // border
		if (!m_ShowSummary)
		{
			m_CDisplay.drawPlot(gx, dp, m_ClustInfo, m_iGroupIndex, m_SmallRect);
		}
		else
		{
			gx.stroke(128);
			gx.strokeWeight(2);
			m_CDisplay.drawSummaryPlot(gx, dp, m_ClustInfo, m_SmallRect, -1, false);
		}
		
		// shade the non-visible regions in the small plot image;
		gx.fill(0, 40);
		gx.noStroke();
		Rect visibleRect = new Rect(m_SmallRect.left() + m_ZoomCoord.left() * m_SmallRect.width(), 
				 m_SmallRect.top() + m_ZoomCoord.top() * m_SmallRect.height(),
				 m_ZoomCoord.width() * m_SmallRect.width(), 
				 m_ZoomCoord.height() * m_SmallRect.height());
		gx.rect(m_SmallRect.left(), m_SmallRect.top(), visibleRect.left() - m_SmallRect.left(), m_SmallRect.height());  
		gx.rect(visibleRect.left(), m_SmallRect.top(), visibleRect.width(), visibleRect.top() - m_SmallRect.top());  
		gx.rect(visibleRect.left(), visibleRect.bottom(), visibleRect.width(), m_SmallRect.bottom() - visibleRect.bottom());  
		gx.rect(visibleRect.right(), m_SmallRect.top(), m_SmallRect.right() - visibleRect.right(), m_SmallRect.height());  
		gx.noFill();
		gx.stroke(0);
		gx.rect(visibleRect.left(), visibleRect.top(), visibleRect.width(), visibleRect.height());  
	}
	
	void drawLargePlot()
	{
		m_Gx.beginDraw();
		m_Gx.stroke(0);
		m_Gx.strokeWeight(1);
		m_Gx.smooth();
		m_Gx.fill(255);
		m_Gx.rect(0, 0, m_Gx.width, m_Gx.height);
		
		float ww = m_Gx.width / m_ZoomCoord.width();
		float hh = m_Gx.height / m_ZoomCoord.height();
		float xx = -m_ZoomCoord.left() * ww;
		float yy = -m_ZoomCoord.top() * hh;
		Rect plotRect = new Rect(xx, yy, ww, hh);
		
		float mx = m_MouseState.x();
		float my = m_MouseState.y();
		
		m_ActivePlotGroup = -1;
		
		if (!m_ShowSummary)
		{
			m_CDisplay.drawPlot(m_Gx, dp, m_ClustInfo, m_iGroupIndex, plotRect);
			
			if (m_LargeRect.isInside(m_MouseState.x(), m_MouseState.y())) {
				m_ActivePlotGroup = m_iGroupIndex; 
			}
	        m_Gx.endDraw();
	        return;
		}
	    ClustInfo[] childrenInfo = null;
	    if (m_bSeparateClusterPlots && m_ClustInfo.m_bShowChildren) {
	        childrenInfo = m_ClustInfo.getChildren();
	    }
	    
	    if (childrenInfo == null)
	    {
	        childrenInfo = new ClustInfo[1];
	        childrenInfo[0] = m_ClustInfo;
	    }
	    
		// modified from http://colorbrewer2.org/index.php?type=qualitative&scheme=Set1&n=9
		//int[] colors = {0xFFFB8072, 0xFF377EB8, 0xFF4DAF4A, 0xFF984EA3, 0xFFFF7F00, 0xFFFFFF33, 0xFFA65628, 0xFFF781BF, 0xFF999999};

	    float legendX = m_FontSize*1;
		float legendY = m_FontSize*1;
		float legendDY = m_FontSize * 0.8f;
		float legendDX = 50;
        double binSize = m_Framework.getMaxRegionSize() / m_Framework.getGroupDim();
		
		//dp.plotType = PlotType.MEAN;
		double minGroupDist = Double.POSITIVE_INFINITY;
		int closestGroupIndex = m_CDisplay.getActivePlotGroup();
		int closestChildIndex = 0;
			
		boolean bRecalculateMax = true;
		m_MaxRPKM = 0.01;
		while (bRecalculateMax)
		{
			bRecalculateMax = false;
			for (int gi = 0; gi < m_Framework.getNumGroups(); ++gi)
			{
			    for (int chi = 0; chi < childrenInfo.length; ++chi)
			    {
					GroupInfo group = m_Framework.getGroup(gi, true);
					double cuttoffMax = m_bRPKM ? m_MaxRPKM : group.m_CutOffMax;
					int[] groupCols = m_Framework.getGroup(gi, true).m_iCols;
					double[] colMean = Utils.subArray(childrenInfo[chi].getStats().m_ColMean, groupCols);
					
					// find max rpkm value, necesary to draw the plot with correct scale later.
					if (m_bRPKM)// && gi > 0) 
					{
                        colMean = Utils.subArray(childrenInfo[chi].getUnNormStats().m_ColMean, groupCols);
                        DataStats globalStats= m_Framework.getDataModel().getVisibleExperimentStats(gi, Parameters.GLOBAL);
                        double dSum = globalStats.m_dMean * globalStats.m_iCount;
                        double dFactor = (1000000000) / (binSize * dSum);
    					//DataStats globalStats= m_Framework.getDataModel().getVisibleExperimentStats(gi, Parameters.GLOBAL);
    					//DataStats experimentStats = m_Framework.getDataModel().getVisibleExperimentStats(gi, group.m_Experiment.getStatType());
    					//double dFactor = (experimentStats.m_dMax * 1000000000) / (globalStats.m_dMean * globalStats.m_iCount);
						
						for (int i = 0; i < colMean.length; ++i)
						{
							colMean[i] *= dFactor;
							float plotScale = 1.1f; // to scale the plot axis by 10%
							if (colMean[i] * plotScale > m_MaxRPKM)
							{
								m_MaxRPKM = colMean[i] * plotScale;
								bRecalculateMax = true;
							}
						}
					}
					
					// while here, also find the closest plot or legend item to the mouse cursor
			        if (m_LargeRect.isInside(mx, my))
			        {
			            // distance to plot
						double dist = DrawUtils.getProfileDist(mx - m_LargeRect.left(), my - m_LargeRect.top(),
								colMean, null, 0, cuttoffMax, 
								plotRect.left(), plotRect.top(), plotRect.width(), plotRect.height(), false);
						
						// distance to legend dash line
						float lineY = legendY + (gi * childrenInfo.length + chi) * legendDY;
						dist = Math.min(dist, Line2D.ptSegDist(legendX, lineY, 
								legendX + legendDX, lineY,
								mx - m_LargeRect.left(), my - m_LargeRect.top()));
						
						if (dist < minGroupDist && dist < 10)
						{
							minGroupDist = dist;
							closestGroupIndex = gi;
							closestChildIndex = chi;
							m_CDisplay.setActivePlotGroup(gi);
							m_ActivePlotGroup = gi;
						}
			        }
			    }// for (int chi
			}// for (int gi
		}
			
		int[] usedColors = new int[ColorPalette.COLOR_MAX.length];// count of curves of the same color
		//int highlightColor = 0xFFE41A1C;
		
		// draw the plots and legend
		for (int gi = 0; gi < m_Framework.getNumGroups(); gi++)
		{
            for (int chi = 0; chi < childrenInfo.length; ++chi)
            {
				GroupInfo group = m_Framework.getGroup(gi, true);
				
				double cuttoffMax = m_bRPKM ? m_MaxRPKM : group.m_CutOffMax;
				int[] groupCols = group.m_iCols;
				double[] colMean = Utils.subArray(childrenInfo[chi].getStats().m_ColMean, groupCols);
				
				/*
					RPK= No.of Mapped reads/ length of transcript in kb (transcript length/1000)
					RPKM = RPK/total no.of reads in million (total no of reads/ 1000000)
					RPK_i = (val[i] / readlength) / (binsize/1000) = (val[i] * 1000) / (readlength * binsize)
					RPKM_i = RPK_i / ((SUM(val[i]) / readlength) / 1000,000) = (RPK_i * 1000,000 * readlength) / SUM(val[i])
					       = (val[i] * 1000 * 1000,000 * readlength) / (readlength * binsize * SUM(val[i]) 
					       = val[i] * 1000,000,000 / (binsize * SUM(val[i]))
				 */
				if (m_bRPKM)// && gi > 0) 
				{
					colMean = Utils.subArray(childrenInfo[chi].getUnNormStats().m_ColMean, groupCols);
					DataStats globalStats= m_Framework.getDataModel().getVisibleExperimentStats(gi, Parameters.GLOBAL);
					double dSum = globalStats.m_dMean * globalStats.m_iCount;
					double dFactor = (1000000000) / (binSize * dSum);
                    //DataStats globalStats= m_Framework.getDataModel().getVisibleExperimentStats(gi, Parameters.GLOBAL);
                    //DataStats experimentStats = m_Framework.getDataModel().getVisibleExperimentStats(gi, group.m_Experiment.getStatType());
                    //double dFactor = (experimentStats.m_dMax * 1000000000) / (globalStats.m_dMean * globalStats.m_iCount);
					
					for (int i = 0; i < colMean.length; ++i) {
						colMean[i] *= dFactor;
					}
				}
				
				int colPalIndex = group.m_Experiment.getColor().getColorIndex();
				float[] lineStyle = DrawUtils.LINE_STYLE_TYPES[usedColors[colPalIndex] % DrawUtils.LINE_STYLE_TYPES.length];
				usedColors[colPalIndex]++;

				int color = ColorPalette.COLOR_MAX[colPalIndex] | 0xFF000000;
				m_Gx.stroke(color);
				m_Gx.strokeWeight((gi == closestGroupIndex && chi == closestChildIndex) ?  m_StrokeWidth * 3 : m_StrokeWidth);
				
				DrawUtils.drawDashlineProfile(m_Gx, colMean, null, 0, cuttoffMax, plotRect.left(), plotRect.top(), plotRect.width(), plotRect.height(), false, lineStyle);
				
				// draw the legend
				if (m_bShowLegend) 
				{
					DrawUtils.dashline(m_Gx,legendX, legendY, legendX + legendDX, legendY, lineStyle);
					m_Gx.textAlign(PConstants.LEFT, PConstants.CENTER);
					m_Gx.textSize(m_FontSize* ((gi == closestGroupIndex && chi == closestChildIndex) ? 0.9f : 0.8f));
					m_Gx.fill(ColorPalette.COLOR_MAX[colPalIndex] | 0xFF000000);
					String sClusterTitle = childrenInfo[chi].getTitle();
					m_Gx.text(group.m_Name + (sClusterTitle.length() > 0 ? ("-" + sClusterTitle) : ""), legendX + legendDX + m_FontSize, legendY);
					legendY += legendDY;
				}
            }
		}
		
		m_Gx.endDraw();
	}
	
	void drawCutOffMax(PGraphics gx)
	{
		float cutoff = (float)m_Framework.getGroup(m_iGroupIndex, true).m_CutOffMax;
		float thresholdLineY = m_LargeRect.bottom() - m_LargeRect.height() * (cutoff - 1 + m_ZoomCoord.bottom()) / (m_ZoomCoord.bottom() - m_ZoomCoord.top());
		float topVal = 1 - m_ScrollY.value();
		float bottomVal = 1 - (m_ScrollY.value() + m_ScrollY.getVisibleAmount());
		
		float gradTop = cutoff > topVal ? m_CuttOffRect.top() : thresholdLineY;
		
		DrawUtils.gradientRect(gx, (int)m_CuttOffRect.left(), (int)gradTop, 
				m_CuttOffRect.width(), m_CuttOffRect.bottom() - gradTop,
				gx.lerpColor(dp.hmColor0, dp.hmColor1, cutoff > topVal ? topVal / cutoff : 1), 
				gx.lerpColor(dp.hmColor0, dp.hmColor1, bottomVal / cutoff), 
				DrawUtils.Y_AXIS);
		
		gx.noFill();
		gx.rect(m_CuttOffRect.left(), m_CuttOffRect.top(), m_CuttOffRect.width(), m_CuttOffRect.height());
			
		if (1 - cutoff > m_ZoomCoord.top() && 1 - cutoff < m_ZoomCoord.bottom())
		{// draw the cuttoff max line
			gx.strokeWeight(3);
			gx.line(m_CuttOffRect.left(), thresholdLineY, m_CuttOffRect.right(), thresholdLineY);
		}
	}
	
	void drawAxisLabels(PGraphics app)
	{
		String xLabel = "Relative Position";
		String yLabel = "";
		NumberFormat xnf = NumberFormat.getInstance();
		xnf.setMinimumFractionDigits(0);
		xnf.setMaximumFractionDigits(0);
		NumberFormat ynf = NumberFormat.getInstance();
		ynf.setMinimumFractionDigits(0);
		ynf.setMaximumFractionDigits(2);
		
		float leftVal = 0;
		float rightVal = 1;
		float topVal = m_bRPKM ? (float)m_MaxRPKM : 1.0f;
		float bottomVal = 0;
		
		if (m_ShowSummary)
		{
			//xLabel = "Coordinate";
			yLabel = m_bRPKM ? "RPKM" : "Mean";
			leftVal = 1;
			rightVal = m_Framework.getMaxRegionSize();
		}
		else {
			switch (dp.plotType)
			{
				case MEAN_STDDEV:
					//xLabel = "Coordinate";
					yLabel = "Mean +/- StdDev";
					leftVal = 1;
					rightVal = m_Framework.getMaxRegionSize();
					break;
				case QUANTILE:
					//xLabel = "Coordinate";
					yLabel = "Quartiles (Min-1stQ-Median-3rdQ-Max)";
					leftVal = 1;
					rightVal = m_Framework.getMaxRegionSize();
					break;
				case LOG_HIST:
					//xLabel = "Coordinate";
					yLabel = "Average Scatter & Mean";
					leftVal = 1;
					rightVal = m_Framework.getMaxRegionSize();
					break;
				case LOG_PEAK:
					//xLabel = "Coordinate";
					yLabel = "Peak Scatter & Mean";
					leftVal = 1;
					rightVal = m_Framework.getMaxRegionSize();
					break;
				case AVG_HIST:
					xLabel = "Average Signal Histogram";
					yLabel = "Signal Value";
					rightVal = m_ClustInfo.size() * 0.2f;
					break;
				case PEAK_HIST:
					xLabel = "Max Peak Signal Histogram";
					yLabel = "Signal Value";
					rightVal = m_ClustInfo.size() * 0.2f;
					break;
				case AVG_AND_PEAK:
					xLabel = "Average Signal Histogram      Peak Signal Histogram   ";
					yLabel = "Signal Value";
					leftVal = -m_ClustInfo.size() * 0.2f;
					rightVal = m_ClustInfo.size() * 0.2f;
					break;
				case AVG_VS_PEAK:
					xLabel = "Average Signal";
					yLabel = "Max Peak Signal";
					xnf.setMaximumFractionDigits(2);
					break;
				case MEAN:
					//xLabel = "Coordinate";
					yLabel = "Mean";
					rightVal = m_Framework.getMaxRegionSize();
					break;
			}
		}
		
		float mouseX = m_MouseState.x();
		float mouseY = m_MouseState.y();
		
		float leftTick = leftVal + (rightVal - leftVal) * m_ScrollX.value();
		float rightTick = leftVal + (rightVal - leftVal) * (m_ScrollX.value() + m_ScrollX.getVisibleAmount());
		float mousexTick = rightTick * (mouseX - m_LargeRect.left()) / m_LargeRect.width() + 
		 				   leftTick *  (m_LargeRect.right() - mouseX) / m_LargeRect.width(); 
		
		app.fill(0);
		app.textSize(m_FontSize);
		app.textAlign(PConstants.CENTER, PConstants.TOP);
		app.text(xLabel, m_LargeRect.hcenter(), m_LargeRect.bottom() + 25);
		if (m_LargeRect.isInside(mouseX, mouseY)) {
			app.text(xnf.format(mousexTick), mouseX, m_LargeRect.bottom() + 10);
		}
		app.textAlign(PConstants.LEFT, PConstants.TOP);
		app.text(xnf.format(leftTick), m_LargeRect.left(), m_LargeRect.bottom());
		app.textAlign(PConstants.RIGHT, PConstants.TOP);
		app.text(xnf.format(rightTick), m_LargeRect.right(), m_LargeRect.bottom());
		

		float topTick = topVal + (bottomVal - topVal) * m_ScrollY.value();
		float bottomTick = topVal + (bottomVal - topVal) * (m_ScrollY.value() + m_ScrollY.getVisibleAmount());
		float mouseyTick = bottomTick * (mouseY - m_LargeRect.top()) / m_LargeRect.height() + 
		                   topTick * (m_LargeRect.bottom() - mouseY) / m_LargeRect.height(); 

		app.textAlign(PConstants.CENTER, PConstants.BOTTOM);
		if (m_LargeRect.isInside(mouseX, mouseY) || m_CuttOffRect.isInside(mouseX, mouseY)) {
			DrawUtils.rotatedText(app, ynf.format(mouseyTick), m_LargeRect.left() - 10, mouseY, -PConstants.PI/2);
		}
		
		DrawUtils.rotatedText(app, yLabel, m_LargeRect.left() - 25, m_LargeRect.vcenter(), -PConstants.PI/2);
		app.textAlign(PConstants.RIGHT, PConstants.BOTTOM);
		DrawUtils.rotatedText(app, ynf.format(topTick), m_LargeRect.left(), m_LargeRect.top(), -PConstants.PI/2);
		app.textAlign(PConstants.LEFT, PConstants.BOTTOM);
		DrawUtils.rotatedText(app, ynf.format(bottomTick), m_LargeRect.left(), m_LargeRect.bottom(), -PConstants.PI/2);
		app.textAlign(PConstants.RIGHT, PConstants.BOTTOM);
		
		if (m_EnableThresholdLine) {
			DrawUtils.rotatedText(app, "Cutoff  ", m_LargeRect.left(), m_LargeRect.bottom(), -PConstants.PI/2);
		}
	}
	
	@Override
	public void updateInternalEvents(PApplet theApplet) 
	{
		super.updateInternalEvents(theApplet);
		
		if (!isVisible() || m_MouseState == null)
			return;
				
		if (m_EnableThresholdLine && m_MouseState.isPressed())
		{
			float threshold = (m_LargeRect.bottom() - m_MouseState.y()) * (m_ZoomCoord.bottom() - m_ZoomCoord.top()) / m_LargeRect.height() + 1 - m_ZoomCoord.bottom();  
			if (m_ClustInfo.getNumThresholds() > 0 && m_LargeRect.isInside(m_MouseState.x(), m_MouseState.y()))
			{
				m_ClustInfo.getThreshold(m_iGroupIndex).value = threshold;
				if (!m_ClustInfo.getThreshold(m_iGroupIndex).on && !m_ClustInfo.getThreshold(m_iGroupIndex).off)
				{
					m_ClustInfo.getThreshold(m_iGroupIndex).on = true;
					m_ClustInfo.getThreshold(m_iGroupIndex).off = true;
				}
				
				m_Framework.createThresholdClusters(m_ClustInfo);
				callChangeListeners(new ChangeEvent(this));
			}
			else if (m_CuttOffRect.isInside(m_MouseState.x(), m_MouseState.y()))
			{
				m_Framework.setGroupCuttOff(m_iGroupIndex, threshold);
				callChangeListeners(new ChangeEvent(this));
			}
		}
		
	}
	
	@Override
	public void keyEvent(KeyEvent theKeyEvent) {
//		if (theKeyEvent.getKeyCode() == KeyEvent.VK_ESCAPE)
//		{
//			hide();
//			ChangeEvent e = new ChangeEvent(this);
//			callChangeListeners(e);
//		}
	}
	
//    public void addChangeListener(ChangeListener listener)
//    {
//    	m_ChangeListener = listener; //TODO: make this a Vector if multiple listeners are needed
//    }
//    
//    public void removeChangeListener(ChangeListener listener)
//    {
//    	m_ChangeListener = null; //TODO: make this a Vector if multiple listeners are needed
//    }
//    
//    void callChangeListeners(ChangeEvent e)
//    {
//    	if (m_ChangeListener != null)
//    	{
//    		m_ChangeListener.stateChanged(e);
//    	}
//    }
}
