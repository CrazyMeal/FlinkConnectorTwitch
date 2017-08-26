package fr.crazymeal.flink.streaming.connectors.twitch.tchat;

import org.junit.Test;

public class TwitchTchatSourceTest {

	/**
	 * This is a basic "test" to check that you can use this source. To make this test running, you have to set some information:<br />
	 *  - Twitch username<br />
	 *  - Twitch channel that you want to collect message from<br />
	 * Don't forget to set your twitch token in flink-twitch-connector.properties
	 * @author KVN
	 */
	@Test
	public void twitchSourceTest() {
		String twitchUsername = "";
		String twitchChannelToConnectTo = "";
		
		TwitchTchatSource source = new TwitchTchatSource(twitchUsername, twitchChannelToConnectTo);
		try {
			source.open(null);
			source.run(null);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
