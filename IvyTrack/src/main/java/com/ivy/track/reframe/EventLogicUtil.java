package com.ivy.track.reframe;

import java.util.List;
import java.util.Map;

public final class EventLogicUtil {
    private EventLogicUtil() {
        // Private constructor to prevent instantiation
    }

    public static List<Token> formatLogicalExpression(String expression) throws Exception {
        Tokenizer tokenizer = new Tokenizer(expression);
        List<Token> tokens = tokenizer.tokenize();

        Parser parser = new Parser(tokens);
        return parser.parseToPostfix();
    }

    public static boolean evaluate(List<Token> postfix, Map<String, Boolean> variables) throws Exception {
        return Evaluator.evaluate(postfix, variables);
    }
}