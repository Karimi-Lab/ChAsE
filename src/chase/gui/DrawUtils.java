package chase.gui;

import java.awt.geom.Line2D;

import processing.core.*;

public class DrawUtils 
{
	// constants
	static final int Y_AXIS = 1;
	static final int X_AXIS = 2;

	/**
	 * 
	 * @param gx
	 * @param x
	 * @param y
	 * @param w
	 * @param h
	 * @param c1
	 * @param c2
	 * @param axis
	 */
	static void gradientRect(PGraphics gx, int x, int y, float w, float h, int c1, int c2, int axis) 
	{
		// calculate differences between color components
		float deltaR = gx.red(c2) - gx.red(c1);
		float deltaG = gx.green(c2) - gx.green(c1);
		float deltaB = gx.blue(c2) - gx.blue(c1);

		// choose axis
		if (axis == Y_AXIS) {
			// nested for loops set pixels
			// in a basic table structure
			// row
			for (int j = y; j <= (y + h); j++) {
				int c = gx.color(
						(gx.red(c1) + (j - y) * (deltaR / h)), 
						(gx.green(c1) + (j - y) * (deltaG / h)),
						(gx.blue(c1) + (j - y) * (deltaB / h)));
				// column
				for (int i = x; i <= (x + w); i++) {
					gx.set(i, j, c);
				}
			}
		} else if (axis == X_AXIS) {
				// row
			for (int j = x; j <= (x + w); j++) {
				int c = gx.color(
						(gx.red(c1) + (j - x) * (deltaR / w)),
						(gx.green(c1) + (j - x) * (deltaG / w)),
						(gx.blue(c1) + (j - x) * (deltaB / w)));
				for (int i = y; i <= (y + h); i++) {
					// column
					gx.set(j, i, c);
				}
			}
		}
	}

	public static final float[] LINE_STYLE_SOLID    = {1000000, 0.001f};
	public static final float[] LINE_STYLE_DASHED   = {10, 10};
	public static final float[] LINE_STYLE_DOTTED   = {2, 10};
	public static final float[] LINE_STYLE_DOTDASH  = {2, 10, 10, 10};
	public static final float[] LINE_STYLE_LONGDASH = {20, 10};
	public static final float[] LINE_STYLE_TWODASH  = {10, 10, 20, 10};
	public static final float[][] LINE_STYLE_TYPES  = {
		LINE_STYLE_SOLID,
		LINE_STYLE_DASHED,
		LINE_STYLE_DOTTED,
		LINE_STYLE_DOTDASH,
		LINE_STYLE_LONGDASH,
		LINE_STYLE_TWODASH
	};
	
	
	/**
	 * Draw a dashed line with given set of dashes and gap lengths.
	 * 
	 * @param gx
	 *            pointer to the PGraphics object
	 * @param x0
	 *            starting x-coordinate of line.
	 * @param y0
	 *            starting y-coordinate of line.
	 * @param x1
	 *            ending x-coordinate of line.
	 * @param y1
	 *            ending y-coordinate of line.
	 * @param lineStyle
	 *            array giving lengths of dashes and gaps in pixels;
	 *            an array with values {5, 3, 9, 4} will draw a line with a
	 *            5-pixel dash, 3-pixel gap, 9-pixel dash, and 4-pixel gap. if
	 *            the array has an odd number of entries, the values are
	 *            recycled, so an array of {5, 3, 2} will draw a line with a
	 *            5-pixel dash, 3-pixel gap, 2-pixel dash, 5-pixel gap, 3-pixel
	 *            dash, and 2-pixel gap, then repeat.
	 */
	public static void dashline(PGraphics gx, float x0, float y0, float x1, float y1,
			float[] lineStyle)
	{
		dashline(gx, x0, y0, x1, y1, lineStyle, 0);
	}
	
