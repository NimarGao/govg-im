package com.im.util;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SensitiveWordFilter {

    public static final SensitiveWordFilter INSTANCE = new SensitiveWordFilter();

    private final Set<String> sensitiveWords = ConcurrentHashMap.newKeySet();
    private volatile TrieNode root = new TrieNode();
    private volatile String policy = "BLOCK"; // Default policy: BLOCK

    private SensitiveWordFilter() {
        // Initialize default sensitive words
        sensitiveWords.addAll(Arrays.asList(
            "fuck", "shit", "crack", "hack", "bomb", "trump", "buy bitcoin", "挂科", "垃圾"
        ));
        rebuild();
    }

    public static class FilterResult {
        private final String filteredText;
        private final boolean hasSensitiveWord;
        private final List<String> matchedWords;

        public FilterResult(String filteredText, boolean hasSensitiveWord, List<String> matchedWords) {
            this.filteredText = filteredText;
            this.hasSensitiveWord = hasSensitiveWord;
            this.matchedWords = matchedWords;
        }

        public String getFilteredText() {
            return filteredText;
        }

        public boolean isHasSensitiveWord() {
            return hasSensitiveWord;
        }

        public List<String> getMatchedWords() {
            return matchedWords;
        }
    }

    private static class TrieNode {
        Map<Character, TrieNode> next = new HashMap<>();
        TrieNode fail = null;
        String word = null; // Stored sensitive word ending at this node
        boolean isEnd = false;
    }

    public synchronized void addWord(String word) {
        if (word == null || word.trim().isEmpty()) {
            return;
        }
        sensitiveWords.add(word.trim().toLowerCase());
        rebuild();
    }

    public synchronized void removeWord(String word) {
        if (word == null) {
            return;
        }
        sensitiveWords.remove(word.trim().toLowerCase());
        rebuild();
    }

    public synchronized void setSensitiveWords(List<String> words) {
        sensitiveWords.clear();
        if (words != null) {
            for (String w : words) {
                if (w != null && !w.trim().isEmpty()) {
                    sensitiveWords.add(w.trim().toLowerCase());
                }
            }
        }
        rebuild();
    }

    public List<String> getSensitiveWords() {
        return new ArrayList<>(sensitiveWords);
    }

    public String getPolicy() {
        return policy;
    }

    public void setPolicy(String policy) {
        if ("MASK".equalsIgnoreCase(policy) || "BLOCK".equalsIgnoreCase(policy)) {
            this.policy = policy.toUpperCase();
        }
    }

    /**
     * Rebuild the AC Automaton Trie and Fail pointers in a thread-safe manner.
     */
    private synchronized void rebuild() {
        TrieNode newRoot = new TrieNode();

        // 1. Build Standard Trie
        for (String word : sensitiveWords) {
            TrieNode current = newRoot;
            for (char c : word.toCharArray()) {
                current = current.next.computeIfAbsent(c, k -> new TrieNode());
            }
            current.isEnd = true;
            current.word = word;
        }

        // 2. Build Fail Pointers via BFS
        Queue<TrieNode> queue = new LinkedList<>();
        for (TrieNode child : newRoot.next.values()) {
            child.fail = newRoot; // First-level child fail pointers point to root
            queue.add(child);
        }

        while (!queue.isEmpty()) {
            TrieNode parent = queue.poll();
            for (Map.Entry<Character, TrieNode> entry : parent.next.entrySet()) {
                char c = entry.getKey();
                TrieNode child = entry.getValue();

                TrieNode failNode = parent.fail;
                while (failNode != null && !failNode.next.containsKey(c)) {
                    failNode = failNode.fail;
                }

                if (failNode == null) {
                    child.fail = newRoot;
                } else {
                    child.fail = failNode.next.get(c);
                }

                queue.add(child);
            }
        }

        // 3. Hot swap the root reference
        this.root = newRoot;
        System.out.println("AC Automaton Trie Rebuilt. Word Count: " + sensitiveWords.size());
    }

    /**
     * Filters input text using the current dynamic AC automaton and policy configuration.
     * Thread-safe and lock-free for readers.
     */
    public FilterResult filter(String text) {
        if (text == null || text.isEmpty()) {
            return new FilterResult("", false, Collections.emptyList());
        }

        String lowerText = text.toLowerCase();
        char[] chars = text.toCharArray();
        char[] lowerChars = lowerText.toCharArray();
        boolean[] matchedIndices = new boolean[chars.length];
        boolean hasSensitiveWord = false;
        List<String> matchedWords = new ArrayList<>();

        TrieNode current = this.root;
        for (int i = 0; i < lowerChars.length; i++) {
            char c = lowerChars[i];

            while (current != this.root && !current.next.containsKey(c)) {
                current = current.fail;
            }

            current = current.next.getOrDefault(c, this.root);

            TrieNode temp = current;
            while (temp != this.root) {
                if (temp.isEnd) {
                    hasSensitiveWord = true;
                    String matchedWord = temp.word;
                    matchedWords.add(matchedWord);

                    // Mark matched indices for MASK replacement
                    int startIdx = i - matchedWord.length() + 1;
                    for (int k = Math.max(0, startIdx); k <= i; k++) {
                        matchedIndices[k] = true;
                    }
                }
                temp = temp.fail;
            }
        }

        // Perform MASK replacement if policy is MASK and sensitive word exists
        String filteredText = text;
        if (hasSensitiveWord && "MASK".equalsIgnoreCase(this.policy)) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < chars.length; i++) {
                if (matchedIndices[i]) {
                    sb.append("*");
                } else {
                    sb.append(chars[i]);
                }
            }
            filteredText = sb.toString();
        }

        return new FilterResult(filteredText, hasSensitiveWord, matchedWords);
    }
}
