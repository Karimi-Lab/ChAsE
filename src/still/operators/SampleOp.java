package still.operators;

import java.awt.event.ActionEvent;

import still.data.Operator;
import still.data.Table;
import still.gui.PSamplePainter;

public class SampleOp extends BasicOp
{
	private static final long serialVersionUID = 5118434778904392340L;

	public SampleOp( Table newTable, boolean isActive, String paramString )
	{
		this( newTable, isActive );
	}
	
	public SampleOp( Table newTable, boolean isActive )
	{
		super(newTable, isActive);
		loadOperatorView();
	}

	public static String getMenuName()
	{
		return "View:SampleFunc";
	}

	public String toString()
	{
		return "[View:Sample]";
	}
	
	public String getSaveString( ) {
		
		return "";
	}
	
	public void activate()
	{
		this.isActive = true;
		this.updateMap();
		function = new BasicFunction(this);
		isLazy  		= true;
		setView( new SampleView( this ) );
	}

	@Override
	protected void computeNewColumns()
	{
		//TODO: create/recompute the operator output here
		m_NewColumns = null;
	}

	@Override
	public void loadOperatorView()
	{
		// comment out if no view
		setView( new SampleView( this ) );
	}
	
	public class SampleView extends BasicOp.BasicView
	{
		private static final long serialVersionUID = 4698263995759097051L;
		
		public SampleView(Operator op)
		{
			super(op);
			init();
		}
		
		@Override
		protected void createPainter(Operator op)
		{
			m_Painter = new PSamplePainter(op);
		}
		
		@Override
		protected void buildGUI()
		{
			this.removeAll();
			
			//TODO: add operator GUI controls
		}
		
		@Override
		public void actionPerformed(ActionEvent e)
		{
			//TODO: code to handle the GUI actions
		}
	}
}
