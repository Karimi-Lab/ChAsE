package still.operators;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import still.data.Operator;
import still.data.Table;
import still.data.TableEvent;
import still.data.TableFactory;

public class SPMOp extends BasicOp
{
	private static final long serialVersionUID = 2396076438071834121L;

	int m_iNumGroups = 1;
	int m_iMaxLevels = 3;		// height of the tree 
	int m_iBranchFactor = 2; 	// branching factor (degree) of the tree
	int m_iNumHistBins = 4;  	// number of histogram bins for multiresolution histogram
	
	boolean m_bPeak 		= true;
	boolean m_bPeakOffset 	= true;
	boolean m_bMean 		= true;
	boolean m_bHistogram 	= true;
	
	// Spatial Pyramid Matching: "Beyond Bags of Features: Spatial Pyramid Matching for Recognizing Natural Scene Categories"
	// "Multiresolution histograms and their use for recognition"
	enum SPMMethod
	{
		PEAK_OFFSET,
		PEAK_VALUE,
		MEAN,
		HISTOGRAM
	}
	
	public SPMOp( Table newTable, boolean isActive, String paramString )
	{
		this( newTable, isActive );
	}
	
	public SPMOp( Table newTable, boolean isActive )
	{
		super(newTable, isActive);
		loadOperatorView();
	}

	public static String getMenuName()
	{
		return "Data:SpatialPyramidMatching";
	}

	public String toString()
	{
		return "[Data:SPM]";
	}
	
	public String getSaveString( ) {
		
		return "";
	}
	
	public void activate()
	{
		this.isActive = true;
		this.updateMap();
		function = new BasicFunction(this);
		isLazy  		= true;
		setView( new SPMView( this ) );
	}

	@Override
	protected void computeNewColumns()
	{
		if (m_iNumGroups == 0)
			return;
		
		ArrayList<double[][]> spmTables = new ArrayList<double[][]>();
		ArrayList<String> allColNames = new ArrayList<String>();
		
		int iGroupDim = input.columns() / m_iNumGroups;
		//double[][] dataTable = input.getTable();

		double[][] dataTable = new double[input.rows()][iGroupDim];
		
		for (int gi = 0; gi < m_iNumGroups; gi++)
		{
			for (int irow = 0; irow < input.rows(); irow++)
			{
				for (int icol = 0; icol < iGroupDim; icol++)
				{
					dataTable[irow][icol] = input.getMeasurement(irow, icol + gi * iGroupDim);
				}
			}
			
			if (m_bPeakOffset)
			{
				double[][] tableO = calcSPM(dataTable, SPMMethod.PEAK_OFFSET, m_iMaxLevels, m_iBranchFactor, m_iNumHistBins);
				spmTables.add(tableO);
				for (int i = 0; i < tableO[0].length; i++)
					allColNames.add("O["+gi+"]"+i);
			}
			if (m_bPeak)
			{
				double[][] tableP = calcSPM(dataTable, SPMMethod.PEAK_VALUE, m_iMaxLevels, m_iBranchFactor, m_iNumHistBins);
				spmTables.add(tableP);
				for (int i = 0; i < tableP[0].length; i++)
					allColNames.add("P["+gi+"]"+i);
			}
			if (m_bMean)
			{
				double[][] tableM = calcSPM(dataTable, SPMMethod.MEAN, m_iMaxLevels, m_iBranchFactor, m_iNumHistBins);
				spmTables.add(tableM);
				for (int i = 0; i < tableM[0].length; i++)
					allColNames.add("M["+gi+"]"+i);
			}
			if (m_bHistogram)
			{
				double[][] tableH = calcSPM(dataTable, SPMMethod.HISTOGRAM, m_iMaxLevels, m_iBranchFactor, m_iNumHistBins);
				spmTables.add(tableH);
				for (int i = 0; i < tableH[0].length; i++)
					allColNames.add("H["+gi+"]"+i);		
			}
		}
		
		if (spmTables.size() > 0)
		{
			m_NewColumns = TableFactory.mergeTables(spmTables);
			m_NewColumnNames = allColNames.toArray(new String[allColNames.size()]);
		}
		else
		{
			m_NewColumns = null;
			m_NewColumnNames = null;
		}
	}

