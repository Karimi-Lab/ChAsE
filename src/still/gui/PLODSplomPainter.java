//TODO: optimize the plot_bounds calculation (currently d^2 * n), and call less often.
package still.gui;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;

import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import processing.core.*;
import still.data.Binner2D;
import still.data.FloatIndexer;
import still.data.Operator;
import still.data.TableEvent;
import still.data.Table.ColType;
import still.gui.metadataviewer.MetadataImageViewer;
import still.gui.metadataviewer.MetadataViewer;
import still.operators.SplomOp;

public class PLODSplomPainter extends OPApplet implements ChangeListener, MouseWheelListener
{
	boolean m_bIsInitializing = false;
	public boolean [] cull_hilight = null;
	public double[][] m_PearsonCorrCoeff = null;
	public int LOD_cell_cutoff = 50; 
	public boolean m_bIsSPLOM = true;
	public int 	m_iMetaDataCol  	= -1;
	
	public int m_iPointSize = 4;
	int	m_iPointAlpha = 255;
	public boolean m_bSelectionOn = false; // whether doing a selection by dragging mouse
	public boolean m_bSelectionMagnified = false; // selecting in the magnified view
	public int minSplomBound 	= 20;
	
	public int LABEL_SPACING_H = 5;
	public int CELL_SPACING = 5; 
	public int BIG_BORDER_H_L = 75;
	public int BIG_BORDER_H_R = 25;
	public int BIG_BORDER_V_TOP = 10;
	public int BIG_BORDER_V_BOT = 40;
	
	public int 		MAGNIFIED_BORDER_H_R 		= 10;
	public int 		MAGNIFIED_BORDER_V_TOP 		= 10;
	public int 		BIG_BORDER_H_R_DEFAULT 		= 25; 
	public int 		BIG_BORDER_V_TOP_DEFAULT 	= 10;
	public int 		CELL_WIDTH_MIN 				= 4;
	public int 		CELL_HEIGHT_MIN 			= 4;
	public boolean 	m_bIsMagnifiedMax 			= false;
	public boolean 	m_bEnableMagnified 			= true;
	public int[] 	m_MagnifiedViewCoords 		= new int[4]; //{left, top, right, bottom}
	public double[] m_MagnifiedPlotBounds 		= new double[4];
	public double 	m_dMagnifiedZoom			= 1;
	double 			m_dMagnifiedOffsetX 		= 0;
	double 			m_dMagnifiedOffsetY 		= 0;
	int 			m_iMagnifiedPoint 			= -1;
	public boolean 	m_bDrawDensityPlot 			= false;
	public boolean 	m_bDrawDensityPlotLog 		= false;
	
	public boolean 		  m_bShowMetaData  		= true;
	public MetadataViewer m_Viewer 				= new MetadataImageViewer(); //TODO: will be replaced with a general Factory.
	public JPanel  		  m_MetaDataPanel = null;

	public double[][][] m_PlotBounds = null;
	public int[][] hist_data = null;
	public int[][] sel_hist_data = null;
	public int[] max_y = null;
	public int hist_bins = 20;
	int cell_width = -1;
	int cell_height = -1;
	int axis_spacing_h =-1;
	int axis_length_h = -1;
	int axis_spacing_v = -1;
	int axis_length_v = -1;
	int[] selSplom = new int[2];
	boolean m_bInSplom = false;
	public int axeslines = 4;
	
	public NumberFormat m_NumberFormatDigits = null;	
	public NumberFormat m_NFScientific 		 = new DecimalFormat("0.#E0");
	public PVector selBoxCoords = null;
	public PVector selBoxDims = null;
	public boolean m_bBubblePlot = false;
	public boolean m_bFreezeAxes = false;
	public Binner2D[][] b2ds = null;
	public double max_bubble_size = -1;
	public int max_bubble_count = -1;
	
	boolean m_bUpdatePlotBounds = true;
	public boolean m_bCalcPearsonCorrCoeff = true;
	
	PGraphics m_PlotsPG[][] = null; // cached PGraphics for plots.
	public boolean   m_bPlotsPGUpToDate = false; // whether the plots need updating
	PGraphics m_MagnifiedPG = null;
	public boolean	  m_bMagnifiedPGUpToDate = false;

	
	public PLODSplomPainter(Operator op)
	{
		super(op);		
		
		m_NumberFormatDigits = NumberFormat.getInstance();
		m_NumberFormatDigits.setMinimumFractionDigits(0);
		m_NumberFormatDigits.setMaximumFractionDigits(2);
		
		addMouseWheelListener(this);
		dataSizeConsiderations();
		updateColorSelectionCol();
		updateSplomViewer();
		calcCorrelationMatrix();
		//calcPlotBounds();
		setSelectedSplom(0, 1);
	}

	// calculations based on datasize and dimension
	void dataSizeConsiderations()
	{
		countNumerics();
		if (num_numerics > 50)
		{
			LABEL_SPACING_H = 0;
			CELL_SPACING = 0;
			CELL_WIDTH_MIN = 1;			
			CELL_HEIGHT_MIN = 1;
			m_bCalcPearsonCorrCoeff = false;
		}
	}
	
	/**
	 * Compute the bubbles for the bubble scatterplot
	 */
	public void genBubbles( ) {
		
		if( !m_bBubblePlot ) {
			
			return;
		}
		
		b2ds = new Binner2D[num_numerics][num_numerics];
		
		for( int i = 0; i < num_numerics; i++) {
			
			for( int j = i+1; j < num_numerics; j++ ) {
				
				if (m_iColorCol == -1)
				{
					b2ds[i][j] = new Binner2D( op, numerics.get(i), numerics.get(j) );
				}
				else
				{
					b2ds[i][j] = new Binner2D( op, numerics.get(i), numerics.get(j), m_iColorCol );
				}
			}
		}
		
		// calculate the maximum bubble dimensions
		
		max_bubble_size = Double.MAX_VALUE;
		max_bubble_count = -1;
		for( int i = 0; i < num_numerics; i++ ) {
			
			for( int j = i+1; j < num_numerics; j++ ) {
				
				max_bubble_size = Math.min( max_bubble_size, (cell_width-2*axis_spacing_h) / ((double)this.b2ds[i][j].bin1.getBinCount()) );
				max_bubble_size = Math.min( max_bubble_size, (cell_height-2*axis_spacing_v) / ((double)this.b2ds[i][j].bin2.getBinCount()) );				
				//				System.out.println("" + ((double)this.b2ds[i][j].bin1.getBinCount()) + " and " + ((double)this.b2ds[i][j].bin2.getBinCount()) );
				max_bubble_count = Math.max(max_bubble_count, this.b2ds[i][j].getMax2DBin());
			}
		}		
		
		//		max_bubble_size /= 2.;
		//		System.out.println("" + (cell_width-2*axis_spacing_h) + " and " + (cell_height-2*axis_spacing_v) );
		//		System.out.println("max_bubble_size = " + max_bubble_size);
		
	}
	
	public void invokeRedraw() 
	{ // to be able to monitor where and when the redraw is being invoked.
		redraw();
	}
	
	/**
	 * Set whether to freeze the axes scale
	 * 
	 * @param freezeAxes
	 */
	public void setFreezeAxes( boolean freezeAxes ) {
		
		this.m_bFreezeAxes = freezeAxes;
		if( ! freezeAxes ) {
			
    		heavyResize();
    		invokeRedraw();
		}
	}
	
	/**
	 * Sets whether to show the magnified view
	 * @param bMagnify
	 */
	public void setMagnify(boolean bMagnify)
	{
		m_bEnableMagnified = bMagnify;
		if (!m_bEnableMagnified)
			setMagnifiedMax(false);
		else
			setMagnifiedMax(true);
		heavyResize();
		invokeRedraw();
	}
	
	public void showMetaData(boolean bShow)
	{
		m_bShowMetaData = bShow;
		invokeRedraw();
	}
	
	public void setDensityPlot(boolean bDensityPlot)
	{
		m_bDrawDensityPlot = bDensityPlot;
		m_bPlotsPGUpToDate     = false;
		m_bMagnifiedPGUpToDate = false;
		invokeRedraw();
	}
	
	public void setDensityPlotLog(boolean bLog)
	{
		m_bDrawDensityPlotLog = bLog;
		m_bPlotsPGUpToDate     = false;
		m_bMagnifiedPGUpToDate = false;
		invokeRedraw();
	}

	int m_iSelectedi = 0;
	int m_iSelectedj = 0;
	void setSelectedSplom(int i, int j)
	{
		if (selSplom[0] != i || selSplom[1] != j)
		{
			selSplom[0] = i;
			selSplom[1] = j;
			m_dMagnifiedZoom			= 0.8;
			m_dMagnifiedOffsetX 		= 0;
			m_dMagnifiedOffsetY 		= 0;
			m_iMagnifiedPoint 			= -1;
			m_bMagnifiedPGUpToDate  		= false;
		}
	}
	
	public void setMagnifiedMax(boolean bMax)
	{
		m_bIsMagnifiedMax = bMax;
		if (m_bIsMagnifiedMax)
		{
			BIG_BORDER_H_R = 	Math.max(BIG_BORDER_H_R_DEFAULT, this.width - BIG_BORDER_H_L - (CELL_WIDTH_MIN * CELL_SPACING) * (num_numerics - 1));
			BIG_BORDER_V_TOP = Math.max(BIG_BORDER_V_TOP_DEFAULT, this.height - BIG_BORDER_V_BOT - (CELL_HEIGHT_MIN * CELL_SPACING) * (num_numerics - 1)); 
		}
		else
		{
			BIG_BORDER_H_R = BIG_BORDER_H_R_DEFAULT;
			BIG_BORDER_V_TOP = BIG_BORDER_V_TOP_DEFAULT;
		}
		
	}
	/**
	 * Set whether to draw a bubble scatterplot
	 * 
	 * @param bubblePlot
	 */
	public void setBubblePlot( boolean bubblePlot ) {
		
		this.m_bBubblePlot = bubblePlot;
		if( bubblePlot ) {
			
			genBubbles();
		}
		
		invokeRedraw();
	}
	
