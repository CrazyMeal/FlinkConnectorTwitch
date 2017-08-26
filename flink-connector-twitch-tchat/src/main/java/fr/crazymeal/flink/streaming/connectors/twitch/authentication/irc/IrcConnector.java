package fr.crazymeal.flink.streaming.connectors.twitch.authentication.irc;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Properties;

import org.jets3t.service.security.OAuth2Tokens;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.crazymeal.flink.streaming.connectors.twitch.util.FlinkConnectorConstants;

public class IrcConnector {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(IrcConnector.class);
	
	private OAuth2Tokens oauthToken;
	
	private Socket socket;
	private BufferedReader reader;
	private BufferedWriter writer;
	
	public boolean initializeReaderWriter(String twitchUsername, String channelToCOnnectTo) throws UnknownHostException, IOException {
		OAuth2Tokens oauthToken = this.initializeAuthToken();
		
		if (oauthToken == null) {
			LOGGER.error("Authentication can't be made, access token isn't initialized");
			return false;
		} else {
			LOGGER.info("Authentication token was made successfully");
				
			this.socket = new Socket(FlinkConnectorConstants.TWITCH_IRC_URI, FlinkConnectorConstants.TWITCH_IRC_PORT);
			this.socket.setSoTimeout(3000000);
			LOGGER.debug("Socket was made");
			
			InputStreamReader inputStreamReader = new InputStreamReader(this.socket.getInputStream());
			this.reader = new BufferedReader(inputStreamReader);
			
			OutputStreamWriter outputStreamWriter = new OutputStreamWriter(this.socket.getOutputStream());
			this.writer = new BufferedWriter(outputStreamWriter);
			
			String oauthString = "PASS oauth:" + oauthToken.getAccessToken() + " \r\n";
			String nicknameString = "NICK " + twitchUsername + " \r\n";
			String userString = "USER " + twitchUsername;
			LOGGER.debug("Sending: " + oauthString);
			LOGGER.debug("Sending: " + nicknameString);
			LOGGER.debug("Sending: " + userString);
			
			this.writer.write(oauthString);
			this.writer.write(nicknameString);
			this.writer.write(userString);
			this.writer.flush();
			LOGGER.info("Connection request to twitch's IRC sent");
			
			String line = "";
			boolean successfullyLoggedIn = false;
			boolean sentJoinMessage = false;
			while ((line = this.reader.readLine()) != null) {
				LOGGER.info(line);

				if (line.equals(":tmi.twitch.tv NOTICE * :Error logging in")) {
					LOGGER.error("Error message received from Twitch's IRC", line);
					return false;

				} else if (line.contains(FlinkConnectorConstants.TWITCH_IRC_CONNECT_SUCCESS_CODE)) {
					LOGGER.info("Successfully authenticate on Twitch's IRC");
					successfullyLoggedIn = true;

				} else if ((successfullyLoggedIn && !sentJoinMessage && line.contains(FlinkConnectorConstants.TWITCH_IRC_POST_LOGGED_CODE))) {
					LOGGER.info("Sending join request for channel: " + channelToCOnnectTo);
					String joinChannelString = "JOIN #" + channelToCOnnectTo;
					LOGGER.debug("Sending join request: " + joinChannelString);
					
					this.sendMessageOnChat(joinChannelString);
					sentJoinMessage = true;
					
				} else if (successfullyLoggedIn && sentJoinMessage && line.contains(FlinkConnectorConstants.TWITCH_IRC_JOIN_SUCCESS_CODE)) {
					LOGGER.info("Successfully joined channel " + channelToCOnnectTo);
					return true;
				}
			}
			
			LOGGER.info("Exited connection loop");
			return false;
		}
	}
	
	private OAuth2Tokens initializeAuthToken() {
		Properties flinkProperties = new Properties();
		try (final InputStream stream = this.getClass().getClassLoader().getResourceAsStream("flink-twitch-connector.properties")) {
			flinkProperties.load(stream);
			
			String accessToken = flinkProperties.getProperty("flink.connector.twitch.token");
			if (accessToken.isEmpty()) {
				LOGGER.error("Can't create auth token, property wasn't found");
			} else {
				OAuth2Tokens token = new OAuth2Tokens(accessToken, "");
				this.oauthToken = token;
			}
		} catch (IOException e) {
			LOGGER.error("An error occured during Flink properties loading", e);
		}
		
		return this.oauthToken;
	}
	
	public void sendMessageOnChat(String message) throws IOException {
		this.writer.write("\r\n" + message + "\r\n");
		this.writer.flush();
	}
	
	public BufferedReader getReader() {
		return reader;
	}

	public BufferedWriter getWriter() {
		return writer;
	}
}
