package still.operators;

import still.data.Function;
import still.data.Group;
import still.data.Map;
import still.data.Operator;
import still.data.Table;
import still.data.TableEvent;
import still.gui.OperatorView;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.io.Serializable;
import java.util.ArrayList;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

public class CullByNameOp extends Operator implements Serializable {

	private static final long serialVersionUID = 4093740321755509633L;
	public ArrayList<String> selectedDims = null;
	public ArrayList<String> culledDims = null;
	
	public String toString() {
		
		return "[Cull:Name]";
	}

	public static String getMenuName() {
		
		return "Cull:Name";
	}

	public String getSaveString( ) {
		
		String saveString = "";

		int k = 0;
		for( String cullDim : culledDims ) {
			
			k++;
			saveString += cullDim;
			if( k < culledDims.size() ) {
				saveString += ",";
			}
		}
		
		return saveString;
	}

	public CullByNameOp( Table newInput, boolean isActive, String paramString ) {
		
		super(newInput);
		
		selectedDims = new ArrayList<String>();
		culledDims = new ArrayList<String>();
		
		// extract parameters
		
		String[] params = paramString.split(",");
		for( String param : params ) {
			
			culledDims.add( param );
		}
		for( int i = 0; i < this.input.columns(); i++ ) {
			
			if( !culledDims.contains( this.input.getColName(i) ) ) {
				
				selectedDims.add(this.input.getColName(i) );
			}
		}
		
		this.isLazy = true;
		this.isActive = isActive;
		
		if( isActive ) {
			activate();
		}
	}
	
	public CullByNameOp(Table newInput, boolean isActive) {

		super(newInput);
		
		this.isLazy = true;
		this.isActive = isActive;
		
		selectedDims = new ArrayList<String>();
		culledDims = new ArrayList<String>();
		
		for( int i = 0; i < this.input.columns(); i++ ) {
			selectedDims.add( this.input.getColName(i) );
		}
		
		
		if( isActive ) {
			activate();
		}
	}

	/**
	 * reset the selected dimensions
	 * 
	 */
	public void updateAvailableNames() {
		
		ArrayList<String> availableDims 	= new ArrayList<String>();
		ArrayList<String> delDims 			= new ArrayList<String>();
		
		// add newly added dimensions (to the table) to the selected dims
		for( int i = 0; i < this.input.columns(); i++ ) {
			availableDims.add( this.input.getColName(i) );
			if( !selectedDims.contains( this.input.getColName(i) ) && 
				!culledDims.contains( this.input.getColName(i) ) )	{
				
				selectedDims.add(  this.input.getColName(i) );
			}
		}
		
		
		// remove nonexistent dimensions from the selected list
		
		for( String selectedDim : selectedDims ) {
		
			if( ! availableDims.contains( selectedDim ) ) {
				
				delDims.add(selectedDim);
			}
		}
		
		for( String delDim : delDims ) {
			
			availableDims.remove(delDim);
		}
		
		// remove nonexistent dimensions from the culled list

		delDims 			= new ArrayList<String>();

		for( String culledDim: culledDims ) {
			
			if( ! availableDims.contains( culledDim ) ) {
				
				delDims.add(culledDim);
			}
		}

		for( String delDim : delDims ) {
			
			culledDims.remove(delDim);
		}
	}
	
	/**
	 * Given the list of selected names, build a boolean map
	 * 
	 * @return
	 */
	public boolean[] genMappingFromNames( ) {
		
		boolean[] internal_map = new boolean[this.input.columns()];
		
		for( int i = 0; i < this.input.columns(); i++ ) {
			
			internal_map[i] = selectedDims.contains( this.input.getColName(i) );
		}
		
		return internal_map;
	}
	
	@Override
	public void activate() {

		this.isActive = true;
		map 			= Map.generateCullMap( genMappingFromNames( ) );
		function 		= new CutoffFunction( this.input, map );
		isLazy  		= true;
		setView( new NameSelectView( this ) );		
	}

	@Override
	public void updateFunction() {

		function 		= new CutoffFunction( input, map );
	}
	
	public class CutoffFunction implements Function {

