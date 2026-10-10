package com.ivy.track.reframe;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class Parser {
    private List<Token> tokens;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    public List<Token> parseToPostfix() throws Exception {
        List<Token> output = new ArrayList<>();
        Stack<Token> operatorStack = new Stack<>();

        for (Token token : tokens) {
            switch (token.getType()) {
                case VARIABLE:
                    output.add(token);
                    break;
                case LEFT_PAREN:
                    operatorStack.push(token);
                    break;
                case RIGHT_PAREN:
                    while (!operatorStack.isEmpty() &&
                            operatorStack.peek().getType() != TokenType.LEFT_PAREN) {
                        output.add(operatorStack.pop());
                    }
                    if (operatorStack.isEmpty()) {
                        throw new Exception("括号不匹配");
                    }
                    operatorStack.pop(); // Remove left parenthesis
                    break;
                case AND:
                case OR:
                case NOT:
                    while (!operatorStack.isEmpty() &&
                            getPrecedence(operatorStack.peek()) >= getPrecedence(token)) {
                        output.add(operatorStack.pop());
                    }
                    operatorStack.push(token);
                    break;
                default:
                    throw new Exception("意外的标记: " + token.getType());
            }
        }

        while (!operatorStack.isEmpty()) {
            Token op = operatorStack.pop();
            if (op.getType() == TokenType.LEFT_PAREN) {
                throw new Exception("括号不匹配");
            }
            output.add(op);
        }

        return output;
    }

    private int getPrecedence(Token token) {
        switch (token.getType()) {
            case NOT:
                return 3;
            case AND:
                return 2;
            case OR:
                return 1;
            default:
                return 0;
        }
    }
}