package com.txwstudio.app.whatthefit.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.txwstudio.app.whatthefit.R
import com.txwstudio.app.whatthefit.data.dao.CategoryDao
import com.txwstudio.app.whatthefit.data.dao.ClothingItemDao
import com.txwstudio.app.whatthefit.data.dao.CrossRefDao
import com.txwstudio.app.whatthefit.data.dao.TagCrossRefDao
import com.txwstudio.app.whatthefit.data.dao.TagDao
import com.txwstudio.app.whatthefit.data.entity.Category
import com.txwstudio.app.whatthefit.data.entity.ClothingItem
import com.txwstudio.app.whatthefit.data.entity.ItemCategoryCrossRef
import com.txwstudio.app.whatthefit.data.entity.ItemTagCrossRef
import com.txwstudio.app.whatthefit.data.entity.Tag

@Database(
    entities = [
        Category::class,
        ClothingItem::class,
        ItemCategoryCrossRef::class,
        Tag::class,
        ItemTagCrossRef::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(TagKindConverter::class)
abstract class WtfDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun clothingItemDao(): ClothingItemDao
    abstract fun crossRefDao(): CrossRefDao
    abstract fun tagDao(): TagDao
    abstract fun tagCrossRefDao(): TagCrossRefDao

    companion object {
        const val NAME = "wtf.db"

        /**
         * Default seeds. Parts/colors/occasions are string resources so they're localized to the
         * device language at first launch; brands are proper nouns, kept verbatim.
         */
        val DEFAULT_PART_NAME_RES = listOf(
            R.string.part_hat,
            R.string.part_glasses,
            R.string.part_jacket,
            R.string.part_top,
            R.string.part_undershirt,
            R.string.part_accessory,
            R.string.part_pants,
            R.string.part_underwear,
            R.string.part_socks,
            R.string.part_shoes,
        )

        val DEFAULT_BRANDS = listOf("UNIQLO", "GU", "plain-me", "niko and ...")

        /** Color tags: localized name resource to ARGB swatch. */
        val DEFAULT_COLORS = listOf(
            R.string.color_black to 0xFF000000L,
            R.string.color_white to 0xFFFFFFFFL,
            R.string.color_gray to 0xFF9E9E9EL,
        )

        val DEFAULT_OCCASION_NAME_RES = listOf(
            R.string.occasion_casual,
            R.string.occasion_work,
            R.string.occasion_formal,
            R.string.occasion_sport,
        )

        /** Seeds default categories + tags via raw SQL (DAOs aren't ready in onCreate). */
        fun seedCallback(context: Context): Callback = object : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                DEFAULT_PART_NAME_RES.forEachIndexed { index, nameRes ->
                    db.execSQL(
                        "INSERT INTO Category (name, sortOrder) VALUES (?, ?)",
                        arrayOf<Any>(context.getString(nameRes), index),
                    )
                }
                DEFAULT_BRANDS.forEachIndexed { index, name ->
                    db.execSQL(
                        "INSERT INTO Tag (kind, name, sortOrder, swatchArgb) VALUES ('BRAND', ?, ?, NULL)",
                        arrayOf<Any>(name, index),
                    )
                }
                DEFAULT_COLORS.forEachIndexed { index, (nameRes, argb) ->
                    db.execSQL(
                        "INSERT INTO Tag (kind, name, sortOrder, swatchArgb) VALUES ('COLOR', ?, ?, ?)",
                        arrayOf<Any>(context.getString(nameRes), index, argb),
                    )
                }
                DEFAULT_OCCASION_NAME_RES.forEachIndexed { index, nameRes ->
                    db.execSQL(
                        "INSERT INTO Tag (kind, name, sortOrder, swatchArgb) VALUES ('OCCASION', ?, ?, NULL)",
                        arrayOf<Any>(context.getString(nameRes), index),
                    )
                }
            }
        }
    }
}
