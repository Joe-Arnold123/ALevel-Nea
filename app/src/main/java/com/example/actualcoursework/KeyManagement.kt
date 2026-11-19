package com.example.actualcoursework

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyPairGenerator

class KeyManagement {

    fun generateKey(alias:String) {

        val keyPairGenerator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore",)
        val parameterSpec: KeyGenParameterSpec = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_AGREE_KEY or KeyProperties.PURPOSE_VERIFY or KeyProperties.PURPOSE_SIGN
        ).run {
            setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
            build()
        }
        keyPairGenerator.initialize(parameterSpec)
        val keypair=keyPairGenerator.generateKeyPair()




    }
}