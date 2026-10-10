package com.ivy.track.reframe;

import java.util.ArrayList;
import java.util.List;

public class Tokenizer {
    private String expression;
    private int pos = 0;

    public Tokenizer(String expression) {
        // Remove whitespace
        this.expression = expression.replaceAll("\\s+", "");
    }

    public List<Token> tokenize() throws Exception {
        List<Token> tokens = new ArrayList<>();
        while (pos < expression.length()) {
            char c = expression.charAt(pos);
            if (Character.isLetterOrDigit(c)) {
                StringBuilder sb = new StringBuilder();
                while (pos < expression.length() &&
                        Character.isLetterOrDigit(expression.charAt(pos))) {
                    sb.append(expression.charAt(pos));
                    pos++;
                }
                tokens.add(new Token(TokenType.VARIABLE, sb.toString()));
            } else if (c == '(') {
                tokens.add(new Token(TokenType.LEFT_PAREN, "("));
                pos++;
            } else if (c == ')') {
                tokens.add(new Token(TokenType.RIGHT_PAREN, ")"));
                pos++;
            } else if (c == '&') {
                if (pos + 1 < expression.length() && expression.charAt(pos + 1) == '&') {
                    tokens.add(new Token(TokenType.AND, "&&"));
                    pos += 2;
                } else {
                    throw new Exception("无效字符: " + c);
                }
            } else if (c == '|') {
                if (pos + 1 < expression.length() && expression.charAt(pos + 1) == '|') {
                    tokens.add(new Token(TokenType.OR, "||"));
                    pos += 2;
                } else {
                    throw new Exception("无效字符: " + c);
                }
            } else if (c == '!') {
                tokens.add(new Token(TokenType.NOT, "!"));
                pos++;
            } else {
                throw new Exception("无效字符: " + c);
            }
        }
        return tokens;
    }
}