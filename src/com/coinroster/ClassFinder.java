package com.coinroster;

import java.io.File;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ClassFinder 
	{
    public static List<Class<?>> find(String package_name) throws Exception
    	{
    	List<Class<?>> classes = new ArrayList<Class<?>>();

		String fs = File.separator;

	    char 
	    
	    PKG_SEPARATOR = '.',
	    DIR_SEPARATOR = fs.charAt(0);

		String package_directory = package_name.replace(PKG_SEPARATOR, DIR_SEPARATOR);

		File jar_file = new File(Server.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());

		if (jar_file.isFile()) // running as Jar
			{
		    JarFile jar = new JarFile(jar_file);
		    Enumeration<JarEntry> entries = jar.entries();
		    while (entries.hasMoreElements()) 
		    	{
		        String name = entries.nextElement().getName();
		        if (name.startsWith(package_directory) && name.endsWith(".class")) 
					{
					name = name.substring(name.lastIndexOf(fs) + 1, name.indexOf(".class"));
					classes.add(Class.forName(package_name + "." + name));
					}
		    	}
		    jar.close();
			}
		else // running in IDE
			{
			String path = Server.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath() + fs + package_directory;

			File f = new File(path);
			
			for (File file : f.listFiles())
				{
				String name = file.getName();
				if (name.endsWith(".class"))
					{
					name = name.substring(0, name.indexOf(".class"));
					classes.add(Class.forName(package_name + "." + name));
					}
				}
			}
		
		return classes;
    	}
    }