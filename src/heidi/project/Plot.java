package heidi.project;

import heidi.plot.ScatterPlot;
import heidi.plot.Splom;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

public class Plot {
	
	private String m_name;
	private String m_type;
	private Group  m_group;
	
	private Dim   m_xDimension;
	private Dim   m_yDimension;
	private Dim[] m_dimensions;
	
	private int[] m_palette;
	private Dim   m_colorDimension;
	private int   m_highlightRGB = -1;
	private int[] m_shapes;
	private Dim   m_shapeDimension;
	
	        static final String PLOT_STR     = "plot";
	private static final String NAME_STR     = "name";
	private static final String TYPE_STR     = "plotType";
	private static final String XDIM_STR     = "xDim";
	private static final String YDIM_STR     = "yDim";
	private static final String DIMS_STR     = "dimensions";
	private static final String DIM_STR      = "dimension";
	private static final String COLORDIM_STR = "colorDim";
	private static final String SHAPEDIM_STR = "shapeDim";
	
	public static boolean getMultipleDimensions(String type) {
		if (ScatterPlot.getType().equals(type)) {
			return false;
		}
		if (Splom.getType().equals(type)) {
			return true;
		}
		return false;
	}
	
	public static Plot CreateFromDom(Group group, Element plotElement) {
		
		NodeList nameElements = plotElement.getElementsByTagName(NAME_STR);
		String name = DomUtil.GetNodeText(nameElements.item(0));
		
		NodeList typeElements = plotElement.getElementsByTagName(TYPE_STR);
		String type = DomUtil.GetNodeText(typeElements.item(0));
		
		Plot plot = new Plot(name, type, group);
		
		NodeList xDimElements = plotElement.getElementsByTagName(XDIM_STR);
		if (xDimElements != null && xDimElements.getLength() > 0) {
			String xColumn = DomUtil.GetNodeText(xDimElements.item(0));
			
			Dim xDim = group.getDim(xColumn);
			plot.setXDimension(xDim);
		}
		
		NodeList yDimElements = plotElement.getElementsByTagName(YDIM_STR);
		if (yDimElements != null && yDimElements.getLength() > 0) {
			String yColumn = DomUtil.GetNodeText(yDimElements.item(0));
			Dim yDim = group.getDim(yColumn);
			plot.setYDimension(yDim);
		}

		NodeList dimensionsElements = plotElement.getElementsByTagName(DIMS_STR);
		if (dimensionsElements != null && dimensionsElements.getLength() > 0) {
			Element dimensionsElement = (Element)(dimensionsElements.item(0));
			NodeList dimensionElements = dimensionsElement.getElementsByTagName(DIM_STR);
			Dim[] dims = new Dim[dimensionElements.getLength()];
			for (int j = 0; j < dimensionElements.getLength(); j++) {
				String column = DomUtil.GetNodeText(dimensionElements.item(j));
				Dim dim = group.getDim(column);
				dims[j] = dim;
			}
			plot.setDimensions(dims);
		}
		
		NodeList colorDimElements = plotElement.getElementsByTagName(COLORDIM_STR);
		if (colorDimElements != null && colorDimElements.getLength() > 0) {
			String colorColumn = DomUtil.GetNodeText(colorDimElements.item(0));
			Dim colorDim = group.getDim(colorColumn);
			plot.setColorDimension(colorDim);
		}
		
		NodeList shapeDimElements = plotElement.getElementsByTagName(SHAPEDIM_STR);
		if (shapeDimElements != null && shapeDimElements.getLength() > 0) {
			String shapeColumn = DomUtil.GetNodeText(shapeDimElements.item(0));
			Dim shapeDim = group.getDim(shapeColumn);
			plot.setShapeDimension(shapeDim);
		}
		
		return plot;
	}
	
	public Plot(String name, String type, Group group) {
		m_name = name;
		m_type = type;
		m_group = group;
	}
	
	public Dim getColorDimension() {
		if (m_colorDimension != null) {
			return m_colorDimension;
		}
		return m_group.getColorDimension();
	}
	
	public void setColorDimension(Dim color) {
		m_colorDimension = color;
	}
	
