package heidi.plot;

import heidi.action.HighlightAction;
import heidi.project.Dim;
import heidi.project.Group;
import heidi.project.Plot;
import heidi.project.Project;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

import prefuse.Constants;
import prefuse.Display;
import prefuse.Visualization;
import prefuse.action.ActionList;
import prefuse.action.RepaintAction;
import prefuse.action.assignment.DataColorAction;
import prefuse.action.assignment.DataShapeAction;
import prefuse.action.filter.VisibilityFilter;
import prefuse.action.layout.AxisLayout;
import prefuse.controls.ControlAdapter;
import prefuse.controls.ToolTipControl;
import prefuse.data.Schema;
import prefuse.data.Table;
import prefuse.data.column.ColumnMetadata;
import prefuse.data.expression.CompositePredicate;
import prefuse.render.DefaultRendererFactory;
import prefuse.render.ShapeRenderer;
import prefuse.util.ColorLib;
import prefuse.util.UpdateListener;
import prefuse.visual.VisualItem;
import prefuse.visual.VisualTable;
import prefuse.visual.expression.InGroupPredicate;
import prefuse.visual.expression.VisiblePredicate;

public class Splom extends JPanel implements IDynamicUpdate {

	private Visualization m_vis;
	private Plot   m_plot;
	
	private String m_groupID;
	private String m_drawAction;
	private String m_drawAxesAction;
	private String m_visibilityAction;
	private String m_highlightAction;
	
	private static int s_index = 0;

	private static final String GROUPID    = "splom";
	private static final String DRAW       = "draw";
	private static final String DRAWAXES   = "drawaxes";
	private static final String VISIBILITY = "visibility";
	private static final String HIGHLIGHT  = "highlight";
	
	private static final int PLOTSIZE = 100;

	private static final long serialVersionUID = 2669641766031054892L;

	public static String getType() {
		return "Scatter Plot Matrix";
	}
	
	public Splom(Plot plot) {
		super();

		m_vis = new Visualization();
		m_plot = plot;

		Group group = m_plot.getGroup();
		Project project = group.getProject();
		Table table = project.getTable();
		
		m_groupID          = GROUPID + (s_index++);
		m_drawAction       = m_groupID + DRAW;
		m_drawAxesAction   = m_groupID + DRAWAXES;
		m_visibilityAction = m_groupID + VISIBILITY;
		m_highlightAction  = m_groupID + HIGHLIGHT;
		
		VisualTable visTable = m_vis.addTable(m_groupID, table);
		// BUG - VisualTable does not inherit comparator from Table 
		// FIX - Need to set comparator for each VisualTable but this should not be required
		int count = table.getColumnCount();
		for (int i = 0; i < count; i++) {
			String column = table.getColumnName(i);
			ColumnMetadata metaTable = table.getMetadata(column);
			ColumnMetadata metaVTable = visTable.getMetadata(column);
			metaVTable.setComparator(metaTable.getComparator());			
		}

		initComponent();
		initAxes();
		initRenderer();
		initUI();
	}

	private String getSubgroupID(String columnX, String columnY) {
		return m_groupID + ":" + columnX + ":" + columnY;
	}
	
