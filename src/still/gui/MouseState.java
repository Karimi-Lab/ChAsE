package still.gui;

public class MouseState// implements Cloneable
{
	public static final int LEFT 	= 0;
	public static final int RIGHT 	= 1;
	public static final int PRESSED  = 0;
	public static final int RELEASED = 1;
	public static final int DRAGGING = 2;
	
	private int x      = -1; // current mouse x
	private int y 	   = -1; // current mouse y
	private int px     = -1; // previous mouse x
	private int py 	   = -1; // previous mouse y
	private int startX = -1; // drag start x
	private int startY = -1; // drag start y
	private int endX   = -1; // drag end x
	private int endY   = -1; // drag end y
	private int button = -1; // mouse button pressed
	private int state  = -1; // mouse state
	private int	clickCount = 0;
	private boolean paused = false;
	
	/*
	public Object clone() {
		try {
			return super.clone();
		} catch(Exception e) {
			return null; 
		}
	}
	*/
	
	public void copy(MouseState m)
	{
		x = m.x;
		y = m.y;
		px = m.px;
		py = m.py;
		startX = m.startX;
		startY = m.startY;
		endX = m.endX;
		endY = m.endY;
		button = m.button;
		state = m.state;
		clickCount = m.clickCount;
	}
	
	public void reset()
	{
		state = -1;
		clickCount = 0;
	}
	
	public int x() {return !paused ? x : -1;}
	public int y() {return !paused ? y : -1;}
	public int px() {return !paused ? px : -1;}
	public int py() {return !paused ? py : -1;}
	public int startX() {return !paused ? startX : -1;}
	public int startY() {return !paused ? startY : -1;}
	public int endX() {return !paused ? endX : -1;}
	public int endY() {return !paused ? endY : -1;}
	public int button() {return !paused ? button : -1;}
	public int state() {return !paused ? state : -1;}

	public void setX(int _x) {x = _x;}
	public void setY(int _y) {y = _y;}
	public void setPX(int _px) {px = _px;}
	public void setPY(int _py) {py = _py;}
	public void setStartX(int _startX) {startX = _startX;}
	public void setStartY(int _startY) {startY = _startY;}
	public void setEndX(int _endX) {endX = _endX;}
	public void setEndY(int _endY) {endY = _endY;}
	public void setButton(int _button) {button = _button;}
	public void setState(int _state) {state = _state;}
	public void setClickCount(int count) {clickCount = count;}
	public void setPaused(boolean _paused) {paused = _paused;}

	public boolean isLeftButton() 
	{
		return !paused ? button == LEFT : false;
	}
	
	public boolean isRightButton()
	{
		return !paused ? button == RIGHT : false;
	}
	
	public boolean isPressed()
	{
		return !paused ? state == PRESSED : false;
	}
	
	public boolean isReleased()
	{
		return !paused ? state == RELEASED : false;
	}
	
	public boolean isDragging()
	{
		return !paused ? state == DRAGGING : false;
	}
	
	public boolean isClicked()
	{
		return !paused ? clickCount > 0 : false;
	}
	
	public boolean isPressed(int button)
	{
		return !paused ? (state == PRESSED && button == this.button) : false;
	}
	
	public boolean isReleased(int button)
	{
		return !paused ? (state == RELEASED && button == this.button) : false;
	}
	
	public boolean isDragging(int button)
	{
		return !paused ? (state == DRAGGING && button == this.button) : false;
	}
	
	public boolean isClicked(int button)
	{
		return !paused ? (clickCount > 0 && button == this.button) : false;
	}

	public int getClickCount()
	{
		return !paused ? clickCount : 0;
	}
	
	public boolean isIn(double _x, double _y, double _w, double _h)
	{
		return !paused ? (x >= Math.min(_x, _x + _w) && x <= Math.max(_x, _x + _w) &&
						  y >= Math.min(_y, _y + _h) && y <= Math.max(_y, _y + _h)) : false;
	}

	public boolean isStartIn(double _x, double _y, double _w, double _h)
	{
		return !paused ? (startX >= Math.min(_x, _x + _w) && startX <= Math.max(_x, _x + _w) &&
				          startY >= Math.min(_y, _y + _h) && startY <= Math.max(_y, _y + _h)) : false;
	}
	
	public boolean isEndIn(double _x, double _y, double _w, double _h)
	{
		return !paused ? (endX >= Math.min(_x, _x + _w) && endX <= Math.max(_x, _x + _w) &&
						  endY >= Math.min(_y, _y + _h) && endY <= Math.max(_y, _y + _h)) : false;
	}
	
	public boolean clickedInside(int button, double _x, double _y, double _w, double _h)
	{
		return state == RELEASED && button == this.button && isStartIn(_x, _y, _w, _h) && isEndIn(_x, _y, _w, _h); 
	}
};
