package heidi.application;

import heidi.plot.IDynamicUpdate;
import heidi.plot.ScatterPlot;
import heidi.plot.Splom;
import heidi.project.Dim;
import heidi.project.Plot;
import heidi.project.Project;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;

public class PlotView  extends JPanel {

	private Plot m_plot;
	
	private static final long serialVersionUID = -3149390110050199403L;

	public PlotView(Plot plot) {
		super();
		setBorder(BorderFactory.createEmptyBorder());
		
		m_plot = plot;
		updatePlot();
		
	}
	
	void updatePlot() {
		
		// TODO optimize and only update what has changed
		removeAll();
		
		
		IDynamicUpdate update = null;
		JComponent component = null;
		
		// TODO turns this into a pluggable factory of plots
		String type = m_plot.getType();
		if (Splom.getType().equals(type)) {
			if (m_plot.getDimensions() != null) {
				Splom splom = createSplom();
				update = splom;
				component = splom;
			}
		} else if (ScatterPlot.getType().equals(type)) {
			if (m_plot.getXDimension() != null && 
				m_plot.getYDimension() != null) {
				ScatterPlot scatter = createScatterPlot();
				update = scatter;
				component = scatter;
			}
		}
		
		// register for filter and highlight updates
		if (update != null) {
			Project project = m_plot.getGroup().getProject();
			update.setFilterPredicate(project.getFilterPredicate());
			update.setHighlightPredicate(project.getHighlightPredicate());
		}
		
		// layout
		GridBagLayout layout = new GridBagLayout();
		setLayout(layout);
		if (component != null) {
			GridBagConstraints constraints = new GridBagConstraints();
			constraints.weightx = 1.0;
			constraints.weighty = 1.0;
			constraints.fill = GridBagConstraints.BOTH;
			add(component, constraints);
		}
	}
	
	private ScatterPlot createScatterPlot() {
		Dim[] dims = m_plot.getDimensions();
		if (dims.length >= 2) {
			return new ScatterPlot(m_plot);
		}
		return null;
	}
	
	private Splom createSplom() {
		Dim[] dims = m_plot.getDimensions();
		if (dims.length >= 2) {
			return new Splom(m_plot);
		}
		return null;
	}
	
	@Override
	public String getName() {
		if (m_plot != null) {
			return m_plot.getName();
		}
		return "Unknown";
	}
}
