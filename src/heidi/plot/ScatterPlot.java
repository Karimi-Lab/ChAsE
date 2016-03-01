package heidi.plot;

import heidi.action.HighlightAction;
import heidi.project.Dim;
import heidi.project.Group;
import heidi.project.Plot;
import heidi.project.Project;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.text.DecimalFormat;

import prefuse.Constants;
import prefuse.Display;
import prefuse.Visualization;
import prefuse.action.ActionList;
import prefuse.action.RepaintAction;
import prefuse.action.assignment.DataColorAction;
import prefuse.action.assignment.DataShapeAction;
import prefuse.action.assignment.StrokeAction;
import prefuse.action.filter.VisibilityFilter;
import prefuse.action.layout.AxisLabelLayout;
import prefuse.action.layout.AxisLayout;
import prefuse.controls.ControlAdapter;
import prefuse.controls.ToolTipControl;
import prefuse.data.Table;
import prefuse.data.column.ColumnMetadata;
import prefuse.data.expression.CompositePredicate;
import prefuse.render.AxisRenderer;
import prefuse.render.Renderer;
import prefuse.render.RendererFactory;
import prefuse.render.ShapeRenderer;
import prefuse.util.UpdateListener;
import prefuse.util.display.PaintListener;
import prefuse.visual.VisualItem;
import prefuse.visual.VisualTable;
import prefuse.visual.expression.VisiblePredicate;

public class ScatterPlot extends Display implements IDynamicUpdate {

	private Table  m_dataTable;
	private Plot   m_plot;
	
	private String m_groupID;
	private String m_xlabelID;
	private String m_ylabelID;

	private String m_drawAction;
	private String m_drawAxesAction;
	private String m_visibilityAction;
	private String m_highlightAction;

	// bounds for scatter plot
	private Rectangle2D m_plotBounds;
	private Rectangle2D m_xLabelBounds;
	private Rectangle2D m_yLabelBounds;
	
	private static int s_index = 0;

	private static final String GROUPID  = "scatterplot";
	private static final String DRAW     = "draw";
	private static final String DRAWAXES = "drawaxes";
	private static final String VISIBILITY = "visibility";
	private static final String HIGHLIGHT = "highlight";
	
	private static final String XLABEL   = "xlabel";
	private static final String YLABEL   = "ylabel";

	private static final long serialVersionUID = 2669641766031054892L;

	public static String getType() {
		return "Scatter Plot";
	}
	
	public ScatterPlot(Plot plot) {
		super(new Visualization());
		
		m_plot = plot;

		Group group = plot.getGroup();
		Project project = group.getProject();
		m_dataTable = project.getTable();
		
		m_groupID          = GROUPID + (s_index++);
		m_drawAction       = m_groupID + DRAW;
		m_drawAxesAction   = m_groupID + DRAWAXES;
		m_visibilityAction = m_groupID + VISIBILITY;
		m_highlightAction  = m_groupID + HIGHLIGHT;
		
		m_xlabelID         = m_groupID + XLABEL;
		m_ylabelID         = m_groupID + YLABEL;

		m_plotBounds   = new Rectangle2D.Double();
		m_xLabelBounds = new Rectangle2D.Double();
		m_yLabelBounds = new Rectangle2D.Double();

		VisualTable visTable = m_vis.addTable(m_groupID, m_dataTable);
		// BUG - VisualTable does not inherit comparator from Table 
		// FIX - Need to set comparator for each VisualTable but this should not be required
		int count = m_dataTable.getColumnCount();
		for (int i = 0; i < count; i++) {
			String column = m_dataTable.getColumnName(i);
			ColumnMetadata metaTable = m_dataTable.getMetadata(column);
			ColumnMetadata metaVTable = visTable.getMetadata(column);
			metaVTable.setComparator(metaTable.getComparator());			
		}

		initComponent();
		initAxes();
		initRenderer();
	}

