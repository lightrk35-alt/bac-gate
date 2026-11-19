package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase

@Entity(tableName = "unit_cycle_state")
data class UnitCycleStateEntity(
    @PrimaryKey val unitKey: String,
    val cycleNumber: Int,
    val cycleStartedAtEpochMs: Long
)

@Entity(
    tableName = "shown_questions",
    primaryKeys = ["unitKey", "cycleNumber", "eventId"]
)
data class ShownQuestionEntity(
    val unitKey: String,
    val cycleNumber: Int,
    val eventId: String,
    val shownAtEpochMs: Long
)

@Entity(
    tableName = "question_option_orders",
    primaryKeys = ["unitKey", "eventId"]
)
data class QuestionOptionOrderEntity(
    val unitKey: String,
    val eventId: String,
    val lastOptionKeysCsv: String
)

@Dao
interface ShuffleBagDao {
    @Query("SELECT * FROM unit_cycle_state WHERE unitKey = :unitKey LIMIT 1")
    suspend fun getCycleState(unitKey: String): UnitCycleStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCycleState(state: UnitCycleStateEntity)

    @Query("SELECT eventId FROM shown_questions WHERE unitKey = :unitKey AND cycleNumber = :cycleNumber")
    suspend fun getShownEventIds(unitKey: String, cycleNumber: Int): List<String>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertShownQuestion(entity: ShownQuestionEntity)

    @Query("SELECT lastOptionKeysCsv FROM question_option_orders WHERE unitKey = :unitKey AND eventId = :eventId LIMIT 1")
    suspend fun getLastOptionOrder(unitKey: String, eventId: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLastOptionOrder(entity: QuestionOptionOrderEntity)

    @Query("DELETE FROM shown_questions WHERE unitKey = :unitKey")
    suspend fun clearShownQuestionsForUnit(unitKey: String)

    @Query("DELETE FROM unit_cycle_state WHERE unitKey = :unitKey")
    suspend fun clearCycleStateForUnit(unitKey: String)
}

@Database(
    entities = [
        UnitCycleStateEntity::class,
        ShownQuestionEntity::class,
        QuestionOptionOrderEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class BacGateDatabase : RoomDatabase() {
    abstract fun shuffleBagDao(): ShuffleBagDao

    companion object {
        @Volatile
        private var instance: BacGateDatabase? = null

        fun getInstance(context: Context): BacGateDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    BacGateDatabase::class.java,
                    "bacgate_local.db"
                ).build().also { instance = it }
            }
        }
    }
}
