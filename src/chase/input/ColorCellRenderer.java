package chase.input;

import java.awt.Color;
import java.awt.Component;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.border.Border;

import chase.gui.ColorPalette;

/**
 * Implements a renderer for the color selection drop down
 */
class ColorCellRenderer extends JLabel implements ListCellRenderer {
    private static final long serialVersionUID = 1L;

    protected DefaultListCellRenderer defaultRenderer = new DefaultListCellRenderer();

    Border unselectedBorder = null;
    Border selectedBorder = null;
    boolean isBordered = true;
    Color  m_Color[];

    public ColorCellRenderer() {
        this.isBordered = true;
        setOpaque(true); //MUST do this for background to show up.
    }

    public Component getListCellRendererComponent(JList list, Object value,
            int index, boolean isSelected, boolean cellHasFocus) {
        String colorStr = (String)value;
        int colorIndex = ColorPalette.COLOR_LIST.indexOf(colorStr);
        colorIndex = colorIndex < 0 ? 0 : colorIndex;
        m_Color = new Color[3];
        m_Color[0] = new Color(ColorPalette.COLOR_MIN[colorIndex], false);
        m_Color[1] = new Color(ColorPalette.COLOR_MED[colorIndex], false);
        m_Color[2] = new Color(ColorPalette.COLOR_MAX[colorIndex], false);
        Color newColor = m_Color[2];
        setBackground(newColor);
        setText(" ");
        
        if (isBordered) {
            if (isSelected) {
                if (selectedBorder == null) {
                    selectedBorder = BorderFactory.createMatteBorder(2,5,2,5,
                            Color.BLACK);
                }
                setBorder(selectedBorder);
            } else {
                if (unselectedBorder == null) {
                    unselectedBorder = BorderFactory.createMatteBorder(2,5,2,5,
                            Color.BLUE);
                }
                setBorder(unselectedBorder);
            }
        }
        
        setToolTipText(colorStr);
                //"RGB value: " + newColor.getRed() + ", " + newColor.getGreen() + ", " + newColor.getBlue());
        return this;

        /*
        JLabel renderer = (JLabel) defaultRenderer
                .getListCellRendererComponent(list, value, index,
                        isSelected, cellHasFocus);

        String colorStr = (String) value;
        int colorIndex = ColorPalette.COLOR_LIST.indexOf(colorStr);
        colorIndex = colorIndex < 0 ? 0 : colorIndex;
        renderer.setBackground(new Color(
                ColorPalette.COLOR_MAX[colorIndex], false));
        renderer.setBorder(BorderFactory.createMatteBorder(2, 2, 2, 2,
                isSelected ? Color.BLACK : Color.white));
        renderer.setText(" ");
        return renderer;
        */
    }
    
    public void paint(Graphics g)
    {
        Graphics2D g2d = (Graphics2D)g;
        int iwidth = getWidth()-3;
        int iheight = getHeight()-3;
        
        GradientPaint gradient = new GradientPaint(0, 0, m_Color[0], iwidth/2, 0, m_Color[1], false);
        g2d.setPaint(gradient);
        g2d.fillRect(1, 0, iwidth/2+1, iheight);
        
        gradient = new GradientPaint(iwidth/2, 0, m_Color[1], iwidth, 0, m_Color[2], false);
        g2d.setPaint(gradient);
        g2d.fillRect(iwidth/2+1, 0, iwidth/2+1, iheight);

        g2d.setColor(Color.black);
        g2d.drawRect(1, 0, iwidth, iheight);
    }
}
