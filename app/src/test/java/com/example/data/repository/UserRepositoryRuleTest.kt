package com.example.data.repository

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UserRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun testGetOrCreateProfile_authenticated_createsDocument() = runTest {
    val uid = signInTestUser("player1@example.com")
    val repository = UserRepository(firestore)

    val profile = repository.getOrCreateProfile("RoyaleKing", "avatar_king")
    assertNotNull(profile)
    assertEquals(uid, profile.userId)
    assertEquals("RoyaleKing", profile.displayName)
    assertTrue(profile.coinBalance >= 500L)
  }

  @Test
  fun testAddDeposit_incrementsBalance() = runTest {
    signInTestUser("depositor@example.com")
    val repository = UserRepository(firestore)
    val initialProfile = repository.getOrCreateProfile("Depositor", "avatar_king")
    val initialBalance = initialProfile.coinBalance

    repository.addDeposit(200L, "UPI")
    val updatedProfile = repository.getOrCreateProfile("Depositor", "avatar_king")
    assertEquals(initialBalance + 200L, updatedProfile.coinBalance)
  }
}
