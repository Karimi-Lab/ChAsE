package still.data;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import still.data.Table.ColType;
import still.expression.Expression;

/**
 * 
 * General factory for creating Dims from various sources.
 * 
 * @author sfingram
 *
 */
public class TableFactory {

	public static String lastFName = null;
	
	public static Table fromInputString( String inputString ) {
		
		return null;
	}
	
	/**
	 * Read the first line of the file and 
	 * 
	 * @param filename
	 * @param skiplines
	 * @return
	 */
	public static ColType[] readColumnsFromFile( 	String filename, 
													String delim, 
													int skiplines ) {
		
		try {
			BufferedReader input =  new BufferedReader(new FileReader(filename));
			String line = null;
			int line_count = 0;
			while (( line = input.readLine()) != null) {

				if( line_count >= skiplines ) {
					
			    	String[] tokens = line.split(delim);		        	
			    	ColType[] types = new ColType[ tokens.length ];
			    	for( int i = 0; i < tokens.length; i++ ) {
			    		
			    		if( tokens[i].equalsIgnoreCase("CATEGORICAL") ) {
			    			
			    			types[i] = ColType.CATEGORICAL;
			    		}
			    		else if( tokens[i].equalsIgnoreCase("NUMERIC") ) {
			    			
			    			types[i] = ColType.NUMERIC;
			    		}
			    		else if( tokens[i].equalsIgnoreCase("ORDINAL") ) {
			    			
			    			types[i] = ColType.ORDINAL;
			    		}
			    		else if( tokens[i].equalsIgnoreCase("METADATA") ) {
			    			
			    			types[i] = ColType.METADATA;
			    		}
			    	}
			    	
			    	return types;
				}
				line_count++;
			}
		} catch (Exception e) {

			e.printStackTrace();
		}
		
		return null;
	}
	
	public static boolean hasTypes( String filename, String delim ) {
		
		try {
			
			BufferedReader input =  new BufferedReader(new FileReader(filename));
			String line = null;
			int line_num = 0;
			
			// diagnose table
			
	        while (( line = input.readLine()) != null) {

	        	line_num++;
	        	if ( line_num == 2) {
	        		
	        		String[] fields = line.split(delim);
	        		for(String field : fields ) {
	        			
	        			if( !field.equalsIgnoreCase("Ordinal") && 
	        				!field.equalsIgnoreCase("Categorical") && 
	        				!field.equalsIgnoreCase("Metadata") && 
	        				!field.equalsIgnoreCase("Numeric") ) {
	        				
	        				return false;
	        			}
	        		}
	        		
	        		return true;
	        	}
	        }
		}
		catch( Exception e ) {
			
			e.printStackTrace();
		}
		
		return false;
	}
	
