package fr.crazymeal.flink.streaming.connectors.twitch.tchat;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.UnknownHostException;

import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.source.RichSourceFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.crazymeal.flink.streaming.connectors.twitch.authentication.irc.IrcConnector;
import fr.crazymeal.flink.streaming.connectors.twitch.util.FlinkConnectorConstants;

public class TwitchTchatSource extends RichSourceFunction<String> {
	
	private static final long serialVersionUID = 3909414896495260607L;
	
	private static final Logger LOGGER = LoggerFactory.getLogger(TwitchTchatSource.class);
	
	private transient volatile boolean isRunning;
	
	private IrcConnector ircConnector;
	
	private BufferedReader reader;
	
	private String twitchUsername;
	private String channelToCOnnectTo;
	
	public TwitchTchatSource(String twitchUsername, String channelToCOnnectTo) {
		super();
		this.twitchUsername = twitchUsername.toLowerCase();
		this.channelToCOnnectTo = channelToCOnnectTo.toLowerCase();
		this.ircConnector = new IrcConnector();
	}
	
	@Override
	public void open(Configuration parameters) throws Exception {
		super.open(parameters);
		
		try {
			boolean initCorrectly = this.ircConnector.initializeReaderWriter(this.twitchUsername, this.channelToCOnnectTo);
			
			if (initCorrectly) {
				this.reader = this.ircConnector.getReader();
				this.isRunning = true;
			}
		} catch (UnknownHostException e) {
			LOGGER.error("Exception about host during initialization", e);
		} catch (IOException e) {
			LOGGER.error("IOException during initialization", e);
		}
	}
	
	@Override
	public void run(SourceContext<String> context) throws Exception {
		LOGGER.info("Twitch source started!");
		String line = "";
		while(this.isRunning && (line = this.reader.readLine()) != null) {
			LOGGER.debug(line);
			
			if (line.contains(FlinkConnectorConstants.PING_REQUEST)) {
				LOGGER.info("Received PING request from Twitch, sending PONG answer");
				this.ircConnector.sendMessageOnChat(FlinkConnectorConstants.PONG_ANSWER);
			} else {
				if (context != null) {
					context.collect(line);
				} else {
					LOGGER.warn("Source context isn't initialize, can't collect message");
				}
			}
		}
	}
	
	@Override
	public void cancel() {
		this.isRunning = false;
	}
}
