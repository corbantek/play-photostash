class @Browser
	constructor: ->
		#Bootup Lightbox
		$(document).delegate('*[data-toggle="lightbox"]', 'click', (event) ->
			event.preventDefault()
			$(this).ekkoLightbox()
		)
		@breadcrumb = $('#photostash-breadcrumb')
		@breadcrumbHome = $('#photostash-breadcrumb-home')
		@breadcrumbAlbum = null

		@photostashAlbumList = $('#photostash-albumlist')
		@photostashStories = $('#photostash-stories')
		@photostashGallery = $('#photostash-gallery')
		
		@displayAlbums()

	displayAlbums: ->
		@photostashStories.hide()
		@photostashGallery.hide()
		@photostashAlbumList.empty()
		$.ajax({
			method: 'GET',
			url: jsRoutesAlbumController.com.ctrengine.photostash.controllers.api.AlbumController.getAlbums().url,
			dataType: 'json'
			, success: (albums) =>
				for album in albums
					albumLink = $('<a href="#" class="list-group-item">')
					albumLink.text(album.name)
					albumLink.click({album: album}, (event) =>
						@displayAlbum(event.data.album)
					)
					@photostashAlbumList.append(albumLink)
				return
			, error: (jqXHR, textStatus, errorThrown) ->
				return
		})
		return

	displayAlbum: (album) ->
		# Setup Breadcrumb
		@breadcrumb.empty()
		@breadcrumb.append(@breadcrumbHome)
		@breadcrumbHome.removeClass('active')
		breadcrumbAlbum = $('<li class="active">').text(album.name)
		@breadcrumb.append(breadcrumbAlbum)

		# Hide Album List
		@photostashAlbumList.hide()
		@photostashGallery.hide()

		@photostashStories.show()
		$.ajax({
			method: 'GET',
			url: album.link,
			dataType: 'json'
			, success: (singleAlbum) =>		
				@photostashStories.empty()
				for story in singleAlbum.stories
					storyCover = $('<div class="col-sm-6 col-md-3">')
					storyLink = $('<a href="#" class="thumbnail">')
					storyLink.append($('<img src="'+story.coverLink+'/resize/180" alt="'+story.name+'" class="img-rounded">')).append($('<div class="caption">)').append($('<h5>').text(story.name))) 
					storyCover.append(storyLink)
					storyLink.click({album: singleAlbum, story: story}, (event) =>
						@displayStory(event.data.album, event.data.story)
					)
					@photostashStories.append(storyCover)
				
				return
			, error: (jqXHR, textStatus, errorThrown) ->
				return
		})
		return

	displayStory: (album, story) ->
		# Setup Breadcrumb
		@breadcrumb.empty()
		@breadcrumb.append(@breadcrumbHome)
		@breadcrumbHome.removeClass('active')
		breadcrumbAlbum = $('<li>').append($('<a href="#">').text(album.name))
		breadcrumbAlbum.click({album: album}, (event) =>
			@displayAlbum(event.data.album)
		)
		@breadcrumb.append(breadcrumbAlbum)
		breadcrumbStory = $('<li class="active">').text(story.name)
		@breadcrumb.append(breadcrumbStory)

		# Hide Story List
		@photostashStories.hide()

		@photostashGallery.empty()
		@photostashGallery.show()
		$.ajax({
			method: 'GET',
			url: story.link,
			dataType: 'json'
			, success: (singleStory) =>
				@photostashGallery.empty()
				for photograph in singleStory.photographs
					photographLink = $('<a href="'+photograph.link+'/image/resize/800" data-toggle="lightbox" data-gallery="'+story.storyId+'" data-title="'+photograph.name+'" data-type="image" class="col-sm-4">')
					photographLink.append($('<img src="'+photograph.link+'/image/resize/360" class="img-responsive" style="width: 360px">'))
					@photostashGallery.append(photographLink)
				return
			, error: (jqXHR, textStatus, errorThrown) ->
				return
		})

		return


