package still.operators;

import java.awt.event.ActionEvent;
import java.io.Serializable;

import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import still.data.Map;
import still.data.Operator;
import still.data.Table;
import still.data.TableEvent;
import still.gui.OPAppletViewFrame;
import still.gui.OperatorView;
import still.gui.PParallelCoordPainter;

public class ParallelCoordOp extends Operator implements Serializable
{
	private static final long serialVersionUID = 2004860657228946047L;

	public ParallelCoordOp( Table newTable, boolean isActive, String paramString )
	{
		this( newTable, isActive );
	}
	
	public ParallelCoordOp( Table newTable, boolean isActive )
	{
		super( newTable );
		this.isActive = isActive;
		if( isActive )
		{
			this.updateMap();
			this.updateFunction();
			isLazy  		= true;
			setView( new ParallelCoordView( this ) );
		}
	}

	public static String getMenuName()
	{
		return "View:ParallelCoordinates";
	}

	public String toString() {
		
		return "[View:ParallelCoordinates]";
	}
	
	public String getSaveString( ) {
		
		return "";
	}
	
	public void activate()
	{
		this.isActive = true;
		this.updateMap();
		this.updateFunction();
		isLazy  		= true;
		setView( new ParallelCoordView( this ) );
	}
	
	@Override
	public void updateFunction()
	{
		function = new IdentityFunction( input );
	}
	

	@Override
	public void updateMap()
	{
		map 			= Map.generateDiagonalMap(input.columns());
	}

	public void tableChanged( TableEvent te ) 
	{
		super.tableChanged(te);
		((ParallelCoordView)getView()).buildGUI();
		//loadOperatorView();
		if( this.isActive() )
		{
			SwingUtilities.invokeLater(new Runnable()
			{
		        public void run()
		        {
		        	PParallelCoordPainter pPCPainter = ((PParallelCoordPainter)((OPAppletViewFrame)((ParallelCoordView)getView()).getViewFrame()).procApp); 
		    		pPCPainter.invokeRedraw(true);
		        }
			});
		}
	}

	public void loadOperatorView()
	{
		setView( new ParallelCoordView( this ) );
	}
	
	public class ParallelCoordView extends OperatorView implements ChangeListener, ListSelectionListener
	{
		private static final long serialVersionUID = -8211890687429249769L;

		PParallelCoordPainter m_ParallelCoordPainter = null;
		ParallelCoordOp m_Operator = null;
		
		public ParallelCoordView(Operator op)
		{
			super(op);
			m_Operator = (ParallelCoordOp) op;
			m_ParallelCoordPainter = new PParallelCoordPainter(op);
			vframe = new OPAppletViewFrame("E"+op.getExpressionNumber()+":"+op, m_ParallelCoordPainter );			
			vframe.addComponentListener(this);
			buildGUI();
		}
		
		void buildGUI()
		{
			removeAll();
		}
		
		public void actionPerformed(ActionEvent e)
		{
		}

		@Override
		public void stateChanged(ChangeEvent e)
		{
		}

		@Override
		public void valueChanged(ListSelectionEvent e)
		{
		}
	}
}
