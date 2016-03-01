package heidi.data.query;

import heidi.frequency.JHistogram;
import heidi.project.Dim;

import java.util.Iterator;
import java.util.Vector;

import javax.swing.JComponent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import prefuse.Constants;
import prefuse.data.Table;
import prefuse.data.expression.AndPredicate;
import prefuse.data.expression.BooleanLiteral;
import prefuse.data.expression.ColumnExpression;
import prefuse.data.expression.ComparisonPredicate;
import prefuse.data.expression.Expression;
import prefuse.data.expression.Literal;
import prefuse.data.expression.OrPredicate;
import prefuse.data.expression.Predicate;
import prefuse.data.expression.parser.ExpressionParser;
import prefuse.data.query.DynamicQueryBinding;
import prefuse.data.tuple.TableTuple;
import prefuse.util.DataLib;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

/**
 * FrequencyQueryBinding supporting queries based on a list of included
 * data values.
 * 
 * Based on ListQueryBinding by jeffrey heer.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 * @author veronika
 */

public class FrequencyQueryBinding extends DynamicQueryBinding {

    private ElementListModel m_model;
    private Dim              m_dim;
    private Listener         m_lstnr;
    
	public FrequencyQueryBinding(Table tupleSet, String field, Dim dim) {
		super(tupleSet, field);
		
		m_dim = dim;
        m_lstnr = new Listener();
        
        // init predicate
        OrPredicate orP = new OrPredicate();
        setPredicate(orP);
    
        // init model
    	m_model = new ElementListModel(m_dim.getBins());
    	m_model.addListSelectionListener(m_lstnr);
	}

    /**
     * Creates a new group of check boxes for interacting with the query.
     * @return a {@link prefuse.util.ui.JToggleGroup} of check boxes bound to
     * this dynamic query.
     * @see prefuse.data.query.DynamicQueryBinding#createComponent()
     */
    public JComponent createComponent() {
        throw new NotImplementedException();
    }

	public JHistogram createHistogram(Predicate primary, Predicate secondary) {
		return new JHistogram(m_model, m_dim, primary, secondary);
    }
	
	public void select(String queryString) {
		
		/* Note: There is a bug in ExpressionParser.  If the expression is "TRUE" or "FALSE"
		 * It should return the BooleanLiteral.TRUE or BooleanLiteral.FALSE singletons but instead
		 * it creates a new instance.  Further, BooleantLiteral does not implement the equals() 
		 * method and by default only compares instance ids.
		 * As a work-around, look for the specific case of a "TRUE" or "FALSE" query string.
		 */
		if (queryString.equals("FALSE")) {
			m_model.clearSelection();
		} else if (queryString.equals("TRUE")) {
			m_model.setSelectionInterval(0, m_model.getSize() - 1);
		} else {
			m_model.setValueIsAdjusting(true);
			m_model.clearSelection();
			Table table = m_dim.getProject().getTable();
			Predicate predicate = ExpressionParser.predicate(queryString);
			Iterator<?> queryIterator = table.tuples(predicate);
			if (m_dim.getType() == Constants.NUMERICAL) {
				while (queryIterator.hasNext()) {
					TableTuple tuple = (TableTuple)queryIterator.next();
					Double object = tuple.getDouble(m_field);
					for (int index = 0; index < m_model.getSize(); index++) {
						Double start = m_model.getDoubleAt(index);
						Double end;
						if (index + 1 < m_model.getSize()) {
							end = m_model.getDoubleAt(index+1);
						} else {
							end = m_model.getDoubleAt(index) + m_dim.getBinWidth();
						}
						if (object >= start && object < end) {
							m_model.addSelectionInterval(index, index);
						}
					}
				}
			} else {
				while (queryIterator.hasNext()) {
					TableTuple tuple = (TableTuple)queryIterator.next();
					Object object = tuple.get(m_field);
					int index = m_model.indexOf(object);
					m_model.addSelectionInterval(index, index);
				}
			}
			m_model.setValueIsAdjusting(false);
		}
	}
	
