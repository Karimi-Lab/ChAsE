package heidi.project;

import heidi.data.query.FrequencyQueryBinding;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import prefuse.Constants;
import prefuse.data.Table;
import prefuse.data.column.ColumnMetadata;
import prefuse.data.event.ExpressionListener;
import prefuse.data.expression.Expression;
import prefuse.data.expression.Predicate;

abstract public class Dim {

	private Project m_project;
	
	private String   m_name;
	private String   m_field;
	protected int    m_type;
	private Object[] m_bins;
	
	private String  m_filter;
	private String  m_highlight;
	
	private FrequencyQueryBinding m_filterQuery;
	private FrequencyQueryBinding m_highlightQuery;
	
	public static final String NOMINAL_STR   = "Categorical";
	public static final String ORDINAL_STR   = "Ordinal";
	public static final String NUMERICAL_STR = "Numerical";
	
	public static final String DIM_STR    = "dim";
	public static final String NAME_STR   = "name";
	public static final String COLUMN_STR = "column";
	public static final String TYPE_STR   = "type";
	public static final String BIN_STR    = "binCount";
	
	public Dim(Project project, String column, int type) {
		m_project = project;
		m_field = column;
		m_type = type;
	}

	protected void initBins(int binCount) {
		m_bins = new Object[binCount];
		
		Table table = m_project.getTable();
		ColumnMetadata metaTable = table.getMetadata(m_field);
			
		if (m_type == Constants.NOMINAL ||
			m_type == Constants.ORDINAL) {
			
			// TODO support an enumeration of ordinal values
			
			// For ordinal or categorical data, use each unique value as a bin 
			Object[] values = metaTable.getOrdinalArray();

			// It is possible for the number of bins to be different
			// than the number of unique ordinal values.  If there are more
			// ordinal values than bins, just show the first
			// binCount ones; if there are fewer, pad the ends with null.
			for (int i = 0; i < m_bins.length; i++) {
				if (i < values.length) {
					m_bins[i] = values[i];
				}
			}
			
		} else if (m_type == Constants.NUMERICAL ) {
			
			// TODO - try to find a better way to determine
			// bin start and bin width
			
			// For numerical values, store the start of the bin
			double min = table.getDouble(metaTable.getMinimumRow(), m_field);
			double max = table.getDouble(metaTable.getMaximumRow(), m_field);
			double span = max - min;
			if (span == 0) {
				m_bins = new Object[]{table.getDouble(0, m_field)};
			} else {
				double binWidth = span / (double)(m_bins.length - 1.0);
			
				for (int i = 0; i < m_bins.length; i++) {
					// store start of bin range
					m_bins[i] = new Double(min + ((double)i)*binWidth);
				}
			}
		}
	}
	
	protected void initPredicates() {
		Table table = m_project.getTable();
		
		m_filterQuery = new FrequencyQueryBinding(table, m_field, this);
		Predicate filterPredicate = m_filterQuery.getPredicate();
		filterPredicate.addExpressionListener(new ExpressionListener() {
			@Override
			public void expressionChanged(Expression expr) {
				m_filter = expr.toString();
			}
		});
		m_filterQuery.select(getFilter());
		
		m_highlightQuery = new FrequencyQueryBinding(table, m_field, this);
		Predicate highlightPredicate = m_highlightQuery.getPredicate();
		highlightPredicate.addExpressionListener(new ExpressionListener() {
			@Override
			public void expressionChanged(Expression expr) {
				m_highlight = expr.toString();
			}
		});
		m_highlightQuery.select(getHighlight());
	}
	
	public Object[] getBins() {
		return m_bins.clone();
	}
	
	public int getBinCount() {
		return m_bins.length;
	}
			
	public void setBinCount(int count) {
		initBins(count);
	}
	
	public double getBinWidth() {
		if (m_bins.length < 2) {
			return 0;
		}
		if (m_bins[0] instanceof Double) {
			return (Double)m_bins[1] - (Double)m_bins[0];
		}
		System.out.println("Unexpected access");
		return 1;
	}
		
	public String getColumn() {
		return m_field;
	}
	
	protected void setColumn(String column) {
		m_field = column;;
	}
	
	abstract public int getDefaultBinCount();
	
	public String getFilter() {
		if (m_filter != null) {
			return m_filter;
		}
		return "TRUE";
	}
	
	public void setFilter(String filter) {
		m_filter = filter;
		m_filterQuery.select(getFilter());
	}
	
	public FrequencyQueryBinding getFilterQuery() {
		return m_filterQuery;
	}
	
	public String getHighlight() {
		if (m_highlight != null) {
			return m_highlight;
		}
		return "FALSE";
	}
	
	public void setHighlight(String highlight) {
		m_highlight = highlight;
		m_highlightQuery.select(getHighlight());
	}
	
	public FrequencyQueryBinding getHighlightQuery() {
		return m_highlightQuery;
	}
	
	public String getName() {
		return m_name == null ? m_field : m_name;
	}

	public void setName(String name) {
		m_name = name;
	}
	
	public Project getProject() {
		return m_project;
	}
	
	public int getType() {
		return m_type;
	}
	
	abstract public void save(Document dom, Element dimsElement);
	
	@Override
	public String toString() {
		return getName();
	}
}
