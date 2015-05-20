package com.ctrengine.photostash.database;

import java.util.Arrays;
import java.util.Collections;
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
import com.ctrengine.photostash.models.Document;
import com.ctrengine.photostash.models.FileDocument;
import com.ctrengine.photostash.models.PhotographDocument;
import com.ctrengine.photostash.models.PhotographCacheDocument;
import com.ctrengine.photostash.models.RelateDocument;
import com.ctrengine.photostash.models.Story;

public enum PhotostashDatabase {
	INSTANCE;

	private static final ALogger LOGGER = Logger.of("database");

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
				throw new PhotostashDatabaseException("ERROR: Could not connect to database: " + DatabaseConfiguration.INSTANCE.toString());
			} else {
				if (!genesisComplete) {
					/**
					 * Need to perform Genesis
					 */
					genesisDatabase();
					if (!genesisComplete) {
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
			for (CollectionEntity collectionEntity : photostashArangoDriver.getCollections(true).getCollections()) {
				collections.add(collectionEntity.getName());
			}
			/**
			 * Create Album Collection
			 */
			if (!collections.contains(Album.COLLECTION)) {
				photostashArangoDriver.createCollection(Album.COLLECTION);
				photostashArangoDriver.createIndex(Album.COLLECTION, IndexType.HASH, true, Album.PATH);
			}
			/**
			 * Create Story Collection
			 */
			if (!collections.contains(Story.COLLECTION)) {
				photostashArangoDriver.createCollection(Story.COLLECTION);
				photostashArangoDriver.createIndex(Story.COLLECTION, IndexType.HASH, true, Story.PATH);
			}
			/**
			 * Create PhotographDocument Collection
			 */
			if (!collections.contains(PhotographDocument.COLLECTION)) {
				photostashArangoDriver.createCollection(PhotographDocument.COLLECTION);
				photostashArangoDriver.createIndex(PhotographDocument.COLLECTION, IndexType.HASH, true, PhotographDocument.PATH);
			}
			/**
			 * Create PhotographDocument Cache Collection
			 */
			if (!collections.contains(PhotographCacheDocument.COLLECTION)) {
				photostashArangoDriver.createCollection(PhotographCacheDocument.COLLECTION);
				photostashArangoDriver.createIndex(PhotographCacheDocument.COLLECTION, IndexType.SKIPLIST, true, PhotographCacheDocument.SIZE);
				photostashArangoDriver.createIndex(PhotographCacheDocument.COLLECTION, IndexType.SKIPLIST, true, PhotographCacheDocument.SQUARE_SIZE);
			}

			CollectionOptions edgeCollectionOptions = new CollectionOptions();
			edgeCollectionOptions.setType(CollectionType.EDGE);
			/**
			 * Create Album->Story Graph/Edge
			 */
			if (!collections.contains(Album.RELATE_COLLECTION)) {
				photostashArangoDriver.createCollection(Album.RELATE_COLLECTION, edgeCollectionOptions);
			}
			if (!collections.contains(Story.RELATE_COLLECTION)) {
				photostashArangoDriver.createCollection(Story.RELATE_COLLECTION, edgeCollectionOptions);
			}
			if (!collections.contains(PhotographDocument.RELATE_COLLECTION)) {
				photostashArangoDriver.createCollection(PhotographDocument.RELATE_COLLECTION, edgeCollectionOptions);
			}
			genesisComplete = true;
		} catch (ArangoException e) {
			LOGGER.error("Photostash Database Genesis Failure: " + e);
		}
	}

	public List<Album> getAlbums() throws PhotostashDatabaseException {
		return getDocuments(Album.COLLECTION, Album.class);
	}

	public Album getAlbum(String albumId) throws PhotostashDatabaseException {
		return getDocument(Album.COLLECTION, albumId, Album.class);
	}

	public Album findAlbum(String path) throws PhotostashDatabaseException {
		return findPathDocument(Album.COLLECTION, path, Album.class);
	}

	public Story getStory(String storyId) throws PhotostashDatabaseException {
		return getDocument(Story.COLLECTION, storyId, Story.class);
	}
	
	public List<Story> getStories() throws PhotostashDatabaseException {
		return getDocuments(Story.COLLECTION, Story.class);
	}

	public Story findStory(String path) throws PhotostashDatabaseException {
		return findPathDocument(Story.COLLECTION, path, Story.class);
	}	
	
	public PhotographDocument getPhotograph(String photographId) throws PhotostashDatabaseException {
		return getDocument(PhotographDocument.COLLECTION, photographId, PhotographDocument.class);
	}
	
	public List<PhotographDocument> getPhotographs() throws PhotostashDatabaseException {
		return getDocuments(PhotographDocument.COLLECTION, PhotographDocument.class);
	}
	
	public PhotographDocument findPhotograph(String path) throws PhotostashDatabaseException {
		return findPathDocument(PhotographDocument.COLLECTION, path, PhotographDocument.class);
	}

	public <R extends RelateDocument, D extends Document> List<D> getRelatedDocuments(R relateDocument, Class<D> clazz) throws PhotostashDatabaseException {
		verifyDatabaseDriver();
		final String QUERY_RELATE = "FOR a in NEIGHBORS(" + relateDocument.getCollection() + ", " + relateDocument.getRelateCollection() + ", @id, 'outbound') RETURN a.vertex";
		try {
			Map<String, Object> bindVars = new MapBuilder().put("id", relateDocument.getDocumentAddress()).get();
			return photostashArangoDriver.executeDocumentQuery(QUERY_RELATE, bindVars, photostashArangoDriver.getDefaultAqlQueryOptions(), clazz).asEntityList();
		} catch (ArangoException e) {
			throw new PhotostashDatabaseException(QUERY_RELATE + " " + e);
		}
	}

	public <D extends Document> D createDocument(D document) throws PhotostashDatabaseException {
		verifyDatabaseDriver();
		try {
			return photostashArangoDriver.createDocument(document.getCollection(), document).getEntity();
		} catch (ArangoException e) {
			throw new PhotostashDatabaseException(e);
		}
	}

	public <R extends RelateDocument, D extends Document> void relateDocumentToDocument(R relateDocument, D document) throws PhotostashDatabaseException {
		verifyDatabaseDriver();
		try {
			LOGGER.debug(relateDocument.getRelateCollection() + ": " + relateDocument.getDocumentAddress() + " -> " + document.getDocumentAddress());
			photostashArangoDriver.createEdge(DatabaseConfiguration.INSTANCE.getDatabase(), relateDocument.getRelateCollection(), new Object(), relateDocument.getDocumentAddress(), document.getDocumentAddress(), false, false);
		} catch (ArangoException e) {
			throw new PhotostashDatabaseException(e);
		}
	}
	
	private <D extends Document> List<D> getDocuments(String collection, Class<D> clazz) throws PhotostashDatabaseException {
		verifyDatabaseDriver();
		try {
			List<D> documentList = photostashArangoDriver.executeSimpleAllDocuments(collection, 0, 0, clazz).asEntityList();
			Collections.sort(documentList);
			return documentList;
		} catch (ArangoException e) {
			throw new PhotostashDatabaseException(e);
		}
	}

	private <D extends FileDocument> D findPathDocument(String collection, String path, Class<D> clazz) throws PhotostashDatabaseException {
		verifyDatabaseDriver();
		final String QUERY_PATH = "FOR a IN "+collection+" FILTER a." + FileDocument.PATH + " == @path RETURN a";
		try {
			Map<String, Object> bindVars = new MapBuilder().put("path", path).get();
			List<D> documents = photostashArangoDriver.executeDocumentQuery(QUERY_PATH, bindVars, photostashArangoDriver.getDefaultAqlQueryOptions(), clazz).asEntityList();
			if (documents.size() > 0) {
				return documents.get(0);
			} else {
				return null;
			}
		} catch (ArangoException e) {
			throw new PhotostashDatabaseException(e);
		}
	}

	private <D extends Document> D getDocument(String collection, String id, Class<D> clazz) throws PhotostashDatabaseException {
		verifyDatabaseDriver();
		try {
			return photostashArangoDriver.getDocument(collection, id, clazz).getEntity();
		} catch (ArangoException e) {
			throw new PhotostashDatabaseException(e);
		}
	}
}
