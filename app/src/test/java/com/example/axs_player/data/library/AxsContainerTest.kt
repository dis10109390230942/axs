package com.example.axs_player.data.library

import org.junit.Assert.*
import org.junit.Test

class AxsContainerTest {

    @Test
    fun testCleanStringRemovesNullLiterals() {
        // Test the cleanString function behavior
        assertNull(AxsContainer.cleanString(null))
        assertNull(AxsContainer.cleanString(""))
        assertNull(AxsContainer.cleanString("   "))
        assertNull(AxsContainer.cleanString("null"))
        assertNull(AxsContainer.cleanString("NULL"))
        assertNull(AxsContainer.cleanString("Null"))
        assertNull(AxsContainer.cleanString("  null  "))

        assertEquals("valid", AxsContainer.cleanString("valid"))
        assertEquals("valid", AxsContainer.cleanString("  valid  "))
        assertEquals("not null", AxsContainer.cleanString("not null"))
    }

    @Test
    fun testAxsArtistCreditDefaultRole() {
        val credit = AxsArtistCredit("Test Artist")
        assertEquals("Test Artist", credit.name)
        assertEquals("MAIN", credit.role)
    }

    @Test
    fun testAxsArtistCreditCustomRole() {
        val credit = AxsArtistCredit("Test Artist", "FEATURED")
        assertEquals("Test Artist", credit.name)
        assertEquals("FEATURED", credit.role)
    }

    @Test
    fun testAxsTrackDefaults() {
        val track = AxsTrack(
            id = null,
            number = null,
            discNumber = 1,
            title = "Test Track",
            subtitle = null,
            artists = listOf("Artist"),
            artistCredits = emptyList(),
            date = null,
            year = null,
            description = null,
            lyrics = null,
            duration = null,
            composer = null,
            hasVideo = false,
            parentTrackId = null,
            playbackVolume = 1f,
            media = AxsBlob("track.mp3", "audio/mpeg", 32, 1000)
        )
        assertEquals("Test Track", track.title)
        assertNull(track.id)
        assertNull(track.number)
        assertEquals(1, track.discNumber)
        assertNull(track.subtitle)
        assertEquals(listOf("Artist"), track.artists)
        assertTrue(track.artistCredits.isEmpty())
        assertNull(track.date)
        assertNull(track.year)
        assertNull(track.description)
        assertNull(track.lyrics)
        assertNull(track.duration)
        assertNull(track.composer)
        assertFalse(track.hasVideo)
        assertNull(track.parentTrackId)
        assertEquals(1f, track.playbackVolume, 0.001f)
    }

    @Test
    fun testAxsTrackWithAllFields() {
        val track = AxsTrack(
            id = "track-1",
            number = 1,
            discNumber = 1,
            title = "Test Track",
            subtitle = "Subtitle",
            artists = listOf("Artist 1", "Artist 2"),
            artistCredits = listOf(AxsArtistCredit("Artist 1", "MAIN"), AxsArtistCredit("Artist 2", "FEATURED")),
            date = "2023-01-01",
            year = 2023,
            description = "Description",
            lyrics = "Lyrics",
            duration = 180000L,
            composer = "Composer",
            hasVideo = true,
            parentTrackId = "parent-id",
            playbackVolume = 0.8f,
            media = AxsBlob("track.mp3", "audio/mpeg", 32, 1000)
        )
        assertEquals("track-1", track.id)
        assertEquals(1, track.number)
        assertEquals(1, track.discNumber)
        assertEquals("Test Track", track.title)
        assertEquals("Subtitle", track.subtitle)
        assertEquals(listOf("Artist 1", "Artist 2"), track.artists)
        assertEquals(listOf(AxsArtistCredit("Artist 1", "MAIN"), AxsArtistCredit("Artist 2", "FEATURED")), track.artistCredits)
        assertEquals("2023-01-01", track.date)
        assertEquals(2023, track.year)
        assertEquals("Description", track.description)
        assertEquals("Lyrics", track.lyrics)
        assertEquals(180000L, track.duration)
        assertEquals("Composer", track.composer)
        assertTrue(track.hasVideo)
        assertEquals("parent-id", track.parentTrackId)
        assertEquals(0.8f, track.playbackVolume, 0.001f)
    }

