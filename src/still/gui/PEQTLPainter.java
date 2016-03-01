package still.gui;

import java.awt.event.ActionEvent;
import java.awt.event.MouseWheelEvent;
import java.text.NumberFormat;

import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;

import processing.core.PConstants;
import processing.core.PGraphics;
import still.data.EQTLTableFactory;
import still.data.FloatIndexer;
import still.data.Operator;
import still.data.EQTLTableFactory.EQTLData;
import still.operators.EQTLOp;

public class PEQTLPainter extends PBasicPainter
{
	private static final long serialVersionUID = -4468727316635587950L;

	class Rect
	{
		public Rect() {left = top = width = height = 0;}
		public Rect(float l, float t, float w, float h) {left = l; top = t; width = w; height = h;}
		public Rect clone()
		{
			return new Rect(left, top, width, height);
		}
		public float left() {return left;}
		public float right() {return left + width;}
		public float top() {return top;}
		public float bottom() {return top + height;}
		public float width() {return width;}
		public float height() {return height;}
		public void setLeft(float l) {left = l;}
		public void setRight(float r) {width = r - left;}
		public void setTop(float t) {top = t;}
		public void setBottom(float b) {height = b - top;}
		public void setWidth(float w) {width = w;}
		public void setHeight(float h) {height = h;}
		public boolean isInside(double x, double y) {return x >= left && x <= right() && y >= top && y <= bottom();}
		
		float left, top, width, height;
	}

	class Bound2D
	{
		public Bound2D() {xmin = ymin = Double.MAX_VALUE; xmax = ymax = Double.NEGATIVE_INFINITY;}
		public Bound2D(double _xmin, double _xmax, double _ymin, double _ymax) {xmin = _xmin; ymin = _ymin; xmax = _xmax; ymax = _ymax;}
		public Bound2D clone()
		{
			return new Bound2D(xmin, xmax, ymin, ymax);
		}
		public double dx() {return xmax - xmin;}
		public double dy() {return ymax - ymin;}
		public boolean isInside(double x, double y) {return x >= xmin && x <= xmax && y >= ymin && y <= ymax;}
		public double xmin, xmax;
		public double ymin, ymax;
	}
	

	PScatterPlot	m_ScatterPlot = new PScatterPlot();
	EQTLOp 		m_EQTLOp = null;
	
	
	public PEQTLPainter(Operator op)
	{
		super(op);		
		
		BIG_BORDER_H_L = 50;
		BIG_BORDER_H_R = 50;
		BIG_BORDER_V_T = 50;
		BIG_BORDER_V_B = 50;

		if (op instanceof EQTLOp)
 		{
 			m_EQTLOp = (EQTLOp) op;
 		}
	}
	
	@Override
	public void actionPerformed(ActionEvent e)
	{
		super.actionPerformed(e);
	}
	
	@Override
	public void setup() 
	{
		super.setup();
		updatePlotInfo();
	}
	
	@Override
	public void heavyResize()
	{
		super.heavyResize();
		updatePlotInfo();
	}
	
	
	class PlotInfo
	{
		int 		m_iXCol = -1;
		int 		m_iYCol = -1;
		String 		m_sName = "";
		Bound2D		m_PlotBound; // min-max coordinates of the plot corners 
		Bound2D		m_DataBound; // min-max value of the plot data
		Rect		m_View;		 // screen coordinates of the plot
		PGraphics 	m_PG  = null; // used to cache the scatter plot (performance)
		boolean		m_bRedraw = true;
		
		float getViewX(double x) {return m_View.left() + (float)((x - m_PlotBound.xmin) * m_View.width() / m_PlotBound.dx());} 
		float getViewY(double y) {return m_View.top() + (float)((m_PlotBound.ymax - y) * m_View.height() / m_PlotBound.dy());} 
		double getValueX(double viewX) {return m_PlotBound.xmin + (viewX - m_View.left()) * m_PlotBound.dx() / m_View.width();} 
		double getValueY(double viewY) {return m_PlotBound.ymax - (viewY - m_View.top()) * m_PlotBound.dy() / m_View.height();} 
	}
	
	PlotInfo 	m_PlotInfo[] = null;
	PlotInfo	m_MagnifiedPlotInfo = null;
	boolean		m_bMagnified = false;
	int 		m_iSelectedPlot = -1;	
	//boolean 	m_bRedrawPlots = true;
	Rect 		m_SelectionRect = null;
	Bound2D 	m_SelectionBound = null;
	int	 		m_iRecomputePheno = -1; // index of the phenotype for which stats need to be recomputed (e.g. when user tweaks the histogram)
	
	Bound2D calcDataBound(int iPlot)
	{
		PlotInfo p = m_PlotInfo[iPlot];
		if (p == null || p.m_iXCol == -1 || p.m_iYCol == -1)
		{
			return null;
		}

		Bound2D dataBound = new Bound2D();
		for (int d = 0; d < m_EQTLOp.m_CurrTable.rows(); d++)
		{
			double dx = m_EQTLOp.m_CurrTable.getMeasurement(d, p.m_iXCol);
			double dy = m_EQTLOp.m_CurrTable.getMeasurement(d, p.m_iYCol);
			dataBound.xmin = Math.min(dx, dataBound.xmin);
			dataBound.xmax = Math.max(dx, dataBound.xmax);
			dataBound.ymin = Math.min(dy, dataBound.ymin);
			dataBound.ymax = Math.max(dy, dataBound.ymax);
		}
		return dataBound;
	}
	