	public Dimension graphSize()
	{
		// calculate cell size
		numerics = getNumerics();
		countNumerics();

		int label_loop_bound = num_numerics - 1;
		float max_title_width = 0;
		for( int i = 0; i < label_loop_bound; i++ )
		{
			max_title_width = Math.max( textWidth( this.getOp().input.getColName(numerics.get(i)) ), max_title_width );
		}
		//BIG_BORDER_H_L = (int)max_title_width+3*LABEL_SPACING_H+(int)textWidth( "0.##E0" );
		BIG_BORDER_H_L = (int)textWidth( "0.##E0" )+3*LABEL_SPACING_H+(int)textWidth( "0.##E0" );
		
		cell_width 	= (int)Math.round(((this.width - (num_numerics-2)*CELL_SPACING) - (BIG_BORDER_H_L+BIG_BORDER_H_R)) / (num_numerics-1.));
		cell_height = (int)Math.round(((this.height - (num_numerics-2)*CELL_SPACING) - (BIG_BORDER_V_TOP+BIG_BORDER_V_BOT)) / (num_numerics-1.));
		
		return new Dimension((int)cell_width,(int)cell_height);
	}
	
	
	private void calcAxis(int iWidth, int iHeight)
	{
		axis_spacing_h = (int)Math.round(iWidth/10.);
		axis_length_h = (int)Math.round((8.*iWidth)/10.);
		axis_spacing_v = (int)Math.round(iHeight/10.);
		axis_length_v = (int)Math.round((8.*iHeight)/10.);	
	}
	
	/**
	 * Perform heavy weight recalculation of sizing terms 
	 */
	public synchronized void heavyResize()
	{
		m_bIsInitializing = true;
		int old_numerics = this.num_numerics;
		
		// calculate cell size
		numerics = getNumerics();
		countNumerics();
		updateColorSelectionCol();
		
		// update the cull hilighting
		
		if (cull_hilight == null ||  
		    cull_hilight.length != num_numerics || 
		    num_numerics != old_numerics ) {
			
			cull_hilight = new boolean[num_numerics];
		}
		
		int label_loop_bound = num_numerics - 1;
		float max_title_width = 0;
		for( int i = 0; i < label_loop_bound; i++ ) {
			
			max_title_width = Math.max( textWidth( this.getOp().input.getColName(numerics.get(i)) ), max_title_width );
		}
		//BIG_BORDER_H_L = (int)max_title_width+3*LABEL_SPACING_H+(int)textWidth( "0.##E0" );
		BIG_BORDER_H_L = (int)textWidth( "0.##E0" )+3*LABEL_SPACING_H+(int)textWidth( "0.##E0" );
		
		cell_width 	= (int)Math.round(((this.width - (num_numerics-2)*CELL_SPACING) - (BIG_BORDER_H_L+BIG_BORDER_H_R)) / (num_numerics-1.));
		cell_height = (int)Math.round(((this.height - (num_numerics-2)*CELL_SPACING) - (BIG_BORDER_V_TOP+BIG_BORDER_V_BOT)) / (num_numerics-1.));
		if (cell_width < CELL_WIDTH_MIN || cell_height < CELL_WIDTH_MIN)
		{
			CELL_SPACING = 0;
		}
		else
		{
			CELL_SPACING = 5; 
		}
		
		cell_width 	= (int)Math.round(((this.width - (num_numerics-2)*CELL_SPACING) - (BIG_BORDER_H_L+BIG_BORDER_H_R)) / (num_numerics-1.));
		cell_height = (int)Math.round(((this.height - (num_numerics-2)*CELL_SPACING) - (BIG_BORDER_V_TOP+BIG_BORDER_V_BOT)) / (num_numerics-1.));
		
		// determine if we will plot a splom or a bom
		
		this.m_bIsSPLOM = ( Math.min(cell_width,cell_height) > this.LOD_cell_cutoff );
		
		calcAxis(cell_width, cell_height);

		if (m_PlotBounds == null || m_PlotBounds.length != num_numerics)
		{
			calcPlotBounds();
		}
		
		m_bPlotsPGUpToDate = false;
		m_bMagnifiedPGUpToDate = false;
		m_bIsInitializing = false;
	}
	
	void calcPlotBounds()
	{
		numerics = getNumerics();
		countNumerics();

		boolean doFreezeAxes = false;
		
		if(!( m_bFreezeAxes && m_PlotBounds != null && m_PlotBounds.length == num_numerics )) {
			
			m_PlotBounds	= new double[num_numerics][num_numerics][4];
		}
		else {
			
			doFreezeAxes = true;
		}
		
		this.axeslines = (int)Math.floor(  axis_length_h / textWidth( "0.##E0" ) );
		
		// if we're bubbling, then bubble on
		
		genBubbles();
		
		
		max_y = new int[num_numerics];
		hist_data	= new int[num_numerics][hist_bins];
		sel_hist_data	= new int[num_numerics][hist_bins];

		double dim_bounds[][] = new double[num_numerics][2];

		if( ! doFreezeAxes ) {
			
			if (!m_bUpdatePlotBounds)
			{
				for (int d = 0; d < num_numerics; d++)
				{
					dim_bounds[d][0] = 0;
					dim_bounds[d][1] = 1;
				}
			}
			else
			{
				for (int d = 0; d < num_numerics; d++)
				{
					dim_bounds[d][0] = Double.POSITIVE_INFINITY;
					dim_bounds[d][1] = Double.NEGATIVE_INFINITY;
				}
	
				for( int k = 0; k < getOp().rows(); k++ ) 
				{
					for (int d = 0; d < num_numerics; d++)
					{
						double fVal = getOp().getMeasurement(k,numerics.get(d));
						dim_bounds[d][0] = Math.min(dim_bounds[d][0], fVal);
						dim_bounds[d][1] = Math.max(dim_bounds[d][1], fVal);
					}
				}
			}
			
			for( int i = 0; i < num_numerics; i++ ) {
				
				for( int j = i; j < num_numerics; j++ ) {
					// maintain the bounds for fast calculation
					m_PlotBounds[i][j][0] = dim_bounds[i][0];
					m_PlotBounds[i][j][1] = dim_bounds[j][0];
					m_PlotBounds[i][j][2] = dim_bounds[i][1];
					m_PlotBounds[i][j][3] = dim_bounds[j][1];
					m_PlotBounds[j][i][0] = dim_bounds[j][0];
					m_PlotBounds[j][i][1] = dim_bounds[i][0];
					m_PlotBounds[j][i][2] = dim_bounds[j][1];
					m_PlotBounds[j][i][3] = dim_bounds[i][1];
				}
			}
		}		
	}
	
	public void componentResized(ComponentEvent e) {
		
		//		System.out.println(""+e.getSource().getClass().getName());
	    SwingUtilities.invokeLater(new Runnable() {
								   public void run() {
								   // Set the preferred size so that the layout managers can handle it
								   heavyResize();
								   invokeRedraw();
								   }
								   });
	}
	
	public void setMetaDataPanel(JPanel panel)
	{
		m_MetaDataPanel = panel; 
		if (m_MetaDataPanel != null)
		{
			m_Viewer.buildGUI(m_MetaDataPanel);
		}
	}
	
	public void updateSplomViewer()
	{
		m_iMetaDataCol = m_Viewer.processData(getOp(), this);
		if (m_MetaDataPanel != null)
		{
			m_Viewer.buildGUI(m_MetaDataPanel);
		}
	}
	
	public void mouseWheelMoved (MouseWheelEvent e)
	{
		//println( e.toString() );
		calcMagnifiedView(e.getWheelRotation(), 0, 0);
		m_bMagnifiedPGUpToDate = false;
		invokeRedraw();
	}
	
	public void setCalcCorrelation(boolean bCalc)
	{
		m_bCalcPearsonCorrCoeff = bCalc;
		if (m_bCalcPearsonCorrCoeff)
		{
			calcCorrelationMatrix();
		}
	}
	
	public void calcCorrelationMatrix()
	{
		double[] sums 	= new double[op.columns()]; // sum(x) / N
		double[] sqSums	= new double[op.columns()]; // sum(x^2) / N
		double[] var	= new double[op.columns()]; // variance
		for( int i = 0; i < op.rows(); i++ )
		{
			for( int j = 0; j < op.columns(); j++ )
			{
				double dValue = op.getMeasurement(i, j);
				sums[j] 	+= 	dValue;
				sqSums[j]	+= 	dValue * dValue;				
			}
		}
		int num_numeric = op.columns();
		for( int j = 0; j < op.columns(); j++ )
		{
			if( op.getColType(j) != ColType.NUMERIC ) {
				sums[j] = -1;
				sqSums[j] = -1;
				var[j] = -1;
				num_numeric--;
			}
			else {
				sums[j] 	= sums[j]	/((double) op.rows());
				sqSums[j] 	= sqSums[j]	/((double) op.rows());
				var[j] = sqSums[j] - (sums[j]*sums[j]);
			}
		}
		
		// compute the d^2 correlation matrix
		m_PearsonCorrCoeff = new double[num_numeric][num_numeric];
		if (!m_bCalcPearsonCorrCoeff)
		{
			return;
		}
		int k = 0;
		int kk = 0;
		for( int i = 0; i < num_numeric; i++ )
		{
			while(op.getColType(k) != ColType.NUMERIC )
				k++;
			
			kk = 0;
			for( int j = 0; j <= i; j++ )
			{
				while(op.getColType(kk) != ColType.NUMERIC )
					kk++;
				
				// compute 1's on the diagonal
				
				if (i == j)
				{
					m_PearsonCorrCoeff[i][j] = 1.0;
					break;
				}
				
				// compute pearson's correlation coefficient
				double sumNum = 0.0;
				for (int ind = 0; ind < op.rows(); ind++)
				{
					sumNum += (op.getMeasurement(ind, k)-sums[k]) * 
							  (op.getMeasurement(ind, kk)-sums[kk]); 
				}
				
				m_PearsonCorrCoeff[i][j] = sumNum / ((op.rows()-1)*Math.sqrt(var[kk])*Math.sqrt(var[k]));
				m_PearsonCorrCoeff[j][i] = m_PearsonCorrCoeff[i][j];
				kk++;
			}
			k++;
		}
	}
	
