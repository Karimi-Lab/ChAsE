package still.operators;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
//import java.awt.event.ActionListener;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Hashtable;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import still.data.Map;
import still.data.Operator;
import still.data.Table;
import still.data.TableEvent;
import still.data.TableEvent.TableEventType;
import still.gui.OPAppletViewFrame;
import still.gui.OperatorView;
import still.gui.PLODSplomPainter;

public class SplomOp extends Operator implements Serializable
{
	private static final long serialVersionUID = 677371950040709878L;

	int selectionCol 	= -1;
	boolean hasSelOp 	= false;
	double[][] selCol 	= null;
	
	public static String getMenuName() {
		
		return "View:SPLOM";
	}

	public void setMeasurement( int point_idx, int dim, double value ) {
		
		if( hasSelOp || (!hasSelOp && dim < map.columns()-1 ) ) {
			
			input.setMeasurement(point_idx, dim, value);
		}
		else {
			
			selCol[point_idx][0] = value;
		}
	}

	public ColType getColType( int dim ) {
		
		if( hasSelOp || (!hasSelOp && dim < map.columns()-1 ) ) {
			for( int i : map.getColumnSamples(dim) ) {
				if( input.getColType(i) == ColType.CATEGORICAL ) {
					
					return ColType.CATEGORICAL;
				}
				else if( input.getColType(i) == ColType.NUMERIC ) {
					
					return ColType.NUMERIC;
				}
				else if( input.getColType(i) == ColType.ORDINAL ) {
					
					return ColType.ORDINAL;
				}
				else if( input.getColType(i) == ColType.ATTRIBUTE ) {
					
					return ColType.ATTRIBUTE;
				}
				else if( input.getColType(i) == ColType.METADATA ) {
					
					return ColType.METADATA;
				}
			}
		}
				
		return ColType.ATTRIBUTE;
	}

	public String getColName( int dim ) {

		if( hasSelOp || (!hasSelOp && dim < map.columns()-1 )) {
		
			ArrayList<Integer> colsamp = map.getColumnSamples(dim); 
			if( colsamp.size() > 1 ) {
				
				return (this.toString() + dim);
			}
			else if( colsamp.size() == 1 ){
				
				return input.getColName(colsamp.get(0));
			}
		}

		return "selection";
	}

	public String toString() {
		
		return "[View:SPLOM]";
	}
	
	public String getSaveString( ) {
		
		return "";
	}
	
	public SplomOp( Table newTable, boolean isActive, String paramString ) {
		
		this( newTable, isActive );
	}
	
	public SplomOp( Table newTable, boolean isActive ) {
		
		super( newTable );
		
		this.isActive = isActive;
		
		if( isActive ) {
		
			// handle the selection operator
			hasSelOp = false;
			for( int i = 0; i < input.columns(); i++ ) {
				
				if( 	input.getColName(i).equalsIgnoreCase("selection")  &&
						input.getColType(i) == ColType.ATTRIBUTE ) {
					
					hasSelOp 		= true;
					selectionCol 	= i;
				}
			}
			
			if( ! hasSelOp ){
			
				selCol 		= new double[input.rows()][1];
				map 		= Map.generateDiagonalMap(input.columns()+1);
				function 	= new AppendFunction(input, selCol );
			}
			else {
				
				map 			= Map.generateDiagonalMap(input.columns());
				function 		= new IdentityFunction( input );
			}
	
			isLazy  		= true;
			setView( new SplomView( this ) );
		}
	}
	
	public void activate() {
		
		this.isActive = true;
		
		// handle the selection operator
		hasSelOp = false;
		for( int i = 0; i < input.columns(); i++ ) {
			
			if( 	input.getColName(i).equalsIgnoreCase("selection")  &&
					input.getColType(i) == ColType.ATTRIBUTE ) {
				
				hasSelOp 		= true;
				selectionCol 	= i;
			}
		}
		
		if( ! hasSelOp ){
		
			selCol 		= new double[input.rows()][1];
			map 		= Map.generateDiagonalMap(input.columns()+1);
			function 	= new AppendFunction(input, selCol );
		}
		else {
			
			map 			= Map.generateDiagonalMap(input.columns());
			function 		= new IdentityFunction( input );
		}

		isLazy  		= true;
		setView( new SplomView( this ) );
	}
	
	@Override
	public void updateFunction() {

		if( ! hasSelOp ) {
			function	= new AppendFunction(input, selCol );
		}
		else {
			function 		= new IdentityFunction( input );
		}
	}

