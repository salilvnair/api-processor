package com.github.salilvnair.api.processor.helper.reflection;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.apache.commons.lang3.reflect.MethodUtils;

import java.lang.reflect.*;
import java.util.*;


@SuppressWarnings("unchecked")
public class ReflectionUtil {

    private static final String SETTER_PREFIX = "set";
    private static final String GETTER_PREFIX = "get";
    private static final String IS_PREFIX = "is";

    public static Method findSetterMethod(Class<?> clazz, String propertyName, Class<?> parameterType) {
        String setterMethodName = SETTER_PREFIX + StringUtils.capitalize(propertyName);
        return findMethod(clazz, setterMethodName, parameterType);
    }

    public static Method findGetterMethod(Class<?> clazz, String propertyName) {
        String getterMethodName = GETTER_PREFIX + StringUtils.capitalize(propertyName);

        Method method = findMethod(clazz, getterMethodName);

        // retry on another name
        if (method == null) {
            getterMethodName = IS_PREFIX + StringUtils.capitalize(propertyName);
            method = findMethod(clazz, getterMethodName);
        }
        return method;
    }

    public static Method findMethod(final Class<?> clazz, final String methodName, Class<?>... parameterTypes) {
        Method method = MethodUtils.getMatchingMethod(clazz, methodName, parameterTypes);
        if (method != null) {
            makeAccessible(method);
        }
        return method;
    }

    public static Method findAccessibleMethodByName(final Class<?> clazz, final String methodName) {
        Validate.notNull(clazz, "clazz can't be null");
        Validate.notEmpty(methodName, "methodName can't be blank");

        for (Class<?> searchType = clazz; searchType != Object.class; searchType = searchType.getSuperclass()) {
            Method[] methods = searchType.getDeclaredMethods();
            for (Method method : methods) {
                if (method.getName().equals(methodName)) {
                    makeAccessible(method);
                    return method;
                }
            }
        }
        return null;
    }

    public static Field findField(final Class<?> clazz, final String fieldName) {
        Validate.notNull(clazz, "clazz can't be null");
        Validate.notEmpty(fieldName, "fieldName can't be blank");
        for (Class<?> superClass = clazz; superClass != Object.class; superClass = superClass.getSuperclass()) {
            try {
                Field field = superClass.getDeclaredField(fieldName);
                makeAccessible(field);
                return field;
            }
            catch (NoSuchFieldException ignored) {}
        }
        return null;
    }

    public static <T> T invokeGetter(Object obj, String propertyName) {
        Method method = findGetterMethod(obj.getClass(), propertyName);
        if (method == null) {
            throw new IllegalArgumentException(
                    "Could not find getter method [" + propertyName + "] on target [" + obj + ']');
        }
        return invokeMethod(obj, method);
    }

    public static void invokeSetter(Object obj, String propertyName, Object value) {
        Method method = findSetterMethod(obj.getClass(), propertyName, value.getClass());
        if (method == null) {
            throw new IllegalArgumentException(
                    "Could not find getter method [" + propertyName + "] on target [" + obj + ']');
        }
        invokeMethod(obj, method, value);
    }

    public static <T> T findFieldValue(final Object obj, final String fieldName) {
        Field field = findField(obj.getClass(), fieldName);
        if (field == null) {
            throw new IllegalArgumentException("Could not find field [" + fieldName + "] on target [" + obj + ']');
        }
        return findFieldValue(obj, field);
    }

    public static <T> T findFieldValue(final Object obj, final Field field) {
        try {
            field.setAccessible(true);
            return (T) field.get(obj);
        }
        catch (Exception ignored) {}
        return null;
    }

    public static void setFieldValue(final Object obj, final String fieldName, final Object value) {
        Field field = findField(obj.getClass(), fieldName);
        if (field == null) {
            throw new IllegalArgumentException("Could not find field [" + fieldName + "] on target [" + obj + ']');
        }
        setField(obj, field, value);
    }

    public static void setField(final Object obj, Field field, final Object value) {
        try {
            makeAccessible(field);
            field.set(obj, value);
        }
        catch (Exception ignored) {}
    }

    public static <T> T findProperty(Object obj, String propertyName) {
        Method method = findGetterMethod(obj.getClass(), propertyName);
        if (method != null) {
            return invokeMethod(obj, method);
        } else {
            return findFieldValue(obj, propertyName);
        }
    }

