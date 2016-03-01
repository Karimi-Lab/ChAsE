package heidi.project;

import heidi.data.query.FrequencyQueryBinding;
import heidi.dimreduction.JamaPCAnalyzer;
import heidi.dimreduction.PrincipalComponent;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import prefuse.Constants;
import prefuse.data.Table;
import prefuse.data.expression.AndPredicate;
import prefuse.data.expression.BooleanLiteral;
import prefuse.data.expression.CompositePredicate;
import prefuse.data.expression.OrPredicate;
import prefuse.data.io.CSVTableReader;
import prefuse.data.io.DataIOException;
import prefuse.data.io.DelimitedTextTableReader;
import prefuse.util.ColorLib;
import prefuse.util.io.IOLib;

public class Project {
	
	private String        m_name;
	private Table         m_rawdata;
	private Vector<Group> m_groups;
	private HashMap<String,Dim> m_dims;
	
	private AndPredicate m_dynamicFilter;
	private OrPredicate  m_dynamicHighlight;
	
	private int[]        m_palette;
	private Dim          m_colorDimension;
	private int          m_highlightRGB = ColorLib.getColor(255, 255, 0).getRGB();
	private int[]        m_shapes;
	private Dim          m_shapeDimension;

	private File         m_projectFile;
	private File         m_dataFile;
	
	private static final String PROJECT_STR = "project";
	private static final String NAME_STR    = "name";
	private static final String DATA_STR    = "data";
	private static final String SRC_STR     = "src";
	private static final String DIMS_STR    = "dims";
	private static final String PCS_STR     = "pcs";
	private static final String GROUPS_STR  = "groups";
	private static final String VISUAL_STR  = "visual";
	private static final String COLORDIM_STR  = "colorDim";
	private static final String PALETTE_STR = "palette";
	private static final String RGB_STR     = "RGB";
	private static final String HIGHLIGHTS_STR = "highlights";
	private static final String HIGHLIGHT_STR  = "highlight";
	private static final String FILTERS_STR    = "filters";
	private static final String FILTER_STR     = "filter";
	
