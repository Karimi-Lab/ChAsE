package chase.gui;

import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

@SuppressWarnings("serial")
public class RegionsDialog extends JPanel implements ActionListener, ChangeListener
{
    Dialog       	m_ModalDialog;
    boolean 		m_ModalResult;
	JTextArea		m_TextAreaRegions;
	
    
    public RegionsDialog() 
    {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        
        m_TextAreaRegions = new JTextArea();
        JScrollPane myScrollPane = new JScrollPane(m_TextAreaRegions);
        add(myScrollPane);
        
        // cancel - ok
        JPanel panelBottomButtons = new JPanel();
        panelBottomButtons.setLayout(new BoxLayout(panelBottomButtons, BoxLayout.X_AXIS));
        panelBottomButtons.setAlignmentX(LEFT_ALIGNMENT);
        panelBottomButtons.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        
        panelBottomButtons.add(Box.createHorizontalGlue());
        panelBottomButtons.add(newButton("Cancel", "CANCEL"));
        registerKeyboardAction(this, "Cancel", KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        
        panelBottomButtons.add(Box.createRigidArea(new Dimension(10, 0)));
        panelBottomButtons.add(newButton("Save...", "OK"));
        add(panelBottomButtons);
        
        // create the modal dialog
    	JFrame window = new JFrame();
		m_ModalDialog = new Dialog(window, "Regions", true);
		m_ModalDialog.setMinimumSize(new Dimension(500, 400));
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
    
    public void setText(String text)
    {
    	m_TextAreaRegions.setText(text);
    }
    
	@Override
	public void actionPerformed(ActionEvent e)
	{
        if (e.getActionCommand().equalsIgnoreCase("OK"))
        {	
        	m_ModalResult = true;
        	m_ModalDialog.setVisible(false);
        }
        else if (e.getActionCommand().equalsIgnoreCase("CANCEL"))
        {	
        	m_ModalResult = false;
        	m_ModalDialog.setVisible(false);
        }
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		// TODO Auto-generated method stub
		
	}
}
