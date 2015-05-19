package com.ctrengine.photostash.controllers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;

import com.ctrengine.photostash.database.PhotostashDatabase;
import com.ctrengine.photostash.database.PhotostashDatabaseException;
import com.ctrengine.photostash.models.Photograph;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class PhotographController extends Controller {
	public static Result getPhotographs(Boolean extended) {
		try {
			ArrayNode photographNode = Json.newObject().arrayNode();
			for (Photograph photograph : PhotostashDatabase.INSTANCE.getPhotographs()) {
				photographNode.add(photograph.toJson(extended).put("link", routes.PhotographController.getPhotograph(photograph.getKey(), extended).absoluteURL(request())));
			}
			return ok(photographNode);
		} catch (PhotostashDatabaseException e) {
			return internalServerError(Json.newObject().put("message", e.getMessage()));
		}
	}

	public static Result getPhotograph(String photographId, Boolean extended) {
		try {
			Photograph photograph = PhotostashDatabase.INSTANCE.getPhotograph(photographId);
			if(photograph == null){
				return badRequest(Json.newObject().put("message", photographId+" not found."));
			}else{
				ObjectNode photogrphNode = photograph.toJson(extended);
				ObjectNode photographImages = Json.newObject();
				photographImages.put("original", routes.PhotographController.getPhotographImage(photograph.getKey()).absoluteURL(request()));
				photogrphNode.put("images", photographImages);
				return ok(photogrphNode);
			}
		} catch (PhotostashDatabaseException e) {
			return internalServerError(Json.newObject().put("message", e.getMessage()));
		}
	}
	
	public static Result getPhotographImage(String photographId) {
		try {
			Photograph photograph = PhotostashDatabase.INSTANCE.getPhotograph(photographId);
			if(photograph == null){
				return badRequest(Json.newObject().put("message", photographId+" not found."));
			}else{
				Path photographPath = Paths.get(photograph.getPath());
				try {
					return ok(Files.readAllBytes(photographPath)).as("image/jpg");
				} catch (IOException e) {
					return internalServerError(Json.newObject().put("message", photographId+" could not be read."));
				}
			}
		} catch (PhotostashDatabaseException e) {
			return internalServerError(Json.newObject().put("message", e.getMessage()));
		}
	}
}
