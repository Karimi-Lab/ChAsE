package still.analysis;

import still.data.Function;
import still.data.Map;
//import still.data.Dims;
abstract class Dims
{
	public int columns() {return 0;}
	public int rows() {return 0;}
	public int getMeasurement(int i, int j){return 0;}
};

/**
 * 
 * 
 * 
 * @author sfingram
 *
 */
public class VarianceAnalysis {

	private Dims dims = null;
	
	public VarianceAnalysis( Dims dims ) {
	
		this.dims = dims; 
	}
	
	/**
	 * Compute the map and function for doing variance culling
	 * 
	 * @param threshold
	 * @return
	 */
	AnalysisPair cullAnalysis( double threshold ) {

		// compute variance
		
		double[] sums 	= new double[dims.columns()];
		double[] sqSums	= new double[dims.columns()];
		double[] var	= new double[dims.columns()];
		for( int i = 0; i < dims.rows(); i++ ) {
			for( int j = 0; j < dims.columns(); j++ ) {
				
				sums[j] 	+= 	dims.getMeasurement(i, j);
				sqSums[j]	+= 	dims.getMeasurement(i, j) * 
								dims.getMeasurement(i, j);				
			}
		}
		boolean[] cullDims = new boolean[dims.columns()];
		for( int j = 0; j < dims.columns(); j++ ) {

			sums[j] 	= sums[j]	/((double) dims.rows());
			sqSums[j] 	= sqSums[j]	/((double) dims.rows());
			var[j] = sqSums[j] - (sums[j]*sums[j]);
			if( var[j] >= threshold ) {
				
				cullDims[j] = true;
			}
		}
		
		// create variance cull map
		Map cullMap = Map.generateCullMap(cullDims);
		
		// create constant mapping function
		
		
		return new AnalysisPair( Function function, cullMap );
	}
	
	/**
	 * Compute map and functions for doing dimension grouping
	 * by pearson's correlation coefficient
	 * 
	 * @param threshold
	 * @return
	 */
	AnalysisPair collectAnalysis( double threshold ) {
		
		// calculate covariance matrix		
		
		// create covariance grouping map
		int[] groupDims = new int[dims.columns()];
		Map groupMap = Map.generateCovarianceMap(groupDims);
		
		// create linear mapping function
		
		return new AnalysisPair( Function function, groupMap );
	}
	
	/**
	 * Compute the map and functions for principal component
	 * analysis.
	 * 
	 * @param threshold
	 * @return
	 */
	AnalysisPair reduceAnalysis( int dimensions ) {
		
		// compute full bipartite mapping
		// create pca projection and (expansion) function
		
		Map fullBipartiteMap = Map.fullBipartite(dims.columns(), dimensions);
		
		return new AnalysisPair( Function function, fullBipartiteMap );
	}	
	
}