	@Override
	public void actionPerformed(ActionEvent e)
	{
		m_bIsInitializing = true;
		dataSizeConsiderations();
		updateColorSelectionCol();
		updateSplomViewer();
		//calcCorrelationMatrix();
		//calcPlotBounds();
		setSelectedSplom(0, 1);
		m_PlotBounds = null;
		m_bPlotsPGUpToDate = false;
		m_bMagnifiedPGUpToDate = false;
	    SwingUtilities.invokeLater(new Runnable() {
								   public void run() {
								   // Set the preferred size so that the layout managers can handle it
								   heavyResize();
								   invokeRedraw();
								   }
								   });
	}
	
	public void setup() 
	{
		textFont(createFont("Helvetica", 10), 10);
		//smooth();
		
		if( this.getOp() instanceof SplomOp ) {
			
			// count the number of dimensions
			
			countNumerics();
			int span_h = (BIG_BORDER_H_L+BIG_BORDER_H_R) + (num_numerics-1 * minSplomBound) + (num_numerics-2)*CELL_SPACING;
			int span_v = (BIG_BORDER_V_TOP+BIG_BORDER_V_BOT) + (num_numerics-1 * minSplomBound) + (num_numerics-2)*CELL_SPACING;
			
			// compute the minimum size
			
			size(	Math.max( span_h, OPAppletViewFrame.MINIMUM_VIEW_WIDTH), 
				 Math.max( span_v, OPAppletViewFrame.MINIMUM_VIEW_HEIGHT));//,P2D);
			this.setPreferredSize(new Dimension(Math.max( span_h, OPAppletViewFrame.MINIMUM_VIEW_WIDTH), 
												Math.max( span_v, OPAppletViewFrame.MINIMUM_VIEW_HEIGHT)));
		}
		
		heavyResize();
		
		// prevent thread from starving everything else
		noLoop();
		
		this.finished_setup = true;
		
	    SwingUtilities.invokeLater(new Runnable() {
								   public void run() {
								   // Set the preferred size so that the layout managers can handle it
								   invalidate();
								   getParent().validate();
								   }
								   });
	}
	
	public boolean nearestSplom(int[] nearest)
	{
		int xComp = mouseX - BIG_BORDER_H_L;
		int yComp = mouseY - BIG_BORDER_V_TOP;
		
		nearest[0] = Math.min( num_numerics-2, Math.max( 0, xComp ) / (cell_width+CELL_SPACING)  );
		nearest[1] = Math.min( num_numerics-2, Math.max( 0, yComp ) / (cell_height+CELL_SPACING) );
		
		// determine if it is IN the splom
		
		if (( 	xComp > 0 && 
			 xComp < (nearest[1]+1)*(cell_width+CELL_SPACING) && 
			 xComp % (cell_width+CELL_SPACING) <= cell_width ) && 
			( 	yComp > 0 && 
			 //yComp < ((num_numerics-1) - nearest[0])*(cell_height+CELL_SPACING) && 
			 yComp % (cell_height+CELL_SPACING) <= cell_height ) &&
			(yComp < (cell_height+CELL_SPACING)*(num_numerics-1)) ){
			
			return true;
		} 
		//System.out.println("("+nearest[0]+","+nearest[1]+") "+inSplom);
		return false;
	}
	
	double m_dMouseZoom = 0;
	double m_dMouseDX 	= 0;
	double m_dMouseDY 	= 0;
	public void calcMagnifiedView(double dZoom, double dX, double dY)
	{
		if (numerics.size() < 2)
		{
			return;
		}
		if (m_PlotBounds.length <= selSplom[0] || m_PlotBounds[selSplom[0]].length <= selSplom[1])
		{
			setSelectedSplom(0, 1);
		}
		
		m_dMouseZoom += dZoom; 
		m_dMouseDX += dX;
		m_dMouseDY += dY;
		
		if (m_bIsDrawing) 
			return; // to avoid change of coordinates in the middle of drawing.
		
		// calculate screen coordinates of the magnified view
		int iNewWidth = this.width - MAGNIFIED_BORDER_H_R - (this.width - BIG_BORDER_H_R + BIG_BORDER_H_L + cell_width + CELL_SPACING + 20) / 2; 
		int iNewHeight = (this.height - BIG_BORDER_V_BOT + BIG_BORDER_V_TOP- cell_height - CELL_SPACING - 20) / 2 - MAGNIFIED_BORDER_V_TOP; 
		m_MagnifiedViewCoords[0] = this.width - iNewWidth - MAGNIFIED_BORDER_H_R;
		m_MagnifiedViewCoords[1] = MAGNIFIED_BORDER_V_TOP;
		m_MagnifiedViewCoords[2] = m_MagnifiedViewCoords[0] + iNewWidth;
		m_MagnifiedViewCoords[3] = m_MagnifiedViewCoords[1] + iNewHeight;
		//calcAxis(iNewWidth, iNewHeight);
		
		// calculate the data boundary of the magnified view
		double w = m_PlotBounds[selSplom[0]][selSplom[1]][2] - 
		m_PlotBounds[selSplom[0]][selSplom[1]][0];
		double h = m_PlotBounds[selSplom[0]][selSplom[1]][3] - 
		m_PlotBounds[selSplom[0]][selSplom[1]][1];
		double cx = 0.5 * (m_PlotBounds[selSplom[0]][selSplom[1]][2] + 
						   m_PlotBounds[selSplom[0]][selSplom[1]][0]);// center x of the original bounds
		double cy = 0.5 * (m_PlotBounds[selSplom[0]][selSplom[1]][3] + 
						   m_PlotBounds[selSplom[0]][selSplom[1]][1]);// center y of the original bounds
		double mcx = w * m_dMagnifiedOffsetX + cx; // center x of the magnified bound
		double mcy = h * m_dMagnifiedOffsetY + cy; // center y of the magnified bound
		
		m_MagnifiedPlotBounds[0] = mcx - 0.5*w/m_dMagnifiedZoom;
		m_MagnifiedPlotBounds[1] = mcy - 0.5*h/m_dMagnifiedZoom;
		m_MagnifiedPlotBounds[2] = mcx + 0.5*w/m_dMagnifiedZoom;
		m_MagnifiedPlotBounds[3] = mcy + 0.5*h/m_dMagnifiedZoom;
		
		mcx = 0.5 * (m_MagnifiedPlotBounds[0] + m_MagnifiedPlotBounds[2]);
		mcy = 0.5 * (m_MagnifiedPlotBounds[1] + m_MagnifiedPlotBounds[3]);
		
		double dPVRatioH = 1.f * (m_MagnifiedPlotBounds[2] - m_MagnifiedPlotBounds[0]) /  // horizontal plot-to-view ratio
		(m_MagnifiedViewCoords[2] - m_MagnifiedViewCoords[0]);
		double dPVRatioV = 1.f * (m_MagnifiedPlotBounds[3] - m_MagnifiedPlotBounds[1]) /  // vertical plot-to-view ratio
		(m_MagnifiedViewCoords[3] - m_MagnifiedViewCoords[1]);
		
		double m_dMagnifiedMouseX = m_MagnifiedPlotBounds[0] + dPVRatioH * (mouseX - m_MagnifiedViewCoords[0]);
		double m_dMagnifiedMouseY = m_MagnifiedPlotBounds[1] + dPVRatioV * (m_MagnifiedViewCoords[3] - mouseY);
		if (m_dMouseDX != 0 || m_dMouseDY != 0)
		{
			double m_dMagnifiedPMouseX = m_MagnifiedPlotBounds[0] + dPVRatioH * (mouseX - m_dMouseDX - m_MagnifiedViewCoords[0]);
			double m_dMagnifiedPMouseY = m_MagnifiedPlotBounds[1] + dPVRatioV * (m_MagnifiedViewCoords[3] - mouseY + m_dMouseDY);
			
			m_dMagnifiedOffsetX = (mcx - (m_dMagnifiedMouseX - m_dMagnifiedPMouseX) - cx) / w;
			m_dMagnifiedOffsetY = (mcy - (m_dMagnifiedMouseY - m_dMagnifiedPMouseY) - cy) / h;
		}
		
		if (m_dMouseZoom != 0)
		{
			double dNewZoom = Math.max(0.5, m_dMagnifiedZoom - m_dMouseZoom * 0.01*Math.abs(m_dMagnifiedZoom));
			if (dNewZoom != m_dMagnifiedZoom)
			{
				double dZoomChange = dNewZoom / m_dMagnifiedZoom;
				mcx = ((dZoomChange - 1) * m_dMagnifiedMouseX + mcx) / dZoomChange;
				mcy = ((dZoomChange - 1) * m_dMagnifiedMouseY + mcy) / dZoomChange;
				m_dMagnifiedOffsetX = (mcx - cx) / w;
				m_dMagnifiedOffsetY = (mcy - cy) / h;
				m_dMagnifiedZoom = dNewZoom;
			}
		}
		
		m_dMouseZoom = 0;
		m_dMouseDX 	= 0;
		m_dMouseDY 	= 0;
	}
	
