package com.coinroster.internal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;

import org.json.JSONException;
import org.json.JSONObject;

import com.coinroster.Utils;

public class JsonReader {

  private static String readAll(Reader rd) throws IOException {
    StringBuilder sb = new StringBuilder();
    int cp;
    while ((cp = rd.read()) != -1) {
      sb.append((char) cp);
    }
    return sb.toString();
  }

  public static JSONObject readJsonFromUrl(String url) throws IOException, JSONException {
    InputStream is = new URL(url).openStream();
    try {
      BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
      // weird issue with pgatour.com's current/schedule-v2.json having duplicate "i":"2018" entries
      String jsonText = readAll(rd).replace("\"i\":\"2018\",", "").replace("\"i\":\"2019\",", "")
    		  .replace("\"i\":\"2020\",", ""); 
      JSONObject json = new JSONObject(jsonText);
      return json;
    }
    catch(JSONException e){
    	Utils.log("Unable to connect to API and grab JSON\n" + e.getMessage());
    	return null;
    }
    finally {
      is.close();
    }
  }
}