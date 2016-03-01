package still.gui.metadataviewer;

/* TODO
 * buildGUI
 * mouse events to splom viewer
 * rotate axis labels by 90
 * add legend to the pearson colors
 * axis labels for mouse position (crossair)
 * separate to a separate class
 * load splom viewers by a factory
 * zoom image on mouse over
 * GUI: aspect ratio, path, alpha, background
 * clustering: check for numerical columns only
 */

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;

import processing.core.PApplet;
import processing.core.PFont;
import processing.core.PGraphics;
import processing.core.PImage;
import still.data.Operator;
import still.data.Table.ColType;

public class MetadataImageViewer implements MetadataViewer, ActionListener {
	
	PImage [] 	m_Images = null;
	PImage		m_DetailedImage = null;
	String   	m_sFilePath = "/Users/hyounesy/SFU/research/Dg61/images256jpg/";
	int 		THUMBNAIL_RESOLUTION = 32;
	Operator	m_Operator = null;
	PApplet		m_Applet   	= null;
	
	int			m_iMetaDataColIndex   = -1; // index of the metadata column
	int			m_iDetailedPointIndex = -1; // index of the last point drawn with detailed view
	String		s_DetailedPointMetaData = null; // string metadata of the last point drawn with detailed view
	
	public String getName()
	{
		return "Image Viewer";
	}

	public MetadataImageViewer()
	{
	}
	
	public int processData(Operator op, PApplet ap)
	{
		m_Operator = op;
		m_Applet   = ap;
		m_DetailedImage = null;
		m_iDetailedPointIndex = -1;
		s_DetailedPointMetaData = null;
		
		// check to see if there is an update to the filename columns
		for( int i : op.getDimTypeCols(ColType.METADATA) )
		{
			if( op.getColName(i).equalsIgnoreCase("filename") )
			{
				m_iMetaDataColIndex = i;
				String[] sFilenames = op.getMetaData(i);
				m_Images = new PImage[sFilenames.length];
				for (int k = 0; k < sFilenames.length; k++)
				{
					m_Images[k] = null;
					try
					{
						if ((new File(m_sFilePath + sFilenames[k])).exists())
						{
							m_Images[k] = ap.loadImage(m_sFilePath + sFilenames[k]);
							m_Images[k].resize(THUMBNAIL_RESOLUTION, THUMBNAIL_RESOLUTION);
						}
					}
					catch (Exception e)
					{
					}
				}
				return m_iMetaDataColIndex;
			}
		}
		return -1;
	}
	
	public void drawPoint(PGraphics pg, int idx, float left, float top, float width, float height)
	{
		if (m_Images[idx] != null)
		{
			try
			{
	 			pg.imageMode(PApplet.CORNER);
				pg.image(m_Images[idx], left, top, width, height);
			}
			catch (Exception e)
			{
			}
		}
		else
		{
			// just draw an empty rect with a cross
			pg.stroke(0);
			pg.rect(left, top, width, height);
			pg.line(left, top, left + width, top + height);
			pg.line(left + width, top, left, top + height);
		}
	}
	
	public void drawPointDetailed(PGraphics pg, int idx, float left, float top, float width, float height)
	{
		if (m_iMetaDataColIndex == -1)
			return;
		
		if (m_iDetailedPointIndex != idx)
		{
			m_DetailedImage = null;
			s_DetailedPointMetaData = m_Operator.getMetaData(m_iMetaDataColIndex, idx);
			try
			{
				if ((new File(m_sFilePath + s_DetailedPointMetaData)).exists())
				{
					m_DetailedImage = m_Applet.loadImage(m_sFilePath + s_DetailedPointMetaData);
				}
			}
			catch (Exception e)
			{
			}
			m_iDetailedPointIndex = idx;
		}
		
		if (m_DetailedImage != null)
		{
 			pg.imageMode(PApplet.CORNER);
			pg.image(m_DetailedImage, left, top, width, height);
		}
		else
		{
			// just draw an empty rect with a cross
			pg.stroke(128);
			pg.rect(left, top, width, height);
			pg.line(left, top, left + width, top + height);
			pg.line(left + width, top, left, top + height);
		}
	}

	
	JPanel m_MetaDataPanel = null;
	public void buildGUI(JPanel parent)
	{
		parent.removeAll();
		parent.setLayout(new GridLayout(5, 1));
		JPanel pathPanel = new JPanel();
		parent.add(pathPanel);
		JButton btnChoosePath = new JButton("Path");
		btnChoosePath.setActionCommand("SelectPath");
		btnChoosePath.addActionListener(this);
		pathPanel.add(btnChoosePath, BorderLayout.EAST);
		m_MetaDataPanel = parent;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub
		if (e.getActionCommand() == "SelectPath")
		{
			JFileChooser fc = new JFileChooser(m_sFilePath);
			fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			if( fc.showOpenDialog(m_MetaDataPanel) == JFileChooser.APPROVE_OPTION )
			{
				try
				{
					m_sFilePath = fc.getSelectedFile().getCanonicalPath();
				}
				catch(Exception ex){}
			}
		}
		
	}
}
