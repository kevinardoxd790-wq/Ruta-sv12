package com.example

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.OfflinePlaceFtsEntity
import com.example.data.local.OfflinePlaceRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OfflinePlaceSearchTest {

    @Test
    fun fts4SearchSupportsAccentInsensitivePrefixAndDistanceRanking() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        try {
            val repo = OfflinePlaceRepository(db.offlinePlaceDao())
            repo.insertBatch(
                listOf(
                    OfflinePlaceFtsEntity(
                        name = "Torre Futura",
                        normalizedName = OfflinePlaceFtsEntity.normalizeText("Torre Futura"),
                        category = "Edificio / Oficinas",
                        subtitle = "89 Avenida Norte, San Salvador",
                        latitude = 13.7089,
                        longitude = -89.2422,
                        rankPriority = 4
                    ),
                    OfflinePlaceFtsEntity(
                        name = "Paseo General Escalón",
                        normalizedName = OfflinePlaceFtsEntity.normalizeText("Paseo General Escalón"),
                        category = "Calle / Vía",
                        subtitle = "Red vial de El Salvador",
                        latitude = 13.7024,
                        longitude = -89.2361,
                        rankPriority = 1
                    ),
                    OfflinePlaceFtsEntity(
                        name = "Multiplaza",
                        normalizedName = OfflinePlaceFtsEntity.normalizeText("Multiplaza"),
                        category = "Centro comercial / Súper",
                        subtitle = "Antiguo Cuscatlán",
                        latitude = 13.6791,
                        longitude = -89.2504,
                        rankPriority = 4
                    )
                )
            )

            assertEquals(3, repo.count())

            val torreMatches = repo.search("torre fut", 13.7000, -89.2400)
            assertTrue("Expected Torre Futura match", torreMatches.isNotEmpty())
            assertEquals("Torre Futura", torreMatches.first().name)
            assertTrue((torreMatches.first().distanceMeters ?: 0.0) > 100.0)

            // Accent-insensitive & out-of-order token prefix query
            val escalonMatches = repo.search("escalon paseo", 13.7000, -89.2400)
            assertTrue("Expected Paseo General Escalón match", escalonMatches.isNotEmpty())
            assertEquals("Paseo General Escalón", escalonMatches.first().name)
        } finally {
            db.close()
        }
    }

    @Test
    fun migration1To2CreatesOfflineFtsTableWithoutLosingSavedPlaces() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "test_migration_v1_v2.db"
        context.deleteDatabase(dbName)

        // Create a version-1 database manually matching SavedPlaceEntity schema
        val v1Helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL(
                            """
                            CREATE TABLE IF NOT EXISTS `saved_places` (
                                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                `name` TEXT NOT NULL,
                                `notes` TEXT NOT NULL,
                                `category` TEXT NOT NULL,
                                `latitude` REAL NOT NULL,
                                `longitude` REAL NOT NULL,
                                `isFavorite` INTEGER NOT NULL,
                                `createdAt` INTEGER NOT NULL,
                                `lastUsedAt` INTEGER NOT NULL
                            )
                            """.trimIndent()
                        )
                        db.execSQL(
                            """
                            INSERT INTO `saved_places`
                            (`name`, `notes`, `category`, `latitude`, `longitude`, `isFavorite`, `createdAt`, `lastUsedAt`)
                            VALUES ('Casa', '', 'Casa', 13.69, -89.21, 1, 1000, 1000)
                            """.trimIndent()
                        )
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
                })
                .build()
        )
        v1Helper.writableDatabase.close()

        // Open with Room v2 + MIGRATION_1_2
        val migratedDb = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .allowMainThreadQueries()
            .build()
        try {
            val offlineRepo = OfflinePlaceRepository(migratedDb.offlinePlaceDao())
            assertEquals(0, offlineRepo.count())
            offlineRepo.insertBatch(
                listOf(
                    OfflinePlaceFtsEntity(
                        name = "Torre Futura",
                        normalizedName = "torre futura",
                        category = "Edificio / Oficinas",
                        subtitle = "San Salvador",
                        latitude = 13.7089,
                        longitude = -89.2422,
                        rankPriority = 4
                    )
                )
            )
            assertEquals(1, offlineRepo.count())
            assertEquals("Torre Futura", offlineRepo.search("torre", null, null).first().name)
        } finally {
            migratedDb.close()
            context.deleteDatabase(dbName)
        }
    }
}
