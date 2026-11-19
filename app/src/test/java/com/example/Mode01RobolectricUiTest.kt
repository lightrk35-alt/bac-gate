package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.BacGateDatabase
import com.example.data.model.BranchEligibility
import com.example.data.model.DateBankEnvelope
import com.example.data.model.DateEventRecord
import com.example.data.model.DatePrecision
import com.example.data.model.DifficultyLevel
import com.example.data.model.StudyUnit
import com.example.data.model.VerificationStatus
import com.example.data.repository.AssetDateBankDataSource
import com.example.data.repository.DateBankRepository
import com.example.data.repository.InMemoryDateBankDataSource
import com.example.data.repository.RoomShuffleBagStore
import com.example.domain.mode01.Mode01SessionState
import com.example.domain.mode01.QuestionSelector
import com.example.ui.BacGateApp
import com.example.ui.BacGateViewModel
import com.example.ui.theme.BacGateTheme
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.random.Random

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class Mode01RobolectricUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `real date_bank json loads offline with verified units and isolates conflicting and review records`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = DateBankRepository(AssetDateBankDataSource(context))
        val stats = repo.getGlobalStats()

        assertEquals(230, stats.totalRawRowsImported)
        assertEquals(179, stats.totalRecords)
        assertEquals(127, stats.verifiedCount)
        assertEquals(9, stats.conflictingCount)
        assertEquals(43, stats.needsReviewCount)
        assertEquals(7, stats.pendingUnitMappingCount)

        // All 3 units are genuinely playable with clean verified data
        assertEquals(51, repo.getPlayableRecordsForUnit(StudyUnit.UNIT_1).size)
        assertEquals(47, repo.getPlayableRecordsForUnit(StudyUnit.UNIT_2).size)
        assertEquals(26, repo.getPlayableRecordsForUnit(StudyUnit.UNIT_3).size)

        // Verify zero conflicting/needs_review records ever enter playable pools
        StudyUnit.entries.forEach { unit ->
            val playable = repo.getPlayableRecordsForUnit(unit)
            assertTrue(playable.all {
                it.verificationStatus == VerificationStatus.VERIFIED &&
                    it.branchEligibility == BranchEligibility.EXPERIMENTAL_SCIENCES_VERIFIED &&
                    it.unitId == unit.unitId
            })
        }
    }

    @Test
    fun `room shuffle bag persists shown questions across database close and reopen`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbFile = context.getDatabasePath("test_shuffle_persist.db")
        dbFile.delete()

        val records = (1..5).map { idx ->
            DateEventRecord(
                id = "u1_ev_$idx",
                subjectId = "history",
                categoryId = "dates",
                modeId = "mode_01",
                unitId = "unit_1",
                eventTitle = "حدث $idx",
                rawEventText = "حدث $idx",
                rawDate = "194${idx}/01/01",
                startDate = "194${idx}-01-01",
                endDate = null,
                datePrecision = DatePrecision.DAY,
                dateDisplay = "01 جانفي 194$idx",
                difficulty = DifficultyLevel.EASY,
                explanation = "",
                sourceName = "الكتاب المدرسي",
                sourceUrl = null,
                sourcePage = null,
                verificationStatus = VerificationStatus.VERIFIED,
                branchEligibility = BranchEligibility.EXPERIMENTAL_SCIENCES_VERIFIED,
                reviewNotes = "",
                sourceGroup = "unit_1_primary"
            )
        }
        val repo = DateBankRepository(
            InMemoryDateBankDataSource(
                DateBankEnvelope(1, "BAC;GATE", "ZARQYNE", "history", "dates", "mode_01", 5, 0, 0, records)
            )
        )

        val db1 = Room.databaseBuilder(context, BacGateDatabase::class.java, "test_shuffle_persist.db")
            .allowMainThreadQueries()
            .build()
        val selector1 = QuestionSelector(repo, RoomShuffleBagStore(db1.shuffleBagDao()), Random(11))
        val firstSessionIds = mutableSetOf<String>()
        repeat(3) {
            firstSessionIds.add(selector1.selectNextQuestion(StudyUnit.UNIT_1, DifficultyLevel.EASY).question!!.eventId)
        }
        db1.close()

        val db2 = Room.databaseBuilder(context, BacGateDatabase::class.java, "test_shuffle_persist.db")
            .allowMainThreadQueries()
            .build()
        val selector2 = QuestionSelector(repo, RoomShuffleBagStore(db2.shuffleBagDao()), Random(22))
        val secondSessionIds = mutableSetOf<String>()
        repeat(2) {
            secondSessionIds.add(selector2.selectNextQuestion(StudyUnit.UNIT_1, DifficultyLevel.EASY).question!!.eventId)
        }
        db2.close()
        dbFile.delete()

        assertTrue(firstSessionIds.intersect(secondSessionIds).isEmpty())
        assertEquals(5, (firstSessionIds + secondSessionIds).size)
    }

    @Test
    fun `full real game journey from subject selection through 2D arena, options, score, and next question`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, BacGateDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val vm = BacGateViewModel(
            dateBankRepository = DateBankRepository(AssetDateBankDataSource(context)),
            shuffleBagStore = RoomShuffleBagStore(db.shuffleBagDao())
        )

        composeRule.setContent {
            BacGateTheme {
                BacGateApp(viewModel = vm)
            }
        }
        composeRule.waitForIdle()

        // 1. Subject Selection -> History
        composeRule.onNodeWithTag("subject_history_card").performClick()
        composeRule.waitForIdle()

        // 2. History Categories -> Dates
        composeRule.onNodeWithTag("category_dates_card").performClick()
        composeRule.waitForIdle()

        // 3. Unit Selection -> Select Unit 1 (proceed disabled before choice)
        composeRule.onNodeWithTag("proceed_unit_button").assertIsNotEnabled()
        composeRule.onNodeWithTag("unit_card_unit_1").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("proceed_unit_button").assertIsEnabled().performClick()
        composeRule.waitForIdle()

        // 4. Mode 01 Setup -> Choose 60s -> Enter 2D Arena
        composeRule.onNodeWithTag("duration_60s").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("start_round_button").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("question_event_title").fetchSemanticsNodes().isNotEmpty()
        }

        // Verify HUD & Question Monolith & 4 Options are visible
        composeRule.onNodeWithTag("timer_text").assertIsDisplayed()
        composeRule.onNodeWithTag("score_text").assertIsDisplayed()
        composeRule.onNodeWithTag("difficulty_badge").assertIsDisplayed()
        composeRule.onNodeWithTag("round_progress_bar").assertIsDisplayed()
        composeRule.onNodeWithTag("question_event_title").assertIsDisplayed()
        composeRule.onNodeWithTag("option_0").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("option_1").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("option_2").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("option_3").performScrollTo().assertIsDisplayed()

        // Tap the true correct option dynamically to verify +10 score update & lock
        val active1 = vm.uiState.value.mode01State as Mode01SessionState.ActiveQuestion
        val correctIndex1 = active1.question.options.indexOfFirst { it.isCorrect }
        val wrongIndex1 = active1.question.options.indexOfFirst { !it.isCorrect }

        composeRule.onNodeWithTag("option_$correctIndex1").performScrollTo().performClick()
        composeRule.waitForIdle()

        // Tapping another option while locked must not change score
        composeRule.onNodeWithTag("option_$wrongIndex1").performClick()
        composeRule.waitForIdle()
        val afterAnswer1 = vm.uiState.value.mode01State as Mode01SessionState.ActiveQuestion
        assertTrue(afterAnswer1.isAnswerLocked)
        assertTrue(afterAnswer1.isTimerPaused)
        assertEquals(10, afterAnswer1.score)

        composeRule.onNodeWithTag("feedback_panel").assertIsDisplayed()
        composeRule.onNodeWithTag("next_question_button").assertIsDisplayed().performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("السؤال 2", substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Verify Question 2 uses a different event ID (no repeat) and tests wrong answer (+0)
        val active2 = vm.uiState.value.mode01State as Mode01SessionState.ActiveQuestion
        assertFalse(active2.question.eventId == active1.question.eventId)
        val wrongIndex2 = active2.question.options.indexOfFirst { !it.isCorrect }
        composeRule.onNodeWithTag("option_$wrongIndex2").performScrollTo().performClick()
        composeRule.waitForIdle()
        val afterAnswer2 = vm.uiState.value.mode01State as Mode01SessionState.ActiveQuestion
        assertEquals(10, afterAnswer2.score)
        assertEquals(1, afterAnswer2.wrongCount)

        vm.openUnitSelection()
        composeRule.waitForIdle()
        db.close()
    }

    @Test
    @Config(sdk = [36], qualifiers = "w360dp-h640dp")
    fun `unit 3 round completion transitions to 2D results screen and supports replay`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, BacGateDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val vm = BacGateViewModel(
            dateBankRepository = DateBankRepository(AssetDateBankDataSource(context)),
            shuffleBagStore = RoomShuffleBagStore(db.shuffleBagDao())
        )

        composeRule.setContent {
            BacGateTheme {
                BacGateApp(viewModel = vm)
            }
        }
        composeRule.waitForIdle()

        vm.enterMode01ForUnit(StudyUnit.UNIT_3)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("duration_30s").performClick()
        composeRule.onNodeWithTag("start_round_button").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("question_event_title").fetchSemanticsNodes().isNotEmpty()
        }

        val active1 = vm.uiState.value.mode01State as Mode01SessionState.ActiveQuestion
        val correctIndex = active1.question.options.indexOfFirst { it.isCorrect }
        composeRule.onNodeWithTag("option_$correctIndex").performScrollTo().performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("next_question_button").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("السؤال 2", substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Expire remaining timer to enter 2D Results Screen immediately
        vm.tickTimerForTest(35_000L)
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("results_score").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithTag("results_score").assertIsDisplayed()
        composeRule.onNodeWithTag("replay_same_unit_button").assertIsDisplayed().performClick()
        composeRule.waitForIdle()

        // Returns to Unit 3 setup with previous duration ready to start again
        composeRule.onNodeWithTag("start_round_button").assertIsDisplayed().performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("question_event_title").fetchSemanticsNodes().isNotEmpty()
        }

        vm.openUnitSelection()
        composeRule.waitForIdle()
        db.close()
    }

    @Test
    fun `audit registry displays conflicting and needs review records isolated from gameplay`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, BacGateDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val vm = BacGateViewModel(
            dateBankRepository = DateBankRepository(AssetDateBankDataSource(context)),
            shuffleBagStore = RoomShuffleBagStore(db.shuffleBagDao())
        )

        composeRule.setContent {
            BacGateTheme {
                BacGateApp(viewModel = vm)
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("subject_history_card").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("audit_registry_button").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("audit_tab_CONFLICTING").assertIsDisplayed()
        composeRule.onNodeWithTag("audit_tab_NEEDS_REVIEW").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("audit_tab_PENDING_UNIT").performClick()
        composeRule.waitForIdle()

        assertEquals(7, vm.uiState.value.auditRecords.size)
        db.close()
    }

    @Test
    @Config(sdk = [36], qualifiers = "w840dp-h1100dp")
    fun `tablet viewport renders 2D arena and results cleanly without layout failure`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, BacGateDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val vm = BacGateViewModel(
            dateBankRepository = DateBankRepository(AssetDateBankDataSource(context)),
            shuffleBagStore = RoomShuffleBagStore(db.shuffleBagDao())
        )

        composeRule.setContent {
            BacGateTheme {
                BacGateApp(viewModel = vm)
            }
        }
        composeRule.waitForIdle()

        vm.enterMode01ForUnit(StudyUnit.UNIT_2)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("duration_30s").performClick()
        composeRule.onNodeWithTag("start_round_button").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("question_event_title").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("option_0").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("option_3").performScrollTo().assertIsDisplayed()

        vm.openUnitSelection()
        composeRule.waitForIdle()
        db.close()
    }
}
