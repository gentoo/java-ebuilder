package org.gentoo.java.ebuilder.portage;

import java.util.Comparator;

/**
 * a comparator to sort keywords
 *
 * @author Zhang Zongyu
 */
public class KeywordComparator implements Comparator<String> {

    /**
     * it is designed to compare KEYWORDS as what repoman will do:
     * 1) "-amd64", "amd64" and "~amd64" are the same -- they will
     *    not appear at the same time in a TreeSet;
     * 2) After splitting the strings into two parts by "-", it will
     *    compare the suffixes before it compares the prefixes.
     */
    @Override
    public int compare(String o1, String o2) {
        // prepend "-0-" to make sure the length of the array is 2
        final String[] trimmedO1 = (o1 + "-0-").
                replaceAll("^[-~]", "").
                split("-", 2);
        final String[] trimmedO2 = (o2 + "-0-").
                replaceAll("^[-~]", "").
                split("-", 2);

        if (trimmedO1[1].compareTo(trimmedO2[1]) == 0) {
            return trimmedO1[0].compareTo(trimmedO2[0]);
        } else {
            return trimmedO1[1].compareTo(trimmedO2[1]);
        }
    }
}
