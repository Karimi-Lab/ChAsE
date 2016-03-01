package heidi.dimreduction;

import heidi.project.Dim;
import heidi.project.Project;

import java.util.Vector;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import prefuse.Constants;
import prefuse.data.Table;
import prefuse.data.column.ColumnMetadata;
import weka.attributeSelection.ASSearch;
import weka.attributeSelection.AttributeSelection;
import weka.attributeSelection.PrincipalComponents;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;

public class WekaPCAnalyzer {
	
	private Project             m_project;
	
	private Instances           m_instances;
	private AttributeSelection  m_attrsel;
	private PrincipalComponents m_pca;
	
	private static final Logger s_logger = Logger.getLogger(WekaPCAnalyzer.class.getName());
	
	public WekaPCAnalyzer(Project project) {
		
		m_project = project;
		
		initInstance();
		initAttributeSelection();
	}

	private void initInstance() {
		
		Table table = m_project.getTable();
		
		// create columns
		FastVector attributes = new FastVector();
		int columnCount = table.getColumnCount();
		for (int i = 0; i < columnCount; i++) {
			String name = table.getColumnName(i);
			
			// TODO How should Categorical Dimensions be handled?
			// For now, treat them the same as Ordinal Dimensions
			attributes.addElement(new Attribute(name));
			
//			Dim dim = m_project.getDim(name);
//			int type = dim.getType(); 
//			if (type == Constants.NUMERICAL ||
//				type == Constants.ORDINAL) {
//				attributes.addElement(new Attribute(name));
//			} else {
//				ColumnMetadata md = table.getMetadata(name);
//				Object[] ordinalValues = md.getOrdinalArray();
//				FastVector ordinalVector = new FastVector();
//				for (int j = 0; j < ordinalValues.length; j++) {
//					// Convert all nominal values to strings because 
//					// Weka can not handle objects like Integer 
//					ordinalVector.addElement(ordinalValues[j].toString());
//				}
//				attributes.addElement(new Attribute(name, ordinalVector));
//			}
		}

		m_instances = new Instances("PrefuseData", attributes, 0);
		
		// add data for rows
		int rowCount = table.getRowCount();
		for (int i = 0; i < rowCount; i++) {
			double[] values = new double[columnCount];
			for (int j = 0; j < columnCount; j++) {
				String name = table.getColumnName(j);
				Dim dim = m_project.getDim(name);
				int type = dim.getType(); 
				if (type == Constants.NUMERICAL) {
					values[j] = table.getDouble(i, j);
				} else {
					ColumnMetadata md = table.getMetadata(name);
					Object[] ordinalValues = md.getOrdinalArray();
					Object dataValue = table.get(i, j);
					for (int k = 0; k < ordinalValues.length; k++) {
						Object ordinalValue = ordinalValues[k];
						if (ordinalValue.equals(dataValue)) {
							values[j] = k;
							break;
						}
					}
				}
			}
			m_instances.add(new Instance(1.0, values));
		}
	}
	
	private void initAttributeSelection() {
		
		m_pca = new PrincipalComponents();
		// Create ALL eigenvectors, don't stop at 95% cumulative variance
		// because we need ALL eigenvectors to perform column subtraction
		// described by Merida
		m_pca. setVarianceCovered(1.0);
		// Include all column names in attribute name
		// because I am relying on the attribute name to get the 
		// column coefficients
		m_pca.setMaximumAttributeNames(-1);
		
		m_attrsel = new AttributeSelection();
		m_attrsel.setEvaluator(m_pca);

		int folds = 10;
		m_attrsel.setFolds(folds);
		
		m_attrsel.setXval(false);
		
		int seed = 1;
		m_attrsel.setSeed(seed);
		
		String searchClassName = new String("weka.attributeSelection.Ranker");
		ASSearch searchMethod = null;
		try {
			searchMethod = (ASSearch)Class.forName(searchClassName).newInstance();
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		m_attrsel.setSearch(searchMethod);
		
	    try {
	    	m_attrsel.SelectAttributes(m_instances);
	    } catch (Exception ex) {
	    	ex.printStackTrace();
	    }

	    System.out.println(m_attrsel.toResultsString());
	}
	
	public Dim[] getSortedDimensions() {
		
		Instances transformed;
		double[][] ranks;
	    try {
	    	transformed = m_pca.transformedData(m_instances);
	    	ranks = m_attrsel.rankedAttributes();
	    } catch (Exception e1) {
	    	// TODO Auto-generated catch block
	    	e1.printStackTrace();
	    	return null;
	    }
	    
    	Vector<String> sortedColumns = new Vector<String>();
    	sortedColumns.setSize(ranks.length);
    	
    	for (int i = ranks.length-1; i >= 0; i--) {
    		
    		double[] rankInfo = ranks[i];
    		int index = (int)rankInfo[0];
    		//double merit = rankInfo[1]; // TODO show merit in a scree plot
    		
    		Attribute attr = transformed.attribute(index);
    		String[] sortedComponents = getSortedComponents(attr);
    		
    		for(int j = 0; j <sortedComponents.length; j++) {
    			String component = sortedComponents[j];
    			if (!sortedColumns.contains(component)){
    				sortedColumns.setElementAt(component, i);
    				break;
    			}
    		}
    	}
    	
    	Dim[] sortedDims = new Dim[sortedColumns.size()];
    	int index = 0;
    	for (int i = 0; i < sortedColumns.size(); i++) {
    		String column = sortedColumns.get(i);
    		if (column != null) {
    			Dim dim = m_project.getDim(column);
    			if (dim != null) {
    				sortedDims[index++] = dim;
    			} else {
    				s_logger.warning("Unable to find dimension for column "+column);
    			}
    		}
		}
    	return sortedDims;
	}
	
	private String[] getSortedComponents(Attribute attribute) {
		
		// TODO - need to figure out how to get coefficients form PCA
		// Currently parsing information from the name of the header of the column
		String attrString = attribute.toString();
		
		String prefix = "@attribute";
		if (attrString.startsWith(prefix)) {
			attrString = attrString.substring(prefix.length());
			attrString = attrString.trim();
		}
		String suffix = "numeric";
		if (attrString.endsWith(suffix)) {
			attrString = attrString.substring(0, attrString.lastIndexOf(suffix));
			attrString = attrString.trim();
		}
		
		if (attrString.startsWith("'")) {
			attrString = attrString.substring(1);
			attrString = attrString.trim();
		}
		
		if (attrString.endsWith("'")) {
			attrString = attrString.substring(0, attrString.lastIndexOf("'"));
			attrString = attrString.trim();
		}
		
		//TODO Handle columns with decimal numbers in column name
		Pattern pattern = Pattern.compile("\\-?\\+?[0-9]+\\.[0-9]+");
		
		Vector<String> columns = new Vector<String>();
		String[] subStrings = pattern.split(attrString);
		for (String s : subStrings) {
			String column = s.trim();
			if (column.length() > 0) {
				// TODO How will Categorical columns be handled?
				//// Nominal columns have "=" in the name
				//if (column.contains("=")) {
				//	int eqIndex = column.indexOf("=");
				//	column = column.substring(0, eqIndex);
				//}
				columns.add(column);
			}
		}
	
		String[] result = new String[columns.size()];
		result = columns.toArray(result);
		return result;
	}
	
}
