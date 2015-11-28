package com.ctrengine.photostash.controllers.api;

import static akka.pattern.Patterns.ask;
import play.Routes;
import play.libs.Akka;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import akka.actor.ActorRef;

import com.ctrengine.photostash.database.DatabaseException;
import com.ctrengine.photostash.database.PhotostashDatabase;
import com.ctrengine.photostash.models.PhotographDocument;
import com.ctrengine.photostash.models.StoryDocument;
import com.ctrengine.photostash.shoebox.Shoebox;
import com.ctrengine.photostash.shoebox.ShoeboxMessages.PhotographResizeRequestMessage;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class StoryController extends Controller {
	public static Result getStories(Boolean extended) {
		try {
			ArrayNode storysNode = Json.newObject().arrayNode();
			for (StoryDocument storyDocument : PhotostashDatabase.INSTANCE.getStories()) {
				storysNode.add(storyDocument.toJson(extended).put("link", routes.StoryController.getStory(storyDocument.getKey(), extended).absoluteURL(request())));
			}
			return ok(storysNode);
		} catch (DatabaseException e) {
			return internalServerError(Json.newObject().put("message", e.getMessage()));
		}
	}
	
	public static Result getRecentStories(int size, Boolean extended) {
		try {
			ArrayNode storysNode = Json.newObject().arrayNode();
			for (StoryDocument storyDocument : PhotostashDatabase.INSTANCE.getRecentStories(size)) {
				storysNode.add(storyDocument.toJson(extended).put("link", routes.StoryController.getStory(storyDocument.getKey(), extended).absoluteURL(request())));
			}
			return ok(storysNode);
		} catch (DatabaseException e) {
			return internalServerError(Json.newObject().put("message", e.getMessage()));
		}
	}

	public static Result getStory(String storyId, Boolean extended) {
		try {
			StoryDocument storyDocument = PhotostashDatabase.INSTANCE.getStory(storyId);
			if(storyDocument == null){
				return badRequest(Json.newObject().put("message", storyId+" not found."));
			}else{
				ObjectNode storyNode = storyDocument.toJson(extended);
				storyNode.put("link", routes.StoryController.getStory(storyDocument.getKey(), extended).absoluteURL(request()));
				ArrayNode photographsNode = storyNode.arrayNode();
				/**
				 * Get the stories associated with this AlbumDocument
				 */
				for(PhotographDocument photographDocument: PhotostashDatabase.INSTANCE.getRelatedDocuments(storyDocument, PhotographDocument.class)){
					ObjectNode photographNode = photographDocument.toJson(extended);
					photographNode.put("link", routes.PhotographController.getPhotograph(photographDocument.getKey(), extended).absoluteURL(request()));
					photographsNode.add(photographNode);
				}
				storyNode.put("photographs", photographsNode);
				return ok(storyNode);
			}
		} catch (DatabaseException e) {
			return internalServerError(Json.newObject().put("message", e.getMessage()));
		}
	}
	
	public static Result getStoryThumbnail(String storyId, Boolean extended) {
		try {
			StoryDocument storyDocument = PhotostashDatabase.INSTANCE.getStory(storyId);
			if(storyDocument == null){
				return badRequest(Json.newObject().put("message", storyId+" not found."));
			}else{
				ObjectNode storyNode = storyDocument.toJson(extended);
				ArrayNode photographsNode = storyNode.arrayNode();
				/**
				 * Get the stories associated with this AlbumDocument
				 */
				for(PhotographDocument photographDocument: PhotostashDatabase.INSTANCE.getRelatedDocuments(storyDocument, PhotographDocument.class)){
					ObjectNode photographNode = photographDocument.toJson(extended);
					photographNode.put("link", routes.PhotographController.getPhotograph(photographDocument.getKey(), extended).absoluteURL(request()));
					photographsNode.add(photographNode);
				}
				storyNode.put("photographs", photographsNode);
				return ok(storyNode);
			}
		} catch (DatabaseException e) {
			return internalServerError(Json.newObject().put("message", e.getMessage()));
		}
	}
	
	public static Result resizeStoryPhotographs(final String storyId, final Integer squareSize) {
		try {
			StoryDocument storyDocument = PhotostashDatabase.INSTANCE.getStory(storyId);
			if(storyDocument == null){
				return badRequest(Json.newObject().put("message", storyId+" not found."));
			}else{
				ObjectNode storyNode = storyDocument.toJson(false);
				storyNode.put("link", routes.StoryController.getStory(storyDocument.getKey(), false).absoluteURL(request()));
				ArrayNode photographsNode = storyNode.arrayNode();
				/**
				 * Get the stories associated with this AlbumDocument and request a resize on each photograph
				 */
				for(PhotographDocument photographDocument: PhotostashDatabase.INSTANCE.getRelatedDocuments(storyDocument, PhotographDocument.class)){
					ObjectNode photographNode = photographDocument.toJson(false);
					photographNode.put("link", routes.PhotographController.getPhotographResizeImage(photographDocument.getKey(), squareSize).absoluteURL(request()));
					photographsNode.add(photographNode);
					Shoebox.INSTANCE.getPhotographRouter().tell(new PhotographResizeRequestMessage(photographDocument, squareSize), ActorRef.noSender());
				}
				storyNode.put("photographs", photographsNode);
				return ok(storyNode);
			}
		} catch (DatabaseException e) {
			return internalServerError(Json.newObject().put("message", e.getMessage()));
		}
	}
	
	public static Result javascriptRoutes() {
		response().setContentType("text/javascript");
		return ok(Routes.javascriptRouter("jsRoutesStoryController",
		/**
		 * Routes
		 */
		com.ctrengine.photostash.controllers.api.routes.javascript.StoryController.getStories(),
		
		com.ctrengine.photostash.controllers.api.routes.javascript.StoryController.getRecentStories(),

		com.ctrengine.photostash.controllers.api.routes.javascript.StoryController.getStory(),
		
		com.ctrengine.photostash.controllers.api.routes.javascript.StoryController.resizeStoryPhotographs(),
		
		com.ctrengine.photostash.controllers.api.routes.javascript.StoryController.getStoryThumbnail()));
	}
}