	public static Project CreateFromDom(File file, Element rootElement) throws DataIOException {
		
		NodeList projectElements = rootElement.getElementsByTagName(PROJECT_STR);
		Element projectElement = (Element)(projectElements.item(0));
		
		Project project = new Project();
		project.setFile(file);
		
		// Set project name
		NodeList nameElements = projectElement.getElementsByTagName(NAME_STR);
		if (nameElements != null && nameElements.getLength() > 0) {
			String name = DomUtil.GetNodeText(nameElements.item(0));
			project.setName(name);
		}
		
		// Initialize data file
		NodeList dataElements = projectElement.getElementsByTagName(DATA_STR);
		if (dataElements != null & dataElements.getLength() > 0) {
			Element dataElement = (Element)(dataElements.item(0));
			NodeList srcElements = dataElement.getElementsByTagName(SRC_STR);
			if (srcElements != null && srcElements.getLength() > 0) {
				String dataFile = DomUtil.GetNodeText(srcElements.item(0));
				try {
					project.setDataFile(dataFile);
				} catch (IOException ioe) {
					throw new DataIOException(ioe.getCause());
				}
			}
		}
		
		// Initialize dimensions from project file
		// Note: these dimensions already exist and we are adding information
		NodeList dimsElements = projectElement.getElementsByTagName(DIMS_STR);
		if (dimsElements != null && dimsElements.getLength() > 0) {
			Element dimsElement = (Element)(dimsElements.item(0));
			NodeList dimElements = dimsElement.getElementsByTagName(DataFileDim.DIM_STR);
			for (int i = 0; i < dimElements.getLength(); i++) {
				Element dimElement = (Element)dimElements.item(i);
				DataFileDim.UpdateFromDom(dimElement, project);
			}
		}
		
		// Initialize principal components from project file
		NodeList pcsElements = projectElement.getElementsByTagName(PCS_STR);
		if (pcsElements != null && pcsElements.getLength() > 0) {
			Element pcsElement = (Element)(pcsElements.item(0));
			NodeList pcElements = pcsElement.getElementsByTagName(PrincipalComponent.PC_STR);
			
			// Perform Principal Component Analysis
			JamaPCAnalyzer pcAnalyzer = new JamaPCAnalyzer(project);
			for (int i = 0; i < pcElements.getLength(); i++) {
				Element pcElement = (Element)pcElements.item(i);
				PrincipalComponent dim = PrincipalComponent.CreateFromDom(pcElement, project, pcAnalyzer);
				dim.addToTable();
				project.addDim(dim);
			}
		}
		
		// Load highlight and filter information
		NodeList filtersElements = projectElement.getElementsByTagName(FILTERS_STR);
		if (filtersElements != null && filtersElements.getLength() > 0) {
			Element filtersElement = (Element)(filtersElements.item(0));
			NodeList filterElements = filtersElement.getElementsByTagName(FILTER_STR);
			for (int i = 0; i < filterElements.getLength(); i++) {
				Element filterElement = (Element)filterElements.item(i);
				String column = filterElement.getAttribute(Dim.COLUMN_STR);
				String filter = DomUtil.GetNodeText(filterElement);
				
				Dim dim = project.getDim(column);
				dim.setFilter(filter);
			}
		}
			
		NodeList highlightsElements = projectElement.getElementsByTagName(HIGHLIGHTS_STR);
		if (highlightsElements != null && highlightsElements.getLength() > 0) {
			Element highlightsElement = (Element)(highlightsElements.item(0));
			NodeList highlightElements = highlightsElement.getElementsByTagName(HIGHLIGHT_STR);
			for (int i = 0; i < highlightElements.getLength(); i++) {
				Element highlightElement = (Element)highlightElements.item(i);
				String column = highlightElement.getAttribute(Dim.COLUMN_STR);
				String highlight = DomUtil.GetNodeText(highlightElement);
				
				Dim dim = project.getDim(column);
				dim.setHighlight(highlight);
			}
		}
		
		project.updateProject();
		
		// Get visual effects from project file
		NodeList visualElements = projectElement.getElementsByTagName(VISUAL_STR);
		if (visualElements != null && visualElements.getLength() > 0) {
			Element visualElement = (Element)(visualElements.item(0));
			
			// highlight color
			NodeList highlightElements = visualElement.getElementsByTagName(HIGHLIGHT_STR);
			if (highlightElements != null && highlightElements.getLength() > 0) {
				Element highlightElement = (Element)highlightElements.item(0);
				NodeList rgbElements = highlightElement.getElementsByTagName(RGB_STR);
				Element rgbElement = (Element)rgbElements.item(0);
				String rgbString = DomUtil.GetNodeText(rgbElement);
				int rgb = Integer.parseInt(rgbString);
				project.setHighlight(rgb);
			}
			
			// color dimension
			NodeList colorDimElements = visualElement.getElementsByTagName(COLORDIM_STR);
			if (colorDimElements != null && colorDimElements.getLength() > 0) {
				Element colorDimElement = (Element)colorDimElements.item(0);
				String column = DomUtil.GetNodeText(colorDimElement);
				Dim dim = project.getDim(column);
				if (dim != null) {
					project.setColorDimension(dim);
				}
			}
		
			// palette
			NodeList paletteElements = visualElement.getElementsByTagName(PALETTE_STR);
			if (paletteElements != null & paletteElements.getLength() > 0) {
				Element paletteElement = (Element)paletteElements.item(0);
				NodeList rgbElements = paletteElement.getElementsByTagName(RGB_STR);
				int[] palette = new int[rgbElements.getLength()];
				for (int i = 0; i < rgbElements.getLength(); i++) {
					Element rgbElement = (Element)rgbElements.item(i);
					String rgbString = DomUtil.GetNodeText(rgbElement);
					int rgb = Integer.parseInt(rgbString);
					palette[i] = rgb;
				}
				project.setPalette(palette);
			}
		}
		
		// Initialize Groups
		
		NodeList groupsElements = projectElement.getElementsByTagName(GROUPS_STR);
		if (groupsElements != null && groupsElements.getLength() > 0) {
			Element groupsElement = (Element)(groupsElements.item(0));
			NodeList groupElements = groupsElement.getElementsByTagName(Group.GROUP_STR);
			for (int i = 0; i < groupElements.getLength(); i++) {
				Element groupElement = (Element)groupElements.item(i);
				Group group = Group.CreatFromDom(project, groupElement);
				project.addGroup(group);
			}
		}
		
		return project;
	}
	
