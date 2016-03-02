package org.sfu.chase.gui;

import java.util.ArrayList;

import org.sfu.chase.*;
import org.sfu.chase.core.ClustFramework;
import org.sfu.chase.core.ClustInfo;
import org.sfu.chase.core.GroupInfo;
import org.sfu.chase.gui.DrawUtils;
import org.sfu.chase.util.Utils;
import org.sfu.chase.util.Utils.Rect;

import processing.core.*;
import still.gui.MouseState;

public class ClusterDisplay
{
	ClustFramework        m_Framework;
	private ArrayList<ClustInfo>  m_SelectedClusters = new ArrayList<ClustInfo>();
	public ClustInfo      m_ClustDragged = null;
	public float          m_ClustDragX, m_ClustDragY;
	MouseState            m_MouseState;
	PApplet 	          m_Applet;
	
//	public ClustInfo      m_SelectedThresholdSet = null;
	public ClustInfo	  m_ActivePlotClust;
	private int			  m_ActivePlotGroup;

	public enum UIRegionType
	{
		CLUSTER,
		TREE_NODE
	}
	
	public class UIRegion
	{
		public UIRegionType 	type;
		public Utils.Rect 		rect;
		public Object			object;
	}
	//ArrayList<UIRegion> m_UIRegions = new ArrayList<UIRegion>(128); //REMOVE
	public UIRegion m_UIRegionMouseOver = null;
	
	public ClusterDisplay(PApplet theApplet)
	{
		m_Applet = theApplet;
	}

	public void setFramework(ClustFramework framework)
	{
		m_Framework = framework;
	}
	
	public void setMouseState(MouseState mouseState)
	{
		m_MouseState = mouseState;
	}
	
	public void setActivePlot(ClustInfo cInfo, int iGroup)
	{
		m_ActivePlotClust = cInfo;
		setActivePlotGroup(iGroup);
	}

