package com.example.axs_player.playback

import android.content.Context
import android.net.Uri
import android.os.Looper
import android.util.Log
import com.example.axs_player.AxsApplication
import com.example.axs_player.data.library.AxsContainer
import com.example.axs_player.data.library.AxsContainerDataSourceFactory
import com.example.axs_player.data.library.FragmentedMp4DurationReader
import com.example.axs_player.data.library.LibraryFolder
import com.example.axs_player.data.library.LibraryLayout
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DataSource
import androidx.media3.extractor.ExtractorsFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private const val LOG_TAG = "PlaybackController"
private const val TICK_INTERVAL_MS = 50L
private const val RESTART_THRESHOLD_MS = 3_000L
private val AUDIO_ATTRIBUTES = AudioAttributes.Builder()
    .setUsage(C.USAGE_MEDIA)
    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
    .build()

private const val GAPLESS_LEAD_MS = 150L

class PlaybackController(context: Context) {

    private val dataSourceFactory = LengthAwareDataSource.Factory(context)
    private val fragmentedMp4Reader = FragmentedMp4DurationReader(context.contentResolver)

    // Cache for container blob info: albumId -> Map<blobName, Pair<offset, length>>
    private val containerBlobCache = mutableMapOf<String, Map<String, Pair<Long, Long>>>()

    // Cache for container track streaming info: trackId -> ContainerStreamInfo
    private val containerTrackInfoCache = mutableMapOf<String, ContainerStreamInfo>()

    // Version bumped whenever container track info cache changes (to invalidate SessionPlayer playlist cache)
    private var containerTracksVersion = 0L

    fun containerTracksVersion(): Long = containerTracksVersion

    /**
     * Awaits resolution of a container track.
     * Returns immediately if track is already resolved or not a container track.
     * Use with timeout: `withTimeoutOrNull(5000) { playbackController.awaitTrackResolution(trackId) }`
     */
    suspend fun awaitTrackResolution(trackId: String) {
        val deferred = trackResolutionDeferreds[trackId] ?: return
        deferred.await()
    }

    // CompletableDeferred for each track resolution - allows await() instead of polling
    private val trackResolutionDeferreds = mutableMapOf<String, CompletableDeferred<Unit>>()

    private fun getOrCreateResolutionDeferred(trackId: String): CompletableDeferred<Unit> {
        return trackResolutionDeferreds.getOrPut(trackId) { CompletableDeferred() }
    }

    private fun completeResolutionDeferred(trackId: String) {
        trackResolutionDeferreds.remove(trackId)?.complete(Unit)
    }

    private var crossfadeMs = 0L
    private var isGapless = false

    data class ContainerStreamInfo(
        val containerUri: Uri,
        val blobOffset: Long,
        val blobLength: Long,
        val durationMs: Long? = null
    )

    // Cache for container manifest data to avoid re-reading the same container
    data class ContainerManifestCache(
        val containerUri: Uri,
        val tracks: Map<String, Pair<Long, Long>>,  // blobName -> (offset, length)
        val trackDurations: Map<String, Long>,      // blobName -> durationMs
        val containerUriCached: Uri
    )

    private val containerManifestCache = mutableMapOf<String, ContainerManifestCache>()

    // Public accessor for SessionPlayer to get container track duration
    fun getContainerTrackDuration(trackId: String): Long? = containerTrackInfoCache[trackId]?.durationMs

    // Public accessor for SessionPlayer to get container track info (for duration estimation)
    fun getContainerTrackInfo(trackId: String): ContainerStreamInfo? = containerTrackInfoCache[trackId]

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val playbackScope: CoroutineScope = scope
    private val audioProcessors = List(2) { PlaybackAudioProcessor() }
    private val players = audioProcessors.map { processor ->
        ExoPlayer.Builder(context, PlaybackRenderersFactory(context, processor))
            .setLooper(Looper.getMainLooper())
            .setAudioAttributes(AUDIO_ATTRIBUTES, false)
            .setHandleAudioBecomingNoisy(true)
            .build()
    }
    private val _activePlayer = MutableStateFlow<Player>(players[0])
    val activePlayer: StateFlow<Player> = _activePlayer.asStateFlow()
    private var active = players[0]
        set(value) {
            field = value
            _activePlayer.value = value
        }
    private val standby: ExoPlayer get() = if (active === players[0]) players[1] else players[0]
    private var fadingOut: ExoPlayer? = null
    private var fadeDurationMs = 0L
    private var ticker: Job? = null

