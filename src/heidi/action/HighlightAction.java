package heidi.action;

import heidi.visual.expression.HighlightPredicate;

import java.util.Iterator;

import prefuse.Visualization;
import prefuse.action.GroupAction;
import prefuse.data.expression.OrPredicate;
import prefuse.data.expression.Predicate;
import prefuse.visual.VisualItem;

/**
 * Action that highlights all items that meet a given Predicate
 * condition and removes highlighting from all other items.
 * 
 * Based on VisibilityFilter by Jeffrey Heer
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 * @author veronika
 */
public class HighlightAction extends GroupAction {
    
	private Predicate m_highlight;
    private Predicate m_predicate;
    
    /**
     * Create a new HighlightFilter.
     * @param p the test predicate used to determine whether item is highlighted
     */
    public HighlightAction(Predicate p) {
        setPredicate(p);
    }

    /**
     * Create a new HighlightFilter.
     * @param group the data group to process
     * @param p the test predicate used to determine whether item is highlighted
     */
    public HighlightAction(String group, Predicate p) {
        super(group);
        setPredicate(p);
    }

    /**
     * Create a new HighlightFilter.
     * @param vis the Visualization to process
     * @param group the data group to process
     * @param p the test predicate used to determine whether item is highlighted
     */
    public HighlightAction(Visualization vis, String group, Predicate p) {
        super(vis, group);
        setPredicate(p);
    }

    /**
     * Set the test predicate used to determine whether item is highlighted
     * @param p the test predicate to set
     */
    protected void setPredicate(Predicate p) {
        m_predicate = p;
        m_highlight = new OrPredicate(p, HighlightPredicate.TRUE);
    }
    
    /**
     * @see prefuse.action.Action#run(double)
     */
    public void run(double frac) {
    	// TODO - investigate concurrent modification errors
    	Iterator<?> items = m_vis.items(m_group, m_highlight);
    	while (items.hasNext()) {
    		VisualItem item = (VisualItem)items.next();
    		boolean highlight = m_predicate.getBoolean(item);
    		item.setHighlighted(highlight);
    	}
    }

} // end of class HighlightAction