	@Override
	public void updateMap() {
		
		// handle the selection operator
		hasSelOp = false;
		for( int i = 0; i < input.columns(); i++ ) {
			
			if( 	input.getColName(i).equalsIgnoreCase("selection")  &&
					input.getColType(i) == ColType.ATTRIBUTE ) {
				
				hasSelOp 		= true;
				selectionCol 	= i;
			}
		}
		
		if( ! hasSelOp ){
			
			selCol 		= new double[input.rows()][1];
			map 		= Map.generateDiagonalMap(input.columns()+1);
		}
		else {
			
			map 			= Map.generateDiagonalMap(input.columns());
		}
	}

	public void tableChanged( TableEvent te ) {
		
		super.tableChanged(te);
		if( this.isActive() ) {
			SwingUtilities.invokeLater(new Runnable() {
	        public void run() {
	        	
	        	
	        	PLODSplomPainter plod = ((PLODSplomPainter)((OPAppletViewFrame)((SplomView)getView()).getViewFrame()).procApp); 
	    		Dimension d = plod.graphSize();
	    		
	    		// determine if there is a need for calculating the correlation matrix
	    		if( Math.min( d.getHeight(), d.getWidth() ) <= plod.LOD_cell_cutoff )
	    		{
	    			plod.calcCorrelationMatrix();
	    		}
	    		
	    		// change the hilight or color status of the underlying visualization
	    		plod.m_bPlotsPGUpToDate = false;
	    		plod.m_bMagnifiedPGUpToDate = false;
	    		plod.invokeRedraw();
	        }
			});
		}
	}
//	public void attributeChanged( String attribute, int[] indices, boolean isUpstream  ) {
//
//		super.attributeChanged(attribute, indices, isUpstream);
//		
//		SwingUtilities.invokeLater(new Runnable() {
//	        public void run() {
//	    		// change the hilight or color status of the underlying visualization
//	    		((OPAppletViewFrame)((SplomView)getView()).getViewFrame()).procApp.invokeRedraw();
//	        }
//	      });
//		
//		
//	}

	public void loadOperatorView() {
		
		setView( new SplomView( this ) );
	}

	public class SplomView extends OperatorView implements ChangeListener {

		JSlider m_SliderPlotSize = null;
		JSlider m_SliderPointSize = null;
		PLODSplomPainter m_PSP = null;
		JCheckBox m_CheckBoxBubble   = null;
		JCheckBox m_CheckBoxFreeze   = null;
		JCheckBox m_CheckBoxCalcCorr = null;
		JCheckBox m_CheckBoxDensity  = null;
		JCheckBox m_CheckBoxDensityLog  = null;
		JCheckBox m_CheckBoxMagnify  = null;
		JCheckBox m_CheckBoxMetaData = null;
		JCheckBox m_CheckBoxPlotSize = null;
		
		Operator m_Operator;
		JPanel  m_SplomPanel = null;
		JPanel  m_MetaDataPanel = null;
		
		public SplomView(Operator op)
		{
			super(op);
			m_Operator = op;
			m_PSP = new PLODSplomPainter(op);
			vframe = new OPAppletViewFrame("E"+op.getExpressionNumber()+":"+op, m_PSP );			
			vframe.addComponentListener(this);
			buildGUI();
		}
		