		private Table table 	= null;
		private int[] dimMap 	= null;
		
		public CutoffFunction( Table table, Map cutoffMap ) {
			this.table 	= table;
			dimMap 		= new int[cutoffMap.columns()];
			for(int i = 0; i < dimMap.length; i++ ) {
				
				for( int j = 0; j < cutoffMap.rows(); j++ ) {
					
					if( cutoffMap.map[j][i] ) {
						
						dimMap[i] = j;
						break;
					}
				}
			}
		}
		
		@Override
		public Table apply(Group group) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public double compute(int row, int col) {
			
			return table.getMeasurement(row, dimMap[col]);
		}

		@Override
		public Group inverse(Table dims) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public double[] invert( Map map, int row, int col, double value ) {
			
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
	public void updateMap() {

		updateAvailableNames();
		map 			= Map.generateCullMap( genMappingFromNames( ) );
	}

	public class NameSelectView extends OperatorView {

		JList selectedDimList = null;
		JList culledDimList = null;
		
		public NameSelectView(CullByNameOp o) {
			
			super(o);			
			
			DefaultListModel listModelSelected = new DefaultListModel();
			DefaultListModel listModelCulled = new DefaultListModel();
			boolean[] mapping = o.genMappingFromNames( );
			for( int i = 0; i < mapping.length; i++ ) {
				
				if( mapping[i] ) {
					
					listModelSelected.addElement( o.input.getColName(i) );
				}
				else {
					
					listModelCulled.addElement( o.input.getColName(i) );
				}
			}
			
			this.setLayout( new BorderLayout(5,5) );
			selectedDimList = new JList(listModelSelected);
			culledDimList = new JList(listModelCulled);
			
			JPanel selectedDimPanel = new JPanel();
			JPanel culledDimPanel = new JPanel();
			selectedDimPanel.setLayout(new BorderLayout(5,5));
			culledDimPanel.setLayout(new BorderLayout(5,5));
			selectedDimPanel.add( new JScrollPane(selectedDimList), "Center");
			culledDimPanel.add( new JScrollPane(culledDimList), "Center");
			selectedDimPanel.add( new JLabel("Selected Dimensions"), "North");
			culledDimPanel.add( new JLabel("Culled Dimensions"), "North");
			this.add( culledDimPanel, "East");
			this.add( selectedDimPanel, "West");
			JPanel centerPanel = new JPanel();
			centerPanel.setLayout(new GridLayout(5,1));
			centerPanel.add(new JPanel());

			JButton toButton = new JButton(">>");
			toButton.setToolTipText("Cull dimensions selected on the left");
			JButton fromButton = new JButton("<<");
			fromButton.setToolTipText("UnCull dimensions selected on the right");
			JButton swapButton = new JButton("<< swap >>");
			swapButton.setToolTipText("Swap selected dimensions on the left and right");
			toButton.addActionListener(this);
			toButton.setActionCommand(">>");
			fromButton.setActionCommand("<<");
			fromButton.addActionListener(this);
			swapButton.addActionListener(this);
			swapButton.setActionCommand("<< >>");
			centerPanel.add(toButton);			
			centerPanel.add(fromButton);
			centerPanel.add(swapButton);			
			centerPanel.add(new JPanel());
			this.add( centerPanel, "Center" );
		}

		/**
		 * 
		 */
		private static final long serialVersionUID = -4476127570420657983L;
		
		public void actionPerformed(ActionEvent e) {

			if( e.getActionCommand().equals("<<") && culledDimList.getModel().getSize() > 0) { // from culled button
					
				// update the gui model
				
				int[] selectedIndices = culledDimList.getSelectedIndices();
				if (selectedIndices.length > 0)
				{
					for (int i = selectedIndices.length - 1; i >= 0; i--)
					{
						DefaultListModel dlmCull = (DefaultListModel)culledDimList.getModel();
						String item = (String) dlmCull.getElementAt(selectedIndices[i]);
						dlmCull.removeElementAt(selectedIndices[i]);
						DefaultListModel dlmSel = (DefaultListModel)selectedDimList.getModel();
						dlmSel.addElement(item);
						
						// update the operator
						((CullByNameOp)operator).selectedDims.add(item);
						((CullByNameOp)operator).culledDims.remove(item);
					}
			        operator.tableChanged( new TableEvent( operator, TableEvent.TableEventType.TABLE_CHANGED ));
				}
			}
			
			else if( e.getActionCommand().equals(">>")  && selectedDimList.getModel().getSize() > 0) { // to culled button
				int[] selectedIndices = selectedDimList.getSelectedIndices();
				if (selectedIndices.length > 0)
				{
					for (int i = selectedIndices.length - 1; i >= 0; i--)
					{
						
						DefaultListModel dlmSel = (DefaultListModel)selectedDimList.getModel();
						String item = (String) dlmSel.getElementAt(selectedIndices[i]);
						dlmSel.removeElementAt(selectedIndices[i]);
						DefaultListModel dlmCull = (DefaultListModel)culledDimList.getModel();
						dlmCull.addElement(item);
						
						// update the operator
						((CullByNameOp)operator).selectedDims.remove(item);
						((CullByNameOp)operator).culledDims.add(item);
					}
			        operator.tableChanged( new TableEvent( operator, TableEvent.TableEventType.TABLE_CHANGED ));
				}
			}
			else if( e.getActionCommand().equals("<< >>")  && selectedDimList.getModel().getSize() > 0) { // to culled button
				int[] selectedIndices = selectedDimList.getSelectedIndices();
				int[] culledIndices = culledDimList.getSelectedIndices();
				String [] selectedItems = null; 
				String [] culledItems = null; 
				DefaultListModel dlmSel = (DefaultListModel)selectedDimList.getModel();
				DefaultListModel dlmCull = (DefaultListModel)culledDimList.getModel();
				boolean bTableChanged = false;
				if (selectedIndices.length > 0)
				{
					selectedItems = new String[selectedIndices.length];
					for (int i = selectedIndices.length - 1; i >= 0; i--)
					{
						String item = (String) dlmSel.getElementAt(selectedIndices[i]);
						dlmSel.removeElementAt(selectedIndices[i]);
						selectedItems[i] = item;
					}
					bTableChanged = true;
				}
				if (culledIndices.length > 0)
				{
					culledItems = new String[culledIndices.length];
					for (int i = culledIndices.length - 1; i >= 0; i--)
					{
						String item = (String) dlmCull.getElementAt(culledIndices[i]);
						dlmCull.removeElementAt(culledIndices[i]);
						culledItems[i] = item;
					}
					bTableChanged = true;
				}
				
				if (selectedItems != null)
				{
					for (int i = 0; i < selectedItems.length; i++)
					{
						dlmCull.addElement(selectedItems[i]);
						// update the operator
						((CullByNameOp)operator).selectedDims.remove(selectedItems[i]);
						((CullByNameOp)operator).culledDims.add(selectedItems[i]);
					}
				}					
				if (culledItems != null)
				{
					for (int i = 0; i < culledItems.length; i++)
					{
						dlmSel.addElement(culledItems[i]);
						((CullByNameOp)operator).culledDims.remove(culledItems[i]);
						((CullByNameOp)operator).selectedDims.add(culledItems[i]);
					}
				}
				
				if (bTableChanged)
				{
			        operator.tableChanged( new TableEvent( operator, TableEvent.TableEventType.TABLE_CHANGED ));
				}
			}
			else {
				
				// update the lists based on what has happened to the operator
				// remove any dimensions that don't exist anymore from any of the lists
				
				DefaultListModel listModelSelected = new DefaultListModel();
				DefaultListModel listModelCulled = new DefaultListModel();
				boolean[] mapping = ((CullByNameOp)operator).genMappingFromNames( );
				for( int i = 0; i < mapping.length; i++ ) {
					
					if( mapping[i] ) {
						
						listModelSelected.addElement( ((CullByNameOp)operator).input.getColName(i) );
					}
					else {
						
						listModelCulled.addElement( ((CullByNameOp)operator).input.getColName(i) );
					}					
				}
				selectedDimList.setModel(listModelSelected);
				culledDimList.setModel(listModelCulled);
			}
		}
	}
}
