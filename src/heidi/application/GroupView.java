package heidi.application;

import heidi.data.query.FrequencyQueryBinding;
import heidi.frequency.JHistogram;
import heidi.project.Dim;
import heidi.project.Group;
import heidi.project.Project;

import java.awt.Color;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.Border;

import prefuse.data.event.ExpressionListener;
import prefuse.data.expression.BooleanLiteral;
import prefuse.data.expression.Expression;
import prefuse.data.expression.Predicate;
import prefuse.util.ColorLib;

public class GroupView extends JPanel {

	private Group m_group;
	
	private static final long serialVersionUID = 5607840958491820162L;
	private static final Logger s_logger = Logger.getLogger(GroupView.class.getName());
	
	public GroupView(Group group) {
		super();
		setBorder(BorderFactory.createEmptyBorder());
		
		m_group = group;

		updateGroup();
	}
	
	void updateGroup() {
		
		// TODO optimize and only update what has changed
		removeAll();
		
		if (m_group.getDims() == null) {
			return;
		}
		
		// Filtering Headers
		JLabel filterLabel = new JLabel("Filters");
		JButton filterClear = new JButton("Clear");
		filterClear.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				startProgress();
				Dim[] dims = m_group.getProject().getDims();
				for (Dim dim : dims) {
					FrequencyQueryBinding filterQuery = dim.getFilterQuery();
					filterQuery.select("TRUE");
				}
				updateGroup();
				endProgress();
			}
		});
		
		// Highlighting Headers
		JLabel highlightLabel = new JLabel("Highlights");
		JButton highlightClear = new JButton("Clear");
		highlightClear.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				startProgress();
				Dim[] dims = m_group.getProject().getDims();
				for (Dim dim : dims) {
					FrequencyQueryBinding highlightQuery = dim.getHighlightQuery();
					highlightQuery.select("FALSE");
				}
				updateGroup();
				endProgress();
			}
		});
		
		// initialize layout of parent
		GridBagLayout layout = new GridBagLayout();
		setLayout(layout);
		
		// layout headers
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridx = 1;
		constraints.gridy = 0;
		constraints.weightx = 1.0;
		constraints.insets = new Insets(4, 4, 4, 4);
		constraints.anchor = GridBagConstraints.LINE_END;
		add(filterLabel, constraints);
		
		constraints.gridx = 2;
		constraints.anchor = GridBagConstraints.LINE_START;
		add(filterClear, constraints);
		
		constraints.gridx = 3;
		constraints.anchor = GridBagConstraints.LINE_END;
		add(highlightLabel, constraints);
		
		constraints.gridx = 4;
		constraints.anchor = GridBagConstraints.LINE_START;
		add(highlightClear, constraints);
		
		
		// create widgets for item selection to control filtering and highlighting
		Dim[] dims = m_group.getDims();
		for (int i = 0; i < dims.length; i++) {
			Dim dim = dims[i];
			JLabel label = new JLabel(dim.getName());
			JComponent filterControl;
			try {
				filterControl = createFilterWidget(dim);
			} catch (Exception e) {
				e.printStackTrace();
				s_logger.warning("Unable to create filter for dimension "+dim.getName());
				filterControl = new JLabel("Invalid");
			}
			JComponent highlightControl;
			try {
				highlightControl = createHighlightWidget(dim);
			} catch (Exception e) {
				e.printStackTrace();
				s_logger.warning("Unable to create highlight for dimension "+dim.getName());
				highlightControl = new JLabel("Invalid");
			}
			
			// layout item selection widgets
			constraints = new GridBagConstraints();
			constraints.gridx = 0;
			constraints.gridy = i + 1;
			constraints.anchor = GridBagConstraints.CENTER;
			add(label, constraints);
			
			constraints.gridx = 1;
			constraints.gridwidth = 2;
			constraints.insets = new Insets(4, 4, 4, 4);
			constraints.anchor = GridBagConstraints.CENTER;
			add(filterControl, constraints);
			
			constraints.gridx = 3;
			constraints.gridwidth = 2;
			constraints.anchor = GridBagConstraints.CENTER;
			add(highlightControl, constraints);
		}
		
		// Add a spacer at the end to force widgets into top left corner on resize
		JLabel spacer = new JLabel();
		constraints = new GridBagConstraints();
		constraints.gridx = 4;
		constraints.gridy = dims.length + 2;
		constraints.weighty = 1.0;
		add(spacer, constraints);
		
		revalidate();
	}
	
	private void setActiveAppearance(JComponent component) {
		Color activeColor = new Color(254, 241, 242);
		Color activeBorderColor = new Color(255, 0, 0);
		Border activeBorder = BorderFactory.createMatteBorder(1, 1, 1, 1, activeBorderColor);
		
		component.setBackground(activeColor);
		component.setBorder(activeBorder);
	}
	
	private void setInactiveAppearance(JComponent component) {
		Color inactiveColor = Color.white;
		Border inactiveBorder = BorderFactory.createMatteBorder(1, 1, 1, 1, inactiveColor);
		
		component.setBackground(inactiveColor);
		component.setBorder(inactiveBorder);
	}
	
	private JComponent createFilterWidget(final Dim dim) {

		Project project = dim.getProject();
		FrequencyQueryBinding query = dim.getFilterQuery();

		// Create histogram
		Predicate primaryFilter = BooleanLiteral.TRUE;
		Predicate secondaryFilter = project.getFilterPredicate();
		final JHistogram histogram = query.createHistogram(primaryFilter, secondaryFilter);

		// Set colours
		int highlight = ColorLib.setAlpha(Color.lightGray.getRGB(), 100);
		int[] primaryPalette = new int[] {
				ColorLib.setAlpha(Color.lightGray.getRGB(), 100),
		};
		int[] secondaryPalette;
		if (dim.equals(dim.getProject().getColorDimension())){
			secondaryPalette = project.getPalette();
		} else {
			secondaryPalette = new int[]{
				Color.gray.getRGB(),
			};
		}
		histogram.setColors(highlight, primaryPalette, secondaryPalette);
		
		Predicate filterPredicate = dim.getFilterQuery().getPredicate();
		if (filterPredicate.toString().equals("TRUE")) {
			setInactiveAppearance(histogram);
		} else {
			setActiveAppearance(histogram);
		}
		filterPredicate.addExpressionListener(new ExpressionListener() {
			@Override
			public void expressionChanged(Expression expr) {
				if (expr.toString().equals("TRUE")) {
					setInactiveAppearance(histogram);
				} else {
					setActiveAppearance(histogram);
				}
			}
		});
		
		JScrollPane scrollPane = new JScrollPane(histogram);
		Dimension size = new Dimension(305, 80);
		scrollPane.setMaximumSize(size);
		scrollPane.setPreferredSize(size);
		return scrollPane;
	}
	
	private JComponent createHighlightWidget(final Dim dim) {
		
		Project project = dim.getProject();
		FrequencyQueryBinding query = dim.getHighlightQuery();

		// Create histogram
		Predicate primary = project.getFilterPredicate();
		Predicate secondary = project.getHighlightPredicate();
		final JHistogram histogram = query.createHistogram(primary, secondary);
		
		// Set colours
		int highlight = project.getHighlightRGB();
		int[] primaryPalette = new int[] {
				ColorLib.setAlpha(Color.lightGray.getRGB(), 100),
		};
		int[] secondaryPalette = new int[] {
				ColorLib.setAlpha(highlight, 75),
		};
		histogram.setColors(highlight, primaryPalette, secondaryPalette);
		
		Predicate highlightPredicate = dim.getHighlightQuery().getPredicate();
		if (highlightPredicate.toString().equals("FALSE")) {
			setInactiveAppearance(histogram);
		} else {
			setActiveAppearance(histogram);
		}
		highlightPredicate.addExpressionListener(new ExpressionListener() {
			@Override
			public void expressionChanged(Expression expr) {
				if (expr.toString().equals("FALSE")) {
					setInactiveAppearance(histogram);
				} else {
					setActiveAppearance(histogram);
				}
			}
		});
		
		JScrollPane scrollPane = new JScrollPane(histogram);
		Dimension size = new Dimension(305, 80);
		scrollPane.setMaximumSize(size);
		scrollPane.setPreferredSize(size);
		return scrollPane;
	}
	
	private void startProgress() {
		Container contentPane = getParent();
		while (contentPane.getParent() != null) {
			contentPane = contentPane.getParent();
		}
		contentPane.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
	}
	
	private void endProgress() {
		Container contentPane = getParent();
		while (contentPane.getParent() != null) {
			contentPane = contentPane.getParent();
		}
		contentPane.setCursor(null);
	}
}