	void getPlotViewCoordinates(int i, int j, int[] viewCoord)
	{
		viewCoord[0] = BIG_BORDER_H_L + i * (cell_width + CELL_SPACING); //top
		viewCoord[1] = BIG_BORDER_V_TOP + (j - 1) * (cell_height + CELL_SPACING);
		viewCoord[2] = viewCoord[0] + cell_width;  //bottom
		viewCoord[3] = viewCoord[1] + cell_height;
	}
	
	void getPlotBounds(int i, int j, double[] plotBound)
	{
		double pw = (m_PlotBounds[i][j][2] - m_PlotBounds[i][j][0]);
		double ph = (m_PlotBounds[i][j][3] - m_PlotBounds[i][j][1]);
		// expand the plot bounds by 20% on each side
		plotBound[0] = m_PlotBounds[i][j][0] - 0.2 * pw;
		plotBound[1] = m_PlotBounds[i][j][1] - 0.2 * ph;
		plotBound[2] = m_PlotBounds[i][j][2] + 0.2 * pw;
		plotBound[3] = m_PlotBounds[i][j][3] + 0.2 * ph;
	}
	
	boolean m_bIsDrawing = false;
	public synchronized void draw()
	{
		try
		{
			if (m_PlotBounds == null || m_PearsonCorrCoeff == null)
			{
				return;
			}
			
			if (numerics.size() < 2)
			{
				background(128 + 64 + 32);
				return;
			}
	
			if (m_bEnableMagnified)
			{
				calcMagnifiedView(0, 0, 0);
			}
			
			m_bIsDrawing = true;
			background(255);//128 + 64 + 32);
			while (this.getOp().isUpdating()) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
	    	if( this.getOp() instanceof SplomOp) {
	    		// draw the cull filling
				
				if(cull_hilight == null || cull_hilight.length != num_numerics ) {
					
					this.heavyResize();
				}
				
	    		noStroke();
	    		fill(255, 255, 0);
	    		for( int i = 0; i < num_numerics; i++ ) {
	 				// horizontal
	 				if( i > 0 ) {
	 					
	 	 				if(cull_hilight[i]) {
	 	 					
	 	 					rect(	BIG_BORDER_H_L - ((float)CELL_SPACING)/2.f,
								 BIG_BORDER_V_TOP + (i-1) * ((float)(cell_height+CELL_SPACING)) - ((float)CELL_SPACING)/2.f,
								 (num_numerics-1) * ((float)(cell_width+CELL_SPACING)),
								 ((float)(cell_height+CELL_SPACING)));
	 	 				}
	 				}
	 				
	 				//vertical
	 				if( i < num_numerics-1 ) {
	 					
	 	 				if(cull_hilight[i]) {
	 	 					
	 	 					rect(	BIG_BORDER_H_L + i * ((float)(cell_width+CELL_SPACING)) - ((float)CELL_SPACING)/2.f,
								 BIG_BORDER_V_TOP - ((float)CELL_SPACING)/2.f,
								 ((float)(cell_width+CELL_SPACING)),
								 (num_numerics-1) * ((float)(cell_height+CELL_SPACING)));
	 	 				}
	 				}
	 			}    		 
				
				// label the axes
				
				fill(0);	
				
				boolean bAllMagnified = true;
				if (bAllMagnified && m_bIsSPLOM)
				{
					drawAxesLabels(false);
					int viewCoord[] = new int[4];
					double plotBound[] = new double[4];
					if (!m_bPlotsPGUpToDate)
					{
						if (m_PlotsPG == null || m_PlotsPG.length != num_numerics || m_PlotsPG[0].length != num_numerics)
						{
							m_PlotsPG = new PGraphics[num_numerics][];
							for (int ipg = 0; ipg < num_numerics; ipg++)
							{
								m_PlotsPG[ipg] = new PGraphics[num_numerics];
							}
						}
					}
					for( int i = 0; i < num_numerics; i++ )
					{
						for( int j = i+1; j < num_numerics; j++ )
						{
							getPlotViewCoordinates(i, j, viewCoord);
							getPlotBounds(i, j, plotBound);
							boolean bReDraw = !m_bPlotsPGUpToDate; 
							if (m_PlotsPG[i][j] == null || m_PlotsPG[i][j].width != cell_width || m_PlotsPG[i][j].height != cell_height)
							{
								m_PlotsPG[i][j] = createGraphics(cell_width, cell_height, P2D);
								bReDraw = true;
							}
							drawMagnifiedPlot(m_PlotsPG[i][j], i, j, plotBound, viewCoord, (j == num_numerics - 1), (i == 0), bReDraw);
						}
					}
					m_bPlotsPGUpToDate = true;
	
				} else
				{
					drawAxesLabels(true);
					pushMatrix();
					translate( BIG_BORDER_H_L, BIG_BORDER_V_TOP );
					// find the proper locations for the plots and draw the plots
					for( int i = 0; i < num_numerics; i++ )
					{
						pushMatrix();
						translate( 0, i*(cell_height + CELL_SPACING) );
						for( int j = i+1; j < num_numerics; j++ )
						{
							drawPlot(i, j);
							translate( 0, cell_height + CELL_SPACING );
						}
						popMatrix();
						translate( cell_width + CELL_SPACING, 0 );
					}
					popMatrix();
				}
				
				
				if (m_bEnableMagnified && m_MagnifiedViewCoords != null)
				{
					// draw the magnified view
					int magnifiedWidth  = m_MagnifiedViewCoords[2] - m_MagnifiedViewCoords[0];
					int magnifiedHeight = m_MagnifiedViewCoords[3] - m_MagnifiedViewCoords[1];
					if (magnifiedWidth > cell_width && magnifiedHeight > cell_height)
					{
						fill(0);
						// draw x label
			    		float fTextHeight = textAscent() + textDescent();
						PVector xlabelpos = new PVector(0.5f*(m_MagnifiedViewCoords[0] + m_MagnifiedViewCoords[2]) ,m_MagnifiedViewCoords[3] + 3*fTextHeight);
						text(this.getOp().input.getColName(numerics.get(selSplom[0])), xlabelpos.x, xlabelpos.y);
						// draw y label
						PVector ylabelpos = new PVector(m_MagnifiedViewCoords[0] - 3*fTextHeight, 0.5f*(m_MagnifiedViewCoords[1] + m_MagnifiedViewCoords[3]));
						pushMatrix();
						translate(ylabelpos.x, ylabelpos.y);
						rotate(-PConstants.PI/2.f); // draw the text vertically
						text(this.getOp().input.getColName(numerics.get(selSplom[1])), 0, 0);
				 		popMatrix();
	
						boolean bReDraw = !m_bMagnifiedPGUpToDate; 
						if (m_MagnifiedPG == null || m_MagnifiedPG.width != magnifiedWidth || m_MagnifiedPG.height != magnifiedHeight)
						{
							m_MagnifiedPG = createGraphics(magnifiedWidth, magnifiedHeight, P2D);
							bReDraw = true;
						}
						drawMagnifiedPlot(m_MagnifiedPG, selSplom[0], selSplom[1], m_MagnifiedPlotBounds, m_MagnifiedViewCoords, true, true, bReDraw);
						m_bMagnifiedPGUpToDate = true;
					}
				}
			}
		}
		catch (Error e)
		{
			System.out.println("Error: PLODSplomPainter.draw()");
			//e.printStackTrace();
		}
		catch (Exception e)
		{
			System.out.println("Exception: PLODSplomPainter.draw()");
			//e.printStackTrace();
		}
		m_bIsDrawing = false;

	}
	
