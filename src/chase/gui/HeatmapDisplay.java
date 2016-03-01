package chase.gui;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.commons.lang3.ArrayUtils;

import processing.core.*;
import processing.pdf.*;
import still.gui.MouseState;
import chase.*;

public class HeatmapDisplay
{
	public ClustFramework m_Framework;
	public PApplet 		  m_Applet;
	int m_SelectionStart;
	int m_SelectionEnd;
	boolean m_bSelected;
	int[] m_SelectedIndices;
	boolean	m_bFastUpdateSelection = false;
	ClustInfo[] m_Nodes;   // array of latest nodes drawn with heat map
	double [] m_NodesYPos; // array of the y positions of the nodes in the heat map
	
	public ClustInfo[] getNodes()
	{
		return m_Nodes;
	}
	
	public double[] getNodesYPos() 
	{
		return m_NodesYPos;
	}
	
	public HeatmapDisplay(PApplet theApplet)
	{
		m_Applet = theApplet;
	}
	
	
	public void setFramework(ClustFramework framework)
	{
		m_Framework = framework;
	}
	
	public PImage renderHeatmap(int[] sortedIndices, DisplayParams dp1)
	{
		if (sortedIndices == null) {
			return null;
		}
		
		int[] columns = m_Framework.getVisibleColumns(true, true);
		int iNumCols = columns.length;// m_Framework.getDataDim();
		int iGroupDims = m_Framework.getGroupDim();
		
		int iClustSize = sortedIndices.length;
		
		int currTopRow  = Math.max(0, (int) (dp1.hmScrollYOffset * iClustSize));
		int currNumRows = Math.min(Math.max((int) (dp1.hmScrollYRange * iClustSize), (int)dp1.hmRect.height()), iClustSize - currTopRow);
		if (currNumRows <= 0)
			return null;
		
		int hmCurrHeight = dp1.hmCurrHeight;
		int hmCurrWidth  = dp1.hmCurrWidth;
		
		double fBlend[] = new double[2]; // blend ratios when a row falls in between two pixels, to avoid aliasing artifacts.
		double [][] dValues = new double[hmCurrWidth][hmCurrHeight];

		for( int kk = 0; kk < currNumRows; kk++)
		{
			int iPointIndex = sortedIndices[kk + currTopRow];
			double fY1 = 1.0 * kk * hmCurrHeight/currNumRows;
			double fY2 = 1.0 * (kk+1) * hmCurrHeight/currNumRows;
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
			
			for (int d = 0; d < iNumCols; d++)
			{
//				int groupIndex = d / iGroupDims;
//				Group group = m_Framework.getGroup(groupIndex, true);
//				double normCuffOff = group.m_CutOffMax;
//				double dVal = m_Framework.getData(iPointIndex, columns[d]) / normCuffOff;
//				dVal = dVal > 1 ? 1 : dVal;
//				if (dVal < 0)
//				{//TODO: investigate why there might ever be a negative value?
//					dVal = 0;
//				}
				
				double dVal = Math.min(Math.max(m_Framework.getData(iPointIndex, columns[d]), 0), 1);
				int iX = d * hmCurrWidth/iNumCols;
				for (int r = 0; r < 2; r++)
				{
					if (iY + r < hmCurrHeight)
					{
						dValues[iX][iY + r] += fBlend[r] * dVal;
					}
				}
			}
		}
		
		double dMaxHMVal = Math.max(1.0*currNumRows/hmCurrHeight, 1.0);
    	
    	PImage hmGx = null;
		if (hmGx == null || hmGx.width != hmCurrWidth || hmGx.height != hmCurrHeight)
		{
			hmGx = m_Applet.createImage(hmCurrWidth, hmCurrHeight, PGraphics.RGB);
		}
    	
    	hmGx.loadPixels();
	    for (int y = 0; y < hmCurrHeight; y++)
	    {
		    for (int x = 0; x < hmCurrWidth; x++)
		    {
				int groupIndex = x / iGroupDims;
				GroupInfo group = m_Framework.getGroup(groupIndex, true);
				int colPalIndex = group.m_Experiment.getColor().getColorIndex();
				boolean logScale = group.m_Experiment.isLogScale();

		    	try
		    	{
		    		double dVal = dValues[x][y] / dMaxHMVal;
		    		if (logScale)
		    			dVal = Math.log(1+100*dVal)/Math.log(101);
		    		
					int clr = dVal < 0.5 ? 
							m_Applet.lerpColor(ColorPalette.COLOR_MIN[colPalIndex], ColorPalette.COLOR_MED[colPalIndex], (float)dVal*2) : 
							m_Applet.lerpColor(ColorPalette.COLOR_MED[colPalIndex], ColorPalette.COLOR_MAX[colPalIndex], (float)dVal*2 - 1);
		    			
		    		hmGx.pixels[y * hmGx.width + x] = clr;
		    	} catch (Exception e)
		    	{
		    		e.printStackTrace();
		    	}
		    }
		}
	    hmGx.updatePixels();
	    return hmGx;
	}

	
	public float m_TopSelected = -1;
	
