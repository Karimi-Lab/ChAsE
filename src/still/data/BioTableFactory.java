package still.data;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;

import still.expression.Expression;
import still.gui.EnumUtils;

import java.lang.String;
import collab.*;

/**
 * General factory for creating tables from genomic data (GFF and WIG files).
 * http://genome.ucsc.edu/FAQ/FAQformat.html
 * @author Hamid Younesy
 */

public class BioTableFactory extends TableFactory
{
	public static Table procTable( Container container )
	{
        //MemoryTable memTab = ProcTableFactory.createTable(s_iInitDataSize, s_iInitDataDim);
		MemoryTable memTab = BioTableFactory.createTable(new double[1][1], null);
		if( memTab != null )
		{
			memTab.setInputControl( new BioReaderPanel(memTab) );
		}
		return memTab;
	}
	
	/**
	 */
	public static MemoryTable createTable(double[][] newTable, String[] colNames)
	{
        int iDim = newTable[0].length;
        if (colNames == null)
        {
        	colNames = new String[iDim];
        	for( int d = 0; d < iDim; d++ )
        	{
        		colNames[d] = "D" + Integer.toString(d+1);
        	}
        }
		MemoryTable memTab = new MemoryTable(newTable, colNames);
		memTab.setDescriptor("BioTable");
		return memTab;
	}

	public static class BioReaderPanel extends JPanel implements ActionListener
	{
		private static final long serialVersionUID = 2727706842660832284L;
		
		Table 			m_InternalTable = null;
		Expression 		m_Exp = null;
		MemoryTable 	m_MemTable = null;
		boolean 		block = false;
		BioReader		m_BioReader;
		//String 			m_NormType = BioReader.NORM_LINEAR;
		enum NormType //TODO: hack
		{
	    	NORM_NONE,
	    	NORM_LINEAR,
	    	NORM_GAUSSIAN;
		}
		
		enum NormStats
		{
			REGIONAL,
			GLOBAL
		}
		
		final String normTypeString[] = {BioReader.NORM_NONE,BioReader.NORM_LINEAR, BioReader.NORM_GAUSSIAN};
		
		JTextField		m_TextGFF;
		
		JList			m_ListWIG;
		JSpinner		m_SpinnerBins;
		JSpinner		m_SpinnerRegionSize;
		JComboBox		m_ComboNormType;
		JComboBox		m_ComboNormStats;
		