	/**
	 * Read a dims object from a delimited file.  
	 * 
	 * @param filename
	 * @param delim
	 * @param skiplines
	 * @param hasCategorical
	 * @return
	 */
	public static MemoryTable fromDelimSpecial( 	String filename, 
											String delim, 
											int skiplines, 
											ColType[] types ) {
		
		try {
			
			BufferedReader input =  new BufferedReader(new FileReader(filename));
			String line = null;
			
			// diagnose table
			
			int line_count 	= 0;
			int dim_count 	= 0;
	        while (( line = input.readLine()) != null) {

	        	if( line_count == 0) {
	        		
	        		dim_count = line.split(delim).length;
	        	}
	        	line_count++;
	        }
	        line_count -= skiplines;
	        double[][] newTable = new double[line_count][dim_count];
	        String[] colNames = new String[dim_count];
	        
	        // read the file in earnest
	        
	        if( types == null ) {
	        	input.close();
				input =  new BufferedReader(new FileReader(filename));
				line = null;
				line_count = 0;
		        while (( line = input.readLine()) != null) {

		        	if(line_count == 0) {
		        		
			        	String[] tokens = line.split(delim);		        	
			        	for( int i = 0; i < tokens.length; i++ ) {
			        		
			        		colNames[i] = tokens[i];
			        	}
		        	}
		        	if( line_count >= skiplines ) {
		        		
			        	String[] tokens = line.split(delim);		        	
			        	for( int i = 0; i < tokens.length; i++ ) {
			        		
			        		newTable[line_count-skiplines][i] = Double.parseDouble(tokens[i]);
			        	}
		        	}
		        	line_count++;
		        }

				return new MemoryTable(newTable, colNames);
	        }
	        else {

	        	// make hashes of the categorical dimensions

	        	int catCount = 0;
	        	int[] catMapIndex = new int[types.length];
	        	ArrayList<HashMap<String,Integer>> catMaps = new ArrayList<HashMap<String,Integer>>();  

	        	int metaCount = 0;
	        	int[] metaMapIndex = new int[types.length];
	        	ArrayList<HashMap<String,Integer>> metaMaps = new ArrayList<HashMap<String,Integer>>();  

	        	for( int i = 0; i < types.length; i++ ) {
	        		
	        		if( types[i]==ColType.CATEGORICAL ) {
	        			catMapIndex[i]=catCount;
	        			catCount++;
	        			catMaps.add( new HashMap<String,Integer>() );
	        		}
	        		else {
	        			catMapIndex[i] = -1;
	        		}
	        		
	        		if( types[i]==ColType.METADATA ) {
	        			metaMapIndex[i]=metaCount;
	        			metaCount++;
	        			metaMaps.add( new HashMap<String,Integer>() );
	        		}
	        		else {
	        			metaMapIndex[i] = -1;
	        		}
	        	}
	        	
	        	input.close();
				input =  new BufferedReader(new FileReader(filename));
				line = null;
				line_count = 0;
		        while (( line = input.readLine()) != null) {

		        	if( line_count == 0 ) {
		        		
			        	String[] tokens = line.split(delim);		        	
			        	for( int i = 0; i < tokens.length; i++ ) {
			        		
			        		colNames[i] = tokens[i];
			        	}
		        	}
		        	if( line_count >= skiplines ) {
		        		
			        	String[] tokens = line.split(delim);		        	
			        	for( int i = 0; i < tokens.length; i++ ) {
			        		
			        		if( types[i] == ColType.NUMERIC || types[i] == ColType.ATTRIBUTE || types[i] == ColType.ORDINAL ) {
			        			newTable[line_count-skiplines][i] = Double.parseDouble(tokens[i]);
			        		}
			        		else if  (types[i] == ColType.CATEGORICAL)
			        		{
			        			if( catMaps.get(catMapIndex[i]).containsKey(tokens[i]) ) {
			        				newTable[line_count-skiplines][i] = catMaps.get(catMapIndex[i]).get(tokens[i]).doubleValue();
			        			}
			        			else {
			        				catMaps.get(catMapIndex[i]).put(tokens[i], catMaps.get(catMapIndex[i]).keySet().size());
			        				newTable[line_count-skiplines][i] = catMaps.get(catMapIndex[i]).get(tokens[i]).doubleValue();
			        			}
			        		}
			        		else if  (types[i] == ColType.METADATA)
			        		{
			        			if( metaMaps.get(metaMapIndex[i]).containsKey(tokens[i]) ) {
			        				newTable[line_count-skiplines][i] = metaMaps.get(metaMapIndex[i]).get(tokens[i]).doubleValue();
			        			}
			        			else {
			        				metaMaps.get(metaMapIndex[i]).put(tokens[i], metaMaps.get(metaMapIndex[i]).keySet().size());
			        				newTable[line_count-skiplines][i] = metaMaps.get(metaMapIndex[i]).get(tokens[i]).doubleValue();
			        			}
			        		}
			        	}
		        	}
		        	line_count++;
		        }

		        String[][] categories = new String[catMapIndex.length][];
		        for( int i = 0; i < catMapIndex.length ; i++ ) {
		        	
		        	if( catMapIndex[i]>=0) {
		        		
		        		categories[i] = new String[catMaps.get(catMapIndex[i]).keySet().size()];
		        		Set<String> myset = catMaps.get(catMapIndex[i]).keySet();
		        		for( String s : myset) {
		        			
		        			categories[i][catMaps.get(catMapIndex[i]).get(s)] = s;
		        		}
		        	}
		        	else {
		        		categories[i] = null;
		        	}
		        }
		        
		        String[][] metadata = new String[metaMapIndex.length][];
		        for( int i = 0; i < metaMapIndex.length ; i++ ) {
		        	
		        	if( metaMapIndex[i]>=0) {
		        		
		        		metadata[i] = new String[metaMaps.get(metaMapIndex[i]).keySet().size()];
		        		Set<String> myset = metaMaps.get(metaMapIndex[i]).keySet();
		        		for( String s : myset) {
		        			
		        			metadata[i][metaMaps.get(metaMapIndex[i]).get(s)] = s;
		        		}
		        	}
		        	else {
		        		metadata[i] = null;
		        	}
		        }
		        
				return new MemoryTable(newTable, types, categories, metadata, colNames );
	        }
		} catch( Exception e ) {
			
			e.printStackTrace();
		}
		
		return null;
	}
	
