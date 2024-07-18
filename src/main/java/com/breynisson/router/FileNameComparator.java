package com.breynisson.router;

import org.apache.camel.component.file.GenericFile;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.regex.Pattern;

@Component
public class FileNameComparator<T> implements Comparator<GenericFile<T>> {

    private static final Pattern fileNamePattern = Pattern.compile(".*[_-]\\d{1,4}\\.\\w{1,4}");

    @Override
    public int compare(GenericFile file1, GenericFile file2) {
        return compare(file1.getFileName(), file2.getFileNameOnly());
    }

    public int compare(String name1, String name2) {
        if(matches(name1) && matches(name2)) {
            CompositeFileName fn1 = extractCompositeFileName(name1);
            CompositeFileName fn2 = extractCompositeFileName(name2);
            if(fn1.firstPart.equals(fn2.firstPart)) {
                return Integer.compare(fn1.number, fn2.number);
            }
        }
        return name1.compareTo(name2);
    }

    private static CompositeFileName extractCompositeFileName(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        int lastHyphenIndex = fileName.lastIndexOf('-');
        int lastUnderscoreIndex = fileName.lastIndexOf('_');
        if(lastHyphenIndex==-1 && lastUnderscoreIndex==-1) {
            throw new RouterException("File name does not contain hyphen or underscore before number: " + fileName);
        }
        int startIndex = Math.max(lastHyphenIndex, lastUnderscoreIndex);
        String number = fileName.substring(startIndex+1, lastDotIndex);
        String firstPart = fileName.substring(0, startIndex);
        return new CompositeFileName(firstPart, Integer.parseInt(number));
    }

    static boolean matches(String fileName) {
        return fileNamePattern.matcher(fileName).matches();
    }

    private static class CompositeFileName {
        private final String firstPart;
        private final int number;
        private CompositeFileName(String firstPart, int number) {
            this.firstPart = firstPart;
            this.number = number;
        }
    }
}