    @Test
    fun testAxsAlbumDefaults() {
        val album = AxsAlbum(
            id = null,
            title = "Test Album",
            artists = listOf("Artist"),
            artistCredits = emptyList(),
            year = null,
            discNumber = 1,
            genre = null,
            description = null,
            cover = null,
            photos = emptyList(),
            mediaFiles = emptyList(),
            backgroundTexture = null,
            backgroundTextureScale = 1f,
            backgroundTextureFit = "cover",
            backgroundTextureOffsetX = 0f,
            backgroundTextureOffsetY = 0f,
            backgroundTextureBlendMode = "multiply",
            backgroundTextureKeyColor = -1,
            backgroundTextureTolerance = 32f,
            backgroundTextureOpacity = 1f,
            tracks = listOf(
                AxsTrack(
                    id = null,
                    number = null,
                    discNumber = 1,
                    title = "Track 1",
                    subtitle = null,
                    artists = listOf("Artist"),
                    artistCredits = emptyList(),
                    date = null,
                    year = null,
                    description = null,
                    lyrics = null,
                    duration = null,
                    composer = null,
                    hasVideo = false,
                    parentTrackId = null,
                    playbackVolume = 1f,
                    media = AxsBlob("track1.mp3", "audio/mpeg", 32, 1000)
                )
            ),
            rawJson = null
        )
        assertEquals("Test Album", album.title)
        assertNull(album.id)
        assertEquals(listOf("Artist"), album.artists)
        assertTrue(album.artistCredits.isEmpty())
        assertNull(album.year)
        assertEquals(1, album.discNumber)
        assertNull(album.genre)
        assertNull(album.description)
        assertNull(album.cover)
        assertTrue(album.photos.isEmpty())
        assertTrue(album.mediaFiles.isEmpty())
        assertNull(album.backgroundTexture)
        assertEquals(1f, album.backgroundTextureScale, 0.001f)
        assertEquals("cover", album.backgroundTextureFit)
        assertEquals(0f, album.backgroundTextureOffsetX, 0.001f)
        assertEquals(0f, album.backgroundTextureOffsetY, 0.001f)
        assertEquals("multiply", album.backgroundTextureBlendMode)
        assertEquals(-1, album.backgroundTextureKeyColor)
        assertEquals(32f, album.backgroundTextureTolerance, 0.001f)
        assertEquals(1f, album.backgroundTextureOpacity, 0.001f)
    }

    @Test
    fun testAxsAlbumWithAllFields() {
        val album = AxsAlbum(
            id = "album-1",
            title = "Test Album",
            artists = listOf("Artist 1", "Artist 2"),
            artistCredits = listOf(AxsArtistCredit("Artist 1", "MAIN"), AxsArtistCredit("Artist 2", "FEATURED")),
            year = 2023,
            discNumber = 1,
            genre = "Rock",
            description = "Album description",
            cover = AxsBlob("cover.jpg", "image/jpeg", 32, 500),
            photos = listOf(AxsBlob("photo1.jpg", "image/jpeg", 532, 300)),
            mediaFiles = listOf(AxsBlob("video.mp4", "video/mp4", 832, 1000)),
            backgroundTexture = AxsBlob("texture.png", "image/png", 1832, 800),
            backgroundTextureScale = 1.5f,
            backgroundTextureFit = "contain",
            backgroundTextureOffsetX = 0.1f,
            backgroundTextureOffsetY = -0.2f,
            backgroundTextureBlendMode = "overlay",
            backgroundTextureKeyColor = 0xFFFF0000.toInt(),
            backgroundTextureTolerance = 16f,
            backgroundTextureOpacity = 0.5f,
            tracks = listOf(
                AxsTrack(
                    id = null,
                    number = null,
                    discNumber = 1,
                    title = "Track 1",
                    subtitle = null,
                    artists = listOf("Artist"),
                    artistCredits = emptyList(),
                    date = null,
                    year = null,
                    description = null,
                    lyrics = null,
                    duration = null,
                    composer = null,
                    hasVideo = false,
                    parentTrackId = null,
                    playbackVolume = 1f,
                    media = AxsBlob("track1.mp3", "audio/mpeg", 32, 1000)
                )
            ),
            rawJson = null
        )
        assertEquals("album-1", album.id)
        assertEquals("Test Album", album.title)
        assertEquals(listOf("Artist 1", "Artist 2"), album.artists)
        assertEquals(listOf(AxsArtistCredit("Artist 1", "MAIN"), AxsArtistCredit("Artist 2", "FEATURED")), album.artistCredits)
        assertEquals(2023, album.year)
        assertEquals("Rock", album.genre)
        assertEquals("Album description", album.description)
        assertNotNull(album.cover)
        assertEquals(1, album.photos.size)
        assertEquals(1, album.mediaFiles.size)
        assertNotNull(album.backgroundTexture)
        assertEquals(1.5f, album.backgroundTextureScale, 0.001f)
        assertEquals("contain", album.backgroundTextureFit)
        assertEquals(0.1f, album.backgroundTextureOffsetX, 0.001f)
        assertEquals(-0.2f, album.backgroundTextureOffsetY, 0.001f)
        assertEquals("overlay", album.backgroundTextureBlendMode)
        assertEquals(0xFFFF0000.toInt(), album.backgroundTextureKeyColor)
        assertEquals(16f, album.backgroundTextureTolerance, 0.001f)
        assertEquals(0.5f, album.backgroundTextureOpacity, 0.001f)
    }