	public static Table csvLoader( Container container ) {
		
		// bring up a file chooser
		JFileChooser fc = new JFileChooser( );
		int returnVal = fc.showOpenDialog(container);
		if( returnVal == JFileChooser.APPROVE_OPTION ) {
			
			// make a new table					
			try {
				String fname = fc.getSelectedFile().getCanonicalPath();
				return loadTableCSV(fname);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			
		}
		
		return null;
	}
	
	/**
	 * Return a Dims object from a csv file.  Assumes *no* categorical data.
	 * and one descriptor line.  
	 * 
	 * @param csvFilename
	 * @return
	 */
	public static Table fromCSV( String csvFilename ) {
	
		return TableFactory.fromCSV(csvFilename, 1);
	}

	/**
	 * Return a Dims object from a csv file.  Assumes *no* categorical data.
	 * and one descriptor line.  
	 * 
	 * @param csvFilename
	 * @return
	 */
	public static Table fromCSV( String csvFilename, int skiplines ) {
	
		return fromCSVTypes( csvFilename, skiplines, false );
	}

	/**
	 * Return a Dims object from a csv file.  Assumes *no* categorical data.
	 * and one descriptor line.  
	 * 
	 * @param csvFilename
	 * @return
	 */
	public static Table fromCSVTypes( String csvFilename, int skiplines, boolean withTypes ) {
	
		MemoryTable memTab = null;
		
		if( ! withTypes ) {
			
			memTab = TableFactory.fromDelimSpecial(csvFilename, "[\\t\\n\\x0B\\f\\r,]+", skiplines, null);
			memTab.setDescriptor(csvFilename);
		}
		else {
			
			ColType[] types = readColumnsFromFile( csvFilename, "[\\t\\n\\x0B\\f\\r,]+", skiplines );
			memTab = TableFactory.fromDelimSpecial(csvFilename, "[\\t\\n\\x0B\\f\\r,]+", skiplines+1, types);
			memTab.setDescriptor(csvFilename);
		}
		
		if( memTab != null ) {
			
			memTab.setInputControl( new CSVPanel(csvFilename, memTab) );
		}
		return memTab;
	}

	public static class CSVPanel extends JPanel implements ActionListener {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1295071012965868291L;
		
		public String fname 	= null;
		JLabel fileLabel 		= null;
		JButton openFileButton 	= null;
		MemoryTable internalTab = null;
		Expression exp = null;
		MemoryTable memTab = null;
		boolean block = false;
		
		public CSVPanel( String csvFilename, MemoryTable intTab ) {
			super();
			
			fname 			= csvFilename;
			fileLabel 		= new JLabel(csvFilename);
			openFileButton 	= new JButton( "Open" );
			openFileButton.addActionListener(this);
			internalTab 	= intTab;
			this.setBorder(	BorderFactory.createEmptyBorder(50, 10, 50, 10));
			this.setLayout(new BorderLayout(5,5));
			this.add(fileLabel,"Center");
			this.add(openFileButton,"West");			
		}

		@Override
		public void actionPerformed(ActionEvent e) {

			if( e.getSource() == internalTab ) {
				
				// update the gui
				fileLabel.setText(fname);
			}
			if( e.getSource() == memTab ) {
				
				// update the gui
				fileLabel.setText(fname);
			}
			if( e.getSource() == openFileButton ) {

				// bring up a file chooser
				JFileChooser fc = new JFileChooser();
				int returnVal = fc.showOpenDialog(this);
				if( returnVal == JFileChooser.APPROVE_OPTION ) {
					
					// make a new table					
					try {
						fname = fc.getSelectedFile().getCanonicalPath();
					} catch (IOException e1) {
						e1.printStackTrace();
					}
					
//					MemoryTable memTab = TableFactory.fromDelimSpecial(	fname, 
//							"[\\t\\n\\x0B\\f\\r,]+", 
//							1, 
//							null);
			    	if( !TableFactory.hasTypes(fname, "[\\t\\n\\x0B\\f\\r,]+")) {
			    		
			    		memTab = (MemoryTable) TableFactory.fromCSV( fname, 1 );
			    	}
			    	else {
			    		
			    		memTab = (MemoryTable) TableFactory.fromCSVTypes( fname, 1, true );
			    	}
			    	block = true;
					
//					memTab.addActionListeners(internalTab.actionListeners);
//					memTab.signalActionListeners();
//					
//					// transfer table listeners
//					for( TableListener tl : internalTab.getTableListeners() ) {
//					
//						memTab.addTableListener(tl);
//					}
			    	
					// inform table listeners
//			    	Expression exp = null;
					for( TableListener tl : internalTab.getTableListeners() ) {
												
						if(tl instanceof Expression) {
							
							exp = (Expression)tl;							
						}
					}
					
					SwingUtilities.invokeLater(new Runnable() {
				        public void run() {
				        	
				        	try {
				        		
				        		while( block ) {
				        			Thread.sleep(100);
				        		}
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							
				        	exp.setTable(memTab);
				        	internalTab = memTab;
				        }
				      });
//					exp.setTable(internalTab);
//					
//					internalTab = memTab;
//					for( TableListener tl : internalTab.getTableListeners() ) {
//						
//						if(tl instanceof Operator) {
//							
//							((Operator)tl).tableChange(internalTab);
//						}
//						if(tl instanceof Expression) {
//							
//							((Expression)tl).table = internalTab;
//							
//						}
//					}
					
					// inform the expression
					block = false;
				}								
			}			
		}
	}
	
	public static Table loadTableCSV(String fname)
	{
		Table table = null;
		if( !TableFactory.hasTypes(fname, "[\\t\\n\\x0B\\f\\r,]+") ){
			
    		table = TableFactory.fromCSV( fname, 1 );
    	}
    	else {
    		
    		table = TableFactory.fromCSVTypes( fname, 1, true );
			
		}
		
		TableFactory.lastFName = fname;

		return table;
	}
	
	public static void saveTableCSV(Table memTab, File outputFile )
	{
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile));
			
			for (int d = 0; d < memTab.columns(); d++)
			{
				if (d > 0)
				{
					bw.write(",");
				}
				bw.write(memTab.getColName(d));
			}
			bw.write("\n");
			
			for (int i = 0; i < memTab.rows(); i++)
			{
				for (int d = 0; d < memTab.columns(); d++)
				{
					if (d > 0)
					{
						bw.write(",");
					}
					bw.write(Double.toString(memTab.getMeasurement(i, d)));
				}
				bw.write("\n");
			}
			bw.close();
		} catch (IOException e) {

			e.printStackTrace();
		}
	}
	
	public static double[][] mergeTables(ArrayList<double[][]> tables)
	{
		int iRows = tables.get(0).length;
		int iCols = 0;
		for (int i = 0; i < tables.size(); i++)
		{
			if (tables.get(i).length != iRows)
				return null;
			iCols += tables.get(i)[0].length;
		}
		
		double[][] mergedTable = new double[iRows][iCols];
		
		int iMergedColIndex = 0; 
		for (int i = 0; i < tables.size(); i++)
		{
			int iNumCurrCols = tables.get(i)[0].length;
			for (int iR = 0; iR < iRows; iR++)
			{
				System.arraycopy(tables.get(i)[iR], 0, mergedTable[iR], iMergedColIndex, iNumCurrCols);
			}
			iMergedColIndex += iNumCurrCols;
		}

		return mergedTable;
	}
	
	
	public static void main(String[] args) {
		
		// load a csv file from disk
		System.out.println( "Loading file " + args[0] );
		Table csvDims = TableFactory.fromCSV(args[0]);
		System.out.println( "Rows = " + csvDims.rows() + " Columns = " + csvDims.columns() );
	}
}
