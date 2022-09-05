package xyz.ahmetflix.chattingclient;

public class ServerData {
    public String serverName;
    public String serverIP;

    public String populationInfo;

    public String serverMOTD;

    public long pingToServer;
    public String playerList;
    private String serverIcon;

    public ServerData(String name, String ip) {
        this.serverName = name;
        this.serverIP = ip;
    }

    public String getBase64EncodedIconData() {
        return this.serverIcon;
    }

    public void setBase64EncodedIconData(String icon) {
        this.serverIcon = icon;
    }


    public void copyFrom(ServerData serverDataIn) {
        this.serverIP = serverDataIn.serverIP;
        this.serverName = serverDataIn.serverName;
        this.serverIcon = serverDataIn.serverIcon;
    }
}
