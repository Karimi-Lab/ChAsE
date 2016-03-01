package still.operators;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.io.Serializable;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import still.data.FloatIndexer;
import still.data.Function;
import still.data.Group;
import still.data.Map;
import still.data.Operator;
import still.data.Table;
import still.data.TableEvent;
import still.data.TableEvent.TableEventType;
import still.gui.EnumUtils;
import still.gui.OPAppletViewFrame;
import still.gui.OperatorView;
import still.gui.PHeatMapClusterPainter;

public class HeatMapClusterOp extends Operator implements Serializable
{
	private static final long serialVersionUID = -6874872676907052211L;

	class ColumnInfo
	{
		int			m_iIndex;
		boolean 	m_bSelected;
		boolean		m_bCulled;
		ColType		m_NewColType;
	}
	
	ColumnInfo[] m_ColumnInfo	= null;
	int[] m_SortedIndex = null;

	public enum SortType
	{
		NONE,
		SUM,
		MAX,
		PEAK_DIM
	}
	public SortType m_SortType = SortType.NONE;
	

	public HeatMapClusterOp( Table newTable, boolean isActive, String paramString )
	{
		this( newTable, isActive );
	}
	
	public HeatMapClusterOp( Table newTable, boolean isActive )
	{
		super( newTable );
		initColumnIndex();
		this.isActive = isActive;
		if( isActive )
		{
			this.updateMap();
			this.updateFunction();
			isLazy  		= true;
			setView( new HeatMapClusterView( this ) );
		}
	}

	public static String getMenuName()
	{
		return "View:HeatMapCluster";
	}

	public void setMeasurement( int point_idx, int dim, double value )
	{
		input.setMeasurement(m_SortedIndex[point_idx], m_ColumnInfo[dim].m_iIndex, value);
	}

	public ColType getColType( int dim )
	{
		return m_ColumnInfo[dim].m_NewColType;
	}

	public String getColName( int dim )
	{
		return input.getColName(m_ColumnInfo[dim].m_iIndex);
	}

	public String toString() {
		
		return "[View:HeatMapCluster]";
	}
	
	public String getSaveString( ) {
		
		return "";
	}
	
	void initColumnIndex()
	{
		if (m_ColumnInfo == null || m_ColumnInfo.length != input.columns())
		{
			m_ColumnInfo = new ColumnInfo[input.columns()];
			for (int i = 0; i < input.columns(); i++)
			{
				m_ColumnInfo[i] = new ColumnInfo();
				m_ColumnInfo[i].m_iIndex = i;
				m_ColumnInfo[i].m_bSelected = false;
				m_ColumnInfo[i].m_bCulled   = false;
				m_ColumnInfo[i].m_NewColType = input.getColType(i);
			}
		}
		
		if (m_SortedIndex == null || m_SortedIndex.length != input.rows())
		{
			m_SortedIndex = new int[input.rows()];
			for (int i = 0; i < input.rows(); i++)
			{
				m_SortedIndex[i] = i;
			}
		}
	}
	
	public void setSelectedCols(int[] selectedCols)
	{
		for (int i = 0; i < m_ColumnInfo.length; i++)
		{
			m_ColumnInfo[i].m_bSelected = false;
		}
		
		if (selectedCols != null)
		{
			for (int i = 0; i < selectedCols.length; i++)
			{
				m_ColumnInfo[selectedCols[i]].m_bSelected = true;
			}
		}
	}

