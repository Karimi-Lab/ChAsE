package heidi.frequency;


import java.awt.Insets;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import prefuse.Constants;
import prefuse.Display;
import prefuse.Visualization;
import prefuse.action.Action;
import prefuse.action.ActionList;
import prefuse.action.RepaintAction;
import prefuse.action.assignment.DataColorAction;
import prefuse.action.layout.AxisLabelLayout;
import prefuse.action.layout.AxisLayout;
import prefuse.controls.ControlAdapter;
import prefuse.controls.ToolTipControl;
import prefuse.data.Table;
import prefuse.data.column.ColumnMetadata;
import prefuse.data.event.TableListener;
import prefuse.data.expression.ColumnExpression;
import prefuse.data.expression.ComparisonPredicate;
import prefuse.data.expression.Expression;
import prefuse.data.expression.Literal;
import prefuse.data.expression.Predicate;
import prefuse.data.query.NumberRangeModel;
import prefuse.render.AxisRenderer;
import prefuse.render.Renderer;
import prefuse.render.RendererFactory;
import prefuse.util.ColorLib;
import prefuse.util.ui.ValuedRangeModel;
import prefuse.visual.VisualItem;
import prefuse.visual.VisualTable;
import prefuse.visual.expression.VisiblePredicate;

/** 
 * Based on HistogramGraph by Kaitlin Sherwood 
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 * @author <a href="http://webfoot.com/ducky.home.html">Kaitlin Duck Sherwood</a>
 * @author veronika
 */

public class HistogramPlot extends Display {
	
	private FrequencyTable m_frequency;
	
	String m_groupID;
	private String m_xlabelID;
	private String m_ylabelID;
	private AxisLayout m_yaxis;
	
	private String m_drawAction;
	private String m_drawAxesAction;

	// bounds for plot
	private Rectangle2D m_plotBounds;
	private Rectangle2D m_xLabelBounds;
	private Rectangle2D m_yLabelBounds;
	
	// bar rendering
	private BarRenderer m_barRenderer;
	
	private static int s_index = 0;

	private static final String GROUPID  = "histogram";
	private static final String DRAW     = "draw";
	private static final String DRAWAXES = "drawaxes";
	private static final String XLABEL   = "xlabel";
	private static final String YLABEL   = "ylabel";
	
	private static final long serialVersionUID = 4645429829577226106L;
	
	public HistogramPlot(FrequencyTable frequencyTable) {
		super(new Visualization());
		
		m_frequency       = frequencyTable;
		
		m_groupID         = GROUPID+s_index++;
		m_drawAction      = m_groupID+DRAW;
		m_drawAxesAction  = m_groupID+DRAWAXES;
		m_xlabelID        = m_groupID+XLABEL;
		m_ylabelID        = m_groupID+YLABEL;

		m_plotBounds   = new Rectangle2D.Double();
		m_xLabelBounds = new Rectangle2D.Double();
		m_yLabelBounds = new Rectangle2D.Double();
		
		
		VisualTable visTable = m_vis.addTable(m_groupID, m_frequency);
		
		// BUG - VisualTable does not inherit comparator from Table 
		// FIX - Need to set comparator for each VisualTable but this should not be required
		String binColumn = m_frequency.getBinColumn();
		ColumnMetadata metaTable = m_frequency.getMetadata(binColumn);
		ColumnMetadata metaVTable = visTable.getMetadata(binColumn);
		metaVTable.setComparator(metaTable.getComparator());
		
		int barWidth = 8;
		int binCount = m_frequency.getBinCount();
		if (300/binCount < 8) {
			barWidth = 3;
		}
		m_barRenderer = new BarRenderer(barWidth);
		
		initComponent();
		initAxes();
		initRenderer();
		initMouseOver();
		
		String rangeColumn = m_frequency.getRangeColumn();
		String countColumn = m_frequency.getCountColumn();
		addControlListener(new ToolTipControl(new String[] {rangeColumn, countColumn}));
		
		// update histogram when data changes
		m_frequency.addTableListener(new TableListener() {
			@Override
			public void tableChanged(Table t, int start, int end, int col, int type) {
				redraw();
			}
		});
	}

	public FrequencyTable getFrequencyTable() {
		return m_frequency;
	}
	
	private void initComponent() {
		setHighQuality(true);
		addComponentListener(new ComponentAdapter() {
			public void componentResized(ComponentEvent e) {
				Insets insets = getInsets();
				insets.left += 10; insets.right += 10;
				insets.top += 10; insets.bottom += 10;
				int width = getWidth() - insets.left - insets.right;
				int height = getHeight() - insets.top - insets.bottom;
				int yLabelWidth = 40; // TODO calculate from string widths
				int xLabelWidth = 40; // TODO calculate from string widths
				int xLabelHeight = 20; // TODO calculate from font height

				m_plotBounds.setRect(insets.left+yLabelWidth, insets.top, width - yLabelWidth - xLabelWidth/2, height - xLabelHeight);
				m_xLabelBounds.setRect(insets.left+yLabelWidth, insets.top, width - yLabelWidth - xLabelWidth/2, height - xLabelHeight + 5);
				m_yLabelBounds.setRect(insets.left, insets.top, width, height - xLabelHeight);
				redraw();
			}
		});
	}

