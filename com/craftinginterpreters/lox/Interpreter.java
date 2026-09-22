package com.craftinginterpreters.lox;

class Interpreter implements Expr.Visitor<Object> {
  void interpret(Expr expression) {
    try {
      Object value = evaluate(expression);
      System.out.println(stringify(value));
    } catch (RuntimeError error) {
      Lox.runtimeError(error);
    }
  }

  @Override
  public Object visitLiteralExpr(Expr.Literal expr) {
    return expr.value;
  }

  @Override
  public Object visitGroupingExpr(Expr.Grouping expr) {
    return evaluate(expr.expression);
  }

  @Override
  public Object visitUnaryExpr(Expr.Unary expr) {
    Object right = evaluate(expr.right);

    switch (expr.operator.type) {
      case BANG:
        return !isTruthy(right);
      case MINUS:
        checkNumberOperand(expr.operator, right);
        return -(double) right;
      default:
        return null;
    }
  }

  @Override
  public Object visitBinaryExpr(Expr.Binary expr) {
    // Evaluate left first so comma expressions preserve C's evaluation order.
    Object left = evaluate(expr.left);
    Object right = evaluate(expr.right);

    switch (expr.operator.type) {
      case COMMA:
        return right;

      case GREATER:
        return compare(expr.operator, left, right) > 0;
      case GREATER_EQUAL:
        return compare(expr.operator, left, right) >= 0;
      case LESS:
        return compare(expr.operator, left, right) < 0;
      case LESS_EQUAL:
        return compare(expr.operator, left, right) <= 0;

      case MINUS:
        checkNumberOperands(expr.operator, left, right);
        return (double) left - (double) right;
      case PLUS:
        if (left instanceof Double && right instanceof Double) {
          return (double) left + (double) right;
        }

        // Challenge 7.2: if either value is a string, stringify both values.
        if (left instanceof String || right instanceof String) {
          return stringify(left) + stringify(right);
        }

        throw new RuntimeError(expr.operator,
            "Operands must be two numbers or include a string.");
      case SLASH:
        checkNumberOperands(expr.operator, left, right);
        if ((double) right == 0.0) {
          throw new RuntimeError(expr.operator, "Cannot divide by zero.");
        }
        return (double) left / (double) right;
      case STAR:
        checkNumberOperands(expr.operator, left, right);
        return (double) left * (double) right;

      case BANG_EQUAL:
        return !isEqual(left, right);
      case EQUAL_EQUAL:
        return isEqual(left, right);
      default:
        return null;
    }
  }

  @Override
  public Object visitConditionalExpr(Expr.Conditional expr) {
    if (isTruthy(evaluate(expr.condition))) {
      return evaluate(expr.thenBranch);
    }
    return evaluate(expr.elseBranch);
  }

  private Object evaluate(Expr expr) {
    return expr.accept(this);
  }

  private boolean isTruthy(Object object) {
    if (object == null)
      return false;
    if (object instanceof Boolean)
      return (boolean) object;
    return true;
  }

  private boolean isEqual(Object a, Object b) {
    if (a == null && b == null)
      return true;
    if (a == null)
      return false;
    return a.equals(b);
  }

  private int compare(Token operator, Object left, Object right) {
    if (left instanceof Double && right instanceof Double) {
      return Double.compare((double) left, (double) right);
    }

    // Challenge 7.1: compare two strings lexicographically.
    if (left instanceof String && right instanceof String) {
      return ((String) left).compareTo((String) right);
    }

    throw new RuntimeError(operator,
        "Operands must be two numbers or two strings.");
  }

  private void checkNumberOperand(Token operator, Object operand) {
    if (operand instanceof Double)
      return;
    throw new RuntimeError(operator, "Operand must be a number.");
  }

  private void checkNumberOperands(Token operator, Object left, Object right) {
    if (left instanceof Double && right instanceof Double)
      return;
    throw new RuntimeError(operator, "Operands must be numbers.");
  }

  private String stringify(Object object) {
    if (object == null)
      return "nil";

    if (object instanceof Double) {
      String text = object.toString();
      if (text.endsWith(".0")) {
        text = text.substring(0, text.length() - 2);
      }
      return text;
    }

    return object.toString();
  }
}
