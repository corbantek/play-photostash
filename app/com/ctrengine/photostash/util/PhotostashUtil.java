package com.ctrengine.photostash.util;

import java.io.File;
import java.util.UUID;

public class PhotostashUtil {
	public static String generateKeyFromFile(File file){
		if(file == null){
			return UUID.randomUUID().toString();
		}else{
			return file.getName().replaceAll("[^A-Za-z\\-\\d\\s]+", "").replaceAll("\\s", "-").toLowerCase();
		}
	}
}