	/**
	 * source: http://www.openprocessing.org/sketch/7013 
	 */
	private static float dashline(PGraphics gx, float x0, float y0, float x1, float y1,
			float[] lineStyle, float offset) 
	{
		float distance = PApplet.dist(x0, y0, x1, y1);
		float[] xSpacing = new float[lineStyle.length];
		float[] ySpacing = new float[lineStyle.length];
		float drawn = 0; // amount of distance drawn

		if (distance > 0) {
			 // Figure out x and y distances for each of the spacing values I
			 // decided to trade memory for time; I'd rather allocate a few dozen
			 // bytes than have to do a calculation every time I draw.
			float sumSpacing = 0;
			for (int i = 0; i < lineStyle.length; i++) {
				xSpacing[i] = PApplet.lerp(0, (x1 - x0), lineStyle[i] / distance);
				ySpacing[i] = PApplet.lerp(0, (y1 - y0), lineStyle[i] / distance);
				sumSpacing += lineStyle[i];
			}
			offset = offset % sumSpacing;
			sumSpacing = 0;
			int si = -1; // spacing index
			float ds = 0; // delta spacing
			for (int i = 0; i < lineStyle.length; i++) {
				sumSpacing += lineStyle[i];
				if (si == -1 && offset < sumSpacing) {
					si = i;
					ds = sumSpacing - offset;
				}
			}
			
			boolean drawLine = (si % 2) == 0; // alternate between dashes and gaps
			while (drawn < distance) {
				ds = Math.min(ds, distance - drawn);
				float dx = xSpacing[si]*ds/lineStyle[si];
				float dy = ySpacing[si]*ds/lineStyle[si];
				if (drawLine) {
					gx.line(x0, y0, x0 + dx, y0 + dy);
				}
				x0 += dx;
				y0 += dy;
				// Add distance "drawn" by this line or gap
				drawn = drawn + ds;//PApplet.mag(xSpacing[si], ySpacing[si]);
				si = (si + 1) % lineStyle.length; // cycle through array
				
				ds = lineStyle[si];
				drawLine = !drawLine; // switch between dash and gap
			}
		}
		return offset + distance;
	}
	
	/**
	 * Draw a dashed line with given dash and gap length.
	 * 
	 * @param gx
	 *            pointer to the PGraphics object
	 * @param x0
	 *            starting x-coordinate of line.
	 * @param y0
	 *            starting y-coordinate of line.
	 * @param x1
	 *            ending x-coordinate of line.
	 * @param y1
	 *            ending y-coordinate of line.
	 * @param dash
	 *            - length of dashed line in pixels
	 * @param gap
	 *            - space between dashes in pixels
	 */
	public static void dashline(PGraphics gx, float x0, float y0, float x1, float y1,
			float dash, float gap)
	{
		float[] lineStyle = { dash, gap };
		dashline(gx, x0, y0, x1, y1, lineStyle);
	}

	/**
	 * 
	 * @param gx
	 * @param str
	 * @param x
	 * @param y
	 * @param rotAngle
	 */
	static void rotatedText(PGraphics gx, String str, float x, float y, float rotAngle)
	{
		gx.pushMatrix();
		gx.translate(x, y);
		gx.rotate(rotAngle);
		gx.text(str, 0, 0);
		gx.popMatrix();
	}
	
	static final int ARROW_DIR_LEFT  = 0;
	static final int ARROW_DIR_RIGHT = 1;
	static final int ARROW_DIR_UP    = 2;
	static final int ARROW_DIR_DOWN  = 3;
	/**
	 * 
	 * @param gx
	 * @param x
	 * @param y
	 * @param w
	 * @param h
	 * @param direction
	 */
	static void arrowHead(PGraphics gx, float x, float y, float w, float h, int direction)
	{
		// up arrow
		gx.beginShape();
		switch (direction)
		{
			case ARROW_DIR_LEFT:
				gx.vertex(x + w, y);
				gx.vertex(x + w, y + h);
				gx.vertex(x, y + h/2);
				break;
			case ARROW_DIR_RIGHT:
				gx.vertex(x, y);
				gx.vertex(x + w, y + h/2);
				gx.vertex(x, y + h);
				break;
			case ARROW_DIR_UP:
				gx.vertex(x, y + h);
				gx.vertex(x + w/2, y);
				gx.vertex(x + w, y + h);
				break;
			case ARROW_DIR_DOWN:
				gx.vertex(x, y);
				gx.vertex(x + w, y);
				gx.vertex(x + w/2, y + h);
				break;
		}
		gx.endShape(PApplet.CLOSE);
	}
	
