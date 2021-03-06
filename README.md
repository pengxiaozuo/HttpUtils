# HttpUtils
Http封装，提供简介的配置和使用方式，有问题可以留言讨论

## gradle配置

在module的build.gradle文件中配置

```gradle
implementation 'com.peng.httputils:httputils:0.3.1'
```

## 在项目中使用

在Application中配置，除了baseUrl之外，其他都是可选配置

### 最简单的配置

```kotlin
//最简单的初始化，使用默认配置
//初始化配置
HttpUtils.init(GithubService.baseUrl) //GithubService.baseUrl在下面的接口中定义
```

### 详细的配置

```kotlin
//详细指定需要的配置
//初始化，如果初始化过多个baseUrl的话，在create时，需要指定baseUrl
HttpUtils.init(HttpsService.baseUrl) {
    //是否打印http的Request Response信息 默认false
    printLog = BuildConfig.DEBUG
    //连接超时配置单位毫秒 默认10000L
    connectTimeout = 10000L
    //读取超时配置单位毫秒 默认10000L
    readTimeout = 10000L
    //写超时配置单位毫秒 默认10000L
    writeTimeout = 10000L
    //是否失败重试 默认true
    retry = true
    //配置cookie缓存实现 默认null
    cookieJar = null
    //给当前初始化的baseUrls指定全局的Headers，如果请求头中已经包含Header则不替换 默认null
    headers = mapOf("Cache-Control" to "public, max-age=60")
    //配置缓存目录和大小 默认null
    cache = Cache(File(getExternalFilesDir(null), "http-cache"), 10 * 1024 * 1024)
    //添加一个拦截器，无网络是尝试访问缓存
    addInterceptor(NetworkCacheInterceptor(this@MyApplication))
    //添加一个CallAdapter.Factory，可以在接口的返回值类型搞事情
    addCallAdapterFactory(RxJava2CallAdapterFactory.create())
    //添加一个Converter.Factory
    addConverterFactory(GsonConverterFactory.create())

    //配置处理身份验证，如果服务器返回401okhttp内置的重试重定向拦截器会调用此配置 默认null
    authenticator = Authenticator { route, response ->
        return@Authenticator if (response.request().header("Authorization") != null) {
            null // Give up, we've already attempted to authenticate.
        } else {
            val credential = Credentials.basic("jesse", "password1")
            response.request().newBuilder()
                    .header("Authorization", credential)
                    .build()
        }
    }
    //配置代理，okhttp在初始化socket的时候会设置此代理 默认null
    proxy = null

    //配置代理选择器 默认null
    proxySelector = null
    //配置处理身份验证，如果服务器返回407okhttp内置的重试重定向拦截器会调用此配置 默认null
    proxyAuthenticator = Authenticator { route, response ->
        return@Authenticator if (response.request().header("Authorization") != null) {
            null // Give up, we've already attempted to authenticate.
        } else {
            val credential = Credentials.basic("jesse", "password1")
            response.request().newBuilder()
                    .header("Authorization", credential)
                    .build()
        }
    }

    /*
        * Https配置
        *
        *  可参考OkHttp中Tls连接部分代码，流程如下：
        *      用sslSocketFactory.createSocket创建一个SSLSocket
        *      配置密码套件部分
        *      开始握手
        *      拿到SSLSession 获取握手信息
        *      主机名验证
        *      证书绑定验证
        *      获取协议
        */
    httpsConfig = HttpsConfig.build {
        //全部允许 默认false
        allAllow = true

        //配置密码套件 默认null
        connectionSpecs = listOf(ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                .tlsVersions(TlsVersion.TLS_1_2)
                .cipherSuites(
                        CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,
                        CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,
                        CipherSuite.TLS_DHE_RSA_WITH_AES_128_GCM_SHA256)
                .build()
        )

        //主机名验证
        hostnameVerifier = HostnameVerifier { hostname, session ->
            true
        }
        //证书绑定 ,如果绑定的签名有问题，可以在异常信息中获得正确的签名 默认null
        certificatePinner = CertificatePinner.Builder()
                //.add("publicobject.com", "sha256/afwiKY3RxoMmLkuRW1l7QsPZTJPwDS2pdDROQjXw8ig=")
                .build()
        //直接指定sslSocketFactory，而不用下面的导入证书方法生成 默认null
        sslSocketFactory = null
        //trustManager，而不用下面的导入证书方法生成 默认null
        trustManager = null
        //客户端证书，双向认证用
        //clientCertificate(inputStream,"password")
        //服务端证书，导入信任列表
        //serverCertificate(inputStream,type = KeyStore.getDefaultType())
    }
}
```

### 多个baseUrl使用统一配置

```kotlin
//初始化，一次用同一个配置初始化多个baseUrl
HttpUtils.init(GithubService.baseUrl, HttpsService.baseUrl)
```

使用方式和retrofit一毛一样

定义api接口

```kotlin
interface GithubService {

    companion object {
        const val baseUrl = "https://api.github.com/"
    }

    @GET("users/{login}")
    fun getUser(@Path("login") login: String): Call<ResponseBody>
}

interface HttpsService {
    companion object {
        const val baseUrl = "https://revoked.badssl.com/"
    }

    @GET("/")
    fun test(): Observable<ResponseBody>
}
```

### 基本使用

```kotlin
//使用已经初始化好的Retrofit创建服务
val githubService = HttpUtils.create(GithubService::class.java)
//使用创建好的服务获取结果
val response = githubService.getUser("pengxiaozuo").execute()
val msg = response.body()?.string()
Log.d("githubService", msg)
```

### 使用RxJava2CallAdapter

```kotlin

//使用已经初始化好的Retrofit创建服务,如果初始化过多个baseUrl需要指定具体的baseUrl
val httpsService = HttpUtils.create(HttpsService::class.java, HttpsService.baseUrl)
httpsService.test().subscribeOn(Schedulers.io())
        .subscribe({
            Log.d("httpsService", "String Length: ${it.string().length}")
        }, {
            Log.e("httpsService", "httpsService request error", it)
        })
```

### 直接创建服务

不用初始化配置，直接创建服务，HttpUtils不会保存此服务

```kotlin
//初始化客户端并创建服务，客户端和配置信息并不会保存
val httpsServiceTemp = HttpUtils.create(HttpsService.baseUrl, HttpsService::class.java) {
    //配置信息
    addCallAdapterFactory(RxJava2CallAdapterFactory.create())
    //https 配置信息
    httpsConfig = HttpsConfig.build {
    }
}

httpsServiceTemp.test()
        .subscribeOn(Schedulers.io())
        .subscribe({
            Log.d("httpsServiceTemp", "String Length: ${it.string().length}")
        }, {
            it.printStackTrace()
            Log.e("httpsServiceTemp", "httpsServiceTemp request error", it)
        })
```

### 用到的权限

```xml
<uses-permission android:name="android.permission.INTERNET"/>
```

```xml
<!-- 无网络情况下访问缓存缓存的拦截器，如果添加了此拦截器需要使用改权限 -->
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>
```

```xml
<!-- 如果设置了缓存目录并且目录在外部存储，需要使用该权限 -->
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
```
