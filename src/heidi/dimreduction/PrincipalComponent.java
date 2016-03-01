package heidi.dimreduction;


import heidi.project.Dim;
import heidi.project.DomUtil;
import heidi.project.Project;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Vector;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import prefuse.Constants;
import prefuse.data.Table;
import prefuse.data.column.Column;
import prefuse.data.column.ColumnMetadata;

public class PrincipalComponent extends Dim {
	
	private int                   m_rank;
	private double                m_eigenValue;
	private BasisTransformation[] m_basisTransformation;
	
	private boolean               m_inTable;
	private double[]              m_transformedData;
	
	private static int Index = 0;
	private static final String COLUMN_NAME = "HeidiPrincipalComponent";
	private static final int DEFAULT_BINCOUNT = 20;
	
	public static final String PC_STR   = "pc";
	public static final String RANK_STR = "rank";
		
	public static PrincipalComponent CreateFromDom(Element pcElement, Project project, JamaPCAnalyzer pcAnalyzer) {
		NodeList nameElements = pcElement.getElementsByTagName(NAME_STR);
		String name = null;
		if (nameElements != null && nameElements.getLength() > 0) {
			name = DomUtil.GetNodeText(nameElements.item(0));
		}
		
		NodeList rankElements = pcElement.getElementsByTagName(RANK_STR);
		Element rankElement = (Element)rankElements.item(0);
		int rank = Integer.valueOf(DomUtil.GetNodeText(rankElement));
		
		NodeList columnElements = pcElement.getElementsByTagName(COLUMN_STR);
		Element columnElement = (Element)columnElements.item(0);
		String column = DomUtil.GetNodeText(columnElement);
		
		int binCount = -1;
		NodeList binCountElements = pcElement.getElementsByTagName(BIN_STR);
		if (binCountElements != null && binCountElements.getLength() > 0) {
			String count = DomUtil.GetNodeText(binCountElements.item(0));
			binCount = Integer.parseInt(count);
		}
		
		PrincipalComponent dim = pcAnalyzer.getPrincipalComponent(rank);
		dim.setColumn(column);
		dim.addToTable();
		if (binCount > 0) {
			dim.setBinCount(binCount);
		}
		if (name != null) {
			dim.setName(name);
		}
		return dim;
	}
	
	public PrincipalComponent(Project project, double eigenValue, double[] eigenVector) {
		super(project, COLUMN_NAME+Index++, Constants.NUMERICAL);
		
		m_eigenValue = eigenValue;
		
		Vector<Dim> numericalDims = new Vector<Dim>();
		Dim[] dims = project.getDims();
		for (Dim dim : dims) {
			if (dim.getType() == Constants.NUMERICAL) {
				numericalDims.add(dim);
			}
		}
		
		int eigenVectorCount = eigenVector.length;
		m_basisTransformation = new BasisTransformation[eigenVectorCount];
		for (int i = 0; i < eigenVectorCount; i++) {
			m_basisTransformation[i] = new BasisTransformation(eigenVector[i], dims[i]);
		}
	}
	
	public int compareTo(PrincipalComponent data2) {
		double eigenValue2 = data2.m_eigenValue;
		return m_eigenValue == eigenValue2 ? 0 : (m_eigenValue > eigenValue2 ? +1 : -1);
	}

	@Override
	public void setBinCount(int count) {
		super.setBinCount(count);
		initPredicates();
	}
	
	public int getRank() {
		return m_rank;
	}

	public void setRank(int rank) {
		m_rank = rank;
	}
		
	public int getDefaultBinCount() {
		Table table = getProject().getTable();
		ColumnMetadata meta = table.getMetadata(getColumn());
		Object[] values = meta.getOrdinalArray();
		
		return Math.min(DEFAULT_BINCOUNT, values.length);
	}
	
	public Dim[] getOriginalDimensions() {
		int dimCount = m_basisTransformation.length;
		Dim[] originalDims = new Dim[dimCount];
		BasisTransformation[] sortedBasisTransform = m_basisTransformation.clone();
		Arrays.sort(sortedBasisTransform, new Comparator<BasisTransformation>() {
			@Override
			public int compare(BasisTransformation o1, BasisTransformation o2) {
				// perform compare in the reverse order to sort
				// in descending order
				return o2.compareTo(o1);
			}
		});
		for (int i = 0; i < dimCount; i++) {
			originalDims[i] = sortedBasisTransform[i].m_dim;
		}
		return originalDims;
	}
		
