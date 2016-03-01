package still.operators;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.io.Serializable;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Hashtable;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.rosuda.JRI.Rengine;
import org.rosuda.REngine.REngine;

import cern.colt.matrix.DoubleMatrix2D;

//import org.jblas.DoubleMatrix;
//import org.jblas.Eigen;
//import org.jblas.util.ArchFlavor;
//import org.jblas.util.LibraryLoader;

import still.data.DimensionDescriptor;
import still.data.FloatIndexer;
import still.data.Function;
import still.data.Group;
import still.data.Map;
import still.data.MathLibs;
import still.data.Operator;
import still.data.Table;
import still.data.TableEvent;
import still.data.TableFactory;
import still.gui.OperatorView;
import still.gui.ScreePlot;
import weka.clusterers.SpectralClusterer;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;

public class PCAOp extends Operator implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2136289045510700058L;
	protected int outdims = -1;
	protected String description = "PCA Operator Description";
	double[] e_vals = null;
	protected boolean append_pc;
	protected boolean m_bRecalcEigen = true;
	
	public double[] getEigenValues( ) {
		
		return e_vals;
	}
	
	public String toString() {
		
		return "[Reduce:PCA]";
	}
	
	public String getParamString() {
		
		return "PC's = "+(outdims);
	}
	
	public ArrayList<Integer> numericIndicesFromOutdims() {
		
		ArrayList<Integer> retVal = new ArrayList<Integer>();
		for( int i = 0; i < outdims; i++ ) {
			
			retVal.add(i);
		}
		
		return retVal;
	}
	
	public ArrayList<Integer> nonNumericIndicesFromOutdims() {
		
		ArrayList<Integer> retVal = new ArrayList<Integer>();
		for( int i = outdims; i < outdims+getNonNumericIndices( input ).size(); i++ ) {
			
			retVal.add(i);
		}
		
		return retVal;
	}
	

	public String getSaveString( ) {
		
		String saveString = "";

		saveString += outdims;
				
		return saveString;
	}

	public PCAOp( Table newTable, boolean isActive, String paramString ) {
		
		super( newTable );
		
		outdims = Integer.parseInt( paramString );
		if( outdims > getNumericIndices( input ).size() ) {
			
			outdims = getNumericIndices( input ).size();
		}
		
		this.isActive = isActive;
		if( isActive ) {
			
			map 			= Map.fullBipartiteExcept( 	getNumericIndices( input ), 
														numericIndicesFromOutdims(),			
														getNonNumericIndices( input ),
														nonNumericIndicesFromOutdims(),
														input.columns(), outdims+getNonNumericDims(input));
			function 		= new PCAFunction( newTable, getNumericIndices( input ), getNonNumericIndices( input ), outdims, true);
			e_vals = ((PCAFunction)function).getEigenValues();
			isLazy  		= true;
			setView( new PCAView( this ) );
		}
	}
	
	public PCAOp( Table newTable, boolean isActive ) {
		
		super( newTable );
		
		this.isActive = isActive;
		if( isActive ) {
			
			outdims 		= getNumericIndices( input ).size();
			if( append_pc ) {
				
				map 			= Map.fullBipartiteAppend( 	getNumericIndices( input ), 
															getNonNumericIndices( input ),									
															input.columns(),
															outdims);
			}
			else {
				
				map 			= Map.fullBipartiteExcept( 	getNumericIndices( input ), 
															numericIndicesFromOutdims(),			
															getNonNumericIndices( input ),
															nonNumericIndicesFromOutdims(),
															input.columns(), outdims+getNonNumericDims(input));
			}
			function 		= new PCAFunction( newTable, getNumericIndices( input ), getNonNumericIndices( input ), outdims, true);
			e_vals = ((PCAFunction)function).getEigenValues();
			isLazy  		= true;
			setView( new PCAView( this ) );
		}
	}
	
	public PCAOp( Table newTable, boolean isActive, int outdims) {
		
		super( newTable );
		
		this.isActive = isActive;
		this.outdims 	= outdims;
		if( isActive ) {
			
			if( append_pc ) {
				
				map 			= Map.fullBipartiteAppend( 	getNumericIndices( input ), 
															getNonNumericIndices( input ),									
															input.columns(),
															outdims);
			}
			else {
				
				map 			= Map.fullBipartiteExcept( 	getNumericIndices( input ), 
															numericIndicesFromOutdims(),			
															getNonNumericIndices( input ),
															nonNumericIndicesFromOutdims(),
															input.columns(), outdims+getNonNumericDims(input));
			}
			function 		= new PCAFunction( newTable, getNumericIndices( input ), getNonNumericIndices( input ), outdims, true);
			e_vals = ((PCAFunction)function).getEigenValues();
			isLazy  		= true;
			setView( new PCAView( this ) );
		}
	}
	
	public void activate() {
	
		this.isActive = true;
		
		if( this.outdims < 0 ) {
		
			this.outdims = getNumericIndices( input ).size();
		}
		
		if( append_pc ) {
			
			map 			= Map.fullBipartiteAppend( 	getNumericIndices( input ), 
														getNonNumericIndices( input ),									
														input.columns(),
														outdims);
		}
		else {
			
			map 			= Map.fullBipartiteExcept( 	getNumericIndices( input ), 
														numericIndicesFromOutdims(),			
														getNonNumericIndices( input ),
														nonNumericIndicesFromOutdims(),
														input.columns(), outdims+getNonNumericDims(input));
		}
		function 		= new PCAFunction( input, getNumericIndices( input ), getNonNumericIndices( input ), outdims, true);
		e_vals = ((PCAFunction)function).getEigenValues();
		isLazy  		= true;
		setView( new PCAView( this ) );
	}
	
	public void updateMap() {
		
		if( outdims > getNumericIndices( input ).size() ) {
			
			outdims = getNumericIndices( input ).size();
		}
		
		if( append_pc ) {
			
			map 			= Map.fullBipartiteAppend( 	getNumericIndices( input ), 
														getNonNumericIndices( input ),									
														input.columns(),
														outdims);
		}
		else {
			
			map 			= Map.fullBipartiteExcept( 	getNumericIndices( input ), 
														numericIndicesFromOutdims(),			
														getNonNumericIndices( input ),
														nonNumericIndicesFromOutdims(),
														input.columns(), outdims+getNonNumericDims(input));
		}
	}
	
	public void updateFunction() {
		
		function 		= new PCAFunction( input, getNumericIndices( input ),getNonNumericIndices( input ), outdims, m_bRecalcEigen);
		e_vals = ((PCAFunction)function).getEigenValues();
		
		ArrayList<Double> univarQuant = new ArrayList<Double>();
		ArrayList<String> univarNames = new ArrayList<String>();
		ArrayList<Integer> dtcols = getDimTypeCols(ColType.NUMERIC);
		int p = 0;
		for( double v : getEigenValues() ) {
			univarQuant.add( v );
			univarNames.add( input.getColName(dtcols.get(p)));
			p++;
		}
		((PCAView)view).scree.setUnivar(univarQuant,univarNames);
	}
		

	public class PCAView extends OperatorView implements ChangeListener {

//		JSlider slider = null;
		ScreePlot scree = null;
		JCheckBox jcb = new JCheckBox( "Use Log Scale" );
		JCheckBox jcbAppend = new JCheckBox( "Append Components" );
		JPanel optionPanel = new JPanel();
		
		public PCAView(PCAOp o) {
			super(o);

			jcbAppend.addActionListener(this);
			Hashtable<Integer,JLabel> labelTable = new Hashtable<Integer,JLabel>();
			int k = 1;
			
			for( int i : o.getNumericIndices(input) ) {
				labelTable.put(new Integer(k),new JLabel(""+k));
				k++;
			}

			ArrayList<Double> univarQuant = new ArrayList<Double>();
			ArrayList<String> univarNames = new ArrayList<String>();
			ArrayList<Integer> dtcols = o.getDimTypeCols(ColType.NUMERIC);
			int p = 0;
			for( double v : o.getEigenValues() ) {
				
				univarQuant.add( v );
				univarNames.add( o.input.getColName( dtcols.get(p) ) );
				p++;
			}
			scree = new ScreePlot(	univarQuant,
									univarNames,
									true,
									new Comparator<Double>() {
										public int compare(Double o1, Double o2) {
											if( o1 < o2 ) {
												return 1;
											}
											else if( o1 > o2 ) {
												return -1;
											}
											return 0;
										}
									},
									null);
			scree.cutoff = o.outdims - 1;
			scree.useDimensionNames = false;
			scree.isCutoffLeft = true;
			
			scree.addLogStateCheckbox( jcb );
			scree.addChangeListener( this );
			
			this.add(scree, "Center");
			optionPanel.setLayout( new GridLayout(1,2) );
			optionPanel.add( jcb );
			optionPanel.add( jcbAppend );
			this.add(optionPanel,	"South");
		}

		public void actionPerformed(ActionEvent e) {

			if( e.getSource() instanceof JCheckBox ) {
				
				append_pc = this.jcbAppend.isSelected();
				operator.tableChanged( new TableEvent( operator, TableEvent.TableEventType.TABLE_CHANGED ) );
			}
			else {
				
				NumberFormat form = NumberFormat.getInstance();
				form.setMaximumIntegerDigits(4);
				form.setMaximumFractionDigits(3);
				form.setMinimumFractionDigits(2);
				
				ArrayList<Double> univarQuant = new ArrayList<Double>();
				ArrayList<String> univarNames = new ArrayList<String>();
				ArrayList<Integer> dtcols = ((PCAOp)this.operator).getDimTypeCols(ColType.NUMERIC);
				
				int p = 0;
				for( double v : ((PCAOp)this.operator).getEigenValues() ) {
					
					univarQuant.add( v );
					univarNames.add( ((PCAOp)this.operator).input.getColName( dtcols.get(p) ) );
					p++;
				}
	
				scree.setUnivar(univarQuant, univarNames);
			}
		}

		/**
		 * 
		 */
		private static final long serialVersionUID = -4476127570420657983L;

		@Override
		public void stateChanged(ChangeEvent e) {

			if( e.getSource() instanceof JSlider ) {
				JSlider source = (JSlider)e.getSource();
			    if (!source.getValueIsAdjusting()) {
			        int val = (int)source.getValue();
			        ((PCAOp)operator).outdims = val;
			        operator.tableChanged( new TableEvent( operator, TableEvent.TableEventType.TABLE_CHANGED ));		    
			    }
			}
			if( e.getSource() instanceof ScreePlot ) {

				((PCAOp)operator).outdims = ((ScreePlot)e.getSource()).cutoff+1;
				((PCAOp)operator).m_bRecalcEigen = false; // no need to recalculate eigen vectors as it is already.
		        operator.tableChanged( new TableEvent( operator, TableEvent.TableEventType.TABLE_CHANGED ));		    
				((PCAOp)operator).m_bRecalcEigen = true;
			}
		}
	}

	public class PCAFunction implements Function {

		public org.jblas.DoubleMatrix m_PrinCompMtx 	= null;
		private org.jblas.DoubleMatrix m_NewCoordsMtx	= null; 
		private int m_iNumericDim = 0;
		private Table m_InputTable = null; 
		ArrayList<Integer> m_NonNumericIdxs = null;
		double[] m_EigenValues;
		int m_iOutDim = -1; 
		
		public double[] getEigenValues() {
			
			return m_EigenValues;
		}
		
		public class EigValVecPair {
			
			public double value = 0.0;
			public double[] vec = null;
			
			public EigValVecPair( double val, double[] v ) {
				
				value = val;
				vec = v;
			}
			
		}
		
		public class EigComparator implements Comparator<EigValVecPair> {

			public int compare(EigValVecPair o1, EigValVecPair o2) {

				if( o1.value >=o2.value ) {
					return -1;
				}

				return 1;
			}
			
			
		}
		
		public org.jblas.DoubleMatrix eigSort( org.jblas.DoubleMatrix[] eig, int cutoff ) {
			
			double [][] vCopy 		= eig[0].transpose().toArray2();//.getArrayCopy();
			m_EigenValues 	= eig[1].diag().data;//eig.getRealEigenvalues();
			EigValVecPair[] p = new EigValVecPair[m_EigenValues.length];
			
			for( int i = 0; i < p.length; i++ ) {
				
				p[i] = new EigValVecPair( m_EigenValues[i], vCopy[i] );
			}
			Arrays.sort(p, new EigComparator());
			
			double[][] vCopyNew = new double[cutoff][];
			for( int i = 0; i < cutoff; i++ ) {
				
				vCopyNew[i] = p[i].vec;
			}
			
			org.jblas.DoubleMatrix retval = new org.jblas.DoubleMatrix( vCopyNew );
			return retval.transpose();
		}
		
		public PCAFunction( Table table, ArrayList<Integer> numericIdxs, ArrayList<Integer> nonNumericIdxs, int iDims, boolean bRecalcEigen) 
		{
			// Convert data to a zero means matrix
			org.jblas.DoubleMatrix zeroMeansMtx = org.jblas.DoubleMatrix.zeros(table.rows(), numericIdxs.size());
			double[] sums 	= new double[numericIdxs.size()];
			this.m_iNumericDim = numericIdxs.size();
			this.m_NonNumericIdxs = nonNumericIdxs;
			this.m_iOutDim = iDims;
			this.m_InputTable = table;
			
//			FastVector attr = new FastVector();
//			for( int j = 0; j < numericIdxs.size(); j++ )
//			{
//				attr.addElement(new Attribute(getColName(numericIdxs.get(j))));
//			}
//			
//			Instances data = new Instances("dim", attr, 0);
//			double[] row 	= new double[numericIdxs.size()];
//			for( int i = 0; i < table.rows(); i++ )
//			{
//				for( int j = 0; j < numericIdxs.size(); j++ )
//				{
//					row[j] 	= 	table.getMeasurement(i, numericIdxs.get(j));
//				}
//				data.add(new Instance(1.0, row));
//			}
//			SpectralClusterer spc = new SpectralClusterer();
//			spc.buildClusterer(data);
			
			for( int i = 0; i < table.rows(); i++ ) {
				for( int j = 0; j < numericIdxs.size(); j++ ) {
					
					sums[j] 	+= 	table.getMeasurement(i, numericIdxs.get(j));
				}
			}
			for( int j = 0; j < numericIdxs.size(); j++ ) {

				sums[j] 	= sums[j]	/((double) table.rows());
				for( int i = 0; i < table.rows(); i++ ) {
					zeroMeansMtx.put(i, j, table.getMeasurement(i, numericIdxs.get(j))-sums[j]);
				}
			}

/*			//HY: Stuff to debug the library loading:
			//String sArch = ArchFlavor.archFlavor();
			//int iSSELevel = ArchFlavor.SSELevel();

			String arch = System.getProperty("os.arch");
			String osname = System.getProperty("os.name");
			java.util.Properties props = System.getProperties();
		    props.list(System.out);
		    try
		    {
		    	//System.load("/Users/hyounesy/SFU/Research/IngramDevelopment/Code_Base/heidi/lib/libjblas_arch_flavor.jnilib");
		    	System.load("/Users/hyounesy/SFU/Research/IngramDevelopment/Code_Base/heidi/lib/libjblas.jnilib");
		    }
		    catch (Error e)
		    {
		    	e.printStackTrace();
		    }
			org.jblas.util.Logger.getLogger().setLevel(org.jblas.util.Logger.DEBUG);
		    new org.jblas.util.ArchFlavor();
			
			ArchFlavor.overrideArchFlavor("SSE3");
			String sArch = ArchFlavor.archFlavor();
			int iSSELevel = ArchFlavor.SSELevel();
			
			//(new org.jblas.util.LibraryLoader()).loadLibrary("libjblas", true);
			(new org.jblas.util.LibraryLoader()).loadLibrary("jblas_arch_flavor", false);
*/

			// Get normalized covariance matrix
			double dNormalizationConstant = 1.0 /((double)zeroMeansMtx.rows - 1.0);

			Rengine re = MathLibs.getREngine();
			if (re != null)
			{
				//  pcdat <- prcomp(m)  #"center" "rotation" "scale" "sdev" "x"
				MathLibs.Rassign("zm", zeroMeansMtx.toArray2());
				re.eval("cm <- t(zm) %*% zm *"+dNormalizationConstant);
				re.eval("em <- eigen(cm,symmetric=TRUE)");
				m_EigenValues = re.eval("em$values").asDoubleArray();
				re.eval("pcm <- t(em$vectors[1:"+iDims+",])");
				double pcm[][] = re.eval("pcm").asDoubleMatrix();
				m_PrinCompMtx = new org.jblas.DoubleMatrix(pcm);
				
				//double zm[][] = re.eval("zm").asDoubleMatrix();
				
				re.eval("ncm <- zm %*% pcm");
				double ncm[][] = re.eval("ncm").asDoubleMatrix();
				m_NewCoordsMtx = new org.jblas.DoubleMatrix(ncm);
			}
			else
			{
				org.jblas.DoubleMatrix covarianceMtx = zeroMeansMtx.transpose();
				covarianceMtx = covarianceMtx.mmul(zeroMeansMtx); //HY: crash
				
				covarianceMtx = covarianceMtx.mmul(dNormalizationConstant);
				
				// Get Eigenvectors and Eigenvalues
				org.jblas.DoubleMatrix[] eigMtx = org.jblas.Eigen.symmetricEigenvectors(covarianceMtx);//.eig();
				m_PrinCompMtx 	= eigSort( eigMtx, iDims );
				m_NewCoordsMtx 	= zeroMeansMtx.mmul(m_PrinCompMtx);
			}
			
		}
		
		@Override
		public Table apply(Group group) {

			return null;
		}

		@Override
		public double compute(int row, int col) {
									
			if( append_pc ) {
				
				if( col >= m_InputTable.columns() ) {
					
					return m_NewCoordsMtx.get( row, col - m_InputTable.columns() );
				}
				
				return m_InputTable.getMeasurement(row, col);
			}
			
			if( col < m_iOutDim ) 
				return m_NewCoordsMtx.get(row, col);
//			if( col < numericDims ) 
//				return newCoords.get(row, col);
			
			if( m_InputTable == null ) {
				System.out.println("ALARM1");
			}
			if( m_NonNumericIdxs == null ) {
				System.out.println("ALARM2");
			}
			return m_InputTable.getMeasurement(row, m_NonNumericIdxs.get(col-m_iOutDim));
		}

		@Override
		public Group inverse(Table dims) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public double[] invert( Map map, int row, int col, double value ) {
			
			// TODO HELP ME, properly invert, puh lease
			
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
	/**
	 * 
	 * Test routine loads a CSV table, displays the dimensions
	 * Applies the cutoff then the collect stage then the PCA stage 
	 * and displays the new dimensions
	 * 
	 * @param args
	 */
	public static void main( String[] args ) {
		
		System.out.println("Loading file " + args[0] );
		
		Table testTable = TableFactory.fromCSV( args[0] );
		
		System.out.println("Table is : " + testTable.rows() + " x " + testTable.columns() );
		
		CutoffOp testOp = new CutoffOp( testTable, true, 0 );
		
		System.out.println("Cutoff table is " + testOp.rows() + " x " + testOp.columns() );

		PearsonCollectOp test2Op = new PearsonCollectOp( testOp, true, 0.8 );
		
		System.out.println("Collect table is " + test2Op.rows() + " x " + test2Op.columns() );

		PCAOp test3Op = new PCAOp( test2Op, true, 1 );
		
		System.out.println("Reduce table is " + test3Op.rows() + " x " + test3Op.columns() );
	}
	
	/**
	 * Return the constructed dimensions for this operator (use the map!)
	 */
	public ArrayList<DimensionDescriptor> getConstructedDimensions() {

		NumberFormat nf = NumberFormat.getInstance();
		nf.setMinimumFractionDigits(2);
		nf.setMaximumFractionDigits(3);
		
		ArrayList<DimensionDescriptor> ret = new ArrayList<DimensionDescriptor>();
		
		if( append_pc ) {
		
			for( int i = 0; i < this.outdims; i++ ) {
				
				String desc = "( ";
				int rowd = ((PCAFunction)this.function).m_PrinCompMtx.rows;
				double[] row_vals = new double[rowd];
				ArrayList<String> sub_values = new ArrayList<String>();
				for( int j = 0; j < rowd; j++ ) {
					
					row_vals[j] = ((PCAFunction)this.function).m_PrinCompMtx.get(j, i);
				}
				FloatIndexer[] eval_indexer = FloatIndexer.sortEVals(row_vals);
				ArrayList<Integer> numIdxs = getNumericIndices( input );
				
				for( FloatIndexer fi: eval_indexer ) {
									
					if( Math.abs( fi.val ) > 1e-1 ) {
						
						sub_values.add(this.input.getColName( numIdxs.get(fi.idx) ) + " : " + nf.format( fi.val ));
					}
				}
				desc += " )";
				ret.add( new DimensionDescriptor( getColName(input.columns()+i), "", sub_values ) );
			}
		}
		else {
			
			for( int i = 0; i < this.outdims; i++ ) {
				
				String desc = "( ";
				int rowd = ((PCAFunction)this.function).m_PrinCompMtx.rows;
				double[] row_vals = new double[rowd];
				ArrayList<String> sub_values = new ArrayList<String>();
				for( int j = 0; j < rowd; j++ ) {
					
					row_vals[j] = ((PCAFunction)this.function).m_PrinCompMtx.get(j, i);
				}
				FloatIndexer[] eval_indexer = FloatIndexer.sortEVals(row_vals);
				ArrayList<Integer> numIdxs = getNumericIndices( input );
				
				for( FloatIndexer fi: eval_indexer ) {
									
					if( Math.abs( fi.val ) > 1e-1 ) {
						
						sub_values.add(this.input.getColName( numIdxs.get(fi.idx) ) + " : " + nf.format( fi.val ));
					}
				}
				desc += " )";
				ret.add( new DimensionDescriptor( getColName(i), "", sub_values ) );
			}
		}
		
		if( ret.size() > 0 ) {
			
			return ret;
		}
		
		return null;
	}

	public static String getMenuName() {
		
		return "Reduce:PCA";
	}
}
