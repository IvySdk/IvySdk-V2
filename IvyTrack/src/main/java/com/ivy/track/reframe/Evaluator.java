package com.ivy.track.reframe;


import java.util.List;
import java.util.Map;
import java.util.Stack;

public final class Evaluator {
    private Evaluator() {
        // Private constructor to prevent instantiation
    }

    public static boolean evaluate(List<Token> postfix, Map<String, Boolean> variables) throws Exception {
        Stack<Boolean> stack = new Stack<>();

        for (Token token : postfix) {
            TokenType type = token.getType();
            String value = token.getValue();

            switch (type) {
                case VARIABLE:
                    if (!variables.containsKey(value)) {
                        throw new Exception("变量未定义: " + value);
                    }
                    stack.push(variables.get(value));
                    break;

                case AND:
                    if (stack.size() < 2) {
                        throw new Exception("AND操作数不足");
                    }
                    boolean firstAnd = stack.pop();
                    boolean secondAnd = stack.pop();
                    stack.push(firstAnd && secondAnd);
                    break;

                case OR:
                    if (stack.size() < 2) {
                        throw new Exception("OR操作数不足");
                    }
                    boolean firstOr = stack.pop();
                    boolean secondOr = stack.pop();
                    stack.push(firstOr || secondOr);
                    break;

                case NOT:
                    if (stack.isEmpty()) {
                        throw new Exception("NOT操作数不足");
                    }
                    stack.push(!stack.pop());
                    break;

                default:
                    throw new Exception("无效的标记类型");
            }
        }

        if (stack.size() != 1) {
            throw new Exception("表达式无效");
        }
        return stack.pop();
    }
}