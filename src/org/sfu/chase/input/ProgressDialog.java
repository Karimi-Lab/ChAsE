/*
 * Copyright (c) 1995, 2008, Oracle and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Oracle or the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */ 

package org.sfu.chase.input;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.SwingWorker.StateValue;
import java.beans.*;


// another alternative: http://www.javalobby.org/java/forums/t53926.html

@SuppressWarnings("serial")
public class ProgressDialog extends JPanel implements ActionListener, PropertyChangeListener
{
    JProgressBar m_ProgressBar;
    JTextArea    m_TextTaskOutput;
    Dialog       m_ModalDialog;
    JLabel       m_LabelActivity;
    boolean		 m_Indeterminate = false;
	public static String PROGRESS_PREFIX = "Progress:";
    
    
    private SwingWorker<?, ?> m_Task;

    public ProgressDialog() 
    {
        super(new BorderLayout());
        
        //Create the demo's UI.
//        startButton = new JButton("Start");
//        startButton.setActionCommand("start");
//        startButton.addActionListener(this);

        m_ProgressBar = new JProgressBar(0, 100);
        m_ProgressBar.setValue(0);
        m_ProgressBar.setStringPainted(true);

        m_TextTaskOutput = new JTextArea(5, 20);
        m_TextTaskOutput.setMargin(new Insets(5,5,5,5));
        m_TextTaskOutput.setEditable(false);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        //panel.add(startButton);
        m_LabelActivity = new JLabel("Processing ...");
        m_LabelActivity.setMaximumSize(new Dimension(Short.MAX_VALUE, 25));
        m_LabelActivity.setAlignmentX(LEFT_ALIGNMENT);
        m_ProgressBar.setMaximumSize(new Dimension(Short.MAX_VALUE, 25));
        m_ProgressBar.setAlignmentX(LEFT_ALIGNMENT);
        panel.add(m_LabelActivity);
        panel.add(m_ProgressBar);

        add(panel, BorderLayout.PAGE_START);
        add(new JScrollPane(m_TextTaskOutput), BorderLayout.CENTER);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

    	JFrame window = new JFrame();
		m_ModalDialog = new Dialog(window, "In Progress ...", true);
		m_ModalDialog.setLayout( new BoxLayout(m_ModalDialog, BoxLayout.Y_AXIS) );
		m_ModalDialog.add(this);
		//m_ModalDialog.setSize(new Dimension(500, 300));
		m_ModalDialog.setMinimumSize(new Dimension(500, 300));
		
		JButton buttonCancel = new JButton ("Cancel");
		buttonCancel.setMaximumSize(new Dimension(100, 25));
		buttonCancel.addActionListener(this);
		
		m_ModalDialog.add(buttonCancel);		
		m_ModalDialog.pack();
    }

    /**
     * Invoked when task's progress property changes.
     */
    public void propertyChange(PropertyChangeEvent evt)
    {
    	String evtName = evt.getPropertyName();
        if ("state" == evtName)
        {
        }
        else if (evtName.startsWith(PROGRESS_PREFIX))
        {
            int progress = (Integer) evt.getNewValue();
            m_ProgressBar.setValue(progress);
        	m_LabelActivity.setText(evtName.substring(PROGRESS_PREFIX.length()));
        }
        else if (evtName.startsWith("activity:"))
        {
        	m_LabelActivity.setText(evtName.substring("activity:".length()));
        }
        else if (evtName.startsWith("cancel"))
        {
        	m_ModalDialog.setVisible(false);
        }
        else if (evtName.startsWith("error"))
        {
        	m_TextTaskOutput.setForeground(Color.red);
        	m_TextTaskOutput.append(evtName.substring("error".length()));
        }
        else
        {
            m_TextTaskOutput.append(evtName+"\n");
            m_TextTaskOutput.setCaretPosition(m_TextTaskOutput.getDocument().getLength());
        }
        
        if (m_Task != null && m_Task.getState() == StateValue.DONE && !m_Task.isCancelled())
        {
        	m_ModalDialog.setVisible(false);
        }
    }

    public void actionPerformed(ActionEvent evt)
    {
    	if (evt.getActionCommand().equalsIgnoreCase("CANCEL"))
    	{
    		if (m_Task.getState() == StateValue.DONE) {
            	m_ModalDialog.setVisible(false);
    		} else {	
    			m_Task.cancel(true);
    		}
    	}
    }
    
    public boolean runTask(SwingWorker<?, ?> newTask)
    {
    	m_ProgressBar.setIndeterminate(m_Indeterminate);
    	m_ProgressBar.setValue(0);
    	m_Task = newTask;
    	m_Task.addPropertyChangeListener(this);
		m_ModalDialog.setLocationRelativeTo(null);
		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));  //turn on the wait cursor
    	m_TextTaskOutput.setText("");
    	
    	newTask.execute();
        
    	m_TextTaskOutput.setForeground(Color.black);
		m_ModalDialog.setVisible(true);
		
		//task finished or cancelled
        setCursor(null); //turn off the wait cursor
    	m_Task.removePropertyChangeListener(this);
        m_Task = null;
		return !newTask.isCancelled();
    }
    
    void setIndeterminate(boolean bIndeterminate)
    {
    	m_Indeterminate = bIndeterminate;
    }
}