	public Project() {
		// Create empty project
		
		m_rawdata = new Table();
		
		m_dynamicFilter = new AndPredicate();
		// By default, do not filter any items
		m_dynamicFilter.set(BooleanLiteral.TRUE);
		
		m_dynamicHighlight = new OrPredicate();
		// By default, do not highlight any items
		m_dynamicHighlight.clear();
		
	}
	
	public Dim getDim(String columnName) {
		if (m_dims != null) {
			return m_dims.get(columnName);
		}
		return null;
	}

	
	public CompositePredicate getFilterPredicate() {
		return m_dynamicFilter;
	}

	public CompositePredicate getHighlightPredicate() {
		return m_dynamicHighlight;
	}
	
	public Dim getDim(int index) {
		if (m_rawdata != null) {
			String columnName = m_rawdata.getColumnName(index);
			return getDim(columnName);
		}
		return null;
	}
	
	public int getDimCount() {
		if (m_dims != null) {
			return m_dims.size();
		}
		return 0;
	}
	
	public Dim[] getDims() {
		if (m_dims != null) {
			int count = m_rawdata.getColumnCount();
			Dim[] result = new Dim[count];
			for (int i = 0; i < count; i++) {
				String columnName = m_rawdata.getColumnName(i);
				result[i] = m_dims.get(columnName);
			}
			return result;
		}
		return null;
	}
	
	public void addDim(Dim dim) {
		if (m_dims == null) {
			m_dims = new HashMap<String, Dim>();
		}
		String key = dim.getColumn();
		m_dims.put(key, dim);
	}
	
	public void removeDim(Dim dim) {
		if (m_dims != null) {
			String key = dim.getColumn();
			if (m_dims.containsKey(key)) {
				m_dims.remove(key);
			}
		}
	}
	
	public Dim getColorDimension() {
		if (m_colorDimension != null) {
			return m_colorDimension;
		}
		if (m_dims != null) {
			Iterator<Dim> values = m_dims.values().iterator();
			return values.next();
		}
		return null;
	}
	
	public void setColorDimension(Dim dim) {
		m_colorDimension = dim;
	}
	
	public File getDataFile() {
		return m_dataFile;
	}
	
	public void setDataFile(String fileName) throws IOException, DataIOException {
	
		// data file path is relative to project file
		if (m_projectFile != null) {
			m_dataFile = new File(m_projectFile.getParent(), fileName);
		} else {
			m_dataFile = new File(fileName);
		}
		
		try {
			loadData();
		} catch (IOException e) {
			m_dataFile = null;
			throw e;
		} catch (DataIOException e) {
			m_dataFile = null;
			throw e;
		}
		
		// create All Group
		if (m_dims.size() < 20) {
			Group allGroup = Group.CreateAllGroup(this);
			addGroup(allGroup);
		}
	}	
	
	public File getFile() {
		return m_projectFile;
	}
	
	public void setFile(File file) {
		m_projectFile = file;
	}
	
	public Group[] getGroups() {
		if (m_groups != null) {
			Group[] result = new Group[m_groups.size()];
			result = m_groups.toArray(result);
			return result;
		}
		return null;
	}
	
	public void addGroup(Group group) {
		if (m_groups == null) {
			m_groups = new Vector<Group>();
		}
		
		if ("All".equals(group.getName())) {
			for (Group g : m_groups) {
				if ("All".equals(group.getName())) {
					int index = m_groups.indexOf(g);
					m_groups.set(index, group);
					return;
				}
			}
		}
		
		m_groups.add(group);
	}
	
	public void removeGroup(Group group) {
		if (m_groups != null) {
			m_groups.remove(group);
		}
	}

	public int getHighlightRGB() {
		return m_highlightRGB;
	}
	
	public void setHighlight(int highlight) {
		m_highlightRGB = highlight;
	}
	
	public String getName() {
		if (m_name == null) {
			return "Untitled";
		}
		return m_name;
	}
	
