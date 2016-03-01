package still.operators;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextArea;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.rosuda.JRI.REXP;
import org.rosuda.JRI.Rengine;

import still.data.EQTLTableFactory;
import still.data.MathLibs;
import still.data.MemoryTable;
import still.data.Operator;
import still.data.Table;
import still.data.EQTLTableFactory.OutputType;
import still.data.EQTLTableFactory.TableColumns;
import still.gui.EnumUtils;
import still.gui.OPAppletViewFrame;
import still.gui.PEQTLPainter;

public class EQTLOp extends BasicOp
{
	private static final long serialVersionUID = 5118434778904392340L;

	public EQTLTableFactory.EQTLData m_EQTLData = null;
	public	MemoryTable 		m_TablePhenoType 	= null;
	public	MemoryTable 		m_TableGenoType 	= null;
	//public	MemoryTable 		m_TableResultSingle = null;

	String 		m_Path = "/Users/hyounesy/SFU/research/BioVis/eQTL/contest_dataset_2011_v2";//SFU
	//String 		m_Path = "/Users/hyounesy/SFU/research/BioVis/eQTL/biovis_2011_contest_demonstration/dataset.hard"; //SFU
	//String		m_Path = "/Users/hyounesy/SFU/Research/BioVis/eQTL/contest_dataset_2011_v2/contest_dataset_2011_v2"; //HOME
	//String		m_Path = "/Users/hyounesy/SFU/Research/BioVis/eQTL/biovis_2011_contest_demonstration/biovis_2011_contest_demonstration/dataset.easiest"; //HOME

	public boolean 				m_bUniformView = true; // uniform plot view for all plots 
	public TableColumns 		m_XCol = TableColumns.LOG_ODDS_RATIO_LOW;
	public TableColumns 		m_YCol = TableColumns.LOG_ODDS_RATIO_HIGH;
	public OutputType 			m_OutputType = OutputType.GENO_TYPE;
	public still.data.Table		m_CurrTable = null;
	public	double[]			m_ExpressionCoefficients; //[num_phenotypes + 1]
	public	boolean[]			m_bExpressionEnabled; //[num_phenotypes]
	
	
	public EQTLOp( Table newTable, boolean isActive, String paramString )
	{
		this( newTable, isActive );
	}
	
	public EQTLOp( Table newTable, boolean isActive )
	{
		super(newTable, isActive);
		
		m_EQTLData = new EQTLTableFactory.EQTLData();
		processEQTLData();
		
		loadOperatorView();
	}
	
	public void processEQTLData()
	{
		m_EQTLData.processAllFiles(m_Path);
		m_TablePhenoType = EQTLTableFactory.createPhenoTable(m_EQTLData);
		m_TableGenoType = EQTLTableFactory.createGenoTable(m_EQTLData);
		calcExpressionCoefficients();
		m_CurrTable = m_TableGenoType;
		m_OutputType = OutputType.GENO_TYPE;
	}

	public static String getMenuName()
	{
		return "View:EQTLFunc";
	}

	@Override
	public String toString()
	{
		return "[View:EQTL]";
	}
	
	@Override
	public String getSaveString( ) {
		
		return "";
	}
	
	public void activate()
	{
		this.isActive = true;
		this.updateMap();
		function = new BasicFunction(this);
		isLazy  		= true;
		setView( new EQTLView( this ) );
	}

	@Override
	protected void computeNewColumns()
	{
		//TODO: create/recompute the operator output here
		m_NewColumns = null;
	}

	public void selectPoints(Table table, String[] ids)
	{
		int iSelectionCol = table.columns() - 1; // i'm being very lazy here
		if (table == m_TableGenoType)
		{
			for (int i = 0; i < ids.length; i++)
			{
				int snp = m_EQTLData.getSNPIndex(ids[i]);
				if (snp >= 0)
				{
					m_TableGenoType.setMeasurement(snp, iSelectionCol, 1);
				}
			}
		}
		else if (table == m_TablePhenoType)
		{
			for (int i = 0; i < ids.length; i++)
			{
				int id = Integer.parseInt(ids[i]) - 1;
				if (id > 0)
				{
					m_TablePhenoType.setMeasurement(id, iSelectionCol, 1);
				}
			}
		}
	}
	
