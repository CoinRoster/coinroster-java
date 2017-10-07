package com.coinroster.internal;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.JSONException;
import org.json.JSONObject;

import com.coinroster.Server;
import com.coinroster.Utils;

public class CallCGS extends Utils
	{
	String post_response;
	
	public JSONObject get_response() throws JSONException
		{
		return new JSONObject(post_response);
		}
	
	public CallCGS(JSONObject post_JSON)
		{
		try {
			String post_content = post_JSON.toString();
			
			log("Submitting:");
			log(post_content);

			URL url = new URL("http://127.0.0.1:8090");

			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestProperty("Content-Type","application/json");
			connection.setRequestMethod("POST");
			connection.setDoOutput(true);
			
			PrintStream ps = new PrintStream(connection.getOutputStream());
			ps.println(post_content);
			ps.close();
			
			connection.connect();

			BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			
			StringBuilder response = new StringBuilder();
			
			String line;
			
			while ((line=reader.readLine()) != null) response.append(line);
			
			reader.close();
			connection.disconnect();
			
			post_response = response.toString().trim();
			}
		catch (Exception e)
			{
			Server.exception(e);
			}
		}
	}
