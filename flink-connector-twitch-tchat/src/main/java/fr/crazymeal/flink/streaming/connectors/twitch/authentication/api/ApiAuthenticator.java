package fr.crazymeal.flink.streaming.connectors.twitch.authentication.api;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import org.jets3t.service.security.OAuth2Tokens;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import fr.crazymeal.flink.streaming.connectors.twitch.util.FlinkConnectorConstants;

public class ApiAuthenticator {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(ApiAuthenticator.class);
	
	private final String parameterSeparator = "&";
	
	private String clientId;
	private String clientSecret;
	
	private URL urlToTwitch;
	HttpsURLConnection httpsConnection;
	
	public ApiAuthenticator(String clientId, String clientSecret) {
		super();
		this.clientId = clientId;
		this.clientSecret = clientSecret;
	}
	
	public OAuth2Tokens getAuthToken() {
		OAuth2Tokens token = null;
		
		try {
			this.initPostConnection();
			this.sendPostAuth();
			String responseBody = this.getResponseBody();
			token = this.getOAuthToken(responseBody);
		} catch (IOException e) {
			LOGGER.error("Error making OAuth token", e);
			e.printStackTrace();
		}
		
		return token;
	}
	
	private OAuth2Tokens getOAuthToken(String responseBody) throws JsonParseException, JsonMappingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		ApiOAuthTokenResponse twitchTokenResponse = mapper.readValue(responseBody, ApiOAuthTokenResponse.class);
		
		LOGGER.info(twitchTokenResponse.toString());
		
		String accessToken = twitchTokenResponse.getAccess_token();
		String refreshToken = twitchTokenResponse.getRefresh_token();
		
		OAuth2Tokens token = new OAuth2Tokens(accessToken, refreshToken);
		return token;
	}

	private void initPostConnection() throws IOException {
		this.urlToTwitch = new URL(FlinkConnectorConstants.BASE_URL_API_TOKEN);
		this.httpsConnection = (HttpsURLConnection) urlToTwitch.openConnection();
		httpsConnection.setRequestMethod("POST");
		httpsConnection.setDoOutput(true);
	}
	
	private void sendPostAuth() throws IOException {
		StringBuilder sb = new StringBuilder();
		sb.append("client_id=");
		sb.append(this.clientId);
		
		sb.append(this.parameterSeparator);
		sb.append("client_secret=");
		sb.append(this.clientSecret);
		
		sb.append(this.parameterSeparator);
		sb.append("grant_type=client_credentials");
		
		sb.append(this.parameterSeparator);
		sb.append("scope=chat_login");
		String urlParameters = sb.toString();
		
		OutputStream connectionOutputStream = this.httpsConnection.getOutputStream();
		DataOutputStream dos = new DataOutputStream(connectionOutputStream);
		dos.writeBytes(urlParameters);
		dos.flush();
		dos.close();
		
		int responseCode = httpsConnection.getResponseCode();
		LOGGER.debug("Sending 'POST' request to URL : " + urlToTwitch);
		LOGGER.debug("Post parameters : " + urlParameters);
		LOGGER.debug("Response Code : " + responseCode);
	}
	
	private String getResponseBody() throws IOException {
		InputStream connectionInputStream = this.httpsConnection.getInputStream();
		BufferedReader in = new BufferedReader(new InputStreamReader(connectionInputStream));
		
		String inputLine;
		StringBuffer response = new StringBuffer();

		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();
		
		String responseBody = response.toString();
		LOGGER.debug(responseBody);
		
		return responseBody;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public void setClientSecret(String clientSecret) {
		this.clientSecret = clientSecret;
	}
}
