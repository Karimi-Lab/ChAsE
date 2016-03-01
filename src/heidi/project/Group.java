package heidi.project;

import java.util.Vector;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

public class Group {

	private String       m_name;
	private Project      m_project;
	private Vector<Dim>  m_dims;
	private Vector<Plot> m_plots;
	
	private int[]        m_palette;
	private Dim          m_colorDimension;
	private int          m_highlightRGB = -1;
	private int[]        m_shapes;
	private Dim          m_shapeDimension;
	
	        static final String GROUP_STR = "group";
	private static final String NAME_STR  = "name";
	private static final String DIMS_STR  = "dimensions";
	private static final String DIM_STR   = "dimension";
	private static final String PLOTS_STR = "plots";
	
	public static Group CreatFromDom(Project project, Element groupElement) {
		
		NodeList nameElements = groupElement.getElementsByTagName(NAME_STR);
		String name = DomUtil.GetNodeText(nameElements.item(0));

		Group group = new Group(name, project);
		
		// Get dimensions for group
		NodeList columnsElements = groupElement.getElementsByTagName(DIMS_STR);
		if (columnsElements != null && columnsElements.getLength() > 0) {
			Element columnsElement = (Element)(columnsElements.item(0));
			NodeList columnElements = columnsElement.getElementsByTagName(DIM_STR);
			Dim[] dims = new Dim[columnElements.getLength()];
			for (int i = 0; i < columnElements.getLength(); i++) {
				String column = DomUtil.GetNodeText(columnElements.item(i));
				Dim dim = project.getDim(column);
				dims[i] = dim;
			}
			group.setDims(dims);
		}
		
		// Get plots for group
		NodeList plotsElements = groupElement.getElementsByTagName(PLOTS_STR);
		if (plotsElements != null && plotsElements.getLength() > 0) {
			Element plotsElement = (Element)(plotsElements.item(0));
			NodeList plotElements = plotsElement.getElementsByTagName(Plot.PLOT_STR);
			Plot[] plots = new Plot[plotElements.getLength()];
			for (int i = 0; i < plotElements.getLength(); i++) {
				Element plotElement = (Element)(plotElements.item(i));
				plots[i] = Plot.CreateFromDom(group, plotElement);
			}
			group.setPlots(plots);
		}
		
		// TODO load visual effects
		
		return group;
	}
	
	static Group CreateAllGroup(Project project) {
		Group group = new Group("All", project);
		
		// init dimensions to include all dimensions
		Dim[] allDims = project.getDims();
		group.setDims(allDims);
		
		// Provide a default Splom plot for the All group
		// TODO - SPLOM is extremely slow and uses up a lot of memory
		// Disabling this for now
//		Plot plot = new Plot("Splom for All", Splom.getType(), group);
//		plot.setDimensions(allDims);
//		plot.setColorDimension(allDims[0]);
//		plot.setShapeDimension(allDims[0]);
//		group.setPlots(new Plot[]{plot});
		return group;
	}
	
	public Group(String name, Project project) {
		m_name = name;
		m_project = project;
	}

	public Dim getColorDimension() {
		if (m_colorDimension != null) {
			return m_colorDimension;
		}
		return m_project.getColorDimension();
	}
	
	public void setColorDimension(Dim dim) {
		m_colorDimension = dim;
	}

	public int getHighlightRGB() {
		if (m_highlightRGB != -1) {
			return m_highlightRGB;
		}
		return m_project.getHighlightRGB();
	}
	
	public void setHighlight(int highlight) {
		m_highlightRGB = highlight;
	}
	
	public String getName() {
		return m_name;
	}

	public void setName(String name) {
		m_name = name;
	}
	
	public Project getProject() {
		return m_project;
	}
	
	public void addDim(Dim dim) {
		if (m_dims == null) {
			m_dims = new Vector<Dim>();
		}
		m_dims.add(dim);
	}
	
	public void removeDim(Dim dim) {
		if (m_dims != null) {
			m_dims.remove(dim);
		}
	}
	
	public Dim[] getDims() {
		if (m_dims != null) {
			Dim[] dims = new Dim[m_dims.size()];
			dims = m_dims.toArray(dims);
			return dims;
		}
		return null;
	}
	
	public void setDims(Dim[] dims) {
		if (m_dims == null) {
			m_dims = new Vector<Dim>();
		}
		m_dims.clear();
		for (Dim dim : dims) {
			m_dims.add(dim);
		}
	}
	
	public Dim getDim(String columnName) {
		return m_project.getDim(columnName);
	}
	
	public int getDimCount() {
		if (m_dims != null) {
			return m_dims.size();
		}
		return 0;
	}
		
	public void addPlot(Plot plot) {
		if (m_plots == null) {
			m_plots = new Vector<Plot>();
		}
		m_plots.add(plot);
	}
	
	public void removePlot(Plot plot) {
		if (m_plots != null) {
			m_plots.remove(plot);
		}
	}
	
	public Plot[] getPlots() {
		if (m_plots != null) {
			Plot[] plots = new Plot[m_plots.size()];
			plots = m_plots.toArray(plots);
			return plots;
		}
		return null;
	}
	
	public void setPlots(Plot[] plots) {
		if (m_plots == null) {
			m_plots = new Vector<Plot>();
		}
		m_plots.clear();
		for (Plot plot : plots) {
			m_plots.add(plot);
		}
	}
	
	public int[] getPalette() {
		if (m_palette != null) {
			return m_palette.clone();
		}
		return m_project.getPalette();
	}

	public void setPalette(int[] palette) {
		m_palette = palette;
	}

	public Dim getShapeDimension() {
		if (m_shapeDimension != null) {
			return m_shapeDimension;
		}
		return m_project.getShapeDimension();
	}
	
	public void setShapeDimension(Dim dim) {
		m_shapeDimension = dim;
	}
	
	public int[] getShapes() {
		if (m_shapes != null) {
			return m_shapes.clone();
		}
		return m_project.getShapes();
	}

	public void setShapes(int[] shapes) {
		m_shapes = shapes;
	}
	
	public void save(Document dom, Element groupsElement) {
		
		Element groupElement = dom.createElement(GROUP_STR);
		groupsElement.appendChild(groupElement);
		
		// Save group  name
		if (m_name != null) {
			Element nameElement = dom.createElement(NAME_STR);
			groupElement.appendChild(nameElement);
			Text nameText = dom.createTextNode(m_name);
			nameElement.appendChild(nameText);
		}
		
		// Save dimensions
		if (m_dims != null) {
			Element dimsElement = dom.createElement(DIMS_STR);
			groupElement.appendChild(dimsElement);
			for (Dim dim : m_dims) {
				Element dimElement = dom.createElement(DIM_STR);
				dimsElement.appendChild(dimElement);
				Text columnText = dom.createTextNode(dim.getColumn());
				dimElement.appendChild(columnText);
			}
		}
		
		// Save plots
		if (m_plots != null) {
			Element plotsElement = dom.createElement(PLOTS_STR);
			groupElement.appendChild(plotsElement);
			for (Plot plot : m_plots) {
				plot.save(dom, plotsElement);
			}
		}
		
		// TODO save visual effects

	}
}