	private void initComponent() {
		setHighQuality(true);
		
		// highlight item under mouse
		addControlListener(new ControlAdapter() {
			public void itemEntered(VisualItem item, MouseEvent evt) {
				if (item.isInGroup(m_groupID)) {
					item.setStrokeColor(m_plot.getHighlightRGB());
					item.setStroke(new BasicStroke(2));
					item.setSize(1.5);
					m_vis.repaint();
				}
			}
			public void itemExited(VisualItem item, MouseEvent evt) {
				if (item.isInGroup(m_groupID)) {
					item.setStrokeColor(item.getEndStrokeColor());
					item.setStroke(new BasicStroke(1));
					item.setSize(item.getEndSize());
					m_vis.repaint();
				}
			}
		});
		
		String xColumn = m_plot.getXDimension().getColumn();
		String yColumn = m_plot.getYDimension().getColumn();
		String colorColumn = m_plot.getColorDimension().getColumn();
		addControlListener(new ToolTipControl(new String[] {xColumn, yColumn, colorColumn}));
		
		// handle resizing
		addComponentListener(new ComponentAdapter() {
			public void componentResized(ComponentEvent e) {
				Insets insets = getInsets();
				insets.left += 20; insets.right += 10;
				insets.top += 10; insets.bottom += 15;
				int width = getWidth() - insets.left - insets.right;
				int height = getHeight() - insets.top - insets.bottom;
				int yLabelWidth = 100; // TODO determine from actual text widths
				int xLabelHeight = 20;

				m_plotBounds.setRect(insets.left, insets.top, width - yLabelWidth, height - xLabelHeight);
				m_xLabelBounds.setRect(insets.left, insets.top, width - yLabelWidth, height - xLabelHeight + 5);
				m_yLabelBounds.setRect(insets.left, insets.top, width - 15, height - xLabelHeight);
				m_vis.run(m_drawAction);
				m_vis.run(m_drawAxesAction);
			}
		});
	}

	private void initAxes() {
		
		Project project = m_plot.getGroup().getProject();
		String xColumn = m_plot.getXDimension().getColumn();
		String yColumn = m_plot.getYDimension().getColumn();
		Dim xDim = project.getDim(xColumn);
		Dim yDim = project.getDim(yColumn);
		
		//x-axis - "xColumn"
		AxisLayout xaxis = new AxisLayout(m_groupID, xColumn, Constants.X_AXIS, VisiblePredicate.TRUE);
		xaxis.setLayoutBounds(m_plotBounds);
		
		// x-axis labels
		AxisLabelLayout xlabels = new AxisLabelLayout(m_xlabelID, xaxis, m_xLabelBounds, 20);
		if (xDim.getType() == Constants.NUMERICAL) {
			// extra width for numbers
			xlabels.setSpacing(40);
			DecimalFormat numberFormat = new DecimalFormat("0.00E0");
			xlabels.setNumberFormat(numberFormat);
		}

		//y-axis - "yColumn"
		AxisLayout yaxis = new AxisLayout(m_groupID, yColumn, Constants.Y_AXIS, VisiblePredicate.TRUE);
		yaxis.setLayoutBounds(m_plotBounds);
		
		// y-axis labels
		AxisLabelLayout ylabels = new AxisLabelLayout(m_ylabelID, yaxis, m_yLabelBounds, 20);
		if (yDim.getType() == Constants.NUMERICAL) {
			ylabels.setSpacing(40);
			DecimalFormat numberFormat = new DecimalFormat("0.00E0");
			ylabels.setNumberFormat(numberFormat);
		}
		
		ActionList drawAxes = new ActionList();
		drawAxes.add(yaxis);
		drawAxes.add(xaxis);
		drawAxes.add(ylabels);
		drawAxes.add(xlabels);
		drawAxes.add(new RepaintAction());
		m_vis.putAction(m_drawAxesAction, drawAxes);
		
		// add paint listener to draw title for x and y axes
		addPaintListener(new PaintListener() {
			@Override
			public void prePaint(Display d, Graphics2D g) {
			}
			@Override
			public void postPaint(Display d, Graphics2D g) {
				g.setColor(Color.gray);
				g.setFont(d.getFont());
				Rectangle bounds = d.getBounds();
				int stringLength = 50;
				g.drawString(m_plot.getXDimension().getName(), bounds.x + bounds.width/2 - stringLength, bounds.y + bounds.height - 5);
				g.rotate(Math.PI/2.0f);
				g.drawString(m_plot.getYDimension().getName(), bounds.y + bounds.height/2 - stringLength, -1 * (bounds.x + bounds.width - 10));
			}
		});
	}
	
