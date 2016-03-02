package org.sfu.chase.gui;

import org.sfu.chase.core.ClustFramework.SortCriteria;
import org.sfu.chase.util.Utils;

import processing.core.PApplet;
import processing.core.PFont;
import processing.core.PImage;

public class DisplayParams
{
	public enum PlotType
	{
		MEAN_STDDEV,
		QUANTILE,
		LOG_HIST,
		LOG_PEAK,
		AVG_HIST,
		PEAK_HIST,
		AVG_AND_PEAK,
		AVG_VS_PEAK,
		MEAN
		//HIST,
		//PEAK
	}

	public float plotW = 40; // size of cluster plots
	public float plotH = plotW; // height of cluster plots
	public float plotSizeMin = 20;
	public float plotSizeMax = 100;
	public float checkBoxSize = 11;
	public float plotGapX  = checkBoxSize * 1.5f; // horizontal gap between cluster plots
	public float plotGapY = 15; // vertical gap between clusters
	public float clustCaptionH = 120; // height of the cluster labels.
	public float clustOffsetX = 0; // x offset (controlled by horizontal scrollbar) 
	public float clustOffsetY = 0; // y offset (controlled by vertical scrollbar)
	public int	 groupLabelFontSize = 10;
	public int 	 groupLabelColor = 64;

	public Utils.Rect  viewRect; // total view rectangle
	//public Utils.Rect  toolbarRect;  // toolbar rectangle
	public Utils.Rect  treeRect; // tree view rectangle
	public Utils.Rect  clustRect; // cluster plots rectangle
	public Utils.Rect  hmRect;    // heatmap view rectangle
	public Utils.Rect  combineRect;
	public Utils.Rect  favoriteRect;
	//public Utils.Rect  hmSideRect; // heatmap sidebar rectangle

	public PlotType plotType = PlotType.MEAN_STDDEV;
	public float treeStrokeWidth = 2.0f;
	public float treeGapX        = 30;
	public float treeChildRadius = 4;
	public float treeParentRadius = 6;
	
	public boolean drawInvisibleGroups = false;
	public boolean drawExpandedParent  = false;
	
	
	public float treeWidth = 100; // required tree width. calculated based on the depth of the tree
	public float summaryLeft = 0; // left coordinate for the summary plots
	
	public int colorPlotMean;
	public int colorPlotStdDev;
	public int colorPlotMedian = 0xFF880000;
	public int [] colorQuantiles  = {0, 0xFFFFD5D5, 0xFFFFAAAA, 0xFFFFAAAA, 0xFFFFD5D5};

	public int colorClustSelect;
	public int colorTree;
	public int colorTreeSelect;
	public int colorPlotBG;
	public int colorWindowBG;
	
	public int regionTitleColor = 160; 
	public int regionTitleFontSize = 16;
	
	public boolean drawSummaryPlots = true;
	public boolean drawCheckBoxes = false;
	
	public int	 panelTitleFontSize = 20;
	public float panelTitleX = 5;
	public float panelTitleY = 5;
	public int   panelTitleColor = 0;
	public PFont fontPanelTitle;

	// heatmap display parameters
	public boolean hmForceUpdate = true;
	public float   hmScrollYOffset = 0;
	public float   hmScrollYRange  = 1;
	public float   hmOffsetX = 0; // x offset, modified by horizontal scrollbar
	public PImage  hmGx = null; // caches rendered heatmap (performance)
	public float   hmLegendH = 80; // the height of the region on the heatmap bottom to show the legends
	public float   hmW = 20; // width of the heatmap for each mark
	public float   hmGapX = 2; // gap between heatmaps for each mark
	public int     hmTopVisibleRow  = -1;  //top most visible row 
	public int     hmNumVisibleRows = -1; //number of visible rows
	public boolean hmLogScale = false; // draw the heatmap in logarithmic scale
	public int	   hmLegendFontSize = 13;
	public boolean hmShowLegend = true;
	
	public int     hmCurrWidth;
	public int     hmCurrHeight;
	
	public int 			hmSortGroup = 0;
	public SortCriteria hmSortCriteria = SortCriteria.INPUT_ORDER;
	public boolean 		hmSortAscending = true;
	
	public int hmColor0 = 0xFFFFFFFF;
	public int hmColor1 = 0xFF1D91C0;
	
	public float hmThreshold = 0;
	
	public PFont fontGuiFx;
	
	public DisplayParams(PApplet theApplet)
	{
		try {
			fontGuiFx        = theApplet.createFont("resources/Guifx.ttf", 20, true);
			fontPanelTitle   = theApplet.createFont("Helvetica",20, true);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		colorPlotMean    = theApplet.color(0, 0, 150, 150);
		colorPlotStdDev  = theApplet.color(255, 200, 160);

		colorClustSelect = theApplet.color(64, 64, 64);
		colorTree        = 0xFFAAAAAA;
		colorTreeSelect  = theApplet.color(128, 0, 255);
		colorPlotBG		 = theApplet.color(255, 255, 255);
		colorWindowBG    = theApplet.color(255, 255, 255);
	}
}
