package com.dailyfit.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationSmokeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun bottomNavigationSwitchesBetweenHomePlanAndHistory() {
        composeRule.onNodeWithText("DailyFit").assertIsDisplayed()

        composeRule.onNodeWithText("计划").performClick()
        composeRule.onNodeWithText("训练计划").assertIsDisplayed()

        composeRule.onNodeWithText("历史").performClick()
        composeRule.onNodeWithText("历史与统计").assertIsDisplayed()

        composeRule.onNodeWithText("首页").performClick()
        composeRule.onNodeWithText("今日习惯").assertIsDisplayed()

        composeRule.onNodeWithText("开始训练").performClick()
        composeRule.onNodeWithText("训练执行").assertIsDisplayed()
        composeRule.onNodeWithText("返回").performClick()
    }
}