	/**
	 * Draws a single plot for one group of a cluster
	 * @param gx
	 * @param dp
	 * @param cInfo
	 * @param iGroupIndex
	 * @param plotRect
	 */
	public void drawPlot(PGraphics gx, DisplayParams dp, ClustInfo cInfo, int iGroupIndex, Utils.Rect plotRect)
	{
		if (m_ActivePlotClust == cInfo && getActivePlotGroup() == iGroupIndex)
		{// highlight the currently selected cluster
			gx.stroke(255, 0, 0);
			gx.strokeWeight(2);
			gx.rect(plotRect.left(), plotRect.top(), plotRect.width(), plotRect.height());
			gx.strokeWeight(1);
		}
		
		if (cInfo.getStats() == null || m_Framework.getGroup(iGroupIndex, true) == null)
			return;
		
		double cuttoffMax = m_Framework.getGroup(iGroupIndex, true).m_CutOffMax;
		int gi = m_Framework.getGroupOrder(iGroupIndex, true);

		PGraphics plotImage = null;
		int numCols = 0;
		int numBins = 0;
		
		GroupInfo group = m_Framework.getGroup(iGroupIndex, true);
		int colPalIndex = group.m_Experiment.getColor().getColorIndex();
		
		if (dp.plotType == DisplayParams.PlotType.LOG_HIST ||
			dp.plotType == DisplayParams.PlotType.LOG_PEAK)
		{
			double[][] hist = (dp.plotType == DisplayParams.PlotType.LOG_HIST) ?
							  cInfo.getStats().m_ColHist : cInfo.getStats().m_ColPeaks;  
			
			int numGroups = m_Framework.getNumGroups();
			int groupDim  = m_Framework.getGroupDim();
			if (hist != null && hist[0] != null)
			{
				numCols = hist.length / numGroups; // number of columns per group
				numBins = hist[0].length; // number of histogram bins column
				plotImage = m_Applet.createGraphics(numCols, numBins, PGraphics.P2D);
				plotImage.loadPixels();
				for (int b = 0; b < numBins; b++)
				{
					for (int c = 0; c < numCols; c++)
					{
						int bn = (int)(b * cuttoffMax);
						double value = hist[c + gi*groupDim][bn];
						value = Math.log(1 + value) / Math.log(1 + cInfo.size());
						
						int clr = value < 0.5 ? 
								m_Applet.lerpColor(ColorPalette.COLOR_MIN[colPalIndex], ColorPalette.COLOR_MED[colPalIndex], (float)value*2) : 
								m_Applet.lerpColor(ColorPalette.COLOR_MED[colPalIndex], ColorPalette.COLOR_MAX[colPalIndex], (float)value*2 - 1);
						
						plotImage.pixels[b*numCols + c] = clr | 0xFF000000;
					}
				}            
				plotImage.updatePixels();
			}
		}
		else if (dp.plotType == DisplayParams.PlotType.AVG_VS_PEAK && cInfo.getStats().m_MeanVsPeak != null)
		{
			double[][] hist = cInfo.getStats().m_MeanVsPeak[gi];  
			
			if (hist != null && hist[0] != null)
			{
				numCols = hist.length;
				numBins = hist[0].length;
				double dMax = Double.NEGATIVE_INFINITY;
				for (int b = 0; b < numBins; b++) {
					for (int c = 0; c < numCols; c++) {
						dMax = Math.max(hist[c][b] / cuttoffMax, dMax);
					}
				}
				
				plotImage = m_Applet.createGraphics(numCols, numBins, PGraphics.P2D);
				plotImage.loadPixels();
				for (int b = 0; b < numBins; b++)
				{
					for (int c = 0; c < numCols; c++)
					{
						double value = hist[c][b] / cuttoffMax;
						boolean bLog = true;
						if (bLog)
							value = Math.log(1 + value) / Math.log(1 + dMax);
						else //if (dp.plotType == DisplayParams.PlotType.HIST || dp.plotType == DisplayParams.PlotType.PEAK)
							value /= dMax;
						
						int clr = value < 0.5 ? 
								m_Applet.lerpColor(ColorPalette.COLOR_MIN[colPalIndex], ColorPalette.COLOR_MED[colPalIndex], (float)value*2) : 
								m_Applet.lerpColor(ColorPalette.COLOR_MED[colPalIndex], ColorPalette.COLOR_MAX[colPalIndex], (float)value*2 - 1);
						
						plotImage.pixels[b*numCols + c] = clr | 0xFF000000;
					}
				}            
				plotImage.updatePixels();
			}
		}
		
		if (plotImage != null)
		{
			gx.imageMode(PGraphics.CORNER);
			gx.noSmooth();
			gx.image(plotImage, plotRect.left(), plotRect.top(), plotRect.width(), plotRect.height(),
					0, numBins, numCols, 0);
			gx.smooth();
		}
		
		if (cInfo.getStats().m_Count > 0)
		{
			int[] groupCols = m_Framework.getGroup(gi, false).m_iCols; // gi is already the ordered index
			if (dp.plotType == DisplayParams.PlotType.QUANTILE)
			{
				double[] colMedian = null;
				gx.noStroke();
				double[] colQ0 = null;
				int[] indices = Utils.concatArray(Utils.intSequence(0, groupCols.length - 1, 1), Utils.intSequence(groupCols.length - 1, 0, -1));
				
				for (int q = 0; q < cInfo.getStats().m_NumQuantiles; q++)
				{// draw all the quantile curves
					double[] colQ1 = Utils.subArray(cInfo.getStats().m_ColQuantile[q], groupCols);
					if (colQ0 != null)
					{
						double[] colQ0Q1 = Utils.concatArray(colQ1,Utils.reverseArray(colQ0)); // q1_q0
						gx.fill(dp.colorQuantiles[q]);
						DrawUtils.drawProfile(gx, colQ0Q1, indices, 0, cuttoffMax, plotRect.left(), plotRect.top(), plotRect.width()*2, plotRect.height(), true);
					}
					colQ0 = colQ1;
					if (q == cInfo.getStats().medianIndex())
						colMedian = colQ1;
				}
				
				// draw the median
				colMedian = Utils.subArray(cInfo.getStats().m_ColQuantile[cInfo.getStats().medianIndex()], groupCols);
				gx.noFill();
				gx.stroke(dp.colorPlotMedian);
				gx.strokeWeight(1.25f);
				DrawUtils.drawProfile(gx, colMedian, null, 0, cuttoffMax, plotRect.left(), plotRect.top(), plotRect.width(), plotRect.height(), false);
			}
			else if (dp.plotType == DisplayParams.PlotType.MEAN_STDDEV || 
					 dp.plotType == DisplayParams.PlotType.LOG_HIST ||
					 dp.plotType == DisplayParams.PlotType.LOG_PEAK ||
					 dp.plotType == DisplayParams.PlotType.MEAN)
			{
				double[] colMean = Utils.subArray(cInfo.getStats().m_ColMean, groupCols);
				if (dp.plotType == DisplayParams.PlotType.MEAN_STDDEV)
				{
					double[] colStdDev = Utils.subArray(cInfo.getStats().m_ColStdDev, groupCols);
			
					// mean +/- stddev
					double[] colMeanStdDev = Utils.clampArray(Utils.concatArray(Utils.sumArray(colMean, colStdDev),
							                                                    Utils.reverseArray(Utils.sumArray(colMean, Utils.multArray(colStdDev, -1)))), 0, 1);
					int[] indices = Utils.concatArray(Utils.intSequence(0, groupCols.length - 1, 1), Utils.intSequence(groupCols.length - 1, 0, -1));
					gx.stroke(dp.colorPlotStdDev);
					gx.fill(dp.colorPlotStdDev);
					DrawUtils.drawProfile(gx, colMeanStdDev, indices, 0, cuttoffMax, plotRect.left(), plotRect.top(), plotRect.width()*2, plotRect.height(), true);
				}
				// mean
				gx.noFill();
				gx.stroke(dp.colorPlotMean);
				gx.strokeWeight(1.25f);
				DrawUtils.drawProfile(gx, colMean, null, 0, cuttoffMax, plotRect.left(), plotRect.top(), plotRect.width(), plotRect.height(), false);
			}
			else if (dp.plotType == DisplayParams.PlotType.PEAK_HIST ||
					 dp.plotType == DisplayParams.PlotType.AVG_HIST)
			{
				gx.fill(255);
				gx.strokeWeight(1);
				gx.stroke(128);
				gx.rect(plotRect.left(), plotRect.top(), plotRect.width(), plotRect.height());
				
				// draw the peak histogram
				gx.pushMatrix();
				
				//gx.translate(plotRect.left(), plotRect.bottom());
				gx.rotate(-PApplet.PI/2);
				gx.scale(1, -1f);
				
				gx.stroke(dp.plotType == DisplayParams.PlotType.AVG_HIST ? 0xFF008800 : 0xFF008888);
				gx.fill(dp.plotType == DisplayParams.PlotType.AVG_HIST ? 0xFF00BB00 : 0xFF00BBBB);
				
				double histMax = cInfo.size() * 0.2; // set max to %20
				double[] hist = dp.plotType == DisplayParams.PlotType.AVG_HIST ? 
								cInfo.getStats().m_MeanHist[gi] : cInfo.getStats().m_PeakHist[gi];
				//DrawUtils.drawProfile(gx, hist, null, 0, histMax, -plotRect.bottom(), plotRect.left(), plotRect.height(), plotRect.width(), true);
				DrawUtils.drawHistogram(gx, hist, 0, histMax, -plotRect.bottom(), -plotRect.right(), plotRect.height(), plotRect.width());
				
				gx.popMatrix();
			}
			else if (dp.plotType == DisplayParams.PlotType.AVG_AND_PEAK)
			{
				gx.fill(255);
				gx.strokeWeight(1);
				gx.stroke(128);
				gx.rect(plotRect.left(), plotRect.top(), plotRect.width()/2, plotRect.height());
				gx.rect(plotRect.hcenter(), plotRect.top(), plotRect.width()/2, plotRect.height());

				// draw the peak histogram
				gx.pushMatrix();
				
				//gx.translate(plotRect.left(), plotRect.bottom());
				
				gx.rotate(-PApplet.PI/2);

				gx.stroke(0, 128, 0);
				gx.fill(0, 180, 0);
				
				double histMax = cInfo.size() * 0.2; // set max to %20
				//ClusterDisplay.DrawUtils.drawProfile(gx, cInfo.m_Stats.m_MeanHist[gi], null, 0, histMax, plotRect.top() - plotRect.height(), plotRect.left(), plotRect.height(), plotRect.width()/2, true);
				DrawUtils.drawProfile(gx, cInfo.getStats().m_MeanHist[gi], null, 0, histMax, -plotRect.bottom(), plotRect.left(), plotRect.height(), plotRect.width() / 2, true);
				gx.fill(0, 180, 180);
				gx.scale(1, -1f);
				DrawUtils.drawProfile(gx, cInfo.getStats().m_PeakHist[gi], null, 0, histMax, -plotRect.bottom(), -plotRect.right(), plotRect.height(), plotRect.width()/2, true);
				
				gx.popMatrix();
			}
		}
	}
	
