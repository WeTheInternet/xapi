package java.net;

import java.io.InputStream;
import java.net.MalformedURLException;

/**
 * A heavily stripped-down URL suitable for use in GWT client-side.
 * 
 * It could do with a lot more trimming, and use of native js encodeURIComponent
 * 
 */
public final class URL implements java.io.Serializable {

  static final long serialVersionUID = -7627629688361524110L;

  /**
   * The protocol to use (ftp, http, nntp, ... etc.) .
   * 
   * @serial
   */
  private String protocol;

  /**
   * The host name to connect to.
   * 
   * @serial
   */
  private String host;

  /**
   * The protocol port to connect to.
   * 
   * @serial
   */
  private int port = -1;

  /**
   * The specified file name on that host. <code>file</code> is defined as <code>path[?query]</code>
   * 
   * @serial
   */
  private String file;

  /**
   * The query part of this URL.
   */
  private transient String query;

  /**
   * The authority part of this URL.
   * 
   * @serial
   */
  private String authority;

  /**
   * The path part of this URL.
   */
  private transient String path;

  /**
   * The userinfo part of this URL.
   */
  private transient String userInfo;

  /**
   * # reference.
   * 
   * @serial
   */
  private String ref;

  /**
   * The URLStreamHandler for this URL.
   */
  // transient URLStreamHandler handler; //XXX

  /* Our hash code.
   * @serial */
  private int hashCode = -1;

  /**
   * Creates a <code>URL</code> object from the <code>String</code> representation.
   * <p>
   * This constructor is equivalent to a call to the two-argument constructor with a <code>null</code> first
   * argument.
   * 
   * @param spec the <code>String</code> to parse as a URL.
   * @exception MalformedURLException If the string specifies an unknown protocol.
   * @see java.net.URL#URL(java.net.URL, java.lang.String)
   */
  public URL(String spec) throws MalformedURLException {
    this(null, spec);
  }

  /**
   * Creates a URL by parsing the given spec within a specified context. The new URL is created from the given
   * context URL and the spec argument as described in RFC2396 &quot;Uniform Resource Identifiers : Generic *
   * Syntax&quot; : <blockquote>
   * 
   * <pre>
   *          &lt;scheme&gt;://&lt;authority&gt;&lt;path&gt;?&lt;query&gt;#&lt;fragment&gt;
   * </pre>
   * 
   * </blockquote> The reference is parsed into the scheme, authority, path, query and fragment parts. If the
   * path component is empty and the scheme, authority, and query components are undefined, then the new URL
   * is a reference to the current document. Otherwise, the fragment and query parts present in the spec are
   * used in the new URL.
   * <p>
   * If the scheme component is defined in the given spec and does not match the scheme of the context, then
   * the new URL is created as an absolute URL based on the spec alone. Otherwise the scheme component is
   * inherited from the context URL.
   * <p>
   * If the authority component is present in the spec then the spec is treated as absolute and the spec
   * authority and path will replace the context authority and path. If the authority component is absent in
   * the spec then the authority of the new URL will be inherited from the context.
   * <p>
   * If the spec's path component begins with a slash character &quot;/&quot; then the path is treated as
   * absolute and the spec path replaces the context path.
   * <p>
   * Otherwise, the path is treated as a relative path and is appended to the context path, as described in
   * RFC2396. Also, in this case, the path is canonicalized through the removal of directory changes made by
   * occurences of &quot;..&quot; and &quot;.&quot;.
   * <p>
   * For a more detailed description of URL parsing, refer to RFC2396.
   * 
   * @param context the context in which to parse the specification.
   * @param spec the <code>String</code> to parse as a URL.
   * @exception MalformedURLException if no protocol is specified, or an unknown protocol is found.
   * @see java.net.URL#URL(java.lang.String, java.lang.String, int, java.lang.String)
   * @see java.net.URLStreamHandler
   * @see java.net.URLStreamHandler#parseURL(java.net.URL, java.lang.String, int, int)
   */
  public URL(URL context, String spec) throws MalformedURLException {
    String original = spec;
    int i, limit, c;
    int start = 0;
    String newProtocol = null;
    boolean aRef = false;
    boolean isRelative = false;

    try {
      limit = spec.length();
      while ((limit > 0) && (spec.charAt(limit - 1) <= ' ')) {
        limit--; // eliminate trailing whitespace
      }
      while ((start < limit) && (spec.charAt(start) <= ' ')) {
        start++; // eliminate leading whitespace
      }

      if (spec.regionMatches(true, start, "url:", 0, 4)) {
        start += 4;
      }
      if (start < spec.length() && spec.charAt(start) == '#') {
        /* we're assuming this is a ref relative to the context URL. This means protocols cannot start w/ '#',
         * but we must parse ref URL's like: "hello:there" w/ a ':' in them. */
        aRef = true;
      }
      for (i = start; !aRef && (i < limit) && ((c = spec.charAt(i)) != '/'); i++) {
        if (c == ':') {

          String s = spec.substring(start, i).toLowerCase();
          if (isValidProtocol(s)) {
            newProtocol = s;
            start = i + 1;
          }
          break;
        }
      }

      // Only use our context if the protocols match.
      protocol = newProtocol;
      if ((context != null) && ((newProtocol == null) || newProtocol.equalsIgnoreCase(context.protocol))) {
        // inherit the protocol handler from the context
        // if not specified to the constructor

        // If the context is a hierarchical URL scheme and the spec
        // contains a matching scheme then maintain backwards
        // compatibility and treat it as if the spec didn't contain
        // the scheme; see 5.2.3 of RFC2396
        if (context.path != null && context.path.startsWith("/")) newProtocol = null;

        if (newProtocol == null) {
          protocol = context.protocol;
          authority = context.authority;
          userInfo = context.userInfo;
          host = context.host;
          port = context.port;
          file = context.file;
          path = context.path;
          isRelative = true;
        }
      }

      if (protocol == null) {
        throw new MalformedURLException("no protocol: " + original);
      }

      i = spec.indexOf('#', start);
      if (i >= 0) {
        ref = spec.substring(i + 1, limit);
        limit = i;
      }

      /* Handle special case inheritance of query and fragment implied by RFC2396 section 5.2.2. */
      if (isRelative && start == limit) {
        query = context.query;
        if (ref == null) {
          ref = context.ref;
        }
      }

      parseURL(this, spec, start, limit);

    } catch (MalformedURLException e) {
      throw e;
    } catch (Exception e) {
      throw new MalformedURLException(e.getMessage());
    }
  }

