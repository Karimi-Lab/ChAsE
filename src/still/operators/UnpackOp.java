package still.operators;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Hashtable;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.*;

import still.data.*;
import still.data.Table.ColType;
import still.gui.OperatorView;

public class UnpackOp extends Operator implements Serializable
{
	private static final long serialVersionUID = -525365751923758479L;
	int m_iNumGroups = 8;
	int m_iGroupDim = 30;
	
	/**
	 * Simple Constructor 
	 */
	public UnpackOp(Table newInput, boolean isActive)
	{
		super(newInput);
		this.isActive = isActive;
		if (isActive ) {
			
			updateMap();
			updateFunction();
			isLazy  		= true;
			setView( new GroupView( this ) );		
		}
	}

	/**
	 * Constructor: Parameterized Operator creation
	 * @param newInput
	 * @param isActive
	 * @param paramString
	 */
	public UnpackOp( Table newInput, boolean isActive, String paramString ) {
		
		super(newInput);
		
		// extract parameters
		String[] params = paramString.split(",");
		m_iNumGroups = Integer.parseInt(params[0]);
		m_iGroupDim = Integer.parseInt(params[1]);
		
		this.isActive = isActive;
		if (isActive ) {
			
			updateMap();
			updateFunction();
			isLazy  		= true;
			setView( new GroupView( this ) );		
		}
	}
	
	/**
	 * @return String menu name of the operator 
	 */
	public static String getMenuName()
	{
		return "Attrib:Unpack";
	}
	
	public String toString()
	{
		return "[Attrib:Unpack]";
	}

	public int rows()
	{
		return input.rows() * m_iNumGroups;
	}
	
	public int columns()
	{
		return input.columns() - ((m_iNumGroups - 1) * m_iGroupDim) + 2; // the last two columns are _point_idx_ and _group_idx_
	}

	/** 
	 * Sets the value of an attribute of a data point in the table.
	 * @param point_idx  index of the data point (i.e. row index).
	 * @param dim        index of the attribute dimension (i.e. column index).
	 * @param value      value to be set.
	 */
	public void setMeasurement( int point_idx, int dim, double value )
	{
		int iGroup = point_idx % m_iNumGroups;
		int iActualRow = point_idx / m_iNumGroups;
		int iActualDim = -1;
		if (dim < m_iGroupDim)
		{
			iActualDim = iGroup*m_iGroupDim + dim;
		}
		else
		{
			iActualDim = (m_iNumGroups - 1) * m_iGroupDim + dim;
		}
		if (iActualDim >= 0 && iActualDim < input.columns())
		{
			input.setMeasurement(iActualRow, iActualDim, value);
		}
	}
	
	/**
	 * Returns the type of a table column (NUMERIC, ORDINAL, CATEGORICAL, ATTRIBUTE)
	 * @param dim   index of the column
	 */
	public ColType getColType( int dim )
	{
		if (dim < m_iGroupDim)
		{
			return input.getColType(dim);
		}
		else if ((m_iNumGroups - 1) * m_iGroupDim + dim < input.columns())
		{
			return input.getColType((m_iNumGroups - 1) * m_iGroupDim + dim);
		}
		return ColType.NUMERIC;
	}

	/**
	 * Gets called when an operator is activated.
	 */
	public void activate()
	{
		isActive = true;
		updateMap();
		updateFunction();
		isLazy  		= true;
		setView( new GroupView( this ) );		
	}
	
	/**
	 * 
	 */
	public String getSaveString( ) {
		
		String saveString = "";
		saveString += m_iNumGroups;
		saveString += ",";
		saveString += m_iGroupDim;
		return saveString;
	}

	/**
	 * Returns the string name of a column.
	 */
	public String getColName( int dim )
	{
		if (dim < m_iGroupDim)
		{
			return input.getColName(dim);
		}
		else if ((m_iNumGroups - 1) * m_iGroupDim + dim < input.columns())
		{
			return input.getColName((m_iNumGroups - 1) * m_iGroupDim + dim);
		}
		else if (dim == columns() - 2)
		{
			return "_point_idx_";
		}
		else if (dim == columns() - 1)
		{
			return "_group_idx_";
		}
			
		return null;
	}


	/**
	 * Function to filter the data using their cluster id. 
	 * @author hyounesy
	 */
	
	public class GroupFunction implements Function, Serializable
	{
		private static final long serialVersionUID = -7422000613556512121L;
		UnpackOp 			m_UnpackOp = null;
		public GroupFunction(UnpackOp op)
		{
			m_UnpackOp  = op;
		}

		@Override
		public double compute(int row, int col) 
		{
			int iGroup = row % m_iNumGroups;
			int iActualRow = row / m_iNumGroups;
			if (col == columns() - 2)
			{
				return iActualRow;
			}
			else if (col == columns() - 1)
			{
				return iGroup;
			}
			
			int iActualDim = -1;
			if (col < m_iGroupDim)
			{
				iActualDim = iGroup*m_iGroupDim + col;
			}
			else
			{
				iActualDim = (m_iNumGroups - 1) * m_iGroupDim + col;
			}
			
			assert (iActualDim >= 0 && iActualDim < input.columns());
			if (iActualDim >= 0 && iActualDim < input.columns())
			{
				return input.getMeasurement(iActualRow, iActualDim);
			}
			
			return 0;
		}

		@Override
		public Table apply(Group group) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Group inverse(Table dims) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public double[] invert(Map map, int row, int col, double value) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public int[] outMap() {
			// TODO Auto-generated method stub
			return null;
		}

//		public double[] invert( Map map, int row, int col, double value ) ...
	}
	
	
	@Override
	public void updateFunction()
	{
		function = new GroupFunction(this);
	}
	
	public void updateMap()
	{
		boolean[][] b = new boolean[input.columns()][columns()];
		for( int i = 0; i < m_iGroupDim; i++ )
		{
			for (int j = 0; j < m_iNumGroups; j++)
			{
				b[j * m_iGroupDim + i][i] = true;
			}
		}
		for( int i = m_iGroupDim; i < columns() - 2; i++ )
		{
			b[(m_iNumGroups - 1)* m_iGroupDim + i][i] = true;
		}
		map = new Map(b);
	}

	/**
	 * The GUI view class for the group operator.
	 *  
	 * @author hyounesy
	 *
	 */
	public class GroupView extends OperatorView implements ChangeListener
	{
		private static final long serialVersionUID = -861658581940957767L;
		JSpinner 	m_SpinnerRepeatCalcAll 	= null;
		Operator op = null;

		/**
		 *  Construct and populate the gui
		 */
		public void buildGui()
		{
			this.setLayout( new BorderLayout(5,5) );
			JButton btn = new JButton("Test");
			this.add(btn);
			
//			m_SpinnerRepeatCalcAll   = new JSpinner(new SpinnerNumberModel(op.m_ClusterTable.m_iRepeatCalculateAll, 0, 100, 1));
//			calcAllPanel.add(m_SpinnerRepeatCalcAll);
//			m_SpinnerRepeatCalcAll.addChangeListener(this);
		}
		
		public GroupView(Operator o)
		{
			super(o);
			op = o;
			buildGui();
		}
		
		@Override
		public void stateChanged(ChangeEvent e)
		{
			if (e.getSource() == m_SpinnerRepeatCalcAll)
			{
				//op.m_ClusterTable.m_iRepeatCalculateAll = ((Integer)m_SpinnerRepeatCalcAll.getValue());
			}
		}
	}
}