    public static void setProperty(Object obj, String propertyName, final Object value) {
        Method method = findSetterMethod(obj.getClass(), propertyName, value.getClass());
        if (method != null) {
            invokeMethod(obj, method, value);
        } else {
            setFieldValue(obj, propertyName, value);
        }
    }

    public static <T> T invokeMethod(Object obj, String methodName, Object... args) {
        Object[] theArgs = ArrayUtils.nullToEmpty(args);
        final Class<?>[] parameterTypes = ClassUtils.toClass(theArgs);
        return invokeMethod(obj, methodName, theArgs, parameterTypes);
    }

    public static <T> T invokeMethod(final Object obj, final String methodName, final Object[] args,
                                     final Class<?>[] parameterTypes) {
        Method method = findMethod(obj.getClass(), methodName, parameterTypes);
        if (method == null) {
            throw new IllegalArgumentException("Could not find method [" + methodName + "] with parameter types:"+ Arrays.toString(parameterTypes));
        }
        return invokeMethod(obj, method, args);
    }

    public static <T> T invokeMethodByName(final Object obj, final String methodName, final Object[] args) {
        Method method = findAccessibleMethodByName(obj.getClass(), methodName);
        if (method == null) {
            throw new IllegalArgumentException(
                    "Could not find method [" + methodName + "] on class [" + obj.getClass() + ']');
        }
        return invokeMethod(obj, method, args);
    }

    public static <T> T invokeMethod(final Object obj, Method method, Object... args) {
        try {
            return (T) method.invoke(obj, args);
        }
        catch (Exception ignored) {}
        return null;
    }

    public static <T> T invokeMethodAndThrowException(final Object obj, Method method, Object... args) throws Exception {
        try {
            return (T) method.invoke(obj, args);
        }
        catch (Exception ex) {
            throw ex;
        }
    }

    public static <T> T invokeConstructor(final Class<T> cls, Object... args) {
        try {
            return ConstructorUtils.invokeConstructor(cls, args);
        }
        catch (Exception ignored) {}
        return null;
    }

    public static void makeAccessible(Method method) {
        if (!method.isAccessible() && (!Modifier.isPublic(method.getModifiers())
                || !Modifier.isPublic(method.getDeclaringClass().getModifiers()))) {
            method.setAccessible(true);
        }
    }

    public static void makeAccessible(Field field) {
        if (!field.isAccessible() && (!Modifier.isPublic(field.getModifiers())
                || !Modifier.isPublic(field.getDeclaringClass().getModifiers())
                || Modifier.isFinal(field.getModifiers()))) {
            field.setAccessible(true);
        }
    }

    public static Set<Field> findFields(Class<?> clazz) {
        return new LinkedHashSet<>(Arrays.asList(clazz.getDeclaredFields()));
    }

    public static Set<Field> findFields(Class<?> clazz, boolean searchInAllSuperClass) {
        Set<Field> fields = new HashSet<>();
        if(!searchInAllSuperClass) {
            return findFields(clazz);
        }
        while (clazz != Object.class) {
            fields.addAll(Arrays.asList(clazz.getDeclaredFields()));
            clazz = clazz.getSuperclass();
        }
        return fields;
    }


    public static boolean hasField(final Object obj, final String fieldName) {
        return findField(obj.getClass(), fieldName) != null;
    }

    public static boolean hasField(final Object obj, final Field field) {
        return findField(obj.getClass(), field.getName()) != null;
    }

    public static Class<?> findParameterizedType(Object object, String fieldName) {
        Field field = ReflectionUtil.findField(object.getClass(), fieldName);
        return field == null ? null : findParameterizedType(object, field);
    }

    public static Class<?> findParameterizedType(Object object, Field field) {
        ParameterizedType stringListType = (ParameterizedType) field.getGenericType();
        return (Class<?>) stringListType.getActualTypeArguments()[0];
    }


    public static Class<?> findType(Object object, String fieldName) {
        Field field = ReflectionUtil.findField(object.getClass(), fieldName);
        return field == null ? null : findType(object, field);
    }

    public static Class<?> findType(Object object, Field field) {
        return field.getType();
    }

    public static <T> T createInstance(Class<T> clazz) throws Exception {
        Constructor<T> constructor = clazz.getDeclaredConstructor();
        constructor.setAccessible(true);
        return constructor.newInstance();
    }

}