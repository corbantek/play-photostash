class @Browser
	PHOTO_THUMBNAIL_SIZE = 360

	constructor: ->
		@breadcrumb = $('#photostash-breadcrumb')
		@breadcrumbHome = $('#photostash-breadcrumb-home')
		@breadcrumbAlbum = null

		@photostashAlbumList = $('#photostash-albumlist')
		@photostashStories = $('#photostash-stories')
		@photostashGallery = $('#photostash-gallery')

		# Setup HTML5 Browser Buttons for Single Page
		window.addEventListener("popstate", (event) =>
			console.log(event)
			if $.fancybox.isOpen
				history.forward()
				$.fancybox.close()
			else
				if event.state?
					if event.state.album?
						if event.state.story?
							@displayPhotographs(event.state.album, event.state.story)
						else
							@displayStories(event.state.album)
					else
						@displayAlbums()
				else
					@displayAlbums()
			return
		)

		# Handle Bookmarks Properly
		urlParameter = window.location.hash.substr(1);
		if urlParameter? and urlParameter isnt ""
			parameter = urlParameter.split('=')
			if parameter.length is 2
				switch parameter[0]
					when "albumId"
						$.ajax({
							method: 'GET',
							cache: false,
							url: jsRoutesAlbumController.com.ctrengine.photostash.controllers.api.AlbumController.getAlbum(parameter[1]).url,
							dataType: 'json'
							, success: (album) =>		
								@displayStories(album)
								return
							, error: (jqXHR, textStatus, errorThrown) ->
								@displayAlbums()
								return
						})
					when "storyId"
						$.ajax({
							method: 'GET',
							cache: false,
							url: jsRoutesStoryController.com.ctrengine.photostash.controllers.api.StoryController.getStory(parameter[1]).url,
							dataType: 'json'
							, success: (story) =>		
								@displayPhotographs(null, story)
								return
							, error: (jqXHR, textStatus, errorThrown) ->
								@displayAlbums()
								return
						})
					else @displayAlbums()
		else
			@displayAlbums()

	displayAlbums: ->
		@photostashStories.hide()
		@photostashStories.empty()

		@photostashGallery.hide()
		@photostashGallery.empty()

		@photostashAlbumList.show()
		@photostashAlbumList.empty()
		$.ajax({
			method: 'GET',
			cache: false,
			url: jsRoutesAlbumController.com.ctrengine.photostash.controllers.api.AlbumController.getAlbums().url,
			dataType: 'json'
			, success: (albums) =>
				for album in albums
					albumLink = $('<a class="list-group-item">')
					albumLink.text(album.name)
					albumLink.click({album: album}, (event) =>
						@displayStories(event.data.album)
						history.pushState({album: event.data.album}, null, '#albumId='+event.data.album.albumId)
					)
					@photostashAlbumList.append(albumLink)
				return
			, error: (jqXHR, textStatus, errorThrown) ->
				return
		})
		return

	displayStories: (album) ->
		# Setup Breadcrumb
		@breadcrumb.empty()
		@breadcrumb.append(@breadcrumbHome)
		@breadcrumbHome.removeClass('active')
		breadcrumbAlbum = $('<li class="active">').text(album.name)
		@breadcrumb.append(breadcrumbAlbum)

		@photostashAlbumList.hide()
		@photostashAlbumList.empty()

		@photostashGallery.hide()
		@photostashGallery.empty()

		@photostashStories.show()
		$.ajax({
			method: 'GET',
			cache: false,
			url: album.link,
			dataType: 'json'
			, success: (singleAlbum) =>		
				@photostashStories.empty()
				columnNumber = 4
				for story in singleAlbum.stories
					if columnNumber is 4
						row = $('<div class="row">')
						@photostashStories.append(row)
						columnNumber = 1
					else
						columnNumber += 1
					storyCover = $('<div class="col-sm-6 col-md-3">')
					storyLink = $('<a class="thumbnail">')
					if story.coverLink?
						storyLink.append($('<img src="'+story.coverLink+'/resize/180" alt="'+story.name+'" class="img-rounded" style="max-width: 180px; max-height: 180px;">')).append($('<div class="caption">)').append($('<h6>').text(story.name))) 
					else
						storyLink.append($('<div class="caption">)').append($('<h5>').text(story.name))) 
					storyCover.append(storyLink)
					storyLink.click({album: singleAlbum, story: story}, (event) =>
						$.ajax({
							method: 'POST',
							cache: false,
							url: jsRoutesStoryController.com.ctrengine.photostash.controllers.api.StoryController.resizeStoryPhotographs(event.data.story.storyId, PHOTO_THUMBNAIL_SIZE).url,
							dataType: 'json'
						})
						@displayPhotographs(event.data.album, event.data.story)
						history.pushState({album: event.data.album, story: event.data.story}, null, '#storyId='+event.data.story.storyId)
					)
					row.append(storyCover)
				return
			, error: (jqXHR, textStatus, errorThrown) ->
				return
		})
		return

	displayPhotographs: (album, story) ->
		# Setup Breadcrumb
		@breadcrumb.empty()
		@breadcrumb.append(@breadcrumbHome)
		@breadcrumbHome.removeClass('active')
		if album?
			breadcrumbAlbum = $('<li>').append($('<a>').text(album.name))
			breadcrumbAlbum.click({album: album}, (event) =>
				@displayStories(event.data.album)
				history.pushState({album: event.data.album}, null, '#albumId='+event.data.album.albumId)
			)
			@breadcrumb.append(breadcrumbAlbum)
		breadcrumbStory = $('<li class="active">').text(story.name)
		@breadcrumb.append(breadcrumbStory)

		@photostashAlbumList.hide()
		@photostashAlbumList.empty()

		@photostashStories.hide()
		@photostashStories.empty()

		@photostashGallery.show()
		$.ajax({
			method: 'GET',
			cache: false,
			url: story.link,
			dataType: 'json'
			, success: (singleStory) =>
				N_COLUMNS = 3
				@photostashGallery.empty()
				columnNumber = N_COLUMNS
				for photograph in singleStory.photographs
					if columnNumber is N_COLUMNS
						row = $('<div class="row">')
						@photostashGallery.append(row)
						columnNumber = 1
					else
						columnNumber += 1
					title = "Title: "+photograph.name
					if photograph.dateTaken?
						dateTaken = new Date(photograph.dateTaken)
						title = "Taken: " + $.format.date(dateTaken, "MM.dd.yyyy HH:mm ") + " -- "+title
					photographLink = $('<a href="'+photograph.link+'/image/resize/1024" rel="'+story.storyId+'" class="fancybox col-sm-4" title="'+title+'">')
					#photographLink = $('<a href="'+photograph.link+'/image/resize/1024" rel="group" class="fancybox">')
					photographLink.append($('<img src="'+photograph.link+'/image/resize/'+PHOTO_THUMBNAIL_SIZE+'" class="img-responsive img-thumbnail" style="max-width: 360px; max-height: 360px;">'))
					row.append(photographLink)
				# Setup Fancybox
				$(".fancybox").fancybox({
					padding: 0,
					type: 'image',
					afterLoad: ->
						originalImageLink = @.href.split("/resize")
						#photographUrlSplit = originalImageLink[0].split("/image")[0].split("/")
						#photograph = photographUrlSplit[photographUrlSplit.length-1]
						#{}history.pushState({photograph: photograph}, null, '#photographId='+photograph)
						@.title = '<a href="'+originalImageLink[0]+'" target="_blank">Download</a> '+@.title
				})
				return
			, error: (jqXHR, textStatus, errorThrown) ->
				return
		})
		return
