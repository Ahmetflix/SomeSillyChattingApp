package xyz.ahmetflix.chattingclient;

import com.google.common.base.Objects;
import xyz.ahmetflix.chattingserver.connection.packet.impl.play.PacketPlayOutUserInfo;
import xyz.ahmetflix.chattingserver.user.UserProfile;

public class NetworkUserInfo {

    private final UserProfile userProfile;
    private int responseTime;

    public NetworkUserInfo(UserProfile userProfile)
    {
        this.userProfile = userProfile;
    }

    public NetworkUserInfo(PacketPlayOutUserInfo.UserInfoData data)
    {
        this.userProfile = data.getProfile();
        this.responseTime = data.getPing();
    }

    public UserProfile getUserProfile() {
        return userProfile;
    }

    public int getResponseTime() {
        return responseTime;
    }

    public void setResponseTime(int responseTime) {
        this.responseTime = responseTime;
    }
}