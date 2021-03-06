// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import java.util.Collection;
import java.util.Objects;
import java.util.regex.Pattern;

import org.openstreetmap.josm.data.osm.OsmPrimitive;

/**
 * Utility class for creating {@link Predicate}s.
 */
public final class Predicates {

    private Predicates() {
    }

    /**
     * Creates a predicate that returns true every time.
     * @param <T> The type of the predicate.
     * @return A predicate returning <code>true</code>
     * @since 10040
     */
    public static <T> Predicate<T> alwaysTrue() {
        return new Predicate<T>() {
            @Override
            public boolean evaluate(T object) {
                return true;
            }
        };
    }

    /**
     * Creates a predicate that returns false every time.
     * @param <T> The type of the predicate.
     * @return A predicate returning <code>false</code>
     * @since 10040
     */
    public static <T> Predicate<T> alwaysFalse() {
        return new Predicate<T>() {
            @Override
            public boolean evaluate(T object) {
                return false;
            }
        };
    }

    /**
     * Returns the negation of {@code predicate}.
     * @param <T> type of items
     * @param predicate the predicate to negate
     * @return the negation of {@code predicate}
     */
    public static <T> Predicate<T> not(final Predicate<T> predicate) {
        return new Predicate<T>() {
            @Override
            public boolean evaluate(T obj) {
                return !predicate.evaluate(obj);
            }
        };
    }

    /**
     * Returns a {@link Predicate} executing {@link Objects#equals}.
     * @param <T> type of items
     * @param ref the reference object
     * @return a {@link Predicate} executing {@link Objects#equals}
     */
    public static <T> Predicate<T> equalTo(final T ref) {
        return new Predicate<T>() {
            @Override
            public boolean evaluate(T obj) {
                return Objects.equals(obj, ref);
            }
        };
    }

    /**
     * Creates a new predicate that checks if elements are exactly of that class.
     * @param <T> The predicate type.
     * @param clazz The class the elements must have.
     * @return A predicate.
     */
    public static <T> Predicate<T> isOfClass(final Class<? extends T> clazz) {
        return new Predicate<T>() {
            @Override
            public boolean evaluate(T obj) {
                return obj != null && obj.getClass() == clazz;
            }
        };
    }

    /**
     * Returns a {@link Predicate} executing {@link Pattern#matcher(CharSequence)} and {@link java.util.regex.Matcher#matches}.
     * @param pattern the pattern
     * @return a {@link Predicate} executing {@link Pattern#matcher(CharSequence)} and {@link java.util.regex.Matcher#matches}
     */
    public static Predicate<String> stringMatchesPattern(final Pattern pattern) {
        return new Predicate<String>() {
            @Override
            public boolean evaluate(String string) {
                return pattern.matcher(string).matches();
            }
        };
    }

    /**
     * Returns a {@link Predicate} executing {@link Pattern#matcher(CharSequence)} and {@link java.util.regex.Matcher#find}.
     * @param pattern the pattern
     * @return a {@link Predicate} executing {@link Pattern#matcher(CharSequence)} and {@link java.util.regex.Matcher#find}
     */
    public static Predicate<String> stringContainsPattern(final Pattern pattern) {
        return new Predicate<String>() {
            @Override
            public boolean evaluate(String string) {
                return pattern.matcher(string).find();
            }
        };
    }

    /**
     * Returns a {@link Predicate} executing {@link String#contains(CharSequence)}.
     * @param pattern the pattern
     * @return a {@link Predicate} executing {@link String#contains(CharSequence)}
     */
    public static Predicate<String> stringContains(final String pattern) {
        return new Predicate<String>() {
            @Override
            public boolean evaluate(String string) {
                return string.contains(pattern);
            }
        };
    }

    /**
     * Returns a {@link Predicate} executing {@link OsmPrimitive#hasTag(String, String...)}.
     * @param key the key forming the tag
     * @param values one or many values forming the tag
     * @return a {@link Predicate} executing {@link OsmPrimitive#hasTag(String, String...)}
     */
    public static Predicate<OsmPrimitive> hasTag(final String key, final String... values) {
        return new Predicate<OsmPrimitive>() {
            @Override
            public boolean evaluate(OsmPrimitive p) {
                return p.hasTag(key, values);
            }
        };
    }

    /**
     * Returns a {@link Predicate} executing {@link OsmPrimitive#hasKey(String)}.
     * @param key the key
     * @return a {@link Predicate} executing {@link OsmPrimitive#hasKey(String)}
     */
    public static Predicate<OsmPrimitive> hasKey(final String key) {
        return new Predicate<OsmPrimitive>() {
            @Override
            public boolean evaluate(OsmPrimitive p) {
                return p.hasKey(key);
            }
        };
    }

    /**
     * Returns a {@link Predicate} executing {@link Collection#contains(Object)}.
     * @param <T> type of items
     * @param target collection
     * @return a {@link Predicate} executing {@link Collection#contains(Object)}
     */
    public static <T> Predicate<T> inCollection(final Collection<? extends T> target) {
        return new Predicate<T>() {
            @Override
            public boolean evaluate(T object) {
                return target.contains(object);
            }
        };
    }

    /**
     * Returns a {@link Predicate} testing whether objects are {@code null}.
     * @param <T> type of items
     * @return a {@link Predicate} testing whether objects are {@code null}
     */
    public static <T> Predicate<T> isNull() {
        return new Predicate<T>() {
            @Override
            public boolean evaluate(T object) {
                return object == null;
            }
        };
    }
}