		void buildGUI()
		{

			JButton buttonCull = new JButton("Cull Highlighted");
			JButton buttonCullNon = new JButton("Cull NonHighlighted");
			buttonCull.setActionCommand("CULLHIGH");
			buttonCullNon.setActionCommand("CULLNONHIGH");
			buttonCull.addActionListener(this);
			buttonCullNon.addActionListener(this);
			JPanel panelCullButtons = new JPanel();
			panelCullButtons.setLayout( new GridLayout(1,2) );
			panelCullButtons.add( buttonCull );
			panelCullButtons.add( buttonCullNon );
			
			Hashtable<Integer,JLabel> labelTable = new Hashtable<Integer,JLabel>();
			int splomsizer = 50;
			for( int i = splomsizer; i <= 500; i += splomsizer) {
				labelTable.put(new Integer((i-splomsizer)/splomsizer),new JLabel(""+i));
			}
			
			m_SliderPlotSize = new JSlider(JSlider.HORIZONTAL, 0, (500-splomsizer)/splomsizer, 0);
			m_SliderPlotSize.setName("slider");
			m_SliderPlotSize.setMajorTickSpacing( 5 );
			m_SliderPlotSize.setMinorTickSpacing( 1 );
			m_SliderPlotSize.setLabelTable(labelTable);
			m_SliderPlotSize.setPaintLabels(true);	
			m_SliderPlotSize.addChangeListener(this);			
			m_SliderPlotSize.addChangeListener(m_PSP);			
			m_SliderPlotSize.setEnabled(false);
			
			Hashtable<Integer,JLabel> labelTableSize = new Hashtable<Integer,JLabel>();
			for( int i = 1; i <= 15; i++) {				
				labelTableSize.put(new Integer(i),new JLabel("" + i));
			}
			
			m_SliderPointSize = new JSlider( 	JSlider.HORIZONTAL, 1, 15, 1);
			m_SliderPointSize.setName("size_slider");
			m_SliderPointSize.setMajorTickSpacing( 2 );
			m_SliderPointSize.setMinorTickSpacing( 1 );
			m_SliderPointSize.setLabelTable(labelTableSize);
			m_SliderPointSize.setPaintLabels(true);	
			m_SliderPointSize.addChangeListener(this);			
			m_SliderPointSize.addChangeListener(m_PSP);			


			JSlider sliderAlpha = new JSlider(JSlider.HORIZONTAL, 1, 255, 255);
			sliderAlpha.setName("alpha_slider");
			//sliderAlpha.setMajorTickSpacing( 2 );
			//sliderAlpha.setMinorTickSpacing( 1 );
			//sliderAlpha.setLabelTable(labelTableSize);
			//sliderAlpha.setPaintLabels(true);	
			//sliderAlpha.addChangeListener(this);			
			sliderAlpha.addChangeListener(m_PSP);			


			JPanel cbox_panel = new JPanel();
			cbox_panel.setLayout(new GridLayout(4,3));
			m_CheckBoxBubble   = new JCheckBox( "Bubble Plot" );
			cbox_panel.add(m_CheckBoxBubble);
			m_CheckBoxBubble.addActionListener(this);

			m_CheckBoxFreeze   = new JCheckBox( "Freeze Axes" );
			cbox_panel.add(m_CheckBoxFreeze);
			m_CheckBoxFreeze.addActionListener(this);

			m_CheckBoxDensity  = new JCheckBox( "Density Plot" );
			cbox_panel.add(m_CheckBoxDensity);
			m_CheckBoxDensity.addActionListener(this);
			m_CheckBoxDensity.setSelected(m_PSP.m_bDrawDensityPlot);

			m_CheckBoxDensityLog  = new JCheckBox( "Log Scale" );
			cbox_panel.add(m_CheckBoxDensityLog);
			m_CheckBoxDensityLog.addActionListener(this);
			m_CheckBoxDensityLog.setSelected(m_PSP.m_bDrawDensityPlotLog);

			m_CheckBoxMagnify  = new JCheckBox( "Magnified View" );
			cbox_panel.add(m_CheckBoxMagnify);
			m_CheckBoxMagnify.addActionListener(this);
			m_CheckBoxMagnify.setSelected(m_PSP.m_bEnableMagnified);

			m_CheckBoxMetaData = new JCheckBox( "Show MetaData" );
			cbox_panel.add(m_CheckBoxMetaData);
			m_CheckBoxMetaData.addActionListener(this);
			m_CheckBoxMetaData.setSelected(m_PSP.m_bShowMetaData);
			
			m_CheckBoxCalcCorr = new JCheckBox( "Calc Correlation" );
			cbox_panel.add(m_CheckBoxCalcCorr);
			m_CheckBoxCalcCorr.addActionListener(this);
			m_CheckBoxCalcCorr.setSelected(m_PSP.m_bCalcPearsonCorrCoeff);

			m_CheckBoxPlotSize = new JCheckBox("Manual Plot Size");
			m_CheckBoxPlotSize.addActionListener(this);
			
			JPanel panelPointSize = new JPanel();
			panelPointSize.setLayout(new BorderLayout(5,5));
			panelPointSize.add(m_SliderPointSize, "Center");
			panelPointSize.add(new JLabel("Point Size"), "West");

			JPanel panelPointAlpha = new JPanel();
			panelPointAlpha.setLayout(new BorderLayout(5,5));
			panelPointAlpha.add(sliderAlpha, "Center");
			panelPointAlpha.add(new JLabel("Point Alpha"), "West");

			JPanel panelPlotSize = new JPanel();
			panelPlotSize.setLayout( new BorderLayout() );
			panelPlotSize.add(m_CheckBoxPlotSize,BorderLayout.WEST);
			panelPlotSize.add(m_SliderPlotSize,BorderLayout.CENTER);
			
			JPanel panelSizeSliders = new JPanel();
			panelSizeSliders.setLayout( new GridLayout(3,1));
			panelSizeSliders.add(panelPointSize);
			panelSizeSliders.add(panelPointAlpha);
			panelSizeSliders.add(panelPlotSize);
			
			m_SplomPanel = new JPanel();
			m_SplomPanel.setLayout(new BorderLayout(5, 5));
			m_MetaDataPanel = new JPanel();
			m_PSP.setMetaDataPanel(m_MetaDataPanel);
			m_SplomPanel.add( panelCullButtons, BorderLayout.NORTH );
			m_SplomPanel.add( cbox_panel, BorderLayout.CENTER );
			m_SplomPanel.add( panelSizeSliders, BorderLayout.SOUTH );
			
			JTabbedPane	m_MasterPanel = new JTabbedPane();// new GridLayout(1,3,0,0));
			this.add(m_MasterPanel, BorderLayout.CENTER);
			m_MasterPanel.addTab("Splom", null, m_SplomPanel, "SPlom view options");
			m_MasterPanel.addTab("MetaData", null, m_MetaDataPanel, "metadata view options");
	        m_MasterPanel.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		}
		
