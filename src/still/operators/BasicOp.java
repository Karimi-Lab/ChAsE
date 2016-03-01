package still.operators;

import java.awt.event.ActionEvent;
import java.io.Serializable;
import java.util.ArrayList;

import javax.swing.SwingUtilities;

import still.data.Function;
import still.data.Group;
import still.data.Map;
import still.data.Operator;
import still.data.Table;
import still.data.TableEvent;
import still.data.TableEvent.TableEventType;
import still.gui.OPAppletViewFrame;
import still.gui.OperatorView;
import still.gui.PBasicPainter;

public class BasicOp extends Operator implements Serializable
{
	private static final long serialVersionUID = 703042345547477138L;

	/// new output columns of the operator.
	protected double [][] m_NewColumns 	= null;
	String [] 	m_NewColumnNames = null;
	ColType [] 	m_NewColumnTypes = null;
	
	/// whether to append the new columns to the input table
	boolean m_bAppendInput 	= true;

	public BasicOp( Table newTable, boolean isActive, String paramString )
	{
		this( newTable, isActive );
		//TODO parse the parameters 
		//String[] params = paramString.split(",");
	}
	
	public BasicOp( Table newTable, boolean isActive )
	{
		super( newTable );
		this.isActive = isActive;
		if( isActive )
		{
			computeNewColumns();
			updateMap();
			updateFunction();
			function = new BasicFunction(this);
			isLazy  		= true;
		}
	}
	
	public static String getMenuName()
	{
		return "View:BasicFunc";
	}

	public String toString()
	{
		return "[View:Basic]";
	}
	
	public String getSaveString( ) 
	{//TODO operator parameters to save
		return ""; 
	}
	
	public void activate()
	{
		this.isActive = true;
		this.updateMap();
		function = new BasicFunction(this);
		isLazy  		= true;
		
		loadOperatorView();
	}
	
	@Override
	public String getColName( int dim )
	{
		int newDim = m_bAppendInput ? dim - input.columns() : dim;
		if (newDim < 0)
		{
			return input.getColName(dim);
		}
		else if (m_NewColumnNames != null && newDim < m_NewColumnNames.length)
		{
			return m_NewColumnNames[newDim];
		}
		else
		{
			return super.getColName(dim);
		}
	}
	
	@Override
	public ColType getColType( int dim )
	{
		int newDim = m_bAppendInput ? dim - input.columns() : dim;
		if (newDim < 0)
		{
			return input.getColType(dim);
		}
		else if (m_NewColumnTypes != null && newDim < m_NewColumnTypes.length)
		{
			return m_NewColumnTypes[newDim];
		}
		else
		{
			return super.getColType(dim);
		}
	}
	
	@Override
	public void updateFunction()
	{
		function = new BasicFunction(this);
	}

	public int rows()
	{
		if (m_bAppendInput)
			return input.rows();
		else if (m_NewColumns != null)
			return m_NewColumns.length;

		return 0;
	}
	
	public void setMeasurement( int point_idx, int dim, double value )
	{
		if (m_bAppendInput)
		{
			if (dim >= input.columns())
			{
				if (m_NewColumns != null &&  point_idx < m_NewColumns.length)
				{
					m_NewColumns[point_idx][dim - input.columns()] = value;
				}
			}
			else
			{
				input.setMeasurement(point_idx, dim, value);
			}
		}
		else if (m_NewColumns != null &&  m_NewColumns[0] != null && dim < m_NewColumns[0].length )
		{
			m_NewColumns[point_idx][dim] = value;
		}
	}
	

	public class BasicFunction implements Function
	{
		private BasicOp m_Operator = null;
		
		public BasicFunction(BasicOp op)
		{
			m_Operator = op;
		}
		
		@Override
		public Table apply(Group group) {
			return null;
		}

