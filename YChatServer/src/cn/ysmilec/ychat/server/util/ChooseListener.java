package cn.ysmilec.ychat.server.util;

import javax.swing.DefaultComboBoxModel;

public interface ChooseListener {
	public DefaultComboBoxModel<String> provideComboBoxModel();
	public void didFinishChooseUser(String choice);
}