    private void drawAxesLabels(boolean bTicks)
    {
    	int label_loop_bound = num_numerics - 1; 
		for( int i = 0; i < label_loop_bound; i++ )
	    {
			// draw column names for x-axis
	    	textAlign(CENTER);
			fill(0);
			if (i == selSplom[0]) 
				fill(255, 0, 0); // highlight the selected splom	
			
			float x_pos = (float)BIG_BORDER_H_L + i * ((float)(cell_width + CELL_SPACING)) + (float)cell_width/2.f;
			float y_pos = BIG_BORDER_V_TOP + (label_loop_bound) * ((float)(cell_height+CELL_SPACING));

			if (cell_width+CELL_SPACING > textAscent() || i == selSplom[0])
			{// draw axis labels only if there is enough space for it
				if (cell_width+CELL_SPACING < textAscent() * 3.0)
				{
					pushMatrix();
					textAlign(RIGHT);
					rotate(-PConstants.PI/2.f); // change the text orientation
					translate(-y_pos - textAscent(), x_pos);
					stroke(0);
					text(this.getOp().input.getColName(numerics.get(i)), 0, 0);
					popMatrix();
				}
				else
				{
					stroke(0);
					text(this.getOp().input.getColName(numerics.get(i)), x_pos, y_pos + 2.5f * textAscent());
				}
			}
			
			if (bTicks)
			{
				if( m_bIsSPLOM	)
				{
					// draw the coordinate labels for the x-axis
					if (m_bBubblePlot)
					{
						x_pos = (float)BIG_BORDER_H_L + i * ((float)(cell_width + CELL_SPACING)) + axis_spacing_h;
						y_pos = BIG_BORDER_V_TOP + textAscent() + label_loop_bound * ((float)(cell_height+CELL_SPACING));
						String[] alabels = b2ds[i][i+1].bin1.getBinStrings();
						for( int k = 0; k < alabels.length; k++ ) {
							
							String numstr = alabels[k];
							float offs = 0.f;
							if( k == 0 ) {
								textAlign(LEFT);
								offs = -textWidth( numstr )/4f;
							}
							else if( k == alabels.length - 1 ) { 
								textAlign(RIGHT);
								offs = textWidth( numstr )/4f;
							}
							else {
								textAlign(CENTER);
							}
							
							text(	numstr, 
								 x_pos + k*(cell_width-2*axis_spacing_h)/((float)(alabels.length-1)) + offs, 
								 y_pos);		
						}
					}
					else { 
						x_pos = (float)BIG_BORDER_H_L + i * ((float)(cell_width + CELL_SPACING)) + axis_spacing_h;
						y_pos = BIG_BORDER_V_TOP + textAscent() + label_loop_bound * ((float)(cell_height+CELL_SPACING));
						for( int k = 0; k <= axeslines-1; k++ ) {
							
							double num = k*(m_PlotBounds[i][0][2]-m_PlotBounds[i][0][0])/(axeslines-1) + m_PlotBounds[i][0][0];
							String numstr = null;
							if( Math.abs(num) > 100.) {
								
								numstr = m_NFScientific.format( num );
							}
							else {
								
								numstr = m_NumberFormatDigits.format( num );
							}
							float offs = 0.f;
							if( k == 0 ) {
								textAlign(LEFT);
								offs = -textWidth( numstr )/4f;
							}
							else if( k == axeslines - 1 ) { 
								textAlign(RIGHT);
								offs = textWidth( numstr )/4f;
							}
							else {
								textAlign(CENTER);
							}
							
							text(	numstr, 
								 x_pos + k*(cell_width-2*axis_spacing_h)/((float)(axeslines-1)) + offs, 
								 y_pos);	
						}
					}
				}
			}
			
			// draw the column labels for the y-axis
			fill(0);	
			if (i+1 == selSplom[1])
				fill(255, 0, 0); // highlight the selected splom

			x_pos = BIG_BORDER_H_L - textAscent();// - (2*LABEL_SPACING_H + (int)textWidth( "0.##E0" ));
			y_pos = textAscent()/2.f + BIG_BORDER_V_TOP + i * ((float)(cell_height+CELL_SPACING)) + cell_height/2.f;

			if (cell_height+CELL_SPACING > textAscent() || i+1 == selSplom[1])
			{
				if (cell_height+CELL_SPACING < textAscent() * 3.0)
				{
					textAlign(RIGHT);
					text(this.getOp().input.getColName(numerics.get(i+1)), x_pos, y_pos );
				}
				else
				{
					pushMatrix();
					textAlign(CENTER);
					rotate(-PConstants.PI/2.f); // change the text orientation
					translate(-y_pos,x_pos - (2*LABEL_SPACING_H + (int)textWidth( "0.##E0" )));
					text(this.getOp().input.getColName(numerics.get(i+1)), 0.f, 0.f );
					popMatrix();
				}
			}
			
			textAlign(RIGHT);
			// draw the coordinates for the y-axis
			if( m_bIsSPLOM ) {
				
				// draw the y-axes coordinates
				if( m_bBubblePlot ) {
					
					x_pos += (LABEL_SPACING_H+(int)textWidth( "0.##E0" ));
					String[] alabels = b2ds[0][i+1].bin2.getBinStrings();
					for( int k = 0; k < alabels.length; k++ ) {
						
						text(	alabels[(alabels.length-1)-k], 
							 (float)x_pos, 
							 textAscent()/2.f + BIG_BORDER_V_TOP + i * ((float)(cell_height+CELL_SPACING)) + axis_spacing_v + k*(cell_height-2*axis_spacing_v)/((float)alabels.length-1) );					
					}
				}
				else {
					x_pos += (LABEL_SPACING_H+(int)textWidth( "0.##E0" ));
					for( int k = 0; k <= axeslines; k++ ) {
						
						double num = (axeslines-k)*(m_PlotBounds[i+1][0][2]-m_PlotBounds[i+1][0][0])/axeslines + m_PlotBounds[i+1][0][0];
						String numstr = null;
						if( Math.abs(num) > 100.) {
							
							numstr = m_NFScientific.format( num );
						}
						else {
							
							numstr = m_NumberFormatDigits.format( num );
						}
						text(	numstr, 
							 (float)x_pos, 
							 textAscent()/2.f + BIG_BORDER_V_TOP + i * ((float)(cell_height+CELL_SPACING)) + axis_spacing_v + k*(cell_height-2*axis_spacing_v)/((float)axeslines) );					
					}
				}
			}
	    }
    }
    
    private void drawPlot(int i, int j)
    {// previous drawPlot. replaced by drawMagnifiedPlot.
    	int iPlotWidth  = cell_width;
    	int iPlotHeight = cell_height;
    	stroke(128);												
		if (i == selSplom[0] && j == selSplom[1])
			stroke(255, 0, 0);

		if( m_bIsSPLOM )
		{
	    	calcAxis(iPlotWidth, iPlotHeight);
	    	
			// draw the background rect
			fill(255);
			beginShape();
			vertex(0, 0);
			vertex(iPlotWidth, 0);
			vertex(iPlotWidth, iPlotHeight);
			vertex(0, iPlotHeight);
			endShape(CLOSE);
			
			// draw the white background
			if( i != j ) {
				
				stroke(255-32);
				
				// draw the label lines
				for( int k = 0; k < axeslines+1; k++ ) {
					
					line(  (float)iPlotWidth-1.f, 
						 (float)(k*((float)(iPlotHeight-2*axis_spacing_v))/axeslines + axis_spacing_v),
						 1.f,
						 (float)(k*((float)(iPlotHeight-2*axis_spacing_v))/axeslines + axis_spacing_v));
					line(  (float)(k*((float)(iPlotWidth-2*axis_spacing_h))/(axeslines-1) + axis_spacing_h), 
						 (float)iPlotHeight-1.f,
						 (float)(k*((float)(iPlotWidth-2*axis_spacing_h))/(axeslines-1) + axis_spacing_h),
						 1.f);
				}
				
				// draw the axes
				
				stroke(0);
				
				// scale the transformation matrix to the appropriate bounds						
				double currPlotBounds[] = m_PlotBounds[i][j];
				
				pushMatrix();
				// scale and translate (or is it the other way around?)
				translate(axis_spacing_h, axis_spacing_v);
				
				scale((float)(axis_length_h/(currPlotBounds[2] - currPlotBounds[0])), 
					  -(float)(axis_length_v/(currPlotBounds[3] - currPlotBounds[1])) );
				
				translate((float)-currPlotBounds[0],-(float)currPlotBounds[3]);
				
				// point size scaling
	    		double R_x = (currPlotBounds[2] - currPlotBounds[0]) / (iPlotWidth - (2*axis_spacing_h));
	    		double R_y = (currPlotBounds[3] - currPlotBounds[1]) / (iPlotHeight - (2*axis_spacing_v));
				float pointsize_x = (float)(m_iPointSize * R_x);
				float pointsize_y = (float)(m_iPointSize * R_y);
				
				noStroke();
				fill(255, 128, 128, 1);
				
				if( !m_bBubblePlot )
				{// draw the plain points
					fill(64);
					// first plot non-selected points
					for( int k = 0; k < getOp().rows(); k++ )
					{
						if (m_iColorCol != -1)
						{
							fill( (int) getOp().getMeasurement(k, m_iColorCol ) );
						}
						double pointx = getOp().getMeasurement(k,numerics.get(i));
						double pointy = getOp().getMeasurement(k,numerics.get(j));
						if ((m_iSelectionCol == -1 || getOp().getMeasurement(k, m_iSelectionCol) < 1e-5) 
						   && pointx >= currPlotBounds[0] && pointx <= currPlotBounds[2]
						   && pointy >= currPlotBounds[1] && pointy <= currPlotBounds[3])
						{
							ellipse((float)pointx, (float)pointy, pointsize_x, pointsize_y);
						}
					}
					
					// plot selected points
					fill(255,0,0);
					//this.strokeWeight(arg0);
					beginShape(POINTS);
					for( int k = 0; k < getOp().rows(); k++ )
					{
						double pointx = getOp().getMeasurement(k,numerics.get(i));
						double pointy = getOp().getMeasurement(k,numerics.get(j));
						if ((m_iSelectionCol != -1 && getOp().getMeasurement(k, m_iSelectionCol) > 0) 
						   && pointx >= currPlotBounds[0] && pointx <= currPlotBounds[2]
						   && pointy >= currPlotBounds[1] && pointy <= currPlotBounds[3])
						{
							ellipse((float)pointx, (float)pointy, pointsize_x, pointsize_y);
						}
					}
					endShape();
				}
				else { // bubblePlot
					
					pointsize_x = (float)(max_bubble_size * R_x);
					pointsize_y = (float)(max_bubble_size * R_y);
					
					// draw bubbles
					
					int bin_info[][][] = this.b2ds[i][j].get2DBins();
					int max_2dbin_count = this.b2ds[i][j].getMax2DBin();
					double range_x = (currPlotBounds[2] - currPlotBounds[0]);
					double range_y = (currPlotBounds[3] - currPlotBounds[1]);
					
					// handle the case with no color
					
					if (m_iColorCol == -1)
					{
						for( int b1s = 0; b1s < bin_info.length; b1s++ ) {
							
							for( int b2s = 0; b2s < bin_info[0].length; b2s++ ) {
								
								if( bin_info[b1s][b2s][0] > 0 ) {
									
									double ptsize = (Math.sqrt(bin_info[b1s][b2s][0]) / ((double)Math.sqrt(max_2dbin_count)))*(max_bubble_size-m_iPointSize) + m_iPointSize;
									
									ellipse((float) (currPlotBounds[0] + b1s*range_x/((double)bin_info.length - 1)),  
											(float) (currPlotBounds[1] + b2s*range_y/((double)bin_info[0].length - 1)), 
											(float)(ptsize * R_x), 
											(float)(ptsize * R_y));
								}
							}
						}
					}
					else {
						
						ArrayList<Double> colors = this.b2ds[i][j].bin3.getUniqueValues();
						
						for( int b1s = 0; b1s < bin_info.length; b1s++ ) {
							
							for( int b2s = 0; b2s < bin_info[0].length; b2s++ ) {
								
								
								double [] temp_floats = new double[bin_info[0][0].length];
								for( int b3s = 0; b3s < bin_info[0][0].length; b3s++ ) {
									temp_floats[b3s] = bin_info[b1s][b2s][b3s];
								}
								int[] skimbox = FloatIndexer.sortFloats( temp_floats );
								for( int b3s = 0; b3s < bin_info[0][0].length; b3s++ ) {
									
									if( bin_info[b1s][b2s][skimbox[(bin_info[0][0].length-1)-b3s]] > 0 ) {
										
										double ptsize = (Math.sqrt(bin_info[b1s][b2s][skimbox[(bin_info[0][0].length-1)-b3s]]) / 
												        ((double)Math.sqrt(max_2dbin_count)))*(max_bubble_size-m_iPointSize) + m_iPointSize;
										
										fill( colors.get( skimbox[(bin_info[0][0].length-1)-b3s]).intValue() );
										ellipse((float) (currPlotBounds[0] + b1s*range_x/((double)bin_info.length - 1)),  
												(float) (currPlotBounds[1] + b2s*range_y/((double)bin_info[0].length - 1)), 
												(float)(ptsize * R_x), 
												(float)(ptsize * R_y));
									}
								}
							}
						}
					}
				}
				
				stroke(0);
				
				popMatrix();
				
			}
			
			// is the selection on?
			if( !m_bSelectionMagnified && m_bSelectionOn && selSplom[0]==i && selSplom[1]==j)
			{
				// draw the selection box
				fill(32,32);
				rect(selBoxCoords.x, selBoxCoords.y, selBoxDims.x, selBoxDims.y);
			}
			
		}
		else { // isBOM
			// draw a box with a color representing correlation
			// set the color
			int m_CorrColorFrom = color(44, 123, 182);
			int m_CorrColorZero = color(255, 255, 191);
			int m_CorrColorTo 	= color(215, 25, 28);
			
			int is_color =  -1;
			float corr_val = (float) Math.min(Math.max(-1.0, m_PearsonCorrCoeff[i][j]), 1.0);
			if( Float.isNaN(corr_val) || Float.isInfinite(corr_val)) {
				corr_val = 0.f;
			}
			if( corr_val < 0.f ) {
				is_color = lerpColor( m_CorrColorZero, m_CorrColorTo, corr_val*(-1.f) ); 
			}
			else {
				is_color = lerpColor( m_CorrColorZero, m_CorrColorFrom, corr_val ); 
			}
			fill(is_color);
			if (iPlotWidth < 10 && (i != selSplom[0] || j != selSplom[1]))
			{
				noStroke();
			}

			rect(0, 0, iPlotWidth, iPlotHeight);
						
			if (iPlotWidth > textAscent())
			{
				// label the data
				fill( 0 );
				textAlign(CENTER);
				int x_pos = (int)Math.round(iPlotWidth/2);
				int y_pos = (int)Math.round(iPlotHeight/2);
				String form_string = m_NumberFormatDigits.format(Math.max(Math.min(1.0,m_PearsonCorrCoeff[i][j]),-1.0));
				if( Double.isNaN(Math.max(Math.min(1.0,m_PearsonCorrCoeff[i][j]),-1.0)) || Double.isInfinite(Math.max(Math.min(1.0,m_PearsonCorrCoeff[i][j]),-1.0)))
				{
					form_string = "N/A";
				}
				
				if (iPlotWidth > this.textWidth(form_string))
				{
					text(form_string, x_pos, y_pos );
				}
			}
		}
	}
	
