package com.gymapp.minimalist.data.security

import android.content.Context
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.security.MessageDigest

object CryptoUtils {

    /**
     * Calcula o hash SHA-256 de uma string para verificação de integridade
     */
    fun computeSha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Sanitiza strings recebidas de fontes externas (como arquivos JSON importados)
     * Removendo caracteres de controle perigosos e truncando para limites aceitáveis.
     */
    fun sanitizeString(input: String, maxLength: Int = 100): String {
        val trimmed = input.trim()
        val sanitized = trimmed.replace(Regex("[\\p{Cntrl}&&[^\r\n\t]]"), "")
        return if (sanitized.length > maxLength) sanitized.substring(0, maxLength) else sanitized
    }

    /**
     * Cria ou obtém a chave mestra no hardware seguro (Android Keystore)
     */
    fun getOrCreateMasterKey(context: Context): MasterKey {
        return MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    /**
     * Escreve dados em um arquivo criptografado com AES-256-GCM via Android Keystore
     */
    fun writeEncryptedFile(context: Context, destinationFile: File, data: ByteArray) {
        if (destinationFile.exists()) {
            destinationFile.delete()
        }
        val masterKey = getOrCreateMasterKey(context)
        val encryptedFile = EncryptedFile.Builder(
            context,
            destinationFile,
            masterKey,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()

        encryptedFile.openFileOutput().use { outputStream ->
            outputStream.write(data)
            outputStream.flush()
        }
    }

    /**
     * Lê dados de um arquivo criptografado via Android Keystore
     */
    fun readEncryptedFile(context: Context, sourceFile: File): ByteArray {
        val masterKey = getOrCreateMasterKey(context)
        val encryptedFile = EncryptedFile.Builder(
            context,
            sourceFile,
            masterKey,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()

        val byteBuffer = ByteArrayOutputStream()
        encryptedFile.openFileInput().use { inputStream: InputStream ->
            val buffer = ByteArray(1024)
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                byteBuffer.write(buffer, 0, bytesRead)
            }
        }
        return byteBuffer.toByteArray()
    }
}
