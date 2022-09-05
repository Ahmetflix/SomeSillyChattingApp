package xyz.ahmetflix.chattingclient.frame;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xyz.ahmetflix.chattingclient.Client;
import xyz.ahmetflix.chattingserver.connection.packet.impl.play.PacketPlayInChat;
import xyz.ahmetflix.chattingserver.connection.packet.impl.play.PacketPlayOutChat;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import java.awt.*;
import java.awt.event.*;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ClientFrame extends JComponent {

    private static final Font FONT = new Font("Monospaced", 0, 12);
    private static final Logger LOGGER = LogManager.getLogger();
    private Client client;

    public static void createFrame(final Client client) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        ClientFrame clientFrame = new ClientFrame(client);
        JFrame jFrame = new JFrame("Chatting App");
        jFrame.add(clientFrame);
        jFrame.pack();
        jFrame.setLocationRelativeTo((Component)null);
        jFrame.setVisible(true);
        jFrame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent var1) {
                client.shutdown();

                while(client.isRunning()) {
                    try {
                        Thread.sleep(100L);
                    } catch (InterruptedException ignored) {
                    }
                }

                System.exit(0);
            }
        });


    }

    public ClientFrame(Client client) {
        this.client = client;
        this.setPreferredSize(new Dimension(854, 480));
        this.setLayout(new BorderLayout());

        try {
            this.add(this.makeChat(), "Center");
            this.add(this.getWestPanel(), "West");
        } catch (Exception exception) {
            LOGGER.error("Couldn't build client frane", exception);
        }

    }

    private JComponent getWestPanel() throws Exception {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new StatsComponent(this.client), "North");
        panel.add(this.getUsersComponent(), "Center");
        panel.setBorder(new TitledBorder(new EtchedBorder(), "Stats"));
        return panel;
    }

    private JComponent getUsersComponent() throws Exception {
        UserListBox userListBox = new UserListBox(this.client);
        JScrollPane scrollPane = new JScrollPane(userListBox, 22, 30);
        scrollPane.setBorder(new TitledBorder(new EtchedBorder(), "Users"));
        return scrollPane;
    }

    public static Queue<String> chatMessages = new ConcurrentLinkedQueue<>();

    private JComponent makeChat() throws Exception {
        JPanel panel = new JPanel(new BorderLayout());
        final JTextArea textArea = new JTextArea();
        final JScrollPane scrollPane = new JScrollPane(textArea, 22, 30);
        textArea.setEditable(false);
        textArea.setFont(FONT);
        final JTextField textField = new JTextField();
        textField.addActionListener(var1 -> {
            String chatText = textField.getText().trim();
            if (chatText.length() > 0) {
                this.client.user.connection.sendPacket(new PacketPlayInChat(chatText));
            }

            textField.setText("");
        });
        panel.add(scrollPane, "Center");
        panel.add(textField, "South");
        panel.setBorder(new TitledBorder(new EtchedBorder(), "Chat"));
        Thread thread = new Thread(() -> {
            while (true) {
                String msg = null;
                if ((msg = chatMessages.poll()) != null) {
                    this.addText(textArea, scrollPane, msg + "\n");
                }
            }
        });
        thread.setDaemon(true);
        thread.start();
        return panel;
    }

    public void addText(final JTextArea textArea, final JScrollPane scrollPane, final String text) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> ClientFrame.this.addText(textArea, scrollPane, text));
        } else {
            Document document = textArea.getDocument();
            JScrollBar verticalScrollBar = scrollPane.getVerticalScrollBar();
            boolean shouldScroll = false;
            if (scrollPane.getViewport().getView() == textArea) {
                shouldScroll = verticalScrollBar.getValue() + verticalScrollBar.getSize().getHeight() + (FONT.getSize() * 4) > verticalScrollBar.getMaximum();
            }

            try {
                document.insertString(document.getLength(), text, null);
            } catch (BadLocationException ignored) {
            }

            if (shouldScroll) {
                verticalScrollBar.setValue(Integer.MAX_VALUE);
            }

        }
    }

}
