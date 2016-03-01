package heidi.frequency;

import heidi.project.Dim;
import prefuse.activity.Activity;
import prefuse.data.CascadedTable;
import prefuse.data.Table;
import prefuse.data.Tuple;
import prefuse.data.column.ColumnMetadata;
import prefuse.data.event.EventConstants;
import prefuse.data.event.TupleSetListener;
import prefuse.data.expression.AndPredicate;
import prefuse.data.expression.BooleanLiteral;
import prefuse.data.expression.Predicate;
import prefuse.data.tuple.TupleSet;
import prefuse.util.collections.CopyOnWriteArrayList;
/**
 * Based on HistogramTable by Kaitlin Sherwood 
 * 
 * A table of frequency counts per range of data 
 * 
 * @author <a href="http://webfoot.com/ducky.home.html">Kaitlin Duck Sherwood</a>
 * @author <a href="http://jheer.org">jeffrey heer</a>
 * @author veronika
 */

abstract public class FrequencyTable extends Table {
	
	Dim    m_dim;
	String m_groupID;
	
	Predicate m_primary;
	Predicate m_secondary;
	Activity  m_updateActivity;
	
	private static int s_index = 0;
	
	private static final String GROUPID  = "histogram";
	private static final String BIN = "binStart:";
	private static final String COUNT = "count:";
	private static final String RANGE = "range:";
	private static final String TYPE = "type:";
	
	public static final String PRIMARY = "primary";
	public static final String SECONDARY = "secondary";
	
	/**
	 * @param table a Prefuse Table with data values (i.e. raw data, not sorted and counted)
	 * @param column the data column on which to measure frequencies
	 * @param binCount count of how many bins across which to split the data's range
	 * @param dataType type of data which may be Heidi.ORDINAL, Heidi.CATEGORICAL or Heidi.QUANTITATIVE
	 */
	public FrequencyTable(Dim dim) {
		this(dim, null, null);
	}
	
	public FrequencyTable(Dim dim, Predicate primary, Predicate secondary) {
		super(0, 0);
		
		m_dim = dim;
		m_groupID = GROUPID+s_index++;
		
		m_primary = primary;
		m_secondary = secondary;
		
		int binCount = m_dim.getBinCount();
		if (binCount < 1) {
			throw new IllegalArgumentException("Can not have a bin count that is less than 1");
		}
		
		init();
		
		// Prefuse Table is not thread safe so make all modifications to it
		// through the Activity Manager
		m_updateActivity = new Activity(10) {
			@Override
			protected void run(long elapsedTime) {
				update();
			}
		};
		
		// Handle changes that affect frequency counts.
		TupleSetListener listener = new TupleSetListener() {
			@Override
			public void tupleSetChanged(TupleSet tset, Tuple[] added, Tuple[] removed) {
				m_updateActivity.run();
			}
		};
		
		Table table = m_dim.getProject().getTable();
		Predicate p1 = primary == null ? BooleanLiteral.TRUE : primary;
		TupleSet tupleSet = new CascadedTable(table, p1);
		tupleSet.addTupleSetListener(listener);
		
		if (secondary != null) {
			AndPredicate p2 = new AndPredicate(p1, secondary);
			tupleSet = new CascadedTable(table, p2);
			tupleSet.addTupleSetListener(listener);
		}
		
		m_updateActivity.run();
	}
	
	public String getBinColumn() {
		return BIN+m_groupID;
	}

	public int getBinCount() {
		return m_dim.getBinCount();
	}
	
	public String getCountColumn() {
		return COUNT+m_groupID;
	}
	
	public String getRangeColumn() {
		return RANGE+m_groupID;
	}	
	
	public String getTypeColumn() {
		return TYPE+m_groupID;
	}	
	
	void init() {
		String rangeColumn = getRangeColumn();
		addColumn(rangeColumn, String.class);
		
		String typeColumn = getTypeColumn(); 
		addColumn(typeColumn, String.class);
		
		String countColumn = getCountColumn(); 
		addColumn(countColumn, int.class);
		
		int binCount = m_dim.getBinCount();
		int rowCount = m_secondary == null ? binCount : 2 * binCount;
		addRows(rowCount);
		
		// make ordering consistent with table
		Table table = m_dim.getProject().getTable();
		ColumnMetadata metaTable = table.getMetadata(m_dim.getColumn());
		ColumnMetadata metaFTable = getMetadata(getBinColumn());
		metaFTable.setComparator(metaTable.getComparator());
	}
	
	void update() {
		// We do not want to notify listeners of table changes 
		// until they are all complete.
		// Remove all listeners from the array and save them
		// for later restoration.
		CopyOnWriteArrayList oldListeners = m_listeners;
		m_listeners = new CopyOnWriteArrayList();

		try {		
			updatePrimaryData();
			if (m_secondary != null) {
				updateSecondaryData();
			}
		} finally {
			// Add the previous listeners back to the list
			m_listeners.addAll(oldListeners);
			fireTableEvent(0, getRowCount()-1, EventConstants.ALL_COLUMNS, EventConstants.UPDATE);
		}
	}
	
	abstract void updatePrimaryData();
	abstract void updateSecondaryData();

} // end of class FrequencyTable
