package com.ctrengine.photostash.shoebox;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.routing.RoundRobinPool;

import com.ctrengine.photostash.database.DatabaseException;
import com.ctrengine.photostash.database.PhotostashDatabase;
import com.ctrengine.photostash.models.AlbumDocument;
import com.ctrengine.photostash.shoebox.ShoeboxMessages.OrganizeCompleteMessage;
import com.ctrengine.photostash.shoebox.ShoeboxMessages.OrganizeMessage;

public class AlbumActor extends UntypedActor {
	private static class OrganizeAlbumRequester {
		private final ActorRef requester;
		private final File albumFile;

		public OrganizeAlbumRequester(ActorRef requester, File albumFile) {
			super();
			this.requester = requester;
			this.albumFile = albumFile;
		}

		public ActorRef getRequester() {
			return requester;
		}

		public File getAlbumFile() {
			return albumFile;
		}
	}

	/**
	 * Basic Needs
	 */
	private final PhotostashDatabase database;
	private ActorRef storyRouter;

	/**
	 * Organizing Tracking
	 */
	private Map<AlbumDocument, Set<File>> storiesOrganizing;
	private Map<AlbumDocument, OrganizeAlbumRequester> organizingRequestMap;

	private AlbumActor() {
		database = PhotostashDatabase.INSTANCE;
		storyRouter = getContext().actorOf(new RoundRobinPool(5).props(Props.create(StoryActor.class)), "story-router");
		storiesOrganizing = new HashMap<AlbumDocument, Set<File>>();
		organizingRequestMap = new HashMap<AlbumDocument, OrganizeAlbumRequester>();
	}

	@Override
	public void onReceive(Object message) throws Exception {
		if (message instanceof OrganizeCompleteMessage) {
			organizeComplete((OrganizeCompleteMessage) message);
		} else if (message instanceof OrganizeMessage) {
			organize((OrganizeMessage) message);
		} else {
			unhandled(message);
		}
	}

	private void organizeComplete(OrganizeCompleteMessage organizeCompleteMessage) {
		Set<File> stories = storiesOrganizing.get(organizeCompleteMessage.getAbstractFileDocument());
		if (stories == null) {
			Shoebox.LOGGER.warn("Album not registered to organize:" + organizeCompleteMessage.getAbstractFileDocument().getKey());
		} else {
			if (!stories.remove(organizeCompleteMessage.getFile())) {
				Shoebox.LOGGER.warn("Completed organize story not found: " + organizeCompleteMessage.getFile().getName());
			}
			if (stories.isEmpty()) {
				Shoebox.LOGGER.info("Organize Album Complete for " + organizeCompleteMessage.getAbstractFileDocument().getKey());
				/**
				 * No more stories in this album are organizing, tell the
				 * original sender and clean up memory
				 */
				storiesOrganizing.remove(organizeCompleteMessage.getAbstractFileDocument());
				OrganizeAlbumRequester organizeAlbumRequester = organizingRequestMap.remove(organizeCompleteMessage.getAbstractFileDocument());
				if (organizeAlbumRequester != null) {
					organizeAlbumRequester.getRequester().tell(new ShoeboxMessages.OrganizeCompleteMessage(organizeAlbumRequester.getAlbumFile()), getSelf());
				} else {
					Shoebox.LOGGER.warn("Organize requester not found for: " + organizeCompleteMessage.getAbstractFileDocument().getName());
				}
			}
		}
	}

	private AlbumDocument getAlbumDocument(File albumDirectory) throws ShoeboxException {
		/**
		 * Verify albumDocument has a database entry
		 */
		try {
			AlbumDocument albumDocument = database.findAlbum(albumDirectory.getAbsolutePath());
			if (albumDocument == null) {
				/**
				 * Create new AlbumDocument Record
				 */
				albumDocument = new AlbumDocument(albumDirectory);
				albumDocument = database.createDocument(albumDocument);
			}
			return albumDocument;
		} catch (DatabaseException e) {
			final String message = "Unable to find/create albumDocument: '" + albumDirectory.getAbsolutePath() + "': " + e.getMessage();
			Shoebox.LOGGER.error(message);
			throw new ShoeboxException(message);
		}
	}

	private void organize(OrganizeMessage organizeMessage) {
		File albumDirectory = organizeMessage.getFile();
		Set<File> storyFiles = new HashSet<File>();
		try {
			AlbumDocument albumDocument = getAlbumDocument(albumDirectory);
			/**
			 * Store this organize requester so we can respond later when
			 * organizing is finished
			 */
			organizingRequestMap.put(albumDocument, new OrganizeAlbumRequester(getSender(), albumDirectory));
			storiesOrganizing.put(albumDocument, storyFiles);

			for (File storyDirectory : organizeMessage.getFile().listFiles()) {
				if (storyDirectory.isDirectory()) {
					Shoebox.LOGGER.info("Found Story: " + storyDirectory.getAbsolutePath());
					storyRouter.tell(new OrganizeMessage(albumDocument, storyDirectory), getSelf());
					storyFiles.add(storyDirectory);
				}
			}

			/**
			 * Need to look for albums that have been removed
			 */
		} catch (ShoeboxException e) {
			final String message = "Unable to organize album: '" + albumDirectory.getAbsolutePath() + "': " + e.getMessage();
			Shoebox.LOGGER.error(message);
		}
		if (storyFiles.isEmpty()) {
			/**
			 * Something wrong happened and there are no story files to
			 * organize, so tell the sender we are done
			 */
			Shoebox.LOGGER.warn("Unable to organize album: "+albumDirectory.getName()+".  No story directories.");
			getSender().tell(new ShoeboxMessages.OrganizeCompleteMessage(albumDirectory), getSender());
		}
	}
}
