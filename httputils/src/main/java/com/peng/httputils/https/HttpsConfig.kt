package com.peng.httputils.https

import okhttp3.CertificatePinner
import okhttp3.ConnectionSpec
import java.io.InputStream
import java.security.KeyStore
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.net.ssl.*

class HttpsConfig(builder: Builder) {

    val sslSocketFactory: SSLSocketFactory?
    val hostnameVerifier: HostnameVerifier?
    val trustManager: X509TrustManager?
    val certificatePinner: CertificatePinner?
    val connectionSpecs: List<ConnectionSpec>?

    init {
        sslSocketFactory = builder.sslSocketFactory
        hostnameVerifier = builder.hostnameVerifier
        trustManager = builder.trustManager
        certificatePinner = builder.certificatePinner
        connectionSpecs = builder.connectionSpecs
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

    /*
        下面是源码中tls连接的部分代码

        // Create the wrapper over the connected socket.
          sslSocket = (SSLSocket) sslSocketFactory.createSocket(
              rawSocket, address.url().host(), address.url().port(), true /* autoClose */);

          // Configure the socket's ciphers, TLS versions, and extensions.
          ConnectionSpec connectionSpec = connectionSpecSelector.configureSecureSocket(sslSocket);
          if (connectionSpec.supportsTlsExtensions()) {
            Platform.get().configureTlsExtensions(
                sslSocket, address.url().host(), address.protocols());
          }

          // Force handshake. This can throw!
          sslSocket.startHandshake();
          // block for session establishment
          SSLSession sslSocketSession = sslSocket.getSession();
          Handshake unverifiedHandshake = Handshake.get(sslSocketSession);

          // Verify that the socket's certificates are acceptable for the target host.
          if (!address.hostnameVerifier().verify(address.url().host(), sslSocketSession)) {
            X509Certificate cert = (X509Certificate) unverifiedHandshake.peerCertificates().get(0);
            throw new SSLPeerUnverifiedException("Hostname " + address.url().host() + " not verified:"
                + "\n    certificate: " + CertificatePinner.pin(cert)
                + "\n    DN: " + cert.getSubjectDN().getName()
                + "\n    subjectAltNames: " + OkHostnameVerifier.allSubjectAltNames(cert));
          }

          // Check that the certificate pinner is satisfied by the certificates presented.
          address.certificatePinner().check(address.url().host(),
              unverifiedHandshake.peerCertificates());

          // Success! Save the handshake and the ALPN protocol.
          String maybeProtocol = connectionSpec.supportsTlsExtensions()
              ? Platform.get().getSelectedProtocol(sslSocket)
              : null;
     */

    class Builder {

        var sslSocketFactory: SSLSocketFactory? = null
        var trustManager: X509TrustManager? = null
        var hostnameVerifier: HostnameVerifier? = null
        var certificatePinner: CertificatePinner? = null
        var connectionSpecs: List<ConnectionSpec>? = null
        var allAllow = false
        private var tms: Array<TrustManager>? = null
        private var kms: Array<KeyManager>? = null
        /**
         * 导入客户端自己的证书
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
         * 导入持有的服务端证书
         */
        fun serverCertificate(vararg certificates: InputStream, type: String = KeyStore.getDefaultType()): Builder {
            require(certificates.isNotEmpty()) { "not found certificate" }
            try {
                val certificateFactory = CertificateFactory.getInstance("X.509")
                val keyStore = KeyStore.getInstance(type)
                //去掉系统默认证书
                keyStore.load(null)

                for ((index, certificate) in certificates.withIndex()) {
                    val certificateAlias = Integer.toString(index)
                    //设置服务器证书
                    keyStore.setCertificateEntry(certificateAlias, certificateFactory.generateCertificate(certificate))
                }

                //设置信任管理器默认算法
                val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
                //初始化证书
                trustManagerFactory.init(keyStore)
                tms = trustManagerFactory.trustManagers
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return this
        }

        fun build(): HttpsConfig {

            val sslContext = SSLContext.getInstance("TLS")

            if (allAllow) {
                trustManager = UnSafeTrustManager()
                hostnameVerifier = UnSafeHostnameVerifier()
                certificatePinner = null
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