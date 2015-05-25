package com.ctrengine.photostash.models;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import play.libs.Json;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class PhotographDocument extends AbstractFileDocument implements RelateDocument {
	public static final String COLLECTION = "photographs";
	public static final String RELATE_COLLECTION = "photographrelations";
	private static final Pattern DATE_PATTERN = Pattern.compile("[0-1]\\d-[0-3]\\d-\\d\\d\\d\\d");
	private static final Pattern DAY_MINUTE_SECOND_PATTERN = Pattern.compile("\\d\\d\\d\\d\\d\\d\\d\\d-\\d\\d\\d\\d");

	private transient final SimpleDateFormat DATE_PARSER = new SimpleDateFormat("MM-dd-yyyy");
	private transient final SimpleDateFormat OLD_DATETIME_PARSER = new SimpleDateFormat("MMddyyyy-HHmm");
	private transient final SimpleDateFormat DATETIME_PARSER = new SimpleDateFormat("yyyyMMdd-HHmm");

	private String description;
	private String mimeType;
	private int squareSize;
	private long size;
	private long stashSize;

	private Long dateTaken;

	public PhotographDocument(File photographFile) throws DocumentException {
		super(photographFile);
		generatePhotographInfo(photographFile);
		stashSize = 0;
	}

	private void generatePhotographInfo(File file) throws DocumentException {
		/**
		 * Get the mimeType and photograph original size
		 */
		Path photographPath = Paths.get(file.getAbsolutePath());
		try {
			mimeType = Files.probeContentType(photographPath);
			if (!mimeType.startsWith("image")) {
				throw new DocumentException(file.getName() + " is not an image.");
			}
			size = Files.size(photographPath);
		} catch (IOException e) {
			throw new DocumentException(e);
		}

		try {
			/**
			 * Get Square Size
			 */
			BufferedImage bufferedImage = ImageIO.read(file);
			squareSize = Math.max(bufferedImage.getWidth(), bufferedImage.getHeight());
		} catch (IOException e) {
			throw new DocumentException(e);
		}

		/**
		 * Figure out when the photograph was taken and generate key
		 */
		String fileName = file.getName().trim();
		fileName = fileName.replaceAll("\\.\\w+$", "");

		/**
		 * Support this weird old formats from ctrengine/corbantek history
		 * 
		 * MM-dd-yyyy--Text
		 * 
		 * MMddyyyy-HHmm-Text
		 * 
		 * yyyyMMdd-HHmm-Text
		 */
		Matcher findDayMinuteSecond = DAY_MINUTE_SECOND_PATTERN.matcher(fileName);
		try {
			if (findDayMinuteSecond.find()) {
				String dayMinuteSecond = findDayMinuteSecond.group();
				/**
				 * Test for old MMddyyyy
				 */
				int yearOrMonth = Integer.parseInt(dayMinuteSecond.substring(0, 4));
				if (yearOrMonth <= 1231) {
					/**
					 * Old Date format with Month-Day-year
					 */
					dateTaken = OLD_DATETIME_PARSER.parse(dayMinuteSecond).getTime();
				} else {
					/**
					 * New Day format Year-Month-Day
					 */
					dateTaken = DATETIME_PARSER.parse(dayMinuteSecond).getTime();
				}
				fileName = fileName.substring(findDayMinuteSecond.end());
			} else {
				Matcher findDate = DATE_PATTERN.matcher(fileName);
				if (findDate.find()) {
					dateTaken = DATE_PARSER.parse(findDate.group()).getTime();
					fileName = fileName.substring(findDate.end());
				}
			}
		} catch (NumberFormatException | ParseException e1) {
			/**
			 * TODO Logging Message
			 */
		}

		/**
		 * If dateTaken isn't from the filename, then look at the metadata
		 */
		if (dateTaken == null) {
			try {
				Metadata metadata = ImageMetadataReader.readMetadata(file);
				ExifSubIFDDirectory exifSubIFDDirectory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
				if (exifSubIFDDirectory != null) {
					Date date = exifSubIFDDirectory.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);					
					if (date != null) {
						/**
						 * TODO Fix Time Zone Issues
						 */
						dateTaken = date.getTime();
					}
				}
			} catch (ImageProcessingException | IOException e) {
				/**
				 * TODO: Log this
				 */
			}
		}

		String key = "";
		if (dateTaken != null) {
			key = DATETIME_PARSER.format(new Date(dateTaken)) + "-";
		}
		key += fileName.trim();
		setKey(key.replaceAll("[^A-Za-z\\-\\d\\s]+", "").replaceAll("\\s+-\\s+", "-").replaceAll("\\s+", "-").replaceAll("-+", "-").toLowerCase());
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

	public long getStashSize() {
		return stashSize;
	}

	public void setStashSize(long stashSize) {
		this.stashSize = stashSize;
	}

	public Long getDateTaken() {
		return dateTaken;
	}

	public int getSquareSize() {
		return squareSize;
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
	public int compareTo(Document o) {
		if (o instanceof PhotographDocument) {
			PhotographDocument otherPhotographDocument = (PhotographDocument) o;
			if (dateTaken != null && otherPhotographDocument.dateTaken != null) {
				return dateTaken.compareTo(otherPhotographDocument.dateTaken);
			}
		}
		return super.compareTo(o);
	}

	@Override
	public ObjectNode toJson() {
		ObjectNode photographNode = Json.newObject();
		photographNode.put("photographId", getKey());
		photographNode.put("name", getName());
		if (getDateTaken() != null) {
			photographNode.put("dateTaken", getDateTaken());
		}
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
		photographNodeExtended.put("squareSize", getSquareSize());
		photographNodeExtended.put("stashSize", getStashSize());

		return photographNodeExtended;
	}
}
