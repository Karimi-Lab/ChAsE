package heidi.application;

import heidi.project.Dim;
import heidi.project.PaletteMgr;
import heidi.project.Project;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.Document;

import prefuse.Constants;
import prefuse.data.Table;
import prefuse.data.column.ColumnMetadata;
import prefuse.util.ui.UILib;


public class ProjectPropertiesDialog extends JDialog {

	private Project    m_project;

	private String     m_name;
	private String     m_datafile;
	
	private Dim        m_colorDim;
	private int[]      m_palette;
	private int        m_highlightRGB = -1;
	private Dim        m_shapeDim;
	
	private ColorComboBox m_colorRangeCombo;
	private JPanel        m_colorLegend;
	
	private int        m_result;
	
	public static final int OK = 1;
	public static final int CANCEL = 2;
	
	private static final long serialVersionUID = -221241798009135258L;
	
	public ProjectPropertiesDialog(Project project, JFrame frame) {
		super(frame, null, true);
		
		m_project = project;
		
		String projectName = m_project.getName();
		if (projectName == null) {
			projectName = "Untitled";
		}
		setTitle("Properties for Project "+projectName);
		
		createContent();
		
		UILib.setPlatformLookAndFeel();
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);		
		pack();
		setLocationRelativeTo(frame);
	}
	
	public Dim getColorDimension() {
		return m_colorDim;
	}
	
	public String getDataFile() {
		return m_datafile;
	}
	
	public int getHighlightRGB() {
		return m_highlightRGB;
	}
	
	public String getName() {
		return m_name;
	}

	public int[] getPalette() {
		return m_palette == null ? null : m_palette.clone();
	}
	
	public Dim getShapeDimension() {
		return m_shapeDim;
	}
	
	public int showDialog() {
		m_result = CANCEL;
		setVisible(true);
        
        return m_result;
	}
	
	private void createContent() {
		
		// Project Name
		JLabel nameLabel = new JLabel("Name:"); 
		JTextField nameField = new JTextField(50);
		String name = m_project.getName();
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
		
		// Data File
		JLabel dataFileLabel = new JLabel("Data File:");
		final JTextField dataFileField = new JTextField(50);
		dataFileField.setEditable(false);
		File dataFile = m_project.getDataFile();
		JButton dataFileChooser = new JButton("Browse");;
		if (dataFile != null) {
			String dataFileName = dataFile.getAbsolutePath(); 
			dataFileField.setText(dataFileName);
			dataFileChooser.setEnabled(false); 
		} else {
			dataFileChooser.setEnabled(true); 
		}
		dataFileChooser.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// Dialog to select a data file
				JFileChooser fileChooser = new JFileChooser();
				FileFilter filter = new FileNameExtensionFilter("Text", "txt", "Comma Separated", "csv", "Compressed", "gz");
				fileChooser.setFileFilter(filter);
				fileChooser.setAcceptAllFileFilterUsed(false);
				fileChooser.setMultiSelectionEnabled(false);
				fileChooser.setName("Choose Project Data File");
				
			    if (fileChooser.showOpenDialog(ProjectPropertiesDialog.this) == JFileChooser.APPROVE_OPTION) {
			    	m_datafile = fileChooser.getSelectedFile().getAbsolutePath();
			    	dataFileField.setText(m_datafile);
			    }
			}
		});
		
		// Only show visual properties if there are valid
		// dimensions
		Container appearance = null;
		if (m_project.getDims() != null) {
			appearance = createAppearanceComponents();
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
		
		constraints.gridy = 1;
		add(dataFileLabel, constraints);
		
		constraints.gridx = 1;
		constraints.gridy = 0;
		constraints.insets = new Insets(5, 5, 0, 0);
		constraints.anchor = GridBagConstraints.BASELINE_LEADING;
		constraints.weightx = 1.0;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		add(nameField, constraints);
		
		constraints.gridy = 1;
		add(dataFileField, constraints);
		
		constraints = new GridBagConstraints();
		constraints.gridx = 2;
		constraints.gridy = 1;
		constraints.insets = new Insets(5, 5, 5, 5);
		constraints.anchor = GridBagConstraints.BASELINE_LEADING;
		add(dataFileChooser, constraints);
		
		if (appearance != null) {
			constraints = new GridBagConstraints();
			constraints.gridx = 0;
			constraints.gridy = 2;
			constraints.gridwidth = 3;
			constraints.fill = GridBagConstraints.BOTH;
			constraints.weighty = 1.0;
			constraints.insets = new Insets(5, 5, 5, 5);
			add(appearance, constraints);
		}
		
		constraints = new GridBagConstraints();
		constraints.gridx = 1;
		constraints.gridy = 3;
		constraints.insets = new Insets(15, 5, 5, 5);
		constraints.anchor = GridBagConstraints.BASELINE_TRAILING;
		constraints.weightx = 1.0;
		add(okButton, constraints);
		
		constraints.gridx = 2;
		constraints.anchor = GridBagConstraints.BASELINE_LEADING;
		constraints.weightx = 0.0;
		add(cancelButton, constraints);
	}
	
	private Container createAppearanceComponents() {
		
		JTabbedPane tabs = new JTabbedPane();
		
		// Color Settings
		Container colorComponents = createColorComponents();
		tabs.add("Colors", colorComponents);
		
		// Shape Settings
		Container shapeComponents = createShapeComponents();
		tabs.add("Shapes", shapeComponents);
		
		return tabs;
	}
	
	private Container createColorComponents() {
		
		JPanel panel = new JPanel();
		panel.setLayout(new GridBagLayout());
		
		// Highlight Color
		JLabel highlightLabel = new JLabel("Highlight Color:");
		final JLabel highlightSplotch = new JLabel();
		highlightSplotch.setOpaque(true);
		highlightSplotch.setMinimumSize(new Dimension(100, 50));
		Color highlight = new Color(m_project.getHighlightRGB());
		highlightSplotch.setBackground(highlight);
		JButton highlightButton = new JButton("Choose Color...");
		highlightButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Color newColor = JColorChooser.showDialog(ProjectPropertiesDialog.this, "Choose Highlight Color", new Color(m_highlightRGB));
				if (newColor != null) {
					highlightSplotch.setBackground(newColor);
					m_highlightRGB = newColor.getRGB();
				}
			}
		});

		// Color Dimension Combo box
		JLabel colorDimLabel = new JLabel("Color Dimension:");
		Dim[] colorDims = m_project.getDims();
		JComboBox colorDimCombo = new JComboBox(colorDims);
		Dim colorDim = m_project.getColorDimension();
		if (colorDim != null) {
			colorDimCombo.setSelectedItem(colorDim);
		} else {
			if (colorDims != null && colorDims.length > 0) {
				colorDimCombo.setSelectedIndex(0);
			}
		}
		m_colorDim = (Dim)colorDimCombo.getSelectedItem();
		colorDimCombo.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JComboBox colorDimCombo = (JComboBox)e.getSource();
				m_colorDim = (Dim)colorDimCombo.getSelectedItem();
				
				// update other components
				m_colorRangeCombo.setColorCount(m_colorDim.getBinCount());
				m_palette = PaletteMgr.GetInstance().getDefaultPalette(m_colorDim.getType(), m_colorDim.getBinCount());
				int index = m_colorRangeCombo.indexOf(m_palette);
				m_colorRangeCombo.setSelectedIndex(index);
				createColorLegend(m_colorLegend);
			}
		});
		
		// Color Combo Box
		JLabel colorLabel = new JLabel("Color Palette:");
		int colorCount = 10;
		if (m_colorDim != null) {
			colorCount = m_colorDim.getBinCount();
		}
		m_colorRangeCombo = new ColorComboBox(colorCount);
		if (m_colorDim != null) {
			int[] palette = m_project.getPalette();
			int paletteIndex = m_colorRangeCombo.indexOf(palette);
			if (paletteIndex != -1) {
				m_colorRangeCombo.setSelectedIndex(paletteIndex);
			}
		}
		m_colorRangeCombo.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ColorComboBox colorPicker = (ColorComboBox)e.getSource();
				int index = colorPicker.getSelectedIndex();
				m_palette = colorPicker.getPalette(index);
				createColorLegend(m_colorLegend);
			}
		});
		
		// Color legend
		JLabel colorLegendLabel = new JLabel("Color Legend:");
		m_colorLegend = new JPanel();
		createColorLegend(m_colorLegend);
		
		// layout
		
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridx = 0;
		constraints.gridy = 0;
		constraints.insets = new Insets(5, 5, 5, 5);
		constraints.anchor = GridBagConstraints.BASELINE_TRAILING;
		panel.add(highlightLabel, constraints);
		
		constraints.gridy = 1;
		panel.add(colorDimLabel, constraints);
		
		constraints.gridy = 2;
		panel.add(colorLabel, constraints);
		
		constraints.gridy = 3;
		panel.add(colorLegendLabel, constraints);
		
		constraints.gridx = 1;
		constraints.gridy = 0;
		constraints.insets = new Insets(5, 5, 5, 5);
		constraints.anchor = GridBagConstraints.BASELINE_LEADING;
		constraints.fill = GridBagConstraints.BOTH;
		constraints.weightx = 1.0;
		panel.add(highlightSplotch, constraints);
		
		constraints = new GridBagConstraints();
		constraints.gridx = 2;
		constraints.gridy = 0;
		constraints.insets = new Insets(5, 5, 5, 5);
		panel.add(highlightButton, constraints);
		
		constraints.gridx = 1;
		constraints.gridy = 0;
		constraints.gridwidth = 2;
		constraints.insets = new Insets(5, 5, 5, 5);
		constraints.anchor = GridBagConstraints.BASELINE_LEADING;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.weightx = 1.0;		
		constraints.gridy = 1;
		panel.add(colorDimCombo, constraints);
		
		constraints.gridy = 2;
		panel.add(m_colorRangeCombo, constraints);
		
		constraints = new GridBagConstraints();
		constraints.gridx = 1;
		constraints.gridy = 3;
		constraints.insets = new Insets(5, 5, 5, 5);
		constraints.fill = GridBagConstraints.BOTH;
		panel.add(m_colorLegend, constraints);
		
		// Force widgets to the top of area
		JLabel label = new JLabel();
		constraints = new GridBagConstraints();
		constraints.gridx = 1;
		constraints.gridy = 4;
		constraints.weighty = 1.0;
		panel.add(label, constraints);
		
		JScrollPane scrollPane = new JScrollPane(panel);
		return scrollPane;
	}
	
	private void createColorLegend(JPanel parent) {
		parent.removeAll();
		
		parent.setLayout(new GridBagLayout());
		
		if (m_colorDim != null) {
			
			int[] palette = m_colorRangeCombo.getPalette(m_colorRangeCombo.getSelectedIndex());
			
			Table table = m_project.getTable();
			ColumnMetadata columnData = table.getMetadata(m_colorDim.getColumn());
			
			int type = m_colorDim.getType();
			if (type == Constants.NUMERICAL) {
				// Numerical color scheme
				double count = m_colorDim.getBinCount();
				int minRow = columnData.getMinimumRow();
				int maxRow = columnData.getMaximumRow();
				double min = table.getDouble(minRow, m_colorDim.getColumn());
				double max = table.getDouble(maxRow, m_colorDim.getColumn());
				
				// TODO - need to get bin start and end from frequency table 
				double binWidth = (max - min)/(count - 1.0);
				double binStart = min;
				double binEnd = min+binWidth;
				
				for (int i = 0; i < palette.length; i++) {
					JPanel colorSplotch = new JPanel();
					Dimension size = new Dimension(15, 15);
					colorSplotch.setMinimumSize(size);
					colorSplotch.setPreferredSize(size);
					colorSplotch.setBackground(new Color(palette[i]));
					
					JLabel textLabel = new JLabel(binStart+" to "+binEnd);
					
					binStart = binEnd;
					binEnd = binStart + binWidth;
					
					GridBagConstraints constraints = new GridBagConstraints();
					constraints.gridx = 0;
					constraints.gridy = i;
					constraints.insets = new Insets(2, 2, 2, 2);
					constraints.fill = GridBagConstraints.BOTH;
					parent.add(colorSplotch, constraints);
					
					constraints.gridx = 1;
					constraints.fill = -1;
					constraints.anchor = GridBagConstraints.BASELINE_LEADING;
					parent.add(textLabel, constraints);
				}
			} else {
				// Ordinal and Categorical color scheme
				Object[] values = columnData.getOrdinalArray();
				int length = Math.min(palette.length, values.length);
				
				for (int i = 0; i <length; i++) {
					JPanel colorSplotch = new JPanel();
					colorSplotch.setPreferredSize(new Dimension(10, 10));
					colorSplotch.setBackground(new Color(palette[i]));
					
					JLabel textLabel = new JLabel(values[i].toString());
					
					GridBagConstraints constraints = new GridBagConstraints();
					constraints.gridx = 0;
					constraints.gridy = i;
					constraints.insets = new Insets(2, 2, 2, 2);
					parent.add(colorSplotch, constraints);
					
					constraints.gridx = 1;
					constraints.anchor = GridBagConstraints.BASELINE_LEADING;
					parent.add(textLabel, constraints);
				}
				
			}
			
			// Push all items to left side
			JLabel spacer = new JLabel("");
			GridBagConstraints constraints = new GridBagConstraints();
			constraints.gridx = 2;
			constraints.gridy = 0;
			constraints.weightx = 1.0;
			parent.add(spacer, constraints);
			
			parent.revalidate();
		}
	}
	
	private Container createShapeComponents() {
		
		JPanel panel = new JPanel();
		panel.setLayout(new GridBagLayout());
		
		// Shape Dimension Combo box
		JLabel shapeLabel = new JLabel("Shape Dimension:");
		Dim[] shapeDims = m_project.getDims();
		JComboBox shapeCombo = new JComboBox(shapeDims);
		Dim shapeDim = m_project.getColorDimension();
		if (shapeDim != null) {
			shapeCombo.setSelectedItem(shapeDim);
		} else {
			if (shapeDims != null && shapeDims.length > 0) {
				shapeCombo.setSelectedIndex(0);
			}
		}
		m_shapeDim = (Dim)shapeCombo.getSelectedItem();
		shapeCombo.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JComboBox shapeCombo = (JComboBox)e.getSource();
				m_shapeDim = (Dim)shapeCombo.getSelectedItem();
			}
		});
		
		// layout
		
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridx = 0;
		constraints.gridy = 3;
		constraints.insets = new Insets(20, 5, 5, 5);
		constraints.anchor = GridBagConstraints.BASELINE_TRAILING;
		panel.add(shapeLabel, constraints);
		
		constraints.gridx = 1;
		constraints.gridy = 3;
		constraints.insets = new Insets(20, 5, 5, 5);
		constraints.anchor = GridBagConstraints.BASELINE_LEADING;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.weightx = 1.0;
		panel.add(shapeCombo, constraints);
		
		// Force widgets to the top of area
		JLabel label = new JLabel();
		constraints = new GridBagConstraints();
		constraints.gridx = 1;
		constraints.gridy = 4;
		constraints.weighty = 1.0;
		panel.add(label, constraints);
		
		JScrollPane scrollPane = new JScrollPane(panel);
		return scrollPane;
	}
}