    // ------------------------------------------------------------------------
    
    /**
    * Create a comparison predicate for the given data value
    */
   private ComparisonPredicate getComparison(int operation, Object o) {
	   // TODO - is it slow to get the type on each comparison?
	   Class<?> type = DataLib.inferType(m_tuples, m_field);
       Expression left = new ColumnExpression(m_field);
       Expression right = Literal.getLiteral(o, type);
       return new ComparisonPredicate(operation, left, right);
   }
   
   private class Listener implements ListSelectionListener {

	   public void valueChanged(ListSelectionEvent e) {
		   ElementListModel model = (ElementListModel)e.getSource();
		   
		   if (m_dim.getType() == Constants.NUMERICAL) {
			   numericalUpdate(model);
		   } else {
			   ordinalUpdate(model);
		   }
	   }
   }
   
   private void numericalUpdate(ElementListModel model) {
	   OrPredicate orP = (OrPredicate)m_query;

	   if (model.isSelectionEmpty()) {
		   orP.clear();
	   } else {
		   
		   int min   = model.getMinSelectionIndex();
		   int max   = model.getMaxSelectionIndex();
		   int count = 0;
	
		   // find contiguous selection ranges
		   Vector<SelectionRange> ranges = new Vector<SelectionRange>();
		   SelectionRange range = null;
		   for (int i = min; i <= max; ++i) {
			   if (model.isSelectedIndex(i)) {
				   ++count;
				   if (range == null) {
					   range = new SelectionRange();
					   range.start = i;
				   }
			   } else {
				   if (range != null) {
					   range.end = i;
					   ranges.add(range);
				   }
				   range = null;
			   }
		   }
		   if (range != null) {
			   range.end = max+1;
			   ranges.add(range);
		   }
	
		   if (count == model.getSize()) {
			   orP.set(BooleanLiteral.TRUE);
		   } else {
			   Predicate[] predicate = new Predicate[ranges.size()];
			   for (int i = 0; i < ranges.size(); i++) {
				   SelectionRange selection = ranges.get(i);
				   ComparisonPredicate greaterThan = getComparison(ComparisonPredicate.GTEQ, model.getElementAt(selection.start));
				   ComparisonPredicate lessThan;
				   if (selection.end == model.getSize()) {
					   Double maxValue = (Double)model.getElementAt(selection.end-1) + (Double)model.getElementAt(1) - (Double)model.getElementAt(0);
					   lessThan = getComparison(ComparisonPredicate.LTEQ, maxValue);
				   } else {
					   lessThan = getComparison(ComparisonPredicate.LT, model.getElementAt(selection.end));
				   }
				   predicate[i] = new AndPredicate(greaterThan, lessThan);
			   }
			   orP.set(predicate);
		   }
	   }
	}   
   
   private void ordinalUpdate(ElementListModel model) {
	   OrPredicate orP = (OrPredicate)m_query;

	   if (model.isSelectionEmpty()) {
		   orP.clear();
	   } else {
		   int min   = model.getMinSelectionIndex();
		   int max   = model.getMaxSelectionIndex();
		   int count = 0;
		   
		   for (int i = min; i <= max; ++i) {
			   if (model.isSelectedIndex(i)) {
				   ++count;
			   }
		   }
		   
		   if (count == model.getSize()) {
			   orP.set(BooleanLiteral.TRUE);
		   } else {
			   Predicate[] predicate = new Predicate[count];
			   for (int i = min, j = 0; i <= max; i++) {
                   if (model.isSelectedIndex(i)) {
                       predicate[j++] = getComparison(ComparisonPredicate.EQ, model.getElementAt(i));
                   }
               }
			   orP.set(predicate);
		   }
	   }
   }
   
   private class SelectionRange {
	   private int start;
	   private int end;
   }
}
