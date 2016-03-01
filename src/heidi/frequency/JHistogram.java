package heidi.frequency;

import heidi.data.query.ElementListModel;
import heidi.project.Dim;

import java.awt.Color;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.JPanel;

import prefuse.Constants;
import prefuse.Display;
import prefuse.controls.ControlAdapter;
import prefuse.data.expression.Predicate;
import prefuse.util.ColorLib;
import prefuse.util.display.PaintListener;
import prefuse.visual.VisualItem;

public class JHistogram extends JPanel {
	
	private ElementListModel  m_model;
	private Dim               m_dim;
	
	private FrequencyTable    m_freqTable;
	private HistogramPlot     m_plot;
	
    private static final long serialVersionUID = -1167011004254303969L;
	
    public JHistogram(ElementListModel model, Dim dim) {
    	this(model, dim, (Predicate)null, (Predicate)null);
    }
    
	public JHistogram(ElementListModel model, Dim dim, Predicate primary, Predicate secondary) {
		super();        
		
		m_dim = dim;		
		m_model = model;
		
		initFrequencyTable(primary, secondary);
		initPlot();
	}
	
	public void setColors(int highlight, int[] primaryPalette, int[] secondaryPalette) {
		m_plot.setColors(highlight, primaryPalette, secondaryPalette);
	}
    
	@Override
	public void setBackground(Color bg) {
		super.setBackground(bg);
		if (m_plot != null) {
			m_plot.setBackground(bg);
		}
	}
	
	private void initFrequencyTable(Predicate primary, Predicate secondary) {
		if (m_dim.getType() == Constants.NUMERICAL) {
			m_freqTable = new NumericalFrequencyTable(m_dim, primary, secondary);
		} else {
			m_freqTable = new OridinalFrequencyTable(m_dim, primary, secondary);
		}
	}
	
	private void initPlot() {
		
		m_plot = new HistogramPlot(m_freqTable);
		
		// listeners
		MouseSelectionListener listener = new MouseSelectionListener();
		m_plot.addControlListener(listener);
		m_plot.addPaintListener(listener);
		
		// minimum bar width will be 3 pixels
		int binCount = m_dim.getBinCount();
		if (binCount < (300/3)) {
			m_plot.setPreferredSize(new Dimension(300, 75));
			m_plot.setMinimumSize(new Dimension(300, 75));
		} else {
			int width = binCount * 3 + 10;
			m_plot.setPreferredSize(new Dimension(width, 75));
			m_plot.setMinimumSize(new Dimension(width, 75));
		}
		
		// layout
		GridBagLayout layout = new GridBagLayout();
		setLayout(layout);
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.weightx = 1.0;
		constraints.weighty = 1.0;
		constraints.fill = GridBagConstraints.BOTH;
		add(m_plot, constraints);
	}
	
	private void startProgress() {
		Container contentPane = getParent();
		while (contentPane.getParent() != null) {
			contentPane = contentPane.getParent();
		}
		contentPane.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
	}
	
	private void endProgress() {
		Container contentPane = getParent();
		while (contentPane.getParent() != null) {
			contentPane = contentPane.getParent();
		}
		contentPane.setCursor(null);
	}
	
    private class MouseSelectionListener extends ControlAdapter implements PaintListener {

    	private Point2D m_mouseStart;
    	private Point2D m_mouseEnd;
    	private boolean m_mouseDown;
        
        // ControlAdapter event handlers
        
        @Override
        public void itemClicked(VisualItem item, MouseEvent e) {
        	if (!item.isInGroup(m_plot.m_groupID)) {
        		return;
        	}
        	
        	startProgress();
        	m_model.setValueIsAdjusting(true);
        	try {
	        	boolean select = !item.isHighlighted();
	        	if (!e.isControlDown()) {
	        		// single selection
	        		// clear previous selection
	        		m_model.clearSelection();
	        		select = true;
	        	}
	        	// TODO add support for shift selection of a range
	        	
	        	if (m_dim.getType() == Constants.NUMERICAL) {
	        		setNumericalSelection(item, item, select);
	        	} else {
	        		setOrdinalSelection(item, select);
	        	}
        	} finally {
        		m_model.setValueIsAdjusting(false);
        		endProgress();
        	}
        }
        
        @Override
        public void mousePressed(MouseEvent e) {
        	m_mouseStart = e.getPoint();
        	m_mouseDown = true;
        }

