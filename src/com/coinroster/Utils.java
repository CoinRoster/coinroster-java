package com.coinroster;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.codec.digest.DigestUtils;

public class Utils 
	{
	public static boolean is_valid_email(String email_address)
		{
		String email_regex = "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@" + "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";
		Pattern pattern = Pattern.compile(email_regex);
		Matcher matcher = pattern.matcher(email_address);
		return matcher.matches();
		}
	
	public static boolean is_valid_btc(double amount)
		{
		if (amount == Double.parseDouble(format_btc(amount))) return true;
		return false;
		}
	
	public static String format_btc(double amount)
		{
		DecimalFormat btc_formatter = new DecimalFormat("#######0.00000000");
		return btc_formatter.format(amount);
		}
	
	public static String SHA1(String data) 
		{
		String digest = DigestUtils.sha1Hex(data);
		return digest;
		}

	protected String no_whitespace(String string) throws Exception
		{
		string = string.replaceAll("\\s","");
		return string;
		}
	
	public static void log(Object msg)
		{
		System.out.println(new SimpleDateFormat("MMM d h:mm:ss a").format(new Date()) + " : " + String.valueOf(msg));
		}

	final static Charset ENCODING = StandardCharsets.UTF_8;
	
	static List<String> read(String file_path) throws IOException 
		{
		Path path = Paths.get(file_path);
		return Files.readAllLines(path, ENCODING);
		}
	
	static void write(String file_path, String[] array) throws IOException 
		{
		Path path = Paths.get(file_path);
		try (BufferedWriter writer = Files.newBufferedWriter(path, ENCODING))
			{
			for(String line : array)
				{
				writer.write(line);
				writer.newLine();
				}
			}
		}
	
	public static byte[] inputstream_to_bytearray(InputStream in) throws Exception
		{
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		
		byte[] data = new byte[1024];

		for (int n; (n = in.read(data, 0, data.length)) != -1;) buffer.write(data, 0, n);

		buffer.flush();

		return buffer.toByteArray();
		}

	}