	public Dim[] getDimensions() {
		if (m_dimensions != null) {
			return m_dimensions.clone();
		}
		return new Dim[] {m_xDimension, m_yDimension};
	}
	
	public void setDimensions(Dim[] dimensions) {
		m_dimensions = dimensions;
	}

	public Group getGroup() {
		return m_group;
	}
	
	public void setGroup(Group group) {
		m_group = group;
	}

	public String getName() {
		return m_name;
	}
	
	public int getHighlightRGB() {
		if (m_highlightRGB != -1) {
			return m_highlightRGB;
		}
		return m_group.getHighlightRGB();
	}
	
	public void setHighlight(int highlight) {
		m_highlightRGB = highlight;
	}
	
	public void setName(String name) {
		m_name = name;
	}
	
	public int[] getPalette() {
		if (m_palette == null) {
			return m_group.getPalette();
		}
		return m_palette.clone();
	}
	
	public void setPalette(int[] palette) {
		m_palette = palette;
	}
	
	public Dim getShapeDimension() {
		if (m_shapeDimension != null) {
			return m_shapeDimension;
		}
		return m_group.getShapeDimension();
	}
	
	public void setShapeDimension(Dim shape) {
		m_shapeDimension = shape;
	}
	
	public int[] getShapes() {
		if (m_shapes == null) {
			return m_group.getShapes();
		}
		return m_shapes.clone();
	}
	
	public void setShapes(int[] shapes) {
		m_shapes = shapes;
	}

	public String getType() {
		return m_type;
	}
	
	public void setType(String type) {
		m_type = type;
	}
	
	public Dim getXDimension() {
		return m_xDimension;
	}
	
	public void setXDimension(Dim x) {
		m_xDimension = x;
	}
	
	public Dim getYDimension() {
		return m_yDimension;
	}
	
	public void setYDimension(Dim y) {
		m_yDimension = y;
	}
	
	public void save(Document dom, Element plotsElement) {

		Element plotElement = dom.createElement(PLOT_STR);
		plotsElement.appendChild(plotElement);
		
		// Save plot  name
		if (m_name != null) {
			Element nameElement = dom.createElement(NAME_STR);
			plotElement.appendChild(nameElement);
			Text nameText = dom.createTextNode(m_name);
			nameElement.appendChild(nameText);
		}
		
		// Save plot type
		Element typeElement = dom.createElement(TYPE_STR);
		plotElement.appendChild(typeElement);
		Text plotText = dom.createTextNode(m_type);
		typeElement.appendChild(plotText);
		
		// Save xDim
		if (m_xDimension != null) {
			Element xElement = dom.createElement(XDIM_STR);
			plotElement.appendChild(xElement);
			Text xText = dom.createTextNode(m_xDimension.getColumn());
			xElement.appendChild(xText);
		}
		
		// Save yDim
		if (m_yDimension != null) {
			Element yElement = dom.createElement(YDIM_STR);
			plotElement.appendChild(yElement);
			Text yText = dom.createTextNode(m_yDimension.getColumn());
			yElement.appendChild(yText);
		}
		
		// Save multiple dimensions
		if (m_dimensions != null) {
			Element dimsElement = dom.createElement(DIMS_STR);
			plotElement.appendChild(dimsElement);
			for (Dim dim : m_dimensions) {
				Element dimElement = dom.createElement(DIM_STR);
				dimsElement.appendChild(dimElement);
				Text columnText = dom.createTextNode(dim.getColumn());
				dimElement.appendChild(columnText);
			}
		}
		
		// Save Color dimension
		if (m_colorDimension != null) {
			Element colorElement = dom.createElement(COLORDIM_STR);
			plotElement.appendChild(colorElement);
			Text colorText = dom.createTextNode(m_colorDimension.getColumn());
			colorElement.appendChild(colorText);
		}
		
		// Save Shape dimension
		if (m_shapeDimension != null) {
			Element shapeElement = dom.createElement(SHAPEDIM_STR);
			plotElement.appendChild(shapeElement);
			Text shapeText = dom.createTextNode(m_shapeDimension.getColumn());
			shapeElement.appendChild(shapeText);
		}
	}
	
}
