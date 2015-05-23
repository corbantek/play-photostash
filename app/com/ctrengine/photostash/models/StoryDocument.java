package com.ctrengine.photostash.models;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import play.libs.Json;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class StoryDocument extends AbstractFileDocument implements RelateDocument {
	private static final Pattern DATE_AND_DATE_RANGE_PATTERN = Pattern.compile("[0-1]\\d-[0-3]\\d-\\d\\d\\d\\d");
	
	public static final String COLLECTION = "storys";
	public static final String RELATE_COLLECTION = "storyrelations";
	
	private transient final SimpleDateFormat DATE_PARSER = new SimpleDateFormat("MM-dd-yyyy");
	private transient final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("yyyyMMdd");

	private String description;
	private String coverPhotographKey;

	private Long storyDate;
	private Long storyEndDate;

	private Integer size;
	private Integer stashSize;

	public StoryDocument(File storyFile, int size, int stashSize) {
		super(storyFile);
		generateStoryInfo(storyFile);
	}

	/**
	 * Support the following weird story folder formats
	 * 
	 * MM-DD-YYYY Title
	 * 
	 * MM-DD-YYYY - MM-DD-YYYY Title
	 * 
	 * MMMM YYYY - Title
	 * 
	 * MMMM - MMMM YYYY - Title
	 * 
	 * @param storyFile
	 * @return
	 */
	private void generateStoryInfo(File file) {
		String fileName = file.getName().trim();
		Matcher findDateAndDateRange = DATE_AND_DATE_RANGE_PATTERN.matcher(fileName);
		if (findDateAndDateRange.find()) {
			try {
				storyDate = DATE_PARSER.parse(findDateAndDateRange.group()).getTime();
				fileName = fileName.substring(findDateAndDateRange.end());
			} catch (ParseException e) {
				/**
				 * TODO Logging Message
				 */
			}
			if (findDateAndDateRange.find()) {
				try {
					storyEndDate = DATE_PARSER.parse(findDateAndDateRange.group()).getTime();
					fileName = fileName.substring(findDateAndDateRange.end());
				} catch (ParseException e) {
					/**
					 * TODO Logging Message
					 */
				}
			}
		}
		String key = "";
		if (storyDate != null) {
			key = DATE_FORMATTER.format(new Date(storyDate)) + "-" ;
			if (storyEndDate != null) {
				key += DATE_FORMATTER.format(new Date(storyEndDate)) + "-";
			}
		}
		key += fileName.trim();
		setKey(key.replaceAll("[^A-Za-z\\-\\d\\s]+", "").replaceAll("\\s+-\\s+", "-").replaceAll("\\s+", "-").replaceAll("-+", "-").toLowerCase());
	}

	public String getDescription() {
		return description;
	}

	public Integer getSize() {
		return size;
	}

	public Integer getStashSize() {
		return stashSize;
	}

	public String getCoverPhotographKey() {
		return coverPhotographKey;
	}

	public void setCoverPhotographKey(String coverPhotographKey) {
		this.coverPhotographKey = coverPhotographKey;
	}

	@Override
	public String getCollection() {
		return COLLECTION;
	}

	@Override
	public ObjectNode toJson() {
		ObjectNode storyNode = Json.newObject();
		storyNode.put("storyId", getKey());
		storyNode.put("name", getName());
		return storyNode;
	}

	@Override
	public ObjectNode toJsonExtended() {
		ObjectNode storyNodeExtended = toJson();
		if (getCoverPhotographKey() != null) {
			storyNodeExtended.put("coverPhotographKey", getCoverPhotographKey());
		}
		if (getDescription() != null) {
			storyNodeExtended.put("description", getDescription());
		}
		if (getSize() != null) {
			storyNodeExtended.put("size", getSize());
		}
		if (getStashSize() != null) {
			storyNodeExtended.put("stashSize", getStashSize());
		}
		return storyNodeExtended;
	}

	@Override
	public String getRelateCollection() {
		return RELATE_COLLECTION;
	}
}