	public void sortRowsBySelectedColumns(SortType criteria)
	{
		boolean bAnyColSelected = false;
		double dVal[] = null;
		if (criteria != SortType.NONE)
		{
			dVal = new double[input.rows()];
			for (int i = 0; i < input.rows(); i++)
			{
				double dPeakVal = Double.NEGATIVE_INFINITY;
				for (int col = 0; col < m_ColumnInfo.length; col++)
				{
					if (m_ColumnInfo[col].m_bSelected)
					{
						bAnyColSelected = true;
						double dValue = input.getMeasurement(i, m_ColumnInfo[col].m_iIndex);
						switch (criteria)
						{
							case SUM:
								dVal[i] += dValue;
								break;
							case MAX:
								dVal[i] = Math.max(dVal[i], dValue);
								break;
							case PEAK_DIM:
								if (dPeakVal < dValue)
								{
									dPeakVal = dValue;
									dVal[i] = col * 1000000 + dPeakVal;
								}
								break;
						}
					}
				}
			}
		}
		
		if (criteria == SortType.NONE || !bAnyColSelected)
		{
			for (int i = 0; i < m_SortedIndex.length; i++)
			{
				m_SortedIndex[i] = i;
			}
		}
		else
		{
			for (int i = 0; i < dVal.length; i++)
			{
				dVal[i] = -dVal[i]; // to inverse the sort order from high to low
			}
			
			m_SortedIndex = FloatIndexer.sortFloats(dVal);
		}
		
		super.tableChanged( new TableEvent(this, TableEvent.TableEventType.TABLE_CHANGED ), true);
	}
	
	public void maskSelectedColumns()
	{
		for (int col = 0; col < m_ColumnInfo.length; col++)
		{
			if (m_ColumnInfo[col].m_bSelected)
			{
				if (m_ColumnInfo[col].m_NewColType == ColType.METADATA)
				{
					m_ColumnInfo[col].m_NewColType = ColType.NUMERIC;
				}
				else
				{
					m_ColumnInfo[col].m_NewColType = ColType.METADATA;
				}
			}
		}
		super.tableChanged( new TableEvent(this, TableEvent.TableEventType.TABLE_CHANGED ), true);
	}
	
	public void activate()
	{
		this.isActive = true;
		this.updateMap();
		this.updateFunction();
		isLazy  		= true;
		setView( new HeatMapClusterView( this ) );
	}
	
	@Override
	public void updateFunction()
	{
		function = new SortFunction(this);
	}
	
	public class SortFunction implements Function
	{
		private HeatMapClusterOp m_Operator = null;
		
		public SortFunction(HeatMapClusterOp op)
		{
			m_Operator = op;
		}
		
		@Override
		public Table apply(Group group) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public double compute(int row, int col) 
		{
			return m_Operator.input.getMeasurement(m_Operator.m_SortedIndex[row], m_Operator.m_ColumnInfo[col].m_iIndex);
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
			// TODO Auto-generated method stub
			return null;
		}
	}

	@Override
	public void updateMap()
	{
		int dims = input.columns();
		assert(dims == m_ColumnInfo.length);
		boolean[][] bmap = new boolean[dims][dims];
		for( int i = 0; i < dims; i++ )
		{
			bmap[m_ColumnInfo[i].m_iIndex][i] = true;
		}
		map = new Map(bmap);
	}

	public void tableChanged( TableEvent te ) 
	{
		initColumnIndex();
		super.tableChanged(te);
		if (te.type != TableEventType.ATTRIBUTE_CHANGED)
		{
			((HeatMapClusterView)getView()).buildGUI();
		}
		sortRowsBySelectedColumns(m_SortType);
		//loadOperatorView();
		if( this.isActive() )
		{
			SwingUtilities.invokeLater(new Runnable()
			{
		        public void run()
		        {
		        	PHeatMapClusterPainter pHMPainter = ((PHeatMapClusterPainter)((OPAppletViewFrame)((HeatMapClusterView)getView()).getViewFrame()).procApp); 
		    		pHMPainter.redraw();
		        }
			});
		}
	}

	public void loadOperatorView()
	{
		setView( new HeatMapClusterView( this ) );
	}
	
	public class HeatMapClusterView extends OperatorView implements ChangeListener, ListSelectionListener
	{
		private static final long serialVersionUID = -5919900799213575720L;

		PHeatMapClusterPainter m_HMCPainter = null;
		JCheckBox m_CheckBoxMetaData = null;
		
		HeatMapClusterOp m_Operator;
		