	void redraw() {
		m_vis.run(m_drawAction);
		m_vis.run(m_drawAxesAction);
	}
	
	private void initAxes() {

		String xColumn = m_frequency.getBinColumn();
		String yColumn = m_frequency.getCountColumn();
		
		// x-axis - "xColumn"
		AxisLayout xaxis = new AxisLayout(m_groupID, xColumn, Constants.X_AXIS, VisiblePredicate.TRUE);
		xaxis.setLayoutBounds(m_plotBounds);
		// x-axis labels
		AxisLabelLayout xlabels = new AxisLabelLayout(m_xlabelID, xaxis, m_xLabelBounds, 15);
		if (m_frequency.canGetDouble(xColumn)) {
			xlabels.setSpacing(40);
			DecimalFormat numberFormat = new DecimalFormat("0.00E0");
			xlabels.setNumberFormat(numberFormat);
		}
		
		// y-axis - "yColumn"
		m_yaxis = new AxisLayout(m_groupID, yColumn, Constants.Y_AXIS, VisiblePredicate.TRUE);
		m_yaxis.setLayoutBounds(m_plotBounds);
		m_yaxis.setDataType(Constants.NUMERICAL);
		
		Action yRange = new Action() {
			@Override
			public void run(double frac) {
				// The y-axis will start at the minimum value of the data set.  This will 
				// result in a bar of zero height for the bin with the smallest value.
				// The workaround is to always start y axis at 0.
				String countColumn = m_frequency.getCountColumn();
				ColumnMetadata columnData = m_frequency.getMetadata(countColumn);
				double max = m_frequency.getDouble(columnData.getMaximumRow(), countColumn);
				ValuedRangeModel yRangeModel = new NumberRangeModel(0, max, 0, max);
				m_yaxis.setRangeModel(yRangeModel);
			}
		};
		
		// y-axis labels
		AxisLabelLayout ylabels = new AxisLabelLayout(m_ylabelID, m_yaxis, m_yLabelBounds, 10);
		// y-axis is a count so it will always be an integer
		NumberFormat format = ylabels.getNumberFormat();
		format.setParseIntegerOnly(true);
		
		ActionList drawAxes = new ActionList();
		drawAxes.add(yRange);
		drawAxes.add(m_yaxis);
		drawAxes.add(xaxis);
		drawAxes.add(ylabels);
		drawAxes.add(xlabels);
		drawAxes.add(new RepaintAction());
		m_vis.putAction(m_drawAxesAction, drawAxes);
	}
	
	public void setColors(int highlight, int[] primaryPalette, int[] secondaryPalette) {
		
		m_vis.removeAction(m_drawAction);
		
		String colorColumn = m_frequency.getBinColumn();
		DataColorAction primaryColor = new DataColorAction(m_groupID, colorColumn, Constants.ORDINAL, VisualItem.FILLCOLOR, primaryPalette);
		if (highlight != -1) {
			primaryColor.add(VisualItem.HIGHLIGHT, highlight);
		}
		
		DataColorAction secondaryColor = null;
		if (secondaryPalette != null) {
			Expression left = new ColumnExpression(m_frequency.getTypeColumn());
			Expression right = Literal.getLiteral(FrequencyTable.SECONDARY);
			Predicate predicate = new ComparisonPredicate(ComparisonPredicate.EQ,left, right);
			secondaryColor = new DataColorAction(m_groupID, colorColumn, Constants.ORDINAL, VisualItem.FILLCOLOR, secondaryPalette);
			secondaryColor.setFilterPredicate(predicate);
		}
		
		ActionList draw = new ActionList();
		draw.add(primaryColor);
		if (secondaryColor != null) {
			draw.add(secondaryColor);
		}
		draw.add(new RepaintAction());
		m_vis.putAction(m_drawAction, draw);
	}
	
	private void initRenderer() {

		m_vis.setRendererFactory(new RendererFactory() {
			Renderer yAxisRenderer = new AxisRenderer(Constants.LEFT, Constants.TOP);
			Renderer xAxisRenderer = new AxisRenderer(Constants.CENTER, Constants.FAR_BOTTOM);
			
			
			public Renderer getRenderer(VisualItem item) {
				if(item.isInGroup(m_ylabelID)) {
					return yAxisRenderer;
				}
				if(item.isInGroup(m_xlabelID)) {
					return xAxisRenderer;
				}	
				m_barRenderer.setBounds(m_plotBounds);
				return m_barRenderer;
			}
		});
	}
	
	private void initMouseOver() {
		addControlListener(new ControlAdapter(){
	        
	        @Override 
	        public void itemEntered(VisualItem item, MouseEvent evt) {
	        	if (item.isInGroup(m_groupID)) {
		        	item.setStrokeColor(ColorLib.rgb(0,0,0));
		        	m_vis.repaint();
	        	}
	        }
	        @Override
	        public void itemExited(VisualItem item, MouseEvent evt) {
	        	if (item.isInGroup(m_groupID)) {
		        	item.setStrokeColor(item.getEndStrokeColor());
		        	m_vis.repaint();
	        	}
	        }			
		});
	}
}
