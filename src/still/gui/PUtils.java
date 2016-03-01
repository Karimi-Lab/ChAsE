/**
 * Wraps commonly used draw routines.
 */

package still.gui;

import java.text.NumberFormat;

import processing.core.PConstants;
import processing.core.PGraphics;

public class PUtils
{

	public static void drawRotatedText(PGraphics p, String txt, float x, float y, float r)
	{
		p.pushMatrix();
		p.translate(x, y);
		p.rotate(r); // draw the text vertically
		p.text(txt, 0, 0);
		p.popMatrix();
	}
	
	public static void drawRoundRect(PGraphics p, float x, float y, float w, float h, float rx, float ry)
	{
		p.beginShape();
		p.vertex(x,y+ry); //top of left side 
		p.bezierVertex(x,y,x,y,x+rx,y); //top left corner
		  
		p.vertex(x+w-rx,y); //right of top side 
		p.bezierVertex(x+w,y,x+w,y,x+w,y+ry); //top right corner
		  
		p.vertex(x+w,y+h-ry); //bottom of right side
		p.bezierVertex(x+w,y+h,x+w,y+h,x+w-rx,y+h); //bottom right corner
		  
		p.vertex(x+rx,y+h); //left of bottom side
		p.bezierVertex(x,y+h,x,y+h,x,y+h-ry); //bottom left corner
		p.endShape(PConstants.CLOSE);
	}
	
	public static void drawArrow(PGraphics p, float x1, float y1, float x2, float y2, float dx, float dy, boolean bDrawArrowLine)
	{
		if (bDrawArrowLine) {
			p.line(x1, y1, x2, y2);
		}
		p.pushMatrix();
		p.translate(x2, y2);
		float a = (float)Math.atan2(x1-x2, (y2-y1)*0.1f);
		p.rotate(a);
		p.line(0, 0, -dy, -dx);
		p.line(0, 0, dy, -dx);
		p.popMatrix();
	}

	public static void drawProfile(PGraphics p, double[] values, double dMin, double dMax, float fLeft, float fTop, float fWidth, float fHeight, boolean bFilled)
	{
		if (values == null || values.length == 0 || dMax - dMin <= 0)
			return;
		
		float fPrevX = 0;
		float fPrevY = 0;
		
		for (int i = 0; i < values.length; i++)
		{
			float fW = fWidth / (values.length - 1);
			float fH = (float)Math.min(fHeight, (values[i] - dMin) * fHeight / (dMax - dMin));
			float fX = i * fW + fLeft;
			float fY = (float)(fTop + fHeight - fH);

			if (bFilled)
			{
				p.rect(fX, fTop + fHeight, fW, -fH);
			}
			else if (i > 0)
			{
				p.line (fPrevX, fPrevY, fX, fY);
			}
			fPrevX = fX;
			fPrevY = fY;
		}	

	}
	
	public static void drawHistogram(PGraphics p, double[] values, int iNumBins, float fLeft, float fTop, float fWidth, float fHeight, double dMaxBinValue, String xLabel, String yLabel, String[] xTicks)
	{
		if (values == null || values.length == 0)
			return;
		
		double[] dBinVal = new double[iNumBins];
		double dMaxBinVal = dMaxBinValue;
		double dSum = 0;
		int ival = 0;
		for (int ibin = 0; ibin < iNumBins; ibin++)
		{
			while (ival < (ibin + 1) * values.length/iNumBins && ival < values.length)
			{
				dBinVal[ibin] += values[ival];
				ival++;
			}
			if (dMaxBinValue == -1)
			{
				dMaxBinVal = Math.max(dMaxBinVal, dBinVal[ibin]);
			}
			dSum += dBinVal[ibin];
		}

		float fBottom = fTop + fHeight;
		for (int ibin = 0; ibin < iNumBins; ibin++)
		{
			float h = -Math.min(fHeight, (float)(fHeight*dBinVal[ibin]/dMaxBinVal));
			p.rect(fLeft + ibin * fWidth / iNumBins, fBottom, fWidth / iNumBins, h);
		}
		
		// draw labels for the y axis
		NumberFormat nf = NumberFormat.getInstance();
		nf.setMaximumFractionDigits(1);
		
		//p.textAlign(PConstants.RIGHT, PConstants.TOP);
		//p.text(nf.format(100 * dMaxBinVal / dSum)+"%", fLeft - 5, fTop);
		
		// draw labels for the x axis
		if (xTicks != null)
		{
			p.textAlign(PConstants.CENTER, PConstants.TOP);
			for (int i = 0; i < xTicks.length; i++)
			{
				p.text(xTicks[i], fLeft + i * fWidth / (xTicks.length - 1), fBottom + 5);
			}
		}
		
		p.textAlign(PConstants.CENTER, PConstants.BOTTOM);
		p.text(xLabel, fLeft + fWidth / 2, fTop);

		p.textAlign(PConstants.CENTER, PConstants.TOP);
		p.pushMatrix();
		p.translate(fLeft + fWidth, fTop + fHeight / 2);
		p.rotate(-PConstants.PI/2);
		p.text(yLabel, 0, 0);
		p.popMatrix();

	}
	
}
