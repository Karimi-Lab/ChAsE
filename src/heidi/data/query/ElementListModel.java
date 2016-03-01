package heidi.data.query;

import java.util.ArrayList;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.DefaultListSelectionModel;
import javax.swing.MutableComboBoxModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import prefuse.util.collections.CopyOnWriteArrayList;

/**
 * Based on prefuse.data.query.ListModel by Jeffert Heer
 * 
 * Add support for discontinuous selection ranges
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 * @author veronika
 */
public class ElementListModel extends DefaultListSelectionModel implements MutableComboBoxModel
{   
	private static final long serialVersionUID = -5523758876806530539L;
	private ArrayList<Object> m_items = new ArrayList<Object>();
    private CopyOnWriteArrayList m_lstnrs = new CopyOnWriteArrayList();
    
    private static final Logger s_logger = Logger.getLogger(ElementListModel.class.getName());
    
    /**
     * Create an empty ElementListModel.
     */
    public ElementListModel() {
        // do nothing
    }
    
    /**
     * Create an ElementListModel with the provided items.
     * @param items the items for the data model.
     */
    public ElementListModel(final Object[] items) {
        for ( int i=0; i<items.length; ++i )
            m_items.add(items[i]);
    }
    
    // --------------------------------------------------------------------
    
    /**
     * Indicates if the ElementListModel currently has multiple selections.
     * @return true if there are multiple selections, false otherwise
     */
    private boolean isMultipleSelection() {
        return getMaxSelectionIndex()-getMinSelectionIndex() > 0;
    }
    
    /**
     * @see javax.swing.ComboBoxModel#getSelectedItem()
     */
    public Object getSelectedItem() {
        int idx = getMinSelectionIndex();
        return ( idx == -1 ? null : m_items.get(idx) );
    }
    
    /**
     * @see javax.swing.ComboBoxModel#setSelectedItem(java.lang.Object)
     */
    public void setSelectedItem(Object item) {
        int idx = m_items.indexOf(item);
        if ( idx < 0 ) return;
        
        if ( !isMultipleSelection() && idx == getMinSelectionIndex() )
            return;
        
        super.setSelectionInterval(idx,idx);
        fireDataEvent(this,ListDataEvent.CONTENTS_CHANGED,-1,-1);
    }
    
    public Object[] getSelection() {
		Vector<Object> selected = new Vector<Object>();
		int startIndex = getMinSelectionIndex();
		int endIndex = getMaxSelectionIndex();
		for (int i = startIndex; i <= endIndex; i++) {
			if (isSelectedIndex(i)) {
				selected.add(getElementAt(i));
			}
		}
		Object[] result = new Object[selected.size()];
		return selected.toArray(result);
    }
    
    /**
     * @see javax.swing.ListModel#getSize()
     */
    public int getSize() {
        return m_items.size();
    }
    
    /**
     * @see javax.swing.ListModel#getElementAt(int)
     */
    public Object getElementAt(int idx) {
        return m_items.get(idx);
    }

	public double getDoubleAt(int idx) {
		Object element = m_items.get(idx);
		Class<?> clazz = element.getClass();
		if (clazz.equals(Double.class)) {
			return ((Double)element).doubleValue();
		} else if (clazz.equals(Integer.class)) {
			return ((Integer)element).doubleValue();
		} else if (clazz.equals(Float.class)) {
			return ((Float)element).doubleValue();
		} else if (clazz.equals(Long.class)) {
			return ((Long)element).doubleValue();
		} else {
			// unsupported data type for Double
			s_logger.warning("Can not convert "+element+" to a double");
		}
		return 0.0;
	}
	
    public int indexOf(Object element) {
    	
    	int index = m_items.indexOf(element);
    	
    	// Workaround for a Bug:  
    	// Prefuse is creating a class of type Integer even though data is Ordinal. 
    	// We are expecting a String.  
    	// Workaround is to create an Integer object from the String and compare.
    	if (index == -1) {
    		Object item = m_items.get(0);
    		if ((element instanceof String) && (item instanceof Integer)) {
    			Integer elementAsInt = Integer.parseInt((String)element);
    			for (Object object : m_items) {
					if (object.equals(elementAsInt)) {
						return m_items.indexOf(object);
					}
				}
    		} else if ((element instanceof String) && (item instanceof Double)) {
    			Double elementAsDouble = Double.parseDouble((String)element);
    			for (Object object : m_items) {
					if (object.equals(elementAsDouble)) {
						return m_items.indexOf(object);
					}
				}
    		}
    	}
    	
        return index;
    }
    
    /**
     * @see javax.swing.MutableComboBoxModel#addElement(java.lang.Object)
     */
    public void addElement(Object item) {
        m_items.add(item);
        int sz = m_items.size()-1;
        fireDataEvent(this,ListDataEvent.INTERVAL_ADDED,sz,sz);
        if ( sz >= 0 && isSelectionEmpty() && item != null )
            setSelectedItem(item);
    }
    
    /**
     * @see javax.swing.MutableComboBoxModel#insertElementAt(java.lang.Object, int)
     */
    public void insertElementAt(Object item, int idx) {
        m_items.add(idx, item);
        fireDataEvent(this,ListDataEvent.INTERVAL_ADDED,idx,idx);
    }
    
    /**
     * @see javax.swing.MutableComboBoxModel#removeElement(java.lang.Object)
     */
    public void removeElement(Object item) {
        int idx = m_items.indexOf(item);
        if ( idx >= 0 )
            removeElementAt(idx);
    }
    
    /**
     * @see javax.swing.MutableComboBoxModel#removeElementAt(int)
     */
    public void removeElementAt(int idx) {
        if ( !isMultipleSelection() && idx == getMinSelectionIndex() ) {
            int sidx = ( idx==0 ? getSize()==1 ? -1 : idx+1 : idx-1 );
            Object sel = ( sidx == -1 ? null : m_items.get(sidx) );
            setSelectedItem(sel);
        }
    
        m_items.remove(idx);
        fireDataEvent(this,ListDataEvent.INTERVAL_REMOVED,idx,idx);
    }
    
    // --------------------------------------------------------------------
    // List Data Listeners
    
    /**
     * @see javax.swing.ListModel#addListDataListener(javax.swing.event.ListDataListener)
     */
    public void addListDataListener(ListDataListener l) {
        if ( !m_lstnrs.contains(l) )
            m_lstnrs.add(l);
    }
    
    /**
     * @see javax.swing.ListModel#removeListDataListener(javax.swing.event.ListDataListener)
     */
    public void removeListDataListener(ListDataListener l) {
        m_lstnrs.remove(l);
    }
    
    /**
     * Fires a change notification in response to changes in the ElementListModel.
     */
    protected void fireDataEvent(Object src, int type, int idx0, int idx1) {
        Object[] lstnrs = m_lstnrs.getArray();
        if ( lstnrs.length > 0 ) {
            ListDataEvent e = new ListDataEvent(src, type, idx0, idx1);
            for ( int i=0; i<lstnrs.length; ++i ) {
                ((ListDataListener)lstnrs[i]).contentsChanged(e);
            }
        }
    }

} // end of class ElementListModel