	/**
	 * Draws the summary plot overlapping the mean plot for all groups of a clusters
	 * @param gx
	 * @param dp
	 * @param cInfo
	 * @param iGroupIndex
	 * @param plotRect
	 */
	public void drawSummaryPlot(PGraphics gx, DisplayParams dp, ClustInfo cInfo, Utils.Rect plotRect, int hiGroupIndex, boolean bDisplaySize)
	{/*
		gx.noFill();
		for (int gi = 0; gi < m_Framework.getNumGroups(); gi++)
		{
			double cuttoffMax = m_Framework.getGroup(gi, true).m_CutOffMax;
			int[] groupCols = m_Framework.getGroup(gi, true).m_iCols;
			double[] colMean = Utils.subArray(cInfo.getStats().m_ColMean, groupCols);
			DrawUtils.drawProfile(gx, colMean, null, 0, cuttoffMax, plotRect.left(), plotRect.top(), plotRect.width(), plotRect.height(), false);
		}
		*/
		
		int numGroups = m_Framework.getNumGroups();
		gx.strokeWeight(1);
		gx.fill(0);
		gx.textAlign(PGraphics.RIGHT, PGraphics.BOTTOM);
		int showSize = 100 * cInfo.size() / m_Framework.getDataSize();
		String sizeStr = showSize > 0 ? ("["+ showSize + "%]") : ("" + cInfo.size());
		gx.textSize(dp.plotGapY/2); 
		gx.text(sizeStr, plotRect.left() + plotRect.width() - 2, plotRect.top());
		
		if (cInfo.getStats().m_Count > 0)
		{
			for (int i = 0; i <= numGroups; ++i)
			{// draw summary plot profiles
				int gi = i < numGroups ? i : hiGroupIndex; // draw the highlighted group at the last iteration
				if ((gi < 0) || (gi >= numGroups))
					continue;
				
				double cuttoffMax = m_Framework.getGroup(gi, true).m_CutOffMax;
				
				int[] groupCols = m_Framework.getGroup(gi, true).m_iCols;
				double[] colProfile = dp.plotType == DisplayParams.PlotType.QUANTILE ?
									  Utils.subArray(cInfo.getStats().m_ColQuantile[cInfo.getStats().medianIndex()], groupCols) : // show median profile
									  Utils.subArray(cInfo.getStats().m_ColMean, groupCols); // show mean profile
				
				gx.stroke(hiGroupIndex == gi ? dp.plotType == DisplayParams.PlotType.QUANTILE ? dp.colorPlotMedian : dp.colorPlotMean : 128);
				gx.strokeWeight(hiGroupIndex == gi ? 3 : 1.5f);
				gx.noFill();
				DrawUtils.drawProfile(gx, colProfile, null, 0, cuttoffMax, plotRect.left(), plotRect.top(), plotRect.width(), plotRect.height(), false);
				//DrawUtils.drawProfile(gx, colProfile, null, 0, 1, dp.summaryLeft, plotY, dp.plotW, dp.plotH, false);
			}
		}		
	}
	