	int pickPointMagnified(int x, int y)
	{
 		int iPicked = -1;
		double fVPRatioH = 1.0 * (m_MagnifiedViewCoords[2] - m_MagnifiedViewCoords[0]) / 
 								 (m_MagnifiedPlotBounds[2] - m_MagnifiedPlotBounds[0]); // horizontal view-to-screen ratio
 		double fVPRatioV = 1.0 * (m_MagnifiedViewCoords[3] - m_MagnifiedViewCoords[1]) / 
 								 (m_MagnifiedPlotBounds[3] - m_MagnifiedPlotBounds[1]); // vertical view-to-screen ratio
		for( int k = 0; k < getOp().rows(); k++ )
		{
			// use the color for non-selected ones in pass 0
			double dx = getOp().getMeasurement(k,numerics.get(selSplom[0]));
			double dy = getOp().getMeasurement(k,numerics.get(selSplom[1]));
			if (dx >= m_MagnifiedPlotBounds[0] && dx <= m_MagnifiedPlotBounds[2] &&
			    dy >= m_MagnifiedPlotBounds[1] && dy <= m_MagnifiedPlotBounds[3])
			{
				float pointx = (float)(m_MagnifiedViewCoords[0] + (dx - m_MagnifiedPlotBounds[0]) * fVPRatioH);
				float pointy = (float)(m_MagnifiedViewCoords[3] - (dy - m_MagnifiedPlotBounds[1]) * fVPRatioV);
				if (Math.abs(pointx - x) <= m_iPointSize && Math.abs(pointy - y) <= m_iPointSize)
				{
					iPicked = k;
				}
			}
		}
		return iPicked;
	}
	
	void drawDensityPlot(PGraphics pg, int pi, int pj, double[] plotBounds, int viewCoords[])
	{
		if (pg == null)
			return;
		
 		int iMagnifiedViewW = viewCoords[2] - viewCoords[0];
 		int iMagnifiedViewH = viewCoords[3] - viewCoords[1];
 		double fVPRatioH = 1.0 * iMagnifiedViewW / (plotBounds[2] - plotBounds[0]); // horizontal view-to-screen ratio
		double fVPRatioV = 1.0 * iMagnifiedViewH / (plotBounds[3] - plotBounds[1]); // vertical view-to-screen ratio
			
		int kernel[][] = {// 5x5 Gaussian Smoothing Kernel
				{ 0, 4, 7, 4, 0},
				{ 4,16,26,16, 4},
				{ 7,26,41,26, 7},
				{ 4,16,26,16, 4},
				{ 0, 4, 7, 4, 0}}; 
		PGradient.init(this);
		int [][] iDensity = new int[iMagnifiedViewW][iMagnifiedViewH];
		double [][][] dColor = new double[iMagnifiedViewW][iMagnifiedViewH][3];
		
		float fMaxVal = 0;
		for( int k = 0; k < getOp().rows(); k++ )
		{
			double dx = getOp().getMeasurement(k,numerics.get(pi));
			double dy = getOp().getMeasurement(k,numerics.get(pj));
			float pointx = (float)((dx - plotBounds[0]) * fVPRatioH);
			float pointy = (float)((dy - plotBounds[1]) * fVPRatioV);

			//colorCol
			int col = color(0, 0, 0);
			if (m_iSelectionCol != -1 && getOp().getMeasurement(k, m_iSelectionCol) > 0)
			{
				col = color(255, 0, 0);
			}
			else if (m_iColorCol != -1)
			{
				col = (int)getOp().getMeasurement(k, m_iColorCol);
			}
			
			// convolution
			for (int kx = 0; kx < kernel.length; kx++)
			{
				for (int ky = 0; ky < kernel.length; ky++)
				{
					int ix = (int)pointx + kx - kernel.length/2;
					int iy = (int)pointy + ky - kernel.length/2;
 					if (ix >= 0 && ix < iMagnifiedViewW && iy >= 0 && iy < iMagnifiedViewH)
 					{
 						for (int ch = 0; ch < 3; ch++) // color channels
 						{
 							float chVal = ch == 0 ? red(col) : ch == 1 ? green(col) : blue(col);
 							dColor[ix][iy][ch] += kernel[kx][ky] * (255 - chVal);
 						}
 						iDensity[ix][iy] += kernel[kx][ky];
						fMaxVal = Math.max(fMaxVal, (float)iDensity[ix][iy]);
// 						if (m_bDrawDensityPlotLog)
// 							fMaxVal = Math.max(fMaxVal, (float)Math.log(iDensity[ix][iy] + 1));
// 						else
// 							fMaxVal = Math.max(fMaxVal, (float)iDensity[ix][iy]);
 					}
				}
			}
		}
		pg.loadPixels();
//		PGradient gradient = PGradient.GRAY;
		for (int x = 0; x < iMagnifiedViewW; x++)
		{
			for (int y = 0; y < iMagnifiedViewH; y++)
			{
				if (iDensity[x][y] == 0)
					continue;
				double ratio = m_bDrawDensityPlotLog ? Math.log(iDensity[x][y]) / (Math.log(fMaxVal) * iDensity[x][y]) : 1.0 / fMaxVal;
		    	int clr = color(255-(int)(dColor[x][y][0]*ratio),
    				    		255-(int)(dColor[x][y][1]*ratio),
    				    		255-(int)(dColor[x][y][2]*ratio));
//				int idx = 0;
//				if (m_bDrawDensityPlotLog)
//					idx = (int) map((float)Math.log(iDensity[x][y] + 1), log(1), fMaxVal, 0, gradient.size-1);
//				else
//					idx = (int) map(iDensity[x][y], 0, fMaxVal, 0, gradient.size-1);
//				int clr = gradient.getColor(idx);
				pg.pixels[(pg.height - y - 1) * pg.width + x] = clr;
		    }
		}
		pg.updatePixels();		
	}
	
