package heidi.application;

import heidi.project.PaletteMgr;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.Vector;

import javax.swing.DefaultComboBoxModel;
import javax.swing.Icon;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

public class ColorComboBox extends JComboBox {
	
	private Vector<int[]>    m_palettes;
	private ColorIcon[]      m_icons;
	private ComboBoxRenderer m_renderer;
	
	private static final long serialVersionUID = -3042866504837892243L;
	
	public ColorComboBox() {
		this(3);
	}
	
	public ColorComboBox(int colorCount) {
		super();
		
		m_renderer = new ComboBoxRenderer();
		setRenderer(m_renderer);
		
		setColorCount(colorCount);
		
		addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				ColorComboBox c = (ColorComboBox)e.getSource();
				// leave space for arrow button and trim
				int width = c.getWidth()- c.getHeight() - 4;
				if (m_icons != null) {
					for (int i = 0; i< m_icons.length; i++) {
						m_icons[i].setIconWidth(width);
					}
				}
			}
		});
	}
	
	public void setColorCount(int colorCount) {

		m_palettes = PaletteMgr.GetInstance().getPalettes(colorCount);
		
		// create images for palettes
		m_icons = new ColorIcon[m_palettes.size()];
		for (int i = 0; i <m_palettes.size(); i++) {
			m_icons[i] = createIcon(m_palettes.get(i));
		}
		m_renderer.setIcons(m_icons);
		
		// fix the height of the render to match the icon size
		m_renderer.setPreferredSize(new Dimension(m_icons[0].getIconWidth() + 4, m_icons[0].getIconHeight() + 4));
		// fix the height of the combo box to be the same as image height
		setMinimumSize(new Dimension(-1, m_icons[0].getIconHeight() + 8));
		setMaximumSize(new Dimension(-1, m_icons[0].getIconHeight() + 8));
		
		// set integer index as model
		Integer[] items = new Integer[m_palettes.size()];
		for (int i = 0; i < items.length; i++) {
			items[i] = new Integer(i);
		}
		DefaultComboBoxModel model = new DefaultComboBoxModel(items);
		setModel(model);
	}
	
	public int[] getPalette(int index) {
		return m_palettes.get(index).clone();
	}
	
	public int indexOf(int[] palette) {
		
		for(int[] p : m_palettes) {
			if (p.length == palette.length) {
				boolean match = true;
				int i = 0;
				while (match && i < p.length) {
					match = (p[i] == palette[i]);
					i++;
				}
				if (match) {
					return m_palettes.indexOf(p);
				}
			}
		}
		return -1;
	}
	
	private ColorIcon createIcon(int[] palette) {
		return new ColorIcon(palette);
	}
	
	class ComboBoxRenderer extends JLabel implements ListCellRenderer {

		private Icon[] m_icons;
		private static final long serialVersionUID = 6221380166251082629L;

		public ComboBoxRenderer() {
			super();
		}
		
		public void setIcons(Icon[] icons) {
			m_icons = icons;
		}

		/*
		 * (non-Javadoc)
		 * @see javax.swing.ListCellRenderer#getListCellRendererComponent(javax.swing.JList, java.lang.Object, int, boolean, boolean)
		 */
		public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {

			if (isSelected) {
				setBackground(list.getSelectionBackground());
				setForeground(list.getSelectionForeground());
			} else {
				setBackground(list.getBackground());
				setForeground(list.getForeground());
			}

			//Set the icon for selected item
			if (m_icons != null) {
				int selectedIndex = ((Integer)value).intValue();
				setIcon(m_icons[selectedIndex]);
			}
			
			return this;
		}
	}
	
	class ColorIcon implements Icon {
	    
		private int[] m_palette;
	    private int   m_width = 200;
	    private int   m_height = 15;
	    
	    ColorIcon(int[] palette){
	    	super();
	    	this.m_palette = palette;
	    }
	    
	    public void paintIcon(Component c, Graphics g, int x, int y) {
	        Graphics2D g2d = (Graphics2D) g.create();
	        
	        int barX = 0;
	        int barWidth = m_width / m_palette.length;
	        
	        for (int i = 0; i < m_palette.length; i++) {
	        	g2d.setColor(new Color(m_palette[i]));
	        	g2d.fillRect(barX, y, barWidth, m_height);
	        	barX += barWidth;
	        }
	        
	        g2d.dispose();
	    }
	    
	    @Override
	    public int getIconWidth() {
	        return m_width;
	    }
	    
	    public void setIconWidth(int width) {
	        m_width = width;
	    }
	    
	    @Override
	    public int getIconHeight() {
	        return m_height;
	    }
	    
	    public void setIconHeight(int height) {
	        m_height = height;
	    }
	}


}
