package fr.crazymeal.flink.streaming.connectors.twitch.util;

public class FlinkConnectorConstants {
	
	public static final String BASE_URL_API_TOKEN = "https://api.twitch.tv/kraken/oauth2/token";
	
	public static final String BASE_URL_CLIENT_TOKEN = "https://api.twitch.tv/kraken/oauth2/authorize";
	public static final String CLIENT_REDIRECTION_URL = "http://localhost";
	public static final String TWITCH_IRC_URI = "irc.chat.twitch.tv";
	public static final int TWITCH_IRC_PORT = 6667;
	
	public static final String PONG_ANSWER = "PONG :tmi.twitch.tv \r\n";
	public static final String PING_REQUEST = "PING :tmi.twitch.tv";
	
	public static final String TWITCH_IRC_CONNECT_SUCCESS_CODE = "004";
	public static final String TWITCH_IRC_POST_LOGGED_CODE = "376";
	public static final String TWITCH_IRC_MEMBERSHIP_CAPABILITIES_ACK = ":tmi.twitch.tv CAP * ACK :twitch.tv/membership";
}
