package xitrum

import java.io.File
import java.nio.charset.Charset

import com.hazelcast.client.HazelcastClient
import com.hazelcast.core.{Hazelcast, HazelcastInstance}

import xitrum.scope.session.SessionStore
import xitrum.util.Loader

/** See config/xitrum.properties */
object Config extends Logger {
  /** See bin/runner.sh */
  val isProductionMode = (System.getProperty("xitrum.mode") == "production")

  val properties = {
    try {
      Loader.propertiesFromClasspath("xitrum.properties")
    } catch {
      case _ =>
        logger.error("Could not load xitrum.properties. The \"config\" directory should be in CLASSPATH.")
        System.exit(-1)
        null
    }
  }

  //----------------------------------------------------------------------------

  val httpPort = getPropertyWithoudDefault("http_port").toInt

  val proxyIpso: Option[Array[String]] = {
    val s = properties.getProperty("proxy_ips")
    if (s == null) None else Some(s.split(",").map(_.trim))
  }

  val baseUri = properties.getProperty("base_uri", "")

  val hazelcastInstance: HazelcastInstance = {
    val hazelcastMode = getPropertyWithoudDefault("hazelcast_mode")

    // http://code.google.com/p/hazelcast/issues/detail?id=94
    // http://code.google.com/p/hazelcast/source/browse/trunk/hazelcast/src/main/java/com/hazelcast/logging/Logger.java
    System.setProperty("hazelcast.logging.type", "slf4j")

    // http://www.hazelcast.com/documentation.jsp#SuperClient
    if (hazelcastMode == "super_client")
      System.setProperty("hazelcast.super.client", "true")

    // http://code.google.com/p/hazelcast/wiki/Config
    // http://code.google.com/p/hazelcast/source/browse/trunk/hazelcast/src/main/java/com/hazelcast/config/XmlConfigBuilder.java
    if (hazelcastMode == "super_client" || hazelcastMode == "cluster_member") {
      val config = System.getProperty("user.dir") + File.separator + "config" + File.separator + "hazelcast_cluster_member_or_super_client.xml"
      System.setProperty("hazelcast.config", config)
      Hazelcast.getDefaultInstance
    } else {
      val props = Loader.propertiesFromClasspath("hazelcast_java_client.properties")
      val groupName     = props.getProperty("group_name")
      val groupPassword = props.getProperty("group_password")
      val addresses     = props.getProperty("addresses").split(",").map(_.trim)
      HazelcastClient.newHazelcastClient(groupName, groupPassword, addresses:_*)
    }
  }

  //----------------------------------------------------------------------------

  val sessionStore  = {
    val className = getPropertyWithoudDefault("session_store")
    Class.forName(className).newInstance.asInstanceOf[SessionStore]
  }

  val sessionCookieName = getPropertyWithoudDefault("session_cookie_name")

  val secureKey = getPropertyWithoudDefault("secure_key")

  //----------------------------------------------------------------------------

  val maxRequestContentLengthInMB   = getPropertyWithoudDefault("max_request_content_length_in_mb").toInt

  val paramCharsetName              = getPropertyWithoudDefault("param_charset")
  val paramCharset                  = Charset.forName(paramCharsetName)

  val filteredParams                = getPropertyWithoudDefault("filtered_params").split(",").map(_.trim)

  val publicFilesNotBehindPublicUrl = getPropertyWithoudDefault("public_files_not_behind_public_url").split(",").map(_.trim)

  val smallStaticFileSizeInKB       = getPropertyWithoudDefault("small_static_file_size_in_kb").toInt
  val maxCachedSmallStaticFiles     = getPropertyWithoudDefault("max_cached_small_static_files").toInt

  /**
   * Static textual files are always compressed
   * Dynamic textual responses are only compressed if they are big
   * http://code.google.com/intl/en/speed/page-speed/docs/payload.html#GzipCompression
   *
   * Google recommends > 150B-1KB
   */
  val BIG_TEXTUAL_RESPONSE_SIZE_IN_KB    = 1

  /**
   * When there is trouble (high load on startup ect.), the response may not be
   * OK. If the response is specified to be cached, we should only cache it
   * for a short time.
   */
  val NON_200_RESPONSE_CACHE_TTT_IN_SECS = 30

  //----------------------------------------------------------------------------

  // For default value, use properties.getProperty(key, default) directly
  private def getPropertyWithoudDefault(key: String): String = {
    try {
      properties.getProperty(key)
    } catch {
      case _ =>
        logger.error("Could not load propery \"" + key + "\" in xitrum.properties. You probably forgot to update xitrum.properties when updating Xitrum.")
        System.exit(-1)
        null
    }
  }
}