    private val queue = mutableListOf<QueueItem>()
    private var currentIndex = -1
    private var isPlaying = false
    private var nextQueueItemId = 0L
    private var isShuffleEnabled = false
    private var repeatMode = RepeatMode.OFF
    private var originalOrder: List<Long> = emptyList()

    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state.asStateFlow()
    val currentTrackId: StateFlow<String?> = state
        .map { it.currentItem?.track?.trackId }
        .stateIn(scope, SharingStarted.Eagerly, null)

    init {
        players.forEach { player ->
            player.addListener(PlayerEvents(player))
        }
        assignAudioFocus()
    }

    fun applySettings(settings: PlaybackSettings) {
        crossfadeMs = settings.crossfadeSeconds * 1_000L
        isGapless = settings.isGapless
        audioProcessors.forEach {
            it.isMono = settings.isMono
            it.isNormalized = settings.isNormalized
        }
    }

    fun setCurrentTrackVolume(volume: Float) {
        val normalized = volume.coerceIn(0f, 2f)
        val playerIndex = players.indexOf(active)
        audioProcessors[playerIndex].trackVolume = normalized
    }

    fun positionMs(): Long = if (currentIndex < 0) 0L else active.currentPosition

    fun durationMs(): Long = if (currentIndex < 0) C.TIME_UNSET else active.duration

    fun setQueue(tracks: List<PlayableTrack>, startIndex: Int = 0, playWhenReady: Boolean = true) {
        val items = tracks.map(::queueItem)
        queue.clear()
        queue += items
        originalOrder = items.map { it.id }
        if (queue.isEmpty()) {
            stopAll()
            return
        }
        isPlaying = playWhenReady
        var index = startIndex.coerceIn(queue.indices)
        if (isShuffleEnabled) {
            currentIndex = index
            shuffleAroundCurrent()
            index = currentIndex
        }

        // Resolve next track for preloading (fire-and-forget).
        // The start track is resolved inside startItem() before playback begins.
        val nextIdx = index + 1
        if (nextIdx < items.size) {
            val nextTrack = items[nextIdx].track
            if (nextTrack.isContainerTrack && nextTrack.resolvedUri == null) {
                scope.launch { resolveContainerTrack(nextTrack) }
            }
        }

        scope.launch {
            try {
                startItem(index)
                updateTicker()
            } catch (e: Exception) {
                Log.e(LOG_TAG, "Failed to start track", e)
            }
        }
    }

    private suspend fun resolveContainerTrack(track: PlayableTrack) {
        val deferred = getOrCreateResolutionDeferred(track.trackId)
        Log.d("PlaybackController", "Resolving container track: ${track.trackId}, blob: ${track.containerBlobName}, album: ${track.containerAlbumId}")
        val albumId = track.containerAlbumId!!
        val blobName = track.containerBlobName!!

        // Check preload cache first (keyed by albumId/blobName)
        val preloadKey = "$albumId/$blobName"
        val preloaded = containerTrackInfoCache[preloadKey]
        if (preloaded != null) {
            Log.d("PlaybackController", "Using preloaded container track info for $preloadKey")
            containerTrackInfoCache[track.trackId] = preloaded
            containerTracksVersion++
            withContext(Dispatchers.Main) {
                track.resolvedUri = track.uri
                preloaded.durationMs?.let { track.durationMs = it }
                Log.d("PlaybackController", "Container track ${track.trackId} ready from preload, duration: ${track.durationMs}")
                publish()
            }
            completeResolutionDeferred(track.trackId)
            return
        }

        val app = AxsApplication.getInstance()
        val layout = app.libraryFolder.layout()
            ?: run { Log.e("PlaybackController", "No layout for ${track.trackId}"); markResolved(track); completeResolutionDeferred(track.trackId); return }
        val album = app.database.libraryDao().albumEntity(albumId)
            ?: run { Log.e("PlaybackController", "No album for $albumId"); markResolved(track); completeResolutionDeferred(track.trackId); return }
        val containerFileName = album.containerFileName
            ?: run { Log.e("PlaybackController", "No container file for album ${album.id}"); markResolved(track); completeResolutionDeferred(track.trackId); return }

        Log.d("PlaybackController", "Container file: $containerFileName")

        val containerUri = app.libraryFolder.containerFileUri(layout, containerFileName)
            ?: run { Log.e("PlaybackController", "No container URI for $containerFileName"); markResolved(track); completeResolutionDeferred(track.trackId); return }

        // Try to load manifest from DB cache first (fast path)
        val manifestJson = album.containerManifestJson
        val manifestCache = if (manifestJson != null && manifestJson.isNotEmpty()) {
            parseManifestJson(manifestJson, containerUri)
        } else {
            // Fallback: read from container (slow path)
            loadContainerManifestCache(albumId, containerUri, app, layout)
        }

        val trackBlob = manifestCache.tracks[blobName]
            ?: run {
                Log.e("PlaybackController", "Blob $blobName NOT FOUND in container for track ${track.trackId}")
                markResolved(track)
                completeResolutionDeferred(track.trackId)
                return
            }

        val (offset, length) = trackBlob
        val manifestDurationMs = manifestCache.trackDurations[blobName]

        Log.d("PlaybackController", "Found blob: $blobName, offset: $offset, length: $length, duration: $manifestDurationMs")

        // Store container streaming info for this track
        containerTrackInfoCache[track.trackId] = ContainerStreamInfo(
            containerUri = manifestCache.containerUriCached,
            blobOffset = offset,
            blobLength = length,
            durationMs = manifestDurationMs
        )
        containerTracksVersion++

        // For container tracks, we don't need to extract - we use custom DataSource
        // Set a dummy resolvedUri to indicate it's ready (the actual reading is done by custom DataSource)
        withContext(Dispatchers.Main) {
            track.resolvedUri = track.uri // placeholder, actual reading via custom DataSource
            // Always update track duration from manifest if available (overrides stale DB value)
            if (manifestDurationMs != null && manifestDurationMs > 0L) {
                track.durationMs = manifestDurationMs
            }
            Log.d("PlaybackController", "Container track ${track.trackId} ready for streaming, duration: ${track.durationMs}")
            publish() // Trigger state update for duration/seekable change
        }
        completeResolutionDeferred(track.trackId)
    }