		public BioReaderPanel(MemoryTable intTab )
		{
			super();
			
			m_InternalTable 	= intTab;
			
			this.setBorder(	BorderFactory.createEmptyBorder(10, 10, 10, 10));
			this.setLayout(new BorderLayout(5,5));
			
			//================ GFF Options

			m_TextGFF = new JTextField("Not Loaded");
			m_TextGFF.setCaretPosition(m_TextGFF.getText().length());
			m_TextGFF.setEditable(false);
			JButton m_ButtonReadGFF = new JButton("Load GFF");
			m_ButtonReadGFF.addActionListener(this);
			m_ButtonReadGFF.setActionCommand("LOADGFF");
			
			JPanel panelGFF = new JPanel(new BorderLayout());
			panelGFF.add(m_TextGFF, BorderLayout.CENTER);
			panelGFF.add(m_ButtonReadGFF, BorderLayout.WEST);

			JPanel panelTop = new JPanel(new GridLayout(3, 1));
			panelTop.add(panelGFF);
			
			//================ Params

			m_SpinnerBins = new JSpinner(new SpinnerNumberModel(30, 1, 10000, 1));
			m_SpinnerBins.setPreferredSize(new Dimension(70, 20));
			JPanel panelBins = new JPanel(new BorderLayout());
			panelBins.add(new JLabel("  Bins:"), BorderLayout.WEST);
			panelBins.add(m_SpinnerBins, BorderLayout.CENTER);
			
			m_SpinnerRegionSize = new JSpinner(new SpinnerNumberModel(1000, 1, 100000, 1));
			m_SpinnerRegionSize.setPreferredSize(new Dimension(70, 20));
			JPanel panelRegionSize = new JPanel(new BorderLayout());
			JButton buttonResizeRegions = new JButton("resize");
			buttonResizeRegions.addActionListener(this);
			buttonResizeRegions.setActionCommand("RESIZE");

			panelRegionSize.add(new JLabel("  Region Size:"), BorderLayout.WEST);
			panelRegionSize.add(m_SpinnerRegionSize, BorderLayout.CENTER);
			panelRegionSize.add(buttonResizeRegions, BorderLayout.EAST);

			JPanel panelParams = new JPanel(new GridLayout(1,3));
			panelParams.add(panelBins);
			panelParams.add(panelRegionSize);
			panelTop.add(panelParams);

			m_ComboNormType  = EnumUtils.getComboBox(NormType.values(), NormType.NORM_LINEAR, "normType", this);
			JPanel panelNormType = new JPanel(new BorderLayout());
			panelNormType.add(new JLabel("  Normalization Method:"), BorderLayout.WEST);
			panelNormType.add(m_ComboNormType, BorderLayout.CENTER);

			m_ComboNormStats  = EnumUtils.getComboBox(NormStats.values(), NormStats.REGIONAL, "normStats", this);
			JPanel panelNormStats = new JPanel(new BorderLayout());
			panelNormStats.add(new JLabel("  Normalization Stats:"), BorderLayout.WEST);
			panelNormStats.add(m_ComboNormStats, BorderLayout.CENTER);

			JPanel panelParams2 = new JPanel(new GridLayout(1,3));
			panelParams2.add(panelNormStats);
			panelParams2.add(panelNormType);
			panelTop.add(panelParams2);
			
			//================ WIG List
			
			m_ListWIG = new JList(new DefaultListModel());
			
			JButton buttonAddWig = new JButton("Add WIG ...");
			buttonAddWig.addActionListener(this);
			buttonAddWig.setActionCommand("ADDWIG");
			
			JButton buttonRemove = new JButton("Remove Selected");
			buttonRemove.addActionListener(this);
			buttonRemove.setActionCommand("REMOVEWIG");

			JButton buttonProcess = new JButton("Process");
			buttonProcess.addActionListener(this);
			buttonProcess.setActionCommand("PROCESS");

			JButton buttonLoad = new JButton("Load Preprocessed");
			buttonLoad.addActionListener(this);
			buttonLoad.setActionCommand("LOADCSV");

			JButton buttonSave = new JButton("Save CSV");
			buttonSave.addActionListener(this);
			buttonSave.setActionCommand("SAVECSV");

			JPanel panelWIGButtons = new JPanel(new GridLayout(5,1));
			panelWIGButtons.add(buttonAddWig);
			panelWIGButtons.add(buttonRemove);
			panelWIGButtons.add(buttonProcess);
			panelWIGButtons.add(buttonLoad);
			panelWIGButtons.add(buttonSave);
			

			JPanel panelWIG = new JPanel(new BorderLayout());
			panelWIG.add( new JScrollPane(m_ListWIG), BorderLayout.CENTER);
			panelWIG.add( new JLabel("WIG Files:"), BorderLayout.NORTH);
			panelWIG.add(panelWIGButtons, BorderLayout.EAST);

			this.add(panelTop, BorderLayout.NORTH);
			this.add(panelWIG, BorderLayout.CENTER);
		}

		@Override
		public void actionPerformed(ActionEvent e)
		{
			if (e.getActionCommand().equals("LOADGFF"))
			{
				System.out.println("loading GFF started");
				loadGFF();
				System.out.println("loading GFF completed");
			}
			else if (e.getActionCommand().equals("RESIZE"))
			{
				int iRegionSize = (Integer)m_SpinnerRegionSize.getValue();
				if (iRegionSize != m_BioReader.getMaxRegionSize() || iRegionSize != m_BioReader.getMinRegionSize())
				{
					m_BioReader.resizeAllRegions(iRegionSize);
				}
			}
			else if (e.getActionCommand().equals("ADDWIG"))
			{
				JFileChooser fc = new JFileChooser();
				fc.setFileFilter(new FileNameExtensionFilter("Wiggle File(s) (.wig|.wig.gz|.wig.zip)", "wig","gz","zip"));
				fc.setMultiSelectionEnabled(true);
				if( fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION )
				{
					File[] files = fc.getSelectedFiles();
					for (int i = 0; i < files.length; i++)
					{
						((DefaultListModel)m_ListWIG.getModel()).addElement(files[i]);
					}
				}
			}
			else if (e.getActionCommand().equals("REMOVEWIG"))
			{
				int[] selectedIndices = m_ListWIG.getSelectedIndices();
				if (selectedIndices.length > 0)
				{
					for (int i = selectedIndices.length - 1; i >= 0; i--)
					{
						DefaultListModel model = (DefaultListModel)m_ListWIG.getModel();
						model.removeElementAt(selectedIndices[i]);
					}
				}
			}
			else if (e.getActionCommand().equals("PROCESS"))
			{
				processWigFiles();
			}
			else if (e.getActionCommand().equals("LOADCSV"))
			{
				Table newTable = TableFactory.csvLoader(this);
				if (newTable != null)
				{
					setInternalTable(newTable);
				}
			}
			else if( e.getActionCommand().equals("SAVECSV"))
			{
				JFileChooser fc = new JFileChooser( );
				int returnVal = fc.showSaveDialog(this);
				if( returnVal == JFileChooser.APPROVE_OPTION )
				{
					saveTableCSV(m_InternalTable, fc.getSelectedFile());
				}
			}
		}
		
