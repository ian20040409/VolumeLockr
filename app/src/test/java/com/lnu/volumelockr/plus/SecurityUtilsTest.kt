package com.lnu.volumelockr.plus

import com.lnu.volumelockr.plus.util.SecurityUtils
import java.security.MessageDigest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SecurityUtilsTest {

    @Test
    fun generateSalt_returnsValidSaltOfCorrectLength() {
        val salt = SecurityUtils.generateSalt()
        assertNotNull(salt)
        assertEquals(16, salt.size)
    }

    @Test
    fun hashPassword_producesConsistentHashForSameInput() {
        val password = "MySecurePassword123"
        val salt = SecurityUtils.generateSalt()

        val hash1 = SecurityUtils.hashPassword(password, salt)
        val hash2 = SecurityUtils.hashPassword(password, salt)

        assertNotNull(hash1)
        assertNotNull(hash2)
        assertTrue(MessageDigest.isEqual(hash1, hash2))
    }

    @Test
    fun hashPassword_producesDifferentHashesForDifferentPasswords() {
        val salt = SecurityUtils.generateSalt()
        val hash1 = SecurityUtils.hashPassword("PasswordA", salt)
        val hash2 = SecurityUtils.hashPassword("PasswordB", salt)

        assertFalse(MessageDigest.isEqual(hash1, hash2))
    }

    @Test
    fun hashPassword_producesDifferentHashesForDifferentSalts() {
        val password = "CommonPassword"
        val salt1 = SecurityUtils.generateSalt()
        val salt2 = SecurityUtils.generateSalt()

        val hash1 = SecurityUtils.hashPassword(password, salt1)
        val hash2 = SecurityUtils.hashPassword(password, salt2)

        assertFalse(MessageDigest.isEqual(hash1, hash2))
    }
}
