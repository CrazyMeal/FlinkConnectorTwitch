package fr.crazymeal.flink.streaming.connectors.twitch;

import fr.crazymeal.flink.streaming.connectors.twitch.tchat.TwitchTchatSource;

public class Main {

	public static void main(String[] args) {
		TwitchTchatSource source = new TwitchTchatSource("", "");
		try {
			source.open(null);
			source.run(null);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