  /**
   * Parses the string representation of a <code>URL</code> into a <code>URL</code> object.
   * <p>
   * If there is any inherited context, then it has already been copied into the <code>URL</code> argument.
   * <p>
   * The <code>parseURL</code> method of <code>URLStreamHandler</code> parses the string representation as if
   * it were an <code>http</code> specification. Most URL protocol families have a similar parsing. A stream
   * protocol handler for a protocol that has a different syntax must override this routine.
   * 
   * @param u the <code>URL</code> to receive the result of parsing the spec.
   * @param spec the <code>String</code> representing the URL that must be parsed.
   * @param start the character index at which to begin parsing. This is just past the '<code>:</code>' (if
   * there is one) that specifies the determination of the protocol name.
   * @param limit the character position to stop parsing at. This is the end of the string or the position of
   * the "<code>#</code>" character, if present. All information after the sharp sign indicates an anchor.
   */
  protected void parseURL(URL u, String spec, int start, int limit) {
    // These fields may receive context content if this was relative URL
    String protocol = u.getProtocol();
    String authority = u.getAuthority();
    String userInfo = u.getUserInfo();
    String host = u.getHost();
    int port = u.getPort();
    String path = u.getPath();
    String query = u.getQuery();

    // This field has already been parsed
    String ref = u.getRef();

    boolean isRelPath = false;
    boolean queryOnly = false;

    // FIX: should not assume query if opaque
    // Strip off the query part
    if (start < limit) {
      int queryStart = spec.indexOf('?');
      queryOnly = queryStart == start;
      if ((queryStart != -1) && (queryStart < limit)) {
        query = spec.substring(queryStart + 1, limit);
        if (limit > queryStart) limit = queryStart;
        spec = spec.substring(0, queryStart);
      }
    }

    int i = 0;
    // Parse the authority part if any
    boolean isUNCName = (start <= limit - 4) && (spec.charAt(start) == '/') &&
      (spec.charAt(start + 1) == '/') && (spec.charAt(start + 2) == '/') && (spec.charAt(start + 3) == '/');
    if (!isUNCName && (start <= limit - 2) && (spec.charAt(start) == '/') && (spec.charAt(start + 1) == '/')) {
      start += 2;
      i = spec.indexOf('/', start);
      if (i < 0) {
        i = spec.indexOf('?', start);
        if (i < 0) i = limit;
      }

      host = authority = spec.substring(start, i);

      int ind = authority.indexOf('@');
      if (ind != -1) {
        userInfo = authority.substring(0, ind);
        host = authority.substring(ind + 1);
      } else {
        userInfo = null;
      }
      if (host != null) {
        // If the host is surrounded by [ and ] then its an IPv6
        // literal address as specified in RFC2732
        if (host.length() > 0 && (host.charAt(0) == '[')) {
          if ((ind = host.indexOf(']')) > 2) {

            String nhost = host;
            host = nhost.substring(0, ind + 1);
            if (!isIPv6LiteralAddress(host.substring(1, ind))) {
              throw new IllegalArgumentException("Invalid host: " + host);
            }

            port = -1;
            if (nhost.length() > ind + 1) {
              if (nhost.charAt(ind + 1) == ':') {
                ++ind;
                // port can be null according to RFC2396
                if (nhost.length() > (ind + 1)) {
                  port = Integer.parseInt(nhost.substring(ind + 1));
                }
              } else {
                throw new IllegalArgumentException("Invalid authority field: " + authority);
              }
            }
          } else {
            throw new IllegalArgumentException("Invalid authority field: " + authority);
          }
        } else {
          ind = host.indexOf(':');
          port = -1;
          if (ind >= 0) {
            // port can be null according to RFC2396
            if (host.length() > (ind + 1)) {
              port = Integer.parseInt(host.substring(ind + 1));
            }
            host = host.substring(0, ind);
          }
        }
      } else {
        host = "";
      }
      if (port < -1) throw new IllegalArgumentException("Invalid port number :" + port);
      start = i;
      // If the authority is defined then the path is defined by the
      // spec only; See RFC 2396 Section 5.2.4.
      if (authority != null && authority.length() > 0) path = "";
    }

    if (host == null) {
      host = "";
    }

    // Parse the file path if any
    if (start < limit) {
      if (spec.charAt(start) == '/') {
        path = spec.substring(start, limit);
      } else if (path != null && path.length() > 0) {
        isRelPath = true;
        int ind = path.lastIndexOf('/');
        String seperator = "";
        if (ind == -1 && authority != null) seperator = "/";
        path = path.substring(0, ind + 1) + seperator + spec.substring(start, limit);

      } else {
        String seperator = (authority != null) ? "/" : "";
        path = seperator + spec.substring(start, limit);
      }
    } else if (queryOnly && path != null) {
      int ind = path.lastIndexOf('/');
      if (ind < 0) ind = 0;
      path = path.substring(0, ind) + "/";
    }
    if (path == null) path = "";

    if (isRelPath) {
      // Remove embedded /./
      while ((i = path.indexOf("/./")) >= 0) {
        path = path.substring(0, i) + path.substring(i + 2);
      }
      // Remove embedded /../ if possible
      i = 0;
      while ((i = path.indexOf("/../", i)) >= 0) {
        /* A "/../" will cancel the previous segment and itself, unless that segment is a "/../" itself i.e.
         * "/a/b/../c" becomes "/a/c" but "/../../a" should stay unchanged */
        if (i > 0 && (limit = path.lastIndexOf('/', i - 1)) >= 0 && (path.indexOf("/../", limit) != 0)) {
          path = path.substring(0, limit) + path.substring(i + 3);
          i = 0;
        } else {
          i = i + 3;
        }
      }
      // Remove trailing .. if possible
      while (path.endsWith("/..")) {
        i = path.indexOf("/..");
        if ((limit = path.lastIndexOf('/', i - 1)) >= 0) {
          path = path.substring(0, limit + 1);
        } else {
          break;
        }
      }
      // Remove starting .
      if (path.startsWith("./") && path.length() > 2) path = path.substring(2);

      // Remove trailing .
      if (path.endsWith("/.")) path = path.substring(0, path.length() - 1);
    }

    u.set(protocol, host, port, authority, userInfo, path, query, ref);
  }

