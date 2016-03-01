package heidi.application;

import heidi.dimreduction.JamaPCAnalyzer;
import heidi.dimreduction.PrincipalComponent;
import heidi.project.DataFileDim;
import heidi.project.Dim;
import heidi.project.Group;

import java.awt.Container;
import java.awt.Cursor;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.text.Document;

import prefuse.util.ui.UILib;

public class GroupPropertiesDialog extends JDialog {

	private Group      m_group;
	
	private String     m_name;
	private DimModel   m_dimModel;
	
	private int        m_result;

	private JPanel     m_pcaPanel;
	private JCheckBox  m_showPCA;

	public static final int OK = 1;
	public static final int CANCEL = 2;
	
	private static final int SELECTED = 0;
	private static final int ID = 1;
	private static final int NAME = 2;
	private static final int RANK = 3;
	private static final int MERIT = 4;
	private static final int COLUMNCOUNT = 5;
	
	private static final long serialVersionUID = -7877648415315879746L;
	
	public GroupPropertiesDialog(Group group, JFrame frame) {
		super(frame, null, true);
		
		m_group = group;
		
		String groupName = m_group.getName();
		if (groupName == null) {
			groupName = "Untitled";
		}
		setTitle("Properties for Group "+groupName);
		
		startProgress();
		createContent();
		endProgress();
		
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);		
		UILib.setPlatformLookAndFeel();
		pack();
		setLocationRelativeTo(frame);
	}

	public String getName() {
		return m_name;
	}
	
	public Dim[] getDimensions() {
		return m_dimModel.getSelection();
	}
	
	public int showDialog() {
        setVisible(true);
        return m_result;
	}
	
	private void createContent() {
		
		// Group Name
		JLabel nameLabel = new JLabel("Name:"); 
		JTextField nameField = new JTextField(50);
		String name = m_group.getName();
		if (name != null) {
			nameField.setText(name);
		}
		Document document = nameField.getDocument();
		document.putProperty("JTextField", nameField);
		document.addDocumentListener(new DocumentListener() {
			@Override
			public void removeUpdate(DocumentEvent event) {
				update(event);
			}
			@Override
			public void insertUpdate(DocumentEvent event) {
				update(event);
			}
			@Override
			public void changedUpdate(DocumentEvent event) {
				update(event);
			}
			private void update(DocumentEvent event) {
				Document document = event.getDocument();
				JTextField nameField = (JTextField)document.getProperty("JTextField");
				String name = nameField.getText();
				if (name != null && name.length() > 0) {
					m_name = name;
				}
			}
		});
		
		Container dimsComponents = createDimsComponents();
		
		// check box for calculating importance
		JCheckBox calcImportance = new JCheckBox("Calculate Importance of Dimensions using PCA");
		calcImportance.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JCheckBox checkBox = (JCheckBox)e.getSource();
				boolean checked = checkBox.isSelected();
				m_dimModel.initImportance(checked);
				m_pcaPanel.setVisible(checked);
			}
		});
		
		m_pcaPanel = createPCAComponents();
		
		// If any of the projects dims are Principal Components, 
		// enable Principal Component Analysis
		int pcCount = m_group.getProject().getPrincipalComponentCount();
		if (pcCount > 0) {
			calcImportance.setSelected(true);
			m_showPCA.setSelected(true);
			m_dimModel.initImportance(true);
			m_dimModel.initPrincipalComponents(true);
		} else {
			m_pcaPanel.setVisible(false);
		}
		
		// OK and Cancel buttons
		JButton okButton = new JButton("OK");
		okButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// apply changes after closing dialog
				setVisible(false);
				m_result = OK;
			}
		});
		
		JButton cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// close dialog without applying changes
				setVisible(false);
				m_result = CANCEL;
			}
		});
		
		// layout
		Container contentPane = getContentPane();
		contentPane.setLayout(new GridBagLayout());
		
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridx = 0;
		constraints.gridy = 0;
		constraints.insets = new Insets(5, 5, 0, 0);
		constraints.anchor = GridBagConstraints.BASELINE_TRAILING;
		add(nameLabel, constraints);
		
		constraints.gridx = 1;
		constraints.gridy = 0;
		constraints.gridwidth = 1;
		constraints.insets = new Insets(5, 5, 0, 5);
		constraints.anchor = GridBagConstraints.BASELINE_LEADING;
		constraints.weightx = 1.0;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		add(nameField, constraints);
		
		constraints = new GridBagConstraints();
		constraints.gridx = 0;
		constraints.gridy = 1;
		constraints.gridwidth = 2;
		constraints.gridheight = 3;
		constraints.insets = new Insets(5, 5, 0, 0);
		constraints.fill = GridBagConstraints.BOTH;
		constraints.weightx = 1.0;
		constraints.weighty = 1.0;
		add(dimsComponents, constraints);
		
		constraints = new GridBagConstraints();
		constraints.gridx = 2;
		constraints.gridy = 1;
		constraints.anchor = GridBagConstraints.FIRST_LINE_START;
		add(calcImportance, constraints);
		
		constraints.gridx = 2;
		constraints.gridy = 2;
		constraints.gridheight = 2;
		constraints.anchor = GridBagConstraints.NORTHWEST;
		constraints.fill = GridBagConstraints.BOTH;
		constraints.weightx = 1.0;
		constraints.weighty = 1.0;
		add(m_pcaPanel, constraints);
		
		constraints = new GridBagConstraints();
		constraints.gridx = 1;
		constraints.gridy = 4;
		constraints.insets = new Insets(15, 5, 5, 5);
		constraints.weightx = 1.0;
		constraints.anchor = GridBagConstraints.BASELINE_TRAILING;
		add(okButton, constraints);
		
		constraints.gridx = 2;
		constraints.gridy = 4;
		constraints.weightx = 0.0;
		constraints.anchor = GridBagConstraints.BASELINE_LEADING;
		add(cancelButton, constraints);

	}
	
	private Container createDimsComponents() {
		// Dimensions List
		m_dimModel = new DimModel(m_group);
		
		JTable table = new JTable(m_dimModel);
		table.setAutoCreateRowSorter(true);
		table.setFillsViewportHeight(true);
		table.getColumnModel().getColumn(SELECTED).setPreferredWidth(25);
		table.getColumnModel().getColumn(SELECTED).setMaxWidth(25);
		table.getColumnModel().getColumn(ID).setPreferredWidth(25);
		table.getColumnModel().getColumn(ID).setMaxWidth(25);
		
		JScrollPane scrolledTable = new JScrollPane(table);
		scrolledTable.setBorder(BorderFactory.createEmptyBorder());
		return scrolledTable;
	}
	
	private JPanel createPCAComponents() {
		JPanel panel = new JPanel();
		
		// check box for showing principal components
		m_showPCA = new JCheckBox("Show Principal Components as Dimensions");
		m_showPCA.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JCheckBox checkBox = (JCheckBox)e.getSource();
				m_dimModel.initPrincipalComponents(checkBox.isSelected());
			}
		});
		
		//TODO - Render Scree plot
		JPanel screePlot = new JPanel();
		
		panel.setLayout(new GridBagLayout());
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridx = 0;
		constraints.gridy = 0;
		constraints.anchor = GridBagConstraints.FIRST_LINE_START;
		panel.add(m_showPCA, constraints);
		
		constraints = new GridBagConstraints();
		constraints.gridx = 0;
		constraints.gridy = 1;
		constraints.fill = GridBagConstraints.BOTH;
		constraints.weightx = 1.0;
		constraints.weighty = 1.0;
		panel.add(screePlot, constraints);
		
		return panel;
	}
	
	private void startProgress() {
		Container contentPane = getParent();
		while (contentPane.getParent() != null) {
			contentPane = contentPane.getParent();
		}
		contentPane.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
	}
	
	private void endProgress() {
		Container contentPane = getParent();
		while (contentPane.getParent() != null) {
			contentPane = contentPane.getParent();
		}
		contentPane.setCursor(null);
	}
	
	private class DimModel extends AbstractTableModel {

		private Group       m_group;
		private Vector<Dim> m_selection;
		private Vector<Dim> m_dims;
		
		// PCA dimension reduction
		private boolean  m_showImportance;
		private boolean  m_showPrincipalComponents;
		private JamaPCAnalyzer  m_dimReducer;
		
		private static final long serialVersionUID = -1601625735758963499L;
		
		public DimModel(Group group) {
			super();
			m_group = group;
			
			// convert array to Vector for easy lookup
			Vector<Dim> selection = new Vector<Dim>();
			Dim[] groupDims = group.getDims();
			if (groupDims != null) {
				for (Dim dim : groupDims) {
					selection.add(dim);
				}
			}
			m_selection = selection;
			
			Vector<Dim> dims = new Vector<Dim>();
			Dim[] allDims = m_group.getProject().getDims();
			if (allDims != null) {
				for (Dim dim : allDims) {
					dims.add(dim);
				}
			}
			m_dims = dims;
		}
		
		public Dim[] getSelection() {
			Dim[] selection = new Dim[m_selection.size()];
			return m_selection.toArray(selection);
		}
		
		@Override
	    public String getColumnName(int col) {
			if (col == SELECTED) {
				return " ";
			}
			if (col == ID) {
				return "#";
			}
			if (col == NAME) {
				return "Dimension Name";
			}
			if (col == RANK) {
				return "Importance";
			}
			if (col == MERIT) {
				return "Merit";
			}

			return "Unknown";
	    }
	    
		@Override
	    public int getRowCount() {
	    	return m_dims.size();
	    }
	    
		@Override
        public Class<?> getColumnClass(int col) {
			switch (col) {
				case SELECTED: {
					return Boolean.class;
				} case ID: {
					return Integer.class;
				} case NAME:{
					return String.class;
				} case RANK: {
					return Integer.class;
				} case MERIT: {
					return Double.class;
				}
			}
            return Object.class;
        }
        
		@Override
	    public int getColumnCount() {
	    	return COLUMNCOUNT;
	    }
	    
		@Override
	    public Object getValueAt(int row, int col) {
			Dim dim = m_dims.get(row);
			switch (col) {
				case SELECTED: {
					return m_selection.contains(dim);
				} case NAME: {
					return dim.getName();
				} case RANK: {
					if (m_showImportance && dim instanceof DataFileDim) {
						// only show rank for original data file dimensions 
						Dim[] pcaOrder = m_dimReducer.getOriginalDimensions();
						for (int i = 0; i < pcaOrder.length; i++) {
							if (pcaOrder[i].equals(dim)) {
								return i + 1;
							}
						}
					}
					return null;
				} case MERIT: {
					if (m_showPrincipalComponents && dim instanceof PrincipalComponent) {
						int index = ((PrincipalComponent)dim).getRank();
						return m_dimReducer.getProportions()[index];
					}
					return null;
				} case ID: {
					if (dim instanceof DataFileDim) {
						return m_group.getProject().getTable().getColumnNumber(dim.getColumn()) + 1;
					}
					return null;
				}
			}
			return null;
	    }
	    
		@Override
	    public boolean isCellEditable(int row, int col){
	    	return col == SELECTED ? true : false;
	    }
	    
		@Override
	    public void setValueAt(Object value, int row, int col) {
	    	if (col == SELECTED) {
	    		Dim dim = m_dims.get(row);
	    		Boolean selected = (Boolean)value;
	    		if (selected) {
	    			m_selection.add(dim);
	    		} else {
	    			m_selection.remove(dim);
	    		}
	    	}
	        fireTableCellUpdated(row, col);
	    }
		
		public void initImportance(boolean enable) {
			if (enable == m_showImportance) {
				return;
			}
			m_showImportance = enable;
			if (m_showImportance) {
				m_dimReducer = new JamaPCAnalyzer(m_group.getProject());
			} else {
				m_dimReducer = null;
			}
			fireTableDataChanged();
		}
		
		public void initPrincipalComponents(boolean enable) {
			if (enable == m_showPrincipalComponents) {
				return;
			}
			int oldCount = m_dims.size();
			m_showPrincipalComponents = enable;
			int pcCount = 0;
			if (!m_showPrincipalComponents) {
				// Loop over model and remove
				// Principal Components
				for (int i = m_dims.size() - 1; i >= 0; i--) {
					Dim dim = m_dims.get(i);
					if (dim instanceof PrincipalComponent) {
						m_dims.remove(dim);
						pcCount++;
					}
				}
				// Loop over selection and remove
				// Principal Components
				for (int i = m_selection.size() - 1; i >= 0; i--) {
					Dim dim = m_selection.get(i);
					if (dim instanceof PrincipalComponent) {
						m_selection.remove(dim);
					}
				}
				if (pcCount > 0) {
					fireTableRowsDeleted(m_dims.size(), oldCount-1);
				}
			} else {
				// Add Principal Components to model
				PrincipalComponent[] pcs = m_dimReducer.getPrincipalComponents();
				for (PrincipalComponent pc : pcs) {
					// Note: a PC may already be contained in m_dims.
					// If this method is called during the initialization of the dialog
					// and the group already contains principal components and 
					// the user is adjusting the group properties (i.e. using
					// Group -> Group Properties and not Group -> Add Group) 
					// then this situation will occur.
					if (!m_dims.contains(pc)) {
						m_dims.add(pc);
						pcCount++;
					}
				}
				if (pcCount > 0) {
					fireTableRowsInserted(oldCount, m_dims.size()-1);
				}
			}
		}
	}
}
