package com.ctrengine.photostash.controllers;

import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;

import com.ctrengine.photostash.database.PhotostashDatabase;
import com.ctrengine.photostash.database.PhotostashDatabaseException;
import com.ctrengine.photostash.models.Album;
import com.ctrengine.photostash.models.Story;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class StoryController extends Controller {
	public static Result getStories(Boolean extended) {
		try {
			ArrayNode storysNode = Json.newObject().arrayNode();
			for (Story story : PhotostashDatabase.INSTANCE.getStories()) {
				storysNode.add(story.toJson(extended));
			}
			return ok(storysNode);
		} catch (PhotostashDatabaseException e) {
			return internalServerError(Json.newObject().put("message", e.getMessage()));
		}
	}

	public static Result getStory(String storyId, Boolean extended) {
		try {
			Story story = PhotostashDatabase.INSTANCE.getStory(storyId);
			if(story == null){
				return badRequest(Json.newObject().put("message", storyId+" not found."));
			}else{
				ObjectNode albumNode = story.toJson(extended);
				//ArrayNode storysNode = albumNode.arrayNode();
				/**
				 * Get the stories associated with this Album
				 */
				//for(Story story: PhotostashDatabase.INSTANCE.getStories(album)){
				//	storysNode.add(story.toJson(extended));
				//}
				//albumNode.put("photographs", storysNode);
				return ok(albumNode);
			}
		} catch (PhotostashDatabaseException e) {
			return internalServerError(Json.newObject().put("message", e.getMessage()));
		}
	}
}