		JList m_ListCols = null;
		JPanel m_PanelMain = null;
		JComboBox m_ComboSortType  = null;
		public HeatMapClusterView(Operator op)
		{
			super(op);
			m_Operator = (HeatMapClusterOp) op;
			m_HMCPainter = new PHeatMapClusterPainter(op);
			vframe = new OPAppletViewFrame("E"+op.getExpressionNumber()+":"+op, m_HMCPainter );			
			vframe.addComponentListener(this);
			m_PanelMain = new JPanel(new BorderLayout(5,5));
			this.setLayout(new BorderLayout(5,5));
			this.add(m_PanelMain, BorderLayout.CENTER);
			this.setBorder(	BorderFactory.createEmptyBorder(10, 10, 10, 10));
			buildGUI();
		}
		
		
		void buildGUI()
		{
			m_PanelMain.removeAll();
//			JPanel  panelMain = null;
//			panelMain = new JPanel();
//			this.add(panelMain, BorderLayout.CENTER);

			DefaultListModel listModelSelected = new DefaultListModel();
			for (int i = 0; i < m_Operator.input.columns(); i++ )
			{
				listModelSelected.addElement(Integer.toString(i));
			}
			
			m_ListCols = new JList(listModelSelected);
			m_ListCols.addListSelectionListener(this);
			updateListCols();
			
			JButton buttonSort = new JButton("Sort Selected");
			buttonSort.setActionCommand("SORT");
			buttonSort.addActionListener(this);

			JButton buttonMask = new JButton("Toggle Type");
			buttonMask.setActionCommand("MASK");
			buttonMask.addActionListener(this);

			JButton buttonReset = new JButton("Reset");
			buttonReset.setActionCommand("RESET");
			buttonReset.addActionListener(this);

			JPanel panelButtons = new JPanel(new GridLayout(5, 1));
			m_ComboSortType  = EnumUtils.getComboBox(SortType.values(), m_SortType, "sort selection by:", this);
			panelButtons.add(m_ComboSortType);
			panelButtons.add(buttonSort);
			panelButtons.add(buttonMask);
			panelButtons.add(buttonReset);
			
			m_PanelMain.add(new JScrollPane(m_ListCols), BorderLayout.CENTER);
			m_PanelMain.add(panelButtons, BorderLayout.EAST);
		}
		
		void updateListCols()
		{
			for (int i = 0; i < m_Operator.input.columns(); i++ )
			{
				((DefaultListModel) m_ListCols.getModel()).set(i, 
						"[" + Integer.toString(m_Operator.m_ColumnInfo[i].m_iIndex) + "] " +
						m_Operator.getColName(i) + " : " +
						m_Operator.getColType(i).toString());
			}
		}
		public void actionPerformed(ActionEvent e)
		{
			if( e.getActionCommand().equalsIgnoreCase("SORT") )
			{
				m_Operator.sortRowsBySelectedColumns(m_SortType);
				//m_Operator.tableChanged( new TableEvent(m_Operator, TableEvent.TableEventType.TABLE_CHANGED ), true);
			}
			else if( e.getActionCommand().equalsIgnoreCase("MASK") )
			{
				m_Operator.maskSelectedColumns();
				updateListCols();
				//m_Operator.tableChanged( new TableEvent(m_Operator, TableEvent.TableEventType.TABLE_CHANGED ), true);
			}
			else if( e.getActionCommand().equalsIgnoreCase("RESET") )
			{
				m_Operator.m_ColumnInfo = null;
				m_Operator.m_SortedIndex = null;
				m_Operator.initColumnIndex();
				m_Operator.tableChanged( new TableEvent(m_Operator, TableEvent.TableEventType.TABLE_CHANGED ), true);
				m_Operator.setSelectedCols(m_ListCols.getSelectedIndices());
				updateListCols();
			}
			else if (e.getSource() instanceof JComboBox)
			{
				m_SortType = SortType.values()[m_ComboSortType.getSelectedIndex()];
			}
		}

		@Override
		public void stateChanged(ChangeEvent e)
		{
		}

		@Override
		public void valueChanged(ListSelectionEvent e)
		{
			m_Operator.setSelectedCols(m_ListCols.getSelectedIndices());
			//int [] m_SelectedIndices = m_ListCols.getSelectedIndices();
			//m_ListCols.setSelectedIndices(m_SelectedIndices);
		}
	}
}