	private void initComponent() {
		
		// highlight item under mouse
		ControlAdapter hover = new ControlAdapter() {
			public void itemEntered(VisualItem item, MouseEvent evt) {
				item.setFillColor(m_plot.getHighlightRGB()); 
				item.setSize(1.5);
				m_vis.repaint();
			}
			public void itemExited(VisualItem item, MouseEvent evt) {
				item.setFillColor(item.getEndFillColor());
				item.setSize(item.getEndSize());
				m_vis.repaint();
			}
		};
        
		Dim[] dims = m_plot.getDimensions();
		Group group = m_plot.getGroup();
		Project project = group.getProject();
		Table table = project.getTable();
		
		for (int x = 0; x < dims.length-1; x++) {
			for (int y = dims.length-1; y > x; y--) {
				
				String xColumn = dims[x].getColumn();
				String yColumn = dims[y].getColumn();
				
				// dynamically generate a group name for each display
				String subGroupID = getSubgroupID(xColumn, yColumn);				

				// add derived group, which only override data "position" attributes
				VisualTable derived = m_vis.addDerivedTable(subGroupID, m_groupID, null, getSchema());

				// BUG - VisualTable does not inherit comparator from Table 
				// FIX - Need to set comparator for each VisualTable but this should not be required
				int count = table.getColumnCount();
				for (int i = 0; i < count; i++) {
					String column = table.getColumnName(i);
					ColumnMetadata metaTable = table.getMetadata(column);
					ColumnMetadata metaDTable = derived.getMetadata(column);
					metaDTable.setComparator(metaTable.getComparator());			
				}

				// dynamically generate a display
				Display d = new Display(m_vis, new InGroupPredicate(subGroupID));
				d.setPreferredSize(new Dimension(PLOTSIZE,PLOTSIZE));
				d.setMinimumSize(new Dimension(PLOTSIZE,PLOTSIZE));
				d.setBorder(BorderFactory.createCompoundBorder(
							BorderFactory.createEtchedBorder(), 
							BorderFactory.createEmptyBorder(10, 10, 10, 10)));
				d.setHighQuality(true);
				d.addComponentListener(new ComponentAdapter() {
					public void componentResized(ComponentEvent e) {
						m_vis.run(m_drawAction);
						m_vis.run(m_drawAxesAction);
					}
				});
				d.addControlListener(new ToolTipControl(new String[] {xColumn, yColumn}));
				d.addControlListener(hover);
			}
		}
	}

	private void initAxes() {
		Dim[] dims = m_plot.getDimensions();
		ActionList drawAxes = new ActionList();
		for (int x=0; x<dims.length-1; x++) {
			for (int y=dims.length-1; y>x; y--) {
				String xColumn = dims[x].getColumn();
				String yColumn = dims[y].getColumn();
				
				String subGroupID = getSubgroupID(xColumn, yColumn);
				AxisLayout xaxis = new AxisLayout(subGroupID, xColumn, Constants.X_AXIS, VisiblePredicate.TRUE);				
				AxisLayout yaxis = new AxisLayout(subGroupID, yColumn, Constants.Y_AXIS, VisiblePredicate.TRUE);
				
				drawAxes.add(yaxis);				
				drawAxes.add(xaxis);
			}
		}
		drawAxes.add(new RepaintAction());
		m_vis.putAction(m_drawAxesAction, drawAxes);
	}

	private void initRenderer() {
		
		Dim colorDim = m_plot.getColorDimension();
		String colorColumn = colorDim.getColumn();
		int colorType = colorDim.getType();
		
		DataColorAction fillColor = new DataColorAction(m_groupID, colorColumn, colorType, VisualItem.FILLCOLOR, m_plot.getPalette());
		fillColor.add(VisualItem.HIGHLIGHT, m_plot.getHighlightRGB()); // highlight selected nodes

		int[] strokePalette = new int[] {ColorLib.getColor(50, 50, 50).getRGB()};
		DataColorAction strokeColor = new DataColorAction(m_groupID, colorColumn, colorType, VisualItem.STROKECOLOR, strokePalette);
		
		String shapeColumn = m_plot.getShapeDimension().getColumn();
		DataShapeAction shape = new DataShapeAction(m_groupID, shapeColumn, m_plot.getShapes());
		
		ActionList draw = new ActionList();
		draw.add(fillColor);
		draw.add(strokeColor);
		draw.add(shape);
		draw.add(new RepaintAction());
		m_vis.putAction(m_drawAction, draw);

		m_vis.setRendererFactory(new DefaultRendererFactory(new ShapeRenderer(4)));
	}

