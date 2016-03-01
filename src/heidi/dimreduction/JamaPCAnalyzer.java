package heidi.dimreduction;

import heidi.project.Dim;
import heidi.project.Project;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Vector;

import prefuse.Constants;
import prefuse.data.Table;
import prefuse.data.column.ColumnMetadata;
import Jama.EigenvalueDecomposition;
import Jama.Matrix;

public class JamaPCAnalyzer {

	private boolean              m_useCorrelation = true;
	private Project              m_project;
	private PrincipalComponent[] m_principalComponents;
	private Dim[]                m_originalComponents;
	
	public JamaPCAnalyzer(Project project) {
		m_project = project;
		initPrincipalComponents();
	}
	
	public Dim[] getOriginalDimensions() {
		/*
		 * "An alternative way to reduce the dimension of a dataset using PCA is suggested in [41]. Instead of using
		 * the PCs as the new variables, this method uses the information in the PCs to find important variables in the
		 * original dataset. As before, one first calculates the PCs, then studies the scree plot to determine the number
		 * k of important variables to keep. Next, one considers the eigenvector corresponding to the smallest eigenvalue
		 * (the least important PC), and discards the variable that has the largest (absolute value) coefficient in that
		 * vector. Then, one considers the eigenvector corresponding to the second smallest eigenvalue, and discards
		 * the variable contributing the largest (absolute value) coefficient to that eigenvector, among the variables not
		 * discarded earlier. The process is repeated until only k variables remain."
		 *  
		 * [41] K.V. Mardia, J.T. Kent, and J.M. Bibby. Multivariate Analysis. Probability and Mathematical Statistics. Academic Press, 1995.
		 */
		
		if (m_originalComponents == null) {
			int eigenValueCount = m_principalComponents.length;
			Vector<Dim> sortedDims = new Vector<Dim>();
	
			// add unique dims to list starting with least significant eigenvector
			for (int i = eigenValueCount-1; i >= 0; i--) {
				Dim[] dims = m_principalComponents[i].getOriginalDimensions();
				for (int j = 0; j < dims.length; j++) {
					if (!sortedDims.contains(dims[j])) {
						sortedDims.add(dims[j]);
						break;
					}
				}
			}
			m_originalComponents = new Dim[sortedDims.size()];
			m_originalComponents = sortedDims.toArray(m_originalComponents);
			// invert order of result
			for (int i = 0; i < Math.floor(m_originalComponents.length/2); i++) {
				Dim temp = m_originalComponents[i];
				m_originalComponents[i] = m_originalComponents[m_originalComponents.length-1 - i];
				m_originalComponents[m_originalComponents.length-1 - i] = temp;
			}
		}
		return m_originalComponents.clone();
	}
	
	public PrincipalComponent[] getPrincipalComponents() {
		return m_principalComponents.clone();
	}
	
	public PrincipalComponent getPrincipalComponent(int index) {
		return m_principalComponents[index];
	}
	
	public int getPrincipalComponentCount() {
		return m_principalComponents.length;
	}
	
	public double[] getProportions() {
		int eigenValueCount = m_principalComponents.length;
		double[] proportions = new double[eigenValueCount];
		double eigenValueTotal = 0.0;
		for (int i = 0; i < eigenValueCount; i++) {
			eigenValueTotal += m_principalComponents[i].getEigenValue();
		}
		for (int i = 0; i < eigenValueCount; i++) {
			proportions[i] = m_principalComponents[i].getEigenValue()/eigenValueTotal;
		}
		return proportions;
	}
	
	public String toString() {
		String result = "";
		if (m_principalComponents == null) {
			return "Principal Component Analysis has not been performed";
		}
		double[] proportions = getProportions();
		for (int i = 0; i < m_principalComponents.length; i++) {
			result += proportions[i]+"\t"+m_principalComponents[i]+"\n";
		}
		return result;
	}
	
