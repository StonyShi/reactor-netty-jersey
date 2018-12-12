package reactor.ipc.netty.http.server;

import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;

import java.net.URI;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>reactor-netty-ext
 * <p>reactor.ipc.netty.http.server
 *
 * @author stony
 * @version 下午4:19
 * @since 2018/3/22
 */
public class SimpleHttpPredicate implements Predicate<HttpServerRequest>, Function<Object, Map<String, String>> {

    /**
     * An alias for {@link HttpPredicate#http}.
     * <p>
     * Creates a {@link Predicate} based on a URI template filtering .
     * <p>
     * This will listen for DELETE Method.
     *
     * @param uri The string to compile into a URI template and use for matching
     *
     * @return The new {@link Predicate}.
     *
     * @see Predicate
     */
    public static Predicate<HttpServerRequest> delete(String uri) {
        return http(uri, null, HttpMethod.DELETE);
    }

    /**
     * An alias for {@link HttpPredicate#http}.
     * <p>
     * Creates a {@link Predicate} based on a URI template filtering .
     * <p>
     * This will listen for GET Method.
     *
     * @param uri The string to compile into a URI template and use for matching
     *
     * @return The new {@link Predicate}.
     *
     * @see Predicate
     */
    public static Predicate<HttpServerRequest> get(String uri) {
        return http(uri, null, HttpMethod.GET);
    }

    /**
     * Creates a {@link Predicate} based on a URI template.
     * This will listen for all Methods.
     *
     * @param uri The string to compile into a URI template and use for matching
     *
     * @return The new {@link HttpPredicate}.
     *
     * @see Predicate
     */
    public static Predicate<HttpServerRequest> http(String uri,
                                                    HttpVersion protocol,
                                                    HttpMethod method) {
        if (null == uri) {
            return null;
        }

        return new SimpleHttpPredicate(uri, protocol, method);
    }

    /**
     * An alias for {@link HttpPredicate#http}.
     * <p>
     * Creates a {@link Predicate} based on a URI template filtering .
     * <p>
     * This will listen for POST Method.
     *
     * @param uri The string to compile into a URI template and use for matching
     *
     * @return The new {@link Predicate}.
     *
     * @see Predicate
     */
    public static Predicate<HttpServerRequest> post(String uri) {
        return http(uri, null, HttpMethod.POST);
    }

    /**
     * An alias for {@link HttpPredicate#get} prefix ([prefix]/**), useful for file system
     * mapping.
     * <p>
     * Creates a {@link Predicate} based on a URI template filtering .
     * <p>
     * This will listen for WebSocket Method.
     *
     * @return The new {@link Predicate}.
     *
     * @see Predicate
     */
    public static Predicate<HttpServerRequest> prefix(String prefix) {
        return prefix(prefix, HttpMethod.GET);
    }

    /**
     * An alias for {@link HttpPredicate#get} prefix (/**), useful for file system mapping.
     * <p>
     * Creates a {@link Predicate} based on a URI template filtering .
     * <p>
     * This will listen for WebSocket Method.
     *
     * @return The new {@link Predicate}.
     *
     *
     * @see Predicate
     */
    public static Predicate<HttpServerRequest> prefix(String prefix, HttpMethod method) {
        Objects.requireNonNull(prefix, "Prefix must be provided");

        String target = prefix.startsWith("/") ? prefix : "/".concat(prefix);
        //target = target.endsWith("/") ? target :  prefix.concat("/");
        return new HttpPrefixPredicate(target, method);
    }

    /**
     * An alias for {@link HttpPredicate#http}.
     * <p>
     * Creates a {@link Predicate} based on a URI template filtering .
     * <p>
     * This will listen for PUT Method.
     *
     * @param uri The string to compile into a URI template and use for matching
     *
     * @return The new {@link Predicate}.
     *
     * @see Predicate
     */
    public static Predicate<HttpServerRequest> put(String uri) {
        return http(uri, null, HttpMethod.PUT);
    }

    final HttpVersion     protocol;
    final HttpMethod      method;
    final String          uri;
    final UriPathTemplate template;

    @SuppressWarnings("unused")
    public SimpleHttpPredicate(String uri) {
        this(uri, null, null);
    }

    public SimpleHttpPredicate(String uri, HttpVersion protocol, HttpMethod method) {
        this.protocol = protocol;
        this.uri = uri;
        this.method = method;
        this.template = uri != null ? new UriPathTemplate(uri) : null;
    }

    @Override
    public Map<String, String> apply(Object key) {
        if(method  == HttpMethod.GET) {
            try {
                String query = URI.create(key.toString()).getQuery();
                if(query != null) {
                    return parseParameters(query);
                }
            } catch (Exception e) {
                System.out.println("warn key [ " + key + " ] parse parameters error : " + e.getMessage());
            }
        }
        if (template == null) {
            return null;
        }
        Map<String, String> headers = template.match(key.toString());
        if (null != headers && !headers.isEmpty()) {
            return headers;
        }
        return null;
    }
    public static Map<String,String> parseParameters(String parameters){
        if (parameters == null || parameters.length() == 0) return null;
        Map<String, String> map = new HashMap<>(8);
        String[] vs = parameters.split("&");
        for(String v : vs){
            String[] ps = v.split("=", 2);
            if(ps.length == 1){
                map.put(ps[0], null);
            }
            if(ps.length == 2){
                map.put(ps[0], ps[1]);
            }
        }
        return map;
    }