	public void setName(String name) {
		m_name = name;
	}
	
	public int[] getPalette() {
		if (m_palette != null) {
			return m_palette.clone();
		}
		Dim colorDim = getColorDimension();
		if (colorDim != null) {
			PaletteMgr paletteMgr = PaletteMgr.GetInstance();
			return paletteMgr.getDefaultPalette(colorDim.getType(), colorDim.getBinCount());
		}
		return null;
	}

	public void setPalette(int[] palette) {
		m_palette = palette;
	}

	public PrincipalComponent[] getPrincipalComponents() {
		if (m_dims == null) {
			return null;
		}
		Vector<PrincipalComponent> pcs = new Vector<PrincipalComponent>();
		Collection<Dim> values = m_dims.values();
		for (Dim dim : values) {
			if (dim instanceof PrincipalComponent) {
				pcs.add((PrincipalComponent)dim);
			}
		}
		if (pcs.size() == 0) {
			return null;
		}
		PrincipalComponent[] result = new PrincipalComponent[pcs.size()];
		return pcs.toArray(result);
	}
	
	public int getPrincipalComponentCount() {
		int result = 0;
		if (m_dims != null) {
			Collection<Dim> values = m_dims.values();
			for (Dim dim : values) {
				if (dim instanceof PrincipalComponent) {
					result++;
				}
			}
		}
		return result;
	}
	
	public Dim getShapeDimension() {
		if (m_shapeDimension != null) {
			return m_shapeDimension;
		}
		if (m_dims != null) {
			Iterator<Dim> values = m_dims.values().iterator();
			return values.next();
		}
		return null;
	}
	
	public void setShapeDimension(Dim dim) {
		m_shapeDimension = dim;
	}
	
	public int[] getShapes() {
		if (m_shapes != null) {
			return m_shapes.clone();
		}
		return new int[]{Constants.SHAPE_ELLIPSE};
	}

	public void setShapes(int[] shapes) {
		m_shapes = shapes;
	}
	
	public Table getTable() {
		return m_rawdata;
	}
	
	public boolean isDirty() {
		// TODO - check for changes to project
		return true;
	}
	