		/**
		 * 
		 */
		private static final long serialVersionUID = 8557769161790649887L;
		
		public void actionPerformed(ActionEvent e)
		{
			if(e.getSource() == m_CheckBoxBubble)
			{
				m_PSP.setBubblePlot(m_CheckBoxBubble.isSelected());
			}			
			else if(e.getSource() == m_CheckBoxFreeze)
			{
				m_PSP.setFreezeAxes(m_CheckBoxFreeze.isSelected());
			}	
			else if(e.getSource() == m_CheckBoxDensity)
			{
				m_PSP.setDensityPlot(m_CheckBoxDensity.isSelected());
			}
			else if(e.getSource() == m_CheckBoxDensityLog)
			{
				m_PSP.setDensityPlotLog(m_CheckBoxDensityLog.isSelected());
			}
			else if (e.getSource() == m_CheckBoxCalcCorr)
			{
				m_PSP.setCalcCorrelation(m_CheckBoxCalcCorr.isSelected());
			}
			else if(e.getSource() == m_CheckBoxMagnify)
			{
				m_PSP.setMagnify(m_CheckBoxMagnify.isSelected());
				m_CheckBoxMetaData.setEnabled(m_PSP.m_bEnableMagnified);
			}
			else if(e.getSource() == m_CheckBoxMetaData)
			{
				m_PSP.showMetaData(((JCheckBox)e.getSource()).isSelected());
			}
			else if(e.getSource() == m_CheckBoxPlotSize) {
				
				((OPAppletViewFrame)this.vframe).setScroll(!((OPAppletViewFrame)this.vframe).hasScroll);
				m_SliderPlotSize.setEnabled(m_CheckBoxPlotSize.isSelected());
				SwingUtilities.invokeLater(new Runnable() {
			        public void run() {
			          // Set the preferred size so that the layout managers can handle it
			        	m_PSP.invalidate();			        	
			        	vframe.setSize(new Dimension(vframe.getSize().width,vframe.getSize().height+1));
			        	vframe.setSize(new Dimension(vframe.getSize().width,vframe.getSize().height-1));
						m_PSP.heavyResize();
						m_PSP.invokeRedraw();
			        }
			      });
			}
			if( e.getActionCommand().equalsIgnoreCase("CULLHIGH") ) {
				
				String paramString = "";
				PLODSplomPainter psp = ((PLODSplomPainter)((OPAppletViewFrame)vframe).procApp);
				for( int i = 0; i < psp.numerics.size(); i++ ) {
					
					if( psp.cull_hilight[i] ) {
						
						paramString += this.operator.input.getColName(psp.numerics.get(i));
						paramString += ",";
					}
				}
				if( paramString.length() > 0 ) {
					
					paramString = paramString.substring(0,paramString.length()-1);
				}
				CullByNameOp cbnOp = new CullByNameOp( this.operator.input, true, paramString );
				this.operator.tableChanged( new TableEvent( this.operator, TableEventType.ADD_ME, cbnOp), true);
			}
			if( e.getActionCommand().equalsIgnoreCase("CULLNONHIGH") ) {
				
				String paramString = "";
				PLODSplomPainter psp = ((PLODSplomPainter)((OPAppletViewFrame)vframe).procApp);
				for( int i = 0; i < psp.numerics.size(); i++ ) {
					
					if( !psp.cull_hilight[i] ) {
						
						paramString += this.operator.input.getColName(psp.numerics.get(i));
						paramString += ",";
					}
				}
				if( paramString.length() > 0 ) {
					
					paramString = paramString.substring(0,paramString.length()-1);
				}
				CullByNameOp cbnOp = new CullByNameOp( this.operator.input, true, paramString );
				this.operator.tableChanged( new TableEvent( this.operator, TableEventType.ADD_ME, cbnOp), true);
			}
		}

		@Override
		public void stateChanged(ChangeEvent e)
		{
			if(e.getSource() == m_SliderPointSize)
			{
				//m_PSP.setPointSize(m_SliderPointSize.getValue());
			}			
		}
		
	}
}
