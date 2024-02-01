package security.vault

import android.annotation.SuppressLint
import android.content.SharedPreferences
import security.Security
import security.atomicRead
import security.atomicWrite
import security.clear
import security.decrypt
import security.encrypt
import security.getByteArray
import security.putByteArray
import java.io.File
import javax.crypto.SecretKey

internal class Storage(
    private val prefs: SharedPreferences
) {

    private companion object {
        private const val IV_SIZE = 16

        fun wrapBodyKey(id: Long): String {
            return "item_body_$id"
        }

        fun wrapIvKey(id: Long): String {
            return "item_iv_$id"
        }
    }

    fun get(id: Long, secret: SecretKey): ByteArray {
        val iv = getIv(id) ?: throw IllegalStateException("iv is empty")
        val encrypted = getBody(id)
        if (encrypted == null) {
            iv.clear()
            throw IllegalStateException("encrypted is empty")
        }
        val data = secret.decrypt(iv, encrypted)
        clear(iv, encrypted)
        return data ?: throw IllegalStateException("data is empty")
    }

    fun put(secret: SecretKey, id: Long, data: ByteArray) {
        if (data.isEmpty()) {
            delete(id)
        } else {
            val iv = Security.randomBytes(IV_SIZE)
            val encrypted = secret.encrypt(iv, data)
            data.clear()
            if (encrypted == null) {
                clear(iv)
                throw IllegalStateException("encrypted is empty")
            }
            setIvAndBody(id, iv, encrypted)
        }
    }

    private fun getIv(id: Long): ByteArray? {
        return prefs.getByteArray(wrapIvKey(id))
    }

    private fun getBody(id: Long): ByteArray? {
        return prefs.getByteArray(wrapBodyKey(id))
    }

    @SuppressLint("ApplySharedPref")
    private fun delete(id: Long) {
        prefs.edit()
            .remove(wrapIvKey(id))
            .remove(wrapBodyKey(id))
            .commit()
    }

    @SuppressLint("ApplySharedPref")
    private fun setIvAndBody(id: Long, iv: ByteArray, body: ByteArray) {
        prefs.edit()
            .putByteArray(wrapIvKey(id), iv)
            .putByteArray(wrapBodyKey(id), body)
            .commit()
    }
}