  private final static int INADDR4SZ = 4;
  private final static int INADDR16SZ = 16;
  private final static int INT16SZ = 2;

  private boolean isIPv6LiteralAddress(String src) {

    // Shortest valid string is "::", hence at least 2 chars
    if (src.length() < 2) {
      return false;
    }
    int colonp;
    char ch;
    boolean saw_xdigit;
    int val;
    char[] srcb = src.toCharArray();
    byte[] dst = new byte[INADDR16SZ];
    int srcb_length = srcb.length;
    int pc = src.indexOf("%");
    if (pc == srcb_length - 1) {
      return false;
    }
    if (pc != -1) {
      srcb_length = pc;
    }
    colonp = -1;
    int i = 0, j = 0;
    /* Leading :: requires some special handling. */
    if (srcb[i] == ':') if (srcb[++i] != ':') return false;
    int curtok = i;
    saw_xdigit = false;
    val = 0;
    while (i < srcb_length) {
      ch = srcb[i++];
      int chval = Character.digit(ch, 16);
      if (chval != -1) {
        val <<= 4;
        val |= chval;
        if (val > 0xffff) return false;
        saw_xdigit = true;
        continue;
      }
      if (ch == ':') {
        curtok = i;
        if (!saw_xdigit) {
          if (colonp != -1) return false;
          colonp = j;
          continue;
        } else if (i == srcb_length) {
          return false;
        }
        if (j + INT16SZ > INADDR16SZ) return false;
        dst[j++] = (byte)((val >> 8) & 0xff);
        dst[j++] = (byte)(val & 0xff);
        saw_xdigit = false;
        val = 0;
        continue;
      }
      if (ch == '.' && ((j + INADDR4SZ) <= INADDR16SZ)) {
        String ia4 = src.substring(curtok, srcb_length);
        /* check this IPv4 address has 3 dots, ie. A.B.C.D */
        int dot_count = 0, index = 0;
        while ((index = ia4.indexOf('.', index)) != -1) {
          dot_count++;
          index++;
        }
        if (dot_count != 3) {
          return false;
        }
        byte[] v4addr = textToNumericFormatV4(ia4);
        if (v4addr == null) {
          return false;
        }
        for (int k = 0; k < INADDR4SZ; k++) {
          dst[j++] = v4addr[k];
        }
        saw_xdigit = false;
        break; /* '\0' was seen by inet_pton4(). */
      }
      return false;
    }
    if (saw_xdigit) {
      if (j + INT16SZ > INADDR16SZ) return false;
      dst[j++] = (byte)((val >> 8) & 0xff);
      dst[j++] = (byte)(val & 0xff);
    }
    if (colonp != -1) {
      int n = j - colonp;
      if (j == INADDR16SZ) return false;
      for (i = 1; i <= n; i++) {
        dst[INADDR16SZ - i] = dst[colonp + n - i];
        dst[colonp + n - i] = 0;
      }
      j = INADDR16SZ;
    }
    if (j != INADDR16SZ) return false;
    byte[] newdst = convertFromIPv4MappedAddress(dst);
    if (newdst != null) {
      return true;
    } else {
      return dst != null;
    }
  }