	// draw the heatmap. caches the heatmap image to improve performance
	public void drawHeatMap(PGraphics gx, ClustInfo[] cInfos, DisplayParams dp, MouseState mouseState)
	{
		if (cInfos == null) {
			return;
		}
		
		boolean isPDF = (gx instanceof PGraphicsPDF);
		
		int[] columns = m_Framework.getVisibleColumns(true, true);
		int iNumCols = columns.length;// m_Framework.getDataDim();
		int iNumGroups = m_Framework.getNumGroups();
		int iGroupDims = m_Framework.getGroupDim();
		int iClustSize = 0;
		for (int ci = 0; ci < cInfos.length; ++ci) {
			iClustSize += cInfos[ci].size();
		}

		dp.hmSortGroup = Math.min(Math.max(dp.hmSortGroup, 0), iNumGroups - 1);
		
		boolean forceUpdate = dp.hmForceUpdate;

		int currTopRow  = Math.max(0, (int) (dp.hmScrollYOffset * iClustSize));
		int currNumRows = Math.min(Math.max((int) (dp.hmScrollYRange * iClustSize), (int)dp.hmRect.height()), iClustSize - currTopRow);
		
		dp.hmTopVisibleRow = currTopRow;
		dp.hmNumVisibleRows = currNumRows;

		if (currNumRows <= 0)
			return;
		
		gx.pushStyle();
		
		//dp.hmCurrWidth = Math.min(iNumCols, (int)dp.hmRect.width());
		dp.hmCurrWidth = iNumCols;//Math.min(iNumCols, (int)dp.hmW * iNumGroups);
		dp.hmCurrHeight  = Math.min(currNumRows, (int)dp.hmRect.height());
		
		//boolean bSelectionUpdated = false;
		
		if (dp.hmGx == null || dp.hmGx.width != dp.hmCurrWidth || dp.hmGx.height != dp.hmCurrHeight)
		{
			dp.hmGx = m_Applet.createImage(dp.hmCurrWidth, dp.hmCurrHeight, PGraphics.RGB);
			forceUpdate = true;
		}
		
		double fBlend[] = new double[2]; // blend ratios when a row falls in between two pixels, to avoid aliasing artifacts.
		
		double dMouseOverCount = 0;
		double[] m_MouseOverProfile = new double[iNumCols];
	
		// create a list of all cluster nodes to be shown
		ArrayList<ClustInfo> allVisibleNodes = new ArrayList<ClustInfo>();
		for (int ci = 0; ci < cInfos.length; ci++)
		{
			ClustInfo[] visibleNodes = cInfos[ci].getVisibleNodes(true);
			for (int ni = 0; ni < visibleNodes.length; ++ni) {
				allVisibleNodes.add(visibleNodes[ni]);
			}
		}
		m_Nodes = allVisibleNodes.toArray(new ClustInfo[allVisibleNodes.size()]);

		// copy the region indices from each node to the sortedIndices
		int[] sortedIndices = new int[iClustSize];
		int destPos = 0;
		for (int ni = 0; ni < allVisibleNodes.size(); ++ni)
		{
			int[] nodeIndices = m_Framework.getSortedIndices(allVisibleNodes.get(ni), dp.hmSortGroup, dp.hmSortCriteria, dp.hmSortAscending);
			System.arraycopy(nodeIndices, 0, sortedIndices, destPos, nodeIndices.length);
			destPos += nodeIndices.length;
		}
		
		/*
		ClustInfo[] nodes = cInfo.getVisibleNodes(true);
		int[] sortedIndices = new int[iClustSize];
		int destPos = 0;
		for (int n = 0; n < nodes.length; ++n)
		{
			int[] nodeIndices = m_Framework.getSortedIndices(nodes[n], dp.hmSortGroup, dp.hmSortCriteria, dp.hmSortAscending);
			System.arraycopy(nodeIndices, 0, sortedIndices, destPos, nodeIndices.length);
			destPos += nodeIndices.length;
		}
		*/
		
		for( int kk = 0; kk < currNumRows; kk++)
		{
			int iPointIndex = sortedIndices[kk + currTopRow];
			double fY1 = 1.0 * kk * dp.hmCurrHeight/currNumRows;
			double fY2 = 1.0 * (kk+1) * dp.hmCurrHeight/currNumRows;
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
			
			// update the profile for rows under mouse cursor
			for (int r = 0; r < 2; r++)
			{
				if (iY + r + dp.hmRect.top() == mouseState.y())
				{
					for (int d = 0; d < iNumCols; d++)
					{
						int groupIndex = d / iGroupDims;
						double normCuffOff = m_Framework.getGroup(groupIndex, true).m_CutOffMax;
						double dVal = m_Framework.getData(iPointIndex, columns[d]) / normCuffOff;
						dVal = dVal > 1 ? 1 : dVal;
	
						m_MouseOverProfile[d] += fBlend[r] * dVal;
						dMouseOverCount += fBlend[r] /  iNumCols;
					}
				}
			}
		}
		
		if (forceUpdate)
		{
			dp.hmGx = renderHeatmap(sortedIndices, dp);
		}
		
//		dp.hmCurrHeight = (int)dp.hmRect.height();
		
		gx.imageMode(PGraphics.CORNER);
		
		if (iNumCols < dp.hmW * iNumGroups)	{
			gx.noSmooth();
		}
		
		//float sortLabelX = -1;//(float)((dp.hmSortGroup + 0.5) * (dp.hmW + dp.hmGapX) + dp.hmRect.left() - dp.clustOffsetX);
		float rightMostX = dp.hmRect.left() + iNumGroups * (dp.hmW + dp.hmGapX);
		Utils.Rect[] columnRects = new Utils.Rect[iNumGroups]; 
//		if (forceUpdate)
		{
			float fScale = 1.f * currNumRows / dp.hmNumVisibleRows;
			float fOffset = (currTopRow - dp.hmTopVisibleRow) * 1.f * dp.hmCurrHeight / dp.hmNumVisibleRows;
			
			int v1 = 0;
			int v2 = dp.hmGx.height;
			float y = dp.hmRect.top() + fOffset;
			float h = dp.hmCurrHeight * fScale;
			
			if (fOffset < 0)
			{
				v1 = (int)(-fOffset * v2 / h);
				h += fOffset;
				y = dp.hmRect.top();
			}
			
			if (y + h > dp.hmRect.top() + dp.hmCurrHeight)
			{
				v2 -= ((y + h) - (dp.hmRect.top() + dp.hmCurrHeight)) * v2 / h;
				h = dp.hmRect.top() + dp.hmCurrHeight - y;
			}
			
			//g.image(dp.hmGx, dp.hmRect.left() - dp.hmOffsetX, y, hmMaxWidth, h, 0, v1, dp.hmGx.width, v2);
			
			gx.strokeWeight(1.f);
			float fX = dp.hmRect.left() - dp.hmOffsetX;
			for (int gi = 0; gi < iNumGroups; gi++, fX += dp.hmW + dp.hmGapX)
			{
				if (!m_Framework.getGroup(gi, true).m_Visible && !dp.drawInvisibleGroups)
				{
					fX -= dp.hmW + dp.hmGapX;
					continue;
				}
				
//				if (dp.hmSortGroup == m_Framework.getGroupOrder(gi, true))
//					sortLabelX = fX + dp.hmW;
				
				if (fX + dp.hmW < dp.hmRect.left()) {
					continue; // entire heatmap on the left of clip rect. go to next
				}
				
				if (fX > dp.hmRect.right()) {
					break; // entire heatmap on the right of clip rect. nothing more to draw
				}
				
				float fX1 = Math.max(fX, dp.hmRect.left());
				float fX2 = fX + dp.hmW;//Math.min(fX + dp.hmW, dp.hmRect.right());
				
				int px1 = (gi * dp.hmGx.width) / iNumGroups; // left pixel coordinate for the heatmap of this group
				int px2 = ((gi + 1) * dp.hmGx.width) / iNumGroups; // right pixel coordinate for the heatmap of this group
				
				
				gx.image(dp.hmGx, fX1, y, fX2 - fX1, h, 
				                  px1, v1, px2, v2);
				
				columnRects[gi] = new Utils.Rect(fX1, y, fX2 - fX1, h);
				
				// draw vertical lines to seperate the groups
				int colorVSeparator = 0xFF444444; // vertical separator between groups
				gx.stroke(colorVSeparator);
				gx.strokeWeight(cInfos[0].getKmeansActiveGroup(gi) ? 2f : 1f);
				
				if (fX >= dp.hmRect.left())// && fX <= dp.hmRect.right())
					gx.line ((int)fX, dp.hmRect.top(), (int)fX, dp.hmRect.top() + dp.hmCurrHeight);

				if (fX+dp.hmW >= dp.hmRect.left())// && fX+dp.hmW <= dp.hmRect.right())
					gx.line ((int)fX+dp.hmW, dp.hmRect.top(), (int)fX+dp.hmW, dp.hmRect.top() + dp.hmCurrHeight);
				
				//if (fX >= dp.hmRect.left() && fX <= dp.hmRect.right())
				//	g.line (fX, dp.hmRect.top(), fX, dp.hmRect.top() + dp.hmCurrHeight);
			}
		}
		gx.smooth();

		
		float numPerPix = 1.f*dp.hmNumVisibleRows/dp.hmCurrHeight;
		boolean drawMouseOver = ((mouseState.isDragging(MouseState.LEFT) || mouseState.isPressed(MouseState.LEFT))  &&
				dp.hmRect.isInside(mouseState.x(), mouseState.y()) && mouseState.y() < dp.hmRect.top() + dp.hmCurrHeight);
		
		boolean drawMouseRegionName = false;
		boolean drawMouseProfile = true;
		
		if (mouseState.isDragging(MouseState.LEFT) &&
			dp.hmRect.isInside(mouseState.startX(), mouseState.startY()))
			//&& dp.hmRect.isInside(mouseState.endX(), mouseState.endY()))
		{
			m_SelectionStart = (int)Math.ceil(dp.hmTopVisibleRow + (mouseState.startY() - dp.hmRect.top()) * numPerPix);
			float mouseEndY = Math.min(Math.max(mouseState.endY(), dp.hmRect.top()), dp.hmRect.bottom());
			m_SelectionEnd = (int)Math.floor(dp.hmTopVisibleRow + (mouseEndY - dp.hmRect.top()) * numPerPix);
			
			m_SelectionStart = Math.min(Math.max(m_SelectionStart, 0), sortedIndices.length - 1);
			m_SelectionEnd = Math.min(Math.max(m_SelectionEnd, 0), sortedIndices.length - 1);
			
			if (m_SelectionStart > m_SelectionEnd)
			{
				int tmp = m_SelectionStart;
				m_SelectionStart = m_SelectionEnd;
				m_SelectionEnd = tmp;
			}
		}
		
		if (mouseState.isReleased(MouseState.LEFT))
		{
			if (mouseState.startX() == mouseState.endX() && 
				mouseState.startY() == mouseState.endY())
			{
				m_SelectionStart = m_SelectionEnd = -1;
				m_SelectedIndices = null;
			}
			else
			{
				if (m_SelectionStart >= 0 && m_SelectionEnd >= 0)
				{
					m_SelectedIndices = new int[m_SelectionEnd - m_SelectionStart + 1];
					for (int i = m_SelectionStart; i < m_SelectionEnd && i < sortedIndices.length; i++)
					{
						m_SelectedIndices[i - m_SelectionStart] = sortedIndices[i]; 
					}
				}
			}
		}
		
		{
			float selectTop = (m_SelectionStart - dp.hmTopVisibleRow) / numPerPix + dp.hmRect.top();
			float selectBottom = (m_SelectionEnd - dp.hmTopVisibleRow) / numPerPix + dp.hmRect.top();
			if (selectTop < dp.hmRect.bottom() && selectBottom > dp.hmRect.top())
			{
				selectTop = Math.max(dp.hmRect.top(), selectTop);
				selectBottom = Math.min(dp.hmRect.bottom(), selectBottom);
				gx.stroke(0, 0, 0, 128);
				gx.strokeWeight(1);
				gx.fill(0, 0, 0, 128);
				gx.rect(dp.hmRect.left(), selectTop, iNumGroups * (dp.hmW + dp.hmGapX) - dp.hmGapX, selectBottom - selectTop);
			}
		}
			
		
		// draw region names under cursor
		if (drawMouseOver && drawMouseRegionName)
		{//TODO
			gx.textSize(10);
			float textH = 12;
			
			float row1 = dp.hmTopVisibleRow + (mouseState.y() - dp.hmRect.top()) * numPerPix;
			int i1 = (int)Math.floor(row1);
			int i2 = (int)Math.min(Math.ceil(row1 + numPerPix), sortedIndices.length);
			
			
			//int i2 = (int)(mouseState.y - dp.hmRect.top() + 0.5) * dp.numRows/dp.currHeight;
			//i1 = Math.max(i1, 0);
			//i2 = Math.min(i2, i1 + 10);

			float textX = dp.hmRect.left() + 20;
			float textY = mouseState.y() - (i2 - i1) * textH / 2;
			textY = Math.min(textY, dp.hmRect.bottom() - textH * (i2 - i1));
			textY = Math.max(textY, dp.hmRect.top());

			gx.noStroke();
			gx.fill(255, 200);
			gx.rect(dp.hmRect.left(), textY, dp.hmRect.right(), textH * (i2 - i1));
			
			//gx.stroke(128, 128, 0);
			//gx.line(textX, textY, dp.hmRect.left(), mouseState.y());
			//gx.line(textX, textY + textH * (i2 - i1) , dp.hmRect.left(), mouseState.y());
			//gx.line(dp.hmRect.left(), mouseState.y() - 1, rightMostX, mouseState.y() - 1);
			//gx.line(dp.hmRect.left(), mouseState.y() + 1, rightMostX, mouseState.y() + 1);

			gx.textAlign(PGraphics.LEFT, PGraphics.TOP);
			gx.fill(0);
			for (int i = i1; i < i2; i++)
			{
				int iPointIndex = sortedIndices[i];
				gx.text(m_Framework.getRegionName(iPointIndex), textX, textY);
				textY += textH;
			}
		}
		
		
		{
			int colorHSeparator = 0xFF000000; // color of horizontal separator between clusters
			int nodeSizeSum = 0;
			float hlineY0 = 0;
			m_NodesYPos = new double[allVisibleNodes.size() + 1];
			for (int ni = 0; ni < allVisibleNodes.size() + 1; ni++)
			{
				float hlineY1 = dp.hmRect.top() + dp.hmCurrHeight * (nodeSizeSum / (dp.hmScrollYRange * iClustSize) - dp.hmScrollYOffset/dp.hmScrollYRange);
				m_NodesYPos[ni] = Math.min(Math.max(hlineY0, dp.hmRect.top()), dp.hmRect.bottom());
				
				// draw the horizontal lines separating clusters
				if (hlineY1 >= dp.hmRect.top() && hlineY1 <= dp.hmRect.bottom()) {
					float fX = dp.hmRect.left() - dp.hmOffsetX;
					gx.stroke(colorHSeparator);
					for (int gi = 0; gi < iNumGroups; gi++, fX += dp.hmW + dp.hmGapX)
					{
						gx.strokeWeight(cInfos[0].getKmeansActiveGroup(gi) ? 3f : 1f);
						//gx.line(dp.hmRect.left(), hlineY1, rightMostX, hlineY1);
						gx.line((int)fX, hlineY1, (int)(fX + dp.hmW), hlineY1);
					}
				}
				
				
				// draw the connecting curves between workspace and heat map view
				if (cInfos.length == 1 && !isPDF && ni > 0)
				{
					gx.fill((dp.hmColor1 & 0x00FFFFFF) | 0x08000000);
					//gx.noFill();
					gx.stroke((dp.hmColor1 & 0x00FFFFFF) | 0x33000000);
					gx.strokeWeight(1.5f);
					gx.beginShape();
					float cx1 = columnRects[0].left() - 45;
					float cy1 = m_TopSelected + (dp.plotH + dp.plotGapY)*(ni-1);
					float cx2 = columnRects[0].left();
					float cy2 = Math.min(Math.max(hlineY0, dp.hmRect.top()), dp.hmRect.bottom());
					float ch1 = dp.plotH;
					float ch2 = Math.max(Math.min(hlineY1, dp.hmRect.bottom())-cy2, 0);
					float cw = 20;
					//if (cy1 < dp.clustRect.top()) {
					//	ch1 = Math.max(1, cy1 + ch1 - dp.clustRect.top());
					//	cy1 = dp.clustRect.top();
					//}

					
					gx.vertex(dp.summaryLeft, cy1);
					gx.vertex(cx1, cy1);
					gx.bezierVertex(cx1 + cw, cy1, cx2 - 2*cw, cy2, cx2, cy2);
					gx.vertex(cx2, cy2+ch2);
					gx.bezierVertex(cx2 - 2*cw, cy2 + ch2, cx1 + cw, cy1 + ch1, cx1, cy1 + ch1);
					gx.vertex(dp.summaryLeft, cy1+ch1);
					gx.endShape();
				}
					
				hlineY0 = hlineY1;				
				if (ni < allVisibleNodes.size()) {
					nodeSizeSum += allVisibleNodes.get(ni).size();
				}
			}
		}

		if (drawMouseOver && drawMouseProfile)
		{
			if (dp.hmLogScale)
			{
				for (int i = 0; i < m_MouseOverProfile.length; i++) {
					m_MouseOverProfile[i] = Math.log(m_MouseOverProfile[i] + 1);
				}
				dMouseOverCount = Math.log(dMouseOverCount + 1);
			}
			
			Utils.Rect profileRect = new Utils.Rect(dp.hmRect.left()-1, dp.hmRect.bottom() + 10, rightMostX - dp.hmRect.left() - dp.hmGapX, 40);
			gx.stroke(0);
			gx.strokeWeight(2.0f);
			
			float fX = dp.hmRect.left() - dp.hmOffsetX;
			for (int gi = 0; gi < iNumGroups; gi++, fX += dp.hmW + dp.hmGapX)
			{
			    double[] arrayZero = new double[1];
			    
	            gx.fill(255);
                gx.rect(fX, profileRect.top(), dp.hmW, profileRect.height());
                gx.fill(100);
                DrawUtils.drawProfile(gx
                        , Utils.concatArray(arrayZero,Utils.concatArray(Arrays.copyOfRange(m_MouseOverProfile, gi * iGroupDims, (gi + 1) * iGroupDims), arrayZero))
                        , null, 0, dMouseOverCount
                        , fX, profileRect.top(), dp.hmW, profileRect.height()
                        , false);
			}
			
			// two horizontal lines at the mouse position
            gx.strokeWeight(1.0f);
			gx.line(profileRect.left(), mouseState.y() - 1, profileRect.right(), mouseState.y() - 1);
			gx.line(profileRect.left(), mouseState.y() + 1, profileRect.right(), mouseState.y() + 1);
		}
		
		if (dp.hmShowLegend & !(drawMouseOver && drawMouseProfile))
		{
			// draw legend: number of base pairs per mark and number of regions.
			int colorLegend = gx.color(64, 64, 64);

			float bpH = dp.hmLegendFontSize;
			float bpY = dp.hmRect.top() + dp.hmCurrHeight + bpH;
			float bpX1 = dp.hmRect.left();
			float bpX2 = bpX1 + dp.hmW;
			gx.strokeWeight(1.0f);
			gx.stroke(colorLegend);
			gx.line(bpX1, bpY, bpX2, bpY);
			gx.line(bpX1, bpY - bpH/2, bpX1, bpY + bpH/2);
			gx.line(bpX2, bpY - bpH/2, bpX2, bpY + bpH/2);
			
			bpY += bpH/2;
			
			gx.textAlign(PGraphics.LEFT, PGraphics.TOP);
			gx.textSize(dp.hmLegendFontSize);
			gx.fill(colorLegend);
			
			String strBPSize = "";
			if (m_Framework.isEqualRegionSize()) {
	            int maxRegionSize = m_Framework.getMaxRegionSize();
			    strBPSize = Integer.toString(maxRegionSize) + " bp / " + iGroupDims + " bins";
			} else {
			    strBPSize = "(variable size) " + iGroupDims + " bin" + (iGroupDims > 1 ? "s" : "");
			}
			
			gx.text(strBPSize, bpX1, bpY);
			NumberFormat nf = NumberFormat.getInstance();
			nf.setMinimumFractionDigits(0);
			nf.setMaximumFractionDigits(1);
			
			bpX1 += gx.textWidth(strBPSize+"WW");
			gx.text(Integer.toString(iClustSize) + " total regions (" + nf.format(numPerPix) + " per pixel row)", bpX1, bpY);
			gx.text("sort: " + (dp.hmSortAscending ? "ascending" : "descending") + " by " + dp.hmSortCriteria.toString(), bpX1, bpY + gx.textAscent()*1.5f);
			
			// draw the label showing the sorted group
			for (int gi = 0; gi < columnRects.length; gi++)
			{
				if (columnRects[gi] == null)
					continue;
				Utils.Rect toolRect = new Utils.Rect(columnRects[gi].left(), columnRects[gi].top() - 20, columnRects[gi].width(), 20);
				// draw the bars above each heatmap column
//				if (!isPDF) {
//					gx.stroke(toolRect.isInside(mouseState.x(), mouseState.y()) ? 0xFFFF0000 : 200);
//					gx.fill(240);
//					gx.rect(toolRect.left(), toolRect.top(), toolRect.width(), toolRect.height());
//				}
				
				if (dp.hmSortGroup == m_Framework.getGroupOrder(gi, true))
				{
					// draw the arrow showing the sorted group
					gx.pushStyle();
					gx.fill(toolRect.isInside(mouseState.x(), mouseState.y()) ? 0xFFFF0000 : 0);
					gx.textAlign(PConstants.CENTER, PConstants.BOTTOM);
					gx.textFont(dp.fontGuiFx);
					//gx.textSize(18);
					gx.textSize(dp.hmLegendFontSize*1.5f);
					gx.text(dp.hmSortAscending ? " w" : " s", toolRect.hcenter(), toolRect.bottom());
					gx.popStyle();
				}
			}
			
			// color gradient
			int colPalIndex = m_Framework.getGroup(dp.hmSortGroup, true).m_Experiment.getColor().getColorIndex();
			Utils.Rect gradRect = new Utils.Rect((int)dp.hmRect.left(), (int)bpY + gx.textAscent()*2f, gx.textWidth(strBPSize), dp.hmLegendFontSize);
			if (isPDF)
			{
				PGraphics gradGx = m_Applet.createGraphics(gradRect.iwidth()/2+1, gradRect.iheight(), PApplet.P2D);
				gradGx.beginDraw();
				DrawUtils.gradientRect(gradGx, 0, 0, gradRect.width()/2+1, gradRect.height(), 
						ColorPalette.COLOR_MIN[colPalIndex], ColorPalette.COLOR_MED[colPalIndex], DrawUtils.X_AXIS);
				gx.image(gradGx, gradRect.left(), gradRect.top());
				DrawUtils.gradientRect(gradGx, 0, 0, gradRect.width()/2+1, gradRect.height(), 
						ColorPalette.COLOR_MED[colPalIndex], ColorPalette.COLOR_MAX[colPalIndex], DrawUtils.X_AXIS);
				gx.image(gradGx, gradRect.hcenter(), gradRect.top());
				gradGx.endDraw();
			}
			else
			{
				DrawUtils.gradientRect(gx, gradRect.ileft(), gradRect.itop(), gradRect.width()/2, gradRect.height(), 
						ColorPalette.COLOR_MIN[colPalIndex], ColorPalette.COLOR_MED[colPalIndex], DrawUtils.X_AXIS);
				DrawUtils.gradientRect(gx, gradRect.ihcenter(), gradRect.itop(), gradRect.width()/2, gradRect.height(), 
						ColorPalette.COLOR_MED[colPalIndex], ColorPalette.COLOR_MAX[colPalIndex], DrawUtils.X_AXIS);
			}
			
			gx.noFill();
			gx.stroke(colorLegend);
			gx.rect(gradRect.left(), gradRect.top(), gradRect.width(), gradRect.height());
			gx.textAlign(PGraphics.CENTER, PGraphics.TOP);
			gx.textSize(dp.hmLegendFontSize);
			gx.fill(colorLegend);
			gx.text("0", gradRect.left(), gradRect.bottom());
			gx.text("1", gradRect.right(), gradRect.bottom());
		}
		
		dp.hmForceUpdate = false;
		gx.popStyle();
	}
	
	public void resetSelection()
	{
		m_SelectedIndices = null;
		m_SelectionStart = m_SelectionEnd = -1;
	}
	
	public int[] getSelectedIndices()
	{
		return m_SelectedIndices;
	}
	
	
}
