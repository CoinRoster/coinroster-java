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
	JSONObject 
	
	result = null,
	error = null;
	
	public JSONObject get_result() throws JSONException
		{
		return result;
		}

	public JSONObject get_error() throws JSONException
		{
		return error;
		}
	
	public CallCGS(JSONObject post_JSON)
		{
		try {
			post_JSON.put("jsonrpc", "2.0");
			post_JSON.put("id", 1);
			
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
			
			JSONObject response_obj = new JSONObject(response.toString().trim());
			
			if (!response_obj.isNull("result")) result = response_obj.getJSONObject("result");	
			if (!response_obj.isNull("error")) result = response_obj.getJSONObject("error");	
			}
		catch (Exception e)
			{
			Server.exception(e);
			}
		}
	}