	void drawMagnifiedPlot(PGraphics pg1, int pi, int pj, double plotBound[], int[] viewCoord, boolean bLabelX, boolean bLabelY, boolean bReDrawPG)
	{
 		fill(255);
 		stroke(0);
 		if (pi == selSplom[0] && pj == selSplom[1])
 			stroke(255, 0, 0);
 		
 		beginShape();
 		vertex(viewCoord[0] - 1, viewCoord[1] - 1);
 		vertex(viewCoord[2] + 1, viewCoord[1] - 1);
 		vertex(viewCoord[2] + 1, viewCoord[3] + 1);
 		vertex(viewCoord[0] - 1, viewCoord[3] + 1);
 		endShape(CLOSE);
 		
 		int viewWidth  = viewCoord[2] - viewCoord[0];
 		int viewHeight = viewCoord[3] - viewCoord[1];
		if (bReDrawPG)
		{
	 		pg1.beginDraw();
	 		pg1.fill(255);
	 		pg1.noStroke();
	 		pg1.rect(0, 0, viewWidth, viewHeight);
			pg1.translate(-viewCoord[0], -viewCoord[1]);
		}
		
 		double fVPRatioH = 1.0 * viewWidth / (plotBound[2] - plotBound[0]); // horizontal view-to-screen ratio
 		double fVPRatioV = 1.0 * viewHeight / (plotBound[3] - plotBound[1]); // vertical view-to-screen ratio
		
    	float fTextHeight = textAscent() + textDescent();
    	textAlign(CENTER);
    	fill(255, 0, 0);
			
		if (m_iMagnifiedPoint != -1 && m_iMetaDataCol != -1 && m_bShowMetaData)
		{// draw the picked point in detailed view
			if (bReDrawPG)
			{
				m_Viewer.drawPointDetailed(pg1, m_iMagnifiedPoint, viewCoord[0] , viewCoord[1], viewWidth, viewHeight);
			}
			String sMetaData = op.getMetaData(m_iMetaDataCol, m_iMagnifiedPoint);
			PVector xlabelpos = new PVector(0.5f*(viewCoord[0] + viewCoord[2]) ,viewCoord[3] + 1*fTextHeight);
			text(sMetaData, xlabelpos.x, xlabelpos.y);
		}
		else 
	 	{
	 		double MIN_MAGNIFIED_STEP_SIZE = 40; // determies the minium distance between the coordinate lines
	 		pg1.stroke(255-32); // coordinates line color
	 		fill(0); // coordinates text color
	 		textAlign(CENTER); // alignment for coordinate labels
			final int X_AXIS = 0, Y_AXIS = 1;
	 		for (int axis = X_AXIS; axis <= Y_AXIS; axis++)
	 		{
	 			int i1 = 0; // min x
	 			int i2 = 2; // max x
	 			double fVPRatio = fVPRatioH; // view to plot ratio
	 			if (axis == Y_AXIS)
	 			{
	 				i1 = 1; // min y
	 				i2 = 3; // max y
	 				fVPRatio = fVPRatioV;
	 			}
				
	 			double fMin = m_PlotBounds[pi][pj][i1];
	 			double fMax = m_PlotBounds[pi][pj][i2];
	 			double fStepSize = 0.5 * (fMax - fMin);
	 			while (fStepSize * fVPRatio > MIN_MAGNIFIED_STEP_SIZE)
	 			{
	 				double fCurr = fMin;
	 				while (fCurr <= fMax + 1e-5)
	 				{
	 					if (fCurr >= plotBound[i1])
	 					{
	 						if (fCurr <= plotBound[i2])
	 						{
								//String numstr = null;
								//if( Math.abs(fCurr) > 100.)
								//	numstr = m_NFScientific.format( fCurr );
								//else
								//	numstr = m_NumberFormatDigits.format( fCurr );
	 							if (axis == X_AXIS)
	 							{
	 								PVector tickpos = new PVector((float) (viewCoord[i1] + (fCurr - plotBound[i1]) * fVPRatio),
	 										 					  (float) (viewCoord[3]) + fTextHeight);
	 								if (bReDrawPG)
	 								{
	 									pg1.line(tickpos.x, (float) (viewCoord[1]), tickpos.x, (float) (viewCoord[3]));
	 								}
	 								if (bLabelX)
	 								{
	 									text((float)fCurr, tickpos.x, tickpos.y);
	 								}
	 							}
	 							else //Y_AXIS
	 							{
	 								PVector tickpos = new PVector((float) (viewCoord[0]),
	 														      (float) (viewCoord[i2] - (fCurr - plotBound[i1]) * fVPRatio));
	 								if (bReDrawPG)
	 								{
	 									pg1.line((float) (viewCoord[0]), tickpos.y, (float) (viewCoord[2]), tickpos.y);
	 								}
	 								
	 								if (bLabelY)
	 								{
		 								pushMatrix();
		 								translate(tickpos.x, tickpos.y);
		 								rotate(-PConstants.PI/2.f); // draw the text vertically
	 									text((float)fCurr, 0, -5);
		 								popMatrix();
	 								}
	 							}
	 						}
	 						else
	 						{
	 							fMax = fCurr;
	 							break;
	 						}
	 					}
	 					else
	 					{
	 						fMin = fCurr;
	 					}
	 					fCurr += fStepSize;
	 				}
	 				
	 				fStepSize /= 2;
	 			}
			}

	 		if (bReDrawPG)
		 	{
		 		pg1.noStroke();
		 		pg1.fill(128);
		 		if (m_bDrawDensityPlot)
		 		{
		 			drawDensityPlot(pg1, pi, pj, plotBound, viewCoord);
		 		}
		 		else
		 		{
		 			int iNumPass = m_iSelectionCol != -1 ? 2 : 1;
		 			for (int pass = 0; pass < iNumPass; pass++)
			 		{
		 				pg1.fill(0, 0, 0, m_iPointAlpha);
			 			if (pass == 1)
			 			{// use red to plot selected points in the pass 1
			 				pg1.fill(255, 0, 0, m_iPointAlpha);
			 			}
			 			
			 			for( int k = 0; k < getOp().rows(); k++ )
			 			{// use the color for non-selected ones in pass 0
							if (pass == 0 && m_iColorCol != -1)
			 				{
								int colRGBA = (int) getOp().getMeasurement(k, m_iColorCol );
								colRGBA = (colRGBA & 0xFFFFFF) | (m_iPointAlpha << 24);
								pg1.fill(colRGBA);
							}
			 				double dx = getOp().getMeasurement(k,numerics.get(pi));
			 				double dy = getOp().getMeasurement(k,numerics.get(pj));
			 				
			 				if (dx >= plotBound[0] && dx <= plotBound[2] &&
			 			        dy >= plotBound[1] && dy <= plotBound[3] &&
		 						(m_iSelectionCol == -1 ||
		 						 (pass == 0 && getOp().getMeasurement(k, m_iSelectionCol) < 1e-5) || // non selected in pass 0
		 						 (pass == 1 && getOp().getMeasurement(k, m_iSelectionCol) > 0)) // selected in pass 1
							   )
			 				{
			 					float pointx = (float)(viewCoord[0] + (dx - plotBound[0]) * fVPRatioH);
			 					float pointy = (float)(viewCoord[3] - (dy - plotBound[1]) * fVPRatioV);
			 					
			 					if (pg1 != null && m_iMetaDataCol != -1 && m_bShowMetaData)
			 					{
			 						try
			 						{
			 				 			pg1.noStroke();
			 	 						pg1.rect(pointx - m_iPointSize - 1, pointy - m_iPointSize - 1, 2 + m_iPointSize*2, 2 + m_iPointSize*2);
			 							m_Viewer.drawPoint(pg1, k, pointx - m_iPointSize, pointy - m_iPointSize, m_iPointSize * 2, m_iPointSize * 2);
			 						}
			 						catch (Exception e)
			 						{
			 						}
			 					}
			 					else
			 					{
			 						pg1.ellipse(pointx, pointy, m_iPointSize, m_iPointSize);
			 					}
			 				}
			 			}// for (k
			 		}// for (pass
		 		}//if (!bDensityPlot
		 	}
 		}
		
		if (bReDrawPG)
		{
			pg1.endDraw();
		}
		
		imageMode(CORNER);
		image(pg1, viewCoord[0], viewCoord[1]);
 		
		//if (m_bSelectionMagnified && m_bSelectionOn)
		if (m_bSelectionOn)
		{	// draw the selection box
			stroke(0);
			fill(32, 16);
			rect(selBoxCoords.x, selBoxCoords.y, selBoxDims.x, selBoxDims.y);
		}
 		
	}
	
	public void mouseReleased()
	{
		// are we creating a box?
		if (m_bSelectionOn)
		{
			selectionUpdate();
			m_bPlotsPGUpToDate = false;
			m_bMagnifiedPGUpToDate = false;
			invokeRedraw();
			m_bSelectionOn = false;
			m_bSelectionMagnified = false;
			invokeRedraw();
		}
	}
	