    @Test
    fun testAxsWriteSourceCreation() {
        val source = AxsWriteSource(
            name = "test.mp3",
            mime = "audio/mpeg",
            openStream = { java.io.ByteArrayInputStream(byteArrayOf(1, 2, 3)) }
        )
        assertEquals("test.mp3", source.name)
        assertEquals("audio/mpeg", source.mime)
        assertNotNull(source.openStream)
    }

    @Test
    fun testAxsTrackWriteDataCreation() {
        val trackData = AxsTrackWriteData(
            id = "track-1",
            number = 1,
            discNumber = 1,
            title = "Track 1",
            subtitle = "Subtitle",
            artists = listOf("Artist"),
            artistCredits = listOf(AxsArtistCredit("Artist", "MAIN")),
            date = "2023-01-01",
            year = 2023,
            description = "Description",
            lyrics = "Lyrics",
            duration = 180000L,
            composer = "Composer",
            hasVideo = true,
            parentTrackId = "parent-id",
            playbackVolume = 0.8f,
            mediaSource = AxsWriteSource(
                name = "track1.mp3",
                mime = "audio/mpeg",
                openStream = { java.io.ByteArrayInputStream(byteArrayOf()) }
            )
        )
        assertEquals("track-1", trackData.id)
        assertEquals(1, trackData.number)
        assertEquals(1, trackData.discNumber)
        assertEquals("Track 1", trackData.title)
        assertEquals("Subtitle", trackData.subtitle)
        assertEquals(listOf("Artist"), trackData.artists)
        assertEquals(listOf(AxsArtistCredit("Artist", "MAIN")), trackData.artistCredits)
        assertEquals("2023-01-01", trackData.date)
        assertEquals(2023, trackData.year)
        assertEquals("Description", trackData.description)
        assertEquals("Lyrics", trackData.lyrics)
        assertEquals(180000L, trackData.duration)
        assertEquals("Composer", trackData.composer)
        assertTrue(trackData.hasVideo)
        assertEquals("parent-id", trackData.parentTrackId)
        assertEquals(0.8f, trackData.playbackVolume, 0.001f)
    }

    @Test
    fun testAxsAlbumWriteDataCreation() {
        val albumData = AxsAlbumWriteData(
            id = "album-1",
            title = "Test Album",
            artists = listOf("Artist"),
            artistCredits = listOf(AxsArtistCredit("Artist", "MAIN")),
            year = 2023,
            discNumber = 1,
            genre = "Rock",
            description = "Description",
            coverSource = null,
            photoSources = emptyList(),
            mediaFileSources = emptyList(),
            backgroundTextureSource = null,
            backgroundTextureScale = 1f,
            backgroundTextureFit = "cover",
            backgroundTextureOffsetX = 0f,
            backgroundTextureOffsetY = 0f,
            backgroundTextureBlendMode = "multiply",
            backgroundTextureKeyColor = -1,
            backgroundTextureTolerance = 32f,
            backgroundTextureOpacity = 1f,
            tracks = emptyList(),
            preservedRawJson = null
        )
        assertEquals("album-1", albumData.id)
        assertEquals("Test Album", albumData.title)
        assertEquals(listOf("Artist"), albumData.artists)
        assertEquals(listOf(AxsArtistCredit("Artist", "MAIN")), albumData.artistCredits)
        assertEquals(2023, albumData.year)
        assertEquals("Rock", albumData.genre)
        assertEquals("Description", albumData.description)
    }

    @Test
    fun testAxsBlobCreation() {
        val blob = AxsBlob("test.mp3", "audio/mpeg", 100, 500)
        assertEquals("test.mp3", blob.name)
        assertEquals("audio/mpeg", blob.mime)
        assertEquals(100, blob.offset)
        assertEquals(500, blob.length)
    }
}