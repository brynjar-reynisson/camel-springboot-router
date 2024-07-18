package com.breynisson.router;

import org.apache.camel.Converter;
import org.apache.camel.TypeConverters;
import org.apache.camel.component.file.GenericFile;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;

@Component
public class RouterTypeConverters implements TypeConverters {

    @Converter
    public <T> java.util.Comparator<GenericFile<T>> stringToComparator(String value) {
        try {
            return (java.util.Comparator<GenericFile<T>>)Class.forName(value).getDeclaredConstructor().newInstance();
        } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RouterException("Can't create new Comparator instance from value " + value);
        }
    }
}
