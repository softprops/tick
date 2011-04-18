package me.lessis

trait HttpClientProvider {
  import java.net.Socket
  import javax.net.ssl.{X509TrustManager, SSLContext}
  import java.security.KeyStore
  import java.security.cert.X509Certificate
  import	org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager
  import org.apache.http.conn.scheme.{PlainSocketFactory, Scheme, SchemeRegistry}
  import org.apache.http.conn.ssl.SSLSocketFactory
  import org.apache.http.params.{HttpProtocolParams, BasicHttpParams}
  import org.apache.http.protocol.HTTP
  import org.apache.http.HttpVersion
  import org.apache.http.impl.client.DefaultHttpClient

  private def sslFactory = {
    val trust = KeyStore.getInstance(KeyStore.getDefaultType())
    trust.load(null, null)
    val ctx = SSLContext.getInstance(SSLSocketFactory.TLS)
    val tm = new X509TrustManager {
      def checkClientTrusted(xcs: Array[X509Certificate], string: String) {}
      def checkServerTrusted(xcs: Array[X509Certificate], string: String) {}
      def getAcceptedIssuers = null
    }
    ctx.init(null, Array(tm), null)
    new SSLSocketFactory(trust) {
      setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER)
      override def createSocket(socket: Socket, host: String,
                                port: Int, autoClose: Boolean) =
         ctx.getSocketFactory.createSocket(socket, host, port, autoClose)
      override def createSocket = ctx.getSocketFactory.createSocket

    }
  }

  def lenientHttp: DefaultHttpClient = {
    val registry = new SchemeRegistry()
    registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80))
    registry.register(new Scheme("https", sslFactory, 443))
    val params = new BasicHttpParams()
    HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1)
    HttpProtocolParams.setContentCharset(params, HTTP.UTF_8)
    val ccm = new ThreadSafeClientConnManager(
      params, registry
    )
    new DefaultHttpClient(ccm, params)
  }
}
