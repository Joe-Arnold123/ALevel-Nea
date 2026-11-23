package com.example.actualcoursework

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.Key
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import java.security.PublicKey
import java.security.spec.ECGenParameterSpec
import javax.crypto.KeyAgreement
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

class KeyManagement {

    fun generateKeyPair(alias:String) {
        val ks: KeyStore = KeyStore.getInstance("AndroidKeyStore").apply {
            load(null)
        }
        if (!ks.containsAlias(alias)) {
            val keyPairGenerator =
                KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore",)
            val parameterSpec: KeyGenParameterSpec = KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_AGREE_KEY or KeyProperties.PURPOSE_VERIFY or KeyProperties.PURPOSE_SIGN,

                ).run {
                setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
                setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
                build()
            }
            keyPairGenerator.initialize(parameterSpec)
            val keypair = keyPairGenerator.generateKeyPair()

        }
        else{
            return
        }
    }
    fun getPrivateKey(): Key{
       val ks: KeyStore = KeyStore.getInstance("AndroidKeyStore").apply {
           load(null)}
       val key = ks.getKey("User0",null)
       return key
    }
     fun getPublicKey(alias:String):PublicKey?{

        val ks: KeyStore = KeyStore.getInstance("AndroidKeyStore").apply {
            load(null)}
        val key=ks.getCertificate(alias)?.publicKey

        return key
    }
    fun deriveAesKey(userID:String): SecretKey{
        val keyAgreement= KeyAgreement.getInstance("ECDH").run{
            init(getPrivateKey())
            doPhase(getPublicKey(userID),true)
            generateSecret()

        }
        val sharedSecret :ByteArray=keyAgreement
        val aes = MessageDigest.getInstance("SHA-256").digest(sharedSecret)
        sharedSecret.fill(0)
        return SecretKeySpec(aes,"AES")

    }
}