package xyz.ahmetflix.chattingclient.frame;

import xyz.ahmetflix.chattingclient.Client;
import xyz.ahmetflix.chattingserver.ITickable;

import javax.swing.*;
import java.util.Vector;

public class UserListBox extends JList<String> implements ITickable {

    private static UserListBox INSTANCE;
    public static UserListBox getInstance() {
        return INSTANCE;
    }

    private Client client;
    private int tick;

    public UserListBox(Client var1) {
        INSTANCE = this;
        this.client = var1;
    }

    public void update() {
        if (this.tick++ % 20 == 0) {
            Vector<String> var1 = new Vector<>();

            /*for(int var2 = 0; var2 < this.client.().v().size(); ++var2) {
                var1.add(((EntityPlayer)this.client.getPlayerList().v().get(var2)).getName());
            }*/

            this.setListData(var1);
        }

    }
}