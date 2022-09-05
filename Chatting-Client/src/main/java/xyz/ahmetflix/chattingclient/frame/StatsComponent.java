package xyz.ahmetflix.chattingclient.frame;

import xyz.ahmetflix.chattingclient.Client;

import javax.swing.*;
import java.awt.*;
import java.text.DecimalFormat;

public class StatsComponent extends JComponent {
    private String stats = "";
    private final Client client;

    public StatsComponent(Client client) {
        this.client = client;
        this.setPreferredSize(new Dimension(150, 30));
        this.setMinimumSize(new Dimension(150, 30));
        this.setMaximumSize(new Dimension(150, 30));
        (new Timer(500, var1 -> StatsComponent.this.tick())).start();
        this.setBackground(Color.BLACK);
    }

    private void tick() {
        long usedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        System.gc();
        this.stats = "Memory use: " + usedMemory / 1024L / 1024L + " mb (" + Runtime.getRuntime().freeMemory() * 100L / Runtime.getRuntime().maxMemory() + "% free)";
        this.repaint();
    }

    public void paint(Graphics graphics) {
        graphics.setColor(Color.BLACK);

        graphics.drawString(this.stats, 32, 16);
    }
}