  public static byte[] textToNumericFormatV4(String src) {
    if (src.length() == 0) {
      return null;
    }

    byte[] res = new byte[INADDR4SZ];
    String[] s = src.split("\\.", -1);
    long val;
    try {
      switch (s.length) {
      case 1:
        /* When only one part is given, the value is stored directly in the network address without any byte
         * rearrangement. */

        val = Long.parseLong(s[0]);
        if (val < 0 || val > 0xffffffffL) return null;
        res[0] = (byte)((val >> 24) & 0xff);
        res[1] = (byte)(((val & 0xffffff) >> 16) & 0xff);
        res[2] = (byte)(((val & 0xffff) >> 8) & 0xff);
        res[3] = (byte)(val & 0xff);
        break;
      case 2:
        /* When a two part address is supplied, the last part is interpreted as a 24-bit quantity and placed
         * in the right most three bytes of the network address. This makes the two part address format
         * convenient for specifying Class A network addresses as net.host. */

        val = Integer.parseInt(s[0]);
        if (val < 0 || val > 0xff) return null;
        res[0] = (byte)(val & 0xff);
        val = Integer.parseInt(s[1]);
        if (val < 0 || val > 0xffffff) return null;
        res[1] = (byte)((val >> 16) & 0xff);
        res[2] = (byte)(((val & 0xffff) >> 8) & 0xff);
        res[3] = (byte)(val & 0xff);
        break;
      case 3:
        /* When a three part address is specified, the last part is interpreted as a 16-bit quantity and
         * placed in the right most two bytes of the network address. This makes the three part address format
         * convenient for specifying Class B net- work addresses as 128.net.host. */
        for (int i = 0; i < 2; i++) {
          val = Integer.parseInt(s[i]);
          if (val < 0 || val > 0xff) return null;
          res[i] = (byte)(val & 0xff);
        }
        val = Integer.parseInt(s[2]);
        if (val < 0 || val > 0xffff) return null;
        res[2] = (byte)((val >> 8) & 0xff);
        res[3] = (byte)(val & 0xff);
        break;
      case 4:
        /* When four parts are specified, each is interpreted as a byte of data and assigned, from left to
         * right, to the four bytes of an IPv4 address. */
        for (int i = 0; i < 4; i++) {
          val = Integer.parseInt(s[i]);
          if (val < 0 || val > 0xff) return null;
          res[i] = (byte)(val & 0xff);
        }
        break;
      default:
        return null;
      }
    } catch (NumberFormatException e) {
      return null;
    }
    return res;
  }

