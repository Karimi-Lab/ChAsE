package heidi.application;

import heidi.project.Dim;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;

import prefuse.Constants;
import prefuse.util.ui.UILib;

public class DimPropertiesDialog extends JDialog {

	private Vector<Dim>     m_dims;
	
	private Vector<String>  m_names;
	private Vector<Integer> m_types;
	private Vector<Integer> m_binCounts;
	
	private int             m_result;
	
	public static final int OK = 1;
	public static final int CANCEL = 2;
	
	private static final long serialVersionUID = 4616007039916555399L;

	public DimPropertiesDialog(Dim[] dims, JFrame frame) {
		super(frame, "Dimension Properties", true);
		
		m_dims = new Vector<Dim>();
		m_names = new Vector<String>(m_dims.size());
		m_types = new Vector<Integer>(m_dims.size());
		m_binCounts = new Vector<Integer>(m_dims.size());
		for (Dim dim : dims) {
			m_dims.add(dim);
			m_names.add(null);
			m_types.add(new Integer(-1));
			m_binCounts.add(new Integer(-1));
		}
		
		createContent();
		
		UILib.setPlatformLookAndFeel();
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		Dimension preferredSize = getPreferredSize();
		preferredSize.width += 40; // make space for scrollbar
		preferredSize.height = Math.min(preferredSize.height, 600); // limit height of dialog
		setSize(preferredSize);
		setLocationRelativeTo(frame);
	}
	
	
	public String getName(Dim dim) {
		int index = m_dims.indexOf(dim);
		if (index >= m_names.size()) {
			return null;
		}
		return m_names.get(index);
	}

	public int getType(Dim dim) {
		int index = m_dims.indexOf(dim);
		if (index >= m_types.size()) {
			return -1;
		}
		return m_types.get(index);
	}
	
	public int showDialog() {
		m_result = CANCEL;
		setVisible(true);
        return m_result;
	}
	
	private void createContent() {
		
		// Create dimensions area
		JPanel dimsPanel = new JPanel();
		dimsPanel.setBorder(BorderFactory.createEmptyBorder());
		
		dimsPanel.setLayout(new GridBagLayout());
		dimsPanel.setMaximumSize(new Dimension(-1, 300));
		
		int row = 0;
		for (Dim dim : m_dims) {
			createDimComponents(dim, dimsPanel, row);
			row += 2;
		}
		// force dim items to top of layout
		JLabel label = new JLabel();
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridx = 0;
		constraints.gridy = row + 1;
		constraints.weighty = 1.0;
		dimsPanel.add(label, constraints);
		
		JScrollPane dimsScrollPane = new JScrollPane(dimsPanel);
		dimsScrollPane.setBorder(BorderFactory.createEmptyBorder());
		
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
		
		constraints = new GridBagConstraints();
		constraints.gridx = 0;
		constraints.gridy = 0;
		constraints.gridwidth = 3;
		constraints.insets = new Insets(5, 5, 0, 5);
		constraints.anchor = GridBagConstraints.FIRST_LINE_START;
		constraints.fill = GridBagConstraints.BOTH;
		constraints.weightx = 1.0;
		constraints.weighty = 1.0;
		
		add(dimsScrollPane, constraints);
		
		constraints = new GridBagConstraints();
		constraints.gridx = 1;
		constraints.gridy = 2;
		constraints.insets = new Insets(15, 5, 5, 5);
		constraints.anchor = GridBagConstraints.LINE_END;
		constraints.weightx = 1.0;
		add(okButton, constraints);
		
		constraints.gridx = 2;
		constraints.anchor = GridBagConstraints.LINE_START;
		constraints.weightx = 0.0;
		add(cancelButton, constraints);
	}
	
	private void createDimComponents(final Dim dim, JPanel parent, int row) {
		
		// Group Name
		JLabel nameLabel = new JLabel("Name:"); 
		JTextField nameField = new JTextField(50);
		String name = dim.getName();
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
					int index = m_dims.indexOf(dim);
					m_names.set(index, name);
				}
			}
		});
		
		// Type Combobox
		JLabel typeLabel = new JLabel("Type:");
		String[] types = new String[]{Dim.NOMINAL_STR, Dim.ORDINAL_STR, Dim.NUMERICAL_STR};
		JComboBox typeCombo = new JComboBox(types);
		int dimType = dim.getType();
		switch (dimType) {
		case Constants.NOMINAL:
			typeCombo.setSelectedItem(Dim.NOMINAL_STR); break;
		case Constants.ORDINAL:
			typeCombo.setSelectedItem(Dim.ORDINAL_STR); break;
		case Constants.NUMERICAL:
			typeCombo.setSelectedItem(Dim.NUMERICAL_STR); break;
		}
		typeCombo.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JComboBox typeCombo = (JComboBox)e.getSource();
				String typeName = (String)typeCombo.getSelectedItem();
				int index = m_dims.indexOf(dim);
				if (Dim.NOMINAL_STR.equals(typeName)) {
					m_types.set(index, new Integer(Constants.NOMINAL));
				}
				if (Dim.ORDINAL_STR.equals(typeName)) {
					m_types.set(index, new Integer(Constants.ORDINAL));
				}
				if (Dim.NUMERICAL_STR.equals(typeName)) {
					m_types.set(index, new Integer(Constants.NUMERICAL));
				}
				// reset bin count
				int defaultCount = dim.getDefaultBinCount();
				m_binCounts.set(index, new Integer(defaultCount));
			}
		});

		GridBagConstraints constraints = new GridBagConstraints();
		
		constraints.gridx = 0;
		constraints.gridy = row;
		constraints.insets = new Insets(2, 5, 2, 0);
		constraints.anchor = GridBagConstraints.BASELINE;
		parent.add(nameLabel, constraints);
		constraints.gridy = row + 1;
		parent.add(typeLabel, constraints);
		
		constraints.gridx = 1;
		constraints.gridy = row;
		constraints.insets = new Insets(2, 5, 2, 15);
		constraints.anchor = GridBagConstraints.BASELINE;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.weightx = 1.0;
		parent.add(nameField, constraints);
		
		constraints.gridy = row + 1;
		constraints.insets = new Insets(2, 5, 15, 15);
		parent.add(typeCombo, constraints);
		
	}
}