	private void initRenderer() { 
		Dim colorDim = m_plot.getColorDimension();
		String colorColumn = colorDim.getColumn();
		int colorType = colorDim.getType();
		
		DataColorAction fillColor = new DataColorAction(m_groupID, colorColumn, colorType, VisualItem.FILLCOLOR, m_plot.getPalette());
		
		int[] strokePalette = new int[] {Color.black.getRGB()};
		DataColorAction strokeColor = new DataColorAction(m_groupID, colorColumn, colorType, VisualItem.STROKECOLOR, strokePalette);
		int highlight = m_plot.getHighlightRGB();
		strokeColor.add(VisualItem.HIGHLIGHT, highlight);
		
		StrokeAction stroke = new StrokeAction(m_groupID, new BasicStroke(1));
		stroke.add(VisualItem.HIGHLIGHT, new BasicStroke(2));

		String shapeColumn = m_plot.getShapeDimension().getColumn();
		DataShapeAction shape = new DataShapeAction(m_groupID, shapeColumn, m_plot.getShapes());
		
		ActionList draw = new ActionList();
		draw.add(fillColor);
		draw.add(strokeColor);
		draw.add(stroke);
		draw.add(shape);
		draw.add(new RepaintAction());
		m_vis.putAction(m_drawAction, draw);

		m_vis.setRendererFactory(new RendererFactory() {
			ShapeRenderer shapeRenderer = new ShapeRenderer(10);
			Renderer yAxisRenderer = new AxisRenderer(Constants.RIGHT, Constants.TOP);
			Renderer xAxisRenderer = new AxisRenderer(Constants.CENTER, Constants.FAR_BOTTOM);

			public Renderer getRenderer(VisualItem item) {
				return item.isInGroup(m_ylabelID) ? yAxisRenderer :
					item.isInGroup(m_xlabelID) ? xAxisRenderer : shapeRenderer;
			}
		});
	}

	@Override
	public Dimension preferredSize() {
		// TODO - determine preferred size from data size
		return new Dimension(150, 150);
	}
	
	public void setFilterPredicate(CompositePredicate predicate) {
		
		predicate.addExpressionListener(new UpdateListener() {
			public void update(Object src) {
				updateFilter();
			}
		});

		// visibility filter action
		VisibilityFilter visibility = new VisibilityFilter(m_groupID, predicate);

		ActionList updateAction = new ActionList();
		updateAction.add(visibility);
		m_vis.putAction(m_visibilityAction, updateAction);
		
		updateFilter();
	}
	
	private void updateFilter() {
		m_vis.run(m_visibilityAction);
		m_vis.run(m_drawAction);
		m_vis.run(m_drawAxesAction);
	}
	
	public void setHighlightPredicate(CompositePredicate predicate) {
		
		predicate.addExpressionListener(new UpdateListener() {
			public void update(Object src) {
				updateHighlight();
			}
		});

		// Highlight action
		HighlightAction highlight = new HighlightAction(m_groupID, predicate);

		ActionList updateAction = new ActionList();
		updateAction.add(highlight);
		m_vis.putAction(m_highlightAction, updateAction);
		
		updateHighlight();
	}
	
	private void updateHighlight() {
		m_vis.run(m_highlightAction);
		m_vis.run(m_drawAction);
		m_vis.run(m_drawAxesAction);
	}
}
