package fr.crazymeal.flink.streaming.connectors.twitch.tchat;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.Properties;

import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.source.RichSourceFunction;
import org.jets3t.service.security.OAuth2Tokens;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.crazymeal.flink.streaming.connectors.twitch.util.FlinkConnectorConstants;

public class TwitchTchatSource extends RichSourceFunction<String> {
	
	private static final long serialVersionUID = 3909414896495260607L;
	
	private static final Logger LOGGER = LoggerFactory.getLogger(TwitchTchatSource.class);
	
	private transient volatile boolean isRunning;
	
	private OAuth2Tokens oauthToken;
	
	private Socket socket;
	private BufferedReader reader;
	private BufferedWriter writer;
	
	private String twitchUsername;
	private String channelToCOnnectTo;
	
	public TwitchTchatSource(String twitchUsername, String channelToCOnnectTo) {
		super();
		this.twitchUsername = twitchUsername;
		this.channelToCOnnectTo = channelToCOnnectTo;
	}
	
	@Override
	public void open(Configuration parameters) throws Exception {
		super.open(parameters);
		
		this.initializeAuthToken();
		
		if (this.oauthToken == null) {
			LOGGER.error("Authentication can't be made, access token isn't initialized");			
		} else {
			LOGGER.info("Authentication token was made successfully");
				
			this.socket = new Socket(FlinkConnectorConstants.TWITCH_IRC_URI, FlinkConnectorConstants.TWITCH_IRC_PORT);
			this.socket.setSoTimeout(3000000);
			LOGGER.debug("Socket was made");
			
			this.reader = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
			this.writer = new BufferedWriter(new OutputStreamWriter(this.socket.getOutputStream()));
			
			String oauthString = "PASS oauth:" + this.oauthToken.getAccessToken() + " \r\n";
			String nicknameString = "NICK " + this.twitchUsername + " \r\n";
			String userString = "USER " + this.twitchUsername;
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
			while ((line = this.reader.readLine()) != null) {
				LOGGER.info(line);

				if (line.equals(":tmi.twitch.tv NOTICE * :Error logging in")) {
					LOGGER.error("Error message received from Twitch's IRC", line);
					break;

				} else if (line.contains(FlinkConnectorConstants.TWITCH_IRC_CONNECT_SUCCESS_CODE)) {
					LOGGER.info("Successfully authenticate on Twitch's IRC");
					successfullyLoggedIn = true;

				} else if ((successfullyLoggedIn && line.contains(FlinkConnectorConstants.TWITCH_IRC_POST_LOGGED_CODE))) {
					LOGGER.info("Sending join request for channel: " + this.channelToCOnnectTo);
					String joinChannelString = "JOIN #" + this.channelToCOnnectTo + "\r\n";
					LOGGER.debug("Sending join request: " + joinChannelString);
					this.sendMessageOnChat(joinChannelString);
					
					LOGGER.info("Sending join request for channel: " + this.channelToCOnnectTo);
					LOGGER.debug("Sending join request: " + joinChannelString);
					this.sendMessageOnChat(joinChannelString);
				}
			}

			LOGGER.info("Exited while loop");
		}

	}
	
	@Override
	public void run(SourceContext<String> context) throws Exception {
		String line = "";
		while(this.isRunning && (line = this.reader.readLine()) != null) {
			LOGGER.debug(line);
			
			if (line.contains(FlinkConnectorConstants.PING_REQUEST)) {
				this.sendMessageOnChat(FlinkConnectorConstants.PONG_ANSWER);
			} else {
				context.collect(line);
			}
		}
	}
	
	@Override
	public void cancel() {
		this.isRunning = false;
	}
	
	private void sendMessageOnChat(String message) throws IOException {
		this.writer.write(message);
		this.writer.flush();
	}
	
	private OAuth2Tokens initializeAuthToken() {
		Properties flinkProperties = new Properties();
		try (final InputStream stream = this.getClass().getClassLoader().getResourceAsStream("flink-connector.properties")) {
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

}
