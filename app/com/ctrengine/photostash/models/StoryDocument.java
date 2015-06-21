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
	private static final Pattern MONTH_YEAR_PATTERN = Pattern.compile("[A-Z][a-z]+\\s\\d\\d\\d\\d");
	private static final Pattern MONTH_RANGE_YEAR_PATTER = Pattern.compile("[A-Z][a-z]+\\s-\\s[A-Z][a-z]+\\s\\d\\d\\d\\d");

	public static final String COLLECTION = "stories";
	public static final String RELATE_COLLECTION = "storyrelations";

	private transient final SimpleDateFormat DATE_PARSER = new SimpleDateFormat("MM-dd-yyyy");
	private transient final SimpleDateFormat MONTH_YEAR_PARSER = new SimpleDateFormat("MMMM yyyy");
	private transient final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("yyyyMMdd");

	private String description;
	private String coverPhotographKey;

	private Long storyDate;
	private Long storyEndDate;

	public StoryDocument(File storyFile) {
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
			String newFileName = fileName;
			try {
				storyDate = DATE_PARSER.parse(findDateAndDateRange.group()).getTime();
				newFileName = fileName.substring(findDateAndDateRange.end());
			} catch (ParseException e) {
				/**
				 * TODO Logging Message
				 */
			}
			if (findDateAndDateRange.find()) {
				try {
					storyEndDate = DATE_PARSER.parse(findDateAndDateRange.group()).getTime();
					newFileName = fileName.substring(findDateAndDateRange.end());
				} catch (ParseException e) {
					/**
					 * TODO Logging Message
					 */
				}
			}
			fileName = newFileName;
		} else {
			Matcher findMonthRangeYear = MONTH_RANGE_YEAR_PATTER.matcher(fileName);
			if (findMonthRangeYear.find()) {
				try {
					String[] monthRangeYearSplit = findMonthRangeYear.group().split("\\s-\\s");
					String year = monthRangeYearSplit[1].split("\\s")[1];
					storyDate = MONTH_YEAR_PARSER.parse(monthRangeYearSplit[0] + " " + year).getTime();
					storyEndDate = MONTH_YEAR_PARSER.parse(monthRangeYearSplit[1]).getTime();
					fileName = fileName.substring(findMonthRangeYear.end());
				} catch (ParseException e) {
					/**
					 * TODO Logging Message
					 */
				}
			} else {
				Matcher findMonthYear = MONTH_YEAR_PATTERN.matcher(fileName);
				if (findMonthYear.find()) {
					try {
						storyDate = MONTH_YEAR_PARSER.parse(findMonthYear.group()).getTime();
						fileName = fileName.substring(findMonthYear.end());
					} catch (ParseException e) {
						/**
						 * TODO Logging Message
						 */
					}
				}
			}
		}
		String key = "";
		if (storyDate != null) {
			key = DATE_FORMATTER.format(new Date(storyDate)) + "-";
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
		return storyNodeExtended;
	}

	@Override
	public String getRelateCollection() {
		return RELATE_COLLECTION;
	}

	@Override
	public int compareTo(Document o) {
		if (o instanceof StoryDocument) {
			StoryDocument otherStory = (StoryDocument) o;
			if (storyDate != null && otherStory.storyDate != null) {
				return storyDate.compareTo(otherStory.storyDate);
			}
		}
		return super.compareTo(o);
	}
}