        @Override
        public void mouseReleased(MouseEvent e) {
        	m_mouseEnd = e.getPoint();
        	m_mouseDown = false;
        	
        	// Get rectangle in component coordinates
        	// Use full height of histogram
        	Rectangle2D itemBounds = m_plot.getItemBounds();
        	int y1 = (int)itemBounds.getMinY();
        	int y2 = (int)itemBounds.getMaxY();
        	int x1 = (int) Math.min(m_mouseStart.getX(), m_mouseEnd.getX());
        	int x2 = (int) Math.max(m_mouseStart.getX(), m_mouseEnd.getX());
        	int width = x2 - x1;
        	int height = y2 - y1;
        	Rectangle rect = new Rectangle(x1, y1, width, height);
    		
    		// User may accidently select a rectangle when only intending
    		// to click.  To prevent the selection from being cleared,
    		// require a minimum selection width of 2 pixels.
    		if (rect.width < 2) {
    			// redraw to remove selection rectangle
    			m_plot.redraw();
    			return;
    		}
    		
    		startProgress();
    		m_model.setValueIsAdjusting(true);
    		try {
    			if (e.isControlDown()) {
    				// Multiple selection
    				// Add to existing selection
    			} else {
		    		// Clear previous selection and selects only
		    		// items in drag rectangle
		    		m_model.clearSelection();
    			}
	    		Vector<VisualItem> selected = new Vector<VisualItem>();
	    		for (Iterator<?> iterator = m_plot.getVisualization().visibleItems(); iterator.hasNext();) {
					VisualItem item = (VisualItem) iterator.next();
					if (item.isInGroup(m_plot.m_groupID)) {
						Rectangle2D bounds = item.getBounds();
						if (bounds.intersects(rect)) {
							selected.add(item);
						}
						if (bounds.getX() + bounds.getWidth() > rect.getX() + rect.getWidth()) {
							// Optimization - stop looking for intersects if item bounds are
							// greater than selection bounds
							break;
						}
					}
	    		}
	    		
	    		if (selected.size() > 0) {
	    			if (m_dim.getType() == Constants.NUMERICAL) {
	    				setNumericalSelection(selected.get(0), selected.get(selected.size()-1), true);
	    			} else {
	    				for (int i = 0; i < selected.size(); i++) {
	    					setOrdinalSelection(selected.get(i), true);
	    				}
					}
	    		}
    		} finally {
    			m_model.setValueIsAdjusting(false);
    			endProgress();
    			
    		}
    		
    		// redraw to remove selection rectangle
    		m_plot.repaint();
        }
  
        @Override
        public void mouseDragged(MouseEvent e) {
        	if (m_mouseDown) {
        		m_mouseEnd = e.getPoint();
        		// redraw to paint selection rectangle
        		Display display = (Display)e.getComponent();
        		display.repaint();
        	}
        }
        
        // PaintListener event handlers
        
        public void postPaint(Display d, Graphics2D g) {
        	if(m_mouseDown && m_mouseEnd != null) {
        		
        		Rectangle2D itemBounds = m_plot.getItemBounds();
        		int x1 = (int) Math.min(m_mouseStart.getX(), m_mouseEnd.getX());
        		int x2 = (int) Math.max(m_mouseStart.getX(), m_mouseEnd.getX());
        		int y1 = (int)itemBounds.getMinY();
        		int y2 = (int)itemBounds.getMaxY();
        		int width = x2 - x1;
        		int height = y2 - y1;
        		
        		// draw selection rectangle
        		g.setColor(new Color(ColorLib.hex("#0000FF"), false));
        		g.drawRect(x1, y1, width, height);
        		g.setColor(new Color(ColorLib.hex("#250000FF"), true));
        		g.fillRect(x1+1, y1+1, width-1, height-1);
        	}
        }

		public void prePaint(Display d, Graphics2D g) {}
		
		private void setNumericalSelection(VisualItem start, VisualItem end, boolean selected) {
			
			String column = m_freqTable.getBinColumn();
			int size = m_model.getSize();
			
			int selStart = Integer.MIN_VALUE;
			int selEnd = 0;
			
			// get first index in model
			double binStart = start.getDouble(column);
			for (int i = 0; i < size; i++) {
				double d = m_model.getDoubleAt(i);
				if (d >= binStart) {
					selStart = i;
					break;
				}
			}
			
			double binEnd = Double.MAX_VALUE;
			int row = end.getRow();
			String type = end.getString(m_freqTable.getTypeColumn());
			
			if (FrequencyTable.PRIMARY.equals(type)) {
				if (row < m_freqTable.getBinCount() - 1) {
					binEnd = m_freqTable.getDouble(row + 1, column);
				}
			} else {
				if (row < 2* m_freqTable.getBinCount() - 2) {
					binEnd = m_freqTable.getDouble(row + 1, column);
				}
			}

			for (int i = selStart; i < size; i++) {
				double d = m_model.getDoubleAt(i);
				if (d < binEnd) {
					selEnd = i;
				} else {
					break;
				}
			}
			
			// set selection for model items in current bin
			if (selStart != Integer.MIN_VALUE) {
				if (selected) {
					m_model.addSelectionInterval(selStart, selEnd);
				} else {
					m_model.removeSelectionInterval(selStart, selEnd);
				}
			}
		}
		
		private void setOrdinalSelection(VisualItem item, boolean selected) {
			if (!item.isInGroup(m_plot.m_groupID)) {
				return;
			}
			// For ordinal data, each bin maps to a single value
			Object element = item.get(m_freqTable.getBinColumn());
			int index = m_model.indexOf(element);
			if (selected) {
				m_model.addSelectionInterval(index, index);
			} else {
				m_model.removeSelectionInterval(index, index);
			}
		}
    }
}