	private void initPrincipalComponents() {
		m_principalComponents = null;
		m_originalComponents = null;
		
		// Create a matrix from numerical data
		Vector<Dim> numericalDims = new Vector<Dim>();
		Dim[] dims = m_project.getDims();
		for (Dim dim : dims) {
			if (dim.getType() == Constants.NUMERICAL) {
				// skip over existing PCs
				if (!(dim instanceof PrincipalComponent)) {
					numericalDims.add(dim);
				}
			}
		}
		
		// Convert data to a zero means matrix
		Table table = m_project.getTable();
		int rowCount = table.getRowCount();
		Matrix zeroMeans = new Matrix(rowCount, numericalDims.size(), 0.0);
		int index = 0;
		for (Dim dim : numericalDims) {
			String column = dim.getColumn();
			ColumnMetadata metaColumn = table.getMetadata(column);
			double mean = metaColumn.getMean();
			for (int r = 0; r < rowCount; r++) {
				double data = table.getDouble(r, column);
				zeroMeans.set(r, index, data - mean);
			}
			index++;
		}
		
		// Get normalized covariance matrix
		Matrix covariance = zeroMeans.transpose().times(zeroMeans);
		double normalizationConstant = 1.0 /((double)zeroMeans.getRowDimension() - 1.0);
		covariance = covariance.times(normalizationConstant);
		
		// Get correlation matrix if required
		Matrix correlation = null;
		if (m_useCorrelation) {
			//
			// Convert covariance matrix to a correlation matrix using the 
			// relationship R(r,c) =  C(r,c)/sqrt(C(r,r)*C(c,c))
			//
			int m = covariance.getRowDimension();
			correlation = new Matrix(m, m, 0.0);
			for (int r = 0; r < m; r++) {
				double Crr = covariance.get(r, r);
				for (int c = 0; c < m; c++) {
					double Ccc = covariance.get(c, c);
					if (Ccc == 0.0 || Crr == 0.0) {
						// Can occur if all the data for a column is the same, 
						// that is, the standard deviation is 0 for the column.
						// In this case, there is no correlation between the
						// dependent and independent variables.
						correlation.set(r, c, 0.0);
					} else {
						double denominator = Math.sqrt(Crr * Ccc);
						correlation.set(r, c, covariance.get(r, c)/denominator);
					}
				}
			}
		}
		
		// Get Eigenvectors and Eigenvalues
		EigenvalueDecomposition eig = m_useCorrelation ? correlation.eig() : covariance.eig();
		double[] eigenValues = eig.getRealEigenvalues();
		int eigenCount = eigenValues.length;
		double[][] eigenVectorArray = eig.getV().getArray();
		// Get transformed data in new basis of eigenvectors
		Matrix dataMatrix = eig.getV().transpose().times(zeroMeans.transpose());
		double[][] transformedData = dataMatrix.transpose().getArray();
		
		// Create Principal Components
		m_principalComponents = new PrincipalComponent[eigenCount];
		for (int c = 0; c < eigenCount; c++) {
			double[] eigenVector = new double[eigenCount];
			for (int r = 0; r < eigenCount; r++) {
				eigenVector[r] = eigenVectorArray[r][c];
			}
			double[] data = new double[transformedData.length];
			for (int r = 0; r < transformedData.length; r++) {
				data[r] = transformedData[r][c];
			}
			PrincipalComponent pc = new PrincipalComponent(m_project, eigenValues[c], eigenVector);
			pc.setTransformedData(data);
			m_principalComponents[c] = pc;
		}
		// sort
		Arrays.sort(m_principalComponents, new Comparator<PrincipalComponent>() {
			@Override
			public int compare(PrincipalComponent o1, PrincipalComponent o2) {
				// Perform compare in the reverse order to sort
				// in descending order
				return o2.compareTo(o1);
			}
		});
		// Set rank (sorted order) of Principal Component.
		// Using rank, look for Principal Components that already exist in project
		PrincipalComponent[] exitingPcs = m_project.getPrincipalComponents();
		for (int c = 0; c < eigenCount; c++) {
			m_principalComponents[c].setRank(c);
			m_principalComponents[c].setName("Principal Component "+c);
			if (exitingPcs != null) {
				for (PrincipalComponent pc : exitingPcs) {
					if (pc.getRank() == c) {
						m_principalComponents[c] = pc;
						break;
					}
				}
			}
		}
	}
}