	public double getEigenValue() {
		return m_eigenValue;
	}
	
	public double[] getEigenVector() {
		int eigenVectorCount = m_basisTransformation.length;
		double[] eigenVector = new double[eigenVectorCount];
		for (int i = 0; i < eigenVectorCount; i++) {
			eigenVector[i] = m_basisTransformation[i].m_coefficient;
		}
		return eigenVector;
	}
	
	public String getFullName() {
		String result = "";
		BasisTransformation[] basis = m_basisTransformation.clone();
		Arrays.sort(basis, new Comparator<BasisTransformation>() {
			@Override
			public int compare(BasisTransformation o1, BasisTransformation o2) {
				// perform compare in the reverse order to sort
				// in descending order
				return o2.compareTo(o1);
			}
		});
		for (int i = 0; i < basis.length; i++) {
			result += basis[i];
			if (i < basis.length - 1) {
				result += "+";
			}
		}
		return result;
	}
	
	void setTransformedData(double[] data) {
		m_transformedData = data;
		if (m_inTable) {
			Table table = getProject().getTable();
			Column column = table.getColumn(getColumn());
			for (int i = 0; i < m_transformedData.length; i++) {
				column.setDouble(m_transformedData[i], i);
			}
		}
	}
	
	public void addToTable() {
		if (m_inTable) {
			return;
		}
		String columnName = getColumn();
		Table table = getProject().getTable();
		table.addColumn(columnName, double.class, 0.0);
		Column column = table.getColumn(columnName);
		for (int i = 0; i < m_transformedData.length; i++) {
			column.setDouble(m_transformedData[i], i);
		}
		initBins(getDefaultBinCount());
		initPredicates();
		m_inTable = true;
	}
	
	public void removeFromTable() {
		if (!m_inTable) {
			return;
		}
		String columnName = getColumn();
		Table table = getProject().getTable();
		table.removeColumn(columnName);
		m_inTable = false;
	}
	
	@Override
	public void save(Document dom, Element pcsElement) {
		Element pcElement = dom.createElement(PC_STR);
		pcsElement.appendChild(pcElement);
		
		// Save name
		String name = getName();
		if (name != null) {
			Element nameElement = dom.createElement(NAME_STR);
			pcElement.appendChild(nameElement);
			Text nameText = dom.createTextNode(name);
			nameElement.appendChild(nameText);
		}
		
		// Save rank
		int rank = getRank();
		Element rankElement = dom.createElement(RANK_STR);
		pcElement.appendChild(rankElement);
		Text rankText = dom.createTextNode(Integer.toString(rank));
		rankElement.appendChild(rankText);
		
		// Save column name
		String column = getColumn();
		Element columnElement = dom.createElement(COLUMN_STR);
		pcElement.appendChild(columnElement);
		Text columnText = dom.createTextNode(column);
		columnElement.appendChild(columnText);
		
		// Save bin count
		Element binElement = dom.createElement(BIN_STR);
		pcElement.appendChild(binElement);
		Text binText = dom.createTextNode(Integer.toString(getBinCount()));
		binElement.appendChild(binText);
	}
	
	@Override
	public String toString() {
		return getName();
	}
	
	private class BasisTransformation {
		
		private double m_coefficient;
		private Dim    m_dim;
		
		private BasisTransformation(double coefficient, Dim dim) {
			m_coefficient = coefficient;
			m_dim = dim;
		}
		
		private int compareTo(BasisTransformation data2) {
			double coefficient1 = Math.abs(m_coefficient);
			double coefficient2 = Math.abs(data2.m_coefficient);
			return coefficient1 == coefficient2 ? 0 : (coefficient1 > coefficient2 ? +1 : -1);
		}
		
		@Override
		public String toString() {
			DecimalFormat format = new DecimalFormat("0.####E0");
			return "("+format.format(m_coefficient)+")"+m_dim.getName();
		}
	}
}