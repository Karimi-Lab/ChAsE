package heidi.plot;

import prefuse.data.expression.CompositePredicate;

public interface IDynamicUpdate {

	public void setFilterPredicate(CompositePredicate predicate);
	public void setHighlightPredicate(CompositePredicate predicate);
	
}
