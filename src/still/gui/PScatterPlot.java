package still.gui;

import processing.core.PGraphics;
import still.data.Table.ColType;
import still.gui.PEQTLPainter.Bound2D;
import still.gui.PEQTLPainter.Rect;

class PScatterPlot
{
	class PlotStyle
	{
		float	m_PointSize = 4.0f;
		double	m_PointAlpha = 0.5;
		int 	m_BackgroundColor = 0xFFFFFFFF;
		int		m_PointColor = 0xFF000000;
		int		m_SelectColor = 0xFFFF0000;
	}	
	
	still.data.Table	m_Table = null;
	int	m_iSelectionCol 	= -1;
	int	m_iColorCol 	= -1;
	PlotStyle m_Style = new PlotStyle();
	
	void setTable(still.data.Table table)
	{
		m_Table = table;
		m_iColorCol = -1;
		m_iSelectionCol = -1;
		if (table != null)
		{
			int i = 0;
			for (ColType type : table.getColTypes())
			{
				if (type == ColType.ATTRIBUTE && table.getColName(i).equalsIgnoreCase("selection") )
					m_iSelectionCol = i;
				if (type == ColType.ATTRIBUTE && table.getColName(i).equalsIgnoreCase("color") )
					m_iColorCol 	= i;
				i++;
			}
		}
	}

	void draw(PGraphics pg, int xcol, int ycol, Bound2D bound, Rect view)
	{
		pg.smooth();
	 	if (m_Table == null)
	 		return;
	 	
 		pg.noStroke();
 		
	 	double fVPRatioH = 1.0 * view.width() / bound.dx(); // horizontal view-to-screen ratio
 		double fVPRatioV = 1.0 * view.height() / bound.dy(); // vertical view-to-screen ratio
    	//float fTextHeight = textAscent() + textDescent();
    	//textAlign(CENTER);
    	//fill(255, 0, 0);
 		int iPointAlpha = (int)(m_Style.m_PointAlpha * 255);

		int iNumPass = m_iSelectionCol != -1 ? 2 : 1;
		for (int pass = 0; pass < iNumPass; pass++)
 		{
			// plot selected points in the pass 1
			int iCurrFillColor = ((pass == 1 ? m_Style.m_SelectColor : m_Style.m_PointColor)  & 0xFFFFFF) | (iPointAlpha << 24);
			pg.fill(iCurrFillColor);
 			
 			for( int k = 0; k < m_Table.rows(); k++ )
 			{// use the color for non-selected ones in pass 0
 				int colRGBA = 0;
 				if (m_iColorCol != -1)
 				{
					//colRGBA = (int) m_Table.getMeasurement(k, m_iColorCol );
					//if ((colRGBA & 0xFF000000) == 0)
					//	continue;// don' render transparent
 				}
 				
 				if (pass == 0 && m_iColorCol != -1)
 				{
					colRGBA = (int) m_Table.getMeasurement(k, m_iColorCol );
					if ((colRGBA & 0xFF000000) == 0)
						continue;// don' render transparent

					colRGBA = (colRGBA & 0xFFFFFF) | (iPointAlpha << 24);
					if (iCurrFillColor != colRGBA)
					{
						iCurrFillColor = colRGBA;
						pg.fill(colRGBA);
					}
				}
 				double dx = m_Table.getMeasurement(k,xcol);
 				double dy = m_Table.getMeasurement(k,ycol);
 				
 				if (bound.isInside(dx, dy) &&
					(m_iSelectionCol == -1 ||
					 (pass == 0 && m_Table.getMeasurement(k, m_iSelectionCol) < 1e-5) || // non selected in pass 0
					 (pass == 1 && m_Table.getMeasurement(k, m_iSelectionCol) > 0)) // selected in pass 1
				   )
 				{
 					float pointx = (float)(view.left() + (dx - bound.xmin) * fVPRatioH);
 					float pointy = (float)(view.bottom() - (dy - bound.ymin) * fVPRatioV);
					pg.ellipse(pointx, pointy, m_Style.m_PointSize, m_Style.m_PointSize);
 				}
 			}// for (k
 		}// for (pass
		pg.noSmooth();
	}
}
