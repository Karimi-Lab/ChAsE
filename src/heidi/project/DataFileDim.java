package heidi.project;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import prefuse.Constants;
import prefuse.data.Table;
import prefuse.data.column.ColumnMetadata;

public class DataFileDim extends Dim {

	private boolean m_customized;
	
	public static DataFileDim UpdateFromDom(Element dimElement, Project project) {
		
		NodeList nameElements = dimElement.getElementsByTagName(NAME_STR);
		String name = null;
		if (nameElements != null && nameElements.getLength() > 0) {
			name = DomUtil.GetNodeText(nameElements.item(0));
		}
		
		NodeList columnElements = dimElement.getElementsByTagName(COLUMN_STR);
		Element columnElement = (Element)columnElements.item(0);
		String column = DomUtil.GetNodeText(columnElement);
		
		String typeAttr = columnElement.getAttribute(TYPE_STR);
		int type;
		if (NOMINAL_STR.equals(typeAttr)) {
			type = Constants.NOMINAL;
		} else if (ORDINAL_STR.equals(typeAttr)) {
			type = Constants.ORDINAL;
		} else if (NUMERICAL_STR.equals(typeAttr)) {
			type = Constants.NUMERICAL;
		} else {
			type = GetDefaultType(project, column);
		}

		int binCount;
		NodeList binCountElements = dimElement.getElementsByTagName(BIN_STR);
		if (binCountElements != null && binCountElements.getLength() > 0) {
			String count = DomUtil.GetNodeText(binCountElements.item(0));
			binCount = Integer.parseInt(count);
		} else {
			binCount = GetDefaultBinCount(project, column);
		}
		
		DataFileDim dim = (DataFileDim)project.getDim(column);
		dim.setCustomized(true);
		if (name != null) {
			dim.setName(name);
		}
		dim.setType(type);
		dim.setBinCount(binCount);
		
		return dim;
	}
		
	public DataFileDim(Project project, String column) {
		this(project, column, GetDefaultType(project, column), GetDefaultBinCount(project, column));
	}
	
	public DataFileDim(Project project, String column, int type, int binCount) {
		super(project, column, type);
		// Note: a column of name <column> has to exist in underlying prefuse table
		initBins(binCount);
		initPredicates();
	}
	
	public boolean getCustomized() {
		return m_customized;
	}
	
	public void setCustomized(boolean customized) {
		m_customized = customized;
	}
	
	@Override
	public void setBinCount(int count) {
		super.setBinCount(count);
		initPredicates();
		m_customized = true;
	}
	
	@Override
	public int getDefaultBinCount() {
		return GetDefaultBinCount(getProject(), getColumn());
	}
		
	@Override
	public void setName(String name) {
		super.setName(name);
		m_customized = true;
	}
	
	public void setType(int type) {
		m_type = type;
		initBins(getDefaultBinCount());
		initPredicates();
		m_customized = true;
	}
	
	@Override
	public void save(Document dom, Element dimsElement) {
		if (!m_customized) {
			// Do not save every dimension because there may
			// be several hundred of them.  Only save
			// a dimension if it is not using the default
			// values.
			return;
		}
		Element dimElement = dom.createElement(DIM_STR);
		dimsElement.appendChild(dimElement);
		
		// Save name
		String name = getName();
		if (name != null) {
			Element nameElement = dom.createElement(NAME_STR);
			dimElement.appendChild(nameElement);
			Text nameText = dom.createTextNode(name);
			nameElement.appendChild(nameText);
		}
		
		// Save column
		Element columnElement = dom.createElement(COLUMN_STR);
		String type;
		switch (getType()) {
		case Constants.NUMERICAL:
			type = NUMERICAL_STR; break;
		case Constants.ORDINAL:
			type = ORDINAL_STR; break;
		case Constants.NOMINAL:
			type = NOMINAL_STR; break;
		default:
			type = NOMINAL_STR;	
		}
		columnElement.setAttribute(TYPE_STR, type);
		dimElement.appendChild(columnElement);
		Text columnText = dom.createTextNode(getColumn());
		columnElement.appendChild(columnText);
		
		// Save bin count
		Element binElement = dom.createElement(BIN_STR);
		dimElement.appendChild(binElement);
		Text binText = dom.createTextNode(Integer.toString(getBinCount()));
		binElement.appendChild(binText);
		
	}
	
	private static int GetDefaultType(Project project, String column) {
		Table table = project.getTable();
		
		if (table.canGetDouble(column)) {
			return Constants.NUMERICAL;
		}
		return Constants.NOMINAL;
	}
	
	private static int GetDefaultBinCount(Project project, String column) {
		Table table = project.getTable();
		
		ColumnMetadata meta = table.getMetadata(column);
		Object[] values = meta.getOrdinalArray();
		
		if (table.canGetDouble(column)) {
			// TODO come up with a reasonable bin size for numerical values
			//int min = m_rawdata.getDouble(meta.getMinimumRow(), colName);
			//int max = m_rawdata.getDouble(meta.getMaximumRow(), colName);
			//dim.setBinCount(Math.min(max - min, 20));
			return Math.min(20, values.length);
		} else if (table.canGetString(column)) {
			return values.length;
		}
		return 20;
	}

}