    private fun markResolved(track: PlayableTrack) {
        track.resolvedUri = track.uri
    }
    
    private fun parseManifestJson(json: String, containerUri: Uri): ContainerManifestCache {
        val trackMap = mutableMapOf<String, Pair<Long, Long>>()
        val durationMap = mutableMapOf<String, Long>()
        try {
            val parser = com.google.gson.JsonParser.parseString(json).asJsonObject
            val tracksArray = parser.getAsJsonArray("tracks")
            Log.d("PlaybackController", "parseManifestJson: tracksArray size = ${if (tracksArray != null) tracksArray.size() else 0}")
            if (tracksArray != null) {
                for (trackElement in tracksArray) {
                    val trackObj = trackElement.asJsonObject
                    val mediaObj = trackObj.getAsJsonObject("media")
                    if (mediaObj != null) {
                        val name = mediaObj.get("file_name")?.asString ?: ""
                        val offset = mediaObj.get("offset")?.asLong ?: 0L
                        val length = mediaObj.get("length")?.asLong ?: 0L
                        if (name.isNotEmpty()) {
                            trackMap[name] = offset to length
                            // Also extract duration_ms if available
                            val durationMs = trackObj.get("duration_ms")?.asLong
                            if (durationMs != null && durationMs > 0L) {
                                durationMap[name] = durationMs
                                Log.d("PlaybackController", "parseManifestJson: track=$name, offset=$offset, length=$length, duration_ms=$durationMs")
                            } else {
                                Log.d("PlaybackController", "parseManifestJson: track=$name, offset=$offset, length=$length, NO duration_ms")
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("PlaybackController", "Failed to parse manifest JSON, will fallback to container read", e)
            return ContainerManifestCache(
                containerUri = containerUri,
                tracks = emptyMap(),
                trackDurations = emptyMap(),
                containerUriCached = containerUri
            )
        }
        
        return ContainerManifestCache(
            containerUri = containerUri,
            tracks = trackMap,
            trackDurations = durationMap,
            containerUriCached = containerUri
        )
    }

    private fun loadContainerManifestCache(albumId: String, containerUri: Uri, app: AxsApplication, layout: LibraryLayout): ContainerManifestCache {
        val tempContainer = File(app.cacheDir, "axs_manifest_${System.currentTimeMillis()}.axs")
        return try {
            app.libraryFolder.copyUriTo(containerUri, tempContainer)
            val albumData = AxsContainer.read(tempContainer)
            
            // Build track map: blobName -> (offset, length)
            val trackMap = mutableMapOf<String, Pair<Long, Long>>()
            val durationMap = mutableMapOf<String, Long>()
            albumData.tracks.forEach { t ->
                trackMap[t.media.name] = t.media.offset to t.media.length
                t.duration?.let { durationMap[t.media.name] = it }
            }
            albumData.cover?.let { trackMap[it.name] = it.offset to it.length }
            albumData.backgroundTexture?.let { trackMap[it.name] = it.offset to it.length }
            albumData.photos.forEach { trackMap[it.name] = it.offset to it.length }
            albumData.mediaFiles.forEach { trackMap[it.name] = it.offset to it.length }
            
            val cache = ContainerManifestCache(
                containerUri = containerUri,
                tracks = trackMap,
                trackDurations = durationMap,
                containerUriCached = containerUri
            )
            containerManifestCache[albumId] = cache
            cache
        } finally {
            tempContainer.delete()
        }
    }

    /**
     * Preloads all container tracks for an album into cache.
     * Call when album screen opens to eliminate delay on first track play.
     * Runs in background with low priority.
     */
    fun preloadAlbumContainerTracks(albumId: String) {
        scope.launch(Dispatchers.IO) {
            try {
                val app = AxsApplication.getInstance()
                val layout = app.libraryFolder.layout() ?: return@launch
                val album = app.database.libraryDao().albumEntity(albumId) ?: return@launch
                val containerFileName = album.containerFileName ?: return@launch
                val containerUri = app.libraryFolder.containerFileUri(layout, containerFileName) ?: return@launch

                // Load manifest (uses DB cache if available, falls back to file read)
                val manifestJson = album.containerManifestJson
                val manifestCache = if (manifestJson != null && manifestJson.isNotEmpty()) {
                    parseManifestJson(manifestJson, containerUri)
                } else {
                    loadContainerManifestCache(albumId, containerUri, app, layout)
                }

                // Populate containerTrackInfoCache for all tracks in manifest
                manifestCache.tracks.forEach { (blobName, pair) ->
                    val (offset, length) = pair
                    val durationMs = manifestCache.trackDurations[blobName]
                    // Create a synthetic trackId for caching - we don't have the actual trackId here,
                    // but we can cache by blobName for the album
                    // The actual track resolution will use this cached data
                    containerTrackInfoCache["$albumId/$blobName"] = ContainerStreamInfo(
                        containerUri = manifestCache.containerUriCached,
                        blobOffset = offset,
                        blobLength = length,
                        durationMs = durationMs
                    )
                }
                containerTracksVersion++
                Log.d("PlaybackController", "Preloaded ${manifestCache.tracks.size} container tracks for album $albumId")
            } catch (e: Exception) {
                Log.w("PlaybackController", "Failed to preload album container tracks for $albumId", e)
            }
        }
    }

    fun toggleShuffle() {
        isShuffleEnabled = !isShuffleEnabled
        if (queue.isNotEmpty()) {
            if (isShuffleEnabled) {
                originalOrder = queue.map { it.id }
                shuffleAroundCurrent()
            } else {
                restoreOriginalOrder()
            }
        }
        onQueueChanged()
    }

    fun cycleRepeatMode() {
        repeatMode = repeatMode.next()
        applyRepeatMode()
        onQueueChanged()
    }

    fun setRepeatMode(mode: RepeatMode) {
        repeatMode = mode
        applyRepeatMode()
        onQueueChanged()
    }

    fun insertIntoQueue(index: Int, tracks: List<PlayableTrack>) {
        if (tracks.isEmpty()) return
        val position = index.coerceIn(0, queue.size)
        val items = tracks.map(::queueItem)
        queue.addAll(position, items)
        when {
            currentIndex < 0 -> scope.launch {
                // Only resolve the first inserted track (and next for preload) - fire-and-forget for container tracks
                val firstTrack = items[0].track
                if (firstTrack.isContainerTrack && firstTrack.resolvedUri == null) {
                    scope.launch { resolveContainerTrack(firstTrack) }
                }
                if (items.size > 1) {
                    val nextTrack = items[1].track
                    if (nextTrack.isContainerTrack && nextTrack.resolvedUri == null) {
                        scope.launch { resolveContainerTrack(nextTrack) }
                    }
                }
                startItem(0)
            }
            position <= currentIndex -> {
                currentIndex += tracks.size
                onQueueChanged()
            }
            else -> onQueueChanged()
        }
    }

    fun removeFromQueue(queueItemId: Long) {
        val index = queue.indexOfFirst { it.id == queueItemId }
        if (index >= 0) removeQueueItems(index, index + 1)
    }

    /** Removes every queued instance of a track that was removed from the library. */
    fun removeTrack(trackId: String) {
        val currentItemId = queue.getOrNull(currentIndex)?.id
        val nextQueue = queue.filterNot { it.track.trackId == trackId }
        if (nextQueue.size == queue.size) return

        queue.clear()
        queue += nextQueue
        originalOrder = originalOrder.filter { id -> queue.any { it.id == id } }

        if (queue.isEmpty()) {
            stopAll()
            return
        }

        val currentItemIndex = queue.indexOfFirst { it.id == currentItemId }
        if (currentItemIndex >= 0) {
            currentIndex = currentItemIndex
            onQueueChanged()
        } else {
            val newIndex = currentIndex.coerceIn(0, queue.lastIndex)
            scope.launch {
                // Only resolve the track that will play next, not the entire queue - fire-and-forget for container tracks
                val nextTrack = queue[newIndex].track
                if (nextTrack.isContainerTrack && nextTrack.resolvedUri == null) {
                    scope.launch { resolveContainerTrack(nextTrack) }
                }
                startItem(newIndex)
            }
        }
    }

    fun removeQueueItems(fromIndex: Int, toIndex: Int) {
        val start = fromIndex.coerceAtLeast(0)
        val end = toIndex.coerceAtMost(queue.size)
        if (start >= end) return
        val removesCurrent = currentIndex in start until end
        queue.subList(start, end).clear()
        when {
            queue.isEmpty() -> stopAll()
            removesCurrent -> scope.launch {
                val newIndex = start.coerceAtMost(queue.lastIndex)
                // Only resolve the track that will play next, not the entire queue - fire-and-forget for container tracks
                val nextTrack = queue[newIndex].track
                if (nextTrack.isContainerTrack && nextTrack.resolvedUri == null) {
                    scope.launch { resolveContainerTrack(nextTrack) }
                }
                startItem(newIndex)
            }
            currentIndex >= end -> {
                currentIndex -= end - start
                onQueueChanged()
            }
            else -> onQueueChanged()
        }
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        moveQueueItems(fromIndex, fromIndex + 1, toIndex)
    }

    fun moveQueueItems(fromIndex: Int, toIndex: Int, newIndex: Int) {
        if (fromIndex !in queue.indices || toIndex !in (fromIndex + 1)..queue.size) return
        val currentId = queue.getOrNull(currentIndex)?.id
        val moved = queue.subList(fromIndex, toIndex).toList()
        queue.subList(fromIndex, toIndex).clear()
        queue.addAll(newIndex.coerceIn(0, queue.size), moved)
        currentIndex = queue.indexOfFirst { it.id == currentId }
        onQueueChanged()
    }

    fun restore(saved: SavedQueue) {
        if (queue.isNotEmpty() || saved.tracks.isEmpty()) return
        val items = saved.tracks.map(::queueItem)
        queue += items
        isShuffleEnabled = saved.isShuffleEnabled
        repeatMode = saved.repeatMode
        originalOrder = saved.originalOrder
            .mapNotNull { items.getOrNull(it)?.id }
            .takeIf { isShuffleEnabled && it.size == items.size }
            ?: items.map { it.id }
        isPlaying = false
        val startIndex = saved.currentIndex.coerceIn(queue.indices)
        scope.launch {
            // Only resolve the starting track (and next for preload) instead of all tracks - fire-and-forget for container tracks
            val startTrack = items[startIndex].track
            if (startTrack.isContainerTrack && startTrack.resolvedUri == null) {
                scope.launch { resolveContainerTrack(startTrack) }
            }
            val nextIdx = startIndex + 1
            if (nextIdx < items.size) {
                val nextTrack = items[nextIdx].track
                if (nextTrack.isContainerTrack && nextTrack.resolvedUri == null) {
                    scope.launch { resolveContainerTrack(nextTrack) }
                }
            }
            startItem(startIndex)
            seekTo(saved.positionMs)
        }
    }

    fun snapshot(): SavedQueue = SavedQueue(
        tracks = queue.map { it.track },
        currentIndex = currentIndex.coerceAtLeast(0),
        positionMs = positionMs(),
        isShuffleEnabled = isShuffleEnabled,
        repeatMode = repeatMode,
        originalOrder = if (isShuffleEnabled) {
            originalOrder.mapNotNull { id -> queue.indexOfFirst { it.id == id }.takeIf { it >= 0 } }
        } else {
            emptyList()
        }
    )

    fun skipTo(queueItemId: Long) {
        val index = queue.indexOfFirst { it.id == queueItemId }
        if (index >= 0) scope.launch { startItem(index) }
    }

    fun skipToNext() {
        nextIndex()?.let { scope.launch { startItem(it) } }
    }

    fun skipToPreviousTrack() {
        previousIndex()?.let { scope.launch { startItem(it) } }
    }

    fun skipToPrevious() {
        if (currentIndex < 0) return
        val previous = previousIndex()
        if (previous != null && positionMs() <= RESTART_THRESHOLD_MS) scope.launch { startItem(previous) } else seekTo(0L)
    }

    fun play() {
        if (currentIndex < 0) return
        if (active.playbackState == Player.STATE_IDLE) active.prepare()
        if (active.playbackState == Player.STATE_ENDED) active.seekTo(0L)
        isPlaying = true
        active.playWhenReady = true
        publish()
        updateTicker()
    }

    fun pause() {
        cutFade()
        isPlaying = false
        active.playWhenReady = false
        publish()
        updateTicker()
    }

    fun togglePlayPause() {
        if (isPlaying) pause() else play()
    }

    fun seekTo(positionMs: Long) {
        if (currentIndex < 0) return
        cutFade()
        active.seekTo(positionMs.coerceAtLeast(0L))
        publish()
    }

    fun seekTo(index: Int, positionMs: Long) {
        if (index !in queue.indices) return
        val target = if (positionMs == C.TIME_UNSET) 0L else positionMs
        if (index == currentIndex) {
            seekTo(target)
        } else {
            scope.launch {
                startItem(index)
                if (target > 0L) seekTo(target)
            }
        }
    }

    private fun queueItem(track: PlayableTrack) = QueueItem(nextQueueItemId++, track)

    private fun nextIndex(): Int? = when {
        currentIndex < 0 -> null
        currentIndex < queue.lastIndex -> currentIndex + 1
        repeatMode == RepeatMode.ALL -> 0
        else -> null
    }

    private fun previousIndex(): Int? = when {
        currentIndex < 0 -> null
        currentIndex > 0 -> currentIndex - 1
        repeatMode == RepeatMode.ALL -> queue.lastIndex
        else -> null
    }

    private fun shuffleAroundCurrent() {
        val current = queue.getOrNull(currentIndex) ?: return
        val rest = queue.filterNot { it.id == current.id }.shuffled()
        queue.clear()
        queue += current
        queue += rest
        currentIndex = 0
    }

    private fun restoreOriginalOrder() {
        val currentId = queue.getOrNull(currentIndex)?.id
        val byId = queue.associateBy { it.id }
        val ordered = originalOrder.mapNotNull { byId[it] }
        val known = originalOrder.toSet()
        val added = queue.filterNot { it.id in known }
        queue.clear()
        queue += ordered
        queue += added
        currentIndex = queue.indexOfFirst { it.id == currentId }
    }

    private fun applyRepeatMode() {
        players.forEach { it.repeatMode = Player.REPEAT_MODE_OFF }
        if (repeatMode == RepeatMode.ONE) active.repeatMode = Player.REPEAT_MODE_ONE
    }

    private suspend fun startItem(index: Int) {
        finishFade()
        val item = queue[index]
        Log.d("PlaybackController", "startItem: index=$index, trackId=${item.track.trackId}, isContainerTrack=${item.track.isContainerTrack}")

        // For container tracks: await resolution before starting playback.
        // Without resolved stream info, getContainerDataSourceFactory() returns null
        // and the axs-container:// URI fails with the regular DataSource, causing
        // ExoPlayer to error and auto-skip to the next track.
        if (item.track.isContainerTrack && item.track.resolvedUri == null) {
            resolveContainerTrack(item.track)
        }
        val preloaded = standby.takeIf { it.currentMediaItem?.mediaId == item.mediaId && it.playerError == null }
        if (preloaded != null) {
            Log.d("PlaybackController", "startItem: using preloaded player")
            audioProcessors[players.indexOf(preloaded)].trackVolume = item.track.playbackVolume
            val previous = active
            active = preloaded
            previous.stop()
            previous.clearMediaItems()
            assignAudioFocus()
            // Preloaded player is already prepared, start playing and preload next
            active.volume = 1f
            active.playWhenReady = isPlaying
            currentIndex = index
            applyRepeatMode()
            publish()
            scope.launch { preloadNext() }
        } else {
            Log.d("PlaybackController", "startItem: creating new media source")
            audioProcessors[players.indexOf(active)].trackVolume = item.track.playbackVolume
            active.setMediaSource(mediaSource(item))
            active.prepare()
            active.volume = 1f
            active.playWhenReady = isPlaying
            currentIndex = index
            applyRepeatMode()
            publish()
            // Don't preload next immediately - wait for active player to be ready
            // preloadNext() will be called from onPlaybackStateChanged when player becomes READY
        }
    }

    private fun onQueueChanged() {
        publish()
        // Only preload next if active player is already ready (avoid premature preload during track startup)
        if (active.playbackState == Player.STATE_READY) {
            scope.launch { preloadNext() }
        }
    }

    private fun stopAll() {
        finishFade()
        players.forEach {
            it.stop()
            it.clearMediaItems()
        }
        currentIndex = -1
        isPlaying = false
        publish()
        updateTicker()
    }

    private suspend fun preloadNext() {
        if (fadingOut != null) return
        val next = nextIndex()?.let(queue::get)
        val player = standby
        player.playWhenReady = false
        when {
            next == null -> {
                player.stop()
                player.clearMediaItems()
            }
            player.currentMediaItem?.mediaId != next.mediaId || player.playerError != null -> {
                // Launch container track resolution in background (fire-and-forget).
                // Don't wait - preload with estimated duration; correct duration comes when resolution completes.
                if (next.track.isContainerTrack && next.track.resolvedUri == null) {
                    scope.launch {
                        try {
                            resolveContainerTrack(next.track)
                        } catch (e: Exception) {
                            Log.e(LOG_TAG, "Failed to preload container track ${next.track.trackId}", e)
                        }
                    }
                }
                audioProcessors[players.indexOf(player)].trackVolume = next.track.playbackVolume
                player.setMediaSource(mediaSource(next))
                player.prepare()
            }
        }
    }

    private fun mediaSource(item: QueueItem): ProgressiveMediaSource {
        val track = item.track
        val uri = track.effectiveUri
        
        // For container tracks, use cached duration from manifest if track.durationMs is missing
        val effectiveDurationMs = if (track.isContainerTrack) {
            val streamInfo = containerTrackInfoCache[track.trackId]
            val dur = track.durationMs?.takeIf { it > 0L } ?: streamInfo?.durationMs?.takeIf { it > 0L }
            Log.d("PlaybackController", "mediaSource: track=${track.trackId} isContainerTrack=true, track.durationMs=${track.durationMs}, cachedDuration=${streamInfo?.durationMs}, effectiveDurationMs=$dur")
            dur
        } else {
            track.durationMs?.takeIf { it > 0L }
        }
        val durationUs = effectiveDurationMs?.times(1_000L)
        
        val seekOffsetCorrection = if (track.hasVideo) {
            fragmentedMp4Reader.seekOffsetCorrection(uri)
        } else null

        val extractorsFactory: ExtractorsFactory = if (track.isContainerTrack) {
            val streamInfo = containerTrackInfoCache[track.trackId]
            val trackLengthBytes = streamInfo?.blobLength
            val mimeType = track.containerBlobName?.substringAfterLast('.')?.lowercase()
            Log.d("PlaybackController", "mediaSource: track=${track.trackId}, trackLengthBytes=$trackLengthBytes, durationUs=$durationUs, mimeType=$mimeType")
            ContainerExtractorsFactory(durationUs, trackLengthBytes, mimeType)
        } else {
            DurationAwareExtractorsFactory(durationUs, seekOffsetCorrection)
        }

        val factory: androidx.media3.datasource.DataSource.Factory = if (track.isContainerTrack) {
            getContainerDataSourceFactory(track) ?: dataSourceFactory
        } else {
            dataSourceFactory
        }

        return ProgressiveMediaSource.Factory(factory, extractorsFactory)
            .createMediaSource(track.toMediaItem(item.id.toString(), effectiveDurationMs))
    }

    private fun getContainerDataSourceFactory(track: PlayableTrack): AxsContainerDataSourceFactory? {
        // Use cached streaming info from resolveContainerTrack
        val streamInfo = containerTrackInfoCache[track.trackId] ?: return null

        val app = AxsApplication.getInstance()
        return AxsContainerDataSourceFactory(
            resolver = app.contentResolver,
            containerUri = streamInfo.containerUri,
            blobOffset = streamInfo.blobOffset,
            blobLength = streamInfo.blobLength
        )
    }

    private fun updateTicker() {
        if (isPlaying && ticker?.isActive != true) {
            ticker = scope.launch {
                while (isActive) {
                    tick()
                    delay(TICK_INTERVAL_MS)
                }
            }
        } else if (!isPlaying) {
            ticker?.cancel()
            ticker = null
        }
    }

    private fun tick() {
        val outgoing = fadingOut
        if (outgoing != null) {
            updateFade(outgoing)
            return
        }
        val transitionMs = when {
            crossfadeMs > 0L -> crossfadeMs
            isGapless -> GAPLESS_LEAD_MS
            else -> return
        }
        if (repeatMode == RepeatMode.ONE) return
        val nextIndex = nextIndex() ?: return
        val next = queue[nextIndex]
        val incoming = standby
        val duration = active.duration
        if (duration == C.TIME_UNSET) return
        if (incoming.currentMediaItem?.mediaId != next.mediaId || incoming.playbackState != Player.STATE_READY) return

        // Prevent premature gapless transition: require active track to have played for at least 2 seconds
        // This avoids switching to preloaded next track during initial buffering of container tracks
        if (active.currentPosition < 2000) return

        // Skip gapless for container tracks without real manifest duration (only estimated)
        // Estimated duration can be wildly inaccurate, causing premature transitions
        val currentTrack = queue.getOrNull(currentIndex)?.track
        if (currentTrack?.isContainerTrack == true) {
            val streamInfo = containerTrackInfoCache[currentTrack.trackId]
            val hasRealDuration = streamInfo?.durationMs != null && streamInfo?.durationMs!! > 0L
            if (!hasRealDuration) return
        }

        val fadeMs = min(transitionMs, min(duration, incoming.duration.takeIf { it != C.TIME_UNSET } ?: duration) / 2)
        val remaining = duration - active.currentPosition
        if (remaining > fadeMs) return

        fadingOut = active
        fadeDurationMs = remaining.coerceAtLeast(1L)
        active = incoming
        active.volume = 0f
        active.playWhenReady = true
        currentIndex = nextIndex
        publish()
    }

    private fun updateFade(outgoing: ExoPlayer) {
        val remaining = (outgoing.duration - outgoing.currentPosition).coerceAtLeast(0L)
        val progress = (1f - remaining.toFloat() / fadeDurationMs).coerceIn(0f, 1f)
        outgoing.volume = cos(progress * PI / 2).toFloat()
        active.volume = sin(progress * PI / 2).toFloat()
        if (progress >= 1f || outgoing.playbackState == Player.STATE_ENDED) cutFade()
    }

    private fun cutFade() {
        if (fadingOut == null) return
        finishFade()
        scope.launch { preloadNext() }
    }

    private fun finishFade() {
        val outgoing = fadingOut ?: return
        fadingOut = null
        outgoing.stop()
        outgoing.clearMediaItems()
        outgoing.volume = 1f
        active.volume = 1f
        assignAudioFocus()
    }

    private fun assignAudioFocus() {
        standby.setAudioAttributes(AUDIO_ATTRIBUTES, false)
        active.setAudioAttributes(AUDIO_ATTRIBUTES, true)
    }

    private fun onActiveEnded() {
        val next = nextIndex()
        if (next != null) {
            scope.launch { startItem(next) }
        } else {
            isPlaying = false
            active.playWhenReady = false
            active.seekTo(0L)
            publish()
            updateTicker()
        }
    }

    private fun publish() {
        _state.value = PlaybackState(
            queue = queue.toList(),
            currentIndex = currentIndex,
            isPlaying = isPlaying,
            isShuffleEnabled = isShuffleEnabled,
            repeatMode = repeatMode
        )
    }

    /**
     * Keeps the UI/session state aligned when ExoPlayer itself pauses or resumes
     * playback (for example, after losing audio focus or unplugging headphones).
     *
     * Most ExoPlayer callbacks are deliberately not mirrored here: preparing a
     * media item and seeking may also change [Player.playWhenReady] transiently.
     */
    private fun updatePlaybackIntent(playWhenReady: Boolean) {
        if (isPlaying == playWhenReady) return
        isPlaying = playWhenReady
        publish()
        updateTicker()
    }

    private inner class PlayerEvents(private val player: ExoPlayer) : Player.Listener {

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_READY && player === active) {
                publish()
                // Preload next track once active player is ready (avoids premature gapless transition during initial buffering)
                scope.launch { preloadNext() }
            }
            if (playbackState != Player.STATE_ENDED) return
            when {
                player === fadingOut -> cutFade()
                player === active -> onActiveEnded()
            }
        }

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            if (player !== active) return
            when (reason) {
                Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_FOCUS_LOSS,
                Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_BECOMING_NOISY,
                Player.PLAY_WHEN_READY_CHANGE_REASON_REMOTE,
                Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST -> {
                    updatePlaybackIntent(playWhenReady)
                }
                // Preparing a media item, seeking, and renderer changes can
                // change playWhenReady internally. Those must not turn a
                // requested playback session into a paused UI state.
                else -> Unit
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            when {
                player === fadingOut -> cutFade()
                player === active -> {
                    Log.e(LOG_TAG, "Не удалось воспроизвести трек ${queue.getOrNull(currentIndex)?.track?.trackId}", error)
                    onActiveEnded()
                }
            }
        }
    }
}