	public void updatePlotInfo()
	{
		try
		{
			if (m_EQTLOp == null || m_EQTLOp.m_EQTLData == null)
				return;
			
	 		int iNumPlots = m_EQTLOp.m_EQTLData.getNumPhenoTypes() + 1;
	 		
	 		if (m_PlotInfo == null || m_PlotInfo.length != iNumPlots)
	 		{
	 			m_PlotInfo = new PlotInfo[iNumPlots];
	 		}
	 		
	 		int iNumPerRow = 5;
	 		float xgap = 50;
	 		float ygap = 50;
	 		float plotWidth = (int)((m_ViewCoords[2] - m_ViewCoords[0]) / iNumPerRow - xgap); 
	 		float plotHeight = plotWidth;
	 		
	 		Bound2D boundAll = new Bound2D();
	
	 		for (int i = 0; i < iNumPlots; i++)
	 		{
	 	 		if (m_PlotInfo[i] == null)
	 			{
	 				m_PlotInfo[i] = new PlotInfo();
	 			}
				PlotInfo p = m_PlotInfo[i];
	 			
	 			p.m_iXCol = EQTLTableFactory.getColIndex(m_EQTLOp.m_EQTLData, m_EQTLOp.m_OutputType, m_EQTLOp.m_XCol, i); 
	 			p.m_iYCol = EQTLTableFactory.getColIndex(m_EQTLOp.m_EQTLData, m_EQTLOp.m_OutputType, m_EQTLOp.m_YCol, i);
	 			if (p.m_iXCol == -1 || p.m_iYCol == -1)
				{
					continue;
				}

	 			p.m_sName = i < m_EQTLOp.m_EQTLData.getNumPhenoTypes() ? m_EQTLOp.m_EQTLData.getPhenoTypeName(i) : "Affection";
	 			p.m_DataBound = calcDataBound(i); 
	 			
	 			//p.m_Bounds = new Rect((float)dMinX, (float)dMinY, (float)(dMaxX - dMinX), (float)(dMaxY - dMinY));
	 			p.m_View = new Rect(m_ViewCoords[0] + (plotWidth + xgap) * (i % iNumPerRow),
							m_ViewCoords[1] + (i / iNumPerRow) * (ygap + plotHeight),
							plotWidth, plotHeight);
	 			
	 			if (p.m_PG == null || p.m_PG.width != (int)plotWidth ||	p.m_PG.height != (int) plotHeight)
	 			{// create a new PGraphics if the old one can't be used
	 				p.m_PG = createGraphics((int)plotWidth, (int)plotHeight, P2D);
	 			}
	 			
	 			boundAll.xmin = Math.min(p.m_DataBound.xmin, boundAll.xmin);
	 			boundAll.xmax = Math.max(p.m_DataBound.xmax, boundAll.xmax);
	 			boundAll.ymin = Math.min(p.m_DataBound.ymin, boundAll.ymin);
	 			boundAll.ymax = Math.max(p.m_DataBound.ymax, boundAll.ymax);
	 		}
	 		
	 		for (int i = 0; i < iNumPlots; i++)
	 		{
				PlotInfo p = m_PlotInfo[i];
				// boundAll = p.m_DataBound; //this makes plot bounds non-uniform
				p.m_PlotBound = new Bound2D(boundAll.xmin - boundAll.dx() * 0.1,
											boundAll.xmax + boundAll.dx() * 0.1,
											boundAll.ymin - boundAll.dy() * 0.1,
											boundAll.ymax + boundAll.dy() * 0.1);
	 		}
	 		
	 		redrawAll();
	 		invokeRedraw();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
 	}

	Bound2D m_UserPlotBound = null; // plot bound modified by the user
	
	boolean updateSelection()
	{
		if (m_iSelectedPlot == -1)
			return false;
		
		boolean	bSelectionUpdated = false;

		try
		{
			PlotInfo p = m_PlotInfo[m_iSelectedPlot];
			boolean bMouseInside = p.m_View.isInside(m_DrawMouseState.startX(), m_DrawMouseState.startY());
			
			m_SelectionRect = null;
			m_SelectionBound = null;
			

			if (m_DrawMouseState.isLeftButton() && bMouseInside && (m_DrawMouseState.isDragging() || m_DrawMouseState.isReleased()))
			{// SELECT
				float sx = Math.max(Math.min(m_DrawMouseState.startX(), m_DrawMouseState.endX()), p.m_View.left());
				float sy = Math.max(Math.min(m_DrawMouseState.startY(), m_DrawMouseState.endY()), p.m_View.top());
				float ex = Math.min(Math.max(m_DrawMouseState.startX(), m_DrawMouseState.endX()), p.m_View.right());
				float ey = Math.min(Math.max(m_DrawMouseState.startY(), m_DrawMouseState.endY()), p.m_View.bottom());
				if (ex - sx > 2 || ey - sy > 2)
				{
					m_SelectionRect = new Rect(sx, sy, ex - sx, ey - sy);
					m_SelectionBound = new Bound2D(p.getValueX(sx), p.getValueX(ex), p.getValueY(ey), p.getValueY(sy));
					boolean bUpdateSelection =  (m_DrawMouseState.isReleased() ||
        					(m_DrawMouseState.isDragging() && m_bMagnified));
					if (bUpdateSelection)
					{
						int iSelectionCol = m_ScatterPlot.m_iSelectionCol; // just being lazy
						int iColorCol = m_ScatterPlot.m_iColorCol; // just being lazy
						if (iSelectionCol != -1)
						{
							for (int i = 0; i < m_EQTLOp.m_CurrTable.rows(); i++)
							{
								if (iColorCol != -1)
								{
									if (((int)m_EQTLOp.m_CurrTable.getMeasurement(i,  iColorCol) & 0xFF000000) == 0)
										continue; // don't change the selection of hidden points
								}
								double x = m_EQTLOp.m_CurrTable.getMeasurement(i, p.m_iXCol);
								double y = m_EQTLOp.m_CurrTable.getMeasurement(i, p.m_iYCol);
								double oldSelected = m_EQTLOp.m_CurrTable.getMeasurement(i,  iSelectionCol);
								double newSelected = isShiftPressed() ? oldSelected : isAltPressed() ? oldSelected : 0; // shift = include the previously selected points
								if (m_SelectionBound.isInside(x, y))
								{
									newSelected = isAltPressed() ? 0 : 1; // alt = exclude these points from selection
								}
								
								if (newSelected != oldSelected)
								{
									m_EQTLOp.m_CurrTable.setMeasurement(i,  iSelectionCol, newSelected);
									bSelectionUpdated = true; 
								}
							}
						}
					}
				}
			}
			else if (m_DrawMouseState.isRightButton() && bMouseInside)
			{// PAN
				double dx = p.getValueX(m_DrawMouseState.startX()) - p.getValueX(m_DrawMouseState.endX());
				double dy = p.getValueY(m_DrawMouseState.startY())- p.getValueY(m_DrawMouseState.endY());
				Bound2D newBound = p.m_PlotBound.clone();
				if (isShiftPressed())
				{
					double rx = (p.getValueX(m_DrawMouseState.startX()) - newBound.xmin) / newBound.dx();
					double ry = (p.getValueY(m_DrawMouseState.startY()) - newBound.ymin) / newBound.dy();
					newBound.xmin -= rx * dx; 
					newBound.xmax += (1 - rx) * dx;
					newBound.ymin -= ry * dy;
					newBound.ymax += (1 - ry) * dy;
				}
				else
				{
					newBound.xmin += dx;
					newBound.xmax += dx;
					newBound.ymin += dy;
					newBound.ymax += dy;
				}
				m_UserPlotBound = null;
				if (m_DrawMouseState.isDragging())
				{
					m_UserPlotBound = newBound;
				}
				else if (m_DrawMouseState.isReleased())
				{
					p.m_PlotBound.xmin = newBound.xmin;
					p.m_PlotBound.xmax = newBound.xmax;
					p.m_PlotBound.ymin = newBound.ymin;
					p.m_PlotBound.ymax = newBound.ymax;
				}
				p.m_bRedraw = true;
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		return bSelectionUpdated;
	}
	
	/**
	 * Handles the interaction of the user with the histogram
	 * @param iPlot  index of the plot/phenotype (e.g. 0 .. 14)
	 * @param rHist  bounding box of the histogram view rect
	 * @return true if the user has modified the histogram thresholds
	 */
	boolean selectGroupHistogramThresholds(int iPlot, Rect rHist)
	{
		boolean bUpdated = false;
		
		boolean bMouseInside = rHist.isInside(m_DrawMouseState.startX(), m_DrawMouseState.startY());
		if (bMouseInside && (m_DrawMouseState.isDragging() || m_DrawMouseState.isReleased()))
		{
			EQTLTableFactory.GroupInfo gi = m_EQTLOp.m_EQTLData.m_GroupInfo[iPlot];
			double xNew = Math.max(Math.min(m_DrawMouseState.endX(), rHist.right()), rHist.left());
			double xThreshold[] = new double[2];
			xThreshold[0] = rHist.left() + rHist.width() * gi.m_dThresholdLow;
			xThreshold[1] = rHist.left() + rHist.width() * gi.m_dThresholdHigh;
			int iIndex = Math.abs(xThreshold[0] - xNew) < Math.abs(xThreshold[1] - xNew) ? 0 : 1; // pick closest
			iIndex = Math.abs(xThreshold[iIndex] - m_DrawMouseState.endX()) < rHist.width / 5 ? iIndex : -1; // pick only if close enough
			
			if (iIndex != -1)
			{// update threshold
				if (xThreshold[iIndex] != xNew)
				{
					double dNewThreshold = (xNew - rHist.left()) / rHist.width();
					if (iIndex == 0)
						gi.m_dThresholdLow = dNewThreshold;
					else
						gi.m_dThresholdHigh = dNewThreshold;
					
					//if (m_DrawMouseState.m_State == MouseState.STATE_RELEASED)
					{
						bUpdated = true;
					}
				}
			}
		}
		return bUpdated;
	}
	
	/**
	 * Draws the expression level histogram for a phenotype, marked with the high/low group thresholds.
	 * @param iPlot  index of the plot/phenotype (e.g. 0 .. 14)
	 * @param rHist  bounding box of the histogram view rect
	 */
	void drawGroupHistogram(int iPlot, Rect rHist)
	{
		//if (true)
		//	return;
		if (selectGroupHistogramThresholds(iPlot, rHist))
		{
			m_iRecomputePheno = iPlot;
		}

		stroke(0);
		noFill();
		rect(rHist.left, rHist.top, rHist.width, rHist.height);
		fill(64);
		noStroke();
		EQTLTableFactory.GroupInfo gi = m_EQTLOp.m_EQTLData.m_GroupInfo[iPlot];
		PUtils.drawHistogram(g, gi.m_Hist, gi.m_Hist.length, rHist.left, rHist.top, rHist.width, rHist.height, -1, "", "", null);
		
		fill(0, 128, 128, 64);
		float thresholdLowWidth = (float)(rHist.width * gi.m_dThresholdLow);
		float thresholdHighWidth = (float)(rHist.width * (1 - gi.m_dThresholdHigh));
		rect(rHist.left, rHist.top, thresholdLowWidth, rHist.height);
		fill(128, 0, 0, 64);
		rect(rHist.right() - thresholdHighWidth, rHist.top, thresholdHighWidth , rHist.height);
		
		if (m_bMagnified)
		{// draw the histogram labels
			NumberFormat nf = NumberFormat.getInstance();
			nf.setMinimumFractionDigits(0);
			nf.setMaximumFractionDigits(2);
			fill (0);
			textAlign(CENTER, TOP);
			text(nf.format(gi.m_dMinEx), rHist.left(), rHist.bottom() + 10);
			text(nf.format(gi.m_dMaxEx), rHist.right(), rHist.bottom() + 10);
			double dLowEx = gi.m_dMinEx + gi.m_dThresholdLow * (gi.m_dMaxEx - gi.m_dMinEx);
			double dHighEx = gi.m_dMinEx + gi.m_dThresholdHigh * (gi.m_dMaxEx - gi.m_dMinEx);
			fill(0, 128, 128);
			text(nf.format(dLowEx), rHist.left() + rHist.width * (float)gi.m_dThresholdLow, rHist.bottom() + 10);
			//text(nf.format(gi.m_dThresholdLow*100)+"%", rHist.left() + rHist.width * (float)gi.m_dThresholdLow, rHist.bottom() + 2);
			fill(128, 0, 0);
			text(nf.format(dHighEx), rHist.left() + rHist.width * (float)gi.m_dThresholdHigh, rHist.bottom() + 10);
			
			fill(0);
			
			textAlign(LEFT, TOP);
			text(nf.format(100.0 * gi.m_Size[1] / m_EQTLOp.m_EQTLData.getNumIndividuals())+"%", rHist.left(), rHist.top);

			textAlign(RIGHT, TOP);
			text(nf.format(100.0 * gi.m_Size[2] / m_EQTLOp.m_EQTLData.getNumIndividuals())+"%", rHist.right(), rHist.top);
		}
	}
	
	/**
	 * Draws a single 2d scatter plot plot. Also handles the user interaction.
	 * @param iPlot		index of the plot/phenotype
	 */
	void drawPlot(int iPlot)
	{
		NumberFormat nf = NumberFormat.getInstance();
		nf.setMinimumFractionDigits(0);
		nf.setMaximumFractionDigits(2);

		PlotInfo p = m_PlotInfo[iPlot];
		if (p == null || p.m_iXCol == -1 || p.m_iYCol == -1)
		{
			return;
		}
		
		float mx = m_DrawMouseState.startX();
		float my = m_DrawMouseState.startY();
		boolean bMouseInside = p.m_View.isInside(mx, my);
		
		fill(0);
		textAlign(CENTER, BOTTOM);
		text(p.m_sName, p.m_View.left() + p.m_View.width() / 2 , p.m_View.top() - 5);
			
 		fill(m_ScatterPlot.m_Style.m_BackgroundColor);
 		noStroke();
	 	rect(p.m_View.left(), p.m_View.top(), p.m_View.width(), p.m_View.height());

		stroke(0);
		if (iPlot == m_iSelectedPlot)
			stroke(255, 0, 0);
		strokeWeight(1.0f);
	 	noFill();
		rect(p.m_View.left()-1, p.m_View.top()-1, p.m_View.width()+2, p.m_View.height()+2);

		if (!m_bMagnified)
		{
			if (p.m_bRedraw)
			{
				p.m_PG.beginDraw();
				p.m_PG.noStroke();
				p.m_PG.fill(0xFFFFFFFF);
				p.m_PG.rect(0, 0, p.m_View.width(), p.m_View.height());
				m_ScatterPlot.draw(p.m_PG, p.m_iXCol, p.m_iYCol, p.m_PlotBound, new Rect(0, 0, p.m_View.width(), p.m_View.height()));
				p.m_PG.endDraw();
				p.m_bRedraw = false;
			}
			image(p.m_PG, p.m_View.left(), p.m_View.top());
		}
		else
		{
			
			m_ScatterPlot.draw(g, p.m_iXCol, p.m_iYCol, m_UserPlotBound == null ? p.m_PlotBound : m_UserPlotBound, p.m_View);
		}
		
		
		if (m_SelectionRect != null)
		{
			stroke(0);
			fill(128, 64);
			rect(m_SelectionRect.left, m_SelectionRect.top, m_SelectionRect.width, m_SelectionRect.height);
		}
		
		
		float tickxmin = p.getViewX(p.m_DataBound.xmin);
		float tickxmax = p.getViewX(p.m_DataBound.xmax);
		float tickx0 = p.getViewX(0);
		float tickymin = p.getViewY(p.m_DataBound.ymin);
		float tickymax = p.getViewY(p.m_DataBound.ymax);
		float ticky0 = p.getViewY(0);
		
		float ticksize = 5;

		// ticks on the x-axis
		fill(0);
		stroke(0);
		line(tickxmin, p.m_View.bottom()+1, tickxmin, p.m_View.bottom() + ticksize);
		line(tickxmax, p.m_View.bottom()+1, tickxmax, p.m_View.bottom() + ticksize);
		textAlign(CENTER, TOP);
		text(nf.format(p.m_DataBound.xmin), tickxmin, p.m_View.bottom() + ticksize+4);
		text(nf.format(p.m_DataBound.xmax), tickxmax, p.m_View.bottom() + ticksize+4);
		if (p.m_PlotBound.xmin < 0 && p.m_PlotBound.xmax > 0)
		{// draw the ticks for x == 0
			fill(160);
			stroke(160, 128);
 			line(tickx0, p.m_View.bottom()+1, tickx0, p.m_View.bottom() + ticksize);
// 			text("o", tickx0, p.m_View.bottom() + ticksize);
			line(tickx0, p.m_View.top(), tickx0, p.m_View.bottom());
		}
			
		// ticks on the y-axis
		fill(0);
		stroke(0);
		line(p.m_View.left(), tickymin, p.m_View.left() - ticksize, tickymin);
		line(p.m_View.left(), tickymax, p.m_View.left() - ticksize, tickymax);
		textAlign(CENTER, BOTTOM);
		PUtils.drawRotatedText(g, nf.format(p.m_DataBound.ymin), p.m_View.left() - ticksize - 4, tickymin, -PConstants.PI/2.f);
		PUtils.drawRotatedText(g, nf.format(p.m_DataBound.ymax), p.m_View.left() - ticksize - 4, tickymax, -PConstants.PI/2.f);
		if (p.m_PlotBound.ymin < 0 && p.m_PlotBound.ymax > 0)
		{// draw the ticks for y == 0
			fill(160);
			stroke(160, 128);
 			line(p.m_View.left(), ticky0, p.m_View.left() - ticksize, ticky0);
//			PUtils.drawRotatedText(g, "o", p.m_View.left() - ticksize, ticky0, -PConstants.PI/2.f);
			line(p.m_View.left(), ticky0, p.m_View.right(), ticky0);
		}
			
		if (bMouseInside)
		{// draw the coordinates of the mouse location
			stroke(255, 0, 0, 64);
			fill(255, 0, 0);
 			line(mx, p.m_View.top(), mx, p.m_View.bottom() + ticksize);
			line(p.m_View.right(), my, p.m_View.left() - ticksize, my);
 			textAlign(CENTER, TOP);
 			text(nf.format(p.getValueX(mx)), mx, p.m_View.bottom() + ticksize+14);
 			textAlign(CENTER, BOTTOM);
			PUtils.drawRotatedText(g, nf.format(p.getValueY(my)), p.m_View.left() - ticksize - 14, my, -PConstants.PI/2.f);
		}
		
		if (iPlot < m_EQTLOp.m_EQTLData.getNumPhenoTypes())
		{// draw the expression histograms
			Rect rHist = new Rect(p.m_View.left(), p.m_View.top() - 40, 50, 30);
			if (m_bMagnified)
			{
				rHist.width  *= 4;
				rHist.top -= 50;
				rHist.height *= 2;
				drawGroupHistogram(iPlot, rHist);
			}
			
		}
		
	}
	
	Rect m_MagnifiedViewCoords;
	Bound2D	m_MagnifiedPlotBound;
	
	public void redrawAll()
	{
		for (int i = 0; i < m_PlotInfo.length; i++)
		{
			m_PlotInfo[i].m_bRedraw = true;
		}
	}

	@Override
	protected void drawPlot()
	{
		//TODO: Write your custom drawing functions here
		background(255);
 		
		if (m_PlotInfo == null)
			return;
		boolean bShowLDAHist = m_EQTLOp.m_OutputType.equals(EQTLTableFactory.OutputType.RESULT_SINGLE);
		if (bShowLDAHist)
		{
			drawLDAHistogram();
			return;
		}
		
		
		
		m_ScatterPlot.setTable(m_EQTLOp.m_CurrTable);
		
		if (!m_bMagnified)
		{
			textSize(15);
			if (updateSelection())
			{
				redrawAll();
			}

			for (int i = 0; i < m_PlotInfo.length; i++)
			{
				drawPlot(i);
			}
		}
		else
		{
			textSize(15);
			PlotInfo p = m_PlotInfo[m_iSelectedPlot];
			if (p != null)
			{
				m_MagnifiedViewCoords = new Rect(m_ViewCoords[0],
												 m_ViewCoords[1] + 50,
												 m_ViewCoords[2] - m_ViewCoords[0] - 100,
												 m_ViewCoords[3] - m_ViewCoords[1] - 50);
				Rect prevView = p.m_View;
				p.m_View = m_MagnifiedViewCoords;
				Bound2D prevBound = p.m_PlotBound;
				p.m_PlotBound = m_MagnifiedPlotBound;
				
				if (updateSelection())
				{
			 		redrawAll();
				}
				
				drawPlot(m_iSelectedPlot);
				
				drawSelectedInfo(new Rect(m_MagnifiedViewCoords.right() + 10, m_MagnifiedViewCoords.top(), 120, m_MagnifiedViewCoords.height));

				p.m_View = prevView;
				p.m_PlotBound = prevBound;
			}
		}
		
		if (m_iRecomputePheno != -1)
		{
			//int[] iGroupIds = m_EQTLOp.m_EQTLData.computeGroups(m_iRecomputePheno);
			//m_EQTLOp.m_EQTLData.calcSNPRatio(m_iRecomputePheno, iGroupIds, null);
			m_EQTLOp.m_EQTLData.calcImportantSNPs(m_iRecomputePheno);
			
	    	//m_EQTLOp.m_EQTLData.calcCommonSNP(m_iRecalculateGroup);
	    	EQTLTableFactory.updatePhenoTable(m_EQTLOp.m_TablePhenoType, m_EQTLOp.m_EQTLData);
	    	EQTLTableFactory.updateGenoTable(m_EQTLOp.m_TableGenoType, m_EQTLOp.m_EQTLData);
			
			
			Bound2D dataBound = calcDataBound(m_iRecomputePheno);
			m_PlotInfo[m_iRecomputePheno].m_DataBound = dataBound;
			
			boolean bMagnifiedAutoBound = false; // automatically calculates the new plot bounds to enclose all the visible data
			if (m_bMagnified && bMagnifiedAutoBound)
			{
				double dx = dataBound.dx() * 0.1;
				double dy = dataBound.dy() * 0.1;
				m_MagnifiedPlotBound.xmin = Math.min(dataBound.xmin - dx, m_MagnifiedPlotBound.xmin);
				m_MagnifiedPlotBound.xmax = Math.max(dataBound.xmax + dx, m_MagnifiedPlotBound.xmax);
				m_MagnifiedPlotBound.ymin = Math.min(dataBound.ymin - dy, m_MagnifiedPlotBound.ymin);
				m_MagnifiedPlotBound.ymax = Math.max(dataBound.ymax + dy, m_MagnifiedPlotBound.ymax);
				//= new Bound2D(dataBound.xmin - dx, dataBound.xmax + dx, dataBound.ymin - dy, dataBound.ymax + dy);
			}
			
			m_PlotInfo[m_iRecomputePheno].m_bRedraw = true;
	    	invokeRedraw();
			m_iRecomputePheno = -1;
		}
	}
	
	double[] binValues(double[] vals, double dMin, double dMax, int iNumBins)
	{
		double[] bins = new double[iNumBins];
		for (int i = 0; i < vals.length; i++)
		{
			int b = (int)((iNumBins * (vals[i] - dMin)) / (dMax - dMin));
			b = Math.min(Math.max(0, b), iNumBins - 1);
			bins[b]++;
		}
		return bins;
	}
	
	void drawLDAHistogram()
	{
		Rect rHist = new Rect (m_ViewCoords[0],
		  		 m_ViewCoords[1],
		  		 m_ViewCoords[2] - m_ViewCoords[0],
		  		 m_ViewCoords[3] - m_ViewCoords[1] - 350);
		
		// draw the sliders
		textSize(15);
		NumberFormat nf = NumberFormat.getInstance();
		nf.setMinimumFractionDigits(0);
		nf.setMaximumFractionDigits(3);
		int iNumSliders = m_EQTLOp.m_ExpressionCoefficients.length;
		double sliderMin = -02.0;
		double sliderMax = 02.0;
		stroke(0);
		Rect rSlider = null;
		for (int i = 0; i < iNumSliders; i++)
		{
			rSlider = new Rect(rHist.left + 100, rHist.bottom() + i * 20 + 50, rHist.width * 0.8f - 100, 7);
			if (m_DrawMouseState.isDragging() && rSlider.isInside(m_DrawMouseState.startX(), m_DrawMouseState.startY()))
			{
				m_EQTLOp.m_ExpressionCoefficients[i] = (m_DrawMouseState.endX() - rSlider.left) * (sliderMax - sliderMin) / rSlider.width + sliderMin;
			}
			float sx = (float)(rSlider.left + (m_EQTLOp.m_ExpressionCoefficients[i] - sliderMin) * rSlider.width / (sliderMax - sliderMin));
			fill(220); // slider
			rect(rSlider.left, rSlider.top, rSlider.width, rSlider.height);
			fill(160); // slider knob
			rect(sx - 2, rSlider.top, 5, rSlider.height+2);
			line(rSlider.left + rSlider.width/2, rSlider.top, rSlider.left + rSlider.width / 2, rSlider.bottom());
			fill(0);
			textAlign(RIGHT, TOP);
			text(nf.format(m_EQTLOp.m_ExpressionCoefficients[i]), rSlider.right() + 50, rSlider.top);
			if ( i < m_EQTLOp.m_EQTLData.getNumPhenoTypes())
			{
				text(m_EQTLOp.m_EQTLData.getPhenoTypeName(i), rSlider.left()-10, rSlider.top);
			}
		}
		textAlign(LEFT, TOP); // slider tick marks
		text(nf.format(sliderMin), rSlider.left, rSlider.bottom() + 10);
		textAlign(RIGHT, TOP); // slider tick marks
		text(nf.format(sliderMax), rSlider.right(), rSlider.bottom() + 10);

		double[] ldaOut = new double[m_EQTLOp.m_EQTLData.getNumIndividuals()];
		int iNumSick = 0;
		int iNumSelected = 0;
		int iSelectionCol = EQTLTableFactory.getSelectionCol(m_EQTLOp.m_TablePhenoType);
		for (int id = 0; id < m_EQTLOp.m_EQTLData.getNumIndividuals(); id++)
		{
			ldaOut[id] = m_EQTLOp.calcLDA(id);
			iNumSick += (m_EQTLOp.m_EQTLData.m_PhenoType[id].m_Affection == 2) ? 1 : 0;
			if (iSelectionCol != -1)
			{
				iNumSelected += (m_EQTLOp.m_TablePhenoType.getMeasurement(id, iSelectionCol) > 0) ? 1 : 0;
			}
		}
		int iNumWell = m_EQTLOp.m_EQTLData.getNumIndividuals() - iNumSick;
		double[] ldaSelected = iNumSelected > 0 ? new double[iNumSelected] : null;
		double[] ldaSick	 = iNumSick > 0 ? new double[iNumSick] : null;
		double[] ldaWell	 = iNumWell > 0 ? new double[iNumWell] : null;
		int iLeftWell = 0;
		int iRightWell = 0;
		int iLeftSick = 0;
		int iRightSick = 0;
		int iSick = 0, iWell = 0, iSelected = 0;
		for  (int id = 0; id < m_EQTLOp.m_EQTLData.getNumIndividuals(); id++)
		{
			if (m_EQTLOp.m_EQTLData.m_PhenoType[id].m_Affection == 2)
			{
				ldaSick[iSick++] = ldaOut[id];
				if (ldaOut[id] < 1.5)
					iLeftSick++;
				else
					iRightSick++;
			}
			else
			{
				ldaWell[iWell++] = ldaOut[id];
				if (ldaOut[id] < 1.5)
					iLeftWell++;
				else
					iRightWell++;
			}
			if (iSelectionCol != -1 && m_EQTLOp.m_TablePhenoType.getMeasurement(id, iSelectionCol) > 0)
			{
				ldaSelected[iSelected++] = ldaOut[id];
			}
		}
		
		double	HIST_MIN	= 0.0;	// minimum x
		double	HIST_MAX	= 3.0;	// minimum y
		int		HIST_BINS	= 100;	// number of histogram bins
		int		HIST_TOP	= 40;	// max y
		double[] histWell = binValues(ldaWell, HIST_MIN, HIST_MAX, HIST_BINS);
		double[] histSick = binValues(ldaSick, HIST_MIN, HIST_MAX, HIST_BINS);
		 
		fill(240);
		stroke(0);
		rect(rHist.left, rHist.top, rHist.width, rHist.height);
		fill(32);
		PUtils.drawHistogram(g, histWell, histWell.length, rHist.left, rHist.top, rHist.width, rHist.height, HIST_TOP, "", "", null);
		fill(0, 0, 255, 128);
		PUtils.drawHistogram(g, histSick, histSick.length, rHist.left, rHist.top, rHist.width, rHist.height, HIST_TOP, "", "", null);
		if (iNumSelected > 0)
		{
			fill(255, 0, 0, 255);
			double[] histSelected = binValues(ldaSelected, HIST_MIN, HIST_MAX, HIST_BINS);
			PUtils.drawHistogram(g, histSelected, histSelected.length, rHist.left, rHist.top, rHist.width, rHist.height, 50, "", "", null);
		}
		
		fill(0);
		textAlign(CENTER, TOP); // histogram x tick marks
		text(nf.format(HIST_MIN), rHist.left, rHist.bottom() + 5);
		text(nf.format(HIST_MAX), rHist.right(), rHist.bottom() + 5);
		text(nf.format((HIST_MIN + HIST_MAX)/2), rHist.left + rHist.width/2, rHist.bottom() + 5);
		
		textAlign(RIGHT, TOP); // histogram y tick marks
		text(Integer.toString(HIST_TOP), rHist.left - 5, rHist.top);
		
		textAlign(LEFT, TOP);
		fill(32);
		text("Well: "+iLeftWell, rHist.left()  + 20, rHist.top + 20);
		fill(0, 0, 255);
		text("Sick: "+iLeftSick, rHist.left()  + 20, rHist.top + 40);
		textAlign(RIGHT, TOP);
		fill(32);
		text("Well: "+iRightWell, rHist.right() - 20, rHist.top + 20);
		fill(0, 0, 255);
		text("Sick: "+iRightSick, rHist.right() - 20, rHist.top + 40);
		
		// vertical line in the middle to separate the sick and healthy region
		line (rHist.left + rHist.width() / 2, rHist.bottom(), rHist.left + rHist.width / 2, rHist.top);
		
	}
	
	void drawSelectedInfo(Rect area)
	{
		textSize(15);

		String info[] = m_EQTLOp.getSelectedInfo(m_EQTLOp.m_CurrTable);
		String genoInfo[] = m_EQTLOp.m_CurrTable == m_EQTLOp.m_TableGenoType ? info : m_EQTLOp.getSelectedInfo(m_EQTLOp.m_TableGenoType);
		int[] selectedSNPs = null;
		double[] selectedValues = null; // the SLR values for selected snps
		int[] sortedSNPs = null;
		int iPheno = m_iSelectedPlot < m_EQTLOp.m_EQTLData.getNumPhenoTypes() ? m_iSelectedPlot : 0;
		if (genoInfo != null && genoInfo.length > 0)
		{
			selectedSNPs = new int[genoInfo.length];
			selectedValues	= new double[genoInfo.length];
			for (int i = 0; i < genoInfo.length; i++)
			{
				selectedSNPs[i] = m_EQTLOp.m_EQTLData.getSNPIndex(genoInfo[i]);
				selectedValues[i] = m_EQTLOp.m_EQTLData.m_SNPInfo[selectedSNPs[i]].m_RatioLow[iPheno];
			}
			sortedSNPs = FloatIndexer.sortFloatsRev(selectedValues);
		}
		
		
		if (info != null)
		{
			stroke(0);
			noFill();
			rect(area.left, area.top, area.width, area.height);
			float y = area.top + 5;
			float dy = 20;
			textAlign(LEFT, TOP);
			fill(0);
			text("Selected: " + Integer.toString(info.length), area.left + 5, y);
			y += dy;
			for (int i = 0; i < info.length; i++)
			{
				if (y + dy > area.bottom())
					break;
				if (m_EQTLOp.m_CurrTable == m_EQTLOp.m_TableGenoType)
				{
					text(info[sortedSNPs[i]], area.left + 5, y += dy);
					int snp = m_EQTLOp.m_EQTLData.getSNPIndex(info[sortedSNPs[i]]);
					if (snp >= 0)
					{
						int a0 = m_EQTLOp.m_EQTLData.m_GroupInfo[m_iSelectedPlot].m_ConsensusAllele[0][snp];
						int a1 = m_EQTLOp.m_EQTLData.m_GroupInfo[m_iSelectedPlot].m_ConsensusAllele[1][snp];
						int a2 = m_EQTLOp.m_EQTLData.m_GroupInfo[m_iSelectedPlot].m_ConsensusAllele[2][snp];
						
						text(EQTLData.getAlleleString(a1) + " " + EQTLData.getAlleleString(a0) + " " + EQTLData.getAlleleString(a2), area.left + 15, y += dy - 5);
					}
				}
				else if (m_EQTLOp.m_CurrTable == m_EQTLOp.m_TablePhenoType && selectedSNPs != null)
				{
					text(info[i], area.left + 5, y += dy);
					int iPerson = Integer.parseInt(info[i]) - 1;
					if (iPerson > 0)
					{
						String strSNPs = "";
						for (int snp = 0; snp < sortedSNPs.length; snp++)
						{
							strSNPs += m_EQTLOp.m_EQTLData.m_GenoType[iPerson].m_SNP[selectedSNPs[sortedSNPs[snp]] * 2];
							strSNPs += m_EQTLOp.m_EQTLData.m_GenoType[iPerson].m_SNP[selectedSNPs[sortedSNPs[snp]] * 2 + 1] + " ";
						}
						text(strSNPs, area.left + 15, y += dy - 5);
					}
				}
			}
		}
	}
	
	void setMagnifiedPlot(int iPlot)
	{
		m_bMagnified = true;
		m_iSelectedPlot = iPlot;
		PlotInfo p = m_PlotInfo[m_iSelectedPlot];
		
		if (p != null)
		{
			double dx = p.m_DataBound.dx() * 0.1;
			double dy = p.m_DataBound.dy() * 0.1;
			m_MagnifiedPlotBound = new Bound2D(p.m_DataBound.xmin - dx, p.m_DataBound.xmax + dx, p.m_DataBound.ymin - dy, p.m_DataBound.ymax + dy);
		}
		
	}
	
	@Override
	public synchronized void mousePressed()
	{
		super.mousePressed();
		if (mouseButton == LEFT)
		{
			if (!m_bMagnified)
			{
				for (int i = 0; i < m_PlotInfo.length; i++)
				{
					PlotInfo p = m_PlotInfo[i];
					if (p != null && p.m_View.isInside(mouseX, mouseY))
					{
						m_iSelectedPlot = i;
						if (mouseEvent.getClickCount() == 2)
						{
							setMagnifiedPlot(i);
						}
						break;
					}
				}
			}
			else if (m_iSelectedPlot != -1 && mouseEvent.getClickCount() == 2)
			{
				if (m_MagnifiedViewCoords.isInside(mouseX, mouseY))
				{
					m_bMagnified = false;
					updatePlotInfo();
				}
			}
		}
		invokeRedraw();
	}
	
	@Override
	public synchronized void mouseReleased()
	{
		super.mouseReleased();
		invokeRedraw();
	}

	
	@Override
	public synchronized void mouseDragged()
	{
		super.mouseDragged();
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
		//e.getWheelRotation();
	}
	
	@Override
	public void keyPressed()
	{
		super.keyPressed();
	}

	@Override
	public void stateChanged(ChangeEvent e)
	{
		if( e.getSource() instanceof JSlider )
		{
			
			JSlider source = (JSlider)e.getSource();
			if (source.getName().equalsIgnoreCase("size_slider")) {
			    if (!source.getValueIsAdjusting()) {
					
			    	m_ScatterPlot.m_Style.m_PointSize = source.getValue();
			 		redrawAll();
			    	invokeRedraw();
			    }
			}
			else if (source.getName().equalsIgnoreCase("alpha_slider")) {
			    if (!source.getValueIsAdjusting()) {
					
			    	m_ScatterPlot.m_Style.m_PointAlpha = (source.getValue())/255.0;
			 		redrawAll();
			    	invokeRedraw();
			    }
			}
		}
	}
}
