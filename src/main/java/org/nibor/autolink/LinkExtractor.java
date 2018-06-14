package org.nibor.autolink;

import org.nibor.autolink.internal.Scanner;

import java.util.*;

/**
 * Extracts links from input.
 * <p>
 * Create and configure an extractor using {@link #builder()}, then call {@link #extractLinks}.
 */
public class LinkExtractor {

    private final Map<Character, Scanner> triggerToScanners;

    private LinkExtractor(Map<Character, Scanner> triggerToScanners) {
        this.triggerToScanners = triggerToScanners;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Extract the links from the input text. Can be called multiple times with different inputs (thread-safe).
     *
     * @param input the input text, must not be {@code null}
     * @return a lazy iterable for the links in order that they appear in the input, never {@code null}
     */
    public Iterable<LinkSpan> extractLinks(final CharSequence input) {
        return new Iterable<LinkSpan>() {
            @Override
            public Iterator<LinkSpan> iterator() {
                return new LinkIterator(input);
            }
        };
    }

    /**
     * Builder for configuring link extractor.
     */
    public static class Builder {

        private Map<Character, Scanner> triggerToScanners = new HashMap<>();

        private Builder() {
        }

        public Builder withScanner(char trigger, Scanner scanner) {
            if (scanner == null) {
                throw new NullPointerException("scanner should not be null");
            }
            if (triggerToScanners.containsKey(trigger)) {
                throw new IllegalStateException("the trigger '" + trigger + "' has already been used for another scanner");
            }
            triggerToScanners.put(trigger, scanner);

            return this;
        }

        /**
         * @return the configured link extractor
         */
        public LinkExtractor build() {
            if (triggerToScanners.isEmpty()) {
                throw new IllegalStateException("no scanners have been defined");
            }
            return new LinkExtractor(triggerToScanners);
        }
    }

    private class LinkIterator implements Iterator<LinkSpan> {

        private final CharSequence input;

        private LinkSpan next = null;
        private int index = 0;
        private int rewindIndex = 0;

        public LinkIterator(CharSequence input) {
            this.input = input;
        }

        @Override
        public boolean hasNext() {
            setNext();
            return next != null;
        }

        @Override
        public LinkSpan next() {
            if (hasNext()) {
                LinkSpan link = next;
                next = null;
                return link;
            } else {
                throw new NoSuchElementException();
            }
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("remove");
        }

        private void setNext() {
            if (next != null) {
                return;
            }

            int length = input.length();
            while (index < length) {
                Scanner scanner = triggerToScanners.get(input.charAt(index));
                if (scanner != null) {
                    LinkSpan link = scanner.scan(input, index, rewindIndex);
                    if (link != null) {
                        next = link;
                        index = link.getEndIndex();
                        rewindIndex = index;
                        break;
                    } else {
                        index++;
                    }
                } else {
                    index++;
                }
            }
        }
    }
}
