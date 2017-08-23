package fr.crazymeal.flink.streaming.connectors.twitch;

import java.io.IOException;
import java.nio.charset.Charset;

import org.jets3t.service.security.OAuth2Tokens;
import org.pircbotx.Configuration;
import org.pircbotx.PircBotX;
import org.pircbotx.exception.IrcException;

import fr.crazymeal.flink.streaming.connectors.twitch.tchat.TwitchTchatSource;

public class Main {

	public static void main(String[] args) {
		TwitchTchatSource source = new TwitchTchatSource("crazymeal", "bibixhd");
		try {
			source.open(null);
			source.run(null);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