		void loadGFF()
		{
			JFileChooser fc = new JFileChooser();
			fc.setFileFilter(new FileNameExtensionFilter("General Feature File (.gff)", "gff"));
			if( fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION )
			{
				try
				{
					m_BioReader = new BioReader();
					m_BioReader.enablePrintProgress(true);
					InputStream streamGFF = new FileInputStream(fc.getSelectedFile());
					m_BioReader.readGFF(streamGFF);
					streamGFF.close();
					m_TextGFF.setText(fc.getSelectedFile().getCanonicalPath());
					m_SpinnerBins.setModel(new SpinnerNumberModel(30, 1, 1000, 1));
					m_SpinnerRegionSize.setValue(new Integer(m_BioReader.getMaxRegionSize()));
				}
				catch (IOException ex)
				{
					m_BioReader = null;
					ex.printStackTrace();
				}
				catch (InterruptedException ex)
				{
					m_BioReader = null;
					ex.printStackTrace();
				}
			}
		}
		
		void processWigFiles()
		{
			if (m_BioReader == null)
			{
				JOptionPane.showMessageDialog(null, "ERROR: No GFF File has been loaded.");
				return;
			}
			
			int iNumBins = (Integer)m_SpinnerBins.getValue();
			//m_BioReader.setNumBins(iNumBins);

			DefaultListModel model = (DefaultListModel)m_ListWIG.getModel();
			int iRows = m_BioReader.getNumFeatures();
			
			int iNumMetaData = 0;
			double [][] table = new double[iRows][iNumBins * model.size() + iNumMetaData];
			
			String [] dataColNames = new String[iNumBins * model.size() + iNumMetaData];
			int iCurrCol = 0; 
			for (int iFileIndex = 0; iFileIndex < model.size(); iFileIndex++)
			{
				try
				{
					File f = (File)model.get(iFileIndex);
					String sFullName = f.getAbsolutePath();

					m_BioReader.readWIG(sFullName, iNumBins, true, false);
					DataStats normStats = NormStats.values()[m_ComboNormStats.getSelectedIndex()] == NormStats.REGIONAL ?
							m_BioReader.getRegionStats() :
							m_BioReader.getGlobalStats();
					double [][] tableW = BioReader.createNormalizedTable(m_BioReader.getDataTable(), normTypeString[m_ComboNormType.getSelectedIndex()], normStats);
					
					assert (tableW.length == iRows && tableW[0].length == iNumBins);
					
					// copy data to the main table
					for (int iR = 0; iR < iRows; iR++)
					{
						System.arraycopy(tableW[iR], 0, table[iR], iCurrCol, iNumBins);
					}

					String sTrackName = m_BioReader.getSampleName();
					if (sTrackName == null)
					{// use filename without the extension
						//sTrackName = sFullName.indexOf('.') > 0 ? sFullName.substring(0, sFullName.indexOf('.')) : sFullName;
						sTrackName = sFullName.lastIndexOf('/') > 0 ? sFullName.substring(sFullName.lastIndexOf('/')+1) : sFullName;
					}

					sTrackName = sTrackName.replace(' ','_'); // having space in the track name will cause problem for csv reader
					for (int d = 0; d < iNumBins; d++)
					{
						//int iOffset = (d - iNumBins / 2) * m_BioReader.getMaxNumBins() / iNumBins;
						//colNames[d + iCurrCol] = sTrackName + "\n" + Integer.toString(iOffset);
						dataColNames[d + iCurrCol] = sTrackName;// + (iOffset > 0 ? "+" : "") + Integer.toString(iOffset);
					}
				} catch (IOException ex) {
					ex.printStackTrace();
				} catch (InterruptedException ex) {
					ex.printStackTrace();
				}
				iCurrCol += iNumBins;
			}
			
			m_MemTable = createTable(table, dataColNames);

			if( m_MemTable != null )
			{
				m_MemTable.setInputControl(this);
			}
			setInternalTable(m_MemTable);
		}
		
		void setInternalTable(final Table table)
		{
			block = true;
			for( TableListener tl : m_InternalTable.getTableListeners() ) {
				
				if(tl instanceof Expression) {
					
					m_Exp = (Expression)tl;							
				}
			}
			
			SwingUtilities.invokeLater(new Runnable() {
		        public void run() {
		        	
		        	try {
		        		
		        		while( block ) {
		        			Thread.sleep(1000);
		        		}
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
					if (table != null)
					{
						m_Exp.setTable(table);
			        	m_InternalTable = table;
					}
		        }
		      });
			// inform the expression
			block = false;			
		}
	}
}
