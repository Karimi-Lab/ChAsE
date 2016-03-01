package heidi.frequency;

import heidi.project.Dim;

import java.util.Iterator;

import prefuse.data.Table;
import prefuse.data.expression.AndPredicate;
import prefuse.data.expression.BooleanLiteral;
import prefuse.data.expression.Predicate;
import prefuse.data.tuple.TableTuple;

public class OridinalFrequencyTable extends FrequencyTable {
	
	public OridinalFrequencyTable(Dim dim) {
		super(dim);
	}

	public OridinalFrequencyTable(Dim dim, Predicate primary, Predicate secondary) {
		super(dim, primary, secondary);
	}
	
	void init() {
		addColumn(getBinColumn(), String.class);
		
		super.init();
		
		String binColumn = getBinColumn();
		String countColumn = getCountColumn();
		String rangeColumn = getRangeColumn();
		String typeColumn = getTypeColumn();

		int binCount = m_dim.getBinCount();
		Object[] bins = m_dim.getBins();
		for (int i = 0; i < binCount; i++) {
			String key = "";
			if (bins[i] != null) {
				key = bins[i].toString();
			}
			setString(i, binColumn, key);
			setString(i, rangeColumn, key);
			setString(i, typeColumn, PRIMARY);
			setInt(i, countColumn, 0);
		}
		
		if (m_secondary != null) {
			int offset = binCount;
			// initialize data to zero
			for (int i = 0; i < binCount; i++) {
				String key = "";
				if (bins[i] != null) {
					key = bins[i].toString();
				}
				setString(offset + i, binColumn, key);
				setString(offset + i, rangeColumn, key);
				setString(offset + i, typeColumn, SECONDARY);
				setInt(offset + i, countColumn, 0);
			}
		}
	}
	
	@Override
	void updatePrimaryData() {
		String countColumn = getCountColumn();

		// For ordinal or categorical data, use each unique value as a bin 
		Table table = m_dim.getProject().getTable();
		String column = m_dim.getColumn();

		// initialize data to zero
		int binCount = m_dim.getBinCount();
		for (int i = 0; i < binCount; i++) {
			setInt(i, countColumn, 0);
		}

		// set counts for items that match primary predicate
		Predicate primary = m_primary == null ? BooleanLiteral.TRUE : m_primary;
		Iterator<?> primaryTuples = table.tuples(primary);
		while (primaryTuples.hasNext()) {
			TableTuple tuple = (TableTuple)primaryTuples.next();
			Object value = tuple.getString(column);
			int bin = getBinIndexOf(value);
			int count = getInt(bin, countColumn);  
			setInt(bin, countColumn, count+1 );
		}
	}
	
	@Override
	void updateSecondaryData() {
		String countColumn = getCountColumn();
		
		// For ordinal or categorical data, use each unique value as a bin 
		Table table = m_dim.getProject().getTable();
		String column = m_dim.getColumn();
		
		int binCount = m_dim.getBinCount();
		int offset = binCount;
		// initialize data to zero
		for (int i = 0; i < binCount; i++) {
			setInt(offset + i, countColumn, 0);
		}
		// set counts for items that match secondary predicate
		Predicate primary = m_primary == null ? BooleanLiteral.TRUE : m_primary;
		AndPredicate secondary = new AndPredicate(primary, m_secondary);
		Iterator<?> secondaryTuples = table.tuples(secondary);
		while (secondaryTuples.hasNext()) {
			TableTuple tuple = (TableTuple)secondaryTuples.next();
			Object value = tuple.getString(column);
			int bin = getBinIndexOf(value);
			int maxCount = getInt(bin, countColumn);
			int count = getInt(offset+bin, countColumn);  
			setInt(offset+bin, countColumn, Math.min(maxCount, count+1));
		}
	}
	
	private int getBinIndexOf(Object value) {
		Object[] bins = m_dim.getBins();
		for (int i = 0; i < bins.length; i++) {
			if (bins[i] != null && bins[i].equals(value)) {
				return i;
			}
		}
		// Workaround for a Bug:  
    	// Prefuse is creating a class of type Integer even though data is Ordinal. 
    	// We are expecting a String.  
    	// Workaround is to create an Integer object from the String and compare.
		if ((value instanceof String) && (bins[0] instanceof Integer)) {
			Integer integer = Integer.parseInt((String)value);
			for (int i = 0; i < bins.length; i++) {
				if (bins[i] != null && bins[i].equals(integer)) {
					return i;
				}
			}
		}
		return -1;
	}
}
