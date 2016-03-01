package heidi.visual.expression;

import prefuse.data.expression.ColumnExpression;
import prefuse.data.expression.Expression;
import prefuse.data.expression.Function;
import prefuse.data.expression.NotPredicate;
import prefuse.data.expression.Predicate;
import prefuse.visual.VisualItem;

/**
 * Expression that indicates if an item is highlighted
 * 
 * Modified from HoverPredicate by Jeffrey Heer
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 * @author veronika
 */
public class HighlightPredicate extends ColumnExpression
    implements Function
{
    /** Convenience instance for the highlight == true case. */
    public static final Predicate TRUE = new HighlightPredicate();
    /** Convenience instance for the highlight == false case. */
    public static final Predicate FALSE = new NotPredicate(TRUE);
    
    /**
     * Create a new HighlightPredicate.
     */
    public HighlightPredicate() {
        super(VisualItem.HIGHLIGHT);
    }

    /**
     * @see prefuse.data.expression.Function#getName()
     */
    public String getName() {
        return "HIGHLIGHT";
    }

    /**
     * @see prefuse.data.expression.Function#addParameter(prefuse.data.expression.Expression)
     */
    public void addParameter(Expression e) {
        throw new IllegalStateException("This function takes 0 parameters");
    }

    /**
     * @see prefuse.data.expression.Function#getParameterCount()
     */
    public int getParameterCount() {
        return 0;
    }
    
    /**
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return getName()+"()";
    }

} // end of class HighlightPredicate
