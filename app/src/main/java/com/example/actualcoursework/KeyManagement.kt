package com.example.actualcoursework

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.Key
import java.security.KeyPairGenerator
import java.security.KeyStore

class KeyManagement {

    fun generateKeyPair(alias:String) {

        val keyPairGenerator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore",)
        val parameterSpec: KeyGenParameterSpec = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_AGREE_KEY or KeyProperties.PURPOSE_VERIFY or KeyProperties.PURPOSE_SIGN,

        ).run {
            setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
            build()
        }
        keyPairGenerator.initialize(parameterSpec)
        val keypair=keyPairGenerator.generateKeyPair()
    }
   private fun getPrivateKey(): Key{
       val ks: KeyStore = KeyStore.getInstance("AndroidKeyStore").apply {
           load(null)}
       val key = ks.getKey("User0",null)
       return key
    }
    private fun getPublicKey(alias:String):Key{

        val ks: KeyStore = KeyStore.getInstance("AndroidKeyStore").apply {
            load(null)}
        val publicKey=ks.getKey(alias,null)

        return publicKey
    }
}