	public String[] getSelectedInfo(Table table)
	{
		ArrayList<String> selected = new ArrayList<String>();
		int iSelectionCol = table.columns() - 1; // i'm being very lazy here
		for (int i = 0; i < table.rows(); i++)
		{
			if (table.getMeasurement(i, iSelectionCol) > 0)
			{
				if (table == m_TableGenoType)
				{
					selected.add(m_EQTLData.m_SNPInfo[i].m_Name);
				}
				else if (table == m_TablePhenoType)
				{
					selected.add(Integer.toString(i + 1));
				}
			}
		}
		
		if (selected.size() > 0)
		{
			String[] output = new String[selected.size()];
			selected.toArray(output);
			return output;
		}
		return null;
	}
	
	void hideBySelection(Table table, boolean bSelected)
	{
		int iSelectionCol = EQTLTableFactory.getSelectionCol(table);
		int iColorCol = EQTLTableFactory.getColorCol(table);
		if (iSelectionCol == -1 || iColorCol == -1)
			return;
		
		for (int i = 0; i < table.rows(); i++)
		{
			if ((table.getMeasurement(i, iSelectionCol) > 0 && bSelected) || (table.getMeasurement(i, iSelectionCol) <= 0 && !bSelected))
			{
				int color = (int)table.getMeasurement(i, iColorCol);
				table.setMeasurement(i, iColorCol, color & 0x00FFFFFF);
			}
		}
    	PEQTLPainter pPainter = ((PEQTLPainter)((OPAppletViewFrame)((EQTLView)getView()).getViewFrame()).procApp);
    	pPainter.redrawAll();
		pPainter.invokeRedraw();
	}
	
	void showAll(Table table)
	{
		calcExpressionCoefficients();
		int iColorCol = EQTLTableFactory.getColorCol(table);
		if (iColorCol == -1)
			return;
		for (int i = 0; i < table.rows(); i++)
		{
			int color = (int)table.getMeasurement(i, iColorCol);
			table.setMeasurement(i, iColorCol, color | 0xFF000000);
		}
    	PEQTLPainter pPainter = ((PEQTLPainter)((OPAppletViewFrame)((EQTLView)getView()).getViewFrame()).procApp);
    	pPainter.redrawAll();
		pPainter.invokeRedraw();
	}

	/**
	 *  Calculates expression Coefficients for the phenotypes enabled in m_bExpressionEnabled
	 *  Result is stored in m_ExpressionCoefficients
	 */
	public void calcExpressionCoefficients()
	{
		double[][] expr = new double[m_EQTLData.getNumIndividuals()][m_EQTLData.getNumPhenoTypes()];
		double[] affect = new double[m_EQTLData.getNumIndividuals()];
		for (int id = 0; id < m_EQTLData.getNumIndividuals(); id++)
		{
			for (int iPh = 0; iPh < m_EQTLData.getNumPhenoTypes(); iPh++)
			{
				expr[id][iPh] = m_EQTLData.m_PhenoType[id].m_ExpressionLevel[iPh];
			}
			affect[id] = m_EQTLData.m_PhenoType[id].m_Affection;
		}
		
		Rengine re = MathLibs.getREngine();
		MathLibs.Rassign("x", expr);
		re.assign("y", affect);
		String cmd = "r<-lm(y~";
		for (int iPh = 0; iPh < m_EQTLData.getNumPhenoTypes(); iPh++)
		{
			cmd += (iPh > 0 ? "+": "") + "x[,"+(iPh+1)+"]";
		}
		cmd+=");";
		re.eval(cmd);
		//re.eval("print(r);");
		REXP rxp = re.eval("r$coef;");
		double[] result =  rxp.asDoubleArray();
		m_ExpressionCoefficients = new double[result.length];
		for (int i = 0; i < result.length; i++)
		{
			m_ExpressionCoefficients[i] = result[(i+1) % result.length]; // moves r0 to the last element
		}
	}
	
	/**
	 * Calculates the LDA projected value for an individual
	 * @param individual	id of the individual
	 * @return				projected expression level
	 */
	public double calcLDA(int individual)
	{
		double result = m_ExpressionCoefficients[m_EQTLData.getNumPhenoTypes()];
		for (int iPh = 0; iPh < m_EQTLData.getNumPhenoTypes(); iPh++)
		{
			result += m_ExpressionCoefficients[iPh] * m_EQTLData.m_PhenoType[individual].m_ExpressionLevel[iPh];
		}
		//System.out.println("["+individual+"]=" + result);
		return result;
	}

	/**
	 * Estimates the change in expression levels of an individual when changing certain snps
	 * @param individual	id of the individual
	 * @param snps			array of snp indices
	 * @param alleles		array of target alleles (same size as snps[])
	 * @return				array of size [num_phenotypes] of estimated change to each expression level
	 */
	public double[] estimateExpressionChange(int individual, int snps[], int alleles[])
	{
		return null;
	}
		
	@Override
	public void loadOperatorView()
	{
		setView( new EQTLView( this ) );
	}
	
	public class EQTLView extends BasicOp.BasicView implements ChangeListener
	{
		private static final long serialVersionUID = 4698263995759097051L;
		JTextArea	m_TextPath			= null;
		JComboBox   m_ComboDataType		= null;
		JComboBox   m_ComboXAxis		= null;
		JComboBox   m_ComboYAxis		= null;

		JTextArea	m_TextHighlight		= null;

		public EQTLView(Operator op)
		{
			super(op);
			init();
		}
		
		@Override
		protected void createPainter(Operator op)
		{
			m_Painter = new PEQTLPainter(op);
		}
		
		@Override
		protected void buildGUI()
		{
			this.removeAll();
			
			this.setBorder(	BorderFactory.createEmptyBorder(10, 10, 10, 10));
			this.setLayout(new BorderLayout(5,5));
			JPanel masterPanel = new JPanel(new GridLayout(11, 1, 5, 5)); // (rows, cols, hgap, vgap)
			this.add(masterPanel, BorderLayout.CENTER);
			
			m_TextPath	= new JTextArea(m_Path);
			JButton buttonGenerate 	= new JButton( "Process" );
			buttonGenerate.addActionListener(this);
			buttonGenerate.setActionCommand("PROCESS");
			masterPanel.add(m_TextPath);
			masterPanel.add(buttonGenerate);			

			m_ComboDataType  = EnumUtils.getComboBox(OutputType.values(), m_OutputType, "Output Type", this);
			masterPanel.add(new JLabel("View Type: "));
			masterPanel.add(m_ComboDataType);
			
			m_ComboXAxis  = EnumUtils.getComboBox(TableColumns.values(), m_XCol, "X-Axis", this);
			masterPanel.add(new JLabel("X-Axis: "));
			masterPanel.add(m_ComboXAxis);

			m_ComboYAxis  = EnumUtils.getComboBox(TableColumns.values(), m_YCol, "Y-Axis", this);
			masterPanel.add(new JLabel("Y-Axis: "));
			masterPanel.add(m_ComboYAxis);
			
			m_TextHighlight		  = new JTextArea("");
			m_TextHighlight.setText("rs12920590 rs13188622 rs1345863 rs12955865 rs713079 rs28401388 rs12583519 rs7060516 rs4456399 rs11083166 rs2425729 rs35143 rs34510977 rs11662394 rs1007588");

			JButton buttonHighlight = new JButton("Select");
			buttonHighlight.addActionListener(this);
			buttonHighlight.setActionCommand("SELECT");
			masterPanel.add(m_TextHighlight);
			masterPanel.add(buttonHighlight);

			JButton buttonOutput = new JButton("Output Selected");
			buttonOutput.addActionListener(this);
			buttonOutput.setActionCommand("OUTPUTSELECTED");
			JButton buttonHideSelected = new JButton("Hide Selected");
			buttonHideSelected.addActionListener(this);
			buttonHideSelected.setActionCommand("HIDESELECTED");
			JButton buttonHideUnSelected = new JButton("Hide UnSelected");
			buttonHideUnSelected.addActionListener(this);
			buttonHideUnSelected.setActionCommand("HIDEUNSELECTED");
			JButton buttonShowAll = new JButton("Show All");
			buttonShowAll.addActionListener(this);
			buttonShowAll.setActionCommand("SHOWALL");
			masterPanel.add(buttonHideSelected);
			masterPanel.add(buttonHideUnSelected);
			masterPanel.add(buttonShowAll);
			masterPanel.add(buttonOutput);

			
			masterPanel.add(new JLabel());
			masterPanel.add(new JLabel());
			
			JSlider sliderSize = new JSlider(JSlider.HORIZONTAL, 1, 20, 4);
			sliderSize.setName("size_slider");
			sliderSize.addChangeListener(m_Painter);
			masterPanel.add(new JLabel("Point Size:"));
			masterPanel.add(sliderSize);

			JSlider sliderAlpha = new JSlider(JSlider.HORIZONTAL, 1, 255, 128);
			sliderAlpha.setName("alpha_slider");
			sliderAlpha.addChangeListener(m_Painter);
			masterPanel.add(new JLabel("Point Alpha:"));
			masterPanel.add(sliderAlpha);
			
		}
		
