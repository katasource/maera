package org.maera.plugin.util;

import java.lang.reflect.*;
import java.util.*;

/**
 * Class utility methods
 */
public class ClassUtils {
    private ClassUtils() {
    }

    /**
     * Finds all super classes and interfaces for a given class
     *
     * @param cls The class to scan
     * @return The collected related classes found
     */
    public static Set<Class> findAllTypes(Class cls) {
        Set<Class> types = new HashSet<Class>();
        findAllTypes(cls, types);
        return types;
    }

    /**
     * Finds all super classes and interfaces for a given class
     *
     * @param cls   The class to scan
     * @param types The collected related classes found
     */
    public static void findAllTypes(Class cls, Set<Class> types) {
        if (cls == null) {
            return;
        }

        // check to ensure it hasn't been scanned yet
        if (types.contains(cls)) {
            return;
        }

        types.add(cls);

        findAllTypes(cls.getSuperclass(), types);
        for (int x = 0; x < cls.getInterfaces().length; x++) {
            findAllTypes(cls.getInterfaces()[x], types);
        }
    }

    /**
     * Get the underlying class for a type, or null if the type is a variable type.
     *
     * @param type the type
     * @return the underlying class
     */
    private static Class<?> getClass(Type type) {
        if (type instanceof Class) {
            return (Class) type;
        } else if (type instanceof ParameterizedType) {
            return getClass(((ParameterizedType) type).getRawType());
        } else if (type instanceof GenericArrayType) {
            Type componentType = ((GenericArrayType) type).getGenericComponentType();
            Class<?> componentClass = getClass(componentType);
            if (componentClass != null) {
                return Array.newInstance(componentClass, 0).getClass();
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * Get the actual type arguments a child class has used to extend a generic base class.
     *
     * @param baseClass  the base class
     * @param childClass the child class
     * @return a list of the raw classes for the actual type arguments.
     * @throws IllegalArgumentException If the child class is not the base of the baseClass
     * @since 2.5.0
     */
    public static <T> List<Class<?>> getTypeArguments(
            Class<T> baseClass, Class<? extends T> childClass) {
        Map<Type, Type> resolvedTypes = new HashMap<Type, Type>();
        Type type = childClass;
        Class typeClass = getClass(type);
        // start walking up the inheritance hierarchy until we hit baseClass
        while (!typeClass.equals(baseClass)) {
            if (type instanceof Class) {
                // there is no useful information for us in raw types, so just keep going.
                type = ((Class) type).getGenericSuperclass();
            } else {
                ParameterizedType parameterizedType = (ParameterizedType) type;
                Class<?> rawType = (Class) parameterizedType.getRawType();

                Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
                TypeVariable<?>[] typeParameters = rawType.getTypeParameters();
                for (int i = 0; i < actualTypeArguments.length; i++) {
                    resolvedTypes.put(typeParameters[i], actualTypeArguments[i]);
                }

                if (!rawType.equals(baseClass)) {
                    type = rawType.getGenericSuperclass();
                }
            }
            typeClass = getClass(type);
            if (typeClass == null) {
                throw new IllegalArgumentException("Unable to find the class for the type " + type);
            }
        }

        // finally, for each actual type argument provided to baseClass, determine (if possible)
        // the raw class for that type argument.
        Type[] actualTypeArguments;
        if (type instanceof Class) {
            actualTypeArguments = ((Class) type).getTypeParameters();
        } else {
            actualTypeArguments = ((ParameterizedType) type).getActualTypeArguments();
        }
        List<Class<?>> typeArgumentsAsClasses = new ArrayList<Class<?>>();
        // resolve types by chasing down type variables.
        for (Type baseType : actualTypeArguments) {
            while (resolvedTypes.containsKey(baseType)) {
                baseType = resolvedTypes.get(baseType);
            }
            typeArgumentsAsClasses.add(getClass(baseType));
        }
        return typeArgumentsAsClasses;
    }
}
