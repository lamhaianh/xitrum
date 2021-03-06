package xitrum.etag

import java.text.SimpleDateFormat
import java.util.{Locale, TimeZone}

import org.jboss.netty.handler.codec.http.{HttpHeaders, HttpResponse, HttpResponseStatus}
import HttpHeaders.Names._
import HttpHeaders.Values._
import HttpResponseStatus._

import xitrum.Action

object NotModified {
  private val SECS_IN_A_YEAR = 60 * 60 * 24 * 365

  // SimpleDateFormat is locale dependent
  // Avoid the case when Xitrum is run on for example Japanese platform
  private val rfc2822 = {
    val ret = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US)
    ret.setTimeZone(TimeZone.getTimeZone("GMT"))
    ret
  }

  // See PublicResourceServerAction, JSRoutesAction
  val serverStartupTimestamp        = System.currentTimeMillis
  val serverStartupTimestampRfc2822 = formatRfc2822(serverStartupTimestamp)

  def formatRfc2822(timestamp: Long) = rfc2822.format(timestamp)

  /**
   * Max-age header is automatically set for static files.
   * Don't worry that browsers do not pick up new files after you modified them,
   * see the doc about static files.
   *
   * Google recommends 1 year:
   * http://code.google.com/intl/ja/speed/page-speed/docs/caching.html
   *
   * Also set Expires because IEs use Expires, not max-age:
   * http://mrcoles.com/blog/cookies-max-age-vs-expires/
   */
  def setMaxAgeAggressively(response: HttpResponse) {
    if (!response.containsHeader(CACHE_CONTROL)) response.setHeader(CACHE_CONTROL, "public, " + MAX_AGE + "=" + SECS_IN_A_YEAR)
    if (!response.containsHeader(EXPIRES))       response.setHeader(EXPIRES, formatRfc2822(System.currentTimeMillis + SECS_IN_A_YEAR * 1000))
  }
}
