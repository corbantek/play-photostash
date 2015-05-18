package com.ctrengine.photostash.database;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import play.Logger;
import play.Logger.ALogger;

import com.arangodb.ArangoConfigure;
import com.arangodb.ArangoDriver;
import com.arangodb.ArangoException;
import com.arangodb.ArangoHost;
import com.arangodb.entity.CollectionEntity;
import com.arangodb.entity.CollectionOptions;
import com.arangodb.entity.CollectionType;
import com.arangodb.entity.EdgeDefinitionEntity;
import com.arangodb.entity.IndexType;
import com.arangodb.entity.StringsResultEntity;
import com.arangodb.util.MapBuilder;
import com.ctrengine.photostash.conf.DatabaseConfiguration;
import com.ctrengine.photostash.models.Album;
import com.ctrengine.photostash.models.Story;

public enum PhotostashDatabase {
	INSTANCE;

	private static final ALogger LOGGER = Logger.of("database");
	
	private static final String ALBUM_RELATIONS_COLLECTION = "albumrelations";
	
	private static final String QUERY_ALBUM = "FOR a IN "+Album.COLLECTION+" FILTER a."+Album.PATH+" == @path RETURN a";
	private static final String QUERY_STORY = "FOR a IN "+Story.COLLECTION+" FILTER a."+Story.PATH+" == @path RETURN a";
	private static final String QUERY_ALBUM_TO_STORY = "FOR a in NEIGHBORS("+Album.COLLECTION+", "+ALBUM_RELATIONS_COLLECTION+", @id, 'outbound') RETURN a.vertex";

	private ArangoDriver photostashArangoDriver;
	private boolean genesisComplete;

	private PhotostashDatabase() {
		genesisComplete = false;
	}

	private void verifyDatabaseDriver() throws PhotostashDatabaseException {
		if (photostashArangoDriver == null) {
			/**
			 * Need to create database connection
			 */
			createDatabaseDriver();
			if (photostashArangoDriver == null) {
				/**
				 * Database creation failure
				 */
				throw new PhotostashDatabaseException("ERROR: Could not connect to database: "+DatabaseConfiguration.INSTANCE.toString());
			} else {
				if (!genesisComplete) {
					/**
					 * Need to perform Genesis
					 */
					genesisDatabase();
					if(!genesisComplete){
						throw new PhotostashDatabaseException("ERROR: Could not create default Photostash database architecture.");
					}
				}
			}
		}
	}

	private synchronized void createDatabaseDriver() {
		ArangoConfigure arangoConfigure = new ArangoConfigure();
		arangoConfigure.setArangoHost(new ArangoHost(DatabaseConfiguration.INSTANCE.getHost(), DatabaseConfiguration.INSTANCE.getPort()));
		arangoConfigure.init();

		try {
			ArangoDriver systemArangoDriver = new ArangoDriver(arangoConfigure);
			StringsResultEntity stringsResultEntity = systemArangoDriver.getDatabases();
			if (!stringsResultEntity.getResult().contains(DatabaseConfiguration.INSTANCE.getDatabase())) {
				systemArangoDriver.createDatabase(DatabaseConfiguration.INSTANCE.getDatabase());
			}
		} catch (ArangoException e) {
			LOGGER.error("Cannot connect/create Photostash Database: " + e);
		}

		photostashArangoDriver = new ArangoDriver(arangoConfigure, DatabaseConfiguration.INSTANCE.getDatabase());
	}

	private synchronized void genesisDatabase() {
		try {
			/**
			 * Get List of Collections
			 */
			List<String> collections = new LinkedList<String>();
			for(CollectionEntity collectionEntity: photostashArangoDriver.getCollections(true).getCollections()){
				collections.add(collectionEntity.getName());
			}
			/**
			 * Create Album Collection
			 */
			if(!collections.contains(Album.COLLECTION)){
				photostashArangoDriver.createCollection(Album.COLLECTION);
				photostashArangoDriver.createIndex(Album.COLLECTION, IndexType.HASH, true, Album.PATH);
			}
			/**
			 * Create Story Collection
			 */
			if(!collections.contains(Story.COLLECTION)){
				photostashArangoDriver.createCollection(Story.COLLECTION);
				photostashArangoDriver.createIndex(Story.COLLECTION, IndexType.HASH, true, Story.PATH);
			}
			
			CollectionOptions edgeCollectionOptions = new CollectionOptions();
			edgeCollectionOptions.setType(CollectionType.EDGE);
			/**
			 * Create Album->Story Graph/Edge
			 */
			if(!collections.contains(ALBUM_RELATIONS_COLLECTION)){
				photostashArangoDriver.createCollection(ALBUM_RELATIONS_COLLECTION, edgeCollectionOptions);
			}
			genesisComplete = true;
		} catch (ArangoException e) {
			LOGGER.error("Photostash Database Genesis Failure: " + e);
		}
	}
	
