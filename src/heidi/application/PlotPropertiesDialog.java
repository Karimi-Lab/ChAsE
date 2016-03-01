package heidi.application;

import heidi.plot.ScatterPlot;
import heidi.plot.Splom;
import heidi.project.Dim;
import heidi.project.Group;
import heidi.project.Plot;

import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.text.Document;

import prefuse.util.ui.UILib;

public class PlotPropertiesDialog extends JDialog {

	private Plot       m_plot;
	
	private String     m_name;
	private String     m_type;
	private Dim        m_xDim;
	private Dim        m_yDim;
	private DimModel   m_dimModel;
	
	private JComponent[] m_dimComponents;
	
	private int        m_result;
	
	public static final int OK = 1;
	public static final int CANCEL = 2;
	
	private static final long serialVersionUID = 4616007039916555399L;

	public PlotPropertiesDialog(Plot plot, JFrame frame) {
		super(frame, null, true);
		
		m_plot = plot;
		
		String plotName = m_plot.getName();
		if (plotName == null) {
			plotName = "Untitled";
		}
		setTitle("Properties for Plot "+plotName);
		
		createContent();
		
		UILib.setPlatformLookAndFeel();
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);		
		pack();
		setLocationRelativeTo(frame);
	}
	
	
	public String getName() {
		return m_name;
	}

	public String getType() {
		return m_type;
	}
	
	public Dim getXDimension() {
		return m_xDim;
	}
	
	public Dim getYDimension() {
		return m_yDim;
	}
	
	public Dim[] getDims() {
		if (m_dimModel != null) {
			return m_dimModel.getSelection();
		}
		return new Dim[] {m_xDim, m_yDim};
	}
	
	public int showDialog() {
		m_result = CANCEL;
		setVisible(true);
        return m_result;
	}
	
	private void createContent() {
		
		Container contentPane = getContentPane();
		contentPane.setLayout(new GridBagLayout());
		
		// Group Name
		JLabel nameLabel = new JLabel("Name:"); 
		JTextField nameField = new JTextField(50);
		String name = m_plot.getName();
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
		
		// Type Combobox
		JLabel typeLabel = new JLabel("Type:");
		String[] types = new String[]{ScatterPlot.getType(), Splom.getType()};
		JComboBox typeCombo = new JComboBox(types);
		typeCombo.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JComboBox typeCombo = (JComboBox)e.getSource();
				m_type = (String)typeCombo.getSelectedItem();
				createDimsComponents(Plot.getMultipleDimensions(m_type));
				pack();
			}
		});
		if (m_plot.getType() != null) {
			typeCombo.setSelectedItem(m_plot.getType());
		} else {
			typeCombo.setSelectedIndex(0);
		}
		m_type = (String)typeCombo.getSelectedItem();
		
		createDimsComponents(Plot.getMultipleDimensions(m_plot.getType()));
		
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
		
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridx = 0;
		constraints.gridy = 0;
		constraints.insets = new Insets(5, 5, 0, 0);
		constraints.anchor = GridBagConstraints.BASELINE_TRAILING;
		add(nameLabel, constraints);
		
		constraints.gridy = 1;
		add(typeLabel, constraints);
		
		constraints.gridx = 1;
		constraints.gridy = 0;
		constraints.gridwidth = 2;
		constraints.insets = new Insets(5, 5, 0, 5);
		constraints.anchor = GridBagConstraints.BASELINE_LEADING;
		constraints.weightx = 1.0;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		add(nameField, constraints);
		
		constraints.gridy = 1;
		add(typeCombo, constraints);
		
		// force items to top of layout
		// keep OK, Cancel buttons at bottom
		JLabel label = new JLabel();
		constraints = new GridBagConstraints();
		constraints.gridx = 0;
		constraints.gridy = 4;
		constraints.weighty = 1.0;
		add(label, constraints);
		
		constraints = new GridBagConstraints();
		constraints.gridx = 1;
		constraints.gridy = 5;
		constraints.insets = new Insets(15, 5, 5, 5);
		constraints.weightx = 1.0;
		constraints.anchor = GridBagConstraints.BASELINE_TRAILING;
		add(okButton, constraints);
		
		constraints.gridx = 2;
		constraints.gridy = 5;
		constraints.weightx = 0.0;
		constraints.anchor = GridBagConstraints.BASELINE_LEADING;
		add(cancelButton, constraints);
	}
	
	private void createDimsComponents(boolean multipleDimensions) {
		
		if (m_dimComponents != null) {
			for (JComponent component : m_dimComponents) {
				remove(component);
				component.revalidate();
			}
		}

		if (!multipleDimensions) {
			// XDimension Combo box
			JLabel xLabel = new JLabel("X Dimension:");
			Dim[] xDims = m_plot.getGroup().getDims();
			JComboBox xDimCombo = new JComboBox(xDims);
			Dim xDim = m_plot.getXDimension();
			if (xDim != null) {
				xDimCombo.setSelectedItem(xDim);
			} else {
				xDimCombo.setSelectedIndex(0);
			}
			m_xDim = (Dim)xDimCombo.getSelectedItem();
			xDimCombo.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					JComboBox xCombo = (JComboBox)e.getSource();
					m_xDim = (Dim)xCombo.getSelectedItem();
				}
			});
			
			// YDimension Combo box
			JLabel yLabel = new JLabel("Y Dimension:");
			Dim[] yDims = m_plot.getGroup().getDims();
			JComboBox yDimCombo = new JComboBox(yDims);
			Dim yDim = m_plot.getYDimension();
			if (yDim != null) {
				yDimCombo.setSelectedItem(yDim);
			} else {
				yDimCombo.setSelectedIndex(0);
			}
			m_yDim = (Dim)yDimCombo.getSelectedItem();
			yDimCombo.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					JComboBox yCombo = (JComboBox)e.getSource();
					m_yDim = (Dim)yCombo.getSelectedItem();
				}
			});
			
			m_dimComponents = new JComponent[] {
					xLabel, yLabel, xDimCombo, yDimCombo,
			};
			
			GridBagConstraints constraints = new GridBagConstraints();
			constraints.gridx = 0;
			constraints.gridy = 2;
			constraints.insets = new Insets(5, 5, 0, 0);
			constraints.anchor = GridBagConstraints.BASELINE_TRAILING;
			add(xLabel, constraints);
			
			constraints.gridy = 3;
			add(yLabel, constraints);
			
			constraints.gridx = 1;
			constraints.gridy = 2;
			constraints.gridwidth = 2;
			constraints.insets = new Insets(5, 5, 0, 5);
			constraints.anchor = GridBagConstraints.BASELINE_LEADING;
			constraints.weightx = 1.0;
			constraints.fill = GridBagConstraints.HORIZONTAL;
			add(xDimCombo, constraints);
			
			constraints.gridy = 3;
			add(yDimCombo, constraints);

		} else {
			
			m_dimModel = new DimModel(m_plot.getGroup(), m_plot.getDimensions());
			JTable table = new JTable(m_dimModel);
			table.setAutoCreateRowSorter(true);
			table.setFillsViewportHeight(true);
			table.getColumnModel().getColumn(0).setPreferredWidth(25);
			table.getColumnModel().getColumn(0).setMaxWidth(25);
			
			JScrollPane multiDims = new JScrollPane(table);
			multiDims.setBorder(BorderFactory.createEmptyBorder());

			m_dimComponents = new JComponent[] {
					multiDims,
			};
			
			GridBagConstraints constraints = new GridBagConstraints();
			constraints = new GridBagConstraints();
			constraints.gridx = 1;
			constraints.gridy = 2;
			constraints.gridwidth = 2;
			constraints.insets = new Insets(5, 5, 0, 5);
			constraints.fill = GridBagConstraints.BOTH;
			constraints.weightx = 1.0;
			constraints.weighty = 1.0;
			add(multiDims, constraints);
		}
	}
	
	private class DimModel extends AbstractTableModel {

		private Group       m_group;
		private Vector<Dim> m_selection;
		
		private static final int SELECTED = 0;
		private static final int NAME = 1;
		
		private static final long serialVersionUID = -1601625735758963499L;
		
		public DimModel(Group group, Dim[] selectedDims) {
			super();
			
			m_group = group;
			
			// convert array to Vector for easy lookup
			Vector<Dim> selection = new Vector<Dim>();
			if (selectedDims != null) {
				for (Dim dim : selectedDims) {
					selection.add(dim);
				}
			}
			m_selection = selection;
		}
		
		public Dim[] getSelection() {
			// remove nulls
			int count = 0;
			for (Dim dim : m_selection) {
				if (dim != null) {
					count++;
				}
			}
			Dim[] selection = new Dim[count];
			int index = 0;
			for (Dim dim : m_selection) {
				if (dim != null) {
					selection[index++] = dim;
				}
			}
			return selection;
		}
		
		@Override
	    public String getColumnName(int col) {
			if (col == SELECTED) {
				return " ";
			}
			if (col == NAME) {
				return "Dimension Name";
			}
			return "Unknown";
	    }
	    
		@Override
	    public int getRowCount() {
	    	return m_group.getDimCount();
	    }
	    
		@Override
        public Class<?> getColumnClass(int col) {
        	if (col == 0) {
        		return Boolean.class;
        	}
            return Object.class;
        }
        
		@Override
	    public int getColumnCount() {
	    	return 2;
	    }
	    
		@Override
	    public Object getValueAt(int row, int col) {
			Dim[] dims = m_group.getDims();
			Dim dim = dims[row];
	    	if (col == SELECTED) {
	    		return m_selection.contains(dim);
	    	}
	    	if (col == NAME) {
	    		return dim.getName();
	    	}
	        return "Unknown"; 
	    }
	    
		@Override
	    public boolean isCellEditable(int row, int col){
	    	return col == 0 ? true : false;
	    }
	    
		@Override
	    public void setValueAt(Object value, int row, int col) {
	    	if (col == 0) {
	    		Dim[] dims = m_group.getDims();
				Dim dim = dims[row];
	    		Boolean selected = (Boolean)value;
	    		if (selected) {
	    			m_selection.add(dim);
	    		} else {
	    			m_selection.remove(dim);
	    		}
	    	}
	        fireTableCellUpdated(row, col);
	    }
	};
}