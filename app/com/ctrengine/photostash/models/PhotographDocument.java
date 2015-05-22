package com.ctrengine.photostash.models;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

import play.libs.Json;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class PhotographDocument extends AbstractFileDocument implements RelateDocument {
	public static final String COLLECTION = "photographs";
	public static final String RELATE_COLLECTION = "photographrelations";
	
	private transient final SimpleDateFormat DATE_PARSER = new SimpleDateFormat("MM-dd-yyyy");
	private transient final SimpleDateFormat OLD_DATETIME_PARSER = new SimpleDateFormat("MMddyyyy-mmss");
	private transient final SimpleDateFormat DATETIME_PARSER = new SimpleDateFormat("yyyyMMdd-mmss");

	private String description;
	private String mimeType;
	private long size;
	private Long dateTaken;

	public PhotographDocument(File photographFile) throws DocumentException {
		super(photographFile);
		generatePhotographInfo(photographFile);
		Path photographPath = Paths.get(photographFile.getAbsolutePath());
		try {
			mimeType = Files.probeContentType(photographPath);
			if (!mimeType.startsWith("image")) {
				throw new DocumentException(photographFile.getName() + "is not an image.");
			}
			size = Files.size(photographPath);
		} catch (IOException e) {
			throw new DocumentException(e);
		}
	}
	
	private void generatePhotographInfo(File file){
		String fileName = file.getName().trim();
		fileName = fileName.replaceAll("\\.\\w+$", "");
		setKey(fileName.replaceAll("[^A-Za-z\\-\\d\\s]+", "").replaceAll("\\s+-\\s+", "-").replaceAll("\\s+", "-").toLowerCase());
		/**
		 * Support this weird old formats from ctrengine/corbantek history
		 * 
		 * MM-dd-yyyy--Text
		 * 
		 * MMddyyyy-mmSS-Text
		 * 
		 * yyyyMMdd-mmSS-Text
		 */
		
		/**
		 * If dateTaken isn't from the filename, then look at the metadata
		 */
		if(dateTaken == null){
			try {
				Metadata metadata = ImageMetadataReader.readMetadata(file);
				ExifSubIFDDirectory exifSubIFDDirectory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
				dateTaken = exifSubIFDDirectory.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL).getTime();
			} catch (ImageProcessingException | IOException e) {
				/**
				 * TODO: Log this
				 */
			}
		}
	}

	public String getDescription() {
		return description;
	}

	public String getMimeType() {
		return mimeType;
	}

	public long getSize() {
		return size;
	}

	public Long getDateTaken() {
		return dateTaken;
	}

	@Override
	public String getRelateCollection() {
		return RELATE_COLLECTION;
	}

	@Override
	public String getCollection() {
		return COLLECTION;
	}

	@Override
	public ObjectNode toJson() {
		ObjectNode photographNode = Json.newObject();
		photographNode.put("photographId", getKey());
		photographNode.put("name", getName());
		return photographNode;
	}

	@Override
	public ObjectNode toJsonExtended() {
		ObjectNode photographNodeExtended = toJson();
		if (getDescription() != null) {
			photographNodeExtended.put("description", getDescription());
		}
		photographNodeExtended.put("mimeType", getMimeType());
		photographNodeExtended.put("size", getSize());
		if (getDateTaken() != null) {
			photographNodeExtended.put("dateTaken", getDateTaken());
		}
		return photographNodeExtended;
	}
}
