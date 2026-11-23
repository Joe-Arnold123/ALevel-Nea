package com.example.actualcoursework

import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec

class Encryption { //encryption and decryption functions for any messages sent

     fun encrypt(message:String, recieverID:String,messageId: String,keymanager: KeyManagement): ByteArray{

        val aesKey=keymanager.deriveAesKey(recieverID)
        val cipher = Cipher.getInstance("AES/CTR/NoPadding")
        val ivParameterSpec=determineSalt(sharedSecret = aesKey.encoded,messageId)
        cipher.init(Cipher.ENCRYPT_MODE,aesKey,ivParameterSpec)
        return cipher.doFinal(message.toByteArray())
    }
    fun decrypt(message:ByteArray,senderId: String,messageId: String,keymanager: KeyManagement):String{

        val aesKey=keymanager.deriveAesKey(senderId)
        val cipher=Cipher.getInstance("AES/CTR/NoPadding")
        val ivParameterSpec=determineSalt(sharedSecret = aesKey.encoded,messageId)
        cipher.init(Cipher.DECRYPT_MODE,aesKey,ivParameterSpec)
        return String(cipher.doFinal(message))
    }



    fun determineSalt(sharedSecret:ByteArray,messageID:String): IvParameterSpec{
    val salt = sharedSecret+("Salt").toByteArray()+messageID.toByteArray()
        val saltySalt=MessageDigest.getInstance("SHA-256").digest(salt).take(16).toByteArray()
        return(IvParameterSpec(saltySalt))

    }

}