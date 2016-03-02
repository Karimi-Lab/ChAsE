package org.sfu.chase;

public class FrameworkHistory extends History
{
	ClustFramework m_Framework;
	
	FrameworkHistory(ClustFramework framework)
	{
		m_Framework = framework;
	}

	@Override
	public void saveSnapShot(String actionName) {
		SerialClustFramework snapShot = new SerialClustFramework();
		snapShot.copyFrom(m_Framework);
		saveSnapShot(actionName, snapShot);
	}

	@Override
	public void undo() {
		SerialClustFramework snapShot = (SerialClustFramework)getUndoSnapShot();
		if (snapShot != null) {
			snapShot.copyTo(m_Framework);
		}
	}

	@Override
	public void redo() {
		SerialClustFramework snapShot = (SerialClustFramework)getRedoSnapShot();
		if (snapShot != null) {
			snapShot.copyTo(m_Framework);
		}
	}
}
