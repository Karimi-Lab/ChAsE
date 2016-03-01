package still.gui.metadataviewer;

import javax.swing.JPanel;
import processing.core.PApplet;
import processing.core.PGraphics;
import still.data.Operator;

public interface MetadataViewer 
{
	/**
	 * Returns the name of the MetaDataViewer.
	 * @return String name of the MetaDataViewer sub class.
	 */
	public String getName();
	
	/**
	 * Preprocesses the data in op and returns the index of the metadata column if any.
	 * @param op  input operator containing the data.
	 * @param ap  applet used as the container for viewing data.
	 * @return index of the metadata column if any. Otherwise -1.
	 */
	public int processData(Operator op, PApplet ap);
	
	/**
	 * Draws a thumbnail view of point using the content of a metadata.
	 * @param pg   		pointer to the PGraphics object used as the canvas.
	 * @param idx  		index of the point in the table.
	 * @param left  	left coordinate of the thumbnail drawable rect.
	 * @param top   	top coordinate of the thumbnail drawable rect.
	 * @param width 	width of the thumbnail drawable rect.
	 * @param heigth 	height of the thumbnail drawable rect.
	 */
	public void drawPoint(PGraphics pg, int idx, float left, float top, float width, float height);
	
	/**
	 * Draws a thumbnail view of point using the content of a metadata.
	 * @param pg   		pointer to the PGraphics object used as the canvas.
	 * @param idx  		index of the point in the table.
	 * @param left  	left coordinate of the detailed drawable rect.
	 * @param top   	top coordinate of the detailed drawable rect.
	 * @param width 	width of the detailed drawable rect.
	 * @param heigth 	height of the detailed drawable rect.
	 */
	public void drawPointDetailed(PGraphics pg, int idx, float left, float top, float width, float height);

	/**
	 * Builds the GUI used to control the metadata viewer parameters.
	 * @param parent 	Parent panel.
	 */
	public void buildGUI(JPanel parent);
}