  /* Convert IPv4-Mapped address to IPv4 address. Both input and returned value are in network order binary
   * form.
   * @param src a String representing an IPv4-Mapped address in textual format
   * @return a byte array representing the IPv4 numeric address */
  public static byte[] convertFromIPv4MappedAddress(byte[] addr) {
    if (isIPv4MappedAddress(addr)) {
      byte[] newAddr = new byte[INADDR4SZ];
      System.arraycopy(addr, 12, newAddr, 0, INADDR4SZ);
      return newAddr;
    }
    return null;
  }

  /**
   * Utility routine to check if the InetAddress is an IPv4 mapped IPv6 address.
   * 
   * @return a <code>boolean</code> indicating if the InetAddress is an IPv4 mapped IPv6 address; or false if
   * address is IPv4 address.
   */
  private static boolean isIPv4MappedAddress(byte[] addr) {
    if (addr.length < INADDR16SZ) {
      return false;
    }
    if ((addr[0] == 0x00) && (addr[1] == 0x00) && (addr[2] == 0x00) && (addr[3] == 0x00) &&
      (addr[4] == 0x00) && (addr[5] == 0x00) && (addr[6] == 0x00) && (addr[7] == 0x00) && (addr[8] == 0x00) &&
      (addr[9] == 0x00) && (addr[10] == (byte)0xff) && (addr[11] == (byte)0xff)) {
      return true;
    }
    return false;
  }

  /* Returns true if specified string is a valid protocol name. */
  private boolean isValidProtocol(String protocol) {
    int len = protocol.length();
    if (len < 1) return false;
    char c = protocol.charAt(0);
    if (!Character.isLetter(c)) return false;
    for (int i = 1; i < len; i++) {
      c = protocol.charAt(i);
      if (!Character.isLetterOrDigit(c) && c != '.' && c != '+' && c != '-') {
        return false;
      }
    }
    return true;
  }

  /**
   * Sets the fields of the URL. This is not a public method so that only URLStreamHandlers can modify URL
   * fields. URLs are otherwise constant.
   * 
   * @param protocol the name of the protocol to use
   * @param host the name of the host
   * @param port the port number on the host
   * @param file the file on the host
   * @param ref the internal reference in the URL
   */
  protected void set(String protocol, String host, int port, String file, String ref) {
    synchronized (this) {
      this.protocol = protocol;
      this.host = host;
      authority = port == -1 ? host : host + ":" + port;
      this.port = port;
      this.file = file;
      this.ref = ref;
      /*
       * This is very important. We must recompute this after the URL has been changed. */
      hashCode = -1;
      int q = file.lastIndexOf('?');
      if (q != -1) {
        query = file.substring(q + 1);
        path = file.substring(0, q);
      } else
        path = file;
    }
  }

  /**
   * Sets the specified 8 fields of the URL. This is not a public method so that only URLStreamHandlers can
   * modify URL fields. URLs are otherwise constant.
   * 
   * @param protocol the name of the protocol to use
   * @param host the name of the host
   * @param port the port number on the host
   * @param authority the authority part for the url
   * @param userInfo the username and password
   * @param path the file on the host
   * @param ref the internal reference in the URL
   * @param query the query part of this URL
   * @since 1.3
   */
  protected void set(String protocol, String host, int port, String authority, String userInfo, String path,
    String query, String ref) {
    synchronized (this) {
      this.protocol = protocol;
      this.host = host;
      this.port = port;
      this.file = query == null ? path : path + "?" + query;
      this.userInfo = userInfo;
      this.path = path;
      this.ref = ref;
      /*
       * This is very important. We must recompute this after the URL has been changed. */
      hashCode = -1;
      this.query = query;
      this.authority = authority;
    }
  }

