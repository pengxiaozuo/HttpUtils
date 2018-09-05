package com.peng.httputils.https

import java.io.InputStream
import java.security.KeyStore
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.net.ssl.*

class HttpsConfig(builder: Builder) {

    var sslSocketFactory: SSLSocketFactory?
    var hostnameVerifier: HostnameVerifier?
    var trustManager: X509TrustManager?

    init {
        sslSocketFactory = builder.sslSocketFactory
        hostnameVerifier = builder.hostnameVerifier
        trustManager = builder.trustManager
    }


    class UnSafeHostnameVerifier : HostnameVerifier {
        override fun verify(hostname: String, session: SSLSession): Boolean {
            return true
        }
    }

    class UnSafeTrustManager : X509TrustManager {
        @Throws(CertificateException::class)
        override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {
        }

        @Throws(CertificateException::class)
        override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
        }

        override fun getAcceptedIssuers(): Array<X509Certificate> {
            return arrayOf()
        }
    }


    companion object {
        inline fun build(block: Builder.() -> Unit) = Builder().apply(block).build()
    }

    class Builder {

        var sslSocketFactory: SSLSocketFactory? = null
        var trustManager: X509TrustManager? = null
        var hostnameVerifier: HostnameVerifier? = null
        var allAllow = false
        private var tms: Array<TrustManager>? = null
        private var kms: Array<KeyManager>? = null
        /**
         * 导入客户端证书
         */
        fun clientCertificate(input: InputStream, password: String? = null,
                              type: String = "BKS"): Builder {
            try {
                val keyStore = KeyStore.getInstance(type)
                keyStore.load(input, password?.toCharArray())

                val keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
                keyManagerFactory.init(keyStore, password?.toCharArray())
                kms = keyManagerFactory.keyManagers
            } catch (e: Exception) {
                e.printStackTrace()
            }

            return this
        }

        /**
         * 导入服务端证书
         */
        fun serverCertificate(vararg certificates: InputStream, type: String = KeyStore.getDefaultType()): Builder {
            try {
                val certificateFactory = CertificateFactory.getInstance("X.509")
                val trustStore = KeyStore.getInstance(type)
                trustStore.load(null)

                for ((index, certificate) in certificates.withIndex()) {
                    val certificateAlias = Integer.toString(index)
                    trustStore.setCertificateEntry(certificateAlias, certificateFactory.generateCertificate(certificate))
                }

                val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
                trustManagerFactory.init(trustStore)
                tms = trustManagerFactory.trustManagers
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return this
        }

        fun hostnameVerifier(hostnameVerifier: HostnameVerifier) {
            this.hostnameVerifier = hostnameVerifier
        }

        fun build(): HttpsConfig {

            val sslContext = SSLContext.getInstance("TLS")

            if (allAllow) {
                trustManager = UnSafeTrustManager()
                hostnameVerifier = UnSafeHostnameVerifier()
            } else {
                tms?.forEach {
                    if (it is X509TrustManager) {
                        trustManager = it
                        return@forEach
                    }
                }
            }

            val trustManagers: Array<X509TrustManager>? = if (trustManager == null) {
                null
            } else {
                arrayOf(trustManager!!)
            }

            sslContext.init(kms, trustManagers, null)
            sslSocketFactory = sslContext.socketFactory
            return HttpsConfig(this)
        }

    }
}