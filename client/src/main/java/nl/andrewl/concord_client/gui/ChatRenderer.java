package nl.andrewl.concord_client.gui;

import com.googlecode.lanterna.TerminalTextUtils;
import com.googlecode.lanterna.graphics.ThemeDefinition;
import com.googlecode.lanterna.gui2.AbstractListBox;
import com.googlecode.lanterna.gui2.TextGUIGraphics;
import nl.andrewl.concord_core.msg.types.chat.Chat;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class ChatRenderer extends AbstractListBox.ListItemRenderer<Chat, ChatList> {
	@Override
	public void drawItem(TextGUIGraphics graphics, ChatList listBox, int index, Chat chat, boolean selected, boolean focused) {
		ThemeDefinition themeDefinition = listBox.getTheme().getDefinition(AbstractListBox.class);
		if(selected && focused) {
			graphics.applyThemeStyle(themeDefinition.getSelected());
		}
		else {
			graphics.applyThemeStyle(themeDefinition.getNormal());
		}
		graphics.putString(0, 0, chat.senderNickname());
		Instant timestamp = Instant.ofEpochMilli(chat.timestamp());
		String timeStr = timestamp.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm"));
		String label = chat.senderNickname() + "@" + timeStr + " : " + chat.message();
		label = TerminalTextUtils.fitString(label, graphics.getSize().getColumns());
		while(TerminalTextUtils.getColumnWidth(label) < graphics.getSize().getColumns()) {
			label += " ";
		}
		graphics.putString(0, 0, label);
	}
}