  /**
   * Gets the query part of this <code>URL</code>.
   * 
   * @return the query part of this <code>URL</code>, or <CODE>null</CODE> if one does not exist
   * @since 1.3
   */
  public String getQuery() {
    return query;
  }

  /**
   * Gets the path part of this <code>URL</code>.
   * 
   * @return the path part of this <code>URL</code>, or an empty string if one does not exist
   * @since 1.3
   */
  public String getPath() {
    return path;
  }

  /**
   * Gets the userInfo part of this <code>URL</code>.
   * 
   * @return the userInfo part of this <code>URL</code>, or <CODE>null</CODE> if one does not exist
   * @since 1.3
   */
  public String getUserInfo() {
    return userInfo;
  }

  /**
   * Gets the authority part of this <code>URL</code>.
   * 
   * @return the authority part of this <code>URL</code>
   * @since 1.3
   */
  public String getAuthority() {
    return authority;
  }

  /**
   * Gets the port number of this <code>URL</code>.
   * 
   * @return the port number, or -1 if the port is not set
   */
  public int getPort() {
    return port;
  }

  /**
   * Gets the default port number of the protocol associated with this <code>URL</code>. If the URL scheme or
   * the URLStreamHandler for the URL do not define a default port number, then -1 is returned.
   * 
   * @return the port number
   * @since 1.4
   */
  public int getDefaultPort() {
    return -1;
  }

  /**
   * Gets the protocol name of this <code>URL</code>.
   * 
   * @return the protocol of this <code>URL</code>.
   */
  public String getProtocol() {
    return protocol;
  }

  /**
   * Gets the host name of this <code>URL</code>, if applicable. The format of the host conforms to RFC 2732,
   * i.e. for a literal IPv6 address, this method will return the IPv6 address enclosed in square brackets (
   * <tt>'['</tt> and <tt>']'</tt>).
   * 
   * @return the host name of this <code>URL</code>.
   */
  public String getHost() {
    return host;
  }

  /**
   * Gets the file name of this <code>URL</code>. The returned file portion will be the same as
   * <CODE>getPath()</CODE>, plus the concatenation of the value of <CODE>getQuery()</CODE>, if any. If there
   * is no query portion, this method and <CODE>getPath()</CODE> will return identical results.
   * 
   * @return the file name of this <code>URL</code>, or an empty string if one does not exist
   */
  public String getFile() {
    return file;
  }

  /**
   * Gets the anchor (also known as the "reference") of this <code>URL</code>.
   * 
   * @return the anchor (also known as the "reference") of this <code>URL</code>, or <CODE>null</CODE> if one
   * does not exist
   */
  public String getRef() {
    return ref;
  }

  /**
   * Compares this URL for equality with another object.
   * <p>
   * If the given object is not a URL then this method immediately returns <code>false</code>.
   * <p>
   * Two URL objects are equal if they have the same protocol, reference equivalent hosts, have the same port
   * number on the host, and the same file and fragment of the file.
   * <p>
   * Two hosts are considered equivalent if both host names can be resolved into the same IP addresses; else
   * if either host name can't be resolved, the host names must be equal without regard to case; or both host
   * names equal to null.
   * <p>
   * Since hosts comparison requires name resolution, this operation is a blocking operation.
   * <p>
   * Note: The defined behavior for <code>equals</code> is known to be inconsistent with virtual hosting in
   * HTTP.
   * 
   * @param obj the URL to compare against.
   * @return <code>true</code> if the objects are the same; <code>false</code> otherwise.
   */
  public boolean equals(Object obj) {
    if (!(obj instanceof URL)) return false;
    URL u2 = (URL)obj;
    String ref1 = getRef();
    String ref2 = getRef();

    return (ref1 == ref2 || (ref1 != null && ref1.equals(ref2))) && sameFile(this, u2);
  }