	public static double[][] calcSPM(double[][] table, SPMMethod method, int iMaxLevel, int iBranch, int iHistBins)
	{
		
		int iNumRows = table.length;
		int iNumCols = table[0].length;
		
		double dMaxValue = Double.NEGATIVE_INFINITY; // maximum value, used for histogram
		double[] dHist = new double[iHistBins];
		if (method == SPMMethod.HISTOGRAM)
		{
			for (int iRow = 0; iRow < iNumRows; iRow++)
			{
				for (int iCol = 0; iCol < iNumCols; iCol++)
				{
					dMaxValue = Math.max(table[iRow][iCol], dMaxValue);
				}
			}
		}
		
		int iCellDim = method == SPMMethod.HISTOGRAM ? iHistBins : 1; // number of features required per cell
		int iTotalCells = (int)(Math.pow(iBranch, iMaxLevel+1) - 1) / (iBranch - 1);
		
		double[][] result = new double[iNumRows][iTotalCells * iCellDim];
		
		int iLevelCells = 1; // number of cells per level
		int iCurrCell = 0;
		for (int l = 0; l <= iMaxLevel; l++)
		{
			double fW = Math.pow(2, l - iMaxLevel); 
			int col1 = 0; // start column index for this cell 
			int col2 = 0; // end column index for this cell
			for (int cell = 0; cell < iLevelCells; cell++)
			{
				col1 = col2;
				col2 = (cell + 1) * iNumCols / iLevelCells; 
				
				for (int iRow = 0; iRow < iNumRows; iRow++)
				{
					Arrays.fill(dHist, 0);
					double dPeakValue = 0;
					for (int col = col1; col < col2; col++)
					{
						double dValue = table[iRow][col];
						
						boolean bIgnoreZeros = false;
						if (method == SPMMethod.HISTOGRAM && (!bIgnoreZeros || dValue > 0))
						{
							int iHistBin = 0;
							if (bIgnoreZeros)
							{
								iHistBin = (int)(iHistBins * dValue / dMaxValue); // treat 0 value specially
							}
							else
							{
								//iHistBin = dValue <= 0 ? 0 : (int)((iHistBins - 1) * dValue / dMaxValue) + 1; // treat 0 value specially
								iHistBin = (int)((iHistBins+1) * dValue / dMaxValue) - 1;
							}
							
							if (iHistBin >= 0)
							{
								iHistBin = Math.min(iHistBin, iHistBins - 1); // clamp to avoid overflow
								dHist[iHistBin] += fW;
							}
						}
						else if (method == SPMMethod.PEAK_OFFSET)
						{
							if (dPeakValue < dValue)
							{
								dPeakValue = dValue;
								dHist[0] = fW*(1.0 * (col - col1 + 1)) / (col2 - col1);
							}
						}
						else if (method == SPMMethod.PEAK_VALUE)
						{
							dHist[0] = Math.max(fW*dValue, dHist[0]);
						}
						else if (method == SPMMethod.MEAN)
						{
							dHist[0] += fW*dValue/(col2 - col1);
						}
					}
					
					try
					{
						System.arraycopy(dHist, 0, result[iRow], iCurrCell * iCellDim, iCellDim);
					}
					catch(Exception e)
					{
						e.printStackTrace();
					}
					
				}
				
				iCurrCell++;
			}
				
			iLevelCells *= iBranch;
			
		}
		
		return result;
	}	
	@Override
	public void loadOperatorView()
	{
		setView( new SPMView( this ) );
	}
	
	public class SPMView extends BasicOp.BasicView
	{
		private static final long serialVersionUID = 4698263995759097051L;
		SPMOp m_SPMOp = null;
		JPanel masterPanel = null;
		JTextField textGroups = null;
		JTextField textBranch = null;
		JTextField textLevels = null;
		JTextField textHistBins = null;
		
		public SPMView(Operator op)
		{
			super(op);
			m_SPMOp = (SPMOp) op;
			init();
		}
		
		@Override
		protected void createPainter(Operator op)
		{
		}
		
		@Override
		protected void buildGUI()
		{
			if( masterPanel != null ) {
				this.removeAll();
				masterPanel.removeAll();
			}
			masterPanel = new JPanel( new GridLayout(0, 2, 5, 5));
			this.setLayout(new BorderLayout(5,5));
			this.add(masterPanel,"Center");
			this.setBorder(	BorderFactory.createEmptyBorder(10, 10, 10, 10));

			
			textGroups = new JTextField(Integer.toString(m_SPMOp.m_iNumGroups));
			masterPanel.add(new JLabel("Number of Groups:"));
			masterPanel.add(textGroups);

			textBranch = new JTextField(Integer.toString(m_SPMOp.m_iBranchFactor));
			masterPanel.add(new JLabel("Branching Factor:"));
			masterPanel.add(textBranch);
			
			textLevels = new JTextField(Integer.toString(m_SPMOp.m_iMaxLevels));
			masterPanel.add(new JLabel("Levels:"));
			masterPanel.add(textLevels);

			textHistBins = new JTextField(Integer.toString(m_SPMOp.m_iNumHistBins));
			masterPanel.add(new JLabel("Histogram Bins:"));
			masterPanel.add(textHistBins);

			JCheckBox checkAppend = new JCheckBox("Append Input");
			checkAppend.setActionCommand("append");
			checkAppend.setSelected( (m_SPMOp).m_bAppendInput);
			checkAppend.addActionListener(this);
			masterPanel.add(checkAppend);

			JButton buttonProcess = new JButton("Process");
			buttonProcess.setActionCommand("process");
			buttonProcess.addActionListener(this);
			masterPanel.add(buttonProcess);

			//TODO: add operator GUI controls
		}
		
		@Override
		public void actionPerformed(ActionEvent e)
		{
			if( e.getActionCommand().equalsIgnoreCase("append"))
			{
				m_SPMOp.m_bAppendInput = ((JCheckBox)e.getSource()).isSelected();
				operator.tableChanged( new TableEvent(operator, TableEvent.TableEventType.TABLE_CHANGED ) );
			}
			else if( e.getActionCommand().equalsIgnoreCase("process"))
			{
				m_SPMOp.m_iNumGroups = Integer.parseInt(textGroups.getText());
				m_SPMOp.m_iBranchFactor = Integer.parseInt(textBranch.getText());
				m_SPMOp.m_iMaxLevels = Integer.parseInt(textLevels.getText());
				m_SPMOp.m_iNumHistBins = Integer.parseInt(textHistBins.getText());
				operator.tableChanged( new TableEvent(operator, TableEvent.TableEventType.TABLE_CHANGED ) );
			}
		}
	}
}