	/**
	 * Draws one row of plots for one cluster
	 * @param gx        reference to the PGraphics object
	 * @param dp       reference to the display params
	 * @param cInfo    reference to the cluster info
	 * @param fLeft    left coordinate of the left most plot
	 * @param fTop     top coordinate
	 */
	public void drawOneCluster(PGraphics gx, DisplayParams dp, ClustInfo cInfo, float fLeft, float fTop, Rect clipRect)
	{
		int numGroups = m_Framework.getNumGroups();

		Utils.Rect ciRect = new Utils.Rect(dp.summaryLeft, fTop, numGroups * (dp.plotW + dp.plotGapX) + clipRect.left() - dp.summaryLeft, dp.plotH);
		if (ciRect.isInside(m_MouseState.x(), m_MouseState.y()))
		{
			// draw a rectangle around the cluser currently mouse over
			//gx.stroke(dp.colorClustSelect);
			//gx.strokeWeight(1.f);
			//gx.noFill();
			//gx.rect(ciRect.left()-4, ciRect.top() - dp.clustGapY + 4, ciRect.width()+8, ciRect.height() + dp.clustGapY);
			
			m_UIRegionMouseOver = new UIRegion();
			m_UIRegionMouseOver.rect = ciRect;
			m_UIRegionMouseOver.type = UIRegionType.CLUSTER;
			m_UIRegionMouseOver.object = cInfo;
		}
		
		gx.pushStyle();
		
		// selection box:
		/*
		if (cInfo == m_ClustSelected)
		{
			gx.fill(0x33000000);
			gx.noStroke();
			gx.rect(dp.summaryLeft - 2, fTop - 2, clipRect.right() - dp.summaryLeft +4, dp.plotH+4);
		}*/
		
		float plotX = fLeft;
		float plotY = fTop;
		float plotDX = dp.plotW + dp.plotGapX;
		int hiGroupIndex = -1;//clustRect.isInside(m_MouseState.x(), m_MouseState.y()) ? (int)((m_MouseState.x() - fLeft) /  plotDX) : -1; // highlighted group
		

		for (int gi = 0; gi < numGroups; ++gi, plotX += plotDX)
		{
			if (!m_Framework.getGroup(gi, true).m_Visible && !dp.drawInvisibleGroups)
			{
				plotX -= plotDX;
				continue;
			}
			
			Utils.Rect plotRect = new Utils.Rect(plotX, plotY, dp.plotW, dp.plotH);
			float fRatio = plotRect.left() < clipRect.left() ? 
	                   Math.min(Math.max(2*(plotRect.left() - clipRect.left()) / plotRect.width() + 1, 0), 1) : // fade on the left
	                	   plotRect.left() + plotRect.width() > clipRect.left() ? 
				       Math.min(Math.max(2*(clipRect.right() - plotRect.left() - plotRect.width()) / plotRect.width() + 1, 0), 1) : 1; // fade on the right
			if (fRatio == 0)
				continue;
			
			if (plotRect.right() < clipRect.left())
				continue; // left of the clip rect. continue
			else if (plotRect.left() > clipRect.right())
				break; // right of the clip rect. no need to draw the rest
			if (plotRect.isInside(m_MouseState.x(), m_MouseState.y()))
				hiGroupIndex = gi;
			
			if (hiGroupIndex == gi)
			{
				setActivePlot(cInfo, hiGroupIndex);
			}

			gx.strokeWeight(1);
			gx.noStroke();//stroke(128);
			gx.fill(dp.colorPlotBG);
			//gx.rect(plotRect.left(), plotRect.top(), plotRect.width(), plotRect.height());
				      
			try
			{
				drawPlot(gx, dp, cInfo, gi, plotRect);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			
			gx.strokeWeight(1);
			gx.stroke(128, 60);
			gx.noFill();
			gx.rect(plotRect.left(), plotRect.top(), plotRect.width(), plotRect.height());

			if (fRatio < 1 && fRatio > 0)
			{
				gx.noStroke();
				gx.fill(255, 255 - (int)(fRatio*255));
				gx.rect(plotRect.left()-2, plotRect.top()-2, plotRect.width()+4, plotRect.height()+4);
			}
		}

		if (dp.drawSummaryPlots)
		{	
			Utils.Rect summaryRect = new Utils.Rect(dp.summaryLeft, plotY, dp.plotW, dp.plotH);
			gx.smooth();
			// summary plot border and size
			if (isClusterSelected(cInfo)) {
				m_TopSelected = plotY;
				gx.stroke(dp.hmColor1);
				gx.strokeWeight(2);
			} else {
				gx.stroke(128, 60);
				gx.strokeWeight(1);
			}
			gx.fill (255);
			gx.rect(summaryRect.left(), summaryRect.top(), summaryRect.width(), summaryRect.height());
			drawSummaryPlot(gx, dp, cInfo, summaryRect, hiGroupIndex, true);
		}
		
		gx.popStyle();
	}
	
	/**
	 * Draws all the plots for multiple clusters
	 * @param gx       reference to PGraphics object
	 * @param dp      reference to display params
	 * @param fLeft   left coordinate
	 * @param fTop    top coordinate of the first to draw
	 */
	public void drawMultipleClusters(PGraphics gx, DisplayParams dp, ClustInfo[] clustInfos, float fLeft, float fTop, Rect clipRect)
	{
		gx.pushStyle();

		int numGroups = m_Framework.getNumGroups();
		//float dragClustLeft = m_MouseState.x - m_ClustDragX;
		float dragClustTop  = m_MouseState.y() - m_ClustDragY;
		
		float y = fTop + dp.plotGapY;
		for (int ci = 0; ci < clustInfos.length; ++ci, y += dp.plotH + dp.plotGapY)
		{
			if (y + dp.plotH < clipRect.top())
				continue; // above the clip rect. go to next.
			else if (y > clipRect.bottom())
				break; // below the clip rect no need to draw the rest
			
			try {
				drawOneCluster(gx, dp, clustInfos[ci], fLeft, y, new Rect(clipRect.left(), y, clipRect.width(), dp.plotH + dp.plotGapY));
			} catch (Exception e) {
				// to make sure an error in rendering one cluster, won't break the entire rendering
				e.printStackTrace();
			}
			
			gx.strokeWeight(1);
			
			// draw the cluster info label (e.g. on/off state)
			float lx = fLeft + dp.plotW; // label x
			float dlx = dp.plotW + dp.plotGapX;
			for (int gi = 0; gi < numGroups; ++gi, lx += dlx)
			{
				if (!m_Framework.getGroup(gi, true).m_Visible && !dp.drawInvisibleGroups)
				{
					lx -= dlx;
					continue;
				}
				
				if (lx < clipRect.left() || lx > clipRect.right())
					continue;
				
				if (clustInfos[ci].getNumInfoLabels() == numGroups)
				{
					if (clustInfos[ci].getInfoLabel(m_Framework.getGroupOrder(gi, true)) != ' ')
					{
						gx.stroke(128);
						if (clustInfos[ci].getInfoLabel(m_Framework.getGroupOrder(gi, true)) == '^') {
							gx.fill(128);
						} else {
							gx.noFill();
						}
						gx.ellipse(lx - dlx/2, y - dp.checkBoxSize/2 - 6, dp.checkBoxSize/2, dp.checkBoxSize/2);
						
						if (clustInfos[ci].getInfoLabel(m_Framework.getGroupOrder(gi, true)) == 'v') {
							gx.fill(128);
						} else {
							gx.noFill();
						}
						gx.ellipse(lx - dlx/2, y - 4, dp.checkBoxSize/2, dp.checkBoxSize/2);
					}
				}
				
				dp.drawCheckBoxes = false;
				if (dp.drawCheckBoxes && clustInfos[ci].getNumKmeansActiveGroups() == numGroups && isClusterSelected(clustInfos[ci]))
				{
					Rect cbRect = new Rect(lx - dp.plotW, y - dp.checkBoxSize, dp.plotW, dp.checkBoxSize);
					if (clustInfos[ci].getKmeansActiveGroup(gi))
					{
						gx.fill(0);
						gx.textFont(dp.fontGuiFx);
						gx.textSize(dp.checkBoxSize * 2);
						gx.text("z", cbRect.hcenter(), cbRect.bottom()); // checkmark
					}
				}
			}
			
			// select only if mouse over the summary plot
			Utils.Rect ciRect = new Utils.Rect(dp.summaryLeft, y, numGroups * (dp.plotW + dp.plotGapX) + clipRect.left() - dp.summaryLeft, dp.plotH);
			if (ciRect.isInside(m_MouseState.x(), m_MouseState.y()))
			{
				// draw a rectangle around the cluser currently mouse over
				//gx.stroke(dp.colorClustSelect);
				//gx.strokeWeight(1.f);
				//gx.noFill();
				//gx.rect(ciRect.left()-4, ciRect.top() - dp.clustGapY + 4, ciRect.width()+8, ciRect.height() + dp.clustGapY);
				
				m_UIRegionMouseOver = new UIRegion();
				m_UIRegionMouseOver.rect = ciRect;
				m_UIRegionMouseOver.type = UIRegionType.CLUSTER;
				m_UIRegionMouseOver.object = clustInfos[ci];
			}
			
//			if (cInfo[ci] == m_ClustSelected)
//			{ // connect currently selected cluster to the heatmap
//				float fSummaryLeft = fLeft + (numGroups + 1) * (vp.plotW + vp.clustGapX);
//				gx.stroke(200);
//				gx.strokeWeight(1.f);
//				gx.line (fSummaryLeft + vp.plotW, fTop, vp.hmRect.left() - 15, vp.hmRect.top());
//				gx.line (fSummaryLeft + vp.plotW, fTop + vp.plotH, vp.hmRect.left() - 15, vp.hmRect.top() + hmp.currHeight);
//			}
			
			// snap:
//			if (Math.abs(dragClustLeft - fLeft) < 10 && Math.abs(dragClustTop - fTop) < 10)
//			{
//				dragClustLeft = fLeft;
//				dragClustTop = fTop;
//			}
		}
		
		if (m_ClustDragged != null)
		{
			int currColor = dp.colorPlotBG;
			dp.colorPlotBG = (dp.colorPlotBG & 0x00FFFFFF) | 0x7F000000; // make the background transparent
			drawOneCluster(gx, dp, m_ClustDragged, fLeft, dragClustTop, new Rect(clipRect.left(), dragClustTop, clipRect.width(), dp.plotH + dp.plotGapY));
			dp.colorPlotBG = currColor; 
		}
		
//		if (m_ClustSelected != null)
//		{
//			drawClusterPlot(gx, m_ClustSelected, dp.hmRect.left(), dp.toolbarRect.top() + 5);
//		}

		
		//drawGroupLabels(gx, frameRect.left(), clipRect.top(), dp.clustGapX + dp.plotW);
		
		gx.popStyle();
	}
	
	void drawClusterInfoLabel(PGraphics gx, char cil, float lx, float ly, float ls)
	{
		if (cil == 'v' || cil == '^')
		{
			gx.noFill(); gx.noStroke();
			if (cil == '^')
			{
				gx.fill(64, 180, 64);
				gx.beginShape();
				gx.vertex(lx - ls, ly);
				gx.vertex(lx + ls, ly);
				gx.vertex(lx, ly - ls);
				gx.endShape(PGraphics.CLOSE);
			}
			else
			{
				gx.stroke(0, 128, 0);
				gx.strokeWeight(0.5f);
				gx.beginShape();
				gx.vertex(lx - ls, ly - ls);
				gx.vertex(lx + ls, ly - ls);
				gx.vertex(lx, ly);
				gx.endShape(PGraphics.CLOSE);
			}
			
		}
	}
	
	public void drawGroupLabels(PGraphics gx, DisplayParams dp, float x, float y, float dx, Utils.Rect clipRect, boolean offsetSortedMark)
	{
		gx.textAlign(PGraphics.LEFT, PGraphics.CENTER);
		int iNumGroups = m_Framework.getNumGroups();
		// draw the group labels on the top
		int fillColor = gx.fillColor;
		
		float groupW = dx;
		float groupX = x;// + g1 * gw;
		
		for (int g1 = 0; g1 < iNumGroups; g1++, groupX += groupW)
		{
			if (!m_Framework.getGroup(g1, true).m_Visible && !dp.drawInvisibleGroups)
			{
				groupX -= groupW;
				continue;
			}
			
			gx.fill(g1 == getActivePlotGroup() ? 0xFFFF0000 : fillColor);
			String sGroupName = m_Framework.getGroup(g1, true).m_Name;
			if (sGroupName == null)
				continue;
			
			while (gx.textWidth(sGroupName) > dp.clustCaptionH - 10) {
			    sGroupName = sGroupName.substring(0, sGroupName.length() - 2);
			}
	        //int maxLength = 20;
			//if (sGroupName.length() > maxLength)
			//	sGroupName = sGroupName.substring(0, maxLength) + "...";
			
			if (clipRect != null && !clipRect.isInside(groupX + 0.1f * groupW, y))
				continue;
			
			gx.pushMatrix();
				//gx.translate(groupX + 0.1f * groupW, y);
				//gx.rotate(-PGraphics.PI/4);
				gx.translate(groupX + 0.5f * groupW, y - 5);
				if (offsetSortedMark && dp.hmSortGroup == m_Framework.getGroupOrder(g1, true)) {
				    sGroupName = "   " + sGroupName; // moves the label upwards (to show the sort order arrow underneath)
				}
				gx.rotate(-PGraphics.PI/2);
				gx.text(sGroupName, 0, 0);
			gx.popMatrix();
		}		
	}

	public float m_TopSelected;
	// returns the number of rows added
	public int drawTree(PGraphics gx, DisplayParams dp, ClustInfo cInfo, float fLeft, float fTop)
	{
		int colorTree = dp.colorTree;
		
		if (isClusterSelected(cInfo)) {
			// highlight the color of the tree for selected clusters
			m_TopSelected = fTop - dp.plotH/2;
			dp.colorTree = dp.hmColor1;
		}
		
		int rows = 0;
		
		if (cInfo.m_Child == null || !cInfo.m_bShowChildren)
		{
			rows = 1;
		}
		else
		{
			ClustInfo childInfo = cInfo.m_Child;
			while (childInfo != null)
			{
				float childX = fLeft + dp.treeGapX;
				float childY = fTop + rows * (dp.plotH + dp.plotGapY);
				gx.strokeWeight(dp.treeStrokeWidth);
				gx.stroke(dp.colorTree);
				
				if (childY > dp.treeRect.top())
				{
					if (childInfo == cInfo.m_Child) // first child
					{
						gx.line(fLeft, fTop, childX - dp.treeChildRadius, childY);
					}
					else
					{
						gx.noFill();
						gx.beginShape();
						float rr = dp.treeGapX*0.7f;
						gx.vertex(fLeft + dp.treeGapX/2, Math.max(fTop, dp.treeRect.top()));
						gx.vertex(fLeft + dp.treeGapX/2, childY - rr);
						gx.bezierVertex(fLeft + dp.treeGapX/2, childY,  fLeft+rr, childY, childX, childY);
						gx.vertex(childX, childY);
						gx.endShape();
					}
				}
	
				rows += drawTree(gx, dp, childInfo, childX, childY);
	
				childInfo = childInfo.m_Sibling;
			}
		}
		
		if (cInfo.m_Clone != null)
		{
			float cloneX = fLeft;
			float cloneY = fTop + rows * (dp.plotH + dp.plotGapY);
			gx.strokeWeight(dp.treeStrokeWidth);
			gx.stroke(dp.colorTree);
			DrawUtils.dashline(gx, fLeft, fTop, cloneX, cloneY, 3, 10);
			rows += drawTree(gx, dp, cInfo.m_Clone, cloneX, cloneY);
		}
		
		if (dp.treeRect.isInside(fLeft, fTop))
		{
			float radius = cInfo.m_Child == null ? dp.treeChildRadius : dp.treeParentRadius;
			gx.stroke(dp.colorTree);
			gx.strokeWeight(dp.treeStrokeWidth);
			gx.ellipseMode(PGraphics.CENTER);
			
			// draw circles for tree nodes
			if (cInfo.m_Child == null)
			{// leaf node
				gx.fill(dp.colorTree);
				gx.ellipse(fLeft, fTop, radius*2, radius*2);
			}
			else
			{// parent node
				gx.fill(dp.colorWindowBG);
				gx.ellipse(fLeft, fTop, radius*2, radius*2);
				gx.line(fLeft - radius+dp.treeStrokeWidth+1, fTop, fLeft + radius-dp.treeStrokeWidth-1, fTop);
				if (!cInfo.m_bShowChildren)
				{
					gx.line(fLeft, fTop - radius+dp.treeStrokeWidth+1, fLeft, fTop + radius-dp.treeStrokeWidth-1);
				}
			}
			
			// draw cluster title
			if (!cInfo.getTitle().isEmpty())
			{
				gx.fill(dp.colorTree);
				gx.textSize(12); //TODO: add to DisplayParams
				gx.pushMatrix();

				if (rows == 1) { // no child node. draw title horizontally
					gx.textAlign(PConstants.LEFT, PConstants.TOP);
					gx.translate(fLeft - dp.treeParentRadius, fTop + dp.treeParentRadius);
				} else { // has children. draw title vertically
					gx.textAlign(PConstants.RIGHT, PConstants.TOP);
					gx.translate(fLeft - dp.treeParentRadius, fTop + dp.treeParentRadius*2);
					gx.rotate((float)-Math.PI / 2);
				}
				gx.text(cInfo.getTitle(), 0, 0);
				gx.popMatrix();
			}
			
			Utils.Rect nodeRect = new Utils.Rect(fLeft - radius - 5, fTop - radius - 5, radius*2 + 10, radius*2 + 10);
			
			if (nodeRect.isInside(m_MouseState.x(), m_MouseState.y()))
			{
				// draw a circle around the node under the mouse
				//gx.stroke(dp.colorTreeSelect);
				gx.noFill();
				gx.ellipse(fLeft, fTop, radius*2 + 6, radius*2 + 6);
	
				m_UIRegionMouseOver = new UIRegion();
				m_UIRegionMouseOver.rect = nodeRect;
				m_UIRegionMouseOver.type = UIRegionType.TREE_NODE;
				m_UIRegionMouseOver.object = cInfo;
			}
		}
		dp.colorTree = colorTree;
		
		return rows;
	}
	
	public void drawNumberOfThresholdRegions(PGraphics gx, ClustInfo cInfo, int groupIndex, float fThreshold, float fx, float fy, boolean detailed)
	{
		// draw the number of on/off data for each group
		int numOn = m_Framework.countOnRegions(cInfo, groupIndex, fThreshold);
		float fSize = 100.f * numOn / cInfo.size();
		String sizeStr = fSize > 1 ? ((int)Math.floor(fSize) + "%") : ("" + numOn);
		if (fSize > 1 && detailed)
			sizeStr += " (" + numOn + ")";
		gx.textAlign(PApplet.CENTER, PApplet.BOTTOM);
		gx.text(sizeStr, fx, fy);
		fSize = 100 - fSize;
		sizeStr = fSize > 1 ? ((int)Math.floor(fSize) + "%") : ("" + (cInfo.size() - numOn));
		if (fSize > 1 && detailed)
			sizeStr += " (" + (cInfo.size() - numOn) + ")";
		gx.textAlign(PApplet.CENTER, PApplet.TOP);
		gx.text(sizeStr, fx, fy);
	}
	
	public boolean dragCluster(ClustInfo cInfo)
	{
		//if (m_ClustDragged == null)
		//{
		//	m_ClustDragged = cInfo;
		//	return true;
		//}
		return false;
	}
	
	public boolean dropCluster(ClustInfo cInfo)
	{
		if (m_ClustDragged != null && m_ClustDragged != cInfo)
		{
			m_Framework.mergeClusters(cInfo, m_ClustDragged);
		}
		m_ClustDragged = null;
		return false;
	}

	public void setActivePlotGroup(int activePlotGroup) {
		m_ActivePlotGroup = activePlotGroup;
	}

	public int getActivePlotGroup() {
		return m_ActivePlotGroup;
	}
	
	public ClustInfo getSelectedCluster(int index)
	{
		if (index >= 0 && index < m_SelectedClusters.size()) {
			return m_SelectedClusters.get(index);
		}
		return null;
	}
	
	public ClustInfo[] getAllSelectedClusters()
	{
		if (m_SelectedClusters.size() > 0) {
			return m_SelectedClusters.toArray(new ClustInfo[m_SelectedClusters.size()]);
		}
		return null;
	}
	
	
	public void selectCluster(ClustInfo cInfo)
	{
		m_SelectedClusters.clear();
		if (cInfo != null) {
			m_SelectedClusters.add(cInfo);
		}
	}
	
	public int getNumSelectedClusters()
	{
		return m_SelectedClusters.size();
	}
	
	public void addClusterToSelection(ClustInfo cInfo)
	{
		if (cInfo != null && !m_SelectedClusters.contains(cInfo))
		{
			m_SelectedClusters.add(cInfo);
		}
	}
	
	public void toggleClusterSelection(ClustInfo cInfo)
	{
		if (!m_SelectedClusters.contains(cInfo))
		{
			m_SelectedClusters.add(cInfo);
		} else if (m_SelectedClusters.size() > 1) // make sure the list won't get empty
		{
			m_SelectedClusters.remove(cInfo);
		}
	}
	
	public boolean isClusterSelected(ClustInfo cInfo)
	{
		return m_SelectedClusters.contains(cInfo);
	}
	
	public int getSelectedClustersTotalSize()
	{
		int iTotalSize = 0;
		for (int i = 0; i < m_SelectedClusters.size(); ++i) {
			ClustInfo cInfo = m_SelectedClusters.get(i);
			if (cInfo != null) {
				iTotalSize = cInfo.size();
			}
		}
		return iTotalSize;
	}
}
