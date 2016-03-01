package heidi.frequency;

import heidi.project.Dim;

import java.util.Iterator;

import prefuse.data.Table;
import prefuse.data.expression.AndPredicate;
import prefuse.data.expression.BooleanLiteral;
import prefuse.data.expression.Predicate;
import prefuse.data.tuple.TableTuple;

public class NumericalFrequencyTable extends FrequencyTable {

	public NumericalFrequencyTable(Dim dim) {
		super(dim);
	}
	
	public NumericalFrequencyTable(Dim dim, Predicate primary, Predicate secondary) {
		super(dim, primary, secondary);
	}
	
	void init() {
		
		addColumn(getBinColumn(), double.class);

		super.init();
		
		String binColumn = getBinColumn();
		String countColumn = getCountColumn();
		String rangeColumn = getRangeColumn();
		String typeColumn = getTypeColumn();
		
		int binCount = m_dim.getBinCount();
		Object[] bins = m_dim.getBins();
		double binWidth = m_dim.getBinWidth();
		
		for (int i = 0; i < binCount; i++) {
			// store start of bin range
			double binStart = (Double)bins[i];
			setDouble(i, binColumn, binStart);
			setString(i, rangeColumn, binStart+" to "+(binStart+binWidth));
			setString(i, typeColumn, PRIMARY);
			setInt(i, countColumn, 0);
		}
		
		if (m_secondary != null) {
			int offset = binCount;
			// initialize data to zero
			for (int i = 0; i < binCount; i++) {
				// store start of bin range
				double binStart = (Double)bins[i];
				setDouble(offset+i, binColumn, binStart);
				setString(offset+i, rangeColumn, binStart+" to "+(binStart+binWidth));
				setString(offset+i, typeColumn, SECONDARY);
				setInt(offset+i, countColumn, 0);
			}
		}
		
	}
	
	void updatePrimaryData() {
		String countColumn = getCountColumn();

		Table table = m_dim.getProject().getTable();
		String column = m_dim.getColumn();
		
		int binCount = m_dim.getBinCount();
		Object[] bins = m_dim.getBins();
		double binWidth = m_dim.getBinWidth();
		double min = (Double)bins[0];
		
		// initialize data to zero
		for (int i = 0; i < binCount; i++) {
			setInt(i, countColumn, 0);
		}
		// set counts for items that match primary predicate
		Predicate primary = m_primary == null ? BooleanLiteral.TRUE : m_primary;
		Iterator<?> primaryTuples = table.tuples(primary);
		while (primaryTuples.hasNext()) {
			TableTuple tuple = (TableTuple)primaryTuples.next();
			double value = tuple.getDouble(column);
			int bin = (int)Math.floor((value - min) / binWidth);
			int count = getInt(bin, countColumn);  
			setInt(bin, countColumn, count+1 );
		}
	}
	
	void updateSecondaryData() {
		String countColumn = getCountColumn();

		Table table = m_dim.getProject().getTable();
		String column = m_dim.getColumn();
		
		int binCount = m_dim.getBinCount();
		Object[] bins = m_dim.getBins();
		double binWidth = m_dim.getBinWidth();
		double min = (Double)bins[0];
		
		int offset = binCount;
		// initialize data to zero
		for (int i = 0; i < binCount; i++) {
			setInt(offset+i, countColumn, 0);
		}
		// set counts for items that match secondary predicate
		Predicate primary = m_primary == null ? BooleanLiteral.TRUE : m_primary;
		AndPredicate secondary = new AndPredicate(primary, m_secondary);
		Iterator<?> secondaryTuples = table.tuples(secondary);
		while (secondaryTuples.hasNext()) {
			TableTuple tuple = (TableTuple)secondaryTuples.next();
			double value = tuple.getDouble(column);
			int bin = (int)Math.floor((value - min) / binWidth);
			int maxCount = getInt(bin, countColumn);
			int count = getInt(offset+bin, countColumn);  
			setInt(offset+bin, countColumn, Math.min(maxCount, count+1));
		}
	}
}