		@Override
		public void actionPerformed(ActionEvent e)
		{
			PEQTLPainter eqtlPainter = (PEQTLPainter)m_Painter;
			if (e.getActionCommand().equalsIgnoreCase("PROCESS"))
			{
				m_Path = m_TextPath.getText();
				processEQTLData();
				eqtlPainter.updatePlotInfo();
			}
			else if (e.getActionCommand().equalsIgnoreCase("SELECT"))
			{
				selectPoints(m_CurrTable, m_TextHighlight.getText().split("[\t ,]+"));
				eqtlPainter.updatePlotInfo();
			}
			else if (e.getActionCommand().equalsIgnoreCase("OUTPUTSELECTED"))
			{
				String[] output = getSelectedInfo(m_CurrTable);
				if (output != null)
				{
					System.out.print("\n");
					for (int i = 0; i < output.length; i++)
					{
						System.out.print(output[i] + " ");
					}
				}
			}
			else if (e.getActionCommand().equalsIgnoreCase("HIDESELECTED"))
			{
				hideBySelection(m_CurrTable, true);
			}
			else if (e.getActionCommand().equalsIgnoreCase("HIDEUNSELECTED"))
			{
				hideBySelection(m_CurrTable, false);
			}
			else if (e.getActionCommand().equalsIgnoreCase("SHOWALL"))
			{
				showAll(m_CurrTable);
			}
			else if (e.getSource() == m_ComboDataType)
			{
				m_OutputType = OutputType.values()[m_ComboDataType.getSelectedIndex()];
				m_CurrTable = (m_OutputType == OutputType.PHENO_TYPE)? m_TablePhenoType : m_TableGenoType;
				eqtlPainter.updatePlotInfo();
			}
			else if (e.getSource() == m_ComboXAxis)
			{
				m_XCol = TableColumns.values()[m_ComboXAxis.getSelectedIndex()];
				eqtlPainter.updatePlotInfo();
			}
			else if (e.getSource() == m_ComboYAxis)
			{
				m_YCol = TableColumns.values()[m_ComboYAxis.getSelectedIndex()];
				eqtlPainter.updatePlotInfo();
			}
		}

		@Override
		public void stateChanged(ChangeEvent e)
		{
			if( e.getSource() instanceof JSlider )
			{
//				JSlider source = (JSlider)e.getSource();
//				if (source.getName().equalsIgnoreCase("threshold")) {
//				    if (!source.getValueIsAdjusting()) {
//				    	for (int i = 0; i < m_EQTLData.getNumPhenoTypes(); i++)
//				    	{
//				    		m_EQTLData.m_GroupInfo[i].m_dThresholdLow = 0.01 * source.getValue(); 
//				    		m_EQTLData.m_GroupInfo[i].m_dThresholdHigh = 1 - (0.01 * source.getValue()); 
//				    	}
//				    	m_EQTLData.calcAllCommonSNPs();
//				    	EQTLTableFactory.updatePhenoTable(m_TablePhenoType, m_EQTLData);
//				    	EQTLTableFactory.updateGenoTable(m_TableGenoType, m_EQTLData);
//						//m_TablePhenoType = EQTLTableFactory.createTable(m_EQTLData, EQTLTableFactory.OutputType.PHENO_TYPE);
//						//m_TableGenoType = EQTLTableFactory.createTable(m_EQTLData, EQTLTableFactory.OutputType.RESULT_SINGLE_and_GENO_TYPE);
//						m_CurrTable = (m_OutputType == OutputType.PHENO_TYPE)? m_TablePhenoType : m_TableGenoType;
//				    	((PEQTLPainter)m_Painter).updatePlotInfo();
//				    }
//				}
			}
		}
	}
}
