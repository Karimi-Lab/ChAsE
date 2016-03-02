package org.sfu.chase.gui;

import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

//import weka.gui.ExtensionFileFilter;

@SuppressWarnings("serial")
public class ExportImageDialog extends JPanel implements ActionListener, ChangeListener
{
    Dialog       	m_ModalDialog;
    boolean 		m_ModalResult;
    
    JSpinner 		m_SpinnerWidth;
    JSpinner		m_SpinnerHeight;
    JTextField		m_TextFilename;
	
    JCheckBox		m_CheckLegend;
	JCheckBox		m_CheckLabels;
    JSpinner		m_SpinnerFontSize;
	
    
    public ExportImageDialog() 
    {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        
        // Width & Height
        JPanel panelDimension = new JPanel();
        panelDimension.setLayout(new BoxLayout(panelDimension, BoxLayout.X_AXIS));
        panelDimension.setAlignmentX(LEFT_ALIGNMENT);
        panelDimension.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        panelDimension.add(new JLabel("Width:"));
        m_SpinnerWidth = new JSpinner(new SpinnerNumberModel(1000, 1, 10000, 1));
        m_SpinnerWidth.setMaximumSize(new Dimension(150, 25));
        m_SpinnerWidth.setAlignmentX(LEFT_ALIGNMENT);
        panelDimension.add(m_SpinnerWidth);
        
        panelDimension.add(Box.createRigidArea(new Dimension(10, 0)));
        panelDimension.add(new JLabel("Height:"));
        m_SpinnerHeight = new JSpinner(new SpinnerNumberModel(1000, 1, 10000, 1));
        m_SpinnerHeight.setMaximumSize(new Dimension(150, 25));
        m_SpinnerHeight.setAlignmentX(LEFT_ALIGNMENT);
        panelDimension.add(m_SpinnerHeight);
        
        add(panelDimension);
        
        // Options
        JPanel panelOptions = new JPanel();
        panelOptions.setLayout(new BoxLayout(panelOptions, BoxLayout.X_AXIS));
        panelOptions.setAlignmentX(LEFT_ALIGNMENT);
        panelOptions.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        
        panelOptions.add(new JLabel("Font Size:"));
        m_SpinnerFontSize = new JSpinner(new SpinnerNumberModel(30, 1, 100, 1));
        m_SpinnerFontSize.setMaximumSize(new Dimension(70, 25));
        m_SpinnerFontSize.setAlignmentX(LEFT_ALIGNMENT);
        panelOptions.add(m_SpinnerFontSize);
        
        panelOptions.add(Box.createRigidArea(new Dimension(10, 0)));
        panelOptions.add(m_CheckLegend = new JCheckBox("Legend", true));
        panelOptions.add(Box.createRigidArea(new Dimension(10, 0)));
        panelOptions.add(m_CheckLabels = new JCheckBox("Labels", true));

        add(panelOptions);
        
        
        // filename
        JPanel panelFilename = new JPanel();
        panelFilename.setLayout(new BoxLayout(panelFilename, BoxLayout.X_AXIS));
        panelFilename.add(m_TextFilename = new JTextField());
        panelFilename.add(newButton("...", "SELECT"));
        panelFilename.setMaximumSize(new Dimension(Short.MAX_VALUE, 25));
        panelFilename.setAlignmentX(LEFT_ALIGNMENT);
        add(Box.createRigidArea(new Dimension(0, 10)));
        add(new JLabel("Filename"));
        add(panelFilename);
        
        // cancel - ok
        JPanel panelBottomButtons = new JPanel();
        panelBottomButtons.setLayout(new BoxLayout(panelBottomButtons, BoxLayout.X_AXIS));
        panelBottomButtons.setAlignmentX(LEFT_ALIGNMENT);
        panelBottomButtons.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        
        panelBottomButtons.add(Box.createHorizontalGlue());
        panelBottomButtons.add(newButton("Cancel", "CANCEL"));
        registerKeyboardAction(this, "Cancel", KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        panelBottomButtons.add(Box.createRigidArea(new Dimension(10, 0)));
        panelBottomButtons.add(newButton("Export", "OK"));
        add(panelBottomButtons);
        
        // create the modal dialog
    	JFrame window = new JFrame();
		m_ModalDialog = new Dialog(window, "Export Options", true);
		m_ModalDialog.setMinimumSize(new Dimension(400, 200));
		m_ModalDialog.setLayout( new BoxLayout(m_ModalDialog, BoxLayout.Y_AXIS) );
		m_ModalDialog.add(this);
		m_ModalDialog.pack();
    }
    
    public boolean showModal()
    {
		m_ModalDialog.setLocationRelativeTo(null);
		m_ModalDialog.setVisible(true);
		return m_ModalResult;
    }
    
    public String getFilename()
    {
    	return m_TextFilename.getText();
    }
    
    public int getInputWidth()
    {
    	return (Integer) m_SpinnerWidth.getValue();
    }

    public int getInputHeight()
    {
    	return (Integer) m_SpinnerHeight.getValue();
    }
    
    public boolean isShowLegend()
    {
    	return m_CheckLegend.isSelected();
    }
    
    public boolean isShowLabels()
    {
    	return m_CheckLabels.isSelected();
    }
    
    public int getFontSize()
    {
    	return (Integer) m_SpinnerFontSize.getValue();
    }
    
    private JButton newButton(String label, String actionCommand)
    {
    	JButton button = new JButton(label);
    	button.setActionCommand(actionCommand);
    	button.addActionListener(this);
    	Dimension buttonDim = new Dimension(100, 25);
    	button.setMinimumSize(buttonDim);
    	button.setMaximumSize(buttonDim);
    	button.setPreferredSize(buttonDim);
    	return button;
    }
    
	@Override
	public void actionPerformed(ActionEvent e)
	{
        if (e.getActionCommand().equalsIgnoreCase("OK"))
        {	
        	if (getFilename().isEmpty())
        	{
        		if (!selectFilename())
        		{
        			return;
        		}
        	}
        	
        	m_ModalResult = true;
        	m_ModalDialog.setVisible(false);
        }
        else if (e.getActionCommand().equalsIgnoreCase("CANCEL"))
        {	
        	m_ModalResult = false;
        	m_ModalDialog.setVisible(false);
        }
        else if (e.getActionCommand().equalsIgnoreCase("SELECT"))
        {
        	selectFilename();
        }
	}
	
	private boolean selectFilename()
	{
		JFileChooser fc = new JFileChooser();
	    fc.setDialogTitle("Select Output Filename");
		if (!m_TextFilename.getText().equals(""))
			fc.setCurrentDirectory(new java.io.File(m_TextFilename.getText()).getParentFile());
		fc.setFileFilter(new FileNameExtensionFilter("PDF file", "pdf"));
		if( fc.showSaveDialog(null) == JFileChooser.APPROVE_OPTION )
		{
			String filename = fc.getSelectedFile().getAbsolutePath();
			if (!filename.toLowerCase().endsWith(".pdf")) {
				filename += ".pdf";
			}
			
			m_TextFilename.setText(filename);
			return true;
		}
		return false;
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		// TODO Auto-generated method stub
		
	}
}