  private boolean sameFile(URL u1, URL u2) {
    // Compare the protocols.
    if (!((u1.getProtocol() == u2.getProtocol()) || (u1.getProtocol() != null && u1.getProtocol()
      .equalsIgnoreCase(u2.getProtocol())))) return false;

    // Compare the files.
    if (!(u1.getFile() == u2.getFile() || (u1.getFile() != null && u1.getFile().equals(u2.getFile()))))
      return false;

    // Compare the ports.
    int port1, port2;
    port1 = u1.getPort();
    port2 = u2.getPort();
    if (port1 != port2) return false;

    // Compare the hosts.
    if (!((u1.getHost() == u2.getHost()) || (u1.getHost() != null && u1.getHost().equalsIgnoreCase(
      u2.getHost())))) return false;

    return true;
  }

  /**
   * Creates an integer suitable for hash table indexing.
   * <p>
   * The hash code is based upon all the URL components relevant for URL comparison. As such, this operation
   * is a blocking operation.
   * <p>
   * 
   * @return a hash code for this <code>URL</code>.
   */
  public synchronized int hashCode() {
    if (hashCode != -1) return hashCode;

    int h = 0;

    // Generate the protocol part.
    String protocol = getProtocol();
    if (protocol != null) h += protocol.hashCode();

    // Generate the host part.
    String host = getHost();
    if (host != null) h += host.toLowerCase().hashCode();

    // Generate the file part.
    String file = getFile();
    if (file != null) h += file.hashCode();

    // Generate the port part.
    h += getPort();

    // Generate the ref part.
    String ref = getRef();
    if (ref != null) h += ref.hashCode();

    hashCode = h;

    return hashCode;
  }

  
  public InputStream openStream() {
    throw new RuntimeException(new 
      NoSuchMethodException("You may not call URL.openStream() in compiled gwt code"));
  }
  
  /**
   * Constructs a string representation of this <code>URL</code>. The string is created by calling the
   * <code>toExternalForm</code> method of the stream protocol handler for this object.
   * 
   * @return a string representation of this object.
   * @see java.net.URL#URL(java.lang.String, java.lang.String, int, java.lang.String)
   * @see java.net.URLStreamHandler#toExternalForm(java.net.URL)
   */
  public String toString() {
    return toExternalForm();
  }

  /**
   * Constructs a string representation of this <code>URL</code>. The string is created by calling the
   * <code>toExternalForm</code> method of the stream protocol handler for this object.
   * 
   * @return a string representation of this object.
   * @see java.net.URL#URL(java.lang.String, java.lang.String, int, java.lang.String)
   * @see java.net.URLStreamHandler#toExternalForm(java.net.URL)
   */
  public String toExternalForm() {

    // pre-compute length of StringBuffer
    int len = getProtocol().length() + 1;
    if (getAuthority() != null && getAuthority().length() > 0) len += 2 + getAuthority().length();
    if (getPath() != null) {
      len += getPath().length();
    }
    if (getQuery() != null) {
      len += 1 + getQuery().length();
    }
    if (getRef() != null) len += 1 + getRef().length();

    StringBuffer result = new StringBuffer(len);
    result.append(getProtocol());
    result.append(":");
    if (getAuthority() != null && getAuthority().length() > 0) {
      result.append("//");
      result.append(getAuthority());
    }
    if (getPath() != null) {
      result.append(getPath());
    }
    if (getQuery() != null) {
      result.append('?');
      result.append(getQuery());
    }
    if (getRef() != null) {
      result.append("#");
      result.append(getRef());
    }
    return result.toString();
  }

  // TODO(james) support URI as well...
  /**
   * Returns a {@link java.net.URI} equivalent to this URL. This method functions in the same way as
   * <code>new URI (this.toString())</code>.
   * <p>
   * Note, any URL instance that complies with RFC 2396 can be converted to a URI. However, some URLs that are
   * not strictly in compliance can not be converted to a URI.
   * 
   * @exception URISyntaxException if this URL is not formatted strictly according to to RFC2396 and cannot be
   * converted to a URI.
   * @return a URI instance equivalent to this URL.
   * @since 1.5
   */
  // public URI toURI() throws URISyntaxException {
  // return new URI (toString());
  // }

  class Parts {
    String path, query, ref;

    Parts(String file) {
      int ind = file.indexOf('#');
      ref = ind < 0 ? null : file.substring(ind + 1);
      file = ind < 0 ? file : file.substring(0, ind);
      int q = file.lastIndexOf('?');
      if (q != -1) {
        query = file.substring(q + 1);
        path = file.substring(0, q);
      } else {
        path = file;
      }
    }

    String getPath() {
      return path;
    }

    String getQuery() {
      return query;
    }

    String getRef() {
      return ref;
    }
  }
}
