package com.example.axs_player.data.db

internal const val TRACK_ARTISTS = """
    NULLIF(
        COALESCE((SELECT group_concat(name, ', ') FROM (
            SELECT a.name AS name FROM track_artists ta
            JOIN artists a ON a.id = ta.artistId
            WHERE ta.trackId = t.id AND ta.role = 'MAIN'
            ORDER BY ta.position
        )), '') || COALESCE(' feat. ' || (SELECT group_concat(name, ', ') FROM (
            SELECT a.name AS name FROM track_artists ta
            JOIN artists a ON a.id = ta.artistId
            WHERE ta.trackId = t.id AND ta.role = 'FEATURED'
            ORDER BY ta.position
        )), ''),
        ''
    )
"""

internal const val TRACK_COVER = """
    COALESCE(t.coverFileName, (
        SELECT COALESCE(parent.coverFileName, (
            SELECT al.coverFileName FROM album_tracks atr
            JOIN albums al ON al.id = atr.albumId
            WHERE atr.trackId = parent.id AND al.coverFileName IS NOT NULL
            ORDER BY al.createdAt
            LIMIT 1
        )) FROM tracks parent
        WHERE parent.id = t.parentTrackId
        LIMIT 1
    ), (
        SELECT al.coverFileName FROM album_tracks atr
        JOIN albums al ON al.id = atr.albumId
        WHERE atr.trackId = t.id AND al.coverFileName IS NOT NULL
        LIMIT 1
    ))
"""

internal const val ALBUM_ARTISTS = """
    (SELECT group_concat(name, ', ') FROM (
        SELECT a.name AS name FROM album_artists aa
        JOIN artists a ON a.id = aa.artistId
        WHERE aa.albumId = al.id
        ORDER BY aa.position
    ))
"""

internal const val PLAYLIST_COLUMNS = """
    (SELECT COUNT(*) FROM playlist_items pi WHERE pi.playlistId = p.id) AS trackCount,
    (SELECT $TRACK_COVER FROM playlist_items pi JOIN tracks t ON t.id = pi.trackId
     WHERE pi.playlistId = p.id ORDER BY pi.position LIMIT 1 OFFSET 0) AS firstCover,
    (SELECT $TRACK_COVER FROM playlist_items pi JOIN tracks t ON t.id = pi.trackId
     WHERE pi.playlistId = p.id ORDER BY pi.position LIMIT 1 OFFSET 1) AS secondCover,
    (SELECT $TRACK_COVER FROM playlist_items pi JOIN tracks t ON t.id = pi.trackId
     WHERE pi.playlistId = p.id ORDER BY pi.position LIMIT 1 OFFSET 2) AS thirdCover
"""
