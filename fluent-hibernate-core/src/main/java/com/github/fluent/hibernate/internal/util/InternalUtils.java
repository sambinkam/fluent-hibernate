package com.github.fluent.hibernate.internal.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.StatelessSession;
import org.hibernate.Transaction;

/**
 * Utils for this library. For internal use only.
 *
 * @author V.Ladynev
 */
public final class InternalUtils {

    private InternalUtils() {

    }

    public static int hashCode(Object... objects) {
        return Arrays.hashCode(objects);
    }

    public static boolean equal(Object a, Object b) {
        return a == b || a != null && a.equals(b);
    }

    public static Object newInstance(Class<?> classToInstantiate) {
        try {
            return classToInstantiate.newInstance();
        } catch (Exception ex) {
            throw new RuntimeException(String.format("Could not instantiate result class: %s",
                    classToInstantiate.getName()), ex);
        }
    }

    public static final class CollectionUtils {

        private CollectionUtils() {

        }

        public static <T> T first(List<T> items) {
            return isEmpty(items) ? null : items.get(0);
        }

        public static boolean isEmpty(final Collection<?> collection) {
            return collection == null || collection.isEmpty();
        }

        public static boolean isNotEmpty(final Collection<?> collection) {
            return !isEmpty(collection);
        }

        public static <E> ArrayList<E> newArrayList() {
            return new ArrayList<E>();
        }

        public static <K, V> HashMap<K, V> newHashMap() {
            return new HashMap<K, V>();
        }

    }

    public static final class HibernateUtils {

        private HibernateUtils() {

        }

        public static void rollback(Transaction txn) {
            if (txn != null) {
                txn.rollback();
            }
        }

        public static void close(Session session) {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }

        public static void close(StatelessSession session) {
            if (session != null) {
                session.close();
            }
        }

    }

}