	public List<Album> getAlbums() throws PhotostashDatabaseException {
		verifyDatabaseDriver();
		
		try {
			return photostashArangoDriver.executeSimpleAllDocuments(Album.COLLECTION, 0, 0, Album.class).asEntityList();
		} catch (ArangoException e) {
			throw new PhotostashDatabaseException(e);
		}
	}
	
	public Album getAlbum(String albumId) throws PhotostashDatabaseException {
		verifyDatabaseDriver();
		try {
			return photostashArangoDriver.getDocument(Album.COLLECTION, albumId, Album.class).getEntity();
		} catch (ArangoException e) {
			throw new PhotostashDatabaseException(e);
		}	
	}

	public Album findAlbum(String path) throws PhotostashDatabaseException {
		verifyDatabaseDriver();
		try {
			Map<String, Object> bindVars = new MapBuilder().put("path", path).get();
			List<Album> albums = photostashArangoDriver.executeDocumentQuery(QUERY_ALBUM, bindVars, photostashArangoDriver.getDefaultAqlQueryOptions(), Album.class).asEntityList();
			if(albums.size() > 0){
				return albums.get(0);
			}else{
				return null;
			}
		} catch (ArangoException e) {
			throw new PhotostashDatabaseException(e);
		}	
	}

	public Album createAlbum(Album album) throws PhotostashDatabaseException {
		verifyDatabaseDriver();
		try {
			return photostashArangoDriver.createDocument(Album.COLLECTION, album, true, true).getEntity();
		} catch (ArangoException e) {
			throw new PhotostashDatabaseException(e);
		}
	}
	
	public List<Story> getStories() throws PhotostashDatabaseException {
		verifyDatabaseDriver();
		
		try {
			return photostashArangoDriver.executeSimpleAllDocuments(Story.COLLECTION, 0, 0, Story.class).asEntityList();
		} catch (ArangoException e) {
			throw new PhotostashDatabaseException(e);
		}
	}
	
	public List<Story> getStories(final Album album) throws PhotostashDatabaseException {
		verifyDatabaseDriver();
		try {
			Map<String, Object> bindVars = new MapBuilder().put("id", album.getDocumentAddress()).get();
			return photostashArangoDriver.executeDocumentQuery(QUERY_ALBUM_TO_STORY, bindVars, photostashArangoDriver.getDefaultAqlQueryOptions(), Story.class).asEntityList();
		} catch (ArangoException e) {
			throw new PhotostashDatabaseException(QUERY_ALBUM_TO_STORY+" "+e);
		}
	}
	
	public Story getStory(String storyId) throws PhotostashDatabaseException {
		verifyDatabaseDriver();
		try {
			return photostashArangoDriver.getDocument(Story.COLLECTION, storyId, Story.class).getEntity();
		} catch (ArangoException e) {
			throw new PhotostashDatabaseException(e);
		}	
	}

	public Story findStory(String path) throws PhotostashDatabaseException {
		verifyDatabaseDriver();
		try {
			Map<String, Object> bindVars = new MapBuilder().put("path", path).get();
			List<Story> stories = photostashArangoDriver.executeDocumentQuery(QUERY_STORY, bindVars, photostashArangoDriver.getDefaultAqlQueryOptions(), Story.class).asEntityList();
			if(stories.size() > 0){
				return stories.get(0);
			}else{
				return null;
			}
		} catch (ArangoException e) {
			throw new PhotostashDatabaseException(e);
		}
		
	}

	public Story createStory(Story story) throws PhotostashDatabaseException {
		verifyDatabaseDriver();
		try {
			return photostashArangoDriver.createDocument(Story.COLLECTION, story).getEntity();
		} catch (ArangoException e) {
			throw new PhotostashDatabaseException(e);
		}
	}

	public void relateAlbumToStory(Album album, Story story) throws PhotostashDatabaseException {
		verifyDatabaseDriver();
		try {
			LOGGER.debug(ALBUM_RELATIONS_COLLECTION+": "+album.getDocumentAddress()+" -> "+story.getDocumentAddress());
			photostashArangoDriver.createEdge(DatabaseConfiguration.INSTANCE.getDatabase(), ALBUM_RELATIONS_COLLECTION, new Object(), album.getDocumentAddress(), story.getDocumentAddress(), false, false);
		} catch (ArangoException e) {
			throw new PhotostashDatabaseException(e);
		}
	}
}