	public void selectionUpdate() 
	{
		
		if (numerics.size() < 2)
			return;
		
		// compute transformation from screen to data coordinates
		
		if( selSplom[0] != selSplom[1])
		{
			double a = 0, b = 0, c = 0, d = 0;
			
			// scatterplot selection routine
			int [] viewCoord;
			double [] plotBound;
			if (m_bSelectionMagnified)
			{
				viewCoord = m_MagnifiedViewCoords;
				plotBound = m_MagnifiedPlotBounds;
			} else
			{
				viewCoord = new int[4];
				plotBound = new double[4];
				getPlotBounds(selSplom[0], selSplom[1], plotBound);
				getPlotViewCoordinates(selSplom[0], selSplom[1], viewCoord);
			}	
			double R_x = (plotBound[2] - plotBound[0]) /  // horizontal plot-to-view ratio
			             (viewCoord[2] - viewCoord[0]);
			double R_y = (plotBound[3] - plotBound[1]) /  // vertical plot-to-view ratio
			             (viewCoord[3] - viewCoord[1]);
			a = (Math.min(selBoxCoords.x, selBoxCoords.x + selBoxDims.x) - viewCoord[0]) * R_x + plotBound[0]; // left
			b = (-Math.max(selBoxCoords.y, selBoxCoords.y + selBoxDims.y) + viewCoord[3]) * R_y + plotBound[1]; // top
			c = (Math.max(selBoxCoords.x, selBoxCoords.x + selBoxDims.x) - viewCoord[0]) * R_x + plotBound[0]; // right
			d = (-Math.min(selBoxCoords.y, selBoxCoords.y + selBoxDims.y) + viewCoord[3]) * R_y + plotBound[1]; // bottom
			
			// determine membership
			for( int k = 0; k < getOp().rows(); k++ )
			{
				double v0 = getOp().getMeasurement(k,numerics.get(selSplom[0]));
				double v1 = getOp().getMeasurement(k,numerics.get(selSplom[1]));
				if (v0 >= a && v0 <= c && v1 >= b && v1 <= d)
				{
					getOp().setMeasurement(k, m_iSelectionCol, m_bKeyAltPressed ? 0.0 : 1.0);
				}
				else {
					if (!m_bKeyShiftPressed && !m_bKeyAltPressed)
					{
						getOp().setMeasurement(k, m_iSelectionCol, 0.0);
					}
				}
			}
			
			getOp().tableChanged(new TableEvent( getOp(), TableEvent.TableEventType.ATTRIBUTE_CHANGED, "selection", null, false), true); // downstream
			getOp().tableChanged(new TableEvent( getOp(), TableEvent.TableEventType.ATTRIBUTE_CHANGED, "selection", null, true), true); // upstream
		}
	}
	
	public void mouseDragged()
	{
		if (m_bIsDrawing)
			return;
		
		if (mouseButton == RIGHT)
		{
			calcMagnifiedView(0, mouseX - pmouseX, mouseY - pmouseY);
			m_bMagnifiedPGUpToDate = false;
			invokeRedraw();
			return;
		}
		
		// are we creating a box?
		//m_bSelectionOn = true;
		if (m_bSelectionOn)
		{
			PVector localCoords = null;
			int[] viewCoord = null;
			if (m_bSelectionMagnified)
			{
				viewCoord = m_MagnifiedViewCoords;
			}
			else
			{
				viewCoord = new int[4];
				getPlotViewCoordinates(selSplom[0], selSplom[1], viewCoord);
			}
			
			localCoords = new PVector(Math.max(Math.min(mouseX, viewCoord[2]) , viewCoord[0]),
									  Math.max(Math.min(mouseY, viewCoord[3]) , viewCoord[1]));
			PVector diffCoords = PVector.sub(localCoords, selBoxCoords);
			selBoxDims = diffCoords;
			
			invokeRedraw();
		}
	}
	
	public void mouseMoved()
	{
		//invokeRedraw();    	 
    }
	
	public void mousePressed()
	{
		if (mouseButton != LEFT)
			return;
		
		if (m_bEnableMagnified && m_MagnifiedViewCoords != null && 
			mouseX > m_MagnifiedViewCoords[0] && mouseX < m_MagnifiedViewCoords[2] && 
			mouseY > m_MagnifiedViewCoords[1] && mouseY < m_MagnifiedViewCoords[3])
		{
			if (mouseEvent.getClickCount() == 2)// double clicked
			{
				if (m_iMagnifiedPoint == -1)
				{
					m_iMagnifiedPoint = pickPointMagnified(mouseX, mouseY);
					if (m_iMagnifiedPoint == -1) // the double click didn't pick any point.
					{
						setMagnifiedMax(!m_bIsMagnifiedMax);
						heavyResize();
					}
				}
				else
				{
					m_iMagnifiedPoint = -1;
				}
				m_bMagnifiedPGUpToDate = true;
				invokeRedraw();
			}
			else
			{
	     	 	// record the start point in local coordinates
				selBoxCoords = new PVector(mouseX, mouseY);
				selBoxDims = new PVector(0,0);
				m_bSelectionOn = true;
				m_bSelectionMagnified = true;
				//invokeRedraw();
			}
		}
		else
		{
			// find the nearest Splom
			int [] newSelSplom = new int[2];
			m_bInSplom = nearestSplom(newSelSplom);
			
			// are we in the splom region?
			if( m_bInSplom )
			{
				int si = min(newSelSplom[0], num_numerics - 1);
				int sj = min(newSelSplom[1] + 1, num_numerics - 1);
				
				if (selSplom != null && selSplom[0] == si && selSplom[1] == sj)
				{// start the selection if the selected splom is already selected
					m_bSelectionOn = true;
					m_bSelectionMagnified = false;
					selBoxCoords = new PVector(mouseX, mouseY);
					selBoxDims = new PVector(0,0);
				}

				if (si != newSelSplom[0] || sj != newSelSplom[1])
				{
					setSelectedSplom(si, sj);
				}
	         	// update the screen (run draw once)
				invokeRedraw();
			}
			else {
				
				// determine if we are over one of the labels
	 			int label_loop_bound = num_numerics - 1;
				boolean in_label = false;
				int in_label_index = -1;
				
				for( int i = 0; i < label_loop_bound; i++ ) {
					
					float x_pos = (float)BIG_BORDER_H_L + i * ((float)(cell_width + CELL_SPACING)) + (float)cell_width/4.f;
					float y_pos = BIG_BORDER_V_TOP + (label_loop_bound) * ((float)(cell_height+CELL_SPACING)) + 2.5f*textAscent();
					float labelTextWidth = this.textWidth(this.getOp().input.getColName(numerics.get(i)));
					if( ( mouseX >= x_pos  && mouseX <= (x_pos+labelTextWidth) ) &&
					   ( mouseY >= y_pos-textAscent()  && mouseY <= (y_pos+textDescent() ) ) ) {
						
						in_label = true;
						in_label_index = i;
						break;
					}
					
					x_pos = BIG_BORDER_H_L - (2*LABEL_SPACING_H+(int)textWidth( "0.##E0" ));
					y_pos = textAscent()/2.f + BIG_BORDER_V_TOP + i * ((float)(cell_height+CELL_SPACING)) + cell_height/2.f;
					//System.out.println("x:"+mouseX+" y:"+mouseY +" versus ("+(x_pos-textAscent())+","+(x_pos+textDescent())+") and ("+(y_pos+labelTextWidth/2.f)+","+(y_pos-labelTextWidth/2.f )+")");
					if( ( mouseX >= x_pos-textAscent()  && mouseX <= (x_pos+textDescent()) ) &&
					   ( mouseY <= y_pos+labelTextWidth/2.f  && mouseY >= (y_pos-labelTextWidth/2.f ) ) ) {
						
						in_label = true;
						in_label_index = i+1;
						break;
					}
				}    		 
				
				if( in_label ) {
					
					cull_hilight[in_label_index] = !cull_hilight[in_label_index];
					invokeRedraw();
				}
			}
		}
	}
	
	boolean m_bKeyAltPressed = false;
	boolean m_bKeyShiftPressed = false;
	public void keyPressed()
	{
		if (key == CODED)
		{
			if (keyCode == ALT)
			{
				m_bKeyAltPressed = true;
			} else if (keyCode == SHIFT)
			{
				m_bKeyShiftPressed = true;
			} 
		}
	}
	
	public void keyReleased()
	{
		if (key == CODED)
		{
			if (keyCode == ALT)
			{
				m_bKeyAltPressed = false;
			} else if (keyCode == SHIFT)
			{
				m_bKeyShiftPressed = false;
			} 
		}
	}	
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -8429666944296674336L;
	
	@Override
	public void stateChanged(ChangeEvent e) {
		
		if( e.getSource() instanceof JSlider ) {
			
			JSlider source = (JSlider)e.getSource();
			
			if( source.getName().equalsIgnoreCase("slider")) {
			    if (!source.getValueIsAdjusting()) {
			        int val = ((int)source.getValue())*50 + 50;
			        
					//			        System.out.println( "val = " + val );	
			        
			        minSplomBound 	= val;
					
			        // count the number of dimensions
					
					int span_h = (BIG_BORDER_H_L+BIG_BORDER_H_R) + (num_numerics * minSplomBound) + (num_numerics-1)*CELL_SPACING;
					int span_v = (BIG_BORDER_V_TOP+BIG_BORDER_V_BOT) + (num_numerics * minSplomBound) + (num_numerics-1)*CELL_SPACING;
					
					// compute the minimum size
					size(span_h, span_v );
					
					SwingUtilities.invokeLater(new Runnable() {
											   public void run() {
											   invalidate();
											   // Set the preferred size so that the layout managers can handle it
											   getParent().getParent().validate();
											   heavyResize();
											   invokeRedraw();
											   }
											   });
			    }
			    
			}
			else if (source.getName().equalsIgnoreCase("hist_slider")) {
			    if (!source.getValueIsAdjusting()) {
					
			    	hist_bins = ((int)source.getValue())+20;
			    	m_bPlotsPGUpToDate = false;
					m_bMagnifiedPGUpToDate = false;
			    	heavyResize();
			    	invokeRedraw();
			    }
			}
			else if (source.getName().equalsIgnoreCase("size_slider")) {
			    if (!source.getValueIsAdjusting()) {
					
			    	m_iPointSize = ((int)source.getValue())+3;
			    	m_bPlotsPGUpToDate = false;
					m_bMagnifiedPGUpToDate = false;
			    	invokeRedraw();
			    }
			}
			else if (source.getName().equalsIgnoreCase("alpha_slider")) {
			    if (!source.getValueIsAdjusting()) {
					
			    	m_iPointAlpha = (int)source.getValue();
			    	m_bPlotsPGUpToDate = false;
					m_bMagnifiedPGUpToDate = false;
			    	invokeRedraw();
			    }
			}
			
			
		}
	}
}