    @Override
    public final boolean test(HttpServerRequest key) {
        String uri = key.uri();
        if(method == HttpMethod.GET) {
            try {
                uri = URI.create(uri).getPath();
            }catch (Exception e) {
                System.out.println("warn uri [ " + uri + " ] get path error : " + e.getMessage());
            }
        }
        return (protocol == null || protocol.equals(key.version())) && (method == null || method.equals(
                key.method())) && (template == null || template.matches(uri));
    }

    /**
     * Represents a URI template. A URI template is a URI-like String that contains
     * variables enclosed by braces (<code>{</code>, <code>}</code>), which can be
     * expanded to produce an actual URI.
     *
     * @author Arjen Poutsma
     * @author Juergen Hoeller
     * @author Jon Brisbin
     * @see <a href="https://tools.ietf.org/html/rfc6570">RFC 6570: URI Templates</a>
     */
    static final class UriPathTemplate {

        private static final Pattern FULL_SPLAT_PATTERN     =
                Pattern.compile("[\\*][\\*]");
        private static final String  FULL_SPLAT_REPLACEMENT = ".*";

        private static final Pattern NAME_SPLAT_PATTERN     =
                Pattern.compile("\\{([^/]+?)\\}[\\*][\\*]");
        // JDK 6 doesn't support named capture groups
        private static final String  NAME_SPLAT_REPLACEMENT = "(?<%NAME%>.*)";
        //private static final String  NAME_SPLAT_REPLACEMENT = "(.*)";

        private static final Pattern NAME_PATTERN     = Pattern.compile("\\{([^/]+?)\\}");
        // JDK 6 doesn't support named capture groups
        private static final String  NAME_REPLACEMENT = "(?<%NAME%>[^\\/]*)";
        //private static final String  NAME_REPLACEMENT = "([^\\/]*)";

        private final List<String> pathVariables =
                new ArrayList<>();
        private final HashMap<String, Matcher> matchers      =
                new HashMap<>();
        private final HashMap<String, Map<String, String>> vars          =
                new HashMap<>();

        private final Pattern uriPattern;

        /**
         * Creates a new {@code UriPathTemplate} from the given {@code uriPattern}.
         *
         * @param uriPattern The pattern to be used by the template
         */
        public UriPathTemplate(String uriPattern) {
            String s = "^" + uriPattern;

            Matcher m = NAME_SPLAT_PATTERN.matcher(s);
            while (m.find()) {
                for (int i = 1; i <= m.groupCount(); i++) {
                    String name = m.group(i);
                    pathVariables.add(name);
                    s = m.replaceFirst(NAME_SPLAT_REPLACEMENT.replaceAll("%NAME%", name));
                    m.reset(s);
                }
            }

            m = NAME_PATTERN.matcher(s);
            while (m.find()) {
                for (int i = 1; i <= m.groupCount(); i++) {
                    String name = m.group(i);
                    pathVariables.add(name);
                    s = m.replaceFirst(NAME_REPLACEMENT.replaceAll("%NAME%", name));
                    m.reset(s);
                }
            }

            m = FULL_SPLAT_PATTERN.matcher(s);
            while (m.find()) {
                s = m.replaceAll(FULL_SPLAT_REPLACEMENT);
                m.reset(s);
            }

            this.uriPattern = Pattern.compile(s + "$");
        }

        /**
         * Tests the given {@code uri} against this template, returning {@code true} if
         * the uri matches the template, {@code false} otherwise.
         *
         * @param uri The uri to match
         *
         * @return {@code true} if there's a match, {@code false} otherwise
         */
        public boolean matches(String uri) {
            return matcher(uri).matches();
        }

        /**
         * Matches the template against the given {@code uri} returning a map of path
         * parameters extracted from the uri, keyed by the names in the template. If the
         * uri does not match, or there are no path parameters, an empty map is returned.
         *
         * @param uri The uri to match
         *
         * @return the path parameters from the uri. Never {@code null}.
         */
        final Map<String, String> match(String uri) {
            Map<String, String> pathParameters = vars.get(uri);
            if (null != pathParameters) {
                return pathParameters;
            }

            pathParameters = new HashMap<>();
            Matcher m = matcher(uri);
            if (m.matches()) {
                int i = 1;
                for (String name : pathVariables) {
                    String val = m.group(i++);
                    pathParameters.put(name, val);
                }
            }
            synchronized (vars) {
                vars.put(uri, pathParameters);
            }

            return pathParameters;
        }

        private Matcher matcher(String uri) {
            Matcher m = matchers.get(uri);
            if (null == m) {
                m = uriPattern.matcher(uri);
                synchronized (matchers) {
                    matchers.put(uri, m);
                }
            }
            return m;
        }

    }

    static final class HttpPrefixPredicate implements Predicate<HttpServerRequest> {

        final HttpMethod method;
        final String     prefix;

        public HttpPrefixPredicate(String prefix, HttpMethod method) {
            this.prefix = prefix;
            this.method = method;
        }

        @Override
        public boolean test(HttpServerRequest key) {
            return (method == null || method.equals(key.method())) && key.uri()
                    .startsWith(prefix);
        }
    }
}
