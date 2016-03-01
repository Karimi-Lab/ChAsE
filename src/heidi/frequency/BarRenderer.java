package heidi.frequency;

import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import prefuse.Constants;
import prefuse.render.AbstractShapeRenderer;
import prefuse.visual.VisualItem;

/*
 * Based on BarRender by Kaitlin Sherwood
 * 
 * @author <a href="http://webfoot.com/ducky.home.html">Kaitlin Duck Sherwood</a>
 * @author <a href="http://jheer.org">jeffrey heer</a>
 * @author veronika
 */
class BarRenderer extends AbstractShapeRenderer {
	private int         m_orientation;
	private int         m_barWidth;
	private Rectangle2D m_bounds;
	private Rectangle2D m_shape;
	
	public BarRenderer() {
		this(10);
	}
	
	BarRenderer(int width) {
		m_barWidth = width;
		m_orientation = Constants.ORIENT_BOTTOM_TOP;
		m_bounds = new Rectangle2D.Double();
		m_shape = new Rectangle2D.Double();
	}

	void setBounds(Rectangle2D bounds) {
		m_bounds.setRect(bounds);
	}

	/**
	 * Sets the orientation of this layout. Must be one of
	 * {@link Constants#ORIENT_BOTTOM_TOP} (to grow bottom-up),
	 * {@link Constants#ORIENT_TOP_BOTTOM} (to grow top-down),
	 * {@link Constants#ORIENT_LEFT_RIGHT} (to grow left-right), or
	 * {@link Constants#ORIENT_RIGHT_LEFT} (to grow right-left).
	 * @param orient the desired orientation of this layout
	 * @throws IllegalArgumentException if the orientation value
	 * is not a valid value
	 */

	void setOrientation(int orient) {
		if (orient != Constants.ORIENT_TOP_BOTTOM &&
			orient != Constants.ORIENT_BOTTOM_TOP &&
			orient != Constants.ORIENT_LEFT_RIGHT &&
			orient != Constants.ORIENT_RIGHT_LEFT) {
			
			throw new IllegalArgumentException("Invalid orientation value: "+orient);
		}
		m_orientation = orient;
	}

	/*
	 * <b>NOTE:</b> For more efficient rendering, subclasses should use a
	 * single shape instance in memory, and update its parameters on each call
	 * to getRawShape, rather than allocating a new Shape object each time.
	 * 
	 * @override(non-Javadoc)
	 * @see prefuse.render.AbstractShapeRenderer#getRawShape(prefuse.visual.VisualItem)
	 */
	protected Shape getRawShape(VisualItem item) {
		double width, height;
		double x = item.getX();
		double y = item.getY();
		
		if (m_orientation == Constants.ORIENT_TOP_BOTTOM ||
		    m_orientation == Constants.ORIENT_BOTTOM_TOP) {
			
			width = m_barWidth*item.getSize(); 
			// getSize() is used for scaling
			// Center the bar around the x-location
			if (width > 1) {
				x = x-width/2;
			}
			
			if(m_orientation == Constants.ORIENT_BOTTOM_TOP) {
				height = m_bounds.getMaxY() - y;
			} else {
				height = y;
				y = m_bounds.getMinY();
			}
			
		} else {
			
			// Center the bar around the y-location
			height = m_barWidth*item.getSize(); // getSize() is used for scaling
			if (height > 1) {
				y = y-height/2;
			}
			
			if (m_orientation == Constants.ORIENT_RIGHT_LEFT) {
				width = m_bounds.getMaxX() - x;
			} else {
				width = x;
				x = m_bounds.getMinX();
			}
		}

		m_shape.setFrame(x, y, width, height);
        return m_shape;

	}
}