		@Override
		public double compute(int row, int col) 
		{
			if (m_bAppendInput)
			{
				if (col >= m_Operator.input.columns())
				{
					if (m_Operator.m_NewColumns != null &&  row < m_Operator.m_NewColumns.length)
					{
						return m_Operator.m_NewColumns[row][col - m_Operator.input.columns()];
					}
				}
				else
				{
					return m_Operator.input.getMeasurement(row, col);
				}
			}
			else if (m_Operator.m_NewColumns != null &&  m_Operator.m_NewColumns[0] != null && col < m_Operator.m_NewColumns[0].length )
			{
				return m_Operator.m_NewColumns[row][col];
			}
			return 0;
		}

		@Override
		public Group inverse(Table dims) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public double[] invert( Map map, int row, int col, double value )
		{
			double[] ret = new double[1];
			ret[0] = value;
			return ret;
		}

		@Override
		public int[] outMap() {
			return null;
		}
	}

	@Override
	public void updateMap()
	{
		if (input == null)
			return;
		
		//TODO: (Optional) specify the mapping between dimensions or leave as is.
		int outdims = (m_NewColumns == null || m_NewColumns.length == 0) ? 0 : m_NewColumns[0].length;
		
		if (m_bAppendInput)
		{
			if (m_NewColumns == null || m_NewColumns.length != input.rows())
				outdims = 0;
			map = Map.fullBipartiteAppend(getNumericIndices(input), 
											getNonNumericIndices( input ),									
											input.columns(),
											outdims);
		}
		else
		{
			ArrayList<Integer> numericIndicesFromOutdims = new ArrayList<Integer>();
			for( int i = 0; i < outdims; i++ )
				numericIndicesFromOutdims.add(i);

			ArrayList<Integer> nonNumericIndicesFromOutdims = new ArrayList<Integer>();
			for( int i = outdims; i < outdims+getNonNumericIndices( input ).size(); i++ )
				nonNumericIndicesFromOutdims.add(i);
			
			map = Map.fullBipartiteExcept( getNumericIndices(input), 
											numericIndicesFromOutdims,			
											getNonNumericIndices( input ),
											nonNumericIndicesFromOutdims,
											input.columns(), outdims + getNonNumericDims(input));
		}
	}

	protected void computeNewColumns()
	{
		//TODO: create/recompute the operator output here
		m_NewColumns = null;
		m_NewColumns = new double[input.rows()][1];

		if( this.isActive() )
		{
			SwingUtilities.invokeLater(new Runnable()
			{
		        public void run()
		        {
		        	PBasicPainter painter = ((PBasicPainter)((OPAppletViewFrame)((BasicView)getView()).getViewFrame()).procApp); 
		    		painter.invokeRedraw();
		        }
			});
		}
	}

	public void tableChanged( TableEvent te ) 
	{
		if (te.type != TableEventType.ATTRIBUTE_CHANGED)
		{
			computeNewColumns();
			updateMap();
			updateFunction();
		}
		super.tableChanged(te, true);
	}
	
	// TODO: called by the constructor of the derived class
	public void loadOperatorView()
	{
		setView( new BasicView( this ) );
	}
	
	public class BasicView extends OperatorView
	{
		private static final long serialVersionUID = -5919900799213575720L;

		
		BasicOp m_Operator = null;
		
		protected PBasicPainter m_Painter = null;
		
		public BasicView(Operator op)
		{
			super(op);
			m_Operator = (BasicOp) op;
		}
		
		//TODO: called by the constructor of the derived class
		protected void init()
		{
			createPainter(m_Operator);
			if (m_Painter != null)
			{
				vframe = new OPAppletViewFrame("E" + m_Operator.getExpressionNumber()+":" + m_Operator, m_Painter);			
				vframe.addComponentListener(this);
				vframe.setVisible(true);
			}
			
			buildGUI();
		}
		
		protected void createPainter(Operator op)
		{
			//TODO override this function
			m_Painter = new PBasicPainter(op);
		}
		
		protected void buildGUI()
		{
			//TODO override this function and add operator GUI controls 
			this.removeAll();
		}
		
		public void actionPerformed(ActionEvent e)
		{
			//TODO: code to handle the GUI actions
		}
	}
}