	private void initUI() {
		Dim[] dims = m_plot.getDimensions();
		
		// Configure Layout
		setLayout(new GridBagLayout());
		
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.ipadx = constraints.ipady = 5;
		int y = 1;
		// label for each row
		for (int i=dims.length-1; i>0; i--) {
			JLabel yLabel = new JLabel(dims[i].getName());
			constraints.gridx = 0;
			constraints.gridy = y;
			constraints.anchor = GridBagConstraints.CENTER;
			yLabel.setMinimumSize(new Dimension(PLOTSIZE - 5, 50));
			add(yLabel, constraints);		
			y++;
		}

		// Create Label for each column
		int displayIndex = 0;
		for (int i=0; i<dims.length-1; i++) {
			JLabel xLabel = new JLabel(dims[i].getName());
			constraints.gridx = i + 1;
			constraints.gridy = 0;
			constraints.anchor = GridBagConstraints.CENTER;
			xLabel.setMinimumSize(new Dimension(PLOTSIZE - 5, 50));
			add(xLabel, constraints);

			y = 0;
			// Create vertical column of scatter plots
			for (int j=dims.length-1; j>0; j--) {
				if (j>i) {
					Display display = m_vis.getDisplay(displayIndex++);
					constraints.gridx = i + 1;
					constraints.gridy = y + 1;
					constraints.anchor = GridBagConstraints.CENTER;
					add(display, constraints);
					y++;
				} 				
			}
		}
		
		// add spacer to place Splom in top left corner
		JLabel label = new JLabel();
		constraints = new GridBagConstraints();
		constraints.gridx = dims.length + 1;
		constraints.gridy = dims.length + 1;
		constraints.gridheight = 0;
		constraints.weightx = 1.0;
		constraints.weighty = 1.0;
		add(label, constraints);
	}
	
	private Schema getSchema() {
		Schema s = new Schema();

		// only some attributes should be independent for each display. 

		// booleans
		s.addColumn(VisualItem.VALIDATED, boolean.class, Boolean.FALSE);
		//s.addColumn(VisualItem.VISIBLE, boolean.class, Boolean.TRUE);
		//s.addColumn(VisualItem.STARTVISIBLE, boolean.class, Boolean.FALSE);
		//s.addColumn(VisualItem.ENDVISIBLE, boolean.class, Boolean.TRUE);
		//s.addColumn(VisualItem.INTERACTIVE, boolean.class, Boolean.TRUE);
		//s.addColumn(VisualItem.EXPANDED, boolean.class, Boolean.TRUE);
		//s.addColumn(VisualItem.FIXED, boolean.class, Boolean.FALSE);
		s.addColumn(VisualItem.HIGHLIGHT, boolean.class, Boolean.FALSE);
		//s.addColumn(VisualItem.HOVER, boolean.class, Boolean.FALSE);

		s.addInterpolatedColumn(VisualItem.X, double.class);
		s.addInterpolatedColumn(VisualItem.Y, double.class);

		// bounding box
		s.addColumn(VisualItem.BOUNDS, Rectangle2D.class, new Rectangle2D.Double());

		// color
		//Integer defStroke = new Integer(ColorLib.rgba(0,0,0,0));
		//s.addInterpolatedColumn(VisualItem.STROKECOLOR, int.class, defStroke);

		//Integer defFill = new Integer(ColorLib.rgba(0,0,0,0));
		//s.addInterpolatedColumn(VisualItem.FILLCOLOR, int.class, defFill);

		//Integer defTextColor = new Integer(ColorLib.rgba(0,0,0,0));
		//s.addInterpolatedColumn(VisualItem.TEXTCOLOR, int.class, defTextColor);

		// size
		//s.addInterpolatedColumn(VisualItem.SIZE, double.class, new Double(1));

		// shape
		//s.addColumn(VisualItem.SHAPE, int.class, new Integer(Constants.SHAPE_RECTANGLE));

		// stroke
		//s.addColumn(VisualItem.STROKE, Stroke.class, new BasicStroke());

		// font
		//Font defFont = FontLib.getFont("SansSerif",Font.PLAIN,10);
		//s.addInterpolatedColumn(VisualItem.FONT, Font.class, defFont);

		// degree-of-interest
		//s.addColumn(VisualItem.DOI, double.class, new Double(Double.MIN_VALUE));

		return s;
	}
	
	@Override
	public Dimension preferredSize() {
		// TODO - determine preferred size from number of plots
		int count = 1;
		if (m_vis != null) {
			int c = m_vis.getDisplayCount();
			do {
				c = c - count;
				count++;
			} while (c - count > 0);
		}
		int labelWidth = 100;
		int labelHeight = 50;
		return new Dimension(count*PLOTSIZE + labelWidth, count*PLOTSIZE + labelHeight);
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