	public void save(Document dom, Element rootElement) {
		
		Element projectElement = dom.createElement(PROJECT_STR);
		rootElement.appendChild(projectElement);
		
		// Save project name
		if (m_name != null) {
			Element nameElement = dom.createElement(NAME_STR);
			Text nameText = dom.createTextNode(m_name);
			nameElement.appendChild(nameText);
			projectElement.appendChild(nameElement);
		}
		
		// Save data file
		if (m_dataFile != null) {
			Element dataElement = dom.createElement(DATA_STR);
			projectElement.appendChild(dataElement);
			Element srcElement = dom.createElement(SRC_STR);
			dataElement.appendChild(srcElement);
			Text srcText = dom.createTextNode(m_dataFile.getName());
			srcElement.appendChild(srcText);
		}
		
		// Save customized dimensions
		Element dimsElement = dom.createElement(DIMS_STR);
		projectElement.appendChild(dimsElement);
		for (Dim dim : m_dims.values()) {
			if (dim instanceof DataFileDim) {
				dim.save(dom, dimsElement);
			}
		}

		// Save principal components
		Element pcsElement = dom.createElement(PCS_STR);
		projectElement.appendChild(pcsElement);
		for (Dim dim : m_dims.values()) {
			if (dim instanceof PrincipalComponent) {
				dim.save(dom, pcsElement);
			}
		}
		
		
		// Save highlight and filter info
		Element filtersElement = dom.createElement(FILTERS_STR);
		projectElement.appendChild(filtersElement);
		for (Dim dim : m_dims.values()) {
			String filter = dim.getFilter();
			// Only save filter if it is not the default
			if (!"TRUE".equals(filter)) {
				Element filterElement = dom.createElement(FILTER_STR);
				filtersElement.appendChild(filterElement);
				filterElement.setAttribute(Dim.COLUMN_STR, dim.getColumn());
				Text filterText = dom.createTextNode(filter);
				filterElement.appendChild(filterText);
			}
		}
		
		Element highlightsElement = dom.createElement(HIGHLIGHTS_STR);
		projectElement.appendChild(highlightsElement);
		for (Dim dim : m_dims.values()) {
			String highlight = dim.getHighlight();
			// Only save highlight if it is not the default
			if (!"FALSE".equals(highlight)) {
				Element highlightElement = dom.createElement(HIGHLIGHT_STR);
				highlightsElement.appendChild(highlightElement);
				highlightElement.setAttribute(Dim.COLUMN_STR, dim.getColumn());
				Text highlightText = dom.createTextNode(highlight);
				highlightElement.appendChild(highlightText);
			}
		}
		
		// save visual effects
		Element visualElement = dom.createElement(VISUAL_STR);
		projectElement.appendChild(visualElement);
		// save highlight color
		if (m_highlightRGB != -1) {
			Element highlightElement = dom.createElement(HIGHLIGHT_STR);
			visualElement.appendChild(highlightElement);
			Element rgbElement = dom.createElement(RGB_STR);
			highlightElement.appendChild(rgbElement);
			Text rgbText = dom.createTextNode(Integer.toString(m_highlightRGB));
			rgbElement.appendChild(rgbText);
		}
		
		if (m_colorDimension != null) {
			Element colorDimElement = dom.createElement(COLORDIM_STR);
			visualElement.appendChild(colorDimElement);
			Text colorDimText = dom.createTextNode(m_colorDimension.getColumn());
			colorDimElement.appendChild(colorDimText);
		}
		
		if (m_palette != null) {
			Element paletteElement = dom.createElement(PALETTE_STR);
			visualElement.appendChild(paletteElement);
			for (int i = 0; i < m_palette.length; i++) {
				Element rgbElement = dom.createElement(RGB_STR);
				paletteElement.appendChild(rgbElement);
				Text rgbText = dom.createTextNode(Integer.toString(m_palette[i]));
				rgbElement.appendChild(rgbText);
			}
		}
		
		// save groups
		Element groupsElement = dom.createElement(GROUPS_STR);
		projectElement.appendChild(groupsElement);
		
		if (m_groups != null) {
			for (Group group : m_groups) {
				group.save(dom, groupsElement);
			}
		}
		
	}
	
	public void updateProject() {
		m_dynamicFilter.clear();
		m_dynamicFilter.set(BooleanLiteral.TRUE);
		
		m_dynamicHighlight.clear();
		
		for (Dim dim : m_dims.values()) {
			FrequencyQueryBinding filterQuery = dim.getFilterQuery();
			m_dynamicFilter.add(filterQuery.getPredicate());
			
			FrequencyQueryBinding highlightQuery = dim.getHighlightQuery();
			m_dynamicHighlight.add(highlightQuery.getPredicate());
		}
	}
	
	private void loadData() throws IOException, DataIOException {
		System.out.println("Loading data file "+m_dataFile.getAbsolutePath());
		
		String path = m_dataFile.getAbsolutePath();
		if (path.endsWith(".csv") || path.endsWith(".gz")) {
			// comma separated
			CSVTableReader reader = new CSVTableReader();
			m_rawdata = reader.readTable(IOLib.streamFromString(path));
		} else if (path.endsWith(".txt")) { // also check for tabs
			// tab separated
			DelimitedTextTableReader reader = new DelimitedTextTableReader();
			m_rawdata = reader.readTable(IOLib.streamFromString(path));
		} else if (path.endsWith(".txt")) {
			// pipe separated
			DelimitedTextTableReader reader = new DelimitedTextTableReader("|");
			m_rawdata = reader.readTable(IOLib.streamFromString(path));
		} else {
			throw new UnsupportedEncodingException();
		}
		
		// Load dims from Data file
		if (m_dims == null) {
			m_dims = new HashMap<String,Dim>();
		}
		int dimCount = m_rawdata.getColumnCount();
		for (int i = 0; i < dimCount; i++) {
			String colName = m_rawdata.getColumnName(i);
			if (!m_dims.containsKey(colName)) {
				DataFileDim dim = new DataFileDim(this, colName);
				m_dims.put(colName, dim);
			}
		}
		
		System.out.println("Finished loading data file "+m_dataFile.getAbsolutePath());
	}
}