	/**
	 * Draws a profile curve
	 * @param gx
	 * @param values
	 * @param indices
	 * @param dMin
	 * @param dMax
	 * @param fLeft
	 * @param fTop
	 * @param fWidth
	 * @param fHeight
	 * @param bClosed
	 */
	public static void drawProfile(PGraphics gx, double[] values, int[] indices, 
			double dMin, double dMax, 
			float fLeft, float fTop, float fWidth, float fHeight, 
			boolean bClosed)	
	{
		PVector[] vertices = getProfileVertices(values, indices, dMin, dMax, fLeft, fTop, fWidth, fHeight, bClosed);
		if (vertices == null)
			return;
		
		gx.beginShape();
		
		for (int i = 0; i < vertices.length; i++) {
			gx.vertex(vertices[i].x, vertices[i].y);
		}

		if (bClosed) {
			gx.endShape(PGraphics.CLOSE);
		} else {
			gx.endShape();
		}
	}
	
	public static void drawDashlineProfile(PGraphics gx, double[] values, int[] indices, 
			double dMin, double dMax,
			float fLeft, float fTop, float fWidth, float fHeight,
			boolean bClosed, float[] lineStyle)
	{
		PVector[] vertices = getProfileVertices(values, indices, dMin, dMax, fLeft, fTop, fWidth, fHeight, bClosed);
		if (vertices == null)
			return;
		
		float offset = 0;
		for (int i = 1; i < vertices.length; i++) {
			offset = dashline(gx, vertices[i-1].x, vertices[i-1].y, vertices[i].x, vertices[i].y, lineStyle, offset);
		}
		
		if (bClosed) {
			dashline(gx, vertices[vertices.length - 1].x, vertices[vertices.length - 1].y, vertices[0].x, vertices[0].y, lineStyle, offset);
		}
	}

	private static PVector[] getProfileVertices(double[] values, int[] indices, 
			double dMin, double dMax, 
			float fLeft, float fTop, float fWidth, float fHeight, 
			boolean bClosed)
	{
		if (values == null || values.length == 0 || dMax - dMin <= 0)
			return null;

		int numVertices = values.length;
		if (indices == null && bClosed)
			numVertices += 2;
		
		PVector[] vertices = new PVector[numVertices];
		int iv = 0;
		
		float fW = fWidth / (values.length - 1);
		
		if (bClosed)
			fW = fWidth / (values.length - 2);
		
		if (indices == null && bClosed)
			vertices[iv++] = new PVector(fLeft, fTop+fHeight);
		
		for (int i = 0; i < values.length; i++)
		{
			float fH = (float)Math.min(fHeight, (values[i] - dMin) * fHeight / (dMax - dMin));
			float fX = (indices == null ? i : indices[i]) * fW + fLeft;
			float fY = (float)(fTop + fHeight - fH);

			vertices[iv++] = new PVector(fX, fY);
		}
		
		if (indices == null && bClosed)
			vertices[iv++] = new PVector(fLeft + fWidth, fTop+fHeight);
		
		return vertices;
	}
	
	public static double getProfileDist(float mx, float my,
			double[] values, int[] indices, 
			double dMin, double dMax, 
			float fLeft, float fTop, float fWidth, float fHeight, 
			boolean bClosed)
	{
		PVector[] vertices = getProfileVertices(values, indices, dMin, dMax, fLeft, fTop, fWidth, fHeight, bClosed);
		if (vertices == null)
			return 0;
		double dist = Double.POSITIVE_INFINITY;
		for (int i = 1; i < vertices.length; i++)
		{
			dist = Math.min(Line2D.ptSegDist(vertices[i-1].x, vertices[i-1].y, vertices[i].x, vertices[i].y, mx, my), dist);
		}

		return dist;
	}
	
	/**
	 * 
	 * @param gx
	 * @param values
	 * @param dMin
	 * @param dMax
	 * @param fLeft
	 * @param fTop
	 * @param fWidth
	 * @param fHeight
	 */
	public static void drawHistogram(PGraphics gx, double[] values, double dMin, double dMax, float fLeft, float fTop, float fWidth, float fHeight)
	{
		gx.noSmooth();
		if (values == null || values.length == 0 || dMax - dMin <= 0)
			return;
		
		float fW = fWidth / (values.length);
		
		for (int i = 0; i < values.length; i++)
		{
			float fH = (float)Math.min(fHeight, (values[i] - dMin) * fHeight / (dMax - dMin));
			float fX = i * fW + fLeft;
			float fY = (float)(fTop + fHeight - fH);
			gx.rect(fX, fY, fW, fH);
		}	
	}		
}
