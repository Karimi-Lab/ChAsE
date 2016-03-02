package org.sfu.chase;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

abstract class History
{
	class HistoryItem
	{
		String m_ActionName;
		Object m_SnapShot;
	}

	protected ArrayList<HistoryItem> m_History = new ArrayList<HistoryItem>();
	protected int m_MaxHistoryItems = 10;
	protected int m_HistoryIndex = 0;

	abstract public void saveSnapShot(String actionName);
	abstract public void undo();
	abstract public void redo();
	
	protected void saveSnapShot(String actionName, Object snapShot)
	{
		HistoryItem newHistory = new HistoryItem();
		newHistory.m_ActionName = actionName;
		newHistory.m_SnapShot = snapShot;
		if (m_HistoryIndex < m_History.size()) 
		{
			for (int i = m_HistoryIndex; i < m_History.size(); i++) {
				m_History.remove(m_HistoryIndex);
			}
		}
		m_History.add(newHistory);
		while (m_History.size() > m_MaxHistoryItems)
			m_History.remove(0);
		m_HistoryIndex = m_History.size();		
		
		if (m_UpdateActionListener != null) {
			m_UpdateActionListener.actionPerformed(new ActionEvent(this, 0, "modified"));
		}
	}

	protected Object getUndoSnapShot()
	{
		if (m_HistoryIndex > 0) 
		{
			if (m_HistoryIndex == m_History.size()) {
				saveSnapShot(""); // to be able to redo
				m_HistoryIndex--;
			}
			m_HistoryIndex--;
			if (m_UpdateActionListener != null) {
				m_UpdateActionListener.actionPerformed(new ActionEvent(this, 0, "undo"));
			}
			return m_History.get(m_HistoryIndex).m_SnapShot;
		}
		return null;
	}

	protected Object getRedoSnapShot()
	{
		if (m_HistoryIndex < m_History.size() - 1) 
		{
			m_HistoryIndex++;
			if (m_UpdateActionListener != null) {
				m_UpdateActionListener.actionPerformed(new ActionEvent(this, 0, "redo"));
			}
			return m_History.get(m_HistoryIndex).m_SnapShot;
		}
		return null;
	}

	public boolean canUndo()
	{
		return (m_HistoryIndex > 0);
	}

	public boolean canRedo()
	{
		return (m_HistoryIndex < m_History.size() - 1);
	}

	public void resetHistory()
	{
		m_History.clear();
		m_HistoryIndex = 0;
	}
	
	public String getUndoText()
	{
		if (m_HistoryIndex > 0 && m_History.size() > 0)
		{
			return m_History.get(m_HistoryIndex-1).m_ActionName;
		}
		return "";
	}
	
	public String getRedoText()
	{
		if (m_HistoryIndex < m_History.size())
		{
			return m_History.get(m_HistoryIndex).m_ActionName;
		}
		return "";
	}
	
	ActionListener m_UpdateActionListener;
	public void setUpdateActionListener(ActionListener listener)
	{
		m_UpdateActionListener = listener;
	}
}