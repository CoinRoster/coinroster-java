package com.coinroster.internal;

import java.io.IOException;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.Connection.Response;
import org.jsoup.nodes.Document;

import com.coinroster.Utils;

public class scrapeHTML {
	
	public static Document connect(String url) throws IOException, InterruptedException {
		int max_tries = 4;
		boolean success = false;
		Document doc = null;
		int attempt_count = 1;
		while(attempt_count <= max_tries && !success){
			try{
				Response response = Jsoup.connect(url)
					.userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
					.referrer("http://www.google.com").timeout(0).execute();
				if(response.statusCode() == 200){
					success = true;
					doc = response.parse();
				}
			}catch(HttpStatusException e){
				Utils.log("couldn't access " + url + " . \t\tSleeping and retrying...");
				Thread.sleep(3000);
			}
			catch(IOException e){
				Utils.log("couldn't access " + url + " . \t\tSleeping and retrying...");
				Thread.sleep(3000);
			}finally{
				attempt_count += 1;
			}
		}
		return(doc);
	}
}

  