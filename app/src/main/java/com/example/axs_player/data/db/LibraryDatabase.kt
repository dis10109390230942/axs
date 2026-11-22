package com.example.axs_player.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("UPDATE tracks SET contentHash = ''")
    }
}

private val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE tracks ADD COLUMN hasVideo INTEGER NOT NULL DEFAULT 0")
    }
}

private val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE tracks ADD COLUMN composer TEXT")
    }
}

private val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `playlists` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, " +
                "`createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `playlist_items` (`id` TEXT NOT NULL, `playlistId` TEXT NOT NULL, " +
                "`trackId` TEXT NOT NULL, `position` INTEGER NOT NULL, PRIMARY KEY(`id`), " +
                "FOREIGN KEY(`playlistId`) REFERENCES `playlists`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , " +
                "FOREIGN KEY(`trackId`) REFERENCES `tracks`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_playlist_items_playlistId` ON `playlist_items` (`playlistId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_playlist_items_trackId` ON `playlist_items` (`trackId`)")
    }
}

private val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `play_events` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`trackId` TEXT NOT NULL, `playedAt` INTEGER NOT NULL, " +
                "FOREIGN KEY(`trackId`) REFERENCES `tracks`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_play_events_trackId` ON `play_events` (`trackId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_play_events_playedAt` ON `play_events` (`playedAt`)")
    }
}

private val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        listOf("fullName", "birthDate", "deathDate", "birthPlace", "countries").forEach { column ->
            db.execSQL("ALTER TABLE artists ADD COLUMN $column TEXT")
        }
    }
}

private val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE tracks ADD COLUMN parentTrackId TEXT")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_tracks_parentTrackId` ON `tracks` (`parentTrackId`)")
    }
}

private val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE tracks ADD COLUMN description TEXT")
    }
}

private val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE tracks ADD COLUMN lyrics TEXT")
    }
}

private val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE albums ADD COLUMN backgroundTextureFileName TEXT")
        db.execSQL("ALTER TABLE albums ADD COLUMN backgroundTextureScale REAL NOT NULL DEFAULT 1.0")
        db.execSQL("ALTER TABLE albums ADD COLUMN backgroundTextureOffsetX REAL NOT NULL DEFAULT 0.0")
        db.execSQL("ALTER TABLE albums ADD COLUMN backgroundTextureOffsetY REAL NOT NULL DEFAULT 0.0")
        db.execSQL("ALTER TABLE albums ADD COLUMN backgroundTextureBlendMode TEXT NOT NULL DEFAULT 'multiply'")
    }
}

private val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE albums ADD COLUMN backgroundTextureFit TEXT NOT NULL DEFAULT 'cover'")
    }
}

private val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE albums ADD COLUMN backgroundTextureKeyColor INTEGER NOT NULL DEFAULT -1")
        db.execSQL("ALTER TABLE albums ADD COLUMN backgroundTextureTolerance REAL NOT NULL DEFAULT 32.0")
    }
}

private val MIGRATION_13_14 = object : Migration(13, 14) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE albums ADD COLUMN backgroundTextureOpacity REAL NOT NULL DEFAULT 1.0")
        db.execSQL("UPDATE albums SET backgroundTextureOpacity = 0.22 WHERE backgroundTextureBlendMode = 'overlay'")
    }
}

private val MIGRATION_14_15 = object : Migration(14, 15) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE tracks ADD COLUMN subtitle TEXT")
        db.execSQL("ALTER TABLE tracks ADD COLUMN trackDate TEXT")
    }
}

private val MIGRATION_15_16 = object : Migration(15, 16) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE tracks ADD COLUMN playbackVolume REAL NOT NULL DEFAULT 1.0")
    }
}

private val MIGRATION_16_17 = object : Migration(16, 17) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE albums ADD COLUMN containerFileName TEXT")
        db.execSQL("ALTER TABLE albums ADD COLUMN isContainerOwned INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE albums ADD COLUMN containerSyncState TEXT")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_albums_containerFileName` ON `albums` (`containerFileName`)")
    }
}

private val MIGRATION_17_18 = object : Migration(17, 18) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE albums ADD COLUMN isContainerOnly INTEGER NOT NULL DEFAULT 0")
    }
}

private val MIGRATION_18_19 = object : Migration(18, 19) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE albums ADD COLUMN containerManifestJson TEXT")
    }
}

@Database(
    entities = [
        TrackEntity::class,
        AlbumEntity::class,
        ArtistEntity::class,
        AlbumTrackEntity::class,
        TrackArtistEntity::class,
        AlbumArtistEntity::class,
        PlaylistEntity::class,
        PlaylistItemEntity::class,
        PlayEventEntity::class
    ],
    version = 19,
    exportSchema = true
)
abstract class LibraryDatabase : RoomDatabase() {

    abstract fun libraryDao(): LibraryDao

    abstract fun statsDao(): StatsDao

    companion object {
        fun create(context: Context): LibraryDatabase =
            Room.databaseBuilder(context, LibraryDatabase::class.java, "library.db")
                .addMigrations(
                    MIGRATION_1_2,
                    MIGRATION_2_3,
                    MIGRATION_3_4,
                    MIGRATION_4_5,
                    MIGRATION_5_6,
                    MIGRATION_6_7,
                    MIGRATION_7_8,
                    MIGRATION_8_9,
                    MIGRATION_9_10,
                    MIGRATION_10_11,
                    MIGRATION_11_12,
                    MIGRATION_12_13,
                    MIGRATION_13_14,
                    MIGRATION_14_15,
                    MIGRATION_15_16,
                    MIGRATION_16_17,
                    MIGRATION_17_18,
                    MIGRATION_18_19
                )
                .build()